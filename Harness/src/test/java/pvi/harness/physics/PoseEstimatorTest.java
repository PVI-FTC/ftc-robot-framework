package pvi.harness.physics;

import com.qualcomm.robotcore.hardware.DcMotorSimple.Direction;

import org.junit.jupiter.api.Test;

import pvi.harness.hardware.FakeHardwareMap;
import pvi.harness.hardware.RobotConfigs;
import pvi.harness.physics.MecanumKinematics.ChassisPower;
import pvi.harness.runtime.HarnessConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Position estimator Step 5: adding up movement over time.
 *
 * <p>These use a made-up robot with round numbers (4 ft/s = 48 in/s forward, 3 ft/s = 36 in/s
 * strafe, 90 °/s turn) and set the motors by hand, so every expected answer can be worked out
 * on paper. Updates happen every 20 ms, like the runner.</p>
 */
class PoseEstimatorTest {
    /** Position tolerance in inches. */
    private static final double INCHES = 1e-6;
    /** One loop, as in the runner. */
    private static final double DT = 0.020;

    /** The round-number test robot. */
    private final DriveModel model = DriveModel.forTesting("Test bot", 4, 3, 90);
    /** Drive motors set up with the verified directions, so each wheel's push equals its power. */
    private final FakeHardwareMap hw = driveWithVerifiedDirections();
    /** Simulated time in seconds, advanced by run(). */
    private double time;

    /** The drive motors' names and forward directions in harness.properties (any team's robot). */
    private static final HarnessConfig CONFIG = HarnessConfig.load();

    private static FakeHardwareMap driveWithVerifiedDirections() {
        FakeHardwareMap hw = RobotConfigs.driveOnly();
        HarnessConfig.DriveMotorNames names = CONFIG.driveMotors();
        HarnessConfig.DriveMotorDirections forward = CONFIG.driveDirections();
        hw.motor(names.frontLeft()).setDirection(forward.frontLeft());
        hw.motor(names.frontRight()).setDirection(forward.frontRight());
        hw.motor(names.rearLeft()).setDirection(forward.rearLeft());
        hw.motor(names.rearRight()).setDirection(forward.rearRight());
        return hw;
    }

    /** Sets the four motors to make the given motion (fractions of full power). */
    private void drive(double forward, double strafeRight, double turnClockwise) {
        WheelPushes p = MecanumKinematics.toWheelPushes(new ChassisPower(forward, strafeRight, turnClockwise));
        HarnessConfig.DriveMotorNames names = CONFIG.driveMotors();
        hw.motor(names.frontLeft()).setPower(p.frontLeft());
        hw.motor(names.frontRight()).setPower(p.frontRight());
        hw.motor(names.rearLeft()).setPower(p.rearLeft());
        hw.motor(names.rearRight()).setPower(p.rearRight());
    }

    /** Updates the estimator every 20 ms for {@code seconds} (plus one update at the start). */
    private void run(PoseEstimator estimator, double seconds) {
        long steps = Math.round(seconds / DT);
        estimator.update(hw, time, time);
        for (long i = 0; i < steps; i++) {
            time += DT;
            estimator.update(hw, time, time);
        }
    }

    /**
     * A new estimator starting at {@code start}, on a field with walls but no HIVE frame or
     * FLOWERs: these tests check the motion math from the field center, where the HIVE stands
     * ({@code RobotObstaclesTest} checks the field elements).
     */
    private PoseEstimator estimatorAt(Pose start) {
        return new PoseEstimator(model, WheelReader.teamA(), start).withoutRobotObstacles();
    }

    /**
     * WHAT: Full forward for 1 s moves 4 ft (48 in) straight ahead.
     *
     * <p>WHY: The simplest possible check that speed × time = distance.</p>
     */
    @Test
    void fullForwardForOneSecondMovesFourFeet() {
        PoseEstimator estimator = estimatorAt(Pose.ORIGIN);

        drive(1, 0, 0);
        run(estimator, 1.0);

        assertEquals(48.0, estimator.pose().xInches(), INCHES);
        assertEquals(0.0, estimator.pose().yInches(), INCHES);
        assertEquals(0.0, estimator.pose().headingDegrees(), INCHES);
        assertEquals(48.0, estimator.distanceTraveledInches(), INCHES);
        assertEquals(1.0, estimator.movingSeconds(), INCHES);
    }

    /**
     * WHAT: Strafing right for 1 s moves 3 ft (36 in) to the right, which is -y facing +x.
     *
     * <p>WHY: Checks the strafe speed is used (not the forward speed) and the direction is right.</p>
     */
    @Test
    void strafeRightForOneSecondMovesThreeFeetRight() {
        PoseEstimator estimator = estimatorAt(Pose.ORIGIN);

        drive(0, 1, 0);
        run(estimator, 1.0);

        assertEquals(0.0, estimator.pose().xInches(), INCHES);
        assertEquals(-36.0, estimator.pose().yInches(), INCHES);
    }

    /**
     * WHAT: Turning clockwise for 1 s turns 90° right and stays in place.
     *
     * <p>WHY: A pure turn must change only the heading. Clockwise is negative in field terms.</p>
     */
    @Test
    void clockwiseTurnForOneSecondTurnsNinetyDegreesInPlace() {
        PoseEstimator estimator = estimatorAt(Pose.ORIGIN);

        drive(0, 0, 1);
        run(estimator, 1.0);

        assertEquals(-90.0, estimator.pose().headingDegrees(), 1e-6);
        assertEquals(0.0, estimator.pose().xInches(), INCHES);
        assertEquals(0.0, estimator.pose().yInches(), INCHES);
        assertEquals(0.0, estimator.distanceTraveledInches(), INCHES);
    }

    /**
     * WHAT: "Forward" follows the way the robot faces.
     *
     * <p>HOW: Starting at 90° (facing +y), full forward for 1 s must move +48 in in y and
     * nothing in x.</p>
     *
     * <p>WHY: Proves robot-relative motion is turned into field directions using the heading.</p>
     */
    @Test
    void forwardFollowsTheHeading() {
        PoseEstimator estimator = estimatorAt(Pose.inches(0, 0, 90));

        drive(1, 0, 0);
        run(estimator, 1.0);

        assertEquals(0.0, estimator.pose().xInches(), INCHES);
        assertEquals(48.0, estimator.pose().yInches(), INCHES);
    }

    /**
     * WHAT: Driving while turning through a full circle comes back to the start.
     *
     * <p>HOW: Half forward (24 in/s) and half counter-clockwise turn (45 °/s) for 8 s = one full
     * circle. The robot must end within 0.01 in of where it began, facing the same way, after
     * driving 192 in along the path.</p>
     *
     * <p>WHY: Checks the add-up-small-steps method stays accurate over a long curved path, not
     * just straight lines.</p>
     */
    @Test
    void fullCircleReturnsToTheStart() {
        PoseEstimator estimator = estimatorAt(Pose.ORIGIN);

        drive(0.5, 0, -0.5);
        run(estimator, 8.0);

        assertEquals(0.0, estimator.pose().distanceInchesTo(Pose.ORIGIN), 0.01);
        assertEquals(0.0, Pose.ORIGIN.headingDifferenceDegreesTo(estimator.pose()), 1e-6);
        assertEquals(192.0, estimator.distanceTraveledInches(), 0.01);
    }

    /**
     * WHAT: Out and back ends at the start, but the path length counts both ways.
     *
     * <p>HOW: Forward 2 s (96 in), then backward 2 s. Straight-line distance from the start
     * is 0; path length is 192 in.</p>
     *
     * <p>WHY: The report shows both numbers, because "where did it end up" and "how far did it
     * drive" are different questions.</p>
     */
    @Test
    void outAndBackEndsAtStartButCountsBothWays() {
        PoseEstimator estimator = estimatorAt(Pose.ORIGIN);

        // 48 in each way: well clear of the wall 63 in away.
        drive(1, 0, 0);
        run(estimator, 1.0);
        drive(-1, 0, 0);
        run(estimator, 1.0);

        assertEquals(0.0, estimator.pose().xInches(), INCHES);
        assertEquals(96.0, estimator.distanceTraveledInches(), INCHES);
    }

    /**
     * WHAT: With no power, nothing moves.
     */
    @Test
    void noPowerMeansNoMovement() {
        PoseEstimator estimator = estimatorAt(Pose.inches(10, -5, 30));

        run(estimator, 3.0);

        assertEquals(Pose.inches(10, -5, 30), estimator.pose());
        assertEquals(0.0, estimator.distanceTraveledInches(), INCHES);
        assertEquals(0.0, estimator.movingSeconds(), INCHES);
    }

    /**
     * WHAT: Power set at one update drives the robot until the next update.
     *
     * <p>HOW: Set full forward, update at 0 s (nothing has moved yet), update at 0.02 s (moved
     * 48 × 0.02 = 0.96 in). Then set zero power: the next update adds nothing.</p>
     *
     * <p>WHY: On the robot, a power set in one loop stays on until the next loop changes it.
     * The estimator must count it for exactly that long, not one loop early or late.</p>
     */
    @Test
    void powerActsUntilTheNextUpdate() {
        PoseEstimator estimator = estimatorAt(Pose.ORIGIN);

        drive(1, 0, 0);
        estimator.update(hw, 0.00, 0.00);
        assertEquals(0.0, estimator.pose().xInches(), INCHES);

        estimator.update(hw, 0.02, 0.02);
        assertEquals(0.96, estimator.pose().xInches(), INCHES);

        drive(0, 0, 0);
        estimator.update(hw, 0.04, 0.04);   // still counts the full power from 0.02 to 0.04
        assertEquals(1.92, estimator.pose().xInches(), INCHES);

        estimator.update(hw, 0.06, 0.06);   // now stopped
        assertEquals(1.92, estimator.pose().xInches(), INCHES);
    }

    /**
     * WHAT: The track keeps one point per update, with its time.
     *
     * <p>WHY: The CSV export (Step 8) and the viewer (Steps 11–14) are built from the track.</p>
     */
    @Test
    void trackHasOnePointPerUpdate() {
        PoseEstimator estimator = estimatorAt(Pose.ORIGIN);

        drive(1, 0, 0);
        run(estimator, 0.1);   // 1 start update + 5 steps

        assertEquals(6, estimator.track().size());
        assertEquals(0.0, estimator.track().get(0).matchTimeSeconds(), 1e-9);
        assertEquals(0.1, estimator.track().get(5).matchTimeSeconds(), 1e-9);
        assertEquals(4.8, estimator.track().get(5).pose().xInches(), INCHES);
    }

    /**
     * WHAT: Time going backwards is refused.
     *
     * <p>WHY: It would mean a bug in how updates are called; failing loudly finds it.</p>
     */
    @Test
    void refusesTimeGoingBackwards() {
        PoseEstimator estimator = estimatorAt(Pose.ORIGIN);
        estimator.update(hw, 1.0, 1.0);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> estimator.update(hw, 0.5, 0.5));

        assertTrue(error.getMessage().contains("backwards"), error.getMessage());
    }

    /**
     * WHAT: The report shows start, end, distances, and what is still a placeholder.
     *
     * <p>WHY: This is what people read in the test output (Step 6).</p>
     */
    @Test
    void reportShowsTheResultInFeet() {
        PoseEstimator estimator = estimatorAt(Pose.ORIGIN);
        drive(1, 0, 0);
        run(estimator, 1.0);

        String report = estimator.report();

        assertTrue(report.contains("end:       x=4.00 ft, y=0.00 ft, heading=0.0 deg"), report);
        assertTrue(report.contains("moved:     4.00 ft straight-line, 4.00 ft along the path, 1.00 s moving"), report);
        assertTrue(report.contains("top speed: 4.00 ft/s"), report);
        assertTrue(report.contains("TO MEASURE"), report);
    }

    // ---- Field walls --------------------------------------------------------------------

    /**
     * WHAT: Driving full forward into the +x wall stops the robot with its front bumper
     * touching the wall (center 9 in from it), and records one wall hit with its time.
     *
     * <p>WHY: A real robot can't drive through the field wall. Without walls the estimate would
     * show it 2 ft outside the field.</p>
     */
    @Test
    void wallsStopTheRobotAtTheEdgeOfTheField() {
        PoseEstimator estimator = estimatorAt(Pose.ORIGIN);
        drive(1, 0, 0);
        run(estimator, 2.0);   // 96 in of driving, but the wall is 63 in away

        assertEquals(63.0, estimator.pose().xInches(), 1e-9, "72 in wall minus 9 in half-length");
        assertEquals(0.0, estimator.pose().yInches(), 1e-9);
        assertEquals(1, estimator.wallHits().size(), "pressing on the wall is one hit, not one per loop");
        PoseEstimator.WallHit hit = estimator.wallHits().get(0);
        assertEquals("+x", hit.wall());
        assertEquals(63.0 / 48.0, hit.matchTimeSeconds(), DT, "reaches the wall after 63 in at 48 in/s");
        assertTrue(estimator.report().contains("walls:     hit 1 time (first: +x wall at 1.32 s)"), estimator.report());
    }

    /**
     * WHAT: Driving at an angle into a wall, the robot slides along it.
     *
     * <p>HOW: Starts 3 in from the +x wall, drives forward and strafes left together. Only the
     * forward part is blocked.</p>
     *
     * <p>WHY: That's what a mecanum robot pushing against the wall does; stopping dead would make
     * driving along the wall impossible in the viewer.</p>
     */
    @Test
    void robotSlidesAlongAWall() {
        PoseEstimator estimator = estimatorAt(Pose.inches(60, 0, 0));
        drive(1, -0.5, 0);
        run(estimator, 1.0);

        assertEquals(63.0, estimator.pose().xInches(), 1e-9);
        assertTrue(estimator.pose().yInches() > 5, "kept moving left along the wall: " + estimator.pose());
    }

    /**
     * WHAT: A robot turned 45 degrees stops further from the wall, because its corner reaches out.
     *
     * <p>WHY: The whole box must stay inside, at any heading.</p>
     */
    @Test
    void aTurnedRobotStopsWhereItsCornerTouches() {
        PoseEstimator estimator = estimatorAt(Pose.inches(50, 0, 45));
        drive(1, 0, 0);
        run(estimator, 1.0);

        double reach = 9 * Math.cos(Math.toRadians(45)) + 9 * Math.sin(Math.toRadians(45));
        assertEquals(72 - reach, estimator.pose().xInches(), 1e-9);
        assertEquals(45.0, estimator.pose().headingDegrees(), 1e-9);
    }

    /**
     * WHAT: With {@code withoutWalls()}, the robot drives straight off the field.
     *
     * <p>WHY: Tests of the off-field check itself need a robot that can leave.</p>
     */
    @Test
    void withoutWallsTheRobotCanLeave() {
        PoseEstimator estimator = estimatorAt(Pose.ORIGIN).withoutWalls();
        drive(1, 0, 0);
        run(estimator, 2.0);

        assertEquals(96.0, estimator.pose().xInches(), 1e-9);
        assertTrue(estimator.wallHits().isEmpty());
        assertTrue(estimator.report().contains("walls:     off"), estimator.report());
    }
}
