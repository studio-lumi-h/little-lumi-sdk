package sdkexample;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * The code the rewritten method calls into. It must be {@code public} with
 * {@code public static} hooks: {@code FocusDirector} lives in another package and
 * the call is an ordinary {@code invokestatic} — the plugin jar sits on the same
 * (system) class path as the app, so the JVM resolves it like any other class.
 */
public final class FocusPatch {
    private static final Logger log = Logger.getLogger("plugin.lumi.sdk.example.transformer");

    /** Set by the transformer once it has rewritten {@code FocusDirector}. */
    static volatile boolean applied;
    static final AtomicInteger hits = new AtomicInteger();

    /** What Lumi says when focus mode is switched on — replaces the app's own line. */
    public static final String ENGAGED_LINE = "[SDK 예제] 집중 모드! 이 말은 플러그인 변환기가 바꿨어요.";

    private FocusPatch() {
    }

    /** Injected at the top of {@code FocusDirector.setManual(boolean)}. */
    public static void onSetManual(boolean enabled) {
        int n = hits.incrementAndGet();
        log.info("MOD TRANSFORMER HIT: FocusDirector.setManual(" + enabled
                + ") rewritten by lumi.sdk.example.transformer (hit #" + n + ")");
    }
}
