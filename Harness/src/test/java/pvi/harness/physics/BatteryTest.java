package pvi.harness.physics;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.pedropathing.ftc.drivetrains.Mecanum;
import com.pedropathing.ftc.drivetrains.MecanumConstants;
import com.pedropathing.math.Vector;

import org.firstinspires.ftc.teamcode.opmodes.autonomous.TeamAPedroPilotAutoOpMode;
import org.firstinspires.ftc.teamcode.opmodes.teleop.TeamATeleOp;
import org.firstinspires.ftc.teamcode.robots.teamA.TeamAPedroHarnessAccess;
import org.firstinspires.ftc.teamcode.robots.teamA.TeamAPedroPathRoute;
import org.junit.jupiter.api.Test;

import pvi.harness.ds.Axis;
import pvi.harness.ds.Button;
import pvi.harness.ds.GamepadScript;
import pvi.harness.hardware.FakeHardwareMap;
import pvi.harness.hardware.RobotConfigs;
import pvi.harness.runtime.OpModeRunner;
import pvi.harness.runtime.RunResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * W3: battery voltage in the physics. A motor's speed is about proportional to its voltage, so
 * a tired battery makes the simulated robot slower, a sagging battery slows it over a match,
 * and Pedro's voltage compensation (when a team turns it on) reads the same battery.
 */
class BatteryTest {

    /** Drives the real Team A TeleOp full forward for 1 s on a battery at {@code volts}; returns how far it went. */
    private static double oneSecondForwardAt(double volts) {
        GamepadScript driver = new GamepadScript()
                .at(0.0).stick(Axis.LEFT_STICK_Y, -1.0)
                .at(1.0).stick(Axis.LEFT_STICK_Y, 0.0);
        FakeHardwareMap robot = RobotConfigs.teamA();
        robot.batteryVoltage().setVoltage(volts);
        OpModeRunner runner = new OpModeRunner(new TeamATeleOp(), robot).gamepad1(driver);
        // Instant speed changes keep the comparison exact: only the voltage differs.
        PoseEstimator estimator = PoseEstimator.attachTo(runner, DriveModels.teamA().withDynamics(null));
        runner.init().start();
        runner.loopUntil(1.5);
        return estimator.pose().xInches();
    }

    /**
     * WHAT: On an 11 V battery the robot drives 11 / 12.5 = 88% as far as on a charged 12.5 V
     * battery, for the same power.
     *
     * <p>WHY: A tired battery is a real reason autonomous routines fall short. The harness
     * should show it, not hide it.</p>
     */
    @Test
    void aTiredBatteryDrivesSlowerInProportion() {
        double charged = oneSecondForwardAt(12.5);
        double tired = oneSecondForwardAt(11.0);

        assertTrue(charged > 50, "drove " + charged + " in on a charged battery");
        assertEquals(11.0 / 12.5, tired / charged, 0.005, "distance scales with voltage");
    }

    /**
     * WHAT: With sag set, the battery voltage drops steadily with simulated time, and never
     * below the brown-out floor.
     *
     * <p>WHY: Batteries drop during a match; a routine that works at the start may not at the end.</p>
     */
    @Test
    void theBatterySagsWithSimulatedTime() {
        FakeHardwareMap robot = RobotConfigs.teamA();
        robot.batteryVoltage().setSag(0.5);   // volts per minute
        OpModeRunner runner = new OpModeRunner(new TeamATeleOp(), robot);
        runner.init().start();
        runner.loopUntil(60.0);

        double minutes = runner.clock().sinceInit() / 60.0;
        assertEquals(12.5 - 0.5 * minutes, robot.batteryVoltage().getVoltage(), 0.02,
                "0.5 V per minute of simulated time");

        robot.batteryVoltage().setSag(100.0);
        robot.batteryVoltage().step(60.0);
        assertEquals(pvi.harness.hardware.FakeVoltageSensor.LOWEST_VOLTS, robot.batteryVoltage().getVoltage(), 1e-9);
    }

    /**
     * WHAT: With Pedro's voltage compensation turned on, Pedro reads the fake battery and asks
     * for more power on a low battery, as on the robot.
     *
     * <p>HOW: Calls Pedro's own mecanum drivetrain directly (Team A's configuration has
     * compensation off, so the routes never use it), once at 12 V and once at 10 V.</p>
     *
     * <p>WHY: Teams that turn compensation on can test it in the harness.</p>
     */
    @Test
    void pedroVoltageCompensationReadsTheBattery() {
        FakeHardwareMap robot = RobotConfigs.teamA();
        MecanumConstants constants = TeamAPedroHarnessAccess.recordedMecanumConstants()
                .useVoltageCompensation(true).nominalVoltage(12.0)
                // Team A caps power low for safety; lift the cap so the compensated power shows.
                .maxPower(1.0);
        Mecanum drive = new Mecanum(robot, constants);
        drive.updateConstants();   // Pedro's follower does this; it reads the compensation settings
        Vector none = new Vector(0, 0);
        Vector halfForward = new Vector(0.3, 0);

        robot.batteryVoltage().setVoltage(12.0);
        double[] atNominal = drive.calculateDrive(none, none, halfForward, 0.0);
        robot.batteryVoltage().setVoltage(10.0);
        double[] atLow = drive.calculateDrive(none, none, halfForward, 0.0);

        assertEquals(10.0, drive.getVoltage(), 1e-9, "Pedro reads the fake battery");
        for (int i = 0; i < 4; i++) {
            assertTrue(Math.abs(atLow[i]) > Math.abs(atNominal[i]) * 1.1,
                    "wheel " + i + ": " + atLow[i] + " at 10 V should be well above " + atNominal[i] + " at 12 V");
        }
    }

    /**
     * WHAT: On a tired 11 V battery, Team A's DECODE route still finishes within 1 in of its end.
     *
     * <p>WHY: Pedro's feedback should make up for a slower robot. If a future change makes
     * the route sensitive to the battery, this shows it.</p>
     */
    @Test
    void decodeRouteStillFinishesOnATiredBattery() {
        TeamAPedroPathRoute route = TeamAPedroPathRoute.DECODE_ROUTE;
        GamepadScript driver = new GamepadScript().duringInit().at(0.2)
                .tap(Button.DPAD_DOWN, 1)          // LP-11 Pilot -> DECODE Route
                .tap(Button.RIGHT_BUMPER);
        FakeHardwareMap robot = RobotConfigs.teamA();
        robot.batteryVoltage().setVoltage(11.0);
        OpModeRunner runner = new OpModeRunner(new TeamAPedroPilotAutoOpMode(), robot).gamepad1(driver);
        DriveModel teamA = DriveModels.teamA();
        Pose start = Pose.fromPedro(route.getStartXInches(), route.getStartYInches(),
                Math.toDegrees(route.getStartHeadingRadians()));
        // Nudge the placeholder-sized robot inside the walls (see PedroRouteTest).
        double limit = Pose.FIELD_HALF_WIDTH_INCHES - teamA.lengthInches() / 2.0;
        start = new Pose(Math.max(-limit, Math.min(limit, start.xInches())),
                Math.max(-limit, Math.min(limit, start.yInches())), start.headingRadians());
        PoseEstimator.attachTo(runner, teamA, WheelReader.teamA(), start);

        RunResult result = runner.run(1.0, 30.0);

        result.assertNoException();
        assertEquals("DECODE Route", result.telemetry().lastValue("Path"));
        assertEquals("true", result.telemetry().lastValue("Sequence Complete"));
        Matcher m = Pattern.compile("([-\\d.]+) in").matcher(String.valueOf(result.telemetry().lastValue("End Position Error")));
        assertTrue(m.find(), "no end error: " + result.telemetry().lastValue("End Position Error"));
        assertTrue(Double.parseDouble(m.group(1)) < 1.0, "ended " + m.group(1) + " in off on an 11 V battery");
    }
}
