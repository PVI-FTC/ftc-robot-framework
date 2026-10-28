package pvi.harness;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;

import org.firstinspires.ftc.teamcode.opmodes.autonomous.TeamAPedroPilotAutoOpMode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import pvi.harness.ds.Button;
import pvi.harness.ds.GamepadScript;
import pvi.harness.ds.TelemetryFrame;
import pvi.harness.hardware.FakeHardwareMap;
import pvi.harness.hardware.RobotConfigs;
import pvi.harness.runtime.MatchPeriod;
import pvi.harness.runtime.OpModeRunner;
import pvi.harness.runtime.RunResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 2: runner lifecycle rules, self-stopping OpModes, and Pedro's menu OpMode.
 *
 * <p>Most tests here check the <b>harness itself</b> using small test-only OpModes, so we
 * can trust it before trusting what it says about robot code.</p>
 */
class RunnerAndMenuTest {

    /**
     * Test-only OpMode: writes down which lifecycle methods were called (each once), and
     * calls requestOpModeStop() after a set number of seconds.
     */
    static class RecordingOpMode extends OpMode {
        /** Lifecycle calls seen, each listed once, in order. */
        final List<String> calls = new ArrayList<>();
        /** Name shown in telemetry, to tell several of these apart. */
        final String label;
        /** Seconds after INIT at which loop() calls requestOpModeStop(). */
        final double stopAfterSeconds;

        /** @param stopAfterSeconds use Double.MAX_VALUE to never stop */
        RecordingOpMode(String label, double stopAfterSeconds) {
            this.label = label;
            this.stopAfterSeconds = stopAfterSeconds;
        }

        // Each lifecycle method records its name (repeating ones only the first time).
        @Override public void init() { calls.add("init"); telemetry.addData("Running", label); }
        @Override public void init_loop() { if (!calls.contains("init_loop")) calls.add("init_loop"); }
        @Override public void start() { calls.add("start"); }

        /** Records the first loop, shows the label, and asks to stop once time is up. */
        @Override
        public void loop() {
            if (!calls.contains("loop")) calls.add("loop");
            telemetry.addData("Running", label);
            // "time" is the SDK's seconds-since-INIT field, kept up to date by the runner.
            if (time >= stopAfterSeconds) {
                requestOpModeStop();
            }
        }

        /** Records that stop() was called. */
        @Override public void stop() { calls.add("stop"); }
    }

    /**
     * WHAT: The runner calls OpMode methods in the same order as the Robot Controller app.
     *
     * <p>HOW: Runs a recording OpMode for 0.5 s of init and 1.0 s of match, then checks the
     * order of calls and how long each period lasted.</p>
     *
     * <p>WHY: Every other test depends on the runner behaving like the real robot. If it
     * called start() before init(), for example, tests would pass that should fail.</p>
     */
    @Test
    void lifecycleRunsInRobotControllerOrder() {
        // SET UP: an OpMode that never stops itself.
        RecordingOpMode opMode = new RecordingOpMode("only", Double.MAX_VALUE);

        // RUN
        RunResult result = new OpModeRunner(opMode, new FakeHardwareMap()).run(0.5, 1.0);

        // CHECK: INIT -> init_loop -> PLAY -> loop -> STOP, with the requested durations.
        result.assertNoException();
        assertEquals(List.of("init", "init_loop", "start", "loop", "stop"), opMode.calls);
        assertEquals(0.5, result.initSeconds(), 0.03);
        assertEquals(1.0, result.matchSeconds(), 0.03);
    }

    /**
     * WHAT: Calling runner steps in the wrong order gives a helpful error.
     *
     * <p>HOW: Calls loop() without init() and start() first, and checks the error message
     * explains the correct order.</p>
     *
     * <p>WHY: A mistake in a test should produce a message that says how to fix it, not a
     * confusing crash deep inside robot code.</p>
     */
    @Test
    void stepsOutOfOrderGiveAHelpfulError() {
        // SET UP
        OpModeRunner runner = new OpModeRunner(new RecordingOpMode("x", 1), new FakeHardwareMap());

        // RUN + CHECK: loop() before init()/start() must throw...
        IllegalStateException error = assertThrows(IllegalStateException.class, () -> runner.loop(1));

        // ...and say what the right order is.
        assertTrue(error.getMessage().contains("init, initLoop, start, loop, stop"), error.getMessage());
    }

    /**
     * WHAT: An OpMode can end itself early with requestOpModeStop().
     *
     * <p>HOW: Runs an autonomous-length (30 s) run with an OpMode that asks to stop after
     * 2 s. The run must end early, report that the OpMode asked to stop, and still call
     * stop().</p>
     *
     * <p>WHY: Real OpModes do this (e.g. "autonomous finished"). The harness has to stand in
     * for the Robot Controller's OpMode manager, which receives that request.</p>
     */
    @Test
    void opModeCanEndTheRunEarly() {
        // SET UP: asks to stop 2 s after INIT.
        RecordingOpMode opMode = new RecordingOpMode("early", 2.0);

        // RUN: allows up to 30 s.
        RunResult result = new OpModeRunner(opMode, new FakeHardwareMap()).run(MatchPeriod.AUTONOMOUS);

        // CHECK: stopped by request, well before 30 s, and stop() was still called.
        result.assertNoException();
        assertTrue(result.stopRequestedByOpMode());
        assertTrue(result.matchSeconds() < 30.0, "should stop well before 30 s: " + result);
        assertTrue(opMode.calls.contains("stop"));
    }

    /**
     * Test-only menu OpMode, like Team A's route chooser: during INIT, D-pad down moves the
     * cursor and the right bumper picks; at PLAY the picked entry runs. Needs no hardware.
     * (Pedro 2's SelectableOpMode menu is gone in Pedro 3, so this is a plain OpMode.)
     */
    static class TestMenuOpMode extends OpMode {
        /** The OpMode the menu created, so the test can inspect it. */
        static RecordingOpMode chosen;
        private static final String[] ENTRIES = {"First", "Second", "Third"};
        private int cursor;
        private boolean wasDown;
        private boolean wasBumper;

        @Override
        public void init() {
            telemetry.addData("Menu", ENTRIES[cursor]);
        }

        @Override
        public void init_loop() {
            // Edge detection on the plain fields, as Team A's chooser does.
            if (gamepad1.dpad_down && !wasDown) {
                cursor = (cursor + 1) % ENTRIES.length;
            }
            if (gamepad1.right_bumper && !wasBumper && chosen == null) {
                chosen = new RecordingOpMode(ENTRIES[cursor], Double.MAX_VALUE);
                chosen.hardwareMap = hardwareMap;
                chosen.telemetry = telemetry;
                chosen.init();
            }
            wasDown = gamepad1.dpad_down;
            wasBumper = gamepad1.right_bumper;
            if (chosen != null) {
                chosen.init_loop();
            }
            telemetry.addData("Menu", ENTRIES[cursor]);
        }

        @Override
        public void start() {
            if (chosen != null) chosen.start();
        }

        @Override
        public void loop() {
            if (chosen != null) chosen.loop();
        }

        @Override
        public void stop() {
            if (chosen != null) chosen.stop();
        }
    }

    /**
     * WHAT: A gamepad script can navigate a selection menu during INIT.
     *
     * <p>HOW: During init, taps d-pad down (cursor moves to "Second") then right bumper
     * (select). Checks that "Second" was the OpMode that actually ran, through its whole
     * lifecycle.</p>
     *
     * <p>WHY: Menu OpModes (like Team A Pedro Paths) read the gamepad during INIT. This proves
     * the harness delivers gamepad input before PLAY the same way the Driver Station does, so
     * those menus can be tested.</p>
     */
    @Test
    void scriptCanPickFromAMenuBeforePlay() {
        TestMenuOpMode.chosen = null;
        // SCRIPT: inputs before PLAY, times are seconds after INIT.
        GamepadScript driver = new GamepadScript()
                .duringInit()
                .at(0.2).tap(Button.DPAD_DOWN)      // cursor First -> Second
                .tap(Button.RIGHT_BUMPER);          // select Second

        // SET UP + RUN: 1 s of init (for the menu), 0.5 s of match.
        RunResult result = new OpModeRunner(new TestMenuOpMode(), new FakeHardwareMap())
                .gamepad1(driver)
                .run(1.0, 0.5);

        // CHECK: "Second" was chosen and ran from INIT to STOP.
        result.assertNoException();
        assertEquals("Second", TestMenuOpMode.chosen.label);
        assertEquals(List.of("init", "init_loop", "start", "loop", "stop"), TestMenuOpMode.chosen.calls);
        assertEquals("Second", result.telemetry().lastValue("Running"));
    }

    /**
     * WHAT: Team A's real route chooser offers all four routes: D-pad down during INIT steps
     * through them, and the Driver Station shows the selected one.
     *
     * <p>HOW: Runs INIT of TeamAPedroPilotAutoOpMode, taps D-pad down three times, and records
     * the "Selected Route" line after each tap. Nothing is started, so no route is driven.</p>
     *
     * <p>WHY: Catches a route being removed or renamed by mistake, which would leave the
     * drive team unable to pick it at a match. (Pedro 3: the chooser is Team A's own D-pad
     * menu; Pedro 2's menu OpMode is gone.)</p>
     */
    @Test
    void teamAPedroPathMenuListsEveryRoute() {
        // SCRIPT: three D-pad down taps during INIT, half a second apart.
        GamepadScript driver = new GamepadScript().duringInit()
                .at(0.5).tap(Button.DPAD_DOWN)
                .at(1.0).tap(Button.DPAD_DOWN)
                .at(1.5).tap(Button.DPAD_DOWN);
        OpModeRunner runner = new OpModeRunner(new TeamAPedroPilotAutoOpMode(), RobotConfigs.teamA()).gamepad1(driver);

        // RUN: INIT, and look at the screen just before each tap and at the end.
        List<String> shown = new ArrayList<>();
        runner.init();
        for (double t : new double[] {0.4, 0.9, 1.4, 1.9}) {
            while (runner.clock().sinceInit() < t) {
                runner.initLoop(1);
            }
            shown.add(runner.telemetry().lastValue("Selected Route"));
        }

        // CHECK: every route, in order.
        assertEquals(List.of("LP-11 Pilot (24 in)", "DECODE Route", "Curving Test", "Illegal Path"), shown);
        assertTrue(runner.telemetry().lastFrame().containsText("Up/Down to select"), runner.telemetry().lastFrame().toString());
    }
}
