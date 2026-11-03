package pvi.harness.agent;

import java.util.function.LongSupplier;

/**
 * The clock robot code sees while running in the harness.
 *
 * <p>{@link HarnessAgent} rewrites calls such as {@code System.nanoTime()} inside FTC SDK,
 * Pedro and TeamCode classes to call the methods here instead. When a harness run has
 * installed its simulated clock, these return simulated time; otherwise they fall back
 * to the real clock, so code outside a run behaves normally.</p>
 */
public final class SimTime {
    /**
     * Added to simulated nanoseconds so robot code never sees a time of exactly 0,
     * which some code treats as "not set yet".
     */
    public static final long NANO_BASE = 1_000_000_000_000L;

    /** Wall-clock time at simulated time 0: 2026-01-01 00:00:00 UTC. */
    public static final long EPOCH_MILLIS_BASE = 1_767_225_600_000L;

    /**
     * Where simulated time comes from (the running OpModeRunner's clock), or null for real time.
     * "volatile" makes a change visible to every thread at once.
     */
    private static volatile LongSupplier simulatedNanos;
    /** Set by HarnessAgent once it has installed the clock swap. */
    private static volatile boolean agentActive;
    /**
     * Where a LinearOpMode's waiting calls go (the runner's {@code LinearOpModeDriver}), or null.
     * Only calls made on that OpMode's own thread use it; every other thread gets the real call.
     */
    private static volatile OpModeThreadHooks opModeThreadHooks;

    /** Only static methods here; nothing to create. */
    private SimTime() {
    }

    /** Makes robot code see {@code nanos} (simulated nanoseconds since INIT). */
    public static void install(LongSupplier nanos) {
        simulatedNanos = nanos;
    }

    /** Goes back to real time. */
    public static void uninstall() {
        simulatedNanos = null;
    }

    /** True while a run's simulated clock is installed. */
    public static boolean isSimulated() {
        return simulatedNanos != null;
    }

    /** True when the agent was loaded, so robot code's time calls really come here. */
    public static boolean isAgentActive() {
        return agentActive;
    }

    /** Called once by HarnessAgent.premain. */
    static void markAgentActive() {
        agentActive = true;
    }

    // ---- Replacements for the real time methods -----------------------------------------

    /** Replaces {@code System.nanoTime()}. */
    public static long nanoTime() {
        LongSupplier source = simulatedNanos;
        return source == null ? System.nanoTime() : NANO_BASE + source.getAsLong();
    }

    /** Replaces {@code System.currentTimeMillis()}. */
    public static long currentTimeMillis() {
        LongSupplier source = simulatedNanos;
        return source == null
                ? System.currentTimeMillis()
                : EPOCH_MILLIS_BASE + source.getAsLong() / 1_000_000L;
    }

    /** Replaces Android's {@code SystemClock.uptimeMillis()} and {@code elapsedRealtime()}. */
    public static long uptimeMillis() {
        return nanoTime() / 1_000_000L;
    }

    /** Replaces Android's {@code SystemClock.elapsedRealtimeNanos()}. */
    public static long elapsedRealtimeNanos() {
        return nanoTime();
    }

    // ---- LinearOpMode support (RP6) -------------------------------------------------------

    /**
     * What a LinearOpMode's thread does at the SDK's waiting calls, so {@code runOpMode()} runs
     * in step with the harness's loops. {@code pvi.harness.runtime.LinearOpModeDriver} implements
     * it.
     */
    public interface OpModeThreadHooks {
        /** True when the calling thread is the LinearOpMode's own thread. */
        boolean isOpModeThread();

        /** {@code isStarted()} or {@code isStopRequested()} is asking whether something happened yet. */
        void check();

        /** {@code idle()} (which calls {@code Thread.yield()}): let one loop pass. */
        void idle();

        /** {@code Thread.sleep(millis)}: let {@code millis} of simulated time pass. */
        void sleep(long millis) throws InterruptedException;

        /** {@code waitForStart()} is waiting on {@code lock} for PLAY: let one loop pass. */
        void waitOn(Object lock) throws InterruptedException;
    }

    /** Sends the LinearOpMode thread's waiting calls to {@code hooks} (one LinearOpMode at a time). */
    public static void installOpModeThreadHooks(OpModeThreadHooks hooks) {
        opModeThreadHooks = hooks;
    }

    /** The hooks, if the calling thread is a LinearOpMode's own thread; otherwise null. */
    private static OpModeThreadHooks hooksForThisThread() {
        OpModeThreadHooks hooks = opModeThreadHooks;
        return hooks != null && hooks.isOpModeThread() ? hooks : null;
    }

    /** Replaces {@code Thread.sleep(millis)} in robot code. */
    public static void sleep(long millis) throws InterruptedException {
        OpModeThreadHooks hooks = hooksForThisThread();
        if (hooks == null) {
            Thread.sleep(millis);
        } else {
            hooks.sleep(millis);
        }
    }

    /** Replaces {@code Thread.yield()} in robot code (the SDK's {@code idle()} calls it). */
    public static void yieldThread() {
        OpModeThreadHooks hooks = hooksForThisThread();
        if (hooks == null) {
            Thread.yield();
        } else {
            hooks.idle();
        }
    }

    /**
     * Replaces {@code thread.isInterrupted()} inside the SDK's {@code LinearOpMode}, where
     * {@code isStarted()} and {@code isStopRequested()} call it.
     */
    public static boolean isInterrupted(Thread thread) {
        OpModeThreadHooks hooks = hooksForThisThread();
        if (hooks != null) {
            hooks.check();
        }
        return thread.isInterrupted();
    }

    /** Replaces {@code lock.wait()} inside the SDK's {@code LinearOpMode}, where {@code waitForStart()} calls it. */
    public static void waitOn(Object lock) throws InterruptedException {
        OpModeThreadHooks hooks = hooksForThisThread();
        if (hooks == null) {
            lock.wait();
        } else {
            hooks.waitOn(lock);
        }
    }
}
