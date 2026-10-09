package com.group_finity.mascot.lumi.plugin;

import java.io.*;
import java.util.*;

/** Bounded, deterministic visual rules shared by host execution and editor previews. No script evaluation. */
public final class VisualLogic {
    private VisualLogic() { }
    public static final String FILE = "plugins/runtime/visual-logic.properties";
    public static final int MAX_RULES = 128;
    public record Dialogue(String id, String language, String text, String audio) { }
    public record Rule(String id, String event, String variable, double increment, String comparison,
                       double threshold, String dialogue, String behavior) { }
    public record Effect(String ruleId, String dialogueId, String text, String audio, String behavior) { }
    public record Document(String id, String imageSet, Map<String, Double> variables,
                           Map<String, Dialogue> dialogues, List<Rule> rules) {
        public Document {
            variables = Collections.unmodifiableMap(new LinkedHashMap<>(variables));
            dialogues = Collections.unmodifiableMap(new LinkedHashMap<>(dialogues));
            rules = List.copyOf(rules);
            validate(id, imageSet, variables, dialogues, rules);
        }
    }
    /** Reads UTF-8 characters supplied by the caller. Limits input to one million characters. */
    public static Document read(Reader reader) throws IOException {
        StringBuilder content = new StringBuilder(); char[] buffer = new char[4096]; int count;
        while ((count = reader.read(buffer)) != -1) {
            if (content.length() + count > 1_000_000) throw new IOException("Visual logic exceeds input limit");
            content.append(buffer, 0, count);
        }
        Properties p = new Properties() {
            @Override public synchronized Object put(Object key, Object value) {
                if (containsKey(key)) throw new IllegalArgumentException("Duplicate field: " + key);
                return super.put(key, value);
            }
        };
        try {
            p.load(new StringReader(content.toString()));
            for (String key : p.stringPropertyNames()) {
                if (!Set.of("schema", "id", "imageSet", "rule.count").contains(key)
                        && !key.matches("variable\\.[A-Za-z0-9][A-Za-z0-9._-]{0,63}")
                        && !key.matches("dialogue\\.[A-Za-z0-9][A-Za-z0-9._-]{0,63}\\.(text|language|audio)")
                        && !key.matches("rule\\.(0|[1-9][0-9]*)\\.(id|event|variable|increment|comparison|threshold|dialogue|behavior)"))
                    throw new IllegalArgumentException("Unknown field: " + key);
                if (key.startsWith("dialogue.") && !p.containsKey(key.substring(0, key.lastIndexOf('.')) + ".text"))
                    throw new IllegalArgumentException("Dialogue text missing: " + key);
            }
            if (!"1".equals(p.getProperty("schema"))) throw new IllegalArgumentException("Unsupported visual logic schema");
            Map<String, Double> variables = new TreeMap<>(); Map<String, Dialogue> dialogues = new TreeMap<>();
            for (String key : p.stringPropertyNames()) {
                if (key.startsWith("variable.")) variables.put(key.substring(9), number(p.getProperty(key)));
                if (key.startsWith("dialogue.") && key.endsWith(".text")) {
                    String id = key.substring(9, key.length() - 5), prefix = "dialogue." + id;
                    dialogues.put(id, new Dialogue(id, p.getProperty(prefix + ".language", ""),
                            p.getProperty(key), p.getProperty(prefix + ".audio", "")));
                }
            }
            int size = Integer.parseInt(p.getProperty("rule.count", "0"));
            if (size < 0 || size > MAX_RULES) throw new IllegalArgumentException("Rule budget exceeded");
            for (String key : p.stringPropertyNames()) if (key.startsWith("rule.") && !key.equals("rule.count")) {
                int index = Integer.parseInt(key.substring(5, key.indexOf('.', 5)));
                if (index >= size) throw new IllegalArgumentException("Rule outside declared count: " + key);
            }
            List<Rule> rules = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                String k = "rule." + i + ".";
                rules.add(new Rule(p.getProperty(k + "id", ""), p.getProperty(k + "event", ""),
                        p.getProperty(k + "variable", ""), number(p.getProperty(k + "increment", "0")),
                        p.getProperty(k + "comparison", "always"), number(p.getProperty(k + "threshold", "0")),
                        p.getProperty(k + "dialogue", ""), p.getProperty(k + "behavior", "")));
            }
            return new Document(p.getProperty("id", ""), p.getProperty("imageSet", ""), variables, dialogues, rules);
        } catch (IllegalArgumentException | NullPointerException e) { throw new IOException("Invalid visual logic: " + e.getMessage(), e); }
    }
    private static double number(String value) { double n = Double.parseDouble(value); finite(n); return n; }
    private static void finite(double n) { if (!Double.isFinite(n) || Math.abs(n) > 1e12) throw new IllegalArgumentException("Numeric value out of range"); }
    private static void id(String id) { if (id == null || !id.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,63}")) throw new IllegalArgumentException("Invalid identifier: " + id); }
    private static void validate(String owner, String imageSet, Map<String, Double> vars, Map<String, Dialogue> lines, List<Rule> rules) {
        id(owner); id(imageSet);
        if (vars.size() > 128 || lines.size() > 256 || rules.size() > MAX_RULES) throw new IllegalArgumentException("Document budget exceeded");
        vars.forEach((key, value) -> { id(key); finite(value); });
        lines.forEach((key, line) -> {
            id(key); if (!key.equals(line.id())) throw new IllegalArgumentException("Dialogue identity mismatch");
            if (line.text() == null || line.text().length() > 4096 || line.language() == null || line.language().length() > 32)
                throw new IllegalArgumentException("Dialogue too long");
            if (line.audio() == null || !line.audio().isEmpty() && (!line.audio().matches("sound/[A-Za-z0-9._/-]+\\.wav")
                    || Arrays.asList(line.audio().split("/")).contains("..") || line.audio().contains("//")))
                throw new IllegalArgumentException("Audio must be a relative sound WAV");
        });
        Set<String> ids = new HashSet<>();
        for (Rule rule : rules) {
            id(rule.id()); if (!ids.add(rule.id())) throw new IllegalArgumentException("Duplicate rule identifier");
            if (!Set.of("ready", "click", "exit").contains(rule.event())) {
                if (!rule.event().startsWith("menu:")) throw new IllegalArgumentException("Unknown event");
                id(rule.event().substring(5));
            }
            if (!Set.of("always", "eq", "ne", "lt", "le", "gt", "ge").contains(rule.comparison())) throw new IllegalArgumentException("Unknown comparison");
            if (!rule.variable().isEmpty() && !vars.containsKey(rule.variable()) || rule.variable().isEmpty()
                    && (!rule.comparison().equals("always") || rule.increment() != 0)) throw new IllegalArgumentException("Unknown variable");
            finite(rule.increment()); finite(rule.threshold());
            if (!rule.dialogue().isEmpty() && !lines.containsKey(rule.dialogue())) throw new IllegalArgumentException("Unknown dialogue");
            if (!rule.behavior().isEmpty()) id(rule.behavior());
        }
    }
    /** Each dispatch visits at most 128 ordered rules; update precedes comparison. No recursion or loops. */
    public static final class Session implements AutoCloseable {
        private final Document document; private final Map<String, Double> values; private boolean closed;
        public Session(Document document) { this.document = Objects.requireNonNull(document); values = new LinkedHashMap<>(document.variables()); }
        public synchronized Map<String, Double> variables() { return Collections.unmodifiableMap(new LinkedHashMap<>(values)); }
        /** Restores known finite values only; ignores unknown keys. Invalid known values reject the entire restore. */
        public synchronized void restore(Map<String, Double> saved) {
            if (closed) throw new IllegalStateException("Session closed"); Objects.requireNonNull(saved);
            Map<String, Double> next = new LinkedHashMap<>(values);
            saved.forEach((key, value) -> { if (next.containsKey(key)) { finite(value); next.put(key, value); } });
            values.clear(); values.putAll(next);
        }
        public synchronized List<Effect> dispatch(String event) {
            if (closed) throw new IllegalStateException("Session closed");
            Map<String, Double> next = new LinkedHashMap<>(values); List<Effect> effects = new ArrayList<>();
            for (Rule rule : document.rules()) {
                if (!rule.event().equals(event)) continue;
                double value = 0;
                if (!rule.variable().isEmpty()) { value = next.get(rule.variable()) + rule.increment(); finite(value); next.put(rule.variable(), value); }
                boolean pass = switch (rule.comparison()) {
                    case "always" -> true; case "eq" -> value == rule.threshold(); case "ne" -> value != rule.threshold();
                    case "lt" -> value < rule.threshold(); case "le" -> value <= rule.threshold(); case "gt" -> value > rule.threshold(); case "ge" -> value >= rule.threshold();
                    default -> false;
                };
                if (!pass) continue;
                Dialogue line = document.dialogues().get(rule.dialogue());
                effects.add(new Effect(rule.id(), rule.dialogue(), line == null ? "" : line.text(), line == null ? "" : line.audio(), rule.behavior()));
            }
            values.clear(); values.putAll(next); return List.copyOf(effects);
        }
        public synchronized void close() { closed = true; }
    }
}
