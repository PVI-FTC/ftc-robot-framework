package pvi.harness.viewer;

import org.firstinspires.ftc.teamcode.opmodes.autonomous.TeamAAutoOpMode;
import org.firstinspires.ftc.teamcode.opmodes.teleop.RumbleDemoTeleOp;
import org.firstinspires.ftc.teamcode.opmodes.teleop.TeamATeleOp;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import pvi.harness.ds.Axis;
import pvi.harness.ds.Button;
import pvi.harness.ds.GamepadScript;
import pvi.harness.hardware.FakeHardwareMap;
import pvi.harness.hardware.RobotConfigs;
import pvi.harness.runtime.HarnessConfig;
import pvi.harness.runtime.MatchPeriod;
import pvi.harness.runtime.OpModeRunner;

/**
 * PVI extra (built only with {@code pvi.extras=true}): PVI's hand-made viewer scenarios, which run
 * Team A's real OpModes with scripted drives. {@link Scenarios#all()} offers these when they are
 * built, and generic ones from the robot code's OpModes otherwise (manager decision D11).
 *
 * <p>To add one, write a method like {@link #teleOpDemoDrive()} and add it to {@link #all()}.
 * Every other autonomous in TeamCode gets the generic 30 s scenario after these, so a new
 * autonomous shows up by itself (W21).</p>
 */
public final class PviScenarios {

    /** Only static methods here; nothing to create. */
    private PviScenarios() {
    }

    /**
     * Every PVI scenario, in the order the dropdown lists them: the hand-made ones, then the
     * generic 30 s run of each autonomous they don't already cover.
     */
    public static List<Scenarios.Scenario> all() {
        List<Scenarios.Scenario> scenarios = new ArrayList<>(List.of(teamAAutonomous(), teleOpDemoDrive(),
                teleOpTurnInPlace(), teleOpMiswiredFrontLeft(), rumbleDemoMatch()));
        scenarios.addAll(Scenarios.autonomousFromCatalog(HarnessConfig.load(), Set.of(TeamAAutoOpMode.class.getName())));
        return scenarios;
    }

    /** Team A's autonomous: drive forward, run the intake, wait. */
    public static Scenarios.Scenario teamAAutonomous() {
        return new Scenarios.Scenario("teamA-autonomous", "Team A autonomous (30 s)",
                "TeamAAutoOpMode for a whole autonomous period: drives about 9 in forward, then runs the intake.",
                () -> Scenarios.record(new OpModeRunner(new TeamAAutoOpMode(), RobotConfigs.teamA()),
                        MatchPeriod.AUTONOMOUS.seconds()));
    }

    /** A short TeleOp drive with both gamepads in use. */
    public static Scenarios.Scenario teleOpDemoDrive() {
        return new Scenarios.Scenario("teamA-teleop-demo-drive", "Team A TeleOp: demo drive (7 s)",
                "Forward, strafe right, turn, then a curve; the operator starts the intake with A and stops it with B.",
                () -> {
                    GamepadScript driver = new GamepadScript()
                            .at(0.0).stick(Axis.LEFT_STICK_Y, -0.6)
                            .at(1.5).stick(Axis.LEFT_STICK_Y, 0.0).stick(Axis.LEFT_STICK_X, 0.6)
                            .at(3.0).stick(Axis.LEFT_STICK_X, 0.0).stick(Axis.RIGHT_STICK_X, -0.5)
                            .at(4.0).stick(Axis.RIGHT_STICK_X, -0.2).stick(Axis.LEFT_STICK_Y, -0.5)
                            .at(6.0).releaseAll();
                    GamepadScript operator = new GamepadScript()
                            .at(2.0).hold(Button.A, 0.4)
                            .at(4.5).hold(Button.B, 0.4);
                    OpModeRunner runner = new OpModeRunner(new TeamATeleOp(), RobotConfigs.teamA())
                            .gamepad1(driver).gamepad2(operator);
                    return Scenarios.record(runner, 7.0);
                });
    }

    /** Full right stick for half a second: a clockwise turn on the spot. */
    public static Scenarios.Scenario teleOpTurnInPlace() {
        return new Scenarios.Scenario("teamA-teleop-turn", "Team A TeleOp: turn in place (2 s)",
                "Full right stick for 0.5 s: the robot turns clockwise without moving its center.",
                () -> {
                    GamepadScript driver = new GamepadScript()
                            .at(0.0).stick(Axis.RIGHT_STICK_X, 1.0)
                            .at(0.5).releaseAll();
                    return Scenarios.record(new OpModeRunner(new TeamATeleOp(), RobotConfigs.teamA()).gamepad1(driver), 2.0);
                });
    }

    /** Full forward with the front-left motor's wires swapped: the robot curves left. */
    public static Scenarios.Scenario teleOpMiswiredFrontLeft() {
        return new Scenarios.Scenario("teamA-teleop-miswired", "Team A TeleOp: miswired front-left motor (2 s)",
                "Full forward for 1 s, but the front-left wires are swapped, so the robot curves off to the left.",
                () -> {
                    GamepadScript driver = new GamepadScript()
                            .at(0.0).stick(Axis.LEFT_STICK_Y, -1.0)
                            .at(1.0).releaseAll();
                    FakeHardwareMap robot = RobotConfigs.teamA().miswire("frontLeft");
                    return Scenarios.record(new OpModeRunner(new TeamATeleOp(), robot).gamepad1(driver), 2.0);
                });
    }

    /** The rumble demo for a whole TeleOp match: watch the rumble counts in the side panel. */
    public static Scenarios.Scenario rumbleDemoMatch() {
        return new Scenarios.Scenario("rumble-demo-match", "Rumble demo TeleOp (2:00 match)",
                "RumbleDemoTeleOp for a whole match: the driver's gamepad buzzes at PLAY, 90 s, 105 s and 115 s.",
                () -> Scenarios.record(new OpModeRunner(new RumbleDemoTeleOp(), RobotConfigs.teamA()),
                        MatchPeriod.TELEOP.seconds()));
    }
}
