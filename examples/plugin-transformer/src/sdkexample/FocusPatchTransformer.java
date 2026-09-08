package sdkexample;

import com.group_finity.mascot.lumi.plugin.LumiTransformer;

import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.ClassTransform;
import java.lang.classfile.CodeBuilder;
import java.lang.classfile.CodeElement;
import java.lang.classfile.CodeTransform;
import java.lang.classfile.instruction.ConstantInstruction;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;

/**
 * A load-time transformer that rewrites ONE method of a core class with the
 * JDK's standard {@code java.lang.classfile} API (final since JDK 24; the app runs
 * on JDK 25, so nothing needs bundling).
 * <p>
 * Target: {@code FocusDirector.setManual(boolean)} in {@code lib\Shimeji-ee.jar}.
 * Two edits:
 * <ol>
 *   <li>a call to {@link FocusPatch#onSetManual(boolean)} is inserted at the top of the
 *       method (proof in the log that the rewritten body runs);</li>
 *   <li>the greeting the method speaks — {@code SpeechDirector.localized("FocusModeEngaged",
 *       "집중 모드예요. …")} — is redirected: the bundle key becomes one that does not
 *       exist, so the fallback text is used, and that fallback becomes
 *       {@link FocusPatch#ENGAGED_LINE}. Both are plain {@code ldc} constants in the
 *       bytecode, so the edit is a constant swap.</li>
 * </ol>
 * The class is offered to us before it loads (the loader registers every plugin's
 * {@code LumiTransformer} services right after the Steam gate), so we may change
 * anything about it; {@code FocusDirector} is not on the protected list. Keep
 * {@link #wants} cheap — it is asked for every class the JVM loads.
 * <p>
 * A transformer is the LAST resort: it is tied to the shape of one method, so an app
 * update that touches that method breaks it. Reach for an event or a judgement hook
 * first — this very greeting also passes the {@code text.localized} hook, and the
 * toggle is published as the {@code focus} event; the rewrite is here to show the
 * mechanism. What a transformer does buy: several plugins may patch the same class,
 * each seeing the previous result, and a patch of one method survives app updates
 * that leave that method alone.
 */
public final class FocusPatchTransformer implements LumiTransformer {
    static final String TARGET = "com.group_finity.mascot.lumi.FocusDirector";
    private static final ClassDesc HOOK = ClassDesc.of("sdkexample.FocusPatch");

    @Override
    public boolean wants(String className) {
        return TARGET.equals(className);
    }

    @Override
    public byte[] transform(String className, byte[] classfile) {
        ClassFile cf = ClassFile.of();
        ClassModel model = cf.parse(classfile);
        CodeTransform patch = new CodeTransform() {
            @Override
            public void atStart(CodeBuilder cb) {
                // static method: local 0 is the boolean parameter
                cb.iload(0);
                cb.invokestatic(HOOK, "onSetManual", MethodTypeDesc.ofDescriptor("(Z)V"));
            }

            @Override
            public void accept(CodeBuilder cb, CodeElement e) {
                if (e instanceof ConstantInstruction.LoadConstantInstruction ldc
                        && ldc.constantValue() instanceof String s) {
                    if ("FocusModeEngaged".equals(s)) {
                        cb.ldc("SdkExampleFocusEngaged"); // no such key → fallback text is spoken
                        return;
                    }
                    if (s.startsWith("집중 모드예요")) {
                        cb.ldc(FocusPatch.ENGAGED_LINE);
                        return;
                    }
                }
                cb.with(e);
            }
        };
        byte[] out = cf.transformClass(model, ClassTransform.transformingMethodBodies(
                mm -> mm.methodName().equalsString("setManual") && mm.methodType().equalsString("(Z)Z"),
                patch));
        FocusPatch.applied = true;
        return out;
    }
}
