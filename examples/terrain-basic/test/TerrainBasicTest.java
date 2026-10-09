import com.group_finity.mascot.lumi.plugin.PluginTestKit;
import sdkexample.TerrainBasicPlugin;

public final class TerrainBasicTest {
    public static void main(String[] args) throws Exception {
        try (PluginTestKit kit = PluginTestKit.start("lumi.sdk.example.terrain")) {
            TerrainBasicPlugin plugin = new TerrainBasicPlugin();
            plugin.start(kit.context());
            if (kit.calls("addTrayItem").size() != 3) throw new AssertionError("expected three API demo menus");
            if (!kit.calls("registerTerrain").isEmpty()) throw new AssertionError("terrain appeared before Show");
            plugin.stop();
            javax.swing.SwingUtilities.invokeAndWait(() -> { });
            if (!kit.calls("registerTerrain").isEmpty()) throw new AssertionError("terrain appeared after stop");
        }
        System.out.println("ALL PASS (single platform example startup/stop)");
    }
}
