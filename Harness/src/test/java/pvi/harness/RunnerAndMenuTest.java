package pvi.harness;

import com.pedropathing.telemetry.SelectableOpMode;
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

    /** Test-only Pedro menu with three entries that need no hardware. */
    static class TestMenuOpMode extends SelectableOpMode {
        /** The OpMode the menu created, so the test can inspect it. */
        static RecordingOpMode chosen;

        /** Builds the menu: three entries, each creating a RecordingOpMode. */
        TestMenuOpMode() {
            super("Pick a test", menu -> {
                menu.add("First", () -> chosen = new RecordingOpMode("First", Double.MAX_VALUE));
                menu.add("Second", () -> chosen = new RecordingOpMode("Second", Double.MAX_VALUE));
                menu.add("Third", () -> chosen = new RecordingOpMode("Third", Double.MAX_VALUE));
            });
        }
    }

    /**
     * WHAT: A gamepad script can navigate a Pedro selection menu during INIT.
     *
     * <p>HOW: During init, taps d-pad down (cursor moves to "Second") then right bumper
     * (select). Checks that "Second" was the OpMode that actually ran, through its whole
     * lifecycle.</p>
     *
     * <p>WHY: Pedro menu OpModes (like Team A Pedro Paths) are driven by the SDK's own button
     * edge detection. This proves the harness delivers gamepad input the same way the
     * Driver Station does, so those menus can be tested. Uses a test-only menu because
     * selecting a real Pedro path needs the Pinpoint (Phase 5).</p>
     */
    @Test
    void scriptCanPickFromAPedroMenuBeforePlay() {
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
     * WHAT: Team A's real Pedro path menu shows its title and all four routes.
     *
     * <p>HOW: Runs INIT plus a few init loops of TeamAPedroPilotAutoOpMode and checks the
     * Driver Station screen text. Nothing is selected, so no Pedro hardware is needed.</p>
     *
     * <p>WHY: Catches a route being removed or renamed by mistake, which would leave the
     * drive team unable to pick it at a match.</p>
     */
    @Test
    void teamAPedroPathMenuListsEveryRoute() {
        // SET UP
        OpModeRunner runner = new OpModeRunner(new TeamAPedroPilotAutoOpMode(), RobotConfigs.teamA());

        // RUN: INIT and let the menu draw.
        runner.init().initLoop(3);

        // CHECK: the menu title and every route name are on screen.
        TelemetryFrame menu = runner.telemetry().lastFrame();
        assertTrue(menu.containsText("Select a Team A Path"), menu.toString());
        for (String route : new String[] {"LP-11 Pilot (24 in)", "DECODE Route", "Curving Test", "Illegal Path"}) {
            assertTrue(menu.containsText(route), "menu should list " + route + ":\n" + menu);
        }
    }
}
