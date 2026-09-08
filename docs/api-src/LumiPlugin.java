package com.group_finity.mascot.lumi.plugin;

/**
 * Entry point of a Little LUMI plugin.
 * <p>
 * A plugin is a jar in {@code plugins\} (a mod drops it there through the launcher's
 * {@code mods\<name>\plugins\} overlay, a Workshop item through its own
 * {@code plugins\} folder) that carries a {@code plugin.json} at its root and names
 * its implementation of this interface in
 * {@code META-INF/services/com.group_finity.mascot.lumi.plugin.LumiPlugin}.
 * <p>
 * The plugin runs <b>inside the app</b>, on the same class path as every core class,
 * with the user's own rights. Anything public in the jar is reachable, and
 * {@link PluginContext} is the curated front door — the parts of the app most plugins
 * need. {@link PluginContext#API_VERSION} identifies the current API: stable items are only
 * added within one API_VERSION, preview items may change without a bump (see the SDK's
 * API.md §8), and a bump keeps plugins written for the previous version (N-1) loading.
 * <p>
 * The loader calls {@link #start} once, after built-in features boot and before
 * the engine loads mascots, and {@link #stop} when the app exits. If {@code start()} throws, the loader
 * also calls {@code stop()} immediately to clean up the partial start, then releases
 * context registrations. A throwable out of either is logged and confined to that
 * plugin; unrelated plugins carry on. Stop implementations should tolerate a partial
 * start. Services and plugins stop in reverse start order. Close plugin-owned resources
 * and remove {@link PluginPrefs#onChange} listeners in {@code stop()}; the context only
 * releases the registrations it tracks.
 */
public interface LumiPlugin {

    void start(PluginContext context) throws Exception;

    default void stop() {
    }
}
