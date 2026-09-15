package com.group_finity.mascot.lumi.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;

/**
 * What a plugin says about itself — the parsed {@code plugin.json} at the root of
 * its jar, plus where the jar came from and its content hash.
 * <p>
 * The manifest follows the shape modders already know from Slay the Spire
 * ({@code ModTheSpire.json} / STS2's {@code <id>.json}):
 * <pre>
 * {
 *   "id": "example.hello",          required — [a-z0-9._-], 1..64, the folder name of its data
 *   "name": "Hello LUMI",           required — what the Mods tab shows
 *   "version": "1.0.0",             optional
 *   "author": "someone",            optional
 *   "description": "…",             optional, one or two lines
 *   "dependencies": ["other.id"],   optional — load before this one; missing = this one is skipped
 *   "api_version": 1,               optional — the {@link PluginContext#API_VERSION} it was written for
 *   "affects_character": true,      optional — changes behaviours or lines (shown as a chip)
 *   "network": false,               optional — talks to the internet (shown as a chip; not enforced)
 *   "hooks": ["focus.quiet"],       optional — hook ids it answers; unknown ones are shown, never blocked
 *   "settings": [ … ]               optional — declarative settings the Mods tab draws ({@link PluginSettingsSchema})
 * }
 * </pre>
 * The JSON reader here is deliberately tiny and self-contained: the loader is core
 * code and may not depend on a feature cluster's parser.
 */
public final class PluginDescriptor {
    public static final String FILE = "plugin.json";
    static final Pattern ID = Pattern.compile("[a-z0-9][a-z0-9._-]{0,63}");

    public final String id;
    public final String name;
    public final String version;
    public final String author;
    public final String description;
    public final List<String> dependencies;
    public final int apiVersion;
    public final boolean affectsCharacter;
    public final boolean network;
    /**
     * Hook ids this plugin answers ({@code "hooks": ["focus.quiet", …]}, optional). The
     * loader compares them with {@link PluginHooks#KNOWN} and tells the user about the
     * ones this app never asks — a compatibility notice, never a block.
     */
    public final List<String> hooks;
    /**
     * The app version the plugin was written for ({@code "app_version": "1.1.0"}, optional).
     * The Mods tab shows a chip when it differs from the running app; nothing is blocked.
     */
    public final String appVersion;
    /**
     * 모드 탭이 그려 주는 선언형 설정({@code "settings": [ … ]}, 선택). 값은
     * 그 플러그인의 {@link PluginPrefs} 에 산다. 틀린 항목은 건너뛰고 로그에 남길 뿐, 실행을 막지 않는다.
     */
    public final PluginSettingsSchema settings;
    /** The jar this came from; {@code null} for a descriptor parsed from text alone. */
    public final Path jar;
    /** SHA-256 of the jar, lowercase hex; empty when there is no jar. */
    public final String sha256;
    /** {@code plugins} for the app's own folder, {@code workshop:<id>} for a subscription. */
    public final String source;

    PluginDescriptor(Map<String, Object> json, Path jar, String sha256, String source) {
        this.id = str(json, "id");
        this.name = str(json, "name").isEmpty() ? id : str(json, "name");
        this.version = str(json, "version");
        this.author = str(json, "author");
        this.description = str(json, "description");
        this.dependencies = strings(json.get("dependencies"));
        Object api = json.get("api_version");
        this.apiVersion = api instanceof Number n ? n.intValue() : 1;
        this.affectsCharacter = Boolean.TRUE.equals(json.get("affects_character"));
        this.network = Boolean.TRUE.equals(json.get("network"));
        this.hooks = strings(json.get("hooks"));
        this.appVersion = str(json, "app_version");
        this.settings = PluginSettingsSchema.parse(json.get("settings"), this.id);
        this.jar = jar;
        this.sha256 = sha256 == null ? "" : sha256;
        this.source = source == null ? "plugins" : source;
    }

    /** Parses the manifest text; throws with a message a modder can act on. */
    public static PluginDescriptor parse(String text, Path jar, String sha256, String source) {
        Object root = new Json(text).parse();
        if (!(root instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException(FILE + " must be a JSON object");
        }
        @SuppressWarnings("unchecked")
        PluginDescriptor d = new PluginDescriptor((Map<String, Object>) map, jar, sha256, source);
        if (!ID.matcher(d.id).matches()) {
            throw new IllegalArgumentException(FILE + ": \"id\" must match " + ID.pattern()
                    + " (got \"" + d.id + "\")");
        }
        for (String dep : d.dependencies) {
            if (!ID.matcher(dep).matches()) {
                throw new IllegalArgumentException(FILE + ": dependency \"" + dep + "\" is not a valid id");
            }
        }
        return d;
    }

    /** Reads a jar's manifest and hashes the jar. */
    public static PluginDescriptor read(Path jar, String source) throws IOException {
        String text;
        try (JarFile file = new JarFile(jar.toFile())) {
            ZipEntry entry = file.getEntry(FILE);
            if (entry == null) {
                throw new IOException("no " + FILE + " at the root of the jar");
            }
            try (InputStream in = file.getInputStream(entry)) {
                byte[] metadata = in.readNBytes(1024 * 1024 + 1);
                if (metadata.length > 1024 * 1024) throw new IOException("plugin.json exceeds 1 MiB");
                text = new String(metadata, StandardCharsets.UTF_8);
            }
        }
        return parse(text, jar, sha256(jar), source);
    }

    public static String sha256(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream in = Files.newInputStream(file)) {
                byte[] buffer = new byte[1 << 16];
                int n;
                while ((n = in.read(buffer)) > 0) {
                    digest.update(buffer, 0, n);
                }
            }
            return java.util.HexFormat.of().formatHex(digest.digest());
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IOException(e);
        }
    }

    @Override
    public String toString() {
        return id + (version.isEmpty() ? "" : " " + version) + (jar == null ? "" : " (" + jar.getFileName() + ")");
    }

    private static String str(Map<String, Object> json, String key) {
        Object v = json.get(key);
        return v == null ? "" : String.valueOf(v).trim();
    }

    private static List<String> strings(Object value) {
        if (!(value instanceof List<?> list)) {
            return Collections.emptyList();
        }
        List<String> out = new ArrayList<>();
        for (Object o : list) {
            if (o != null) {
                out.add(String.valueOf(o).trim());
            }
        }
        return Collections.unmodifiableList(out);
    }

    // ── a minimal JSON reader (objects, arrays, strings, numbers, true/false/null) ──

    static final class Json {
        private final String s;
        private int i;

        Json(String s) {
            this.s = s;
        }

        Object parse() {
            Object v = value();
            ws();
            if (i != s.length()) {
                throw error("trailing characters");
            }
            return v;
        }

        private Object value() {
            ws();
            if (i >= s.length()) {
                throw error("unexpected end");
            }
            char c = s.charAt(i);
            switch (c) {
                case '{': return object();
                case '[': return array();
                case '"': return string();
                case 't': return literal("true", Boolean.TRUE);
                case 'f': return literal("false", Boolean.FALSE);
                case 'n': return literal("null", null);
                default:
                    if (c == '-' || (c >= '0' && c <= '9')) {
                        return number();
                    }
                    throw error("unexpected '" + c + "'");
            }
        }

        private Map<String, Object> object() {
            Map<String, Object> map = new LinkedHashMap<>();
            i++; // {
            ws();
            if (peek() == '}') {
                i++;
                return map;
            }
            while (true) {
                ws();
                if (peek() != '"') {
                    throw error("expected a quoted key");
                }
                String key = string();
                ws();
                if (peek() != ':') {
                    throw error("expected ':'");
                }
                i++;
                map.put(key, value());
                ws();
                char c = peek();
                i++;
                if (c == ',') {
                    continue;
                }
                if (c == '}') {
                    return map;
                }
                throw error("expected ',' or '}'");
            }
        }

        private List<Object> array() {
            List<Object> list = new ArrayList<>();
            i++; // [
            ws();
            if (peek() == ']') {
                i++;
                return list;
            }
            while (true) {
                list.add(value());
                ws();
                char c = peek();
                i++;
                if (c == ',') {
                    continue;
                }
                if (c == ']') {
                    return list;
                }
                throw error("expected ',' or ']'");
            }
        }

        private String string() {
            StringBuilder out = new StringBuilder();
            i++; // "
            while (i < s.length()) {
                char c = s.charAt(i++);
                if (c == '"') {
                    return out.toString();
                }
                if (c == '\\') {
                    if (i >= s.length()) {
                        break;
                    }
                    char e = s.charAt(i++);
                    switch (e) {
                        case 'n': out.append('\n'); break;
                        case 't': out.append('\t'); break;
                        case 'r': out.append('\r'); break;
                        case 'b': out.append('\b'); break;
                        case 'f': out.append('\f'); break;
                        case 'u':
                            if (i + 4 > s.length()) {
                                throw error("bad \\u escape");
                            }
                            out.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
                            i += 4;
                            break;
                        default: out.append(e);
                    }
                } else {
                    out.append(c);
                }
            }
            throw error("unterminated string");
        }

        private Object number() {
            int start = i;
            if (peek() == '-') {
                i++;
            }
            while (i < s.length() && "0123456789.eE+-".indexOf(s.charAt(i)) >= 0) {
                i++;
            }
            String t = s.substring(start, i);
            try {
                if (t.contains(".") || t.contains("e") || t.contains("E")) {
                    return Double.parseDouble(t);
                }
                return Long.parseLong(t);
            } catch (NumberFormatException e) {
                throw error("bad number '" + t + "'");
            }
        }

        private Object literal(String word, Object value) {
            if (!s.startsWith(word, i)) {
                throw error("expected " + word);
            }
            i += word.length();
            return value;
        }

        private void ws() {
            while (i < s.length() && Character.isWhitespace(s.charAt(i))) {
                i++;
            }
        }

        private char peek() {
            return i < s.length() ? s.charAt(i) : '\0';
        }

        private IllegalArgumentException error(String what) {
            return new IllegalArgumentException(FILE + ": " + what + " at offset " + i);
        }
    }
}
