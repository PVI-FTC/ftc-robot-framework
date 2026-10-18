package pvi.harness.physics;

import java.util.Locale;

/**
 * Where the robot is on the field and which way it faces.
 *
 * <p><b>Coordinates (team decision, Step 0):</b> FTC field frame. The origin (0, 0) is the
 * center of the field; x and y run from -72 to +72 inches (-6 to +6 ft). Heading is
 * measured counter-clockwise from the +x direction, the same convention the FTC SDK and
 * Pedro Pathing use.</p>
 *
 * <p><b>Units:</b> stored in inches and radians, because that is what the robot code and
 * the math use. Use {@link #xFeet()}, {@link #headingDegrees()} or {@link #toString()} to
 * read it in friendlier units.</p>
 *
 * <p>A Pose never changes once created. Moving the robot means making a new Pose.</p>
 *
 * @param xInches        x position in inches
 * @param yInches        y position in inches
 * @param headingRadians heading in radians, always kept between -π (exclusive) and +π
 */
public record Pose(double xInches, double yInches, double headingRadians) {

    /** Half the field width: the field runs from -72 to +72 inches in x and y. */
    public static final double FIELD_HALF_WIDTH_INCHES = 72.0;

    /** Field center, facing +x. The default starting pose (team decision, Step 0). */
    public static final Pose ORIGIN = new Pose(0.0, 0.0, 0.0);

    /**
     * Checks the numbers and keeps the heading in one standard range, so 370° and 10°
     * become the same Pose.
     */
    public Pose {
        if (!Double.isFinite(xInches) || !Double.isFinite(yInches) || !Double.isFinite(headingRadians)) {
            throw new IllegalArgumentException(String.format(Locale.ROOT,
                    "A pose needs real numbers, got x=%s in, y=%s in, heading=%s rad",
                    xInches, yInches, headingRadians));
        }
        headingRadians = normalizeRadians(headingRadians);
    }

    // ---- Making poses ----------------------------------------------------------------------

    /** A pose from inches and degrees, e.g. {@code Pose.inches(24, 0, 90)}. */
    public static Pose inches(double xInches, double yInches, double headingDegrees) {
        return new Pose(xInches, yInches, Math.toRadians(headingDegrees));
    }

    /** A pose from feet and degrees, e.g. {@code Pose.feet(2, 0, 90)}. */
    public static Pose feet(double xFeet, double yFeet, double headingDegrees) {
        return inches(DisplayUnit.FEET.toInches(xFeet), DisplayUnit.FEET.toInches(yFeet), headingDegrees);
    }

    // ---- Reading in other units ------------------------------------------------------------

    /** x in feet. */
    public double xFeet() {
        return DisplayUnit.FEET.fromInches(xInches);
    }

    /** y in feet. */
    public double yFeet() {
        return DisplayUnit.FEET.fromInches(yInches);
    }

    /** Heading in degrees, between -180 (exclusive) and +180. */
    public double headingDegrees() {
        return Math.toDegrees(headingRadians);
    }

    /** x in any display unit. */
    public double x(DisplayUnit unit) {
        return unit.fromInches(xInches);
    }

    /** y in any display unit. */
    public double y(DisplayUnit unit) {
        return unit.fromInches(yInches);
    }

    // ---- Comparing poses -------------------------------------------------------------------

    /** Straight-line distance to another pose, in inches (heading is ignored). */
    public double distanceInchesTo(Pose other) {
        return Math.hypot(other.xInches - xInches, other.yInches - yInches);
    }

    /** Straight-line distance to another pose, in feet. */
    public double distanceFeetTo(Pose other) {
        return DisplayUnit.FEET.fromInches(distanceInchesTo(other));
    }

    /**
     * The smallest turn from this heading to the other one, in degrees: positive means
     * counter-clockwise. From 170° to -170° is +20°, not -340°.
     */
    public double headingDifferenceDegreesTo(Pose other) {
        return Math.toDegrees(normalizeRadians(other.headingRadians - headingRadians));
    }

    /** True when the center of the robot is on the 12 × 12 ft field (edges count as on). */
    public boolean isOnField() {
        return Math.abs(xInches) <= FIELD_HALF_WIDTH_INCHES && Math.abs(yInches) <= FIELD_HALF_WIDTH_INCHES;
    }

    // ---- Printing ----------------------------------------------------------------------------

    /**
     * Feet, two decimals: {@code x=0.66 ft, y=0.00 ft, heading=0.0 deg}. Uses "deg" rather
     * than the ° symbol, which Windows PowerShell shows as garbled characters.
     */
    @Override
    public String toString() {
        return toString(DisplayUnit.FEET);
    }

    /** Like {@link #toString()} but in the unit you choose. */
    public String toString(DisplayUnit unit) {
        // Locale.ROOT: always "0.66", never "0,66", whatever language the PC is set to.
        return String.format(Locale.ROOT, "x=%.2f %s, y=%.2f %s, heading=%.1f deg",
                tidy(x(unit), 2), unit.symbol(), tidy(y(unit), 2), unit.symbol(), tidy(headingDegrees(), 1));
    }

    // ---- Helpers -----------------------------------------------------------------------------

    /** Wraps any angle into the range -π (exclusive) to +π. */
    static double normalizeRadians(double radians) {
        double wrapped = radians % (2 * Math.PI);
        if (wrapped <= -Math.PI) {
            wrapped += 2 * Math.PI;
        } else if (wrapped > Math.PI) {
            wrapped -= 2 * Math.PI;
        }
        return wrapped;
    }

    /** Turns tiny values like -0.0001 into 0 so they print as "0.00" instead of "-0.00". */
    static double tidy(double value, int decimals) {
        double smallestShown = 0.5 * Math.pow(10, -decimals);
        return Math.abs(value) < smallestShown ? 0.0 : value;
    }
}
