package hello;

import com.group_finity.mascot.lumi.plugin.LumiTransformer;

import java.util.logging.Logger;

/**
 * A load-time transformer — registered by the loader right after the Steam gate,
 * before the engine loads, through
 * {@code META-INF/services/com.group_finity.mascot.lumi.plugin.LumiTransformer}.
 * This one only watches: it asks for the engine's {@code Manager} class and hands
 * it back unchanged, logging that it was offered. Rewrite the bytes with ASM
 * ({@code lib\asm-*.jar} is on the class path) to actually change the class —
 * anything goes here, including new methods and fields, because the class has
 * not loaded yet.
 */
public final class HelloTransformer implements LumiTransformer {
    private static final Logger log = Logger.getLogger("plugin.hello.lumi.sample");

    @Override
    public boolean wants(String className) {
        return className.equals("com.group_finity.mascot.Manager");
    }

    /**
     * Deliberately a no-op. A transformer that really rewrites a method — with the
     * JDK's {@code java.lang.classfile} API, no library — is the mod SDK's example
     * {@code examples/plugin-transformer}: it patches {@code FocusDirector.setManual}
     * at load time.
     */
    @Override
    public byte[] transform(String className, byte[] classfile) {
        log.info("HelloTransformer saw " + className + " (" + classfile.length + " bytes) — left unchanged");
        return null;
    }
}
