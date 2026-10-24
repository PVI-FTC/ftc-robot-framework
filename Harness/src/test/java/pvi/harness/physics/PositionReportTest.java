package pvi.harness.physics;

import org.firstinspires.ftc.teamcode.opmodes.autonomous.TeamAAutoOpMode;
import org.firstinspires.ftc.teamcode.opmodes.teleop.TeamATeleOp;
import org.junit.jupiter.api.Test;

import pvi.harness.ds.Axis;
import pvi.harness.ds.GamepadScript;
import pvi.harness.hardware.RobotConfigs;
import pvi.harness.runtime.MatchPeriod;
import pvi.harness.runtime.OpModeRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Position estimator Step 6: the estimator attached to real Team A OpModes, checking where
 * the robot would end up. Look for "Final robot position" in the test output (printed
 * automatically after every test by PositionReportExtension).
 *
 * <p>Expected distances come from Team A's drive model: forward 62.61 in/s and strafe
 * 49.76 in/s at full power (Pedro tuning).</p>
 */
class PositionReportTest {

    /**
     * WHAT: Team A's autonomous drives about 9.4 inches straight ahead.
     *
     * <p>HOW: Attaches the estimator, runs the whole 30 s autonomous, prints the report.
     * The OpMode drives at 0.20 power for 0.75 s, so the robot should move
     * 0.20 × 62.61 in/s × 0.75 s ≈ 9.39 in forward, with no sideways drift or turning.</p>
     *
     * <p>WHY: The first end-to-end position check on real robot code. A sign or direction bug
     * anywhere (code, motor directions, mixing) would show up as drift, turning, or going
     * backwards.</p>
     */
    @Test
    void autonomousDrivesAboutNineInchesStraightAhead() {
        OpModeRunner runner = new OpModeRunner(new TeamAAutoOpMode(), RobotConfigs.teamA());
        PoseEstimator estimator = PoseEstimator.attachTo(runner, DriveModels.teamA());

        runner.run(MatchPeriod.AUTONOMOUS).assertNoException();
        // The final-position report is printed automatically after the test.

        double expectedInches = 0.20 * DriveModels.teamA().forwardInchesPerSecond() * 0.75;
        Pose end = estimator.pose();
        assertEquals(expectedInches, end.xInches(), 0.5, "forward distance");
        assertEquals(0.0, end.yInches(), 1e-6, "no sideways drift");
        assertEquals(0.0, end.headingDegrees(), 1e-6, "no turning");
    }

    /**
     * WHAT: In TeleOp, 1 s forward then 1 s strafing right ends forward and to the right.
     *
     * <p>HOW: Scripted driver: full forward from 0 to 1 s, full right from 1 to 2 s. Expected
     * end: x ≈ +62.6 in (5.2 ft), y ≈ -49.8 in (4.1 ft to the right), heading unchanged.</p>
     *
     * <p>WHY: Shows the driver's controls move the robot the way the driver expects, with the
     * forward and strafe speeds from Team A's tuning.</p>
     */
    @Test
    void teleOpForwardThenRightEndsForwardAndRight() {
        GamepadScript driver = new GamepadScript()
                .at(0.0).stick(Axis.LEFT_STICK_Y, -1.0)
                .at(1.0).stick(Axis.LEFT_STICK_Y, 0.0).stick(Axis.LEFT_STICK_X, 1.0)
                .at(2.0).releaseAll();
        OpModeRunner runner = new OpModeRunner(new TeamATeleOp(), RobotConfigs.teamA()).gamepad1(driver);
        PoseEstimator estimator = PoseEstimator.attachTo(runner, DriveModels.teamA());

        runner.run(1.0, 3.0).assertNoException();
        // The final-position report is printed automatically after the test.

        DriveModel teamA = DriveModels.teamA();
        Pose end = estimator.pose();
        // About one second at each full speed. Momentum costs a little while speeding up and
        // gives some back while braking, so allow 10%.
        assertEquals(teamA.forwardInchesPerSecond(), end.xInches(), 0.1 * teamA.forwardInchesPerSecond(), "forward part");
        assertEquals(-teamA.strafeInchesPerSecond(), end.yInches(), 0.1 * teamA.strafeInchesPerSecond(),
                "strafe part (right is -y)");
        assertEquals(0.0, end.headingDegrees(), 1e-6, "no turning");
    }
}
