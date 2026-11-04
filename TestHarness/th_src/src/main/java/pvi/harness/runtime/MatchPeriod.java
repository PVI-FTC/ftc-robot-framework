package pvi.harness.runtime;

/** Standard FTC match period lengths. */
public enum MatchPeriod {
    /** The 30-second autonomous period. */
    AUTONOMOUS(30.0),
    /** The 2-minute driver-controlled period. */
    TELEOP(120.0);

    /** Length of the period in seconds. */
    private final double seconds;

    /** Each period is created with its length. */
    MatchPeriod(double seconds) {
        this.seconds = seconds;
    }

    /** Length of the period in seconds (30 or 120). */
    public double seconds() {
        return seconds;
    }
}
