package pvi.harness.viewer;

import org.firstinspires.ftc.teamcode.opmodes.autonomous.TeamAAutoOpMode;
import org.firstinspires.ftc.teamcode.opmodes.teleop.RumbleDemoTeleOp;
import org.firstinspires.ftc.teamcode.opmodes.teleop.TeamATeleOp;

import java.util.List;
import java.util.function.Supplier;

import pvi.harness.ds.Axis;
import pvi.harness.ds.Button;
import pvi.harness.ds.GamepadScript;
import pvi.harness.hardware.FakeHardwareMap;
import pvi.harness.hardware.RobotConfigs;
import pvi.harness.runtime.MatchPeriod;
import pvi.harness.runtime.OpModeRunner;
import pvi.harness.runtime.RunRecorder;
import pvi.harness.runtime.RunRecording;
import pvi.harness.runtime.RunnerRegistry;

/**
 * Ready-made runs the viewer's dropdown can perform: each runs the real, unchanged TeamCode in
 * the harness (fake hardware, scripted gamepads, simulated time) and returns its recording.
 *
 * <p>To add one, write a method like {@link #teleOpDemoDrive()} and add it to {@link #all()}.</p>
 */
public final class Scenarios {

    /**
     * One ready-made run.
     *
     * @param id          short name, used for the saved file, e.g. "teamA-autonomous"
     * @param title       what the dropdown shows, e.g. "Team A autonomous (30 s)"
     * @param description one sentence on what happens
     * @param run         runs it and returns the recording
     */
    public record Scenario(String id, String title, String description, Supplier<RunRecording> run) {

        /**
         * Runs the scenario. Takes well under a second, because time is simulated. The robot
         * code reads simulated time only when the harness Java agent is loaded (as in tests and
         * {@code gradlew viewRecording}).
         */
        public RunRecording perform() {
            // Each run makes a new OpModeRunner; forget the previous ones (only tests read them).
            RunnerRegistry.startTest();
            return run.get();
        }
    }

    /** Only static methods here; nothing to create. */
    private Scenarios() {
    }

    /** Every scenario, in the order the dropdown lists them. */
    public static List<Scenario> all() {
        return List.of(teamAAutonomous(), teleOpDemoDrive(), teleOpTurnInPlace(), teleOpMiswiredFrontLeft(),
                rumbleDemoMatch());
    }

    /** Team A's autonomous: drive forward, run the intake, wait. */
    public static Scenario teamAAutonomous() {
        return new Scenario("teamA-autonomous", "Team A autonomous (30 s)",
                "TeamAAutoOpMode for a whole autonomous period: drives about 9 in forward, then runs the intake.",
                () -> record(new OpModeRunner(new TeamAAutoOpMode(), RobotConfigs.teamA()), MatchPeriod.AUTONOMOUS.seconds()));
    }

    /** A short TeleOp drive with both gamepads in use. */
    public static Scenario teleOpDemoDrive() {
        return new Scenario("teamA-teleop-demo-drive", "Team A TeleOp: demo drive (7 s)",
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
                    return record(runner, 7.0);
                });
    }

    /** Full right stick for half a second: a clockwise turn on the spot. */
    public static Scenario teleOpTurnInPlace() {
        return new Scenario("teamA-teleop-turn", "Team A TeleOp: turn in place (2 s)",
                "Full right stick for 0.5 s: the robot turns clockwise without moving its center.",
                () -> {
                    GamepadScript driver = new GamepadScript()
                            .at(0.0).stick(Axis.RIGHT_STICK_X, 1.0)
                            .at(0.5).releaseAll();
                    return record(new OpModeRunner(new TeamATeleOp(), RobotConfigs.teamA()).gamepad1(driver), 2.0);
                });
    }

    /** Full forward with the front-left motor's wires swapped: the robot curves left. */
    public static Scenario teleOpMiswiredFrontLeft() {
        return new Scenario("teamA-teleop-miswired", "Team A TeleOp: miswired front-left motor (2 s)",
                "Full forward for 1 s, but the front-left wires are swapped, so the robot curves off to the left.",
                () -> {
                    GamepadScript driver = new GamepadScript()
                            .at(0.0).stick(Axis.LEFT_STICK_Y, -1.0)
                            .at(1.0).releaseAll();
                    FakeHardwareMap robot = RobotConfigs.teamA().miswire("frontLeft");
                    return record(new OpModeRunner(new TeamATeleOp(), robot).gamepad1(driver), 2.0);
                });
    }

    /** The rumble demo for a whole TeleOp match: watch the rumble counts in the side panel. */
    public static Scenario rumbleDemoMatch() {
        return new Scenario("rumble-demo-match", "Rumble demo TeleOp (2:00 match)",
                "RumbleDemoTeleOp for a whole match: the driver's gamepad buzzes at PLAY, 90 s, 105 s and 115 s.",
                () -> record(new OpModeRunner(new RumbleDemoTeleOp(), RobotConfigs.teamA()), MatchPeriod.TELEOP.seconds()));
    }

    /** Runs with 1 s of INIT and {@code matchSeconds} of match, and returns the recording. */
    private static RunRecording record(OpModeRunner runner, double matchSeconds) {
        RunRecorder recorder = RunRecorder.attachTo(runner);
        runner.run(1.0, matchSeconds).assertNoException();
        return recorder.recording();
    }
}
