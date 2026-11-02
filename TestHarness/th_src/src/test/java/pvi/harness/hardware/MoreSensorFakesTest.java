package pvi.harness.hardware;

import com.qualcomm.hardware.limelightvision.LLResult;
import com.qualcomm.hardware.limelightvision.LLResultTypes;
import com.qualcomm.hardware.limelightvision.Limelight3A;
import com.qualcomm.hardware.sparkfun.SparkFunOTOS;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.hardware.AnalogInput;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.junit.jupiter.api.Test;

import pvi.harness.physics.Pose;
import pvi.harness.runtime.OpModeRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RP7, second batch: analog input, SparkFun OTOS, dead wheels and the Limelight 3A, each read
 * through the real SDK calls by a small TeamCode-style sample OpMode.
 *
 * <p>The OTOS and dead wheels follow the simulated robot's motion; the analog input and the
 * Limelight follow a {@link SensorScript}. None of them becomes the harness's own position
 * source (mecanum + Pinpoint only).</p>
 */
class MoreSensorFakesTest {

    // ---- Sample OpModes, written like TeamCode -------------------------------------------------

    /** Turns an arm potentiometer's volts into degrees (0..270 over 0..3.3 V). */
    public static class ArmPot extends OpMode {
        private AnalogInput pot;

        @Override
        public void init() {
            pot = hardwareMap.get(AnalogInput.class, "armPot");
        }

        @Override
        public void loop() {
            double degrees = pot.getVoltage() / pot.getMaxVoltage() * 270.0;
            telemetry.addData("Arm (deg)", "%.1f", degrees);
        }
    }

    /** Drives the config robot forward and shows the OTOS's pose, started at (10, 5, 90 deg). */
    public static class OtosDrive extends OpMode {
        private SparkFunOTOS otos;
        private ConfigDrive drive;

        @Override
        public void init() {
            otos = hardwareMap.get(SparkFunOTOS.class, "otos");
            otos.setLinearUnit(DistanceUnit.INCH);
            otos.setAngularUnit(AngleUnit.DEGREES);
            otos.begin();
            otos.resetTracking();
            otos.setPosition(new SparkFunOTOS.Pose2D(10, 5, 90));
            drive = new ConfigDrive(hardwareMap);
        }

        @Override
        public void loop() {
            drive.power(0.5, 0.5);
            SparkFunOTOS.Pose2D pose = otos.getPosition();
            telemetry.addData("OTOS x", pose.x);
            telemetry.addData("OTOS y", pose.y);
            telemetry.addData("OTOS h", pose.h);
        }
    }

    /** Spins the config robot in place and shows two dead wheels' counts. */
    public static class DeadWheelSpin extends OpMode {
        private DcMotorEx parallelPod;
        private DcMotorEx perpendicularPod;
        private ConfigDrive drive;

        @Override
        public void init() {
            parallelPod = hardwareMap.get(DcMotorEx.class, "leftOdo");
            perpendicularPod = hardwareMap.get(DcMotorEx.class, "strafeOdo");
            parallelPod.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
            perpendicularPod.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
            drive = new ConfigDrive(hardwareMap);
        }

        @Override
        public void loop() {
            drive.power(-0.5, 0.5);
            telemetry.addData("Parallel ticks", parallelPod.getCurrentPosition());
            telemetry.addData("Perpendicular ticks", perpendicularPod.getCurrentPosition());
        }
    }

    /** Reads the Limelight like KimCode's TeleOp: a target's angles, and the AprilTags in view. */
    public static class LimelightAim extends OpMode {
        private Limelight3A limelight;

        @Override
        public void init() {
            limelight = hardwareMap.get(Limelight3A.class, "limelight");
            limelight.pipelineSwitch(0);
            limelight.start();
        }

        @Override
        public void loop() {
            LLResult result = limelight.getLatestResult();
            if (result == null || !result.isValid()) {
                telemetry.addData("Target", "none");
                return;
            }
            telemetry.addData("Target", "tx %.1f ty %.1f ta %.1f", result.getTx(), result.getTy(), result.getTa());
            StringBuilder ids = new StringBuilder();
            for (LLResultTypes.FiducialResult fiducial : result.getFiducialResults()) {
                ids.append(fiducial.getFiducialId()).append(' ');
            }
            telemetry.addData("Tags", ids.toString().trim());
        }
    }

    // ---- Tests -----------------------------------------------------------------------------------

    /** WHAT: An analog input reads the scripted volts, and its maximum is the hub's 3.3 V. */
    @Test
    void analogInputFollowsTheScript() {
        FakeHardwareMap robot = new FakeHardwareMap();
        robot.addAnalogInput("armPot");
        robot.sensorScript(new SensorScript().afterStart().at(0).volts("armPot", 0.0).at(1.0).volts("armPot", 1.65));
        OpModeRunner runner = new OpModeRunner(new ArmPot(), robot).init().start();

        runner.loopFor(0.5);
        assertEquals("0.0", runner.telemetry().lastValue("Arm (deg)"));
        runner.loopFor(0.6);
        assertEquals("135.0", runner.telemetry().lastValue("Arm (deg)"));
    }

    /**
     * WHAT: The OTOS follows the robot's real motion in its own frame: after setPosition(10, 5,
     * 90 deg), driving forward moves its y up (its frame is turned 90 degrees) by exactly the
     * distance the robot really drove.
     */
    @Test
    void otosFollowsTheRobotInItsOwnFrame() {
        FakeHardwareMap robot = RobotConfigs.teamA();
        robot.addOtos("otos");
        OpModeRunner runner = new OpModeRunner(new OtosDrive(), robot);
        Pose start = runner.positionEstimator().pose();

        runner.init().start().loopFor(1.0);

        Pose end = runner.positionEstimator().pose();
        double drove = Math.hypot(end.xInches() - start.xInches(), end.yInches() - start.yInches());
        assertTrue(drove > 5, "the robot should have driven, drove " + drove);
        assertEquals(10.0, Double.parseDouble(runner.telemetry().lastValue("OTOS x")), 0.05);
        assertEquals(5.0 + drove, Double.parseDouble(runner.telemetry().lastValue("OTOS y")), 0.05);
        assertEquals(90.0, Double.parseDouble(runner.telemetry().lastValue("OTOS h")), 0.5);
    }

    /**
     * WHAT: Dead wheels count the robot's real motion: spinning in place, a parallel pod 7 in
     * left of center rolls backwards by 7 in per radian, and a perpendicular pod 3 in ahead
     * rolls left by 3 in per radian, each in goBILDA pod counts.
     */
    @Test
    void deadWheelsCountTheRobotsRealMotion() {
        FakeHardwareMap robot = RobotConfigs.teamA();
        robot.addDeadWheel(FakeDeadWheel.parallel("leftOdo", 7.0));
        robot.addDeadWheel(FakeDeadWheel.perpendicular("strafeOdo", 3.0));
        OpModeRunner runner = new OpModeRunner(new DeadWheelSpin(), robot);

        runner.init().start().loopFor(1.0);

        double turned = totalTurnRadians(runner);
        assertTrue(Math.abs(turned) > 0.3, "the robot should have spun, turned " + turned);
        double ticksPerInch = FakeDeadWheel.GOBILDA_TICKS_PER_INCH;
        assertEquals(-7.0 * turned * ticksPerInch, Double.parseDouble(runner.telemetry().lastValue("Parallel ticks")), 25);
        assertEquals(3.0 * turned * ticksPerInch, Double.parseDouble(runner.telemetry().lastValue("Perpendicular ticks")), 25);
    }

    /**
     * WHAT: The Limelight shows the scripted target and AprilTags through the SDK's own result
     * type, and nothing before its first target.
     */
    @Test
    void limelightShowsTheScriptedTargetsThroughTheSdk() {
        FakeHardwareMap robot = new FakeHardwareMap();
        FakeLimelight limelight = robot.addLimelight("limelight");
        robot.sensorScript(new SensorScript().afterStart()
                .at(1.0).set("limelight", LimelightView.target(-4.5, 12.0, 1.8))
                .at(2.0).set("limelight", LimelightView.tags(SeenAprilTag.straightAhead(20), SeenAprilTag.straightAhead(24))));
        OpModeRunner runner = new OpModeRunner(new LimelightAim(), robot).init().start();

        runner.loopFor(0.5);
        assertEquals("none", runner.telemetry().lastValue("Target"));

        runner.loopFor(1.0);
        assertEquals("tx -4.5 ty 12.0 ta 1.8", runner.telemetry().lastValue("Target"));

        runner.loopFor(1.0);
        assertEquals("20 24", runner.telemetry().lastValue("Tags"));
        assertEquals(0, limelight.pipeline());
    }

    // ------------------------------------------------------------------------------------------

    /** How far the robot turned during the run, in radians (counter-clockwise positive). */
    private static double totalTurnRadians(OpModeRunner runner) {
        var track = runner.positionEstimator().track();
        double total = 0;
        for (int i = 1; i < track.size(); i++) {
            total += Math.IEEEremainder(track.get(i).pose().headingRadians() - track.get(i - 1).pose().headingRadians(),
                    2 * Math.PI);
        }
        return total;
    }
}
