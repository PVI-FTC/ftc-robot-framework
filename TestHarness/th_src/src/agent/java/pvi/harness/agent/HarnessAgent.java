package pvi.harness.agent;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.MemberSubstitution;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

import java.lang.instrument.Instrumentation;

import static net.bytebuddy.matcher.ElementMatchers.any;
import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;
import static net.bytebuddy.matcher.ElementMatchers.nameStartsWith;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

/**
 * Java agent the test JVM loads before any test runs ({@code -javaagent:harness-agent.jar}).
 *
 * <p>As FTC SDK, Pedro and TeamCode classes load, it rewrites their calls to the
 * system clock so they call {@link SimTime} instead. Class files on disk are not changed;
 * only the copy in memory during tests.</p>
 *
 * <ul>
 *   <li>{@code System.nanoTime()} (used by {@code ElapsedTime} and {@code OpMode.getRuntime()})</li>
 *   <li>{@code System.currentTimeMillis()}</li>
 *   <li>{@code android.os.SystemClock} uptime / elapsed-realtime methods</li>
 *   <li>{@code Thread.sleep(ms)} and {@code Thread.yield()}, and inside the SDK's
 *       {@code LinearOpMode} its {@code Thread.isInterrupted()} and {@code Object.wait()} calls,
 *       so a LinearOpMode runs in step with the harness's loops (RP6). On any thread except a
 *       LinearOpMode's own, these still do exactly what they did before.</li>
 * </ul>
 */
public final class HarnessAgent {
    /** The SDK class whose waiting calls become sync points (RP6). */
    private static final String LINEAR_OP_MODE = "com.qualcomm.robotcore.eventloop.opmode.LinearOpMode";

    /** Only static methods here; nothing to create. */
    private HarnessAgent() {
    }

    /**
     * Java runs this before {@code main} (or the tests) when started with
     * {@code -javaagent:harness-agent.jar}. The name "premain" and this exact signature are
     * required by Java.
     *
     * @param arguments       text after "=" in the -javaagent option (unused)
     * @param instrumentation Java's handle for changing classes as they load
     */
    public static void premain(String arguments, Instrumentation instrumentation) throws Exception {
        // 1. Describe the swap: "wherever code calls X, call SimTime's version instead".
        //    relaxed() = don't fail if a class has none of these calls (most don't).
        MemberSubstitution clockSubstitution = MemberSubstitution.relaxed()
                .method(named("nanoTime").and(isDeclaredBy(System.class)).and(takesArguments(0)))
                .replaceWith(SimTime.class.getMethod("nanoTime"))
                .method(named("currentTimeMillis").and(isDeclaredBy(System.class)).and(takesArguments(0)))
                .replaceWith(SimTime.class.getMethod("currentTimeMillis"))
                .method(named("uptimeMillis").and(isDeclaredBy(named("android.os.SystemClock"))))
                .replaceWith(SimTime.class.getMethod("uptimeMillis"))
                .method(named("elapsedRealtime").and(isDeclaredBy(named("android.os.SystemClock"))))
                .replaceWith(SimTime.class.getMethod("uptimeMillis"))
                .method(named("elapsedRealtimeNanos").and(isDeclaredBy(named("android.os.SystemClock"))))
                .replaceWith(SimTime.class.getMethod("elapsedRealtimeNanos"));

        //    LinearOpModes (RP6): sleeping and yielding follow simulated time. SimTime passes
        //    them straight to the real calls on every thread except a LinearOpMode's own.
        MemberSubstitution waitSubstitution = MemberSubstitution.relaxed()
                .method(named("sleep").and(isDeclaredBy(Thread.class)).and(takesArguments(long.class)))
                .replaceWith(SimTime.class.getMethod("sleep", long.class))
                .method(named("yield").and(isDeclaredBy(Thread.class)).and(takesArguments(0)))
                .replaceWith(SimTime.class.getMethod("yieldThread"));
        //    Inside the SDK's LinearOpMode only: isStarted() and isStopRequested() ask
        //    Thread.isInterrupted(), and waitForStart() calls Object.wait(). These become the
        //    sync points where simulated time moves on.
        MemberSubstitution linearOpModeSubstitution = MemberSubstitution.relaxed()
                .method(named("isInterrupted").and(isDeclaredBy(Thread.class)).and(takesArguments(0)))
                .replaceWith(SimTime.class.getMethod("isInterrupted", Thread.class))
                .method(named("wait").and(isDeclaredBy(Object.class)).and(takesArguments(0)))
                .replaceWith(SimTime.class.getMethod("waitOn", Object.class));

        // 2. Apply the swap to robot classes as they load.
        new AgentBuilder.Default()
                // Print a message if a class can't be changed (silent otherwise).
                .with(AgentBuilder.Listener.StreamWriting.toSystemError().withErrorsOnly())
                // Never change the harness itself (SimTime must read the real clock) or Byte Buddy.
                .ignore(nameStartsWith("pvi.harness.").or(nameStartsWith("net.bytebuddy.")))
                // Only FTC SDK, Pedro and TeamCode classes.
                .type(robotCode())
                // In every method of those classes, apply the clock swap and the sleep/yield swap,
                // and in the SDK's LinearOpMode also its sync-point swap.
                .transform((builder, type, classLoader, module, protectionDomain) -> {
                    builder = builder.visit(clockSubstitution.on(any())).visit(waitSubstitution.on(any()));
                    if (type.getName().equals(LINEAR_OP_MODE)) {
                        builder = builder.visit(linearOpModeSubstitution.on(any()));
                    }
                    return builder;
                })
                .installOn(instrumentation);

        // 3. Let the harness know time is now under its control.
        SimTime.markAgentActive();
    }

    /** Packages whose time calls should follow the simulated clock. */
    private static ElementMatcher.Junction<TypeDescription> robotCode() {
        return nameStartsWith("org.firstinspires.ftc.")
                .or(nameStartsWith("com.qualcomm."))
                .or(nameStartsWith("com.pedropathing."));
    }
}
