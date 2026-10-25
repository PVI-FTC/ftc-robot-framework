package pvi.harness.physics;

import java.util.ArrayList;
import java.util.List;

/**
 * How the game pieces move: how fast a rolling ball slows down, how much bounce is left after it
 * hits a wall, another ball or the robot, and how heavy each kind is.
 *
 * <p>None of these have been measured yet, so they are all placeholders. Only the masses' ratio
 * matters (it decides who moves more when two balls collide).</p>
 *
 * @param rollingDeceleration how fast a rolling ball slows down on the tiles (in/s²)
 * @param wallBounce          share of a ball's speed into a wall that it bounces back with (0 to 1)
 * @param pieceBounce         the same, for two balls hitting each other
 * @param robotBounce         the same, for the robot hitting a ball
 * @param pollenMass          a POLLEN ball's mass (g)
 * @param nectarMass          a NECTAR ball's mass (g)
 */
public record PieceModel(ModelValue rollingDeceleration, ModelValue wallBounce, ModelValue pieceBounce,
                         ModelValue robotBounce, ModelValue pollenMass, ModelValue nectarMass) {

    /** Unit label for a bounce: a share of the speed, 0 to 1. */
    public static final String RATIO = "ratio";
    /** Unit label for masses. */
    public static final String GRAMS = "g";

    /** Below this speed (in/s) a ball stops (rolling resistance holds it). */
    public static final double STOP_INCHES_PER_SECOND = 0.05;

    /** Refuses missing values, bounces outside 0 to 1, and masses or friction that aren't positive. */
    public PieceModel {
        for (ModelValue v : new ModelValue[] {rollingDeceleration, wallBounce, pieceBounce, robotBounce,
                pollenMass, nectarMass}) {
            if (v == null) {
                throw new IllegalArgumentException("A piece model needs every value.");
            }
        }
        for (ModelValue bounce : new ModelValue[] {wallBounce, pieceBounce, robotBounce}) {
            if (bounce.value() < 0 || bounce.value() > 1) {
                throw new IllegalArgumentException("A bounce must be between 0 and 1, got " + bounce.value());
            }
        }
        if (rollingDeceleration.value() < 0 || pollenMass.value() <= 0 || nectarMass.value() <= 0) {
            throw new IllegalArgumentException("Rolling deceleration can't be negative and masses must be positive.");
        }
    }

    /** The BIOBUZZ balls, all values placeholders until someone measures them. */
    public static PieceModel bioBuzz() {
        return new PieceModel(
                ModelValue.placeholder(15.0, Dynamics.INCHES_PER_SECOND_SQUARED,
                        "not measured; roll a ball across the tiles and time how far it goes"),
                ModelValue.placeholder(0.4, RATIO, "not measured; a guess for a foam ball against the wall"),
                ModelValue.placeholder(0.6, RATIO, "not measured; a guess for two balls"),
                ModelValue.placeholder(0.2, RATIO, "not measured; a guess for the robot's bumper"),
                ModelValue.placeholder(15.0, GRAMS, "not weighed"),
                ModelValue.placeholder(30.0, GRAMS, "not weighed"));
    }

    /** A ball's mass for its kind, in grams. */
    public double massOf(String kind) {
        return GamePiece.POLLEN.equals(kind) ? pollenMass.value() : nectarMass.value();
    }

    /** The names of every value still to measure, for reports. */
    public List<String> unmeasured() {
        List<String> names = new ArrayList<>();
        String[] labels = {"ball rolling deceleration", "ball wall bounce", "ball-to-ball bounce", "robot-to-ball bounce",
                "pollen mass", "nectar mass"};
        ModelValue[] values = {rollingDeceleration, wallBounce, pieceBounce, robotBounce, pollenMass, nectarMass};
        for (int i = 0; i < values.length; i++) {
            if (!values[i].measured()) {
                names.add(labels[i]);
            }
        }
        return names;
    }
}
