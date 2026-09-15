package com.group_finity.mascot.lumi.plugin;

/**
 * A <b>load-time</b> class transformer a plugin ships as a service
 * ({@code META-INF/services/com.group_finity.mascot.lumi.plugin.LumiTransformer}).
 * It is registered by {@link PluginLoader#attach()} — right after the Steam gate,
 * before the engine and most of the app have loaded — so it sees every class
 * that loads from then on and may rewrite it <b>freely</b>: unlike
 * {@link PluginContext#patch} (a retransform of an already-loaded class, which the
 * JVM limits to method bodies), a class that has not loaded yet can gain methods,
 * fields, interfaces, anything. That is the industry-standard "Mixin/ASM at
 * load time" door.
 * <p>
 * The loader wraps every transformer: protected classes
 * ({@link PluginContext#PROTECTED_PREFIXES}) are never offered, a transformer that
 * throws is logged and the class loads unchanged, and a disabled plugin's
 * transformers are not registered at all. ASM 7.3 is on the app's class path
 * ({@code lib\asm-*.jar}), so a transformer needs nothing bundled.
 * <p>
 * Keep {@link #wants} cheap: it is asked for every class the JVM loads.
 */
public interface LumiTransformer {

    /** {@code className} in dotted form. Return true to be handed the bytes. */
    boolean wants(String className);

    /**
     * Return the new class file, or {@code null} to leave the class as it is.
     * Runs on the thread that triggered the load, with the JVM's class-loading
     * lock held — do not touch the app from here.
     */
    byte[] transform(String className, byte[] classfile);
}
