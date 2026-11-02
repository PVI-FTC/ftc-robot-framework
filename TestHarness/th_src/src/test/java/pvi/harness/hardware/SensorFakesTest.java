package pvi.harness.hardware;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.DigitalChannel;
import com.qualcomm.robotcore.hardware.DistanceSensor;
import com.qualcomm.robotcore.hardware.IMU;
import com.qualcomm.robotcore.hardware.NormalizedColorSensor;
import com.qualcomm.robotcore.hardware.NormalizedRGBA;
import com.qualcomm.robotcore.hardware.TouchSensor;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.AxesOrder;
import org.firstinspires.ftc.robotcore.external.navigation.AxesReference;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.junit.jupiter.api.Test;

import pvi.harness.runtime.OpModeRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RP7: the harness's fake sensors, each read through the real SDK calls by a small
 * TeamCode-style sample OpMode, the way a team's robot code would read them.
 *
 * <p>Motion-driven sensors (the IMU) follow the simulated robot; world-driven ones (distance,
 * touch, color) follow a {@link SensorScript}.</p>
 */
class SensorFakesTest {

    // ---- Sample OpModes, written like TeamCode -------------------------------------------------

    /** Spins the robot in place and shows the IMU's yaw, as a field-centric drive would read it. */
    public static class ImuSpin extends OpMode {
        private IMU imu;
        private ConfigDrive drive;

        @Override
        public void init() {
            imu = hardwareMap.get(IMU.class, "imu");
            imu.initialize(new IMU.Parameters(new com.qualcomm.hardware.rev.RevHubOrientationOnRobot(
                    com.qualcomm.hardware.rev.RevHubOrientationOnRobot.LogoFacingDirection.UP,
                    com.qualcomm.hardware.rev.RevHubOrientationOnRobot.UsbFacingDirection.FORWARD)));
            drive = new ConfigDrive(hardwareMap);
        }

        @Override
        public void loop() {
            // Spin for the first 2 s after INIT, then stop.
            double power = getRuntime() < 2.0 ? 0.5 : 0.0;
            drive.power(-power, power);
            telemetry.addData("Yaw", imu.getRobotYawPitchRollAngles().getYaw(AngleUnit.DEGREES));
            telemetry.addData("First angle", imu.getRobotOrientation(AxesReference.INTRINSIC, AxesOrder.ZYX,
                    AngleUnit.DEGREES).firstAngle);
            telemetry.addData("Yaw rate", imu.getRobotAngularVelocity(AngleUnit.DEGREES).zRotationRate);
        }
    }

    /** Runs a reversed intake roller at 70 % after PLAY. */
    public static class RollerIntake extends OpMode {
        private CRServo roller;

        @Override
        public void init() {
            roller = hardwareMap.get(CRServo.class, "roller");
            roller.setDirection(DcMotorSimple.Direction.REVERSE);
        }

        @Override
        public void loop() {
            roller.setPower(0.7);
        }
    }

    /** Drives forward until the distance sensor sees something closer than 10 in. */
    public static class StopBeforeWall extends OpMode {
        private DistanceSensor front;
        private DcMotor drive;

        @Override
        public void init() {
            front = hardwareMap.get(DistanceSensor.class, "frontDistance");
            drive = hardwareMap.get(DcMotor.class, "drive");
        }

        @Override
        public void loop() {
            double inches = front.getDistance(DistanceUnit.INCH);
            drive.setPower(inches < 10 ? 0.0 : 0.5);
            telemetry.addData("Distance (cm)", "%.1f", front.getDistance(DistanceUnit.CM));
        }
    }

    /** Lowers an arm until its limit switch is pressed, reading the switch both ways REV allows. */
    public static class ArmToLimit extends OpMode {
        private TouchSensor limit;
        private DigitalChannel limitLine;
        private DcMotor arm;

        @Override
        public void init() {
            limit = hardwareMap.get(TouchSensor.class, "armLimit");
            limitLine = hardwareMap.get(DigitalChannel.class, "armLimit");
            limitLine.setMode(DigitalChannel.Mode.INPUT);
            arm = hardwareMap.get(DcMotor.class, "arm");
        }

        @Override
        public void loop() {
            arm.setPower(limit.isPressed() ? 0.0 : -0.3);
            telemetry.addData("Line state", limitLine.getState());
        }
    }

    /** Names the color of the game piece in the intake. */
    public static class PieceColor extends OpMode {
        private NormalizedColorSensor sensor;

        @Override
        public void init() {
            sensor = hardwareMap.get(NormalizedColorSensor.class, "intakeColor");
            sensor.setGain(2.0f);
        }

        @Override
        public void loop() {
            NormalizedRGBA colors = sensor.getNormalizedColors();
            String piece = colors.red > 0.5 && colors.red > colors.blue ? "RED"
                    : colors.blue > 0.5 ? "BLUE" : "NONE";
            telemetry.addData("Piece", piece);
            telemetry.addData("Red count", ((com.qualcomm.robotcore.hardware.ColorSensor) sensor).red());
            telemetry.addData("Gap (in)", "%.1f", ((DistanceSensor) sensor).getDistance(DistanceUnit.INCH));
        }
    }

    // ---- Tests -----------------------------------------------------------------------------------

    /**
     * WHAT: The IMU's yaw follows the robot's real turning: after a spin it equals the change in
     * the simulated heading, in every way the SDK offers to read it.
     */
    @Test
    void imuYawFollowsTheRobotsTurn() {
        FakeHardwareMap robot = RobotConfigs.teamA();
        robot.addImu("imu");
        OpModeRunner runner = new OpModeRunner(new ImuSpin(), robot);
        double startHeading = runner.positionEstimator().pose().headingRadians();

        runner.init().start().loopFor(1.0);

        double turnedDegrees = Math.toDegrees(Math.IEEEremainder(
                runner.positionEstimator().pose().headingRadians() - startHeading, 2 * Math.PI));
        double yaw = Double.parseDouble(runner.telemetry().lastValue("Yaw"));
        assertTrue(Math.abs(turnedDegrees) > 20, "the robot should have turned, turned " + turnedDegrees);
        assertEquals(turnedDegrees, yaw, 0.5);
        assertEquals(yaw, Double.parseDouble(runner.telemetry().lastValue("First angle")), 0.01);
        assertTrue(Math.abs(Double.parseDouble(runner.telemetry().lastValue("Yaw rate"))) > 1, "spinning: rate");
    }

    /** WHAT: resetYaw() makes the current heading 0, and drift adds up over time. */
    @Test
    void imuResetYawAndDrift() {
        FakeImu imu = new FakeImu("imu").setDriftDegreesPerSecond(1.0);
        SensorScript none = new SensorScript();
        for (int i = 0; i <= 500; i++) {   // 10 s of 20 ms steps, robot not moving
            imu.step(new SimulatedSensor.Step(pvi.harness.physics.Pose.ORIGIN, i * 0.02, -1, i == 0 ? 0 : 0.02, none));
        }
        assertEquals(10.0, imu.getRobotYawPitchRollAngles().getYaw(AngleUnit.DEGREES), 1e-6);

        imu.resetYaw();
        assertEquals(0.0, imu.getRobotYawPitchRollAngles().getYaw(AngleUnit.DEGREES), 1e-9);
    }

    /**
     * WHAT: Script times count from INIT for {@code duringInit()} lines and from PLAY for
     * {@code afterStart()} lines, and each value holds until the next change.
     */
    @Test
    void scriptTimesCountFromInitThenFromPlay() {
        SensorScript script = new SensorScript()
                .duringInit().at(0.5).distanceInches("front", 20)
                .afterStart().at(0).distanceInches("front", 10);

        assertEquals(null, script.valueAt("front", Double.class, 0.2, -1));
        assertEquals(20.0, script.valueAt("front", Double.class, 0.6, -1));
        assertEquals(20.0, script.valueAt("front", Double.class, 5.0, -1), "still in INIT");
        assertEquals(10.0, script.valueAt("front", Double.class, 5.0, 0.0), "after PLAY");
        assertEquals(null, script.valueAt("front", Boolean.class, 5.0, 0.0), "no press was scripted");
    }

    /** WHAT: A CR servo records the power and direction the code sets. */
    @Test
    void crServoRecordsPowerAndDirection() {
        FakeHardwareMap robot = new FakeHardwareMap();
        FakeCRServo roller = robot.addCRServo("roller");

        new OpModeRunner(new RollerIntake(), robot).init().start().loop(3);

        assertEquals(0.7, roller.getPower(), 1e-9);
        assertEquals(-0.7, roller.getAppliedPower(), 1e-9);
    }

    /** WHAT: A distance sensor reads the scripted distances, so the robot stops before the "wall". */
    @Test
    void distanceSensorFollowsTheScript() {
        FakeHardwareMap robot = new FakeHardwareMap();
        robot.addMotor("drive");
        robot.addDistanceSensor("frontDistance");
        robot.sensorScript(new SensorScript().afterStart().at(0).distanceInches("frontDistance", 30)
                .at(1.0).distanceInches("frontDistance", 8));
        OpModeRunner runner = new OpModeRunner(new StopBeforeWall(), robot).init().start();

        runner.loopFor(0.5);
        assertEquals(0.5, robot.motor("drive").getPower(), 1e-9);
        assertEquals("76.2", runner.telemetry().lastValue("Distance (cm)"));

        runner.loopFor(0.6);
        assertEquals(0.0, robot.motor("drive").getPower(), 1e-9);
    }

    /** WHAT: Before the script sets a distance, the sensor sees nothing in range, as the SDK reports it. */
    @Test
    void distanceSensorSeesNothingUntilScripted() {
        FakeDistanceSensor sensor = new FakeDistanceSensor("frontDistance");

        assertEquals(DistanceUnit.infinity, sensor.getDistance(DistanceUnit.INCH));
    }

    /**
     * WHAT: A touch sensor is pressed by the script, and reads the REV way both as a
     * TouchSensor and as a DigitalChannel (state true = not pressed).
     */
    @Test
    void touchSensorFollowsTheScriptBothWays() {
        FakeHardwareMap robot = new FakeHardwareMap();
        robot.addMotor("arm");
        robot.addTouchSensor("armLimit");
        robot.sensorScript(new SensorScript().afterStart().at(1.0).press("armLimit"));
        OpModeRunner runner = new OpModeRunner(new ArmToLimit(), robot).init().start();

        runner.loopFor(0.5);
        assertEquals(-0.3, robot.motor("arm").getPower(), 1e-9);
        assertEquals("true", runner.telemetry().lastValue("Line state"));

        runner.loopFor(0.6);
        assertEquals(0.0, robot.motor("arm").getPower(), 1e-9);
        assertEquals("false", runner.telemetry().lastValue("Line state"));
    }

    /**
     * WHAT: A color sensor sees the scripted color (with the gain applied) and its distance
     * part reads the scripted gap.
     */
    @Test
    void colorSensorFollowsTheScript() {
        FakeHardwareMap robot = new FakeHardwareMap();
        robot.addColorSensor("intakeColor");
        robot.sensorScript(new SensorScript()
                .afterStart().at(0).color("intakeColor", 0.05, 0.05, 0.05).distanceInches("intakeColor", 4.0)
                .at(1.0).color("intakeColor", 0.4, 0.05, 0.1).distanceInches("intakeColor", 0.5));
        OpModeRunner runner = new OpModeRunner(new PieceColor(), robot).init().start();

        runner.loopFor(0.5);
        assertEquals("NONE", runner.telemetry().lastValue("Piece"));
        assertEquals("4.0", runner.telemetry().lastValue("Gap (in)"));

        runner.loopFor(0.6);
        // 0.4 red with gain 2 is 0.8 of full scale.
        assertEquals("RED", runner.telemetry().lastValue("Piece"));
        assertEquals(String.valueOf(Math.round(0.8f * 65535)), runner.telemetry().lastValue("Red count"));
        assertEquals("0.5", runner.telemetry().lastValue("Gap (in)"));
    }
}
