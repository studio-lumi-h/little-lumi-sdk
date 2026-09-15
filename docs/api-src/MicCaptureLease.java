package com.group_finity.mascot.lumi.plugin;

/**
 * Keeps LUMI voice output quiet while this plugin records a microphone.
 * Obtain it off the EDT through {@link PluginContext#beginMicCapture()} before opening the
 * microphone, and close it after closing the input line. Closing twice is harmless; all
 * outstanding leases must close before new voice output is accepted. Plugin shutdown also
 * closes the leases owned by its context. This does not open a microphone or mute other apps.
 */
public final class MicCaptureLease implements AutoCloseable {
    private Runnable release;
    private boolean closed;

    MicCaptureLease() {
    }

    /** Also handles shutdown racing the backend's registration. Never runs callbacks under this lock. */
    void bind(Runnable action) {
        boolean run;
        synchronized (this) {
            if (release != null) {
                throw new IllegalStateException("Microphone lease already registered");
            }
            run = closed;
            if (!run) {
                release = action;
            }
        }
        if (run) {
            action.run();
        }
    }

    synchronized boolean isClosed() {
        return closed;
    }

    /** Releases only this lease. No checked exception, and safe from any thread. */
    @Override
    public void close() {
        Runnable action;
        synchronized (this) {
            if (closed) {
                return;
            }
            closed = true;
            action = release;
            release = null;
        }
        if (action != null) {
            action.run();
        }
    }
}
