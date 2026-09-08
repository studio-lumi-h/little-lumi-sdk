package com.group_finity.mascot.lumi.plugin;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The semantic hooks plugins subscribe to — a topic bus the core publishes into
 * from a handful of places (behaviour changes, bubbles, the ready moment).
 * <p>
 * Every listener runs on a daemon thread, never on the thread that published.
 * That is not a nicety: {@code Mascot.setBehavior} publishes from the EDT and from
 * the ticker while holding engine locks, and a listener that blocked there would
 * freeze every mascot (the ABBA deadlock documented in {@code Mascot.setBehavior}).
 * <b>Each plugin has a thread of its own</b>
 * ({@code lumi-plugin-events-<id>}): a slow listener delays only its own plugin's
 * later events, never another plugin's. Within one plugin events keep their order.
 * A listener that throws is logged and skipped.
 * <p>
 * Topics and their payload keys (all values are strings unless said otherwise):
 * <ul>
 *   <li>{@code behavior} — {@code imageSet}, {@code name}, {@code mascotId} (Integer)</li>
 *   <li>{@code bubble} — a bubble went up: {@code imageSet}, {@code mascotId} (Integer, 0 = by image
 *       set), {@code text} (<b>as shown</b> — after the {@code bubble.text} hook), {@code ms} (Long, the
 *       final duration after {@code bubble.duration}), {@code kind} ({@code say} = a plain line incl.
 *       queued lines and {@code say.txt}, {@code private} = an alarm/errand line the broadcast stage
 *       skips, {@code choice} = a bubble with buttons opened; then {@code ms} is its timeout). The
 *       「···」 busy indicator raises no event</li>
 *   <li>{@code choice} — a choice bubble was answered: {@code imageSet}, {@code mascotId} (Integer),
 *       {@code index} (Integer, 0-based option, -1 = opened, the user gave no answer: timeout/Escape/character gone, -2 = withdrawn by the app: superseded by the same plugin, clearQueue, waited too long, line full),
 *       {@code option} (the label, "" when negative), {@code from} (plugin id that asked)</li>
 *   <li>{@code ready} — the engine is up and the plugins have started; no keys</li>
 *   <li>{@code click} — a mouse button went down on a character: {@code imageSet},
 *       {@code mascotId} (Integer), {@code button} ({@code left}/{@code right}/{@code middle}),
 *       {@code x}, {@code y} (Integer, screen coordinates)</li>
 *   <li>{@code mascot} — a character appeared or left: {@code imageSet}, {@code mascotId}
 *       (Integer), {@code event} ({@code added}/{@code removed})</li>
 *   <li>{@code headpat} — a head-pat began: {@code behavior}</li>
 *   <li>{@code affection} — the shown affection tier changed: {@code tier}, {@code previous},
 *       {@code points} (Integer)</li>
 *   <li>{@code achievement} — a Steam achievement unlocked this run: {@code id}</li>
 *   <li>{@code focus} — focus mode engaged or released: {@code active} (Boolean)</li>
 *   <li>{@code chat} — AI chat traffic: {@code phase} ({@code ask}/{@code reply}),
 *       {@code imageSet}, {@code text}</li>
 *   <li>{@code tts} — a voice line started or ended: {@code phase} ({@code start}/{@code end})</li>
 *   <li>{@code settings} — the settings window was applied: {@code phase} ({@code apply}),
 *       {@code changed} (comma-joined keys written through {@code LumiSettings.set} since the
 *       previous apply; stock engine keys are not journaled); or the language was switched:
 *       {@code phase} ({@code language}), {@code language} (BCP-47 tag)</li>
 *   <li>{@code ask} — {@code speech\ask.txt} was written: {@code text} (the AI plugin answers it)</li>
 *   <li>{@code settings_open} — {@code speech\settings_open.txt} was written; no keys</li>
 *   <li>{@code say_file} — a {@code speech\say.txt} line was shown: {@code text}</li>
 *   <li>{@code drag} — the user picked a character up or let go ({@code Dragged} began/ended):
 *       {@code phase} ({@code start}/{@code end}), {@code imageSet}, {@code mascotId} (Integer),
 *       {@code x}, {@code y} (Integer, screen coordinates of the anchor)</li>
 *   <li>{@code throw} — a character was let go with momentum ({@code Thrown} began) or that
 *       flight ended (the next behaviour began): {@code phase} ({@code thrown}/{@code landed}),
 *       {@code imageSet}, {@code mascotId} (Integer), {@code x}, {@code y} (Integer),
 *       {@code next} (always present — the behaviour after the flight on {@code landed},
 *       {@code ""} on {@code thrown})</li>
 *   <li>{@code hover} — the pointer entered or left a character's window: {@code phase}
 *       ({@code enter}/{@code exit}), {@code imageSet}, {@code mascotId} (Integer)</li>
 *   <li>{@code doubleclick} — two left presses on the same character within
 *       {@link DoubleClickGate#WINDOW_MS} ms and {@link DoubleClickGate#RADIUS_PX} px (the AWT click
 *       count cannot be trusted on a mascot, see {@code DoubleClickWatcher}): {@code imageSet},
 *       {@code mascotId} (Integer), {@code x}, {@code y} (Integer). Each such pair also
 *       produced two {@code click} events</li>
 *   <li>{@code alarm} — a reminder was delivered to the user: {@code kind} ({@code hourly}/{@code alarm}),
 *       {@code time} ({@code HH:mm} of the chime or the alarm the user set), {@code late} (Boolean —
 *       an alarm held back while the screen was busy)</li>
 *   <li>{@code pomodoro} — the focus/break cycle moved: {@code phase} ({@code focus}/{@code break}/
 *       {@code stopped}), {@code minutes} (Integer, length of the phase that begins; 0 on stop),
 *       {@code round} (Integer, focus phases started since the cycle was switched on)</li>
 *   <li>{@code bongo} — bongo mode drew a keystroke or click: {@code kind} ({@code keyboard}/
 *       {@code left_click}/{@code right_click}), {@code frame} (Integer, the hand-slot index drawn),
 *       {@code look} ({@code bongo}/{@code small}). <b>Never a key code or character</b> — the
 *       core does not read keys ({@code docs/spec-bongo.md} §3)</li>
 *   <li>{@code app} — the app is going down through its own exit door: {@code phase} ({@code exit}).
 *       Published before the plugins' {@code stop()} and flushed for a short bounded wait</li>
 *   <li>{@code island} — the garden (island) window process was asked to start or stop, or served
 *       a photo request: {@code phase} ({@code start}/{@code stop}/{@code photo}), {@code request}
 *       (photo only, the {@code photo_request.txt} body)</li>
 *   <li>{@code game} — the contraption game board opened, started a run or closed:
 *       {@code phase} ({@code start}/{@code run}/{@code stop})</li>
 * </ul>
 * There is no per-tick position topic on purpose: a plugin that wants positions polls
 * {@code PluginContext.mascots()}.
 * <p>
 * Publishers call {@link #publish(String, Object...)} with key/value pairs; the map
 * form is for callers that already hold one.
 */
public final class PluginEvents {
    private static final Logger log = Logger.getLogger(PluginEvents.class.getName());

    /** 구독 한 건 — 어느 플러그인의 리스너인가(빈 문자열 = 주인 없음, 옛 2-인자 subscribe). */
    record Sub(String pluginId, Consumer<Map<String, Object>> listener) {
    }

    private static final Map<String, List<Sub>> listeners = new ConcurrentHashMap<>();
    private static volatile boolean any;
    /** 플러그인 id → 그 플러그인의 전용 배달 스레드. 늦게 만든다(플러그인 없는 빌드는 0 스레드). */
    private static final Map<String, ExecutorService> executors = new ConcurrentHashMap<>();

    private PluginEvents() {
    }

    /** 주인 없는 구독 — 코어 안의 리스너와 플러그인을 밝히지 않은 호출자. 배달 스레드는 {@code lumi-plugin-events}. */
    public static void subscribe(String topic, Consumer<Map<String, Object>> listener) {
        subscribe("", topic, listener);
    }

    /**
     * 어느 플러그인의 구독인지 밝혀 건다 — 그 플러그인의 전용 스레드에서 배달된다
     * ({@link PluginContext#on} 이 부른다).
     */
    public static void subscribe(String pluginId, String topic, Consumer<Map<String, Object>> listener) {
        if (topic == null || listener == null) {
            return;
        }
        listeners.computeIfAbsent(topic, k -> new CopyOnWriteArrayList<>())
                .add(new Sub(pluginId == null ? "" : pluginId, listener));
        any = true;
    }

    public static void unsubscribe(String topic, Consumer<Map<String, Object>> listener) {
        List<Sub> list = topic == null ? null : listeners.get(topic);
        if (list != null) {
            list.removeIf(sub -> sub.listener == listener);
        }
    }

    /** 한 플러그인의 구독을 전부 거둔다 — {@code stop()} 뒤 {@code PluginContext.releaseAll} 이 부른다. */
    static void unsubscribeAll(String pluginId) {
        if (pluginId == null || pluginId.isEmpty()) {
            return;
        }
        for (List<Sub> list : listeners.values()) {
            list.removeIf(sub -> pluginId.equals(sub.pluginId));
        }
    }

    /** Cheap test for the publishers, so a build with no plugins pays nothing. */
    public static boolean hasListeners() {
        return any;
    }

    /** {@code publish("click", "button", "left", "x", 10)} — pairs of key, value. Nulls become "". */
    public static void publish(String topic, Object... keyValues) {
        if (!any) {
            return;
        }
        Map<String, Object> payload = new java.util.HashMap<>();
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            Object v = keyValues[i + 1];
            payload.put(String.valueOf(keyValues[i]), v == null ? "" : v);
        }
        publish(topic, payload);
    }

    public static void publish(String topic, Map<String, Object> payload) {
        if (!any) {
            return;
        }
        List<Sub> list = listeners.get(topic);
        if (list == null || list.isEmpty()) {
            return;
        }
        Map<String, Object> copy = Map.copyOf(payload);
        // 플러그인별로 묶어 각자의 스레드에 — 한 플러그인 안에서는 등록 순서가 지켜진다.
        Map<String, List<Consumer<Map<String, Object>>>> byPlugin = new java.util.LinkedHashMap<>();
        for (Sub sub : list) {
            byPlugin.computeIfAbsent(sub.pluginId, k -> new java.util.ArrayList<>()).add(sub.listener);
        }
        for (Map.Entry<String, List<Consumer<Map<String, Object>>>> e : byPlugin.entrySet()) {
            List<Consumer<Map<String, Object>>> mine = e.getValue();
            executorFor(e.getKey()).execute(() -> {
                for (Consumer<Map<String, Object>> listener : mine) {
                    try {
                        listener.accept(copy);
                    } catch (Throwable t) {
                        log.log(Level.WARNING, "Plugin " + (e.getKey().isEmpty() ? "" : e.getKey() + " ")
                                + "listener on '" + topic + "' failed", t);
                    }
                }
            });
        }
    }

    /**
     * 지금까지 넣은 이벤트가 리스너에게 <b>다 건네질 때까지</b> 기다린다 — 늦어도 {@code maxMs}.
     * <p>
     * 쓰는 자리는 하나, {@code Main.exit} 의 {@code app} 이벤트다. 버스는 데몬 스레드로 돌아
     * {@code System.exit} 가 그 스레드를 그대로 끊으므로, 「내려간다」는 통지를 넣고 바로 내려가면
     * 아무도 못 듣는다. 기다림은 <b>스레드마다 표식 작업 하나</b>가 큐를 빠져나오는 것으로 재고
     * (리스너가 느려도 상한이 있다), 리스너가 없으면 즉시 돌아온다.
     */
    public static void flush(long maxMs) {
        List<ExecutorService> all = new java.util.ArrayList<>(executors.values());
        if (all.isEmpty()) {
            return;
        }
        java.util.concurrent.CountDownLatch drained = new java.util.concurrent.CountDownLatch(all.size());
        for (ExecutorService e : all) {
            try {
                e.execute(drained::countDown);
            } catch (RuntimeException rejected) {
                // 종료 중에 큐가 닫혔다 — 그 스레드는 기다릴 것이 없다.
                drained.countDown();
            }
        }
        try {
            drained.await(Math.max(0, maxMs), java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    /** 그 플러그인의 배달 스레드 — {@link PluginContext#offEdt} 도 여기에 얹는다. */
    static ExecutorService executorFor(String pluginId) {
        String id = pluginId == null ? "" : pluginId;
        return executors.computeIfAbsent(id, k -> Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, k.isEmpty() ? "lumi-plugin-events" : "lumi-plugin-events-" + k);
            t.setDaemon(true);
            return t;
        }));
    }

    /**
     * {@code doubleclick} 판정 — 같은 캐릭터 위의 왼쪽 누름 둘이 {@link #WINDOW_MS} 안,
     * {@link #RADIUS_PX} 안에 오면 두 번째 누름이 더블클릭이다.
     * <p>
     * AWT 의 {@code clickCount} 를 믿지 못하는 이유는 CLAUDE.md 「Gotchas」와 LUMI Chat 의
     * {@code DoubleClickWatcher} 에 있다: 누르는 순간 엔진이 들기를 시작해 창이 커서 밑에서
     * 움직이므로 툴킷이 두 번째 클릭을 합치지 못한다. 그 판정을 플러그인마다 다시 쓰지 않도록
     * 코어가 {@code click} 을 내는 자리에서 한 번 세어 준다. 순수 상태 기계라 검사가 시계를
     * 직접 준다.
     */
    public static final class DoubleClickGate {
        public static final long WINDOW_MS = 600;
        public static final int RADIUS_PX = 90;

        private int lastMascot;
        private int lastX;
        private int lastY;
        private long lastAt = Long.MIN_VALUE / 2;

        /** 이 누름이 더블클릭의 두 번째인가. 참이면 기록을 비워 세 번째가 새 첫 번째가 된다. */
        public synchronized boolean press(int mascotId, int x, int y, long now) {
            boolean second = mascotId == lastMascot
                    && now - lastAt <= WINDOW_MS
                    && Math.abs(x - lastX) <= RADIUS_PX
                    && Math.abs(y - lastY) <= RADIUS_PX;
            if (second) {
                lastAt = Long.MIN_VALUE / 2;
                return true;
            }
            lastMascot = mascotId;
            lastX = x;
            lastY = y;
            lastAt = now;
            return false;
        }
    }
}
