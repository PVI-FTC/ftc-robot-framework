package pvi.harness.physics;

import java.util.Locale;

/**
 * One number describing the real robot, plus where it came from.
 *
 * <p>Every speed and size the estimator uses is a fact about the physical robot. Keeping the
 * source and a measured/placeholder flag next to the number means a report can never present
 * a guess as if it were a measurement.</p>
 *
 * @param value    the number, in {@code unit}
 * @param unit     the unit, e.g. {@link #INCHES_PER_SECOND}
 * @param source   where the number came from, or why it is a placeholder
 * @param measured true when measured on the real robot; false for placeholders to be replaced
 */
public record ModelValue(double value, String unit, String source, boolean measured) {

    /** Unit label for speeds. */
    public static final String INCHES_PER_SECOND = "in/s";
    /** Unit label for turn rates ("deg" rather than °, which Windows PowerShell garbles). */
    public static final String DEGREES_PER_SECOND = "deg/s";
    /** Unit label for lengths. */
    public static final String INCHES = "in";

    /** Refuses non-numbers, a missing unit, and a missing source. */
    public ModelValue {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("A model value must be a real number, got " + value);
        }
        if (unit == null || unit.isBlank()) {
            throw new IllegalArgumentException("A model value needs a unit.");
        }
        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException("A model value needs a source (or a reason it is a placeholder).");
        }
    }

    /** A value measured on the real robot. */
    public static ModelValue measured(double value, String unit, String source) {
        return new ModelValue(value, unit, source, true);
    }

    /** A stand-in value that the team still needs to measure. */
    public static ModelValue placeholder(double value, String unit, String reason) {
        return new ModelValue(value, unit, reason, false);
    }

    /** Converts a speed in feet per second to inches per second, for building models in feet. */
    public static double feetPerSecondToInchesPerSecond(double feetPerSecond) {
        return DisplayUnit.FEET.toInches(feetPerSecond);
    }

    /** e.g. {@code 62.61 in/s (measured: ...)} or {@code 180.00 deg/s (TO MEASURE: ...)}. */
    @Override
    public String toString() {
        return String.format(Locale.ROOT, "%.2f %s (%s: %s)", value, unit,
                measured ? "measured" : "TO MEASURE", source);
    }
}
