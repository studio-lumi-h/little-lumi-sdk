package com.group_finity.mascot.lumi.plugin;

/**
 * A scheduled task's handle — what {@link PluginContext#every} and {@link PluginContext#after}
 * return. {@link #cancel} stops it; a periodic task that is cancelled from
 * inside its own run finishes that run and is not called again. Every handle a plugin still
 * holds when the app exits is cancelled by the loader after {@code stop()}.
 */
public interface ScheduledHandle {

    /** Stops the task. Safe to call twice; safe from inside the task itself. */
    void cancel();

    boolean isCancelled();
}
