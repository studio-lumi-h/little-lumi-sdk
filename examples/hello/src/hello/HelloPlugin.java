package hello;

import com.group_finity.mascot.lumi.plugin.LumiPlugin;
import com.group_finity.mascot.lumi.plugin.PluginContext;
import com.group_finity.mascot.lumi.plugin.PluginPrefs;

import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

/**
 * The smallest useful plugin: greets when the engine is ready, logs behaviour
 * changes, counts clicks, hangs two rows in the tray menu, answers two hooks and
 * leaves a file in its own data folder ({@code plugindata\<id>\started.txt}) so you
 * can see at a glance that it ran.
 * <p>
 * Everything a plugin can do starts from {@link PluginContext}; anything public in
 * the app's jar is reachable too (the plugin is on the same class path). The
 * load-time transformer next to this class ({@code HelloTransformer}) shows the
 * last-resort door: rewriting a core class before it loads. Prefer hooks — see
 * {@link PluginContext#hook}, {@code docs/API.md} §5 and the hook table in {@code docs/CATALOG.md}.
 */
public final class HelloPlugin implements LumiPlugin {
    private final AtomicInteger clicks = new AtomicInteger();
    private final AtomicBoolean quietSeen = new AtomicBoolean();
    private PluginPrefs prefs;
    private BiConsumer<String, String> settingsChanged;

    @Override
    public void start(PluginContext ctx) throws Exception {
        ctx.log().info("Hello from " + ctx.id() + " (data folder: " + ctx.dataDir() + ")");
        Files.writeString(ctx.dataDir().resolve("started.txt"), "ok");

        ctx.on("behavior", event -> ctx.log().fine(event.get("imageSet") + " -> " + event.get("name")));
        ctx.on("ready", event -> {
            if (ctx.prefs().getBoolean("greet", true)) {
                // ctx.text = this plugin's own files (src/plugin_<lang>.properties — ko and en ship
                // here, every other language falls back to en); ctx.localized would be the app's bundle.
                ctx.say(ctx.text("greeting", "Luba! (plugin)"), 4000);
            }
        });
        ctx.on("click", event -> {
            int n = clicks.incrementAndGet();
            if (n % 10 == 0) {
                ctx.say(ctx.text("counted", "Click number {0}.", n), 3000);
            }
        });
        ctx.on("headpat", event -> ctx.log().info("head pat during " + event.get("behavior")));

        // Hooks answer BEFORE the app decides. Return null to pass; the value so far is
        // `current`; handlers run in Mods-list order and the lower one wins.
        // focus.quiet — asked several times a second, so keep it constant-time and log once.
        ctx.hook("focus.quiet", (hook, current, c) -> {
            if (quietSeen.compareAndSet(false, true)) {
                ctx.log().info("HOOK HIT focus.quiet current=" + current + " ctx=" + c);
            }
            // -Dhello.quiet=true makes Lumi keep quiet no matter what the app thinks.
            return Boolean.getBoolean("hello.quiet") ? Boolean.TRUE : null;
        });
        // speech.greeting — tag every greeting; "" would mean "no greeting", so leave that alone.
        ctx.hook("speech.greeting", (hook, current, c) -> {
            String line = String.valueOf(current);
            return line.isEmpty() ? null : "[hello] " + line;
        });

        // Settings: declared in plugin.json ("settings"), drawn by the Mods tab, stored in
        // ctx.prefs() (plugindata\<id>\settings.properties). Read them; listen for edits.
        // This listener is the ONE registration the context does not reclaim, so keep it and take
        // it off again in stop() (docs/API.md §2).
        prefs = ctx.prefs();
        settingsChanged = (key, value) -> ctx.log().info("setting " + key + " -> " + value);
        prefs.onChange(settingsChanged);

        // A row in the tray menu — the greeting follows the "mood" setting.
        ctx.addTrayItem(ctx.text("tray.hi", "Hello LUMI: say hi"), () -> ctx.say(
                "loud".equals(ctx.prefs().get("mood", "calm"))
                        ? ctx.text("hi.loud", "HI FROM THE TRAY!!") : ctx.text("hi.calm", "Hi from the tray!"), 3000));
        // A toggle row: label stays, a 켜짐/꺼짐 chip shows the state; the handle can relabel or remove it.
        ctx.addTrayToggle(ctx.text("tray.greet", "Hello LUMI: greet on ready"), ctx.prefs().getBoolean("greet", true),
                on -> ctx.prefs().set("greet", on));
        // A timer without a Timer: the app's scheduler, released for you after stop().
        ctx.every(60_000, 60_000, () -> ctx.log().fine("clicks so far: " + clicks.get()));
    }

    @Override
    public void stop() {
        // Events, hooks, tray rows and timers are released for us; the prefs listener is not.
        if (prefs != null && settingsChanged != null) {
            prefs.removeOnChange(settingsChanged);
            settingsChanged = null;
        }
    }
}
