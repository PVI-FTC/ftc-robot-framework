package pvi.harness;

import org.firstinspires.ftc.teamcode.opmodes.autonomous.TeamAAutoOpMode;
import org.firstinspires.ftc.teamcode.opmodes.teleop.RumbleDemoTeleOp;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import pvi.harness.ds.RumbleRecord;
import pvi.harness.hardware.RobotConfigs;
import pvi.harness.runtime.MatchPeriod;
import pvi.harness.runtime.OpModeRunner;
import pvi.harness.runtime.RunResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 3: timed autonomous and match-timer behavior, now that robot code sees simulated time.
 *
 * <p>TeamAAutoOpMode runs three steps: drive forward at 0.20 power for 0.75 s, run the intake
 * for 0.50 s, then wait 0.25 s. Most tests below record what the robot did on every loop
 * (see {@link #runAutonomous()}) and then check that recording.</p>
 */
class TeamAAutoOpModeTest {
    /** One loop is 20 ms; allow one loop of slack on step boundaries. */
    private static final double ONE_LOOP = 0.021;

    /** What the robot was doing on one loop: time, current step, five motor powers, done flag. */
    record Sample(double t, String step, double frontLeft, double frontRight,
                  double rearLeft, double rearRight, double intake, String complete) {
    }

    /**
     * Runs the whole 30 s autonomous and returns one {@link Sample} per loop after PLAY.
     * {@code everyLoop} is called after every OpMode call, so nothing is missed.
     */
    private static List<Sample> runAutonomous() {
        List<Sample> samples = new ArrayList<>();
        OpModeRunner runner = new OpModeRunner(new TeamAAutoOpMode(), RobotConfigs.teamA());
        runner.everyLoop(r -> {
            if (!r.clock().hasStarted()) {
                return; // skip INIT; only record from PLAY on
            }
            samples.add(new Sample(r.clock().sinceStart(),
                    r.telemetry().lastValue("Auto Step"),
                    r.hardwareMap().motor("frontLeft").getPower(),
                    r.hardwareMap().motor("frontRight").getPower(),
                    r.hardwareMap().motor("rearLeft").getPower(),
                    r.hardwareMap().motor("rearRight").getPower(),
                    r.hardwareMap().motor("intake").getPower(),
                    r.telemetry().lastValue("Sequence Complete")));
        });
        runner.run(MatchPeriod.AUTONOMOUS).assertNoException();
        return samples;
    }

    /** First match time at which {@code step} was the current step. */
    private static double firstTime(List<Sample> samples, String step) {
        return samples.stream().filter(s -> step.equals(s.step())).findFirst()
                .orElseThrow(() -> new AssertionError("step never ran: " + step)).t();
    }

    /**
     * WHAT: The three steps run in order, each for its configured time.
     *
     * <p>HOW: Finds when each step first appears in the recording, and when the sequence
     * first reports complete. The gaps between those times are the step lengths, which
     * must match the OpMode's constants within one 20 ms loop.</p>
     *
     * <p>WHY: If someone changes a duration, skips a step, or a step never finishes, the
     * autonomous would do something different on the field. This catches it.</p>
     */
    @Test
    void stepsRunInOrderForTheirConfiguredDurations() {
        // RUN
        List<Sample> samples = runAutonomous();

        // When did each step start, and when did the sequence finish?
        double driveStart = firstTime(samples, "TimedDrive");
        double intakeStart = firstTime(samples, "TimedIntake");
        double waitStart = firstTime(samples, "Wait");
        double complete = samples.stream().filter(s -> "true".equals(s.complete())).findFirst()
                .orElseThrow(() -> new AssertionError("sequence never completed")).t();

        // CHECK: each step's length, and the total.
        assertEquals(0.75, intakeStart - driveStart, ONE_LOOP, "TimedDrive should last 0.75 s");
        assertEquals(0.50, waitStart - intakeStart, ONE_LOOP, "TimedIntake should last 0.50 s");
        assertEquals(0.25, complete - waitStart, ONE_LOOP, "Wait should last 0.25 s");
        assertEquals(1.50, complete, 2 * ONE_LOOP, "whole sequence should finish at about 1.5 s");
    }

    /**
     * WHAT: During the drive step all four wheels get exactly 0.20 power; afterward, 0.
     *
     * <p>HOW: Goes through every recorded loop. While the step is TimedDrive (after the first
     * loop, while the drive state machine switches on), all four powers must be 0.20. After
     * the drive step ends, the drive must be at 0.</p>
     *
     * <p>WHY: Equal power on all four wheels means straight forward with no drift or
     * turning. Checking every loop, not just one moment, catches a power that flickers or
     * drifts.</p>
     */
    @Test
    void drivesStraightForwardAtTwentyPercentThenStops() {
        // RUN
        List<Sample> samples = runAutonomous();

        // CHECK every loop.
        for (Sample s : samples) {
            if ("TimedDrive".equals(s.step()) && s.t() > ONE_LOOP) {
                assertEquals(0.20, s.frontLeft(), 1e-9, "front left at " + s.t());
                assertEquals(0.20, s.frontRight(), 1e-9, "front right at " + s.t());
                assertEquals(0.20, s.rearLeft(), 1e-9, "rear left at " + s.t());
                assertEquals(0.20, s.rearRight(), 1e-9, "rear right at " + s.t());
            }
            if (s.t() > 0.75 + 2 * ONE_LOOP) {
                assertEquals(0.0, s.frontLeft(), 1e-9, "drive should be stopped at " + s.t());
                assertEquals(0.0, s.rearRight(), 1e-9, "drive should be stopped at " + s.t());
            }
        }
    }

    /**
     * WHAT: The intake runs during the intake step and at no other time.
     *
     * <p>HOW: Goes through every recorded loop. During TimedIntake the intake power must be
     * above 0; once the drive step is over, outside TimedIntake it must be exactly 0.</p>
     *
     * <p>WHY: An intake that keeps running after its step wastes battery, can jam game
     * pieces, and may break game rules.</p>
     */
    @Test
    void intakeRunsOnlyDuringTheIntakeStep() {
        // RUN
        List<Sample> samples = runAutonomous();

        // CHECK every loop.
        for (Sample s : samples) {
            boolean intakeStep = "TimedIntake".equals(s.step());
            if (intakeStep && s.intake() <= 0) {
                throw new AssertionError("intake should run during TimedIntake, t=" + s.t());
            }
            if (!intakeStep && s.t() > 0.75 + 2 * ONE_LOOP && s.intake() != 0.0) {
                throw new AssertionError("intake should be off outside TimedIntake, t=" + s.t() + " " + s);
            }
        }
    }

    /**
     * WHAT: At the end of the 30 s autonomous, the sequence is complete and everything is stopped.
     *
     * <p>HOW: Looks at the very last recorded loop: it must be at about 30 s, report
     * "Sequence Complete: true", and have the drive and intake at 0.</p>
     *
     * <p>WHY: The robot must sit still for the rest of autonomous once its routine is done,
     * not creep or restart a step.</p>
     */
    @Test
    void finishesWellInsideTheThirtySecondPeriodAndStaysStopped() {
        // RUN
        List<Sample> samples = runAutonomous();

        // CHECK the final loop.
        Sample last = samples.get(samples.size() - 1);
        assertEquals(30.0, last.t(), 0.03);
        assertEquals("true", last.complete());
        assertEquals(0.0, last.frontLeft(), 1e-9);
        assertEquals(0.0, last.intake(), 1e-9);
    }

    /**
     * WHAT: The match-timer rumbles fire at 90 s, 105 s and 115 s with the right patterns.
     *
     * <p>HOW: Runs RumbleDemoTeleOp for a full 2-minute match and checks the four rumbles on
     * the driver's gamepad: 1000 ms at PLAY, 2 blips at 90 s (endgame), 3 blips at 105 s,
     * and one 500 ms pulse at 115 s.</p>
     *
     * <p>WHY: RumbleManager uses ElapsedTime, so before Phase 3 this needed a real
     * 2-minute wait. Now it runs in about a second, and the timing is checked to the loop.</p>
     */
    @Test
    void matchTimerRumblesFireAtNinetyOneHundredFiveAndOneHundredFifteenSeconds() {
        // SET UP + RUN: full TeleOp match.
        RunResult result = new OpModeRunner(new RumbleDemoTeleOp(), RobotConfigs.teamA()).run(MatchPeriod.TELEOP);

        // CHECK: exactly four rumbles on gamepad 1.
        result.assertNoException();
        List<RumbleRecord> rumbles = result.gamepad1Rumbles();
        assertEquals(4, rumbles.size(), "expected PLAY + 3 timer alerts: " + rumbles);

        // At PLAY: one long buzz.
        assertEquals(0.0, rumbles.get(0).matchTimeSeconds(), 1e-9);
        assertEquals(1000, rumbles.get(0).totalDurationMs());

        // 90 s: endgame warning.
        assertEquals(90.0, rumbles.get(1).matchTimeSeconds(), ONE_LOOP);
        assertEquals(2, rumbles.get(1).pulses(), "90 s: 2 blips");

        // 105 s: final countdown.
        assertEquals(105.0, rumbles.get(2).matchTimeSeconds(), ONE_LOOP);
        assertEquals(3, rumbles.get(2).pulses(), "105 s: 3 blips");

        // 115 s: last 5 seconds.
        assertEquals(115.0, rumbles.get(3).matchTimeSeconds(), ONE_LOOP);
        assertEquals(500, rumbles.get(3).totalDurationMs(), "115 s: one 500 ms pulse");
        // The operator's gamepad never buzzes.
        assertTrue(result.gamepad2Rumbles().isEmpty());
    }
}
