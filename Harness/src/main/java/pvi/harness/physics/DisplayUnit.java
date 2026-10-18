package pvi.harness.physics;

/**
 * The unit used when <b>showing</b> distances to people (reports, printed poses, the viewer).
 *
 * <p>The harness always <b>calculates</b> in inches, because the FTC SDK, Pedro Pathing and
 * TeamCode all use inches. Converting only when displaying keeps the ÷12 in one place.</p>
 */
public enum DisplayUnit {
    /** Feet: 12 inches each. The default for reports. */
    FEET("ft", 12.0),
    /** Inches: the unit everything is calculated in. */
    INCHES("in", 1.0);

    /** How many inches are in one foot. */
    public static final double INCHES_PER_FOOT = 12.0;

    /** Short label for printing. */
    private final String symbol;
    /** How many inches one of this unit is. */
    private final double inchesPerUnit;

    /** Each unit is created with its label and size in inches. */
    DisplayUnit(String symbol, double inchesPerUnit) {
        this.symbol = symbol;
        this.inchesPerUnit = inchesPerUnit;
    }

    /** Short label for printing, such as "ft". */
    public String symbol() {
        return symbol;
    }

    /** Converts a distance in inches to this unit. */
    public double fromInches(double inches) {
        return inches / inchesPerUnit;
    }

    /** Converts a distance in this unit to inches. */
    public double toInches(double value) {
        return value * inchesPerUnit;
    }
}
