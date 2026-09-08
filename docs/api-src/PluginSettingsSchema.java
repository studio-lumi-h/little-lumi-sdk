package com.group_finity.mascot.lumi.plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * {@code plugin.json} 의 선언형 설정 — {@code "settings": [ … ]}.
 * <p>
 * 플러그인은 칸을 <b>선언</b>만 하고 그리지 않는다. 모드 탭이 그 플러그인 줄의 「자세히」 안에
 * 앱 테마로 그려 주고, 값은 그 플러그인의 {@link PluginPrefs}(<code>plugindata\&lt;id&gt;\settings.properties</code>)에
 * <b>바꾸는 즉시</b> 저장한다(모드 탭의 다른 스위치와 같다 — [적용]을 기다리지 않는다). 플러그인은
 * {@code ctx.prefs().get(key)} 로 읽고 {@code ctx.prefs().onChange} 로 바뀜을 듣는다. 모드 탭이 받는 것은
 * 이 선언형 카드뿐이다 — <i>자유형 Swing 패널</i>은 들이지 않는다.
 * <pre>
 * "settings": [
 *   { "key": "greet",  "type": "bool",   "label": {"ko": "인사하기", "en": "Greet"}, "default": true },
 *   { "key": "volume", "type": "int",    "label": "Volume", "min": 0, "max": 100, "step": 5, "default": 50 },
 *   { "key": "name",   "type": "string", "label": "Name", "default": "" , "hint": {"ko": "…", "en": "…"} },
 *   { "key": "mood",   "type": "choice", "label": "Mood", "default": "calm",
 *     "options": [ {"value": "calm", "label": {"ko": "차분", "en": "Calm"}}, {"value": "wild", "label": "Wild"} ] },
 *   { "key": "toggle", "type": "hotkey", "label": "Toggle key", "default": "Ctrl+Alt+L" }
 * ]
 * </pre>
 * 글은 문자열 하나(모든 언어 공통)이거나 언어별 객체다. 고르는 차례: 정확한 태그({@code zh-TW}/{@code zh_TW})
 * → 언어({@code zh}; 단 번체는 이 단계를 건너뛴다 — 간체로 새지 않게) → {@code en} → 첫 값. 틀린 항목은 로그에 남기고 건너뛴다 — 절대 치명적이지
 * 않다(설정 하나 잘못 적었다고 플러그인이 안 실리면 안 된다).
 */
public final class PluginSettingsSchema {
    private static final Logger log = Logger.getLogger(PluginSettingsSchema.class.getName());
    static final Pattern KEY = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,63}");
    public static final PluginSettingsSchema EMPTY = new PluginSettingsSchema(List.of());

    public enum Type { BOOL, INT, STRING, CHOICE, HOTKEY }

    /** 선택지 하나 — 저장되는 값과 보이는 이름. */
    public static final class Option {
        public final String value;
        private final Map<String, String> labels;

        Option(String value, Map<String, String> labels) {
            this.value = value;
            this.labels = labels;
        }

        public String label(String language) {
            String t = pick(labels, language);
            return t.isEmpty() ? value : t;
        }
    }

    /** 칸 하나. */
    public static final class Entry {
        public final String key;
        public final Type type;
        private final Map<String, String> labels;
        private final Map<String, String> hints;
        /** 기본값(문자열 꼴). 없으면 형마다의 빈 값({@code false}, {@code min}, {@code ""}, 첫 선택지, {@code ""}). */
        public final String defaultValue;
        public final int min;
        public final int max;
        public final int step;
        public final List<Option> options;

        Entry(String key, Type type, Map<String, String> labels, Map<String, String> hints,
              String defaultValue, int min, int max, int step, List<Option> options) {
            this.key = key;
            this.type = type;
            this.labels = labels;
            this.hints = hints;
            this.defaultValue = defaultValue;
            this.min = min;
            this.max = max;
            this.step = step;
            this.options = options;
        }

        /** 보이는 이름 — 없으면 열쇠 그대로. */
        public String label(String language) {
            String t = pick(labels, language);
            return t.isEmpty() ? key : t;
        }

        /** 설명 한 줄 — 없으면 빈 글. */
        public String hint(String language) {
            return pick(hints, language);
        }

        /** 이 칸이 받아들이는 값인가 — 모드 탭·검증 하네스가 쓴다. */
        public boolean accepts(String value) {
            if (value == null) {
                return false;
            }
            switch (type) {
                case BOOL:
                    return value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false");
                case INT:
                    try {
                        int v = Integer.parseInt(value.strip());
                        return v >= min && v <= max;
                    } catch (NumberFormatException e) {
                        return false;
                    }
                case CHOICE:
                    for (Option o : options) {
                        if (o.value.equals(value)) {
                            return true;
                        }
                    }
                    return false;
                case HOTKEY:
                    return value.isBlank() || com.group_finity.mascot.lumi.LumiHotkeys.normalize(value) != null;
                default:
                    return true;
            }
        }
    }

    public final List<Entry> entries;

    PluginSettingsSchema(List<Entry> entries) {
        this.entries = Collections.unmodifiableList(new ArrayList<>(entries));
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public Entry entry(String key) {
        for (Entry e : entries) {
            if (e.key.equals(key)) {
                return e;
            }
        }
        return null;
    }

    /** 저장소에 없는 열쇠에 기본값을 채워 넣는다(플러그인이 첫 실행부터 값을 읽을 수 있게). 있는 값은 건드리지 않는다. */
    public void seedDefaults(PluginPrefs prefs) {
        if (prefs == null) {
            return;
        }
        for (Entry e : entries) {
            if (!prefs.has(e.key)) {
                prefs.set(e.key, e.defaultValue);
            }
        }
    }

    // ── 읽기 ────────────────────────────────────────────────────────────────

    /** {@code "settings"} 값(배열)을 읽는다. 배열이 아니면 {@link #EMPTY}. */
    public static PluginSettingsSchema parse(Object json, String pluginId) {
        if (!(json instanceof List<?> list) || list.isEmpty()) {
            return EMPTY;
        }
        List<Entry> out = new ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        int index = 0;
        for (Object item : list) {
            index++;
            String where = "plugin " + pluginId + " settings[" + (index - 1) + "]";
            if (!(item instanceof Map<?, ?> raw)) {
                log.warning(where + ": not an object — skipped");
                continue;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> m = (Map<String, Object>) raw;
            String key = text(m.get("key"));
            if (!KEY.matcher(key).matches()) {
                log.warning(where + ": bad or missing \"key\" (" + key + ") — skipped");
                continue;
            }
            if (!seen.add(key)) {
                log.warning(where + ": duplicate key \"" + key + "\" — skipped");
                continue;
            }
            Type type;
            String typeName = text(m.get("type")).toLowerCase(Locale.ROOT);
            switch (typeName) {
                case "bool", "boolean" -> type = Type.BOOL;
                case "int", "integer", "number" -> type = Type.INT;
                case "string", "text", "" -> type = Type.STRING;
                case "choice", "enum", "select" -> type = Type.CHOICE;
                case "hotkey", "key" -> type = Type.HOTKEY;
                default -> {
                    log.warning(where + ": unknown type \"" + typeName + "\" — skipped");
                    continue;
                }
            }
            Map<String, String> labels = texts(m.get("label"));
            Map<String, String> hints = texts(m.get("hint"));
            int min = 0;
            int max = 100;
            int step = 1;
            List<Option> options = List.of();
            if (type == Type.INT) {
                min = number(m.get("min"), 0);
                max = number(m.get("max"), 100);
                step = Math.max(1, number(m.get("step"), 1));
                if (max < min) {
                    log.warning(where + ": max < min — skipped");
                    continue;
                }
            }
            if (type == Type.CHOICE) {
                List<Option> opts = new ArrayList<>();
                if (m.get("options") instanceof List<?> ol) {
                    for (Object o : ol) {
                        if (o instanceof Map<?, ?> om) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> option = (Map<String, Object>) om;
                            String value = text(option.get("value"));
                            if (!value.isEmpty()) {
                                opts.add(new Option(value, texts(option.get("label"))));
                            }
                        } else if (o != null) {
                            String value = String.valueOf(o).strip();
                            if (!value.isEmpty()) {
                                opts.add(new Option(value, Map.of()));
                            }
                        }
                    }
                }
                if (opts.isEmpty()) {
                    log.warning(where + ": choice without options — skipped");
                    continue;
                }
                options = Collections.unmodifiableList(opts);
            }
            String dflt = defaultOf(m.get("default"), type, min, options);
            out.add(new Entry(key, type, labels, hints, dflt, min, max, step, options));
        }
        return out.isEmpty() ? EMPTY : new PluginSettingsSchema(out);
    }

    /** 앱이 지금 쓰는 UI 언어 — {@code ko} {@code en} {@code ja} {@code zh-CN} {@code zh-TW}. 모르면 {@code en}. */
    public static String currentLanguage() {
        try {
            return PluginContext.languageCode(com.group_finity.mascot.Main.getInstance().getSettings().language);
        } catch (Exception | LinkageError e) {
            return "en";
        }
    }

    // ── 속 ──────────────────────────────────────────────────────────────────

    private static String defaultOf(Object raw, Type type, int min, List<Option> options) {
        String given = raw == null ? null : (raw instanceof Number n && n.doubleValue() == Math.rint(n.doubleValue())
                ? String.valueOf(n.longValue()) : String.valueOf(raw).strip());
        switch (type) {
            case BOOL:
                return given == null ? "false" : String.valueOf(Boolean.parseBoolean(given));
            case INT:
                if (given != null) {
                    try {
                        return String.valueOf(Integer.parseInt(given));
                    } catch (NumberFormatException e) {
                        // 아래로 — min
                    }
                }
                return String.valueOf(min);
            case CHOICE:
                if (given != null) {
                    for (Option o : options) {
                        if (o.value.equals(given)) {
                            return given;
                        }
                    }
                }
                return options.isEmpty() ? "" : options.get(0).value;
            case HOTKEY: {
                if (given == null) {
                    return "";
                }
                String normalized = com.group_finity.mascot.lumi.LumiHotkeys.normalize(given);
                return normalized == null ? "" : normalized;
            }
            default:
                return given == null ? "" : given;
        }
    }

    private static int number(Object v, int fallback) {
        if (v instanceof Number n) {
            return n.intValue();
        }
        if (v instanceof String s) {
            try {
                return Integer.parseInt(s.strip());
            } catch (NumberFormatException e) {
                return fallback;
            }
        }
        return fallback;
    }

    private static String text(Object v) {
        return v == null ? "" : String.valueOf(v).strip();
    }

    /** 문자열 하나 → {@code {"": 글}}, 객체 → 언어별. 열쇠는 소문자·{@code -} 로 맞춘다. */
    static Map<String, String> texts(Object v) {
        if (v == null) {
            return Map.of();
        }
        if (v instanceof Map<?, ?> m) {
            Map<String, String> out = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : m.entrySet()) {
                if (e.getValue() != null) {
                    out.put(tag(String.valueOf(e.getKey())), String.valueOf(e.getValue()).strip());
                }
            }
            return Collections.unmodifiableMap(out);
        }
        return Map.of("", String.valueOf(v).strip());
    }

    /** {@code zh_TW}·{@code zh-tw}·{@code zh-Hant} → {@code zh-tw} 꼴(소문자, 붙임표). */
    static String tag(String raw) {
        String t = raw == null ? "" : raw.strip().replace('_', '-').toLowerCase(Locale.ROOT);
        if (t.equals("zh-hant") || t.equals("zh-hant-tw")) {
            return "zh-tw";
        }
        if (t.equals("zh-hans") || t.equals("zh-hans-cn")) {
            return "zh-cn";
        }
        return t;
    }

    /** 언어별 글에서 하나 고르기: 정확한 태그 → 언어 → 공통(빈 열쇠) → en → 첫 값. */
    static String pick(Map<String, String> texts, String language) {
        if (texts == null || texts.isEmpty()) {
            return "";
        }
        String want = tag(language);
        String hit = texts.get(want);
        if (hit != null) {
            return hit;
        }
        int dash = want.indexOf('-');
        String lang = dash > 0 ? want.substring(0, dash) : want;
        // zh-CN 을 청하면 zh 가, zh 를 청하면 zh-cn 이 그다음이다. 번체(zh-TW)만은 언어 단계를 건너뛴다 —
        // 모더가 적은 "zh" 는 십중팔구 간체이고, 앱 번들도 번체가 간체로 새는 것을 막는다(BundleCoverageTest §3).
        if (!want.equals("zh-tw")) {
            hit = texts.get(lang);
            if (hit != null) {
                return hit;
            }
            for (Map.Entry<String, String> e : texts.entrySet()) {
                if (e.getKey().startsWith(lang + "-") && !e.getKey().equals("zh-tw")) {
                    return e.getValue();
                }
            }
        }
        hit = texts.get("");
        if (hit != null) {
            return hit;
        }
        hit = texts.get("en");
        if (hit != null) {
            return hit;
        }
        return texts.values().iterator().next();
    }
}
