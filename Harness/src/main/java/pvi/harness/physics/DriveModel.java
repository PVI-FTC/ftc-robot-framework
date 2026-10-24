package pvi.harness.physics;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * How fast a particular robot moves at full power, and how big it is.
 *
 * <p>The position estimator multiplies wheel power by these speeds. For example, all wheels
 * at 0.5 power on a robot whose forward speed is 60 in/s means 30 in/s forward. With
 * {@link Dynamics} the robot gets there gradually (momentum); without, speed changes
 * instantly (the round-number test robots).</p>
 *
 * <p>Use {@link DriveModels} for real robots (values with sources), or
 * {@link #forTesting(String, double, double, double)} for round-number test robots.</p>
 */
public final class DriveModel {
    /** Robot name shown in reports, e.g. "Team A". */
    private final String name;
    /** Full-power forward speed (in/s), with its source. */
    private final ModelValue forward;
    /** Full-power sideways speed (in/s), with its source. */
    private final ModelValue strafe;
    /** Full-power turn rate (°/s), with its source. */
    private final ModelValue turn;
    /** Bumper-to-bumper length, front to back (in). */
    private final ModelValue length;
    /** Bumper-to-bumper width, side to side (in). */
    private final ModelValue width;
    /** How the robot speeds up and slows down, or null to change speed instantly. */
    private final Dynamics dynamics;

    /**
     * @param name    robot name for reports, e.g. "Team A"
     * @param forward full-power forward speed, in {@link ModelValue#INCHES_PER_SECOND}
     * @param strafe  full-power sideways speed, in {@link ModelValue#INCHES_PER_SECOND}
     * @param turn    full-power turn rate, in {@link ModelValue#DEGREES_PER_SECOND}
     * @param length  bumper-to-bumper length (front to back), in {@link ModelValue#INCHES}
     * @param width   bumper-to-bumper width (side to side), in {@link ModelValue#INCHES}
     */
    public DriveModel(String name, ModelValue forward, ModelValue strafe, ModelValue turn,
                      ModelValue length, ModelValue width) {
        this(name, forward, strafe, turn, length, width, null);
    }

    /** Like the other constructor, with momentum ({@code dynamics}, or null for instant speed changes). */
    public DriveModel(String name, ModelValue forward, ModelValue strafe, ModelValue turn,
                      ModelValue length, ModelValue width, Dynamics dynamics) {
        this.dynamics = dynamics;
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("A drive model needs a name.");
        }
        this.name = name;
        this.forward = require("forward speed", forward, ModelValue.INCHES_PER_SECOND);
        this.strafe = require("strafe speed", strafe, ModelValue.INCHES_PER_SECOND);
        this.turn = require("turn rate", turn, ModelValue.DEGREES_PER_SECOND);
        this.length = require("robot length", length, ModelValue.INCHES);
        this.width = require("robot width", width, ModelValue.INCHES);
    }

    /**
     * A made-up robot with round numbers, for estimator tests. Speeds are in <b>feet</b> per
     * second; size is 18 × 18 in. Every value is marked as a test value, not a measurement.
     */
    public static DriveModel forTesting(String name, double forwardFeetPerSecond,
                                        double strafeFeetPerSecond, double turnDegreesPerSecond) {
        String why = "test value, not a real robot";
        return new DriveModel(name,
                ModelValue.placeholder(ModelValue.feetPerSecondToInchesPerSecond(forwardFeetPerSecond),
                        ModelValue.INCHES_PER_SECOND, why),
                ModelValue.placeholder(ModelValue.feetPerSecondToInchesPerSecond(strafeFeetPerSecond),
                        ModelValue.INCHES_PER_SECOND, why),
                ModelValue.placeholder(turnDegreesPerSecond, ModelValue.DEGREES_PER_SECOND, why),
                ModelValue.placeholder(18.0, ModelValue.INCHES, why),
                ModelValue.placeholder(18.0, ModelValue.INCHES, why));
    }

    // ---- Numbers the estimator uses -------------------------------------------------------

    /** Robot name for reports. */
    public String name() { return name; }
    /** Full-power forward speed in inches per second (the estimator calculates in inches). */
    public double forwardInchesPerSecond() { return forward.value(); }
    /** Full-power sideways speed in inches per second. */
    public double strafeInchesPerSecond() { return strafe.value(); }
    /** Full-power turn rate in degrees per second. */
    public double turnDegreesPerSecond() { return turn.value(); }
    /** Full-power turn rate in radians per second (what Math.sin and Math.cos need). */
    public double turnRadiansPerSecond() { return Math.toRadians(turn.value()); }
    /** Robot length in inches. */
    public double lengthInches() { return length.value(); }
    /** Robot width in inches. */
    public double widthInches() { return width.value(); }
    /** How the robot speeds up and slows down, or null if speed changes instantly. */
    public Dynamics dynamics() { return dynamics; }

    /** The same robot with momentum ({@code dynamics}), or without (null). */
    public DriveModel withDynamics(Dynamics dynamics) {
        return new DriveModel(name, forward, strafe, turn, length, width, dynamics);
    }

    /** Forward speed in feet per second, for reports. */
    public double forwardFeetPerSecond() { return DisplayUnit.FEET.fromInches(forward.value()); }
    /** Sideways speed in feet per second, for reports. */
    public double strafeFeetPerSecond() { return DisplayUnit.FEET.fromInches(strafe.value()); }

    // ---- Where the numbers came from -------------------------------------------------------

    /** Forward speed with its source and measured/placeholder flag. */
    public ModelValue forward() { return forward; }
    /** Strafe speed with its source and flag. */
    public ModelValue strafe() { return strafe; }
    /** Turn rate with its source and flag. */
    public ModelValue turn() { return turn; }
    /** Length with its source and flag. */
    public ModelValue length() { return length; }
    /** Width with its source and flag. */
    public ModelValue width() { return width; }

    /** Names of values that are still placeholders, e.g. ["turn rate"]. Empty when all are measured. */
    public List<String> unmeasured() {
        List<String> names = new ArrayList<>();
        if (!forward.measured()) names.add("forward speed");
        if (!strafe.measured()) names.add("strafe speed");
        if (!turn.measured()) names.add("turn rate");
        if (!length.measured()) names.add("robot length");
        if (!width.measured()) names.add("robot width");
        if (dynamics != null) names.addAll(dynamics.unmeasured());
        return names;
    }

    /** True when every value was measured on the real robot (no placeholders left). */
    public boolean isFullyMeasured() {
        return unmeasured().isEmpty();
    }

    /** A few lines describing the model and where each value came from, for reports. */
    public String describe() {
        return String.format(Locale.ROOT,
                "Drive model: %s%n"
                        + "  forward speed  %.2f ft/s  = %s%n"
                        + "  strafe speed   %.2f ft/s  = %s%n"
                        + "  turn rate      %s%n"
                        + "  robot length   %s%n"
                        + "  robot width    %s%n"
                        + "%s",
                name, forwardFeetPerSecond(), forward, strafeFeetPerSecond(), strafe, turn, length, width,
                dynamics == null ? "  momentum       none (speed changes instantly)" : dynamics.describe());
    }

    /** Short summary, e.g. {@code DriveModel[Team A, unmeasured: [turn rate, ...]]}. */
    @Override
    public String toString() {
        return "DriveModel[" + name + (isFullyMeasured() ? "" : ", unmeasured: " + unmeasured()) + "]";
    }

    /** Checks one value: present, in the expected unit, and greater than zero. Says which one if not. */
    private static ModelValue require(String what, ModelValue value, String expectedUnit) {
        if (value == null) {
            throw new IllegalArgumentException("The drive model needs a " + what + ".");
        }
        if (!expectedUnit.equals(value.unit())) {
            throw new IllegalArgumentException("The " + what + " must be in " + expectedUnit
                    + ", but was given in " + value.unit() + ".");
        }
        if (value.value() <= 0) {
            throw new IllegalArgumentException("The " + what + " must be greater than 0, got "
                    + value.value() + " " + value.unit() + ".");
        }
        return value;
    }
}
