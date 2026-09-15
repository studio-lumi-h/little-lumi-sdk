package com.group_finity.mascot.lumi.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 플러그인 하나의 <b>제 설정 저장소</b> — {@code plugindata\<id>\settings.properties}.
 * <p>
 * 앱의 {@code conf\settings.properties} 는 앱 것이다 — 플러그인은 {@link PluginContext#setting}
 * 으로 <i>읽기</i>만 하고 제 상태는 여기에 둔다. 모드 탭의 선언형 설정 카드
 * ({@link PluginSettingsSchema}, {@code plugin.json} 의 {@code "settings"})도 같은 파일에 쓰므로,
 * 사용자가 카드에서 바꾼 값과 플러그인이 코드로 쓴 값이 한 곳에 산다.
 * <ul>
 *   <li>느리게 읽는다 — 처음 묻는 순간 파일을 읽고, 없으면 빈 것.</li>
 *   <li>{@link #set} 은 변경 시 동기 저장을 시도한다. 임시 파일을 쓴 뒤 원자적 이동을 시도하며,
 *       지원되지 않으면 일반 교체한다. 실패는 로그에 남고 호출자에게 예외를 전달하지 않는다.
 *       값이 같으면 아무 일도 없다. 평문 저장소이므로 비밀 저장용으로 사용하지 않는다.</li>
 *   <li>{@link #onChange} 는 값이 <b>실제로 바뀐</b> 뒤 <b>EDT 밖</b>의 플러그인들이 공유하는 설정 알림 스레드에서
 *       차례로 불린다 — 모드 탭이 EDT 에서 값을 바꾸므로, 느린 리스너가 설정 창을 얼릴 수
 *       없어야 한다. 던지는 리스너는 로그에 남고 건너뛴다.</li>
 *   <li>인스턴스는 id 마다 하나({@link #of}) — 플러그인의 {@code ctx.prefs()} 와 모드 탭이 같은
 *       것을 쥔다.</li>
 * </ul>
 * {@link #onChange} 리스너는 컨텍스트가 자동 해제하지 않는다.
 * 플러그인의 {@code stop()}에서 {@link #removeOnChange}로 직접 해제한다.
 */
public final class PluginPrefs {
    private static final Logger log = Logger.getLogger(PluginPrefs.class.getName());
    private static final Map<String, PluginPrefs> INSTANCES = new ConcurrentHashMap<>();
    private static volatile ExecutorService notifier;

    private final String pluginId;
    private final Path file;
    private final Properties values = new Properties();
    private final List<BiConsumer<String, String>> listeners = new CopyOnWriteArrayList<>();
    private boolean loaded;

    private PluginPrefs(String pluginId, Path file) {
        this.pluginId = pluginId;
        this.file = file;
    }

    /** 그 플러그인의 저장소 — {@code plugindata\<id>\settings.properties}. */
    public static PluginPrefs of(String pluginId) {
        String id = pluginId == null ? "" : pluginId.strip();
        return INSTANCES.computeIfAbsent(id,
                k -> new PluginPrefs(k, PluginLoader.DATA_DIR.resolve(k).resolve("settings.properties")));
    }

    /** 검사용 — 파일 자리를 직접 준다. 캐시에 넣지 않는다. */
    static PluginPrefs at(String pluginId, Path file) {
        return new PluginPrefs(pluginId, file);
    }

    public String pluginId() {
        return pluginId;
    }

    /** 이 저장소가 쓰는 파일. */
    public Path file() {
        return file;
    }

    // ── 읽기 ────────────────────────────────────────────────────────────────

    public synchronized String get(String key, String fallback) {
        load();
        String v = key == null ? null : values.getProperty(key);
        return v == null ? fallback : v;
    }

    public int getInt(String key, int fallback) {
        String v = get(key, null);
        if (v == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(v.strip());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public long getLong(String key, long fallback) {
        String v = get(key, null);
        if (v == null) {
            return fallback;
        }
        try {
            return Long.parseLong(v.strip());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public boolean getBoolean(String key, boolean fallback) {
        String v = get(key, null);
        if (v == null) {
            return fallback;
        }
        String t = v.strip();
        if (t.equalsIgnoreCase("true") || t.equals("1") || t.equalsIgnoreCase("on") || t.equalsIgnoreCase("yes")) {
            return true;
        }
        if (t.equalsIgnoreCase("false") || t.equals("0") || t.equalsIgnoreCase("off") || t.equalsIgnoreCase("no")) {
            return false;
        }
        return fallback;
    }

    /** 있는 열쇠 전부, 이름순. */
    public synchronized Set<String> keys() {
        load();
        Set<String> out = new TreeSet<>();
        for (String name : values.stringPropertyNames()) {
            out.add(name);
        }
        return Collections.unmodifiableSet(out);
    }

    public boolean has(String key) {
        return get(key, null) != null;
    }

    // ── 쓰기 ────────────────────────────────────────────────────────────────

    /** 값을 두고 곧바로 저장한다. {@code null} 값은 {@link #remove} 와 같다. 같은 값이면 아무 일도 없다. */
    public void set(String key, String value) {
        if (key == null || key.isBlank()) {
            return;
        }
        if (value == null) {
            remove(key);
            return;
        }
        boolean changed;
        synchronized (this) {
            load();
            String before = values.getProperty(key);
            changed = !value.equals(before);
            if (changed) {
                values.setProperty(key, value);
                save();
            }
        }
        if (changed) {
            notifyChange(key, value);
        }
    }

    public void set(String key, int value) {
        set(key, String.valueOf(value));
    }

    public void set(String key, long value) {
        set(key, String.valueOf(value));
    }

    public void set(String key, boolean value) {
        set(key, String.valueOf(value));
    }

    /** 열쇠를 지우고 저장한다. 없던 열쇠면 아무 일도 없다(알림도 없다). */
    public void remove(String key) {
        if (key == null) {
            return;
        }
        boolean changed;
        synchronized (this) {
            load();
            changed = values.remove(key) != null;
            if (changed) {
                save();
            }
        }
        if (changed) {
            notifyChange(key, null);
        }
    }

    /**
     * 값이 바뀔 때마다 {@code (열쇠, 새 값)} 을 받는다 — 지운 열쇠는 {@code null}. EDT 밖의
     * 저장소 스레드에서 차례로 불린다. 같은 리스너를 두 번 걸면 두 번 불린다.
     */
    public void onChange(BiConsumer<String, String> listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeOnChange(BiConsumer<String, String> listener) {
        listeners.remove(listener);
    }

    /** 지금 값을 파일에 다시 쓴다. {@link #set} 이 이미 저장하므로 보통은 부를 일이 없다. */
    public synchronized void save() {
        load();
        try {
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            try (OutputStream out = Files.newOutputStream(tmp)) {
                values.store(new java.io.OutputStreamWriter(out, StandardCharsets.UTF_8),
                        "Little LUMI plugin " + pluginId + " — settings");
            }
            try {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            log.log(Level.WARNING, "plugin " + pluginId + ": cannot save " + file, e);
        }
    }

    /** 파일을 다시 읽는다(밖에서 고쳤을 때). 바뀐 열쇠마다 알린다. */
    public void reload() {
        List<String[]> diffs = new ArrayList<>();
        synchronized (this) {
            Properties before = new Properties();
            before.putAll(values);
            values.clear();
            loaded = false;
            load();
            Set<String> all = new TreeSet<>(before.stringPropertyNames());
            all.addAll(values.stringPropertyNames());
            for (String k : all) {
                String a = before.getProperty(k);
                String b = values.getProperty(k);
                if (a == null ? b != null : !a.equals(b)) {
                    diffs.add(new String[]{k, b});
                }
            }
        }
        for (String[] d : diffs) {
            notifyChange(d[0], d[1]);
        }
    }

    // ── 속 ──────────────────────────────────────────────────────────────────

    private void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        if (!Files.isRegularFile(file)) {
            return;
        }
        try (InputStream in = Files.newInputStream(file)) {
            values.load(new java.io.InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (IOException e) {
            log.log(Level.WARNING, "plugin " + pluginId + ": cannot read " + file, e);
        }
    }

    private void notifyChange(String key, String value) {
        if (listeners.isEmpty()) {
            return;
        }
        notifier().execute(() -> {
            for (BiConsumer<String, String> l : listeners) {
                try {
                    l.accept(key, value);
                } catch (Throwable t) {
                    log.log(Level.WARNING, "plugin " + pluginId + ": prefs listener failed on '" + key + "'", t);
                }
            }
        });
    }

    private static ExecutorService notifier() {
        ExecutorService e = notifier;
        if (e == null) {
            synchronized (PluginPrefs.class) {
                if (notifier == null) {
                    notifier = Executors.newSingleThreadExecutor(r -> {
                        Thread t = new Thread(r, "lumi-plugin-prefs");
                        t.setDaemon(true);
                        return t;
                    });
                }
                e = notifier;
            }
        }
        return e;
    }
}
