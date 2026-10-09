package com.group_finity.mascot.lumi.plugin;

import java.util.List;

/**
 * A plugin-owned terrain registration, returned by {@link PluginContext#registerTerrain}.
 * Commands may be submitted from any thread. The engine applies the latest complete snapshot
 * before a mascot's next movement tick. Desktop terrain is suspended in broadcast mode.
 * Removing a handle is permanent; register again to make a new one with the same name.
 * The context removes outstanding registrations after the plugin stops or fails to start.
 */
public interface TerrainHandle extends AutoCloseable {
    /**
     * Replaces all pieces. An empty list clears the geometry while keeping the name registered.
     * Returns false after removal or context shutdown. Invalid geometry throws before changing
     * the previous snapshot. This accepts data; it does not assert a character has landed on it.
     */
    boolean update(List<TerrainRect> rects);

    /** Releases this registration and its name. Safe to call repeatedly. */
    void remove();

    /** True once removal has been requested, including a registration refused after stop. */
    boolean isRemoved();

    /** Equivalent to {@link #remove()}. */
    @Override
    default void close() {
        remove();
    }
}
