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
}
