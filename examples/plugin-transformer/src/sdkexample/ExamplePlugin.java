package sdkexample;

import com.group_finity.mascot.lumi.FocusDirector;
import com.group_finity.mascot.lumi.plugin.LumiPlugin;
import com.group_finity.mascot.lumi.plugin.PluginContext;

import java.nio.file.Files;
import java.nio.file.Path;

import javax.swing.SwingUtilities;

/**
 * The plugin half of the example: reports whether the transformer got to
 * {@code FocusDirector} in time, listens to the {@code focus} event, and adds a
 * tray row that toggles focus mode so a person can see the patched greeting.
 * <p>
 * Self-test (for a test harness, no clicking needed): create an empty file
 * {@code plugindata\lumi.sdk.example.transformer\selftest.txt} before starting the
 * app; once the engine is ready the plugin switches focus mode on and off by
 * itself, which drives the patched method twice and leaves
 * {@code MOD TRANSFORMER HIT} lines in the log.
 */
public final class ExamplePlugin implements LumiPlugin {

    @Override
    public void start(PluginContext ctx) throws Exception {
        ctx.log().info("focus transformer applied=" + FocusPatch.applied
                + (FocusPatch.applied ? "" : " (FocusDirector loaded before plugins attached?)"));
        ctx.on("focus", event -> ctx.log().info("focus event: active=" + event.get("active")));
        ctx.addTrayItem("SDK example: toggle focus (patched greeting)", () -> FocusDirector.toggleManual());

        Path flag = ctx.dataDir().resolve("selftest.txt");
        if (Files.exists(flag)) {
            ctx.on("ready", event -> selfTest(ctx));
        }
    }

    private static void selfTest(PluginContext ctx) {
        Thread t = new Thread(() -> {
            try {
                Thread.sleep(3000);
                SwingUtilities.invokeAndWait(() -> FocusDirector.setManual(true));
                Thread.sleep(3000);
                SwingUtilities.invokeAndWait(() -> FocusDirector.setManual(false));
                ctx.log().info("selftest done: patched method ran " + FocusPatch.hits.get() + " time(s)");
            } catch (Exception e) {
                ctx.log().warning("selftest failed: " + e);
            }
        }, "sdk-example-selftest");
        t.setDaemon(true);
        t.start();
    }

    @Override
    public void stop() {
    }
}
