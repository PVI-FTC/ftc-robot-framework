package pvi.harness.physics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Position estimator Step 1: units and poses.
 *
 * <p>These test harness code only (no robot code). Every later estimator step builds on
 * Pose, so its conversions and angle handling have to be right first.</p>
 */
class PoseTest {
    /** Allowed rounding error for decimal math. */
    private static final double TOLERANCE = 1e-9;

    /**
     * WHAT: Feet and inches convert correctly in both directions.
     *
     * <p>HOW: Checks 12 in = 1 ft, a full field width (144 in) = 12 ft, and that a pose made
     * in feet reads back the same in inches and feet.</p>
     *
     * <p>WHY: Reports are shown in feet but everything is calculated in inches. A wrong
     * conversion would make every reported distance wrong by a factor of 12.</p>
     */
    @Test
    void feetAndInchesConvertBothWays() {
        // CHECK the unit itself.
        assertEquals(1.0, DisplayUnit.FEET.fromInches(12.0), TOLERANCE);
        assertEquals(12.0, DisplayUnit.FEET.fromInches(144.0), TOLERANCE);
        assertEquals(36.0, DisplayUnit.FEET.toInches(3.0), TOLERANCE);
        assertEquals(5.0, DisplayUnit.INCHES.fromInches(5.0), TOLERANCE);

        // CHECK a pose made in feet.
        Pose pose = Pose.feet(2.0, -1.5, 0);
        assertEquals(24.0, pose.xInches(), TOLERANCE);
        assertEquals(-18.0, pose.yInches(), TOLERANCE);
        assertEquals(2.0, pose.xFeet(), TOLERANCE);
        assertEquals(-1.5, pose.yFeet(), TOLERANCE);
    }

    /**
     * WHAT: Headings are always kept between -180° and +180°.
     *
     * <p>HOW: Makes poses with headings outside that range and checks where they end up:
     * 370° is 10°, -190° is 170°, and both 180° and -180° become 180° (the same direction).</p>
     *
     * <p>WHY: A robot that turns one and a half times faces the same way as one that turned
     * half a turn. Keeping one standard range means comparisons and printouts always agree.</p>
     */
    @Test
    void headingIsAlwaysBetweenMinus180And180() {
        assertEquals(10.0, Pose.inches(0, 0, 370).headingDegrees(), TOLERANCE);
        assertEquals(170.0, Pose.inches(0, 0, -190).headingDegrees(), TOLERANCE);
        assertEquals(180.0, Pose.inches(0, 0, 180).headingDegrees(), TOLERANCE);
        assertEquals(180.0, Pose.inches(0, 0, -180).headingDegrees(), TOLERANCE);
        assertEquals(180.0, Pose.inches(0, 0, 540).headingDegrees(), TOLERANCE);
        assertEquals(-90.0, Pose.inches(0, 0, 270).headingDegrees(), TOLERANCE);
    }

    /**
     * WHAT: Poses print in feet by default, and in inches when asked.
     *
     * <p>HOW: 7.92 in forward prints as 0.66 ft. Tiny negative leftovers (like -0.0001)
     * print as 0.00, not "-0.00".</p>
     *
     * <p>WHY: This is the text people read in test output and the viewer. It must be clear
     * and the same on every computer (the format ignores the PC's language setting, so a
     * German PC still prints "0.66", not "0,66").</p>
     */
    @Test
    void printsInFeetByDefaultAndInchesWhenAsked() {
        Pose pose = Pose.inches(7.92, -0.0001, -0.01);

        assertEquals("x=0.66 ft, y=0.00 ft, heading=0.0 deg", pose.toString());
        assertEquals("x=7.92 in, y=0.00 in, heading=0.0 deg", pose.toString(DisplayUnit.INCHES));
        assertEquals("x=-3.00 ft, y=1.50 ft, heading=90.0 deg", Pose.feet(-3, 1.5, 90).toString());
    }

    /**
     * WHAT: Distance between two poses is the straight-line distance.
     *
     * <p>HOW: Uses a 3-4-5 triangle: 36 in across and 48 in up is 60 in (5 ft) away.</p>
     *
     * <p>WHY: Later tests ask "did the robot end within 1 inch of where it should?". That
     * question is this method.</p>
     */
    @Test
    void distanceIsStraightLine() {
        Pose start = Pose.ORIGIN;
        Pose end = Pose.inches(36, 48, 123);   // heading doesn't affect distance

        assertEquals(60.0, start.distanceInchesTo(end), TOLERANCE);
        assertEquals(5.0, start.distanceFeetTo(end), TOLERANCE);
        assertEquals(60.0, end.distanceInchesTo(start), TOLERANCE);
    }

    /**
     * WHAT: The heading difference is the shortest turn, with direction.
     *
     * <p>HOW: From 170° to -170° is a 20° counter-clockwise turn across the ±180° line, not
     * -340°. The reverse is -20° (clockwise). From 0° to 90° is +90°.</p>
     *
     * <p>WHY: Tests like "the robot turned about 90° left" must not fail just because the
     * heading crossed ±180°.</p>
     */
    @Test
    void headingDifferenceTakesTheShortWayAround() {
        assertEquals(20.0, Pose.inches(0, 0, 170).headingDifferenceDegreesTo(Pose.inches(0, 0, -170)), TOLERANCE);
        assertEquals(-20.0, Pose.inches(0, 0, -170).headingDifferenceDegreesTo(Pose.inches(0, 0, 170)), TOLERANCE);
        assertEquals(90.0, Pose.ORIGIN.headingDifferenceDegreesTo(Pose.inches(0, 0, 90)), TOLERANCE);
    }

    /**
     * WHAT: The pose knows whether it is on the 12 × 12 ft field.
     *
     * <p>HOW: With the FTC center origin, the field runs from -72 to +72 in. The center and
     * the exact corner count as on the field; 73 in does not.</p>
     *
     * <p>WHY: Lets later tests check "the autonomous never drives the robot off the field".</p>
     */
    @Test
    void knowsWhetherItIsOnTheField() {
        assertTrue(Pose.ORIGIN.isOnField());
        assertTrue(Pose.inches(72, -72, 0).isOnField());
        assertFalse(Pose.inches(73, 0, 0).isOnField());
        assertFalse(Pose.feet(0, -6.5, 0).isOnField());
    }

    /**
     * WHAT: A pose refuses numbers that aren't real values.
     *
     * <p>HOW: NaN ("not a number", e.g. from 0 / 0) or infinity must throw immediately, with a
     * message showing the bad values.</p>
     *
     * <p>WHY: A NaN silently spreads through every later calculation. Failing at the moment
     * it appears points straight at the bug.</p>
     */
    @Test
    void rejectsValuesThatAreNotRealNumbers() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> new Pose(Double.NaN, 0, 0));
        assertTrue(error.getMessage().contains("NaN"), error.getMessage());

        assertThrows(IllegalArgumentException.class, () -> Pose.inches(0, Double.POSITIVE_INFINITY, 0));
    }
}
