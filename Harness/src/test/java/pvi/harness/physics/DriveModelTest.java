package pvi.harness.physics;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Position estimator Step 2: the drive model (robot speeds and size, with sources).
 */
class DriveModelTest {
    /** Allowed rounding error for decimal math. */
    private static final double TOLERANCE = 1e-9;

    /**
     * WHAT: Team A's model uses the speeds from the team's Pedro tuning.
     *
     * <p>HOW: Compares the model's forward and strafe speeds with the exact values in
     * TeamAPedroConfiguration (xVelocity, yVelocity), and checks both are marked measured.</p>
     *
     * <p>WHY: These come from real tuner runs on the robot. The harness reads them from
     * TeamCode rather than copying them, so this test also proves that link works. If the team
     * re-tunes, the harness follows, and this test shows the new numbers.</p>
     */
    @Test
    void teamAModelUsesPedroTunedSpeeds() {
        DriveModel teamA = DriveModels.teamA();

        assertEquals(62.61374213751846, teamA.forwardInchesPerSecond(), TOLERANCE);
        assertEquals(49.757958599901585, teamA.strafeInchesPerSecond(), TOLERANCE);
        assertTrue(teamA.forward().measured());
        assertTrue(teamA.strafe().measured());
        assertTrue(teamA.forward().source().contains("TeamAPedroConfiguration"));
    }

    /**
     * WHAT: Team A's model clearly flags the values that are still placeholders.
     *
     * <p>HOW: Turn rate and robot size were not measured (Step 0), nor were the momentum's
     * response time and traction limit (Step 17). They must be listed as unmeasured, and the
     * printed description must say "TO MEASURE" next to them.</p>
     *
     * <p>WHY: Estimates built on guesses must never look like measured results. This label
     * travels into every report until the team measures the real values.</p>
     */
    @Test
    void teamAModelFlagsWhatStillNeedsMeasuring() {
        DriveModel teamA = DriveModels.teamA();

        assertEquals(List.of("turn rate", "robot length", "robot width", "response time", "maximum acceleration"),
                teamA.unmeasured());
        assertFalse(teamA.isFullyMeasured());
        assertEquals(180.0, teamA.turnDegreesPerSecond(), TOLERANCE);
        assertEquals(18.0, teamA.lengthInches(), TOLERANCE);

        String description = teamA.describe();
        assertTrue(description.contains("turn rate      180.00 deg/s (TO MEASURE"), description);
        assertTrue(description.contains("forward speed  5.22 ft/s"), description);
    }

    /**
     * WHAT: Speeds convert to feet per second and turn rate to radians per second.
     *
     * <p>HOW: Forward speed in ft/s must be in/s ÷ 12; turn rate in rad/s must be °/s × π/180.</p>
     *
     * <p>WHY: Reports show feet (Step 0), and the estimator's math needs radians (Step 5).</p>
     */
    @Test
    void convertsToFeetAndRadians() {
        DriveModel teamA = DriveModels.teamA();

        assertEquals(62.61374213751846 / 12.0, teamA.forwardFeetPerSecond(), TOLERANCE);
        assertEquals(49.757958599901585 / 12.0, teamA.strafeFeetPerSecond(), TOLERANCE);
        assertEquals(Math.PI, teamA.turnRadiansPerSecond(), TOLERANCE);  // 180 °/s
    }

    /**
     * WHAT: A test robot can be built from round numbers in feet.
     *
     * <p>HOW: forTesting(4 ft/s, 3 ft/s, 90 °/s) must give 48 in/s and 36 in/s, with every
     * value marked as a test value (not measured).</p>
     *
     * <p>WHY: Estimator tests (Step 5) are easier to check by hand with round numbers, e.g.
     * "4 ft/s for 1 s = 4 ft".</p>
     */
    @Test
    void testRobotCanBeBuiltFromRoundNumbersInFeet() {
        DriveModel test = DriveModel.forTesting("Test bot", 4.0, 3.0, 90.0);

        assertEquals(48.0, test.forwardInchesPerSecond(), TOLERANCE);
        assertEquals(36.0, test.strafeInchesPerSecond(), TOLERANCE);
        assertEquals(90.0, test.turnDegreesPerSecond(), TOLERANCE);
        assertEquals(5, test.unmeasured().size());
    }

    /**
     * WHAT: A speed of zero or less is refused, and the error says which setting is wrong.
     *
     * <p>WHY: A zero speed would make the robot look like it never moves, and a negative one
     * would reverse it. Both are typing mistakes worth catching immediately.</p>
     */
    @Test
    void rejectsZeroOrNegativeValuesAndSaysWhichOne() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> new DriveModel("Bad",
                        ModelValue.measured(60, ModelValue.INCHES_PER_SECOND, "x"),
                        ModelValue.measured(-5, ModelValue.INCHES_PER_SECOND, "x"),
                        ModelValue.measured(180, ModelValue.DEGREES_PER_SECOND, "x"),
                        ModelValue.measured(18, ModelValue.INCHES, "x"),
                        ModelValue.measured(18, ModelValue.INCHES, "x")));

        assertTrue(error.getMessage().contains("strafe speed"), error.getMessage());
    }

    /**
     * WHAT: A value in the wrong unit is refused.
     *
     * <p>HOW: Passes a forward speed labeled "ft/s" where "in/s" is required.</p>
     *
     * <p>WHY: Mixing feet and inches is the easiest mistake to make, and it would make every
     * estimate 12 times too small. The unit label is checked, so it can't slip through.</p>
     */
    @Test
    void rejectsValuesInTheWrongUnit() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> new DriveModel("Mixed up",
                        ModelValue.measured(5.2, "ft/s", "someone typed feet"),
                        ModelValue.measured(50, ModelValue.INCHES_PER_SECOND, "x"),
                        ModelValue.measured(180, ModelValue.DEGREES_PER_SECOND, "x"),
                        ModelValue.measured(18, ModelValue.INCHES, "x"),
                        ModelValue.measured(18, ModelValue.INCHES, "x")));

        assertTrue(error.getMessage().contains("forward speed must be in in/s"), error.getMessage());
    }
}
