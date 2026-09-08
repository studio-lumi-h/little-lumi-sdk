import com.group_finity.mascot.lumi.plugin.PluginTestKit;
import hello.HelloPlugin;

import java.nio.file.Path;

/**
 * A plugin test that needs no app, no window and no Steam: {@link PluginTestKit} hands the plugin a
 * context whose calls are recorded and whose readings are fakes. Plain {@code main} — the SDK ships no
 * test framework; {@code python tools\run_tests.py examples\hello} compiles and runs every
 * {@code test\*.java} of a plugin folder against {@code lib\*.jar} + the plugin's own jar.
 * <p>
 * The kit reads {@code plugin.json} from {@code -Dplugin.dir} (default: the working directory).
 */
public final class HelloPluginTest {
    private static int checks;
    private static int failures;

    public static void main(String[] args) throws Exception {
        Path dir = Path.of(System.getProperty("plugin.dir", "."));
        try (PluginTestKit kit = PluginTestKit.start(dir.resolve("plugin.json"))) {
            kit.language("ko");
            new HelloPlugin().start(kit.context());

            // 1. settings defaults from plugin.json were seeded into prefs()
            check("greet defaults to true", kit.context().prefs().getBoolean("greet", false));
            check("mood defaults to calm", "calm".equals(kit.context().prefs().get("mood", "")));

            // 2. ready → one greeting, in the plugin's own Korean text
            kit.publish("ready");
            check("one say after ready", kit.calls("say").size() == 1);
            check("greeting comes from plugin_ko.properties",
                    "루바! (플러그인)".equals(String.valueOf(kit.lastCall("say").arg(1))));

            // 3. the setting switches the greeting off
            kit.context().prefs().set("greet", false);
            kit.publish("ready");
            check("no second say when greet=false", kit.calls("say").size() == 1);

            // 4. hooks
            check("speech.greeting is tagged", "[hello] 안녕".equals(kit.ask("speech.greeting", "안녕")));
            check("an empty greeting is left alone", "".equals(kit.ask("speech.greeting", "")));
            check("focus.quiet passes by default", Boolean.FALSE.equals(kit.ask("focus.quiet", Boolean.FALSE)));

            // 5. menu rows: one plain row + one toggle, labels from the plugin's text
            check("two tray rows", kit.calls("addTrayItem").size() == 1 && kit.calls("addTrayToggle").size() == 1);
            check("tray label is Korean", "Hello LUMI: 인사하기".equals(kit.lastCall("addTrayItem").arg(0)));

            // 6. click counting speaks every 10th click
            for (int i = 0; i < 10; i++) {
                kit.publish("click", "button", "left", "mascotId", 1, "x", 0, "y", 0);
            }
            check("10th click speaks", kit.calls("say").size() == 2
                    && "10번째 클릭이에요.".equals(String.valueOf(kit.lastCall("say").arg(1))));

            // 7. the scheduler is the app's: nothing runs until the clock moves
            check("every() registered", kit.calls("every").size() == 1);
            kit.tick(59_999);
            kit.tick(1);
            check("clock moved", kit.now() == 60_000);
        }
        System.out.println(failures == 0 ? "ALL PASS (" + checks + " checks)" : failures + " FAILED of " + checks);
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void check(String label, boolean ok) {
        checks++;
        if (!ok) {
            failures++;
        }
        System.out.println((ok ? "  [ok]   " : "  [FAIL] ") + label);
    }
}
