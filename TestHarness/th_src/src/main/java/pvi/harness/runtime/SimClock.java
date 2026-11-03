package pvi.harness.runtime;

/**
 * Simulated time for one harness run.
 *
 * <p>Time only moves when the runner advances it (one loop period per OpMode call), so
 * every run of a test sees exactly the same timeline no matter how fast the PC is.</p>
 *
 * <p>Robot code sees this clock too: the harness Java agent redirects
 * {@code System.nanoTime()} (and so {@code ElapsedTime}) in FTC SDK, Pedro and TeamCode
 * classes to {@link pvi.harness.agent.SimTime}, which reads this clock during a run.</p>
 */
public class SimClock {
    /** Simulated nanoseconds since INIT. Whole numbers, so time never drifts from rounding. */
    private long nanos;
    /** The value of nanos when PLAY was pressed; -1 until then. */
    private long startNanos = -1;

    /** Seconds since INIT was pressed. */
    public double sinceInit() {
        return nanos / 1e9;
    }

    /** Seconds since PLAY was pressed, or 0 before PLAY. */
    public double sinceStart() {
        return startNanos < 0 ? 0.0 : (nanos - startNanos) / 1e9;
    }

    /** True once PLAY has been pressed. */
    public boolean hasStarted() {
        return startNanos >= 0;
    }

    /** Nanoseconds since INIT; this is what robot code sees (through SimTime) as System.nanoTime(). */
    public long nanoTime() {
        return nanos;
    }

    /** Moves time forward. The runner calls this once per OpMode call (one loop period). */
    public void advance(double seconds) {
        if (seconds < 0) {
            throw new IllegalArgumentException("Time cannot go backwards: " + seconds);
        }
        nanos += Math.round(seconds * 1e9);
    }

    /** Marks the moment PLAY is pressed. */
    void markStart() {
        if (startNanos < 0) {
            startNanos = nanos;
        }
    }
}
