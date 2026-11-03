package pvi.harness;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.opmodes.autonomous.TeamAAutoOpMode;
import org.firstinspires.ftc.teamcode.opmodes.teleop.TeamATeleOp;
import org.junit.jupiter.api.Test;

import pvi.harness.ds.TelemetryFrame;
import pvi.harness.hardware.FakeHardwareMap;
import pvi.harness.hardware.RobotConfigs;
import pvi.harness.runtime.MatchPeriod;
import pvi.harness.runtime.OpModeRunner;
import pvi.harness.runtime.RunResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 3: robot code reads the harness's simulated clock.
 *
 * <p>These check the Java agent and simulated time themselves. If any of these fail, the
 * timed tests in TeamAAutoOpModeTest can't be trusted.</p>
 */
class SimulatedTimeTest {

    /** Test-only OpMode: reports what the SDK's own timers say each loop. */
    static class TimerOpMode extends OpMode {
        /** An SDK timer, started at INIT. */
        ElapsedTime sinceInit;

        /** Starts the timer. */
        @Override
        public void init() {
            sinceInit = new ElapsedTime();
        }

        /** Shows both SDK timers to 6 decimals, so the test can compare them exactly. */
        @Override
        public void loop() {
            telemetry.addData("ElapsedTime", "%.6f", sinceInit.seconds());
            telemetry.addData("Runtime", "%.6f", getRuntime());
        }
    }

    /**
     * WHAT: The harness Java agent was loaded when the tests started.
     *
     * <p>HOW: Asks the runner whether robot code's clocks follow simulated time.</p>
     *
     * <p>WHY: Without the agent, timed code reads the PC's real clock. This test fails
     * first with a clear message ("run the tests through Gradle"), so the cause is obvious
     * instead of a confusing timing failure elsewhere.</p>
     */
    @Test
    void harnessAgentIsLoaded() {
        // CHECK
        assertTrue(OpModeRunner.isTimeSimulated(),
                "harness-agent.jar was not loaded; run the tests through Gradle");
    }

    /**
     * WHAT: The SDK's ElapsedTime and getRuntime() report exactly the simulated time.
     *
     * <p>HOW: Runs 0.5 s of init and 2.0 s of match. The last loop happens at 1.98 s after
     * PLAY, which is 2.48 s after INIT, so both timers must read exactly 2.480000.</p>
     *
     * <p>WHY: Proves robot code really runs on harness time, to the nanosecond, not
     * approximately. "Exactly" is what makes timed tests repeatable.</p>
     */
    @Test
    void sdkTimersFollowSimulatedTimeExactly() {
        // SET UP + RUN
        RunResult result = new OpModeRunner(new TimerOpMode(), new FakeHardwareMap()).run(0.5, 2.0);

        // CHECK: both SDK timers read the exact simulated time.
        result.assertNoException();
        // Last loop runs at 1.98 s after PLAY = 2.48 s after INIT (20 ms loops).
        assertEquals("2.480000", result.telemetry().lastValue("ElapsedTime"));
        assertEquals("2.480000", result.telemetry().lastValue("Runtime"));
    }

    /**
     * WHAT: A full 2-minute TeleOp runs in much less than 2 minutes of real time.
     *
     * <p>HOW: Measures real time with the PC's clock (test code is not redirected) around a
     * full match, and requires under 10 s. In practice it takes about 1 s.</p>
     *
     * <p>WHY: If tests ran in real time, a full test run would take many minutes and
     * nobody would run them. Speed is what makes testing every change practical.</p>
     */
    @Test
    void aFullTeleOpMatchTakesFarLessThanTwoMinutes() {
        // Real clock, start.
        long wallStart = System.nanoTime();

        // RUN: a full simulated match.
        RunResult result = new OpModeRunner(new TeamATeleOp(), RobotConfigs.teamA()).run(MatchPeriod.TELEOP);

        // CHECK: 120 s simulated, under 10 s real.
        double wallSeconds = (System.nanoTime() - wallStart) / 1e9;
        result.assertNoException();
        assertEquals(120.0, result.matchSeconds(), 0.03);
        assertTrue(wallSeconds < 10.0, "120 s match took " + wallSeconds + " s of real time");
    }

    /**
     * WHAT: Running the same test twice gives identical results.
     *
     * <p>HOW: Runs the Team A autonomous twice and compares every telemetry frame, including
     * its timestamp.</p>
     *
     * <p>WHY: A test that sometimes passes and sometimes fails ("flaky") is worse than no
     * test. Simulated time removes the PC's speed from the picture.</p>
     */
    @Test
    void sameTestGivesExactlyTheSameResultEveryRun() {
        // RUN twice
        List<TelemetryFrame> first = new OpModeRunner(new TeamAAutoOpMode(), RobotConfigs.teamA())
                .run(MatchPeriod.AUTONOMOUS).telemetry().frames();
        List<TelemetryFrame> second = new OpModeRunner(new TeamAAutoOpMode(), RobotConfigs.teamA())
                .run(MatchPeriod.AUTONOMOUS).telemetry().frames();

        // CHECK: same number of frames, and every frame identical.
        assertEquals(first.size(), second.size());
        assertEquals(first, second);
    }
}
