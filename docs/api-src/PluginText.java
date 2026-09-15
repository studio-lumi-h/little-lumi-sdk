package com.group_finity.mascot.lumi.plugin;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.JarURLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * A plugin's <b>own</b> five-language strings — the app's bundle holds only the app's text, so a
 * plugin ships its translations here.
 * <p>
 * Files at the root of the plugin jar, plain {@code key=value} properties in UTF-8:
 * <pre>
 *   plugin_ko.properties   plugin_en.properties   plugin_ja.properties
 *   plugin_zh.properties   plugin_zh_TW.properties   plugin.properties (base)
 * </pre>
 * Lookup order for the app's current language ({@link PluginContext#language}):
 * <ul>
 *   <li>{@code ko} → {@code plugin_ko} → {@code plugin_en} → {@code plugin} → app bundle → fallback</li>
 *   <li>{@code zh-CN} → {@code plugin_zh_CN} → {@code plugin_zh} → {@code plugin_en} → …</li>
 *   <li>{@code zh-TW} → {@code plugin_zh_TW} → {@code plugin_zh_Hant} → {@code plugin_en} → … —
 *       <b>never {@code plugin_zh}</b>: a modder's {@code zh} is Simplified nine times out of ten, and the
 *       app's own rule is that Traditional never falls back to Simplified (CLAUDE.md, zh-TW).</li>
 * </ul>
 * Several plugin jars share one class loader, so a resource is picked by the URL that lies inside
 * <i>this</i> plugin's jar; without a jar (a test) a class-folder resource wins over any jar, then the first match. Files are read once and cached.
 * The same pattern LUMI Chat's {@code AiText} used for {@code lumi_ai*.properties}, made general.
 */
public final class PluginText {
    private final String pluginId;
    private final Path jarPath;
    private final ClassLoader loader;
    private final Supplier<String> language;
    private final BiFunction<String, String, String> appBundle;
    private final Map<String, Properties> cache = new ConcurrentHashMap<>();

    PluginText(String pluginId, Path jar, ClassLoader loader, Supplier<String> language,
               BiFunction<String, String, String> appBundle) {
        this.pluginId = pluginId == null ? "" : pluginId;
        this.jarPath = jar == null ? null : canonical(PluginLoader.runtimeJarFor(jar));
        this.loader = loader == null ? ClassLoader.getSystemClassLoader() : loader;
        this.language = language == null ? () -> "en" : language;
        this.appBundle = appBundle == null ? (k, f) -> f : appBundle;
    }

    /** The text for {@code key} in the app's current language, or {@code fallback}. */
    public String get(String key, String fallback) {
        if (key == null || key.isBlank()) {
            return fallback;
        }
        for (String suffix : candidates(language.get())) {
            Properties p = load(suffix);
            String v = p == null ? null : p.getProperty(key);
            if (v != null) {
                return v;
            }
        }
        return appBundle.apply(key, fallback);
    }

    /** {@link #get} then {@link MessageFormat#format} — {@code {0}} {@code {1}} … Braces that fail to parse return the raw text. */
    public String format(String key, String fallback, Object... args) {
        String text = get(key, fallback);
        if (text == null || args == null || args.length == 0) {
            return text;
        }
        try {
            return new MessageFormat(text).format(args);
        } catch (IllegalArgumentException e) {
            return text;
        }
    }

    /** True when the plugin's own files (not the app bundle) carry the key in the current language chain. */
    public boolean has(String key) {
        if (key == null) {
            return false;
        }
        for (String suffix : candidates(language.get())) {
            Properties p = load(suffix);
            if (p != null && p.getProperty(key) != null) {
                return true;
            }
        }
        return false;
    }

    /** Suffixes to try, most specific first; {@code ""} is the base file. Pure — tested. */
    static List<String> candidates(String languageCode) {
        String code = languageCode == null ? "en" : languageCode.trim();
        List<String> out = new ArrayList<>();
        switch (code) {
            case "zh-TW", "zh_TW", "zh-Hant", "zh_Hant" -> {
                out.add("zh_TW");
                out.add("zh_Hant");
            }
            case "zh-CN", "zh_CN", "zh", "zh-Hans", "zh_Hans" -> {
                out.add("zh_CN");
                out.add("zh");
            }
            case "en" -> {
            }
            default -> out.add(code.replace('-', '_'));
        }
        if (!out.contains("en")) {
            out.add("en");
        }
        out.add("");
        return List.copyOf(out);
    }

    private Properties load(String suffix) {
        return cache.computeIfAbsent(suffix, s -> {
            String name = s.isEmpty() ? "plugin.properties" : "plugin_" + s + ".properties";
            URL url = locate(name);
            if (url == null) {
                return new Properties(); // remembered as empty so the jar is not searched again
            }
            Properties p = new Properties();
            try (InputStream in = url.openStream()) {
                p.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            } catch (Exception e) {
                // 읽다 실패한 파일은 없는 것으로 — 텍스트 하나 때문에 플러그인이 넘어지지 않게.
            }
            return p;
        });
    }

    /** The resource inside this plugin's jar when several jars carry the same file name. */
    private URL locate(String name) {
        try {
            Enumeration<URL> all = loader.getResources(name);
            URL first = null;
            URL firstDir = null;
            while (all.hasMoreElements()) {
                URL u = all.nextElement();
                if (first == null) {
                    first = u;
                }
                if (jarPath != null && belongsToJar(u)) {
                    return u;
                }
                if (firstDir == null && "file".equals(u.getProtocol())) {
                    firstDir = u;
                }
            }
            // 제 jar 를 모를 때(검사 — 플러그인이 클래스 폴더로 컴파일돼 있다)는 폴더의 것을 jar 의 것보다
            // 먼저: 검사 클래스패스에 다른 플러그인 jar(LUMI Chat)가 같이 실려 있으면 그쪽의
            // plugin_ko.properties 가 먼저 잡혀 예제의 문구가 영어로 나왔다(러너에서 실측).
            // 폴더도 없으면 처음 것 — 한 jar 만 있을 때 그것이 곧 제 것이다.
            // A missing translation in our jar must fall through to our next language,
            // never to another plugin's file with the same resource name.
            return jarPath != null ? null : firstDir != null ? firstDir : first;
        } catch (Exception e) {
            return null;
        }
    }

    private boolean belongsToJar(URL resource) {
        try {
            if (!(resource.openConnection() instanceof JarURLConnection connection)) {
                return false;
            }
            URL archive = connection.getJarFileURL();
            return "file".equals(archive.getProtocol())
                    && jarPath.equals(canonical(Path.of(archive.toURI())));
        } catch (Exception ignored) {
            return false;
        }
    }

    private static Path canonical(Path path) {
        try {
            return path.toRealPath();
        } catch (java.io.IOException ignored) {
            return path.toAbsolutePath().normalize();
        }
    }

    @Override
    public String toString() {
        return "PluginText[" + pluginId + "]";
    }
}
