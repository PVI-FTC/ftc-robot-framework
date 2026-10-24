package pvi.harness.physics;

import org.firstinspires.ftc.teamcode.opmodes.autonomous.TeamAAutoOpMode;
import org.firstinspires.ftc.teamcode.opmodes.teleop.TeamATeleOp;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import pvi.harness.ds.Axis;
import pvi.harness.ds.GamepadScript;
import pvi.harness.hardware.FakeHardwareMap;
import pvi.harness.hardware.RobotConfigs;
import pvi.harness.runtime.MatchPeriod;
import pvi.harness.runtime.OpModeRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Position estimator Step 7: more motion checks on the real, unchanged Team A OpModes.
 *
 * <p>Each test runs an OpMode through {@link OpModeRunner} and checks where the estimator says
 * the robot ended up. Team A's turn rate and robot size are still placeholders (Step 10), so
 * expected turns and robot corners are worked out from {@link DriveModels#teamA()} instead of
 * typed in. When the team measures them, these tests keep working.</p>
 */
class RealCodeMotionTest {
    /** One loop is 20 ms; stick changes can be one loop late. */
    private static final double ONE_LOOP_SECONDS = 0.020;

    /**
     * WHAT: Full right stick for half a second turns the robot clockwise on the spot.
     *
     * <p>HOW: Scripted driver: right stick full right from 0 to 0.5 s, then let go. The robot
     * should turn 0.5 s × Team A's turn rate clockwise (negative heading), give or take one
     * loop, and not move its center at all.</p>
     *
     * <p>WHY: The right stick is the only way the driver turns. If the turn went the wrong way,
     * or the robot slid while turning, a sign mistake in the TeleOp, the drive mixing or the
     * motor directions would be to blame.</p>
     */
    @Test
    void rightStickTurnsTheRobotClockwiseInPlace() {
        // SCRIPT
        GamepadScript driver = new GamepadScript()
                .at(0.0).stick(Axis.RIGHT_STICK_X, 1.0)
                .at(0.5).releaseAll();

        // SET UP
        OpModeRunner runner = new OpModeRunner(new TeamATeleOp(), RobotConfigs.teamA()).gamepad1(driver);
        PoseEstimator estimator = PoseEstimator.attachTo(runner, DriveModels.teamA());

        // RUN
        runner.run(1.0, 1.5).assertNoException();

        // CHECK
        // Half a second at the full turn rate. Momentum loses a little while the turn speeds up
        // and gives some back while it brakes, so allow 10%.
        double turnDegreesPerSecond = DriveModels.teamA().turnDegreesPerSecond();
        Pose end = estimator.pose();
        assertEquals(-0.5 * turnDegreesPerSecond, end.headingDegrees(),
                0.1 * 0.5 * turnDegreesPerSecond, "clockwise turn (negative heading)");
        assertEquals(0.0, end.xInches(), 1e-6, "no forward movement while turning");
        assertEquals(0.0, end.yInches(), 1e-6, "no sideways movement while turning");
    }

    /**
     * WHAT: With the front-left motor's wires swapped, "full forward" curves off to the left.
     *
     * <p>HOW: Same driver as a straight 1 s drive, but the fake robot has frontLeft miswired.
     * The code still sends +1 power to every wheel and can't tell anything is wrong, but the
     * front-left wheel really pushes backward. The robot turns counter-clockwise, slides left
     * and falls well short of the 62.6 in a healthy robot would cover.</p>
     *
     * <p>WHY: A swapped motor connector is a common pit mistake. This shows what it looks like
     * on the field, so students can recognize it, and proves the estimator follows what the
     * wheels physically do rather than what the code asked for.</p>
     */
    @Test
    void miswiredFrontLeftMotorMakesForwardCurveLeft() {
        // SCRIPT
        GamepadScript driver = new GamepadScript()
                .at(0.0).stick(Axis.LEFT_STICK_Y, -1.0)
                .at(1.0).releaseAll();

        // SET UP: the front-left motor's power wires are swapped.
        FakeHardwareMap robot = RobotConfigs.teamA().miswire("frontLeft");
        OpModeRunner runner = new OpModeRunner(new TeamATeleOp(), robot).gamepad1(driver);
        PoseEstimator estimator = PoseEstimator.attachTo(runner, DriveModels.teamA());

        // RUN: check the code's view while the stick is still held.
        runner.init().start();
        runner.loopUntil(0.5);
        assertEquals(1.0, runner.hardwareMap().motor("frontLeft").getPower(), 1e-9,
                "the code still thinks it is sending full forward power");
        runner.loopUntil(2.0);
        runner.stop();

        // CHECK: the robot really drifted.
        Pose end = estimator.pose();
        double healthyForwardInches = DriveModels.teamA().forwardInchesPerSecond();
        assertTrue(end.headingDegrees() > 30.0,
                "should turn counter-clockwise (left), but ended at " + end.toString(DisplayUnit.INCHES));
        assertTrue(end.yInches() > 10.0,
                "should drift left (+y), but ended at " + end.toString(DisplayUnit.INCHES));
        assertTrue(end.xInches() < healthyForwardInches - 10.0,
                "should fall well short of a healthy " + healthyForwardInches + " in, but ended at "
                        + end.toString(DisplayUnit.INCHES));
    }

    /**
     * WHAT: Pressing STOP while the driver is still holding full forward freezes the robot.
     *
     * <p>HOW: The driver holds the stick forward and never lets go. After 1 s the test presses
     * STOP, notes the pose, then lets one more second of time pass on the estimator. The pose
     * must not change, because TeamCode's stop() must have set every drive motor to 0.</p>
     *
     * <p>WHY: STOP is the safety button. If stop() left power on the wheels, a real robot would
     * keep driving after the match or after a referee stop.</p>
     */
    @Test
    void pressingStopFreezesThePositionEvenWithTheStickHeld() {
        // SCRIPT: the stick is never released.
        GamepadScript driver = new GamepadScript()
                .at(0.0).stick(Axis.LEFT_STICK_Y, -1.0);

        // SET UP
        OpModeRunner runner = new OpModeRunner(new TeamATeleOp(), RobotConfigs.teamA()).gamepad1(driver);
        PoseEstimator estimator = PoseEstimator.attachTo(runner, DriveModels.teamA());

        // RUN: drive for 1 s, then press STOP.
        runner.init().start();
        runner.loopUntil(1.0);
        runner.stop();
        Pose atStop = estimator.pose();

        // Two more seconds pass after STOP, in 20 ms steps, with whatever the wheels are doing now.
        double t = runner.clock().sinceInit();
        double m = runner.clock().sinceStart();
        Pose afterOneSecond = null;
        for (int i = 1; i <= 100; i++) {
            estimator.update(runner.hardwareMap(), t + i * ONE_LOOP_SECONDS, m + i * ONE_LOOP_SECONDS);
            if (i == 50) {
                afterOneSecond = estimator.pose();
            }
        }

        // CHECK: the power is off, so the robot brakes to a stop (momentum carries it a little
        // further, like a real robot), then stays put.
        assertTrue(atStop.xInches() > 40.0, "the robot should have been driving before STOP: " + atStop);
        for (String motor : new String[] {"frontLeft", "frontRight", "rearLeft", "rearRight"}) {
            assertEquals(0.0, runner.hardwareMap().motor(motor).getPower(), 0.0, motor + " has no power after STOP");
        }
        double braking = afterOneSecond.xInches() - atStop.xInches();
        assertTrue(braking >= 0 && braking < 15.0, "stops within a short braking distance, went " + braking + " in");
        assertEquals(afterOneSecond, estimator.pose(), "the robot kept moving long after STOP");
    }

    /**
     * WHAT: Starting with its back against a wall, the whole robot stays on the field for the
     * full 30 s autonomous.
     *
     * <p>HOW: Starts the estimator with the robot's back bumper touching the -x wall, facing
     * +x, runs the autonomous, and checks all four corners of the robot box at every loop.</p>
     *
     * <p>WHY: A robot that leaves the field (or hits a wall) in autonomous loses points or gets
     * stuck. If the autonomous is changed to drive too far, this fails and says when and where
     * the robot left the field.</p>
     */
    @Test
    void autonomousStaysOnTheFieldFromAStartAgainstTheWall() {
        // SET UP
        DriveModel teamA = DriveModels.teamA();
        Pose againstTheWall = Pose.inches(-Pose.FIELD_HALF_WIDTH_INCHES + teamA.lengthInches() / 2.0, 0.0, 0.0);
        OpModeRunner runner = new OpModeRunner(new TeamAAutoOpMode(), RobotConfigs.teamA());
        PoseEstimator estimator = PoseEstimator.attachTo(runner, teamA, WheelReader.teamA(), againstTheWall);

        // RUN
        runner.run(MatchPeriod.AUTONOMOUS).assertNoException();

        // CHECK
        OffField offField = firstMomentOffField(estimator);
        assertNull(offField, () -> offField.message());
    }

    /**
     * WHAT: When the robot does leave the field, the on-field check says when and where.
     *
     * <p>HOW: Starts the robot facing the +x wall with only 5 in between its front bumper and
     * the wall. The autonomous drives about 9.4 in forward, so it must cross the wall. The
     * report must name the time and the corner that went out.</p>
     *
     * <p>WHY: Proves the on-field check can fail, and that its message is clear enough to act
     * on without digging through the track by hand.</p>
     */
    @Test
    void onFieldCheckReportsWhenAndWhereTheRobotLeavesTheField() {
        // SET UP
        DriveModel teamA = DriveModels.teamA();
        double gapInches = 5.0;
        Pose nearTheWall = Pose.inches(Pose.FIELD_HALF_WIDTH_INCHES - teamA.lengthInches() / 2.0 - gapInches, 0.0, 0.0);
        OpModeRunner runner = new OpModeRunner(new TeamAAutoOpMode(), RobotConfigs.teamA());
        // The walls would stop the robot; turn them off so it can cross and the check can fire.
        // Instant speed changes (no momentum) keep the expected crossing time simple to work out.
        PoseEstimator estimator = PoseEstimator.attachTo(runner, teamA.withDynamics(null), WheelReader.teamA(),
                nearTheWall).withoutWalls();

        // RUN
        runner.run(MatchPeriod.AUTONOMOUS).assertNoException();

        // CHECK
        OffField offField = firstMomentOffField(estimator);
        assertNotNull(offField, "the robot should have crossed the +x wall");
        System.out.println(offField.message());
        // Both front corners reach the wall together; either one may be named.
        assertTrue(offField.corner().startsWith("front-"), offField.message());
        // Driving at 0.20 power covers the 5 in gap in about 5 / (0.20 x 62.61) = 0.40 s. Allow
        // two loops: the drive turns on one loop after PLAY, and the crossing is only seen at
        // the next 20 ms track point.
        double expectedSeconds = gapInches / (0.20 * teamA.forwardInchesPerSecond());
        assertEquals(expectedSeconds, offField.matchTimeSeconds(), 2 * ONE_LOOP_SECONDS + 1e-6, offField.message());
        assertTrue(offField.message().startsWith("The robot left the field at "), offField.message());
    }

    /**
     * WHAT: Driving the real Team A TeleOp full forward for 3 s (far more than the 63 in to the
     * wall), the robot stops at the +x wall instead of leaving the field, and the run shows one
     * wall hit.
     *
     * <p>WHY: Every runner tracks position with the field walls on, so a long drive in the
     * viewer or a test ends against the wall, as on the real field.</p>
     */
    @Test
    void teleOpCannotDriveThroughTheWall() {
        // SCRIPT: full forward for 3 s.
        GamepadScript driver = new GamepadScript()
                .at(0.0).stick(Axis.LEFT_STICK_Y, -1.0)
                .at(3.0).stick(Axis.LEFT_STICK_Y, 0.0);
        OpModeRunner runner = new OpModeRunner(new TeamATeleOp(), RobotConfigs.teamA()).gamepad1(driver);

        // RUN
        runner.init().start();
        runner.loopUntil(3.5);

        // CHECK
        PoseEstimator estimator = runner.positionEstimator();
        assertTrue(estimator.hasWalls(), "runners track position with the walls on");
        assertEquals(Pose.FIELD_HALF_WIDTH_INCHES - DriveModels.teamA().lengthInches() / 2.0,
                estimator.pose().xInches(), 1e-6, "front bumper against the +x wall");
        assertTrue(estimator.pose().isOnField());
        assertEquals(1, estimator.wallHits().size(), estimator.report());
        assertEquals("+x", estimator.wallHits().get(0).wall());
    }

    // ---- Helpers --------------------------------------------------------------------------

    /** The first moment a robot corner was off the field: when, which corner, and a readable message. */
    private record OffField(double matchTimeSeconds, String corner, String message) {
    }

    /**
     * Checks every pose in the track and returns the first moment any corner of the robot box
     * was outside the ±72 in field, or null if it stayed on the field. The robot box comes from
     * the drive model's length and width.
     */
    private static OffField firstMomentOffField(PoseEstimator estimator) {
        DriveModel model = estimator.model();
        double halfLength = model.lengthInches() / 2.0;
        double halfWidth = model.widthInches() / 2.0;
        // Each corner as (inches toward the front, inches toward the left side).
        double[][] corners = {{halfLength, halfWidth}, {halfLength, -halfWidth},
                {-halfLength, halfWidth}, {-halfLength, -halfWidth}};
        String[] cornerNames = {"front-left", "front-right", "back-left", "back-right"};

        for (PoseEstimator.TrackPoint point : estimator.track()) {
            Pose pose = point.pose();
            double cos = Math.cos(pose.headingRadians());
            double sin = Math.sin(pose.headingRadians());
            for (int i = 0; i < corners.length; i++) {
                double forward = corners[i][0];
                double left = corners[i][1];
                // Same robot-to-field turn as the estimator uses.
                double cornerX = pose.xInches() + forward * cos - left * sin;
                double cornerY = pose.yInches() + forward * sin + left * cos;
                if (Math.abs(cornerX) > Pose.FIELD_HALF_WIDTH_INCHES + 1e-9
                        || Math.abs(cornerY) > Pose.FIELD_HALF_WIDTH_INCHES + 1e-9) {
                    String message = String.format(Locale.ROOT,
                            "The robot left the field at %.2f s: its %s corner was at x=%.2f in, y=%.2f in"
                                    + " (field edges are at +/-%.0f in). Robot center: %s. Robot box: %.0f x %.0f in"
                                    + " from the %s drive model%s.",
                            point.matchTimeSeconds(), cornerNames[i], cornerX, cornerY,
                            Pose.FIELD_HALF_WIDTH_INCHES, pose.toString(DisplayUnit.INCHES),
                            model.lengthInches(), model.widthInches(), model.name(),
                            model.isFullyMeasured() ? "" : " (some values are placeholders)");
                    return new OffField(point.matchTimeSeconds(), cornerNames[i], message);
                }
            }
        }
        return null;
    }
}
