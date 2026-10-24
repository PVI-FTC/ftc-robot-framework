package pvi.harness.hardware;

import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.robotcore.external.navigation.Pose2D;
import org.firstinspires.ftc.robotcore.external.navigation.UnnormalizedAngleUnit;
import org.firstinspires.ftc.teamcode.opmodes.autonomous.TeamAPedroPilotAutoOpMode;
import org.firstinspires.ftc.teamcode.robots.teamA.TeamAPedroHarnessAccess;
import org.junit.jupiter.api.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pvi.harness.ds.Button;
import pvi.harness.ds.GamepadScript;
import pvi.harness.physics.Pose;
import pvi.harness.runtime.OpModeRunner;
import pvi.harness.runtime.RunResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 5, step 1: fake sensors that follow the simulated robot, so code that reads position
 * (Pedro, through the goBILDA Pinpoint) or motor encoders works in tests.
 *
 * <p>The fake Pinpoint ({@link FakePinpoint}) reports the position estimator's pose; motor
 * encoders ({@link FakeDcMotorEx#step(double)}) count with motor power. Both are moved by the
 * runner before every OpMode call.</p>
 */
class SensorsTest {
    private static final double INCH = 1e-6;

    // ---- The fake Pinpoint on its own --------------------------------------------------------

    /**
     * WHAT: After a reset, the Pinpoint measures from where the robot was, in the robot's own
     * directions: X forward, Y left.
     *
     * <p>HOW: The robot starts at (10, 20) facing +y (90°) on the field and drives 12 in along
     * +y, which is straight ahead for it.</p>
     *
     * <p>WHY: That is how the real device behaves, and what Pedro's localizer expects.</p>
     */
    @Test
    void pinpointMeasuresFromTheResetInTheRobotsDirections() {
        FakePinpoint pinpoint = new FakePinpoint("pinpoint");
        pinpoint.step(Pose.inches(10, 20, 90), 0.0);
        pinpoint.resetPosAndIMU();

        pinpoint.step(Pose.inches(10, 32, 90), 1.0);
        pinpoint.update();

        assertEquals(12.0, pinpoint.getPosX(DistanceUnit.INCH), INCH, "forward is +X");
        assertEquals(0.0, pinpoint.getPosY(DistanceUnit.INCH), INCH);
        assertEquals(0.0, pinpoint.getHeading(AngleUnit.DEGREES), INCH);
        assertEquals(12.0, pinpoint.getVelX(DistanceUnit.INCH), INCH, "12 in in 1 s, forward");
        assertEquals(0.0, pinpoint.getVelY(DistanceUnit.INCH), INCH);
    }

    /**
     * WHAT: setPosition() moves the Pinpoint's frame: it then reports the set pose and moves
     * from there.
     *
     * <p>WHY: Pedro calls setPosition() with the path's start pose (for example (56, 8, 90°)).
     * Everything Pedro does afterwards relies on this.</p>
     */
    @Test
    void setPositionMovesTheFrame() {
        FakePinpoint pinpoint = new FakePinpoint("pinpoint");
        pinpoint.step(Pose.ORIGIN, 0.0);
        pinpoint.setPosition(new Pose2D(DistanceUnit.INCH, 56, 8, AngleUnit.DEGREES, 90));

        // The robot drives 10 in straight ahead (field +x for the harness, which started facing +x).
        pinpoint.step(Pose.inches(10, 0, 0), 0.5);
        pinpoint.update();

        Pose2D position = pinpoint.getPosition();
        assertEquals(56.0, position.getX(DistanceUnit.INCH), INCH, "straight ahead is +y in Pedro's frame");
        assertEquals(18.0, position.getY(DistanceUnit.INCH), INCH);
        assertEquals(90.0, position.getHeading(AngleUnit.DEGREES), INCH);
        assertEquals(0.0, pinpoint.getVelX(DistanceUnit.INCH), INCH);
        assertEquals(20.0, pinpoint.getVelY(DistanceUnit.INCH), INCH);
    }

    /**
     * WHAT: Values only change when the code calls update(), like the real device's bulk read.
     *
     * <p>WHY: Code that forgets update() reads stale positions on the robot. The harness must
     * show the same bug instead of hiding it.</p>
     */
    @Test
    void readingsOnlyChangeOnUpdate() {
        FakePinpoint pinpoint = new FakePinpoint("pinpoint");
        pinpoint.step(Pose.ORIGIN, 0.0);
        pinpoint.resetPosAndIMU();

        pinpoint.step(Pose.inches(5, 0, 0), 0.1);
        assertEquals(0.0, pinpoint.getPosX(DistanceUnit.INCH), INCH, "before update()");
        pinpoint.update();
        assertEquals(5.0, pinpoint.getPosX(DistanceUnit.INCH), INCH, "after update()");
        assertEquals(1, pinpoint.updateCount() - 1, "reset reads once, then one update");
    }

    /**
     * WHAT: Raw pod counts follow the pod offsets and resolution the code set. Driving forward
     * turns only the forward pod; turning on the spot turns the pods by their distance from
     * the center.
     *
     * <p>HOW: 4-bar pods (19.894 counts/mm). Forward pod 6.25 in to the right (offset -6.25),
     * strafe pod 10 in behind (offset -10), as in Team A's configuration. Drive 10 in forward,
     * then turn 90° left on the spot.</p>
     *
     * <p>WHY: Tuning OpModes read the raw counts to check pod setup.</p>
     */
    @Test
    void podCountsFollowOffsetsAndResolution() {
        FakePinpoint pinpoint = new FakePinpoint("pinpoint");
        pinpoint.setOffsets(-6.25, -10.0, DistanceUnit.INCH);
        pinpoint.setEncoderResolution(GoBildaPinpointDriver.GoBildaOdometryPods.goBILDA_4_BAR_POD);
        pinpoint.step(Pose.ORIGIN, 0.0);
        pinpoint.resetPosAndIMU();

        pinpoint.step(Pose.inches(10, 0, 0), 1.0);
        pinpoint.update();
        assertEquals(Math.round(254.0 * FakePinpoint.FOUR_BAR_TICKS_PER_MM), pinpoint.getEncoderX(), 1);
        assertEquals(0, pinpoint.getEncoderY());

        pinpoint.step(Pose.inches(10, 0, 90), 1.0);
        pinpoint.update();
        double quarterTurn = Math.PI / 2;
        // A left turn moves a pod 6.25 in to the right forward, and a pod 10 in behind to the right.
        assertEquals(Math.round((254.0 + 6.25 * quarterTurn * 25.4) * FakePinpoint.FOUR_BAR_TICKS_PER_MM),
                pinpoint.getEncoderX(), 1);
        assertEquals(Math.round(-10.0 * quarterTurn * 25.4 * FakePinpoint.FOUR_BAR_TICKS_PER_MM),
                pinpoint.getEncoderY(), 1);
        assertEquals(90.0, pinpoint.getHeading(AngleUnit.DEGREES), INCH);
    }

    /**
     * WHAT: The unwrapped heading keeps counting past 180°.
     *
     * <p>WHY: Pedro uses it to know how far the robot has turned in total.</p>
     */
    @Test
    void unwrappedHeadingKeepsCounting() {
        FakePinpoint pinpoint = new FakePinpoint("pinpoint");
        pinpoint.step(Pose.ORIGIN, 0.0);
        pinpoint.resetPosAndIMU();
        for (int degrees = 30; degrees <= 270; degrees += 30) {
            pinpoint.step(Pose.inches(0, 0, degrees), 0.1);
        }
        pinpoint.update();

        assertEquals(270.0, pinpoint.getHeading(UnnormalizedAngleUnit.DEGREES), 1e-6);
        assertEquals(-90.0, pinpoint.getHeading(AngleUnit.DEGREES), 1e-6);
    }

    // ---- Motor encoders -------------------------------------------------------------------

    /**
     * WHAT: Encoders count with power: full power for one second is the full-speed count, and
     * positive power counts up even on a REVERSE motor, as on the robot.
     *
     * <p>WHY: Code that drives "until the encoder reads N" must see counts move.</p>
     */
    @Test
    void encoderCountsWithPower() {
        FakeDcMotorEx motor = new FakeDcMotorEx("lift", 0);
        motor.setDirection(DcMotorSimple.Direction.REVERSE);
        motor.setPower(0.5);
        for (int i = 0; i < 50; i++) {
            motor.step(0.02);
        }

        assertEquals(Math.round(0.5 * FakeDcMotorEx.FULL_SPEED_TICKS_PER_SECOND), motor.getCurrentPosition(), 1);
        assertEquals(0.5 * FakeDcMotorEx.FULL_SPEED_TICKS_PER_SECOND, motor.getVelocity(), 1e-6);
    }

    /**
     * WHAT: RUN_TO_POSITION drives to the target and stops there; isBusy() goes false.
     * STOP_AND_RESET_ENCODER zeroes the count.
     *
     * <p>WHY: Lifts and arms usually use RUN_TO_POSITION.</p>
     */
    @Test
    void runToPositionStopsOnTheTarget() {
        FakeDcMotorEx motor = new FakeDcMotorEx("lift", 0);
        motor.setTargetPosition(1000);
        motor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        motor.setPower(1.0);
        motor.step(0.2);
        assertTrue(motor.isBusy(), "about 560 of 1000 ticks after 0.2 s");
        motor.step(0.5);

        assertEquals(1000, motor.getCurrentPosition());
        assertFalse(motor.isBusy());

        motor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        assertEquals(0, motor.getCurrentPosition());
    }

    /**
     * WHAT: A miswired motor's encoder counts against its power.
     *
     * <p>WHY: On the robot, swapped power wires make the motor turn the wrong way while the
     * encoder wires are still right, so counts go down for positive power.</p>
     */
    @Test
    void miswiredMotorCountsBackwards() {
        FakeDcMotorEx motor = new FakeDcMotorEx("lift", 0);
        motor.setMiswired(true);
        motor.setPower(1.0);
        motor.step(1.0);

        assertTrue(motor.getCurrentPosition() < 0, "count was " + motor.getCurrentPosition());
    }

    /**
     * WHAT: setVelocity() in RUN_USING_ENCODER runs at that speed, also when given in degrees
     * per second.
     */
    @Test
    void setVelocityRunsAtThatSpeed() {
        FakeDcMotorEx motor = new FakeDcMotorEx("flywheel", 0);
        motor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        motor.setVelocity(360.0, AngleUnit.DEGREES);   // one turn per second
        motor.step(1.0);

        assertEquals(Math.round(FakeDcMotorEx.PLACEHOLDER_TICKS_PER_REV), motor.getCurrentPosition(), 1);
        assertEquals(360.0, motor.getVelocity(AngleUnit.DEGREES), 1e-6);
    }

    // ---- Real robot code ---------------------------------------------------------------------

    /**
     * WHAT: Team A's fake robot has a Pinpoint under the name its Pedro configuration uses, and
     * a battery voltage reading.
     *
     * <p>WHY: Pedro looks up both while building its follower; without them it crashes.</p>
     */
    @Test
    void teamARobotHasAPinpointAndBatteryVoltage() {
        FakeHardwareMap robot = RobotConfigs.teamA();
        String name = TeamAPedroHarnessAccess.recordedPinpointConstants().hardwareMapName;

        assertNotNull(robot.get(GoBildaPinpointDriver.class, name));
        assertEquals(FakeVoltageSensor.DEFAULT_VOLTS, robot.voltageSensor.iterator().next().getVoltage(), 1e-9);
    }

    /**
     * WHAT: A real Pedro route runs on Team A's fake robot, and Pedro's position matches where
     * the harness moved the robot.
     *
     * <p>HOW: Picks "DECODE Route" in Team A's Pedro path menu and runs a 30 s autonomous.
     * The route starts at Pedro's (56, 8, 90°); the harness starts at its own origin facing +x.
     * After the route, Pedro's pose (from the Driver Station screen) must equal the route start
     * moved by exactly what the harness says the robot did.</p>
     *
     * <p>WHY: Proves Pedro's localization reads the simulated robot correctly. (How well Pedro
     * follows the route is checked in {@code PedroRouteTest}, with the robot placed at the
     * route's real start; here it starts at the field center and may meet a wall.)</p>
     */
    @Test
    void pedroRouteRunsAndPedroSeesWhereTheRobotWent() {
        GamepadScript driver = new GamepadScript().duringInit().at(0.2)
                .tap(Button.DPAD_DOWN)          // LP-11 Pilot -> DECODE Route
                .tap(Button.RIGHT_BUMPER);      // select
        OpModeRunner runner = new OpModeRunner(new TeamAPedroPilotAutoOpMode(), RobotConfigs.teamA())
                .gamepad1(driver);

        RunResult result = runner.run(1.0, 30.0);

        result.assertNoException();
        assertEquals("DECODE Route", result.telemetry().lastValue("Path"));
        assertTrue(runner.hardwareMap().pinpoint("pinpoint").updateCount() > 1000, "Pedro read the Pinpoint every loop");

        double[] start = parsePose(result.telemetry().lastValue("Expected Start"));
        double[] pedro = parsePose(result.telemetry().lastValue("Current Pose"));
        Pose harness = runner.positionEstimator().pose();
        assertTrue(harness.distanceInchesTo(Pose.ORIGIN) > 12.0, "the robot really moved: " + harness);

        // The harness's motion (from its origin facing +x), turned onto Pedro's start heading.
        double turn = Math.toRadians(start[2]);
        double expectedX = start[0] + harness.xInches() * Math.cos(turn) - harness.yInches() * Math.sin(turn);
        double expectedY = start[1] + harness.xInches() * Math.sin(turn) + harness.yInches() * Math.cos(turn);
        double expectedHeading = start[2] + harness.headingDegrees();
        assertEquals(expectedX, pedro[0], 0.01, "Pedro x");
        assertEquals(expectedY, pedro[1], 0.01, "Pedro y");
        assertEquals(0.0, Math.IEEEremainder(expectedHeading - pedro[2], 360.0), 0.01, "Pedro heading");
    }

    /** "(37.544, 68.559, 160.990 deg)" as {x, y, degrees}. */
    private static double[] parsePose(String text) {
        Matcher m = Pattern.compile("\\(([-\\d.]+), ([-\\d.]+), ([-\\d.]+) deg\\)").matcher(String.valueOf(text));
        assertTrue(m.find(), "not a pose: " + text);
        return new double[]{Double.parseDouble(m.group(1)), Double.parseDouble(m.group(2)), Double.parseDouble(m.group(3))};
    }
}
