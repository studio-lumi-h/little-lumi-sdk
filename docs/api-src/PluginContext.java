package com.group_finity.mascot.lumi.plugin;

import com.group_finity.mascot.lumi.AffectionDirector;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * What a plugin is handed in {@link LumiPlugin#start}: the curated front door to
 * the app. Everything public in the jar is reachable anyway (a plugin is on the
 * app's class path). Prefer this public API over core implementation classes.
 * This documents the shipped version; test each app version you support. Thread descriptions
 * below refer to the normal app backend; {@link PluginTestKit} runs many operations inline.
 * <ul>
 *   <li><b>speech</b> — {@link #say}, {@link #sayFor}, {@link #hideBubble}, {@link #showBusy};
 *       a queue — {@link #sayQueued}, {@link #sayQueuedFor}, {@link #clearQueue}, {@link #queueLength};
 *       a bubble with buttons — {@link #ask}, {@link #askFor}</li>
 *   <li><b>behaviour</b> — {@link #behave}, {@link #behaveFor}, {@link #currentBehavior},
 *       {@link #behaviorNames}</li>
 *   <li><b>state</b> — {@link #mascots} and friends ({@link MascotState}),
 *       affection ({@link #affectionTier} …), focus ({@link #focusActive} …),
 *       {@link #bubbleVisible}, {@link #charactersHidden}, {@link #language},
 *       {@link #installedImageSets}, {@link #activeImageSets}, {@link #screens}</li>
 *   <li><b>control</b> — {@link #summon}, {@link #dismiss}, {@link #moveTo},
 *       {@link #setCharactersVisible}, {@link #awardAffection} (capped),
 *       {@link #heart}, {@link #playSound}</li>
 *   <li><b>events</b> — {@link #on} / {@link #off} (topics in {@link PluginEvents}; listeners run on
 *       <i>this plugin's</i> own thread); {@link #publish} for a plugin's own topics
 *       ({@code <id>.<topic>})</li>
 *   <li><b>hooks</b> — {@link #hook} / {@link #unhook}: answer a decision before the core acts
 *       (see {@link PluginHooks})</li>
 *   <li><b>settings · prefs · text</b> — {@link #prefs()}: the plugin's <b>own</b> store
 *       ({@code plugindata\<id>\settings.properties}, typed getters, change listener; the values of the
 *       declarative settings card, {@code plugin.json} {@code "settings"}, live there too);
 *       {@link #setting} / {@link #setSetting} read the app's {@code conf/settings.properties}
 *       (app keys are the app's — read them, do not write them); {@link #text} = the plugin's own
 *       five-language files ({@link PluginText}), {@link #localized} = the app's bundle</li>
 *   <li><b>a folder of its own</b> — {@link #dataDir()}</li>
 *   <li><b>UI</b> — {@link #addTrayItem} (a row in the tray menu),
 *       {@link #addCharacterMenuItem} (a row in a character's
 *       menu), {@link #addTrayToggle} / {@link #addCharacterMenuToggle} (a row with a 켜짐/꺼짐 chip);
 *       {@link #addSettingsButton} (a button on the settings window's Information tab);
 *       {@link #addFeatureToggle} (an immediately applied switch at the bottom of the Lumi tab's
 *       「기능 켜고 끄기」 card). Menu rows and settings buttons give back a
 *       {@link PluginUi.MenuHandle} to relabel,
 *       check, disable or remove the row. The 「모드」 tab draws the declarative settings
 *       card; anything richer is the plugin's own window. {@link #claimClickGreeting}</li>
 *   <li><b>input</b> — {@link #bindHotkey} / {@link #unbindHotkey}: a system-wide key
 *       combination, answered with a {@link HotkeyResult}; {@link #normalizeHotkey},
 *       {@link #hotkeyFromKeyEvent} for a settings field</li>
 *   <li><b>scheduling</b> — {@link #every}, {@link #after} ({@link ScheduledHandle}),
 *       {@link #onEdt}, {@link #offEdt}; each plugin has one event thread and one timer thread</li>
 *   <li><b>feature switches</b> — {@link #featureEnabled} (bongo · broadcast · island · game · focus.auto)</li>
 *   <li><b>test</b> — {@link PluginTestKit} hands out a context whose calls are recorded instead of
 *       reaching the app, for headless unit tests of a plugin</li>
 *   <li><b>bytecode</b> — {@link #patch}: retransform a loaded core class (method bodies
 *       only, a JVM rule); {@link #onLoad}: rewrite classes that have not loaded
 *       yet, freely (the engine loads after the plugins start). A transformer that
 *       must see <i>everything</i> ships as a {@link LumiTransformer} service
 *       instead — that one is registered before the app loads.</li>
 *   <li><b>voice</b> — {@link #speak}, {@link #stopSpeaking}, {@link #setSpeechVolume}, {@link #mouthFlap}; <b>look</b> — {@link #theme}, {@link #appIcon}, {@link #manageWindow}; <b>files</b> — {@link #writeAtomic}; <b>more state</b> — {@link #quietNow}, {@link #foregroundScreen}, {@link #persona}, {@link #isCharacter}; <b>one character</b> — {@link #sayTo}, {@link #showBusyFor}; <b>typed hotkeys</b> — {@link #bindHotkey}, {@link #normalizeHotkey}, {@link #hotkeyFromKeyEvent}</li>
 * </ul>
 * The loader releases context-managed event listeners, hooks, hotkeys, UI handles and
 * scheduled tasks after {@code stop()}. It also releases this plugin's click-greeting claim.
 * On failed startup while the app continues, it dismisses tracked summons and restores
 * characters this context left hidden; those two operations are skipped during app shutdown.
 * <p>
 * Plugins must close their own windows, streams, clients and worker threads. Preference
 * listeners registered with {@link PluginPrefs#onChange} are not tracked by this context;
 * remove them with {@link PluginPrefs#removeOnChange}. Transformed classes are not rolled back.
 * <p>
 * After release, most registrations and actions return inert results: cancelled or removed
 * handles, {@link HotkeyResult#REFUSED}, {@code -1} from {@code summon}, or {@code false}
 * from {@code onLoad}/{@code patch}. Rejected {@code ask} calls invoke the callback inline
 * with {@code -2}. Argument validation can still throw: {@code every} checks its task and
 * period before checking release. Reads remain available, but a retained {@code prefs()}
 * reference remains mutable; it is not converted to a read-only store.
 * <p>
 * Plugin enablement changes require a new app launch. {@link #API_VERSION} is the API a plugin
 * declares in {@code plugin.json}: within one API_VERSION stable items are only added, preview
 * items may change without a bump (see the SDK's API.md §8), and a bump keeps plugins written for
 * the previous version (N-1) loading for one more version. {@code PluginContext.API_VERSION} rises
 * only when a stable item is removed or reshaped.
 */
public final class PluginContext {
    public static final int API_VERSION = 1;

    /**
     * Classes a plugin may not retransform: the Steam gate, the AI unlock verdict,
     * the loader, and the Steam stack. A plugin lives on the app's class path and
     * could in principle reach these through other means; this list is where the
     * supported surface stops, and it is checked by {@code PluginLoaderTest}.
     */
    static final String[] PROTECTED_PREFIXES = {
            "com.group_finity.mascot.lumi.SteamGate",
            // 게이트가 스팀 없이 통과할지를 정하는 증표의 서명·판정. 게이트만 잠그고
            // 이것을 열어 두면 게이트를 우회하는 길이 그대로 남는다.
            "com.group_finity.mascot.lumi.OfflineToken",
            "com.group_finity.mascot.lumi.Features",
            "com.group_finity.mascot.lumi.plugin.",
            "com.group_finity.mascot.lumi.steam.",
            // 호감도 저장소와 도전과제: 둘 중 하나를 변환기로
            // 만지면 스팀 도전과제 부정이 된다. 점수를 주고 싶은 플러그인은 상한이 있는
            // awardAffection 을 쓴다.
            "com.group_finity.mascot.lumi.AffectionStore",
            "com.group_finity.mascot.lumi.Achievements",
    };

    private final PluginDescriptor descriptor;
    private final Logger log;
    /**
     * 앱에 손을 대는 자리 전부 — 제품은 {@link RealPluginBackend}, 검사는 {@link PluginTestKit} 의
     * 기록판. 공개 메서드는 이 면에 1:1 로 건넨다.
     */
    private final PluginBackend backend;
    private volatile PluginText text;

    // ── 등록 장부 — stop() 뒤 releaseAll() 이 거둔다 ─────────────────────────
    private record Subscription(String topic, Consumer<Map<String, Object>> listener) {
    }

    private record HookEntry(String hook, PluginHooks.Handler handler) {
    }

    private final List<Subscription> subscriptions = new ArrayList<>();
    private final List<HookEntry> hooks = new ArrayList<>();
    private final List<String> hotkeys = new ArrayList<>();
    private final List<PluginUi.MenuHandle> rows = new ArrayList<>();
    private final List<PluginUi.FeatureToggle> featureToggles = new ArrayList<>();
    private final List<ScheduledHandle> tasks = new ArrayList<>();
    private final List<MicCaptureLease> micCaptures = new ArrayList<>();
    /**
     * {@link #releaseAll} 이 지나갔다 — 이 모니터 아래에서만 읽고 쓴다. 등록 문마다 {@link #refused}
     * 로 먼저 묻고, 백엔드에 건 뒤 {@link #ledger} 로 다시 묻는다: 그 사이에 거둠이 지나갔으면
     * 곧장 되돌린다. 그래서 거두는 도중에 끼어든 등록은 함께 거둬지거나 거절되거나 둘 중 하나다.
     */
    private boolean released;
    /** 거둔 뒤의 등록을 플러그인마다 한 번만 경고하려고. */
    private boolean warnedAfterRelease;
    /** 앱 종료 길에서 거뒀다 — 캐릭터를 보내거나 EDT 를 건드리는 복구는 그때 건너뛴다. */
    private volatile boolean releasedAtShutdown;

    // ── 복구 장부 — 등록이 아니라 상태를 바꾼 것 ──────────────────────────────
    /** {@link #claimClickGreeting} 을 불렀다 — 거둘 때 이 플러그인이 쥔 경우에만 푼다. */
    private boolean clickGreetingClaimed;
    /** {@link #setCharactersVisible} 의 마지막 값; {@code null} = 손댄 적 없음. {@code false} 로 남으면 거둘 때 되돌린다. */
    private Boolean charactersVisibleLast;
    /** {@link #summon} 으로 부른 개체 id — 아직 안 보낸 것을 거둘 때 보낸다. */
    private final List<Integer> summoned = new ArrayList<>();

    PluginContext(PluginDescriptor descriptor) {
        this(descriptor, RealPluginBackend.INSTANCE);
    }

    PluginContext(PluginDescriptor descriptor, PluginBackend backend) {
        this.descriptor = descriptor;
        this.backend = backend;
        this.log = Logger.getLogger("plugin." + descriptor.id);
    }

    public String id() {
        return descriptor.id;
    }

    public PluginDescriptor descriptor() {
        return descriptor;
    }

    /** A logger named after the plugin; lines land in the app's own log file. */
    public Logger log() {
        return log;
    }

    /** {@code plugindata\<id>\} — the plugin's own folder, created on first ask. */
    public Path dataDir() {
        Path dir = backend.dataDir(descriptor.id);
        try {
            Files.createDirectories(dir);
        } catch (Exception e) {
            log.warning("cannot create " + dir + ": " + e);
        }
        return dir;
    }

    // ── speech ──────────────────────────────────────────────────────────────

    /** Shows a bubble on the character that last spoke. {@code ms <= 0} = default length. */
    public void say(String text, long ms) {
        if (refused("say")) {
            return;
        }
        backend.say(0, null, text, ms);
    }

    public void sayFor(String imageSet, String text, long ms) {
        if (refused("sayFor")) {
            return;
        }
        backend.say(0, imageSet, text, ms);
    }

    /** Takes the bubble down now — whatever put it up (the app, a file, another plugin). */
    public void hideBubble() {
        if (refused("hideBubble")) {
            return;
        }
        backend.hideBubble();
    }

    /** The 「···」 thinking bubble (up to 90 s, replaced by the next line). {@code null} = the preferred character. */
    public void showBusy(String imageSet) {
        if (refused("showBusy")) {
            return;
        }
        backend.showBusy(0, imageSet);
    }

    /**
     * Queues a line <b>behind</b> whatever is showing: it appears when the current bubble
     * has gone down (plus a short gap), then the next queued line, in order — ordered
     * guidance (a pomodoro's steps, a tutorial) no longer overwrites itself. {@link #say}
     * keeps its old meaning (show now, replacing) and leaves the queue in place. The queue
     * holds {@link com.group_finity.mascot.lumi.BubbleQueue#MAX} lines; beyond that the
     * oldest is dropped. Each line passes the {@code bubble.text}/{@code bubble.duration}
     * hooks and raises the {@code bubble} event like any other. While a choice bubble is
     * open the queue waits.
     */
    public void sayQueued(String text, long ms) {
        if (refused("sayQueued")) {
            return;
        }
        backend.enqueue(0, null, text, ms, descriptor.id);
    }

    public void sayQueuedFor(String imageSet, String text, long ms) {
        if (refused("sayQueuedFor")) {
            return;
        }
        backend.enqueue(0, imageSet, text, ms, descriptor.id);
    }

    /**
     * Drops this plugin's queued lines (not the one showing — that is {@link #hideBubble}) and its
     * choice bubbles still <b>waiting</b> in line (their {@code onPick} gets {@code -2}); an open
     * choice bubble stays — the user is looking at it.
     */
    public void clearQueue() {
        if (refused("clearQueue")) {
            return;
        }
        backend.clearQueue(descriptor.id);
    }

    /** Lines still waiting in the queue, all plugins together. */
    public int queueLength() {
        return backend.queueLength();
    }

    /**
     * A bubble with buttons — the question and 2 to 4 options
     * ({@link com.group_finity.mascot.lumi.ChoiceBubbleWindow#MAX_OPTIONS}; more are cut).
     * {@code onPick} gets the option index (0-based), {@code -1} = it opened and the user gave no
     * answer (timeout, Escape, the character left) or {@code -2} = <b>the app withdrew it</b> before
     * the user answered (this plugin's newer {@code ask} replaced it, open or waiting; {@link #clearQueue};
     * it waited in line longer than {@link #DEFAULT_ASK_WAIT_MS}; the line was full). {@code timeoutMs} starts when the bubble
     * opens, never while it waits. Normal app callbacks run once on the choice callback executor,
     * off the EDT. A call rejected after context release invokes the callback inline with
     * {@code -2}; the test kit also invokes choice callbacks inline. Keep callbacks brief and
     * use {@link #onEdt} for Swing updates. One choice bubble shows at a time and
     * the rest <b>wait in line</b>: asks from different plugins queue up in order and the next
     * opens when the open one settles; a plugin holds one place in that line — its newer
     * {@code ask} replaces its own open or waiting one (that one gets {@code -2}), never another
     * plugin's. The question passes the {@code bubble.text} hook
     * ({@code source=choice}); the {@code bubble} event fires with {@code kind=choice} and the
     * {@code choice} event carries the answer. {@code timeoutMs <= 0} waits for the user.
     * The bubble does not steal keyboard focus when it opens; Escape works after a click on it.
     */
    public void ask(String text, List<String> options, long timeoutMs, java.util.function.IntConsumer onPick) {
        ask(text, options, timeoutMs, DEFAULT_ASK_WAIT_MS, onPick);
    }

    public void askFor(String imageSet, String text, List<String> options, long timeoutMs,
                       java.util.function.IntConsumer onPick) {
        askFor(imageSet, text, options, timeoutMs, DEFAULT_ASK_WAIT_MS, onPick);
    }

    /**
     * How long an {@code ask} may wait in line before it is withdrawn with {@code -2}, when the
     * four-argument overloads are used: 30 s. Two clocks: {@code timeoutMs} starts when the bubble
     * <b>opens</b> (time spent waiting never counts), {@code maxWaitMs} starts when the ask is made.
     */
    public static final long DEFAULT_ASK_WAIT_MS = 30_000;

    /**
     * {@link #ask(String, List, long, java.util.function.IntConsumer)} with an explicit line limit:
     * an ask still waiting after {@code maxWaitMs} is withdrawn with {@code -2} before it ever opens
     * ({@code <= 0} = {@link #DEFAULT_ASK_WAIT_MS}). {@code -1} always means "opened, the user gave no answer".
     */
    public void ask(String text, List<String> options, long timeoutMs, long maxWaitMs,
                    java.util.function.IntConsumer onPick) {
        askFor(null, text, options, timeoutMs, maxWaitMs, onPick);
    }

    public void askFor(String imageSet, String text, List<String> options, long timeoutMs, long maxWaitMs,
                       java.util.function.IntConsumer onPick) {
        if (refused("ask")) {
            // 죽은 컨텍스트의 표는 뜨지 않는다 — 콜백 약속(한 번, -2 = 앱이 거뒀다)은 지킨다.
            if (onPick != null) {
                try {
                    onPick.accept(-2);
                } catch (Throwable t) {
                    log.log(Level.WARNING, "ask callback failed", t);
                }
            }
            return;
        }
        backend.ask(0, imageSet, text, options, timeoutMs, maxWaitMs, descriptor.id, onPick);
    }

    // ── behaviour ───────────────────────────────────────────────────────────

    /** Makes every mascot start the named behaviour (as {@code behave.txt} does). */
    public void behave(String behaviorName) {
        if (refused("behave(" + behaviorName + ")")) {
            return;
        }
        backend.behaveAll(behaviorName);
    }

    public void behaveFor(String imageSet, String behaviorName) {
        if (refused("behaveFor(" + behaviorName + ")")) {
            return;
        }
        backend.behave(imageSet, behaviorName);
    }

    public String currentBehavior() {
        return backend.currentBehavior(null);
    }

    public String currentBehavior(String imageSet) {
        return backend.currentBehavior(imageSet);
    }

    /**
     * Every behaviour name this image set's {@code behaviors.xml} knows — what {@link #behave}
     * accepts. Empty when the set is not loaded. Meant for building a menu without opening
     * the XML; the names are the conf's own (Korean display names come from the bundle).
     */
    public java.util.Set<String> behaviorNames(String imageSet) {
        return backend.behaviorNames(imageSet);
    }

    // ── state ────────────────────────────────────────────────────────────────
    //
    // 이벤트를 놓쳐도 여기서 지금 상태를 다시 읽을 수 있다.
    // 전부 읽기이고, 엔진이 뜨기 전에는 빈 값·기본값을 돌려준다(예외를 던지지 않는다).
    // 좌표 약속은 MascotState 의 자바독에 — 바탕화면 좌표, Swing 창을 놓는 그 공간.

    /** Every live character (props excluded), desktop coordinates. Empty before the engine is up. */
    public List<MascotState> mascots() {
        return backend.mascots(false);
    }

    /** {@link #mascots()} plus props (the Credit items during a gift, for example). */
    public List<MascotState> mascotsIncludingProps() {
        return backend.mascots(true);
    }

    /** The image set's preferred (first) character, or {@code null}. */
    public MascotState mascot(String imageSet) {
        return backend.mascot(imageSet);
    }

    /** The character with that id ({@link MascotState#id}), or {@code null} once it is gone. */
    public MascotState mascotById(int id) {
        return backend.mascotById(id);
    }

    /** The character whose picture contains the desktop point, or {@code null}. */
    public MascotState mascotAt(java.awt.Point screen) {
        return backend.mascotAt(screen);
    }

    /** Affection tier, 1..6 — what the UI shows. */
    public int affectionTier() {
        return backend.affectionTier();
    }

    /** Progress inside the current tier, 0..100. */
    public int affectionProgressPercent() {
        return backend.affectionProgressPercent();
    }

    /** Points still needed for the next tier; 0 at the top. */
    public int affectionPointsUntilNextTier() {
        return backend.affectionPointsUntilNextTier();
    }

    /**
     * Points a source can still earn today. App sources are the keys of the unlock panel
     * ({@code pat} {@code throwRecover} {@code climb} {@code social} {@code pomodoro} {@code alarm});
     * {@code null} or blank asks about <b>this plugin's own</b> allowance ({@link #awardAffection}).
     */
    public int affectionTodayRemaining(String source) {
        String key = source == null || source.isBlank()
                ? AffectionDirector.pluginSourceKey(descriptor.id) : source;
        return backend.affectionTodayRemaining(key);
    }

    /** Days the user and the characters have spent together (cumulative, not a streak). */
    public int daysTogether() {
        return backend.daysTogether();
    }

    /** Focus mode engaged right now (by any route: manual, pomodoro, auto). */
    public boolean focusActive() {
        return backend.focusActive();
    }

    /** Which routes hold focus mode, comma-separated ({@code manual}, {@code pomodoro}, {@code auto}); "" when none. */
    public String focusSources() {
        return backend.focusSources();
    }

    /** A fullscreen program or a meeting is in front — the app itself keeps quiet then. */
    public boolean screenBusy() {
        return backend.screenBusy();
    }

    /** A bubble is showing on some character. */
    public boolean bubbleVisible() {
        return backend.bubbleVisible();
    }

    /** The characters are hidden (boss key, manual hide or auto-hide). */
    public boolean charactersHidden() {
        return backend.charactersHidden();
    }

    /**
     * The feature switches of the settings window: {@code bongo}, {@code broadcast}, {@code island},
     * {@code game}, {@code focus.auto} ({@link #FEATURE_IDS}). Off when absent, off for an unknown id.
     * Read only — the switches belong to the user.
     */
    public boolean featureEnabled(String feature) {
        String key = FEATURE_IDS.get(feature == null ? "" : feature.strip().toLowerCase(java.util.Locale.ROOT));
        return key != null && backend.featureEnabled(key);
    }

    /** Feature id → the app setting key it reads ({@code featureEnabled}). CATALOG.md lists the ids. */
    static final Map<String, String> FEATURE_IDS = Map.of(
            "bongo", com.group_finity.mascot.lumi.Features.KEY_BONGO,
            "broadcast", com.group_finity.mascot.lumi.Features.KEY_BROADCAST,
            "island", com.group_finity.mascot.lumi.Features.KEY_ISLAND,
            "game", com.group_finity.mascot.lumi.Features.KEY_GAME,
            "focus.auto", com.group_finity.mascot.lumi.Features.KEY_FOCUS_AUTO);

    /**
     * The UI language the app runs in — one of {@code ko} {@code en} {@code ja} {@code zh-CN}
     * {@code zh-TW}. {@link #text} already picks the plugin's own bundle by this.
     */
    public String language() {
        return backend.language();
    }

    /** {@code img\} folders that hold a character (props excluded), name order. */
    public List<String> installedImageSets() {
        return backend.installedImageSets();
    }

    /** The image sets switched on right now ({@code ActiveShimeji}), props included. */
    public List<String> activeImageSets() {
        return backend.activeImageSets();
    }

    /** Every monitor's rectangle, desktop coordinates — the same space as {@link MascotState#bounds}. */
    public List<java.awt.Rectangle> screens() {
        return backend.screens();
    }

    // ── control ──────────────────────────────────────────────────────────────
    //
    // 전부 앱이 이미 하는 일의 문(門)이다 —
    // 부르기는 트레이의 「하나 더」, 보내기는 우클릭 「보내기」, 숨기기는 보스키, 적립은
    // 여느 적립원과 같은 award(). 새 규칙을 만들지 않았다.

    /**
     * One more character of that image set — the tray's 「하나 더 부르기」 (shiny roll included).
     * Only an <b>active</b> set works (see {@link #activeImageSets}); an installed-but-off set
     * returns -1 rather than the error box the engine would raise.
     *
     * @return the new character's id, or -1
     */
    public int summon(String imageSet) {
        if (refused("summon(" + imageSet + ")")) {
            return -1;
        }
        int id = backend.summon(imageSet);
        if (id >= 0) {
            ledger(() -> summoned.add(id), () -> {
                if (!releasedAtShutdown) {
                    backend.dismiss(id);
                }
            });
        }
        return id;
    }

    /** Sends one character away (the menu's 「보내기」, without its sound). False if the id is gone. */
    public boolean dismiss(int mascotId) {
        if (refused("dismiss(" + mascotId + ")")) {
            return false;
        }
        synchronized (this) {
            summoned.remove(Integer.valueOf(mascotId));
        }
        return backend.dismiss(mascotId);
    }

    /**
     * Teleports a character so its anchor (feet) lands on the desktop point. The engine
     * switches it to {@code Fall} first so it settles on whatever is below; false when the
     * switch was refused (nothing moved — try again next tick) or the id is gone.
     */
    public boolean moveTo(int mascotId, int x, int y) {
        if (refused("moveTo(" + mascotId + ")")) {
            return false;
        }
        return backend.moveTo(mascotId, x, y);
    }

    /**
     * Hides every character or brings them back — the boss key's own switch, so a later
     * boss-key press or auto-hide behaves exactly as if the user had done this. Runs on the EDT.
     * A plugin that leaves them hidden when it stops (a failed start) has them shown again by the
     * loader; at the app's own exit nothing is touched.
     */
    public void setCharactersVisible(boolean visible) {
        if (refused("setCharactersVisible(" + visible + ")")) {
            return;
        }
        backend.setCharactersVisible(visible);
        ledger(() -> charactersVisibleLast = visible, () -> {
            if (!visible && !releasedAtShutdown) {
                backend.setCharactersVisible(true);
            }
        });
    }

    /**
     * Credits affection points from this plugin — gifts, quests, mini-game rewards. Goes through
     * the app's one award gate: the {@code affection.award} hook sees it, the plugin's daily cap
     * ({@link AffectionDirector#PLUGIN_CAP} points per plugin per day, whatever {@code source}
     * says) stops it, and it can never subtract. {@code source} is a label for the log.
     *
     * @return the points actually credited (0 at the cap)
     */
    public int awardAffection(String source, int points) {
        if (refused("awardAffection")) {
            return 0;
        }
        return backend.awardAffection(descriptor.id, source, points);
    }

    /** Pops the head-pat heart over a character's head. {@code null} = the preferred character. */
    public void heart(String imageSet) {
        if (refused("heart")) {
            return;
        }
        backend.heart(imageSet);
    }

    /**
     * Plays a one-shot effect sound through the app's own player — repo-relative path such as
     * {@code img/Lumi/sound/jump.wav} or a file inside the mod folder. Honours the user's volume,
     * the per-sound mute list and the {@code sound.effect} hook. False when sounds are off.
     */
    public boolean playSound(String path) {
        if (refused("playSound")) {
            return false;
        }
        return backend.playSound(path);
    }

    /** {@code ko} {@code en} {@code ja} {@code zh-CN} {@code zh-TW} for the app's locale (pure; tested). */
    static String languageCode(java.util.Locale locale) {
        if (locale == null) {
            return "en";
        }
        String lang = locale.getLanguage();
        if ("zh".equals(lang)) {
            String script = locale.getScript();
            String region = locale.getCountry();
            boolean traditional = "Hant".equals(script)
                    || (script.isEmpty() && ("TW".equals(region) || "HK".equals(region) || "MO".equals(region)));
            return traditional ? "zh-TW" : "zh-CN";
        }
        return switch (lang) {
            case "ko", "ja" -> lang;
            default -> "en";
        };
    }

    // ── events ──────────────────────────────────────────────────────────────

    /**
     * Listens to a topic ({@link PluginEvents}). The listener runs on <b>this plugin's own</b>
     * event thread, in order, never on the publisher's thread; a listener that throws is logged
     * and the next event still arrives. Released automatically after {@code stop()}.
     */
    public void on(String topic, Consumer<Map<String, Object>> listener) {
        if (topic == null || listener == null) {
            return;
        }
        if (refused("on(" + topic + ")")) {
            return;
        }
        backend.subscribe(descriptor.id, topic, listener);
        ledger(() -> subscriptions.add(new Subscription(topic, listener)),
                () -> backend.unsubscribe(descriptor.id, topic, listener));
    }

    public void off(String topic, Consumer<Map<String, Object>> listener) {
        synchronized (this) {
            subscriptions.removeIf(s -> s.topic.equals(topic) && s.listener == listener);
        }
        backend.unsubscribe(descriptor.id, topic, listener);
    }

    /**
     * Publishes an event of this plugin's own for other plugins to {@link #on}. The topic is
     * namespaced: {@code publish("score", …)} goes out as {@code <id>.score} (a topic already
     * carrying the prefix is left alone), so the app's topics ({@link PluginEvents}) can never be
     * forged and two plugins never collide. The payload gets {@code from} = this plugin's id;
     * the rest are key/value pairs as in {@link PluginEvents#publish(String, Object...)}.
     * Listeners run on their own plugin's event thread, never on the caller's.
     */
    public void publish(String topic, Object... keyValues) {
        if (refused("publish(" + topic + ")")) {
            return;
        }
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("from", descriptor.id);
        if (keyValues != null) {
            for (int i = 0; i + 1 < keyValues.length; i += 2) {
                Object v = keyValues[i + 1];
                payload.put(String.valueOf(keyValues[i]), v == null ? "" : v);
            }
        }
        backend.publish(namespaced(descriptor.id, topic), payload);
    }

    /** {@code <pluginId>.<topic>} — idempotent, so a caller that already prefixed is unchanged. */
    static String namespaced(String pluginId, String topic) {
        String prefix = pluginId + ".";
        String t = topic == null ? "" : topic.strip();
        return t.startsWith(prefix) ? t : prefix + t;
    }

    // ── hooks ───────────────────────────────────────────────────────────────

    /**
     * Answers a decision the core asks <b>before</b> acting (see {@link PluginHooks} for
     * the rules and {@code docs/spec-plugins.md} for the catalogue). The handler gets
     * the value so far and returns a new one, or {@code null} to pass. Handlers run
     * in Mods-list order and the lower one wins; they run on the caller's thread, so
     * keep them cheap and never block. Registering an id this app does not ask is
     * harmless — it is simply never called (the Mods tab says so).
     */
    public void hook(String hook, PluginHooks.Handler handler) {
        if (hook == null || handler == null) {
            return;
        }
        if (refused("hook(" + hook + ")")) {
            return;
        }
        backend.hook(descriptor.id, hook, handler);
        ledger(() -> hooks.add(new HookEntry(hook, handler)),
                () -> backend.unhook(descriptor.id, hook, handler));
    }

    public void unhook(String hook, PluginHooks.Handler handler) {
        synchronized (this) {
            hooks.removeIf(h -> h.hook.equals(hook) && h.handler == handler);
        }
        backend.unhook(descriptor.id, hook, handler);
    }

    // ── settings · prefs · text ─────────────────────────────────────────────

    /**
     * The plugin's <b>own</b> settings store — {@code plugindata\<id>\settings.properties}.
     * Typed getters and synchronous save attempts; {@code onChange} runs off the
     * EDT on a shared preferences notifier. Unsupported atomic moves fall back to replacement;
     * save failures are logged. Remove preference listeners explicitly in {@code stop()}. The values a user edits in the Mods tab's declarative card ({@code plugin.json}
     * {@code "settings"}, {@link PluginSettingsSchema}) land here, so read them with
     * {@code prefs().get(key, …)} and listen with {@code prefs().onChange(…)}. The schema's
     * defaults are seeded into the store before {@code start()} so a first run reads them too.
     */
    public PluginPrefs prefs() {
        return backend.prefs(descriptor.id);
    }

    /**
     * Reads one key of the app's {@code conf/settings.properties}. App keys are the app's:
     * read them to follow the user's choices (opacity, language, …), but keep your own
     * state in {@link #prefs()}.
     */
    public String setting(String key, String fallback) {
        return backend.setting(key, fallback);
    }

    /**
     * Writes into the app's {@code conf/settings.properties}. Kept for compatibility —
     * by convention a plugin does <b>not</b> change app keys (the user's settings window
     * owns them and a later apply may overwrite yours); own state belongs in
     * {@link #prefs()}. If you must, prefix the key with your id.
     */
    public void setSetting(String key, String value) {
        if (refused("setSetting(" + key + ")")) {
            return;
        }
        backend.setSetting(key, value);
    }

    /** The app's five-language bundle ({@code conf/language*.properties}); {@code fallback} when the key is missing. */
    public String localized(String key, String fallback) {
        return backend.localized(key, fallback);
    }

    /**
     * The plugin's <b>own</b> five-language text: {@code plugin_ko.properties},
     * {@code plugin_en.properties}, {@code plugin_ja}, {@code plugin_zh}, {@code plugin_zh_TW} and the
     * base {@code plugin.properties} at the root of the plugin jar, UTF-8. Picked by
     * {@link #language()}; falls back {@code exact → en → base → app bundle → fallback}
     * (zh-TW never falls to zh — see {@link PluginText}). Read once, cached.
     */
    public String text(String key, String fallback) {
        return text().get(key, fallback);
    }

    /** {@link #text(String, String)} then {@code MessageFormat}: {@code text("hello", "Hi {0}", name)}. */
    public String text(String key, String fallback, Object... args) {
        return text().format(key, fallback, args);
    }

    /** The plugin's text bundle itself, for a class that wants to hold it. */
    public PluginText text() {
        PluginText t = text;
        if (t == null) {
            synchronized (this) {
                if (text == null) {
                    text = new PluginText(descriptor.id, descriptor.jar, backend.textLoader(),
                            backend::language, backend::localized);
                }
                t = text;
            }
        }
        return t;
    }

    // ── UI ──────────────────────────────────────────────────────────────────

    /**
     * Adds a row to the tray menu (below the app's rows, above 「종료」). {@code action} runs on
     * the EDT after the menu closes. The returned {@link PluginUi.MenuHandle} relabels, disables,
     * marks checked or removes the row — ignore it when the row never changes; the next time the
     * menu opens it is drawn the way the handle says.
     */
    public PluginUi.MenuHandle addTrayItem(String label, Runnable action) {
        return remember("addTrayItem(" + label + ")",
                () -> backend.addTrayItem(new PluginUi.TrayItem(descriptor.id, label, action)));
    }

    /**
     * A tray-menu <b>toggle</b> row — the label stays, a 켜짐/꺼짐 chip shows the state (the
     * app's own 「잠깐 숨기기」 grammar). Pressing it flips the state and calls {@code onChange}
     * with the new value on the EDT; {@code setChecked} on the handle changes the chip without
     * calling it.
     */
    public PluginUi.MenuHandle addTrayToggle(String label, boolean initial, Consumer<Boolean> onChange) {
        return remember("addTrayToggle(" + label + ")",
                () -> backend.addTrayItem(new PluginUi.TrayItem(descriptor.id, label, initial, onChange)));
    }

    /**
     * Adds a row to every character's right-click menu. {@code forImageSet} decides
     * per character (image set folder name) whether the row appears; {@code action}
     * gets the image set and the mascot id of the character that was clicked. Returns the
     * row's {@link PluginUi.MenuHandle}.
     */
    public PluginUi.MenuHandle addCharacterMenuItem(String label, java.util.function.Predicate<String> forImageSet,
                                                    java.util.function.BiConsumer<String, Integer> action) {
        return remember("addCharacterMenuItem(" + label + ")",
                () -> backend.addCharacterMenuItem(new PluginUi.CharacterMenuItem(descriptor.id, label,
                        forImageSet == null ? s -> true : forImageSet, action)));
    }

    /**
     * A character-menu toggle row (chip like the app's 「집중 모드 · 켜짐」). The state is one
     * per row, not per character; {@code onChange} gets the new value on the EDT.
     */
    public PluginUi.MenuHandle addCharacterMenuToggle(String label, java.util.function.Predicate<String> forImageSet,
                                                      boolean initial, Consumer<Boolean> onChange) {
        return remember("addCharacterMenuToggle(" + label + ")",
                () -> backend.addCharacterMenuItem(new PluginUi.CharacterMenuItem(descriptor.id, label,
                        forImageSet == null ? s -> true : forImageSet, initial, onChange)));
    }

    /**
     * Adds a button to the settings window's Information tab, beside the app's help buttons.
     * {@code action} runs on the EDT. The window reads the live list each time it opens, so a
     * plugin that is enabled or disabled appears or disappears after the next app start.
     */
    public PluginUi.MenuHandle addSettingsButton(String label, Runnable action) {
        return remember("addSettingsButton(" + label + ")",
                () -> backend.addSettingsButton(new PluginUi.SettingsButton(descriptor.id, label, action)));
    }

    /**
     * Adds an immediately applied switch at the bottom of the Lumi tab's 「기능 켜고 끄기」 card.
     * The window reads {@code state} when it opens. It calls {@code onChange} as the user edits the switch
     * only when the value changed; false rejects the change and restores the checkbox from
     * {@code state}. When {@code dependsOnFeature} (for example {@code bongo}) is off the
     * row is disabled without clearing its check.
     */
    public PluginUi.FeatureToggle addFeatureToggle(String label, String description,
                                                    java.util.function.BooleanSupplier state,
                                                    java.util.function.Predicate<Boolean> onChange,
                                                    String dependsOnFeature) {
        PluginUi.FeatureToggle made = new PluginUi.FeatureToggle(
                descriptor.id, label, description, state, onChange, dependsOnFeature);
        if (refused("addFeatureToggle(" + label + ")")) {
            made.remove(); // 등록된 적 없는 줄 — removed() 가 참인 무해한 손잡이
            return made;
        }
        PluginUi.FeatureToggle toggle = backend.addFeatureToggle(made);
        if (toggle != null) {
            ledger(() -> featureToggles.add(toggle), toggle::remove);
        }
        return toggle;
    }

    /**
     * Tells the app this plugin answers a short click on a character itself, so the
     * app's own click greeting stays quiet (the AI chat does this: its double-click
     * would otherwise collide with a greeting on the first click). Released after
     * {@code stop()} if this plugin still holds the claim.
     */
    public void claimClickGreeting() {
        if (refused("claimClickGreeting")) {
            return;
        }
        backend.claimClickGreeting(descriptor.id);
        ledger(() -> clickGreetingClaimed = true, () -> backend.releaseClickGreeting(descriptor.id));
    }

    /** 관문 → 백엔드 → 장부. 거둔 뒤면 백엔드는 보지 못하고 떼어진 손잡이가 돌아간다. */
    private PluginUi.MenuHandle remember(String what, java.util.function.Supplier<PluginUi.MenuHandle> register) {
        if (refused(what)) {
            return REFUSED_ROW;
        }
        PluginUi.MenuHandle handle = register.get();
        if (handle != null) {
            ledger(() -> rows.add(handle), handle::remove);
        }
        return handle;
    }

    // ── 거둔 뒤의 등록에 돌려주는 무해한 손잡이 ─────────────────────────────

    /** 메뉴에 오른 적 없는 줄: {@code removed()} 가 참, set… 은 아무 일도 않는다. */
    private static final PluginUi.MenuHandle REFUSED_ROW = new PluginUi.MenuHandle() {
        @Override
        public void setLabel(String label) {
        }

        @Override
        public void setEnabled(boolean enabled) {
        }

        @Override
        public void setChecked(Boolean checked) {
        }

        @Override
        public String label() {
            return "";
        }

        @Override
        public boolean enabled() {
            return false;
        }

        @Override
        public Boolean checked() {
            return null;
        }

        @Override
        public void remove() {
        }

        @Override
        public boolean removed() {
            return true;
        }
    };

    /** 예약된 적 없는 작업: 이미 취소된 손잡이. */
    private static final ScheduledHandle REFUSED_TASK = new ScheduledHandle() {
        @Override
        public void cancel() {
        }

        @Override
        public boolean isCancelled() {
            return true;
        }
    };

    /**
     * 등록 문의 관문 — {@link #releaseAll} 이 지나갔으면 참. 첫 번째만 경고하고(플러그인마다 한 줄:
     * {@code <id>: registration after stop() ignored: <what>}) 그 뒤는 FINE 으로 내린다. 던지지
     * 않는다: 부르는 쪽은 실패한 플러그인이 띄운 떠돌이 스레드이고, 거기서 던져 봐야 로그 소음이다.
     */
    private synchronized boolean refused(String what) {
        if (!released) {
            return false;
        }
        String line = descriptor.id + ": registration after stop() ignored: " + what;
        if (warnedAfterRelease) {
            log.fine(line);
        } else {
            warnedAfterRelease = true;
            log.warning(line);
        }
        return true;
    }

    /**
     * 백엔드에 건 뒤의 두 번째 관문. 모니터 아래에서 {@code released} 를 다시 보고 — 아니면 장부에
     * 올리고, 그 사이 {@link #releaseAll} 이 지나갔으면 모니터 밖에서 곧장 {@code undo}. 잠금은
     * 장부 한 줄만 덮는다: 백엔드 호출은 플러그인으로 되돌아올 수 있어 잠근 채 부르지 않는다.
     */
    private void ledger(Runnable add, Runnable undo) {
        synchronized (this) {
            if (!released) {
                add.run();
                return;
            }
        }
        try {
            undo.run();
        } catch (Throwable t) {
            log.log(Level.FINE, "undo of a registration that raced releaseAll failed", t);
        }
    }

    // ── scheduling ───────────────────────────────────────────────────────────
    //
    // 플러그인이 제 Timer 를 들 필요가 없다. 스레드는 앱이 쥔다 — 플러그인마다
    // 이벤트 스레드 하나(리스너·offEdt)와 타이머 스레드 하나(every·after), 둘 다 데몬이고 늦게
    // 만든다. 그래서 느린 작업은 제 플러그인의 다음 작업만 늦추고 남의 것은 안 밀린다. 던지는
    // 작업은 로그만 남기고 주기 작업은 계속 돈다, stop() 뒤에 남은 것은 로더가 끊고 스레드도 내린다.

    /**
     * Runs {@code task} every {@code periodMs} ms, first after {@code initialMs}, on <b>this plugin's
     * timer thread</b> ({@code lumi-plugin-timer-<id>}, never the EDT). Each plugin has one event
     * thread and one timer thread of its own: a slow task delays only that plugin's later tasks,
     * never another plugin's. A run that throws is logged and the next run still comes. Cancel
     * through the handle; anything still scheduled at exit is cancelled after {@code stop()}.
     * @throws IllegalArgumentException if task is null or periodMs is not positive, even after release
     */
    public ScheduledHandle every(long initialMs, long periodMs, Runnable task) {
        if (task == null || periodMs <= 0) {
            throw new IllegalArgumentException("every(): task and a positive period are required");
        }
        if (refused("every(" + periodMs + "ms)")) {
            return REFUSED_TASK;
        }
        return rememberTask(backend.schedule(descriptor.id, initialMs, periodMs, guarded(task), log));
    }

    /**
     * Runs {@code task} once after {@code ms} ms, on this plugin's timer thread.
     * @throws IllegalArgumentException if task is null, even after release
     */
    public ScheduledHandle after(long ms, Runnable task) {
        if (task == null) {
            throw new IllegalArgumentException("after(): task is required");
        }
        if (refused("after(" + ms + "ms)")) {
            return REFUSED_TASK;
        }
        return rememberTask(backend.schedule(descriptor.id, ms, 0, guarded(task), log));
    }

    /** Runs {@code task} on the Swing event thread (where windows and menus must be touched). */
    public void onEdt(Runnable task) {
        if (task != null && !refused("onEdt")) {
            backend.onEdt(guarded(task));
        }
    }

    /** Runs {@code task} on this plugin's own event thread — off the EDT, in order with its listeners. */
    public void offEdt(Runnable task) {
        if (task != null && !refused("offEdt")) {
            backend.offEdt(descriptor.id, guarded(task));
        }
    }

    private Runnable guarded(Runnable task) {
        return () -> {
            try {
                task.run();
            } catch (Throwable t) {
                log.log(Level.WARNING, "plugin task failed", t);
            }
        };
    }

    private ScheduledHandle rememberTask(ScheduledHandle handle) {
        if (handle == null) {
            return REFUSED_TASK;
        }
        ledger(() -> {
            tasks.removeIf(ScheduledHandle::isCancelled);
            tasks.add(handle);
        }, handle::cancel);
        return handle;
    }

    // ── 생애 — stop() 뒤 로더가 거둔다, 그 뒤의 등록은 거절한다 ────────────────

    /**
     * 이 컨텍스트로 등록한 것 전부를 거둔다 — 리스너·훅·단축키·메뉴 줄·예약 작업 — 그리고 바꾼
     * 상태를 되돌린다 — 클릭 인사 점유(이 플러그인이 쥔 경우만), 부른 개체(아직 안 보낸 것),
     * 숨겨 둔 캐릭터(마지막 값이 {@code false} 면 다시 보인다). {@code stop()} 이 돌아오거나 던진 뒤
     * {@link PluginLoader} 가 부른다({@link PluginTestKit#close} 도). 두 번 불러도 안전. 변환기는
     * 거두지 않는다 — 이미 바뀐 클래스는 되돌릴 수 없다.
     * <p>
     * 그 뒤로 이 컨텍스트는 <b>죽은 것</b>이다: 늦게 오는 등록은 {@link #refused} 가 막고
     * ({@code <id>: registration after stop() ignored: <what>}, 플러그인마다 한 줄), 거두는 도중에
     * 백엔드까지 갔던 등록은 {@link #ledger} 가 곧장 되돌린다 — 실패한 플러그인이 띄운 스레드가
     * 5초 뒤 건 타이머·트레이 줄·훅이 앱 종료까지 살던 것.
     *
     * @param atShutdown 앱 종료 길({@code PluginLoader.stopAll} 의 종료 훅)에서 부른다 — 개체를 보내거나
     *                   캐릭터를 다시 보이는 EDT 복구는 건너뛴다(종료 중의 EDT 는 멈춰 있을 수 있어
     *                   끝내기를 매달 수 있다); 등록물과 클릭 인사 점유는 그때도 거둔다
     */
    void releaseAll(boolean atShutdown) {
        List<Subscription> subs;
        List<HookEntry> hks;
        List<String> keys;
        List<PluginUi.MenuHandle> rws;
        List<PluginUi.FeatureToggle> tgs;
        List<ScheduledHandle> tks;
        List<MicCaptureLease> captures;
        boolean claim;
        boolean showAgain;
        List<Integer> ids;
        // 모니터 아래에서는 표식과 장부만 — 백엔드는 밖에서 부른다(플러그인으로 되돌아올 수 있다).
        synchronized (this) {
            if (released) {
                return;
            }
            released = true;
            releasedAtShutdown = atShutdown;
            subs = List.copyOf(subscriptions);
            subscriptions.clear();
            hks = List.copyOf(hooks);
            hooks.clear();
            keys = List.copyOf(hotkeys);
            hotkeys.clear();
            rws = List.copyOf(rows);
            rows.clear();
            tgs = List.copyOf(featureToggles);
            featureToggles.clear();
            tks = List.copyOf(tasks);
            tasks.clear();
            captures = List.copyOf(micCaptures);
            micCaptures.clear();
            claim = clickGreetingClaimed;
            clickGreetingClaimed = false;
            showAgain = Boolean.FALSE.equals(charactersVisibleLast);
            charactersVisibleLast = null;
            ids = List.copyOf(summoned);
            summoned.clear();
        }
        // Close microphone holds outside this monitor, including acquisitions still in flight.
        for (MicCaptureLease lease : captures) {
            quietly(lease::close);
        }
        // 거두는 중의 예외는 다음 항목을 막지 않는다
        for (Subscription s : subs) {
            quietly(() -> backend.unsubscribe(descriptor.id, s.topic, s.listener));
        }
        for (HookEntry h : hks) {
            quietly(() -> backend.unhook(descriptor.id, h.hook, h.handler));
        }
        for (String key : keys) {
            quietly(() -> backend.unbindHotkey(key));
        }
        for (PluginUi.MenuHandle row : rws) {
            quietly(row::remove);
        }
        for (PluginUi.FeatureToggle toggle : tgs) {
            quietly(toggle::remove);
        }
        for (ScheduledHandle task : tks) {
            quietly(task::cancel);
        }
        if (claim) {
            quietly(() -> backend.releaseClickGreeting(descriptor.id));
        }
        if (!atShutdown) {
            for (int id : ids) {
                quietly(() -> backend.dismiss(id));
            }
            if (showAgain) {
                quietly(() -> backend.setCharactersVisible(true));
            }
        }
        quietly(() -> backend.releaseTimers(descriptor.id));
    }

    /** {@link #releaseAll(boolean)} — 앱이 계속 도는 길(시작 실패, 검사 킷의 {@code close}). */
    void releaseAll() {
        releaseAll(false);
    }

    private void quietly(Runnable step) {
        try {
            step.run();
        } catch (Throwable t) {
            log.log(Level.FINE, "release step failed", t);
        }
    }

    /** 검사용 — 아직 거두지 않은 등록 수(리스너+훅+단축키+줄+작업). */
    synchronized int registeredCount() {
        return subscriptions.size() + hooks.size() + hotkeys.size() + rows.size()
                + featureToggles.size() + tasks.size()
                + (int) micCaptures.stream().filter(lease -> !lease.isClosed()).count();
    }

    // ── LUMI Chat 을 약속 API 위로 옮기며 뚫은 자리 ────────────────────────────

    /** The 「···」 thinking bubble above <b>one</b> character ({@link MascotState#id}); {@code 0} = the set's first. */
    public void showBusyFor(int mascotId, String imageSet) {
        if (refused("showBusyFor")) {
            return;
        }
        backend.showBusy(mascotId, imageSet);
    }

    /**
     * A bubble above <b>one specific</b> character ({@link MascotState#id}). When two characters share an
     * image set, {@link #sayFor} puts the bubble on the first one — the wrong one when the second was
     * the one spoken to. Falls back to the set's preferred character if that id is gone.
     */
    public void sayTo(int mascotId, String imageSet, String text, long ms) {
        if (refused("sayTo")) {
            return;
        }
        backend.say(mascotId, imageSet, text, ms);
    }

    /**
     * Is now a time to keep quiet? Characters hidden, a fullscreen app in front, focus mode on —
     * and the {@code focus.quiet} hook's answer on top. The app's own self-talk, greetings and
     * celebrations all ask this one question; a plugin that speaks up on its own should too.
     */
    public boolean quietNow() {
        return backend.quietNow();
    }

    /**
     * The monitor the <b>foreground window</b> is on, as an AWT rectangle ({@link java.awt.Robot}
     * coordinates), or {@code null} when there is no foreground window or it touches no monitor.
     * {@link #screens()} cannot answer this: it does not know which window the user is working in.
     */
    public java.awt.Rectangle foregroundScreen() {
        return backend.foregroundScreen();
    }

    /** Is this image set a character (not a prop such as the credit items)? */
    public boolean isCharacter(String imageSet) {
        return backend.isCharacter(imageSet);
    }

    /**
     * The character's persona text — identity, voice, taboos — from {@code img\<Set>\persona.txt}
     * (Lumi falls back to the app's own copy). Empty when the character has none: write your own
     * stand-in. Output-format rules are <b>not</b> in it; every consumer appends its own.
     */
    public String persona(String imageSet) {
        return backend.persona(imageSet);
    }

    /** Where {@link #persona} reads from, so a settings window can let the user edit it. */
    public Path personaFile(String imageSet) {
        return backend.personaFile(imageSet);
    }

    // ── voice ───────────────────────────────────────────────────────────────

    /**
     * Plays PCM audio through the app's voice line — one line for everyone, so a new call cuts
     * whatever was still speaking and voice never overlaps with a voice pack. The volume is the
     * shared voice volume ({@link #setSpeechVolume}). {@code onStart}/{@code onEnd} run on the
     * player thread; the {@code tts} event fires around it. Calls suppressed by a microphone
     * lease or made after this context's shutdown are discarded without playback callbacks.
     */
    public synchronized void speak(PcmAudio audio, Runnable onStart, Runnable onEnd) {
        if (refused("speak")) {
            return;
        }
        backend.speak(audio, onStart, onEnd);
    }

    /**
     * Stops current LUMI voice output and waits until its audio line has closed, then keeps all
     * new voice output (voice packs, previews and every plugin) suppressed until the returned
     * lease closes. Suppressed or previously queued voices are discarded, never replayed.
     * Acquire <b>off the EDT</b>, before opening the microphone; close the microphone before
     * closing this lease. Nested leases are independent and this context releases its own on
     * shutdown, including acquisitions still waiting for playback to stop.
     *
     * @throws IllegalStateException on the EDT, after plugin shutdown, if interrupted, or if
     *         the output line cannot be closed. An unsuccessful call must not start recording.
     */
    public MicCaptureLease beginMicCapture() {
        if (javax.swing.SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("Microphone capture must begin off the EDT");
        }
        MicCaptureLease lease = new MicCaptureLease();
        synchronized (this) {
            if (released) {
                throw new IllegalStateException("Plugin context has been released");
            }
            micCaptures.removeIf(MicCaptureLease::isClosed);
            micCaptures.add(lease);
        }
        try {
            backend.beginMicCapture(descriptor.id, lease);
            synchronized (this) {
                if (released || lease.isClosed()) {
                    throw new IllegalStateException("Microphone capture was cancelled during shutdown");
                }
            }
            return lease;
        } catch (RuntimeException | Error e) {
            lease.close();
            synchronized (this) {
                micCaptures.remove(lease);
            }
            throw e;
        }
    }

    /** Cuts the current voice line. */
    public void stopSpeaking() {
        if (refused("stopSpeaking")) {
            return;
        }
        backend.stopSpeaking();
    }

    /** Is the voice line playing right now? */
    public boolean isSpeaking() {
        return backend.isSpeaking();
    }

    /** The shared voice volume knob (0–100). One knob for every plugin and voice pack — set it from a setting the user sees. */
    public void setSpeechVolume(int percent) {
        if (refused("setSpeechVolume")) {
            return;
        }
        backend.setSpeechVolume(percent);
    }

    /**
     * Moves the character's mouth while {@code speaking} says true (checked about once a second,
     * the {@code Talk} behaviour re-run each time), then stops on its own. Pair it with
     * {@link #speak}: {@code ctx.mouthFlap(set, ctx::isSpeaking)}.
     */
    public void mouthFlap(String imageSet, java.util.function.BooleanSupplier speaking) {
        if (refused("mouthFlap")) {
            return;
        }
        backend.mouthFlap(imageSet, speaking);
    }

    /** Mouth movement for a fixed time — for a line whose audio you do not play yourself. */
    public void mouthFlapFor(String imageSet, long millis) {
        if (refused("mouthFlapFor")) {
            return;
        }
        backend.mouthFlapFor(imageSet, millis);
    }

    public void stopMouthFlap() {
        if (refused("stopMouthFlap")) {
            return;
        }
        backend.stopMouthFlap();
    }

    // ── files ───────────────────────────────────────────────────────────────

    /**
     * Writes through a temporary file in the target directory. The shipped backend attempts
     * an atomic move, but falls back to a regular replacement when atomic moves are unsupported.
     * With {@code ownerOnly}, it attempts an owner/SYSTEM ACL after writing the temporary file
     * and before moving it. Missing ACL support or a failed ACL update does not guarantee
     * restricted access: this is neither encryption nor a guaranteed secret store.
     * The method does not constrain {@code target} to the plugin data directory.
     *
     * @throws java.io.IOException if writing or moving the file fails; ACL failures may only be logged
     */
    public void writeAtomic(Path target, byte[] content, boolean ownerOnly) throws java.io.IOException {
        if (refused("writeAtomic(" + target + ")")) {
            return;
        }
        backend.writeAtomic(target, content, ownerOnly);
    }

    public void writeAtomic(Path target, String content, boolean ownerOnly) throws java.io.IOException {
        if (refused("writeAtomic(" + target + ")")) {
            return;
        }
        backend.writeAtomic(target, (content == null ? "" : content).getBytes(java.nio.charset.StandardCharsets.UTF_8), ownerOnly);
    }

    // ── look ────────────────────────────────────────────────────────────────

    /** The app's look for a plugin's own windows — colours, the CJK-capable font, button/toggle/slider dress, cards. */
    public PluginTheme theme() {
        return backend.theme();
    }

    /** The app icon, for an owner-less dialog's title bar. */
    public java.awt.Image appIcon() {
        return backend.appIcon();
    }

    /**
     * Places the window where the user last left it (per {@code key}, per plugin) and remembers
     * where it ends up — centred on the active monitor the first time, never off-screen.
     * Call after the window has its final size; idempotent, so call it on every open.
     */
    public void manageWindow(java.awt.Window window, String key) {
        if (refused("manageWindow(" + key + ")")) {
            return;
        }
        backend.manageWindow(window, "plugin." + descriptor.id + "." + (key == null ? "window" : key));
    }

    // ── input ───────────────────────────────────────────────────────────────

    /**
     * Binds a <b>system-wide</b> key combination, heard even while another program is
     * in front. {@code combination} is written {@code Win+Ctrl+Alt+Shift+key}
     * and needs at least one modifier — a bare key registered globally would be taken
     * away from every other program. A blank combination means "no hotkey" and simply
     * releases whatever this {@code name} held, which is how a settings window rebinds
     * one. {@code action} runs on the EDT.
     * <p>
     * The plugin's id prefixes the registration name. Actual key combinations can still collide;
     * the {@link HotkeyResult} says <i>why</i> nothing was bound (a bad combination, or
     * Windows refusing because another program already holds it) so a settings window
     * can tell the user. Registering costs nothing until the first call: the app has no
     * hotkey thread until a plugin asks for one.
     *
     * @param name this plugin's own name for the binding — call again with the same
     *             name to swap the combination
     */
    public HotkeyResult bindHotkey(String name, String combination, Runnable action) {
        String key = descriptor.id + ":" + name;
        if (refused("bindHotkey(" + name + ")")) {
            // 빈 조합 = 「지정 없음」 — 이전 등록은 거둠이 이미 풀었으니 그 답이 맞다.
            return combination == null || combination.isBlank() ? HotkeyResult.NONE : HotkeyResult.REFUSED;
        }
        HotkeyResult result = backend.bindHotkey(key, combination, action);
        ledger(() -> {
            if (!hotkeys.contains(key)) {
                hotkeys.add(key);
            }
        }, () -> backend.unbindHotkey(key));
        return result;
    }

    /** Releases one binding made by {@link #bindHotkey}. */
    public void unbindHotkey(String name) {
        String key = descriptor.id + ":" + name;
        synchronized (this) {
            hotkeys.remove(key);
        }
        backend.unbindHotkey(key);
    }

    /** The canonical spelling of a combination ({@code Ctrl+Shift+L}), or {@code null} when it is not one. */
    public String normalizeHotkey(String combination) {
        return backend.normalizeHotkey(combination);
    }

    /** A key press as a combination string for a hotkey field, or {@code null} for a bare key / a modifier alone. */
    public String hotkeyFromKeyEvent(int awtKeyCode, int awtModifiersEx) {
        return backend.hotkeyFromKeyEvent(awtKeyCode, awtModifiersEx);
    }

    // ── bytecode ────────────────────────────────────────────────────────────

    /**
     * Registers a transformer for classes that have <b>not loaded yet</b> whose
     * dotted name starts with {@code classNamePrefix} — the stock engine and the
     * mascot classes load after {@code start()}, so a plugin can rewrite them here
     * without the retransform limits of {@link #patch} (new methods and fields are
     * fine). Protected classes are never offered; a transformer that throws is
     * logged and the class loads unchanged. Returns false without instrumentation.
     */
    public boolean onLoad(String classNamePrefix, LumiTransformer transformer) {
        if (transformer == null || refused("onLoad(" + classNamePrefix + ")")) {
            return false;
        }
        Instrumentation inst = PluginAgent.instrumentation();
        if (inst == null) {
            log.warning("no instrumentation — cannot register a load-time transformer");
            return false;
        }
        String prefix = classNamePrefix == null ? "" : classNamePrefix;
        LumiTransformer scoped = new LumiTransformer() {
            @Override
            public boolean wants(String className) {
                return className.startsWith(prefix) && transformer.wants(className);
            }

            @Override
            public byte[] transform(String className, byte[] classfile) {
                return transformer.transform(className, classfile);
            }
        };
        inst.addTransformer(PluginLoader.guard(scoped, descriptor.id), false);
        log.info("load-time transformer registered for " + (prefix.isEmpty() ? "*" : prefix + "*"));
        return true;
    }

    /**
     * Retransforms one already-loaded class through the given transformer — the
     * Harmony-shaped escape hatch for what no hook reaches. Returns {@code false}
     * when the class is protected, not loaded yet, or the JVM has no
     * instrumentation (a {@code -cp} run). The transformer stays registered, so
     * it also sees the class if it is ever redefined again.
     */
    public boolean patch(String className, ClassFileTransformer transformer) {
        if (transformer == null || refused("patch(" + className + ")")) {
            return false;
        }
        if (isProtected(className)) {
            log.warning("refused to patch protected class " + className);
            return false;
        }
        Instrumentation inst = PluginAgent.instrumentation();
        if (inst == null || !inst.isRetransformClassesSupported()) {
            log.warning("no instrumentation — cannot patch " + className);
            return false;
        }
        Class<?> target = null;
        for (Class<?> c : inst.getAllLoadedClasses()) {
            if (c.getName().equals(className) && inst.isModifiableClass(c)) {
                target = c;
                break;
            }
        }
        if (target == null) {
            log.warning("class not loaded or not modifiable: " + className);
            return false;
        }
        try {
            inst.addTransformer(transformer, true);
            inst.retransformClasses(target);
            log.info("patched " + className);
            return true;
        } catch (Throwable t) {
            log.warning("patch of " + className + " failed: " + t);
            return false;
        }
    }

    static boolean isProtected(String className) {
        for (String prefix : PROTECTED_PREFIXES) {
            if (className.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
