package com.group_finity.mascot.lumi.plugin;

import com.group_finity.mascot.lumi.LumiHotkeys;

import java.awt.Point;
import java.awt.Rectangle;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.logging.Logger;

/**
 * A headless test harness for plugins — the app's jar cannot be run from a test (the Steam gate) and
 * {@link PluginContext}'s constructor is package-private, so a plugin's own tests get their context
 * from here.
 * <p>
 * {@link #start} hands out a {@link PluginContext} whose every door leads to this kit instead of the
 * app: what the plugin says, queues, asks, forces, summons, awards or registers is <b>recorded</b>
 * ({@link #calls}), what it reads (mascots, affection, focus, language, settings…) comes from fakes
 * you set ({@link #mascots}, {@link #affection}, …, sane defaults otherwise), events you
 * {@link #publish} reach its listeners <b>synchronously on your thread</b>, {@link #ask} runs its hook
 * handlers, {@link #tick} advances a fake clock for {@code every}/{@code after}, and
 * {@link #answerChoice} completes the last choice bubble. Nothing of the app starts — no engine, no
 * windows, no Steam. {@code prefs()} is a real store in a temporary folder ({@link #dataDir}),
 * deleted by {@link #close}, which releases context-managed registrations. The kit does not
 * instantiate your plugin, publish {@code ready}, or call its {@code stop()} for you.
 * Call {@code stop()} before closing the kit and release preference listeners and owned resources.
 * <p>
 * Threads: the kit runs listeners, hooks, scheduled tasks and {@code onEdt}/{@code offEdt} inline, so
 * those operations are deterministic. Preference change notifications remain asynchronous
 * because the preferences store is real. Bytecode ({@code patch}/{@code onLoad}) is not simulated — without
 * instrumentation those return {@code false}, as in a {@code -cp} run of the app.
 * <pre>
 * try (PluginTestKit kit = PluginTestKit.start("my.plugin.test")) {
 *     kit.context().on("ready", event -&gt; kit.context().say("Hello", 3000));
 *     kit.publish("ready");
 *     if (kit.calls("say").size() != 1) {
 *         throw new AssertionError("Expected one greeting");
 *     }
 * }
 * </pre>
 * Ships in the app jar so {@code lib\Shimeji-ee.jar} of the SDK carries it; the SDK's
 * {@code tools\run_tests.py} compiles and runs {@code test\*.java} of a plugin folder against it.
 */
public final class PluginTestKit implements AutoCloseable {

    /** One recorded call: the {@link PluginContext} method's name and its arguments in order. */
    public record Call(String name, List<Object> args) {
        public Call {
            args = args == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(args));
        }

        public Object arg(int i) {
            return i < args.size() ? args.get(i) : null;
        }

        @Override
        public String toString() {
            return name + args;
        }
    }

    private final PluginDescriptor descriptor;
    private final Recording backend;
    private final PluginContext context;
    private final Path dataDir;
    private final PluginPrefs prefs;
    private boolean closed;

    private PluginTestKit(PluginDescriptor descriptor, ClassLoader textLoader) throws IOException {
        this.descriptor = descriptor;
        this.dataDir = Files.createTempDirectory("lumi-plugin-test-" + safe(descriptor.id) + "-");
        this.prefs = PluginPrefs.at(descriptor.id, dataDir.resolve("settings.properties"));
        this.backend = new Recording(textLoader == null ? pickLoader() : textLoader);
        this.context = new PluginContext(descriptor, backend);
        try {
            descriptor.settings.seedDefaults(prefs);
        } catch (Exception ignored) {
            // 스키마 없는 매니페스트
        }
    }

    // ── making one ──────────────────────────────────────────────────────────

    /** A kit for a plugin with that id and no manifest (no settings schema, no dependencies). */
    public static PluginTestKit start(String pluginId) {
        String id = pluginId == null || pluginId.isBlank() ? "test.plugin" : pluginId.strip();
        return start(PluginDescriptor.parse("{\"id\":\"" + id + "\",\"name\":\"" + id + "\"}", null, "", "test"));
    }

    /** A kit built from a real {@code plugin.json} — its settings defaults are seeded into {@code prefs()}. */
    public static PluginTestKit start(Path pluginJson) {
        try {
            String text = Files.readString(pluginJson, StandardCharsets.UTF_8);
            return start(PluginDescriptor.parse(text, null, "", "test"));
        } catch (IOException e) {
            throw new IllegalArgumentException("cannot read " + pluginJson + ": " + e, e);
        }
    }

    public static PluginTestKit start(PluginDescriptor descriptor) {
        try {
            return new PluginTestKit(descriptor, null);
        } catch (IOException e) {
            throw new IllegalStateException("cannot create the kit's temp folder: " + e, e);
        }
    }

    public PluginContext context() {
        return context;
    }

    public PluginDescriptor descriptor() {
        return descriptor;
    }

    /** The temporary folder behind {@code ctx.dataDir()} and {@code ctx.prefs()}; deleted by {@link #close}. */
    public Path dataDir() {
        return dataDir;
    }

    /** Where {@code ctx.text(...)} looks for {@code plugin_<lang>.properties}; default = the test's class loader. */
    public PluginTestKit textLoader(ClassLoader loader) {
        backend.textLoader = loader;
        return this;
    }

    // ── what the plugin did ─────────────────────────────────────────────────

    /** Every recorded call, in order. */
    public List<Call> calls() {
        return List.copyOf(backend.calls);
    }

    /** The recorded calls of one method ({@code "say"}, {@code "behave"}, {@code "trayItem"}, …). */
    public List<Call> calls(String name) {
        List<Call> out = new ArrayList<>();
        for (Call c : backend.calls) {
            if (c.name.equals(name)) {
                out.add(c);
            }
        }
        return out;
    }

    /** The last call of that method, or {@code null}. */
    public Call lastCall(String name) {
        List<Call> all = calls(name);
        return all.isEmpty() ? null : all.get(all.size() - 1);
    }

    public void clearCalls() {
        backend.calls.clear();
    }

    /** Lines the plugin queued with {@code sayQueued}, in order (they never "show" in the kit). */
    public List<String> queued() {
        return List.copyOf(backend.queue);
    }

    /** Menu rows the plugin added (tray and character menu), with their live handles. */
    public List<PluginUi.MenuHandle> menuRows() {
        return List.copyOf(backend.rows);
    }

    /** Switches the plugin added to the Lumi tab's 「기능 켜고 끄기」 card. */
    public List<PluginUi.FeatureToggle> featureToggles() {
        return List.copyOf(backend.featureToggles);
    }

    /** Hotkeys the plugin registered: name → combination (as given). */
    public Map<String, String> hotkeys() {
        return Map.copyOf(backend.hotkeys);
    }

    /** True while a choice bubble ({@code ask}) is open — answer it with {@link #answerChoice}. */
    public boolean choiceOpen() {
        return backend.openChoice != null;
    }

    // ── fakes the plugin reads ──────────────────────────────────────────────

    public PluginTestKit mascots(List<MascotState> mascots) {
        backend.mascots = mascots == null ? List.of() : List.copyOf(mascots);
        return this;
    }

    public PluginTestKit mascots(MascotState... mascots) {
        return mascots(mascots == null ? List.of() : List.of(mascots));
    }

    public PluginTestKit affection(int tier, int progressPercent, int pointsUntilNextTier, int daysTogether) {
        backend.tier = tier;
        backend.progress = progressPercent;
        backend.untilNext = pointsUntilNextTier;
        backend.days = daysTogether;
        return this;
    }

    /** What {@code affectionTodayRemaining(...)} answers for every source. */
    public PluginTestKit affectionRemaining(int points) {
        backend.remaining = points;
        return this;
    }

    public PluginTestKit focus(boolean active, String sources) {
        backend.focusActive = active;
        backend.focusSources = sources == null ? "" : sources;
        return this;
    }

    public PluginTestKit screenBusy(boolean busy) {
        backend.screenBusy = busy;
        return this;
    }

    public PluginTestKit bubbleVisible(boolean visible) {
        backend.bubbleVisible = visible;
        return this;
    }

    public PluginTestKit charactersHidden(boolean hidden) {
        backend.hidden = hidden;
        return this;
    }

    /** {@code ko} {@code en} {@code ja} {@code zh-CN} {@code zh-TW}; default {@code ko}. */
    public PluginTestKit language(String code) {
        backend.language = code == null ? "ko" : code;
        return this;
    }

    public PluginTestKit behaviorNames(Set<String> names) {
        backend.behaviorNames = names == null ? Set.of() : Set.copyOf(names);
        return this;
    }

    public PluginTestKit currentBehavior(String name) {
        backend.currentBehavior = name == null ? "" : name;
        return this;
    }

    public PluginTestKit installedImageSets(List<String> sets) {
        backend.installed = sets == null ? List.of() : List.copyOf(sets);
        return this;
    }

    public PluginTestKit activeImageSets(List<String> sets) {
        backend.active = sets == null ? List.of() : List.copyOf(sets);
        return this;
    }

    public PluginTestKit screens(List<Rectangle> screens) {
        backend.screens = screens == null ? List.of() : List.copyOf(screens);
        return this;
    }

    /** A value of the app's {@code conf/settings.properties} as the plugin will read it with {@code setting()}. */
    public PluginTestKit setting(String key, String value) {
        if (value == null) {
            backend.settings.remove(key);
        } else {
            backend.settings.put(key, value);
        }
        return this;
    }

    /** A line of the app's five-language bundle as {@code localized()} (and {@code text()}'s last step) will find it. */
    public PluginTestKit appText(String key, String value) {
        if (value == null) {
            backend.appBundle.remove(key);
        } else {
            backend.appBundle.put(key, value);
        }
        return this;
    }

    /** What {@code playSound} answers (sounds on/off); default true. */
    public PluginTestKit soundsEnabled(boolean enabled) {
        backend.sounds = enabled;
        return this;
    }

    // ── driving the plugin ──────────────────────────────────────────────────

    /**
     * Delivers an event to the plugin's listeners on that topic, <b>synchronously</b>, key/value pairs
     * as the app publishes them ({@code publish("click", "button", "left", "x", 10)}). A listener that
     * throws is logged and the next one still runs — the app's rule.
     */
    public void publish(String topic, Object... keyValues) {
        Map<String, Object> payload = new HashMap<>();
        if (keyValues != null) {
            for (int i = 0; i + 1 < keyValues.length; i += 2) {
                Object v = keyValues[i + 1];
                payload.put(String.valueOf(keyValues[i]), v == null ? "" : v);
            }
        }
        backend.deliver(topic, Map.copyOf(payload));
    }

    /**
     * Asks the plugin's hook handlers the way the core does: {@code defaultValue} first, each handler
     * sees the value so far, {@code null} = pass, a value of another type than the default = pass,
     * a throwing handler is skipped. Returns the final answer.
     */
    @SuppressWarnings("unchecked")
    public <T> T ask(String hook, T defaultValue, Object... keyValues) {
        Map<String, Object> ctx = PluginHooks.context(keyValues);
        Object current = defaultValue;
        Class<?> type = defaultValue == null ? null : defaultValue.getClass();
        for (PluginHooks.Handler h : backend.handlers.getOrDefault(hook, List.of())) {
            Object answer;
            try {
                answer = h.handle(hook, current, ctx);
            } catch (Throwable t) {
                Logger.getLogger("plugin." + descriptor.id).warning("hook '" + hook + "' threw: " + t);
                continue;
            }
            if (answer == null || (type != null && !type.isInstance(answer)
                    && !(current instanceof Number && answer instanceof Number))) {
                continue;
            }
            current = answer;
        }
        return (T) current;
    }

    /**
     * Completes the open choice bubble with that option index (or -1) and opens the next one waiting
     * in line, as the app does; false when none is open. A plugin's newer {@code ask} supersedes its
     * own open or waiting one with {@code -2} here too.
     */
    public boolean answerChoice(int index) {
        Recording.Choice c = backend.settle(index);
        if (c == null) {
            return false;
        }
        backend.record("choiceAnswered", index);
        if (c.onPick != null) {
            c.onPick.accept(index);
        }
        return true;
    }

    /** Choice bubbles waiting behind the open one. */
    public int choicesWaiting() {
        return backend.waitingChoices.size();
    }

    /**
     * What {@code ctx.featureEnabled(id)} answers for one of the settings-window switches
     * ({@code bongo} {@code broadcast} {@code island} {@code game} {@code focus.auto}); default off.
     */
    public PluginTestKit feature(String id, boolean enabled) {
        String key = PluginContext.FEATURE_IDS.get(id == null ? "" : id.strip().toLowerCase(java.util.Locale.ROOT));
        if (key != null) {
            backend.features.put(key, enabled);
        }
        return this;
    }

    /** The options of the open choice bubble, or an empty list. */
    public List<String> choiceOptions() {
        Recording.Choice c = backend.openChoice;
        return c == null ? List.of() : c.options;
    }

    /** The kit's fake clock, ms since start (0 at {@link #start}). */
    public long now() {
        return backend.now;
    }

    /**
     * Advances the fake clock by {@code ms} and runs every {@code every}/{@code after} task that
     * falls due, in due order, a periodic task as many times as its period fits. Tasks run on the
     * calling thread.
     */
    public void tick(long ms) {
        backend.tick(Math.max(0, ms));
    }

    /** Releases context-managed registrations and deletes the temp folder. Idempotent.
     * Does not call the plugin's stop method or remove its preference listeners. */
    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        context.releaseAll();
        deleteTree(dataDir);
    }

    // ── internals ───────────────────────────────────────────────────────────

    private static String safe(String id) {
        return id == null ? "plugin" : id.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private static ClassLoader pickLoader() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        return cl != null ? cl : ClassLoader.getSystemClassLoader();
    }

    private static void deleteTree(Path root) {
        try (var walk = Files.walk(root)) {
            walk.sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                    // 검사 임시 폴더 — 남아도 해가 없다
                }
            });
        } catch (IOException ignored) {
            // 위와 같다
        }
    }

    /** 기록하는 {@link PluginBackend} — 앱에는 손을 대지 않는다. */
    /**
     * A context on the <b>product</b> backend — the same statics the app uses (focus mode, the
     * speech director, the affection store…), no app window needed. For integration checks that
     * drive a plugin's classes against real app state (the repo's own tests do this); a modder's
     * unit tests want {@link #start} instead. Nothing here is recorded or released for you.
     */
    public static PluginContext realContext(String pluginId) {
        String id = pluginId == null || pluginId.isBlank() ? "test.plugin" : pluginId.strip();
        PluginDescriptor descriptor = PluginDescriptor.parse(
                "{\"id\":\"" + id + "\",\"name\":\"" + id + "\"}", null, "", "test");
        return new PluginContext(descriptor, RealPluginBackend.INSTANCE);
    }

    // ── 가짜 값 ──────────────────────────────────────────────────────────────

    /** {@code ctx.quietNow()} 의 답을 정한다(집중·숨김·화면 바쁨 중 하나라도 참이면 어차피 참). */
    public PluginTestKit quiet(boolean quiet) {
        backend.quiet = quiet;
        return this;
    }

    /** {@code ctx.foregroundScreen()} 이 돌려줄 사각형({@code null} = 모름). */
    public PluginTestKit foregroundScreen(Rectangle bounds) {
        backend.foreground = bounds == null ? null : new Rectangle(bounds);
        return this;
    }

    /** {@code ctx.persona(imageSet)} 이 돌려줄 글. */
    public PluginTestKit persona(String imageSet, String text) {
        backend.personas.put(imageSet == null || imageSet.isBlank() ? "Lumi" : imageSet, text == null ? "" : text);
        return this;
    }

    /** {@code ctx.isSpeaking()} 의 답. {@code speak()} 는 기록판에서 즉시 끝나므로 길게 말하는 척할 때 쓴다. */
    public PluginTestKit speaking(boolean speaking) {
        backend.speaking = speaking;
        return this;
    }

    /** 마지막 {@code setSpeechVolume} 값(기본 100). */
    public int speechVolume() {
        return backend.speechVolume;
    }

    /** 지금 입을 뻐끔이는 이미지셋, 없으면 {@code null}. */
    public String mouthFlapping() {
        return backend.flapping;
    }

    private final class Recording implements PluginBackend {
        final List<Call> calls = new CopyOnWriteArrayList<>();
        final Map<String, List<Consumer<Map<String, Object>>>> listeners = new LinkedHashMap<>();
        final Map<String, List<PluginHooks.Handler>> handlers = new LinkedHashMap<>();
        final List<String> queue = new ArrayList<>();
        final List<PluginUi.MenuHandle> rows = new ArrayList<>();
        final List<PluginUi.FeatureToggle> featureToggles = new ArrayList<>();
        final Map<String, String> hotkeys = new LinkedHashMap<>();
        final Map<String, String> settings = new HashMap<>();
        final Map<String, String> appBundle = new HashMap<>();
        final List<Task> tasks = new ArrayList<>();
        volatile ClassLoader textLoader;
        Choice openChoice;
        long now;
        int nextId = 1;

        List<MascotState> mascots = List.of();
        int tier = 1;
        int progress;
        int untilNext;
        int days;
        int remaining = 2;
        boolean focusActive;
        String focusSources = "";
        boolean screenBusy;
        boolean bubbleVisible;
        boolean hidden;
        String language = "ko";
        Set<String> behaviorNames = Set.of();
        String currentBehavior = "";
        List<String> installed = List.of("Lumi");
        List<String> active = List.of("Lumi");
        List<Rectangle> screens = List.of(new Rectangle(0, 0, 1920, 1080));
        boolean sounds = true;
        int micCaptures;

        record Choice(String owner, String imageSet, String text, List<String> options, long timeoutMs,
                      long maxWaitMs, long offeredAt, IntConsumer onPick) {
        }

        /** 차례를 기다리는 선택지(앱의 줄서기와 같은 규칙). */
        final List<Choice> waitingChoices = new ArrayList<>();
        /** {@code featureEnabled} 의 가짜값 — 앱 키로 든다({@link PluginTestKit#feature}). */
        final Map<String, Boolean> features = new HashMap<>();

        final class Task implements ScheduledHandle {
            long dueAt;
            final long period;
            final Runnable body;
            boolean cancelled;

            Task(long dueAt, long period, Runnable body) {
                this.dueAt = dueAt;
                this.period = period;
                this.body = body;
            }

            @Override
            public void cancel() {
                cancelled = true;
            }

            @Override
            public boolean isCancelled() {
                return cancelled;
            }
        }

        Recording(ClassLoader textLoader) {
            this.textLoader = textLoader;
        }

        void record(String name, Object... args) {
            calls.add(new Call(name, args == null ? List.of() : java.util.Arrays.asList(args)));
        }

        void deliver(String topic, Map<String, Object> payload) {
            List<Consumer<Map<String, Object>>> list;
            synchronized (this) {
                list = listeners.get(topic);
                list = list == null ? List.of() : new ArrayList<>(list);
            }
            for (Consumer<Map<String, Object>> l : list) {
                try {
                    l.accept(payload);
                } catch (Throwable t) {
                    Logger.getLogger("plugin." + descriptor.id).warning("listener on '" + topic + "' threw: " + t);
                }
            }
        }

        void tick(long ms) {
            long target = now + ms;
            // 줄에서 maxWaitMs 를 넘긴 선택지는 뜨지 않은 채 -2 — 앱의 followChoice 와 같은 규칙.
            for (java.util.Iterator<Choice> it = waitingChoices.iterator(); it.hasNext();) {
                Choice w = it.next();
                if (target >= w.offeredAt + w.maxWaitMs) {
                    it.remove();
                    superseded(w);
                }
            }
            while (true) {
                Task next = null;
                synchronized (this) {
                    tasks.removeIf(t -> t.cancelled);
                    for (Task t : tasks) {
                        if (t.dueAt <= target && (next == null || t.dueAt < next.dueAt)) {
                            next = t;
                        }
                    }
                }
                if (next == null) {
                    break;
                }
                now = Math.max(now, next.dueAt);
                next.body.run();
                synchronized (this) {
                    if (next.period > 0 && !next.cancelled) {
                        next.dueAt += next.period;
                    } else {
                        tasks.remove(next);
                    }
                }
            }
            now = target;
        }

        // speech
        @Override
        public void say(int mascotId, String imageSet, String text, long ms) {
            record("say", imageSet, text, ms);
        }

        @Override
        public void hideBubble() {
            record("hideBubble");
        }

        @Override
        public void showBusy(int mascotId, String imageSet) {
            record("showBusy", imageSet);
        }

        @Override
        public void enqueue(int mascotId, String imageSet, String text, long ms, String owner) {
            record("sayQueued", imageSet, text, ms);
            queue.add(text);
        }

        @Override
        public void clearQueue(String owner) {
            record("clearQueue");
            queue.clear();
        }

        @Override
        public int queueLength() {
            return queue.size();
        }

        @Override
        public void ask(int mascotId, String imageSet, String text, List<String> options, long timeoutMs,
                        long maxWaitMs, String owner, IntConsumer onPick) {
            record("ask", imageSet, text, options, timeoutMs, maxWaitMs);
            if (options == null || options.size() < 2) {
                if (onPick != null) {
                    onPick.accept(-1);
                }
                return;
            }
            // 앱과 같은 줄서기: 같은 주인의 표는 자리 하나 — 새 ask 가 옛 표를 -2 로 대체한다. 킷에는
            // 플러그인이 하나뿐이라 남의 표가 기다리는 경우는 없지만, 규칙은 같은 코드로 둔다.
            String who = owner == null ? "" : owner;
            Choice fresh = new Choice(who, imageSet, text, List.copyOf(options), timeoutMs,
                    maxWaitMs <= 0 ? PluginContext.DEFAULT_ASK_WAIT_MS : maxWaitMs, now, onPick);
            if (openChoice != null && who.equals(openChoice.owner)) {
                Choice bumped = openChoice;
                openChoice = fresh;
                superseded(bumped);
                return;
            }
            for (int i = 0; i < waitingChoices.size(); i++) {
                if (who.equals(waitingChoices.get(i).owner)) {
                    superseded(waitingChoices.set(i, fresh));
                    return;
                }
            }
            if (openChoice == null) {
                openChoice = fresh;
            } else {
                waitingChoices.add(fresh);
            }
        }

        private void superseded(Choice bumped) {
            record("choiceSuperseded", bumped.text);
            if (bumped.onPick != null) {
                bumped.onPick.accept(-2);
            }
        }

        /** 뜬 표를 닫고 줄의 다음 표를 연다 — {@link PluginTestKit#answerChoice}. */
        Choice settle(int index) {
            Choice c = openChoice;
            openChoice = waitingChoices.isEmpty() ? null : waitingChoices.remove(0);
            return c;
        }

        // behaviour
        @Override
        public void behaveAll(String behaviorName) {
            record("behave", behaviorName);
        }

        @Override
        public void behave(String imageSet, String behaviorName) {
            record("behaveFor", imageSet, behaviorName);
        }

        @Override
        public String currentBehavior(String imageSet) {
            return currentBehavior;
        }

        @Override
        public Set<String> behaviorNames(String imageSet) {
            return behaviorNames;
        }

        // state
        @Override
        public List<MascotState> mascots(boolean includeProps) {
            return mascots;
        }

        @Override
        public MascotState mascot(String imageSet) {
            for (MascotState m : mascots) {
                if (imageSet == null || imageSet.equals(m.imageSet())) {
                    return m;
                }
            }
            return null;
        }

        @Override
        public MascotState mascotById(int id) {
            for (MascotState m : mascots) {
                if (m.id() == id) {
                    return m;
                }
            }
            return null;
        }

        @Override
        public MascotState mascotAt(Point screen) {
            if (screen == null) {
                return null;
            }
            for (MascotState m : mascots) {
                if (m.bounds().contains(screen)) {
                    return m;
                }
            }
            return null;
        }

        @Override
        public int affectionTier() {
            return tier;
        }

        @Override
        public int affectionProgressPercent() {
            return progress;
        }

        @Override
        public int affectionPointsUntilNextTier() {
            return untilNext;
        }

        @Override
        public int affectionTodayRemaining(String sourceKey) {
            return remaining;
        }

        @Override
        public int daysTogether() {
            return days;
        }

        @Override
        public boolean focusActive() {
            return focusActive;
        }

        @Override
        public String focusSources() {
            return focusSources;
        }

        @Override
        public boolean screenBusy() {
            return screenBusy;
        }

        @Override
        public boolean bubbleVisible() {
            return bubbleVisible;
        }

        @Override
        public boolean charactersHidden() {
            return hidden;
        }

        @Override
        public String language() {
            return language;
        }

        @Override
        public List<String> installedImageSets() {
            return installed;
        }

        @Override
        public List<String> activeImageSets() {
            return active;
        }

        @Override
        public List<Rectangle> screens() {
            return screens;
        }

        // control
        @Override
        public int summon(String imageSet) {
            int id = 1000 + nextId++;
            record("summon", imageSet, id);
            return id;
        }

        @Override
        public boolean dismiss(int mascotId) {
            record("dismiss", mascotId);
            return true;
        }

        @Override
        public boolean moveTo(int mascotId, int x, int y) {
            record("moveTo", mascotId, x, y);
            return true;
        }

        @Override
        public void setCharactersVisible(boolean visible) {
            record("setCharactersVisible", visible);
            hidden = !visible;
        }

        @Override
        public int awardAffection(String pluginId, String source, int points) {
            int credited = Math.max(0, Math.min(points, remaining));
            remaining -= credited;
            record("awardAffection", source, points, credited);
            return credited;
        }

        @Override
        public void heart(String imageSet) {
            record("heart", imageSet);
        }

        @Override
        public boolean playSound(String path) {
            record("playSound", path);
            return sounds && path != null && !path.isBlank();
        }

        // events · hooks
        @Override
        public synchronized void subscribe(String pluginId, String topic, Consumer<Map<String, Object>> listener) {
            listeners.computeIfAbsent(topic, k -> new ArrayList<>()).add(listener);
        }

        @Override
        public synchronized void unsubscribe(String pluginId, String topic, Consumer<Map<String, Object>> listener) {
            List<Consumer<Map<String, Object>>> list = listeners.get(topic);
            if (list != null) {
                list.remove(listener);
            }
        }

        @Override
        public void publish(String topic, Map<String, Object> payload) {
            record("publish", topic, payload);
            deliver(topic, payload);
        }

        @Override
        public synchronized void hook(String pluginId, String hook, PluginHooks.Handler handler) {
            handlers.computeIfAbsent(hook, k -> new ArrayList<>()).add(handler);
        }

        @Override
        public synchronized void unhook(String pluginId, String hook, PluginHooks.Handler handler) {
            List<PluginHooks.Handler> list = handlers.get(hook);
            if (list != null) {
                list.remove(handler);
            }
        }

        // settings · text · folder
        @Override
        public Path dataDir(String pluginId) {
            return dataDir;
        }

        @Override
        public PluginPrefs prefs(String pluginId) {
            return prefs;
        }

        @Override
        public String setting(String key, String fallback) {
            return settings.getOrDefault(key, fallback);
        }

        @Override
        public void setSetting(String key, String value) {
            record("setSetting", key, value);
            settings.put(key, value);
        }

        @Override
        public String localized(String key, String fallback) {
            return appBundle.getOrDefault(key, fallback);
        }

        @Override
        public ClassLoader textLoader() {
            return textLoader;
        }

        // UI
        @Override
        public PluginUi.TrayItem addTrayItem(PluginUi.TrayItem item) {
            record(item.isToggle() ? "addTrayToggle" : "addTrayItem", item.label());
            rows.add(item);
            return item;
        }

        @Override
        public PluginUi.CharacterMenuItem addCharacterMenuItem(PluginUi.CharacterMenuItem item) {
            record(item.isToggle() ? "addCharacterMenuToggle" : "addCharacterMenuItem", item.label());
            rows.add(item);
            return item;
        }

        @Override
        public PluginUi.SettingsButton addSettingsButton(PluginUi.SettingsButton item) {
            record("addSettingsButton", item.label());
            rows.add(item);
            return item;
        }

        @Override
        public PluginUi.FeatureToggle addFeatureToggle(PluginUi.FeatureToggle item) {
            record("addFeatureToggle", item.label(), item.description(), item.dependsOnFeature());
            featureToggles.add(item);
            return item;
        }

        @Override
        public void claimClickGreeting(String pluginId) {
            record("claimClickGreeting");
        }

        @Override
        public void releaseClickGreeting(String pluginId) {
            record("releaseClickGreeting");
        }

        // input
        @Override
        public void unbindHotkey(String key) {
            String name = key.substring(key.indexOf(':') + 1);
            record("unbindHotkey", name);
            hotkeys.remove(name);
        }

        // scheduling — manual clock
        @Override
        public ScheduledHandle schedule(String pluginId, long initialMs, long periodMs, Runnable task, Logger log) {
            Task t = new Task(now + Math.max(0, initialMs), periodMs, task);
            synchronized (this) {
                tasks.add(t);
            }
            record(periodMs > 0 ? "every" : "after", initialMs, periodMs);
            return t;
        }

        @Override
        public void onEdt(Runnable task) {
            task.run();
        }

        @Override
        public void offEdt(String pluginId, Runnable task) {
            task.run();
        }

        @Override
        public boolean featureEnabled(String key) {
            return Boolean.TRUE.equals(features.get(key));
        }

        // ── LUMI Chat 을 옮길 때 막혔던 자리 ─────────────────────────────────
        boolean quiet;
        Rectangle foreground;
        final Map<String, String> personas = new HashMap<>();
        boolean speaking;
        int speechVolume = 100;
        String flapping;

        @Override
        public boolean quietNow() {
            return quiet || hidden || screenBusy || focusActive;
        }

        @Override
        public Rectangle foregroundScreen() {
            return foreground == null ? null : new Rectangle(foreground);
        }

        @Override
        public boolean isCharacter(String imageSet) {
            return imageSet != null && !imageSet.isBlank()
                    && !imageSet.equalsIgnoreCase("credit") && !imageSet.equalsIgnoreCase("unused");
        }

        @Override
        public String persona(String imageSet) {
            String set = imageSet == null || imageSet.isBlank() ? "Lumi" : imageSet;
            return personas.getOrDefault(set, "");
        }

        @Override
        public Path personaFile(String imageSet) {
            String set = imageSet == null || imageSet.isBlank() ? "Lumi" : imageSet;
            return dataDir(PluginTestKit.this.context.id()).resolve("img").resolve(set).resolve("persona.txt");
        }

        @Override
        public synchronized void speak(PcmAudio audio, Runnable onStart, Runnable onEnd) {
            record("speak", audio == null ? 0L : audio.millis());
            if (audio == null || micCaptures > 0) {
                if (micCaptures > 0) {
                    record("speakSuppressed");
                }
                return;
            }
            speaking = true;
            if (onStart != null) {
                onStart.run();
            }
            speaking = false;
            if (onEnd != null) {
                onEnd.run();
            }
        }

        @Override
        public void stopSpeaking() {
            record("stopSpeaking");
            speaking = false;
        }

        @Override
        public synchronized void beginMicCapture(String owner, MicCaptureLease lease) {
            record("beginMicCapture", owner);
            micCaptures++;
            speaking = false;
            lease.bind(() -> {
                synchronized (Recording.this) {
                    micCaptures--;
                    record("endMicCapture", owner);
                }
            });
        }

        @Override
        public boolean isSpeaking() {
            return speaking;
        }

        @Override
        public void setSpeechVolume(int percent) {
            record("setSpeechVolume", percent);
            speechVolume = Math.max(0, Math.min(100, percent));
        }

        @Override
        public void mouthFlap(String imageSet, java.util.function.BooleanSupplier speaking) {
            record("mouthFlap", imageSet);
            flapping = imageSet;
        }

        @Override
        public void mouthFlapFor(String imageSet, long millis) {
            record("mouthFlapFor", imageSet, millis);
            flapping = imageSet;
        }

        @Override
        public void stopMouthFlap() {
            record("stopMouthFlap");
            flapping = null;
        }

        @Override
        public void writeAtomic(Path target, byte[] content, boolean ownerOnly) throws java.io.IOException {
            record("writeAtomic", target, ownerOnly);
            Path parent = target.toAbsolutePath().getParent();
            if (parent != null) {
                java.nio.file.Files.createDirectories(parent);
            }
            Path tmp = parent == null ? Path.of(target.getFileName() + ".tmp") : parent.resolve(target.getFileName() + ".tmp");
            java.nio.file.Files.write(tmp, content == null ? new byte[0] : content);
            java.nio.file.Files.move(tmp, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }

        @Override
        public PluginTheme theme() {
            return PluginTheme.INSTANCE;
        }

        @Override
        public java.awt.Image appIcon() {
            return new java.awt.image.BufferedImage(16, 16, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        }

        @Override
        public void manageWindow(java.awt.Window window, String key) {
            record("manageWindow", key);
        }

        @Override
        public HotkeyResult bindHotkey(String key, String combination, Runnable action) {
            String name = key.substring(key.indexOf(':') + 1);
            record("bindHotkey", name, combination);
            if (combination == null || combination.isBlank()) {
                hotkeys.remove(name);
                return HotkeyResult.NONE;
            }
            String normal = normalizeHotkey(combination);
            if (normal == null) {
                return HotkeyResult.BAD_COMBINATION;
            }
            hotkeys.put(name, normal);
            return HotkeyResult.OK;
        }

        @Override
        public String normalizeHotkey(String combination) {
            return LumiHotkeys.normalize(combination);
        }

        @Override
        public String hotkeyFromKeyEvent(int keyCode, int modifiersEx) {
            return LumiHotkeys.fromKeyEvent(keyCode, modifiersEx);
        }
    }
}
