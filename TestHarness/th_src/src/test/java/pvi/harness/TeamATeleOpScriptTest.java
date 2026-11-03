package pvi.harness;

import org.firstinspires.ftc.teamcode.opmodes.teleop.RumbleDemoTeleOp;
import org.firstinspires.ftc.teamcode.opmodes.teleop.TeamATeleOp;
import org.firstinspires.ftc.teamcode.opmodes.teleop.TeamBTeleOp;
import org.firstinspires.ftc.teamcode.opmodes.teleop.TeamCTeleOp;
import org.junit.jupiter.api.Test;

import pvi.harness.ds.Axis;
import pvi.harness.ds.Button;
import pvi.harness.ds.GamepadScript;
import pvi.harness.ds.RumbleRecord;
import pvi.harness.ds.TelemetryFrame;
import pvi.harness.hardware.FakeHardwareMap;
import pvi.harness.hardware.RobotConfigs;
import pvi.harness.runtime.MatchPeriod;
import pvi.harness.runtime.OpModeRunner;
import pvi.harness.runtime.RunResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 2: scripted drivers and whole-match runs against the real TeleOps.
 *
 * <p>These tests describe driver input as a timeline with {@link GamepadScript}. Times in
 * {@code .at(...)} are seconds after PLAY.</p>
 */
class  TeamATeleOpScriptTest {

    /**
     * WHAT: The robot drives while the stick is pushed and stops when it is released.
     *
     * <p>HOW: The driver holds the left stick forward from 0 s to 2 s. At 1.0 s every wheel
     * should be driving; at 3.0 s (stick released) every wheel should be at exactly 0.</p>
     *
     * <p>WHY: "Stops when you let go" is a safety behavior. A bug that kept the last stick
     * value would make the robot run away.</p>
     */
    @Test
    void scriptedDriverDrivesThenStops() {
        // SCRIPT: push forward at PLAY, let go at 2 s.
        GamepadScript driver = new GamepadScript()
                .at(0.0).stick(Axis.LEFT_STICK_Y, -1.0)
                .at(2.0).stick(Axis.LEFT_STICK_Y, 0.0);
        // SET UP: Team A TeleOp, Team A hardware, script on gamepad 1 (driver).
        OpModeRunner runner = new OpModeRunner(new TeamATeleOp(), RobotConfigs.teamA())
                .gamepad1(driver);

        // RUN to 1.0 s, then CHECK: all wheels driving.
        runner.init().start();
        runner.loopUntil(1.0);
        for (String name : RobotConfigs.driveMotorNames()) {
            assertTrue(runner.hardwareMap().motor(name).getPower() > 0.5, name + " should drive at 1.0 s");
        }

        // RUN on to 3.0 s, then CHECK: all wheels stopped.
        runner.loopUntil(3.0);
        for (String name : RobotConfigs.driveMotorNames()) {
            assertEquals(0.0, runner.hardwareMap().motor(name).getPower(), 1e-9, name + " should stop at 3.0 s");
        }
    }

    /**
     * WHAT: Operator taps move the intake through every state: Intaking, Holding, Ejecting, Idle.
     *
     * <p>HOW: Taps A, Y, X, B one second apart on gamepad 2, and checks halfway between
     * taps: the state on telemetry and, where it matters, the motor direction (forward
     * while intaking, backward while ejecting, zero when idle).</p>
     *
     * <p>WHY: Checks both what the drivers <i>see</i> (telemetry) and what the robot
     * <i>does</i> (motor power), so a bug in either is caught.</p>
     */
    @Test
    void operatorTapsWalkTheIntakeThroughItsStates() {
        // SCRIPT: one tap per second on the operator's gamepad.
        GamepadScript operator = new GamepadScript()
                .at(0.5).tap(Button.A)       // start intake
                .at(1.5).tap(Button.Y)       // hold
                .at(2.5).tap(Button.X)       // eject
                .at(3.5).tap(Button.B);      // stop
        // SET UP: script on gamepad 2, because TeamATeleOp reads intake buttons there.
        OpModeRunner runner = new OpModeRunner(new TeamATeleOp(), RobotConfigs.teamA())
                .gamepad2(operator);
        runner.init().start();

        // After A: intaking, motor forward.
        runner.loopUntil(1.0);
        assertEquals("Intaking", runner.telemetry().lastValue("Intake State"));
        assertTrue(runner.hardwareMap().motor("intake").getPower() > 0);

        // After Y: holding.
        runner.loopUntil(2.0);
        assertEquals("Holding", runner.telemetry().lastValue("Intake State"));

        // After X: ejecting, motor backward.
        runner.loopUntil(3.0);
        assertEquals("Ejecting", runner.telemetry().lastValue("Intake State"));
        assertTrue(runner.hardwareMap().motor("intake").getPower() < 0);

        // After B: idle, motor off.
        runner.loopUntil(4.0);
        assertEquals("IdleIntake", runner.telemetry().lastValue("Intake State"));
        assertEquals(0.0, runner.hardwareMap().motor("intake").getPower(), 1e-9);
    }

    /**
     * WHAT: Holding a button down counts as one press, not a press every loop.
     *
     * <p>HOW: The operator holds A from 0.5 s to 3.5 s and taps B at 1.0 s while A is still
     * held. If A re-triggered every loop, the intake would restart right after B stopped
     * it. At 3.0 s it must still be idle.</p>
     *
     * <p>WHY: Drivers often rest a thumb on a button. InputManager's "just pressed" edge
     * detection must make a held button act once.</p>
     */
    @Test
    void holdingAButtonOnlyCountsAsOnePress() {
        // SCRIPT: InputManager should start the intake once, even if A is held for 3 seconds.
        GamepadScript operator = new GamepadScript()
                .at(0.5).hold(Button.A, 3.0)
                .at(1.0).tap(Button.B);      // B stops the intake while A is still held
        // SET UP + RUN
        OpModeRunner runner = new OpModeRunner(new TeamATeleOp(), RobotConfigs.teamA())
                .gamepad2(operator);
        runner.init().start();

        runner.loopUntil(3.0);

        // CHECK: B's stop "stuck"; the held A did not restart the intake.
        assertEquals("IdleIntake", runner.telemetry().lastValue("Intake State"),
                "a held A must not restart the intake every loop");
    }

    /**
     * WHAT: Team A's TeleOp survives a whole 2-minute match and switches drive modes on command.
     *
     * <p>HOW: Runs a full match with {@code run(MatchPeriod.TELEOP)}: some strafing, Y at
     * 10 s (heading hold), X at 20 s (back to manual). Then looks back through the saved
     * telemetry to see the drive state at 15 s and 25 s, and checks the motors after STOP.</p>
     *
     * <p>WHY: Some bugs only appear after many loops or after a mode change. 6,000 loops
     * at 20 ms each is a full match, and it runs in about a second.</p>
     */
    @Test
    void fullTeleOpMatchRunsCleanly() {
        // SCRIPT: strafe right for 1 s, then switch drive modes.
        GamepadScript driver = new GamepadScript()
                .at(5.0).stick(Axis.LEFT_STICK_X, 0.6)
                .at(6.0).stick(Axis.LEFT_STICK_X, 0.0)
                .at(10.0).tap(Button.Y)      // heading hold
                .at(20.0).tap(Button.X);     // back to manual
        // SET UP
        OpModeRunner runner = new OpModeRunner(new TeamATeleOp(), RobotConfigs.teamA()).gamepad1(driver);

        // RUN: INIT, 1 s of init, PLAY, 120 s, STOP. A crash is stored in the result.
        RunResult result = runner.run(MatchPeriod.TELEOP);

        // CHECK: no crash, full length, expected number of 20 ms loops.
        result.assertNoException();
        assertEquals(120.0, result.matchSeconds(), 0.03);
        assertEquals(6000, result.loopCount(), 2);
        // CHECK: look back in time at what the Driver Station showed.
        TelemetryFrame duringHeadingHold = frameAt(result, 15.0);
        assertEquals("HeadingHold", duringHeadingHold.value("Drive State"));
        assertEquals("ManualDrive", frameAt(result, 25.0).value("Drive State"));
        // CHECK: everything stopped after STOP.
        for (String name : RobotConfigs.driveMotorNames()) {
            assertEquals(0.0, runner.hardwareMap().motor(name).getPower(), 1e-9, name + " after STOP");
        }
    }

    /**
     * WHAT: When an OpMode crashes during a full run, the crash is recorded, not thrown.
     *
     * <p>HOW: Removes "frontLeft" so INIT fails, runs a whole match with {@code run(...)}, and
     * inspects the {@link RunResult}: it crashed, during INIT, with the expected error type
     * and a message naming the motor.</p>
     *
     * <p>WHY: Lets a test check <i>how</i> something fails (the right error, at the right
     * time), not only that it fails. Compare with {@code missingDriveMotorFailsWithClearMessage}
     * in HarnessSmokeTest, which uses step-by-step calls where the crash is thrown.</p>
     */
    @Test
    void crashDuringInitIsCapturedNotThrown() {
        // SET UP: Team A hardware with one drive motor removed.
        FakeHardwareMap hardwareMap = RobotConfigs.teamA().without("frontLeft");

        // RUN: the whole match; the crash ends it early.
        RunResult result = new OpModeRunner(new TeamATeleOp(), hardwareMap).run(MatchPeriod.TELEOP);

        // CHECK: what failed, when, and why.
        assertTrue(result.crashed());
        assertEquals(RunResult.Stage.INIT, result.exceptionStage());
        assertInstanceOf(IllegalStateException.class, result.exception());
        assertTrue(result.exception().getMessage().contains("frontLeft"));
    }

    /**
     * WHAT: The rumble demo buzzes the driver's gamepad for 1 second when PLAY is pressed.
     *
     * <p>HOW: Runs 1 s of init and 2 s of match, then reads the rumble commands the harness
     * recorded for each gamepad.</p>
     *
     * <p>WHY: Rumble is an output to the drivers that can't be seen in telemetry. The
     * harness records every rumble command with its time, length and number of pulses.
     * Only gamepad 1 (driver) should buzz.</p>
     */
    @Test
    void rumbleDemoBuzzesDriverAtPlay() {
        // SET UP + RUN: 1 s of init, 2 s of match.
        RunResult result = new OpModeRunner(new RumbleDemoTeleOp(), RobotConfigs.teamA()).run(1.0, 2.0);

        // CHECK: one 1000 ms rumble on gamepad 1 at match time 0, none on gamepad 2.
        result.assertNoException();
        assertFalse(result.gamepad1Rumbles().isEmpty(), "expected a rumble at PLAY");
        RumbleRecord first = result.gamepad1Rumbles().get(0);
        assertEquals(0.0, first.matchTimeSeconds(), 1e-9);
        assertEquals(1000, first.totalDurationMs());
        assertTrue(result.gamepad2Rumbles().isEmpty());
    }

    /**
     * WHAT: Team B's and Team C's TeleOps get through a full match without crashing.
     *
     * <p>HOW: The same short driving script (forward, then turn, then let go) is used for
     * each team's TeleOp on its own robot configuration.</p>
     *
     * <p>WHY: A basic "does it survive?" check that every team gets for free. As Teams B
     * and C add mechanisms, they can copy the Team A tests above for their own controls.</p>
     */
    @Test
    void teamBAndTeamCTeleOpsSurviveAFullMatch() {
        // SCRIPT: shared by both teams.
        GamepadScript driver = new GamepadScript()
                .at(1.0).stick(Axis.LEFT_STICK_Y, -0.5)
                .at(3.0).stick(Axis.RIGHT_STICK_X, 0.5)
                .at(5.0).releaseAll();

        // SET UP + RUN + CHECK for each team: assertNoException fails the test on any crash.
        new OpModeRunner(new TeamBTeleOp(), RobotConfigs.teamB()).gamepad1(driver)
                .run(MatchPeriod.TELEOP).assertNoException();
        new OpModeRunner(new TeamCTeleOp(), RobotConfigs.teamC()).gamepad1(driver)
                .run(MatchPeriod.TELEOP).assertNoException();
    }

    /** The last frame sent at or before a match time. */
    private static TelemetryFrame frameAt(RunResult result, double matchSeconds) {
        TelemetryFrame found = null;
        for (TelemetryFrame frame : result.telemetry().frames()) {
            if (frame.matchTimeSeconds() <= matchSeconds) {
                found = frame;
            }
        }
        if (found == null) {
            throw new AssertionError("No telemetry frame at or before " + matchSeconds + " s");
        }
        return found;
    }
}
