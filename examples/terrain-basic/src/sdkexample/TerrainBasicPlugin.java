package sdkexample;

import com.group_finity.mascot.lumi.plugin.LumiPlugin;
import com.group_finity.mascot.lumi.plugin.PluginContext;
import com.group_finity.mascot.lumi.plugin.TerrainHandle;
import com.group_finity.mascot.lumi.plugin.TerrainRect;
import java.awt.Color;
import java.awt.Rectangle;
import java.util.List;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;

/** One visible platform: register it, change its width, then remove it. */
public final class TerrainBasicPlugin implements LumiPlugin {
    private PluginContext ctx;
    private volatile TerrainHandle terrain;
    private volatile boolean stopped;
    // Window and width state belong to the EDT.
    private JWindow platform;
    private int fullWidth;

    @Override
    public void start(PluginContext context) {
        ctx = context;
        ctx.addTrayItem(ctx.text("terrain.show", "SDK: show platform"), () -> ctx.onEdt(this::show));
        ctx.addTrayItem(ctx.text("terrain.resize", "SDK: change platform width"), () -> ctx.onEdt(this::resize));
        ctx.addTrayItem(ctx.text("terrain.remove", "SDK: remove platform"), () -> ctx.onEdt(this::remove));
    }

    private void show() {
        if (stopped || platform != null || ctx.featureEnabled("broadcast")) return;
        List<Rectangle> screens = ctx.screens();
        if (screens.isEmpty()) return;
        Rectangle screen = screens.getFirst();
        if (screen.width < 80 || screen.height < 200) return;
        fullWidth = Math.min(240, screen.width - 40);
        // An untitled window paints the bar; the SDK supplies its terrain separately.
        platform = new JWindow();
        platform.setAlwaysOnTop(true);
        platform.getContentPane().setBackground(new Color(68, 183, 124));
        platform.setBounds(screen.x + (screen.width - fullWidth) / 2,
                screen.y + Math.max(160, screen.height / 2), fullWidth, 8);
        terrain = ctx.registerTerrain("platform", geometry());
        if (stopped || terrain.isRemoved()) { remove(); return; }
        platform.setVisible(true);
    }

    private List<TerrainRect> geometry() {
        Rectangle r = platform.getBounds(); // Desktop logical coordinates.
        return List.of(new TerrainRect("bar", r.x, r.y, r.x + r.width, r.y + r.height));
    }

    private void resize() {
        if (stopped || platform == null) return;
        int width = platform.getWidth() == fullWidth ? fullWidth / 2 : fullWidth;
        platform.setSize(width, 8);
        terrain.update(geometry()); // Keep the piece ID while replacing its bounds.
    }

    private void remove() {
        if (terrain != null) terrain.remove();
        if (platform != null) platform.dispose();
        terrain = null;
        platform = null;
    }

    @Override
    public void stop() {
        stopped = true;
        TerrainHandle active = terrain;
        if (active != null) active.remove();
        SwingUtilities.invokeLater(this::remove);
    }
}
