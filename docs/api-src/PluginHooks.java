package com.group_finity.mascot.lumi.plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 질의형 훅 — 기능 코드가 <b>결정하기 전에</b> 플러그인에게 묻는 자리.
 * {@link PluginEvents} 가 「일어난 뒤 알림」이라면 이것은 「정하기 전 질문」이다.
 * <p>
 * 코어는 {@code decide("focus.quiet", 기본값, k, v, …)} 한 줄로 묻고, 플러그인은
 * {@link PluginContext#hook} 으로 답한다. 규약:
 * <ul>
 *   <li><b>동기.</b> 부른 스레드에서 그 자리에서 돈다 — EDT·감독자 타이머·엔진 틱 위일 수 있으니
 *       핸들러는 값싸야 하고 절대 블록하지 않는다. {@value #SLOW_MS}ms 를 넘긴 핸들러는
 *       훅+플러그인당 한 번 경고하고 값은 그대로 쓴다.</li>
 *   <li><b>순서 = 모드 탭 순서, 아래가 이긴다.</b> 위에서 아래로 부르고 각 핸들러는 앞선 결과를
 *       {@code current} 로 받는다. 마지막 쓰기가 이긴다 — 오버레이·클래스패스와 같은 뜻. AND 판은
 *       없다: 앞선 답을 존중하려면 핸들러가 {@code current} 를 보고 스스로 만든다.</li>
 *   <li><b>{@code null} = 통과.</b> 「없음/취소」는 훅마다 정한 값({@code ""}, {@code -1})으로만 말한다.
 *       {@code bubble.text}의 공백 반환은 원문 유지다. 사용자가 건 약속(알람·부탁·{@code say.txt})을
 *       삼키는 훅은 두지 않는다.</li>
 *   <li><b>핸들러 예외 격리.</b> 핸들러가 던진 {@code Throwable}을 잡고 훅+플러그인당 한 번만 로그, 그 핸들러만
 *       건너뛰고 체인은 계속. 아무도 답하지 않으면 기본값. 반환값이 기대 타입과 다르면 통과 + 한 번 경고.
 *       숫자 전용 메서드는 {@code Number}를 받아 호출부 범위로 제한한다.</li>
 *   <li><b>핸들러 없는 경우.</b> ctx Map을 만들지 않고 기본값을 즉시 반환한다.</li>
 * </ul>
 * {@link #KNOWN} 이 카탈로그다. 훅 id 는 영어 점 표기이고, 같은 {@code API_VERSION} 안에서 안정 id 는
 * 더해지기만 하며 결정 자리가 없어진 훅은 기본값만 돌려주는 빈 자리로 남는다; 실험 id(SDK 의 API.md §8)는
 * 바뀔 수 있다. {@code HookCatalogTest} 가 소스·문서와 맞춘다.
 * 이 클래스는 보호 접두({@code lumi.plugin.}) 안에 있어 플러그인이 patch 하지 못한다.
 */
public final class PluginHooks {
    private static final Logger log = Logger.getLogger(PluginHooks.class.getName());

    /** 느린 핸들러 경고 문턱. */
    public static final long SLOW_MS = 20;

    /** 자리·ctx·반환은 docs/spec-plugins.md 「질의형 훅」 표. */
    public static final Set<String> KNOWN = Set.of(
            "focus.quiet", "focus.screen_busy",
            "text.localized", "sound.effect", "behavior.force", "bubble.duration",
            "selftalk.line", "selftalk.interval", "speech.greeting",
            "affection.award", "affection.tier",
            "place.allowed",
            "bongo.key_frame", "bongo.key_input",
            "bubble.text");

    /** 핸들러: 지금까지의 값({@code current})을 받아 새 값을 돌려주거나 {@code null}(= 통과). */
    @FunctionalInterface
    public interface Handler {
        Object handle(String hook, Object current, Map<String, Object> ctx);
    }

    /** 등록 한 건 — 플러그인 id 와 순위(모드 탭 순서)를 같이 쥔다. */
    static final class Entry {
        final String pluginId;
        final int rank;
        final Handler handler;

        Entry(String pluginId, int rank, Handler handler) {
            this.pluginId = pluginId;
            this.rank = rank;
            this.handler = handler;
        }
    }

    private static final Map<String, List<Entry>> handlers = new ConcurrentHashMap<>();
    private static volatile boolean any;
    /** 훅+플러그인당 한 번만 남기는 경고의 열쇠. */
    static final Set<String> reportedOnce = ConcurrentHashMap.newKeySet();

    private PluginHooks() {
    }

    // ── 코어가 묻는 자리 ─────────────────────────────────────────────────────

    /** {@link PluginEvents#hasListeners()} 와 같은 값싼 문 — 이 훅에 답하는 플러그인이 있는가. */
    public static boolean hasHandlers(String hook) {
        if (!any) {
            return false;
        }
        List<Entry> list = handlers.get(hook);
        return list != null && !list.isEmpty();
    }

    /**
     * 묻는다. {@code defaultValue} 가 지금의 동작이고, ctx 는 {@code k, v, k, v…}. 답하는 플러그인이
     * 없으면 {@code defaultValue} 그 객체를 그대로 돌려준다 (Map 을 만들지 않는다).
     */
    public static <T> T decide(String hook, T defaultValue, Object... ctx) {
        return decideAs(hook, defaultValue, defaultValue == null ? null : defaultValue.getClass(), ctx);
    }

    /**
     * {@code defaultValue} 가 {@code null} 일 수 있는 훅(Map 계열)은 기대하는 클래스를 명시한다.
     */
    @SuppressWarnings("unchecked")
    public static <T> T decideAs(String hook, T defaultValue, Class<?> type, Object... ctx) {
        List<Entry> list = any ? handlers.get(hook) : null;
        if (list == null || list.isEmpty()) {
            return defaultValue;
        }
        Map<String, Object> context = context(ctx);
        Object current = defaultValue;
        for (Entry e : ordered(list)) {
            Object answer;
            long t0 = System.nanoTime();
            try {
                answer = e.handler.handle(hook, current, context);
            } catch (Throwable t) {
                warnOnce(hook, e.pluginId, "threw", t);
                continue;
            }
            long tookMs = (System.nanoTime() - t0) / 1_000_000L;
            if (tookMs > SLOW_MS) {
                warnOnce(hook, e.pluginId, "slow: " + tookMs + " ms (limit " + SLOW_MS + ")", null);
            }
            if (answer == null) {
                continue;
            }
            if (type != null && !type.isInstance(answer)) {
                warnOnce(hook, e.pluginId, "returned " + answer.getClass().getSimpleName()
                        + " where " + type.getSimpleName() + " was expected — ignored", null);
                continue;
            }
            current = answer;
        }
        return (T) current;
    }

    public static boolean decideBoolean(String hook, boolean dflt, Object... ctx) {
        Boolean out = decideAs(hook, Boolean.valueOf(dflt), Boolean.class, ctx);
        return out != null ? out : dflt;
    }

    /** 숫자 훅은 부르는 자리에서 범위를 고정한다 — clamp 를 잊지 않도록 min/max 가 서명에 있다. */
    public static int decideInt(String hook, int dflt, int min, int max, Object... ctx) {
        Number out = decideAs(hook, (Number) Integer.valueOf(dflt), Number.class, ctx);
        long v = out == null ? dflt : out.longValue();
        return (int) Math.max(min, Math.min(max, v));
    }

    public static long decideLong(String hook, long dflt, long min, long max, Object... ctx) {
        Number out = decideAs(hook, (Number) Long.valueOf(dflt), Number.class, ctx);
        long v = out == null ? dflt : out.longValue();
        return Math.max(min, Math.min(max, v));
    }

    public static String decideString(String hook, String dflt, Object... ctx) {
        String out = decideAs(hook, dflt, String.class, ctx);
        return out == null ? dflt : out;
    }

    /** 결과는 항상 복사본 — 핸들러가 받은 목록을 고쳐도 기본 목록은 변하지 않는다. */
    @SuppressWarnings("unchecked")
    public static <E> List<E> decideList(String hook, List<E> dflt, Object... ctx) {
        List<E> base = dflt == null ? List.of() : dflt;
        if (!hasHandlers(hook)) {
            return base;
        }
        List<?> out = decideAs(hook, (List<?>) new ArrayList<>(base), List.class, ctx);
        return out == null ? base : Collections.unmodifiableList(new ArrayList<>((List<E>) out));
    }

    // ── 플러그인이 답하는 자리 (PluginContext 만 부른다) ───────────────────────

    static void register(String pluginId, String hook, Handler handler) {
        register(pluginId, PluginLoader.rankOf(pluginId), hook, handler);
    }

    /** 순위를 직접 주는 판 — 검사가 모드 순서를 흉내 낼 때. */
    static void register(String pluginId, int rank, String hook, Handler handler) {
        if (hook == null || hook.isBlank() || handler == null) {
            return;
        }
        handlers.computeIfAbsent(hook, k -> new CopyOnWriteArrayList<>())
                .add(new Entry(pluginId == null ? "" : pluginId, rank, handler));
        any = true;
        if (!KNOWN.contains(hook)) {
            log.info("Plugin " + pluginId + " hooks '" + hook + "' which this app does not ask — it will never be called");
        }
    }

    static void unregister(String pluginId, String hook, Handler handler) {
        List<Entry> list = handlers.get(hook);
        if (list == null) {
            return;
        }
        list.removeIf(e -> e.handler == handler && (pluginId == null || pluginId.equals(e.pluginId)));
    }

    /** 검사용 — 모든 등록과 경고 기록을 비운다. */
    static void resetForTest() {
        handlers.clear();
        reportedOnce.clear();
        any = false;
    }

    // ── 내부 ────────────────────────────────────────────────────────────────

    /** 모드 탭 순서(순위 오름차순, 같은 순위는 등록 순)로 — 매 호출 정렬해 늦게 등록해도 자리를 지킨다. */
    private static List<Entry> ordered(List<Entry> list) {
        if (list.size() < 2) {
            return list;
        }
        List<Entry> copy = new ArrayList<>(list);
        copy.sort((a, b) -> Integer.compare(a.rank, b.rank)); // stable
        return copy;
    }

    /** {@link PluginEvents#publish(String, Object...)} 와 같은 k,v 규약: 불변, null 은 "". */
    static Map<String, Object> context(Object... ctx) {
        if (ctx == null || ctx.length == 0) {
            return Map.of();
        }
        Map<String, Object> m = new java.util.HashMap<>();
        for (int i = 0; i + 1 < ctx.length; i += 2) {
            Object v = ctx[i + 1];
            m.put(String.valueOf(ctx[i]), v == null ? "" : v);
        }
        return Map.copyOf(m);
    }

    private static void warnOnce(String hook, String pluginId, String what, Throwable t) {
        if (reportedOnce.add(hook + "|" + pluginId + "|" + what.split(":")[0])) {
            log.log(Level.WARNING, "Plugin " + pluginId + " on hook '" + hook + "' " + what
                    + " — reported once, the chain goes on", t);
        }
    }
}
