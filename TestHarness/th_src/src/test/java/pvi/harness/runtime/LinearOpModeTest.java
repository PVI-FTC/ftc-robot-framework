package pvi.harness.runtime;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotor;

import org.firstinspires.ftc.teamcode.harnesssample.ThreadSleepLinearSample;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import pvi.harness.hardware.FakeHardwareMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that the runner runs {@link LinearOpMode}s (RP6): {@code runOpMode()} runs on its own
 * thread in step with the loops, and {@code waitForStart()}, {@code sleep()},
 * {@code opModeIsActive()} and {@code isStopRequested()} work as on the robot.
 *
 * <p>Each sample OpMode below is a tiny, made-up LinearOpMode that does one thing, written the
 * way FTC's sample OpModes are.</p>
 */
class LinearOpModeTest {

    // ---- Sample LinearOpModes ----------------------------------------------------------------

    /** Drives at full power for {@code sleep(1000)}, then stops the motor and waits for STOP. */
    public static class SleepDrive extends LinearOpMode {
        @Override
        public void runOpMode() {
            DcMotor motor = hardwareMap.get(DcMotor.class, "motor");
            waitForStart();
            motor.setPower(1.0);
            sleep(1000);
            motor.setPower(0.0);
            while (opModeIsActive()) {
                idle();
            }
        }
    }

    /** Shows one line in INIT, then another after PLAY. */
    public static class WaitsForStart extends LinearOpMode {
        @Override
        public void runOpMode() {
            telemetry.addLine("Ready");
            telemetry.update();
            waitForStart();
            telemetry.addLine("Running");
            telemetry.update();
            while (opModeIsActive()) {
                idle();
            }
        }
    }

    /** Counts its passes through {@code while (opModeIsActive())} and notes when it ends. */
    public static class LoopsUntilStop extends LinearOpMode {
        int passes;
        boolean endedCleanly;

        @Override
        public void runOpMode() {
            waitForStart();
            while (opModeIsActive()) {
                passes++;
            }
            endedCleanly = true;
        }
    }

    /** Skips its drive if STOP is pressed during INIT, as FTC's samples show. */
    public static class ChecksStopAfterInit extends LinearOpMode {
        boolean drove;

        @Override
        public void runOpMode() {
            waitForStart();
            if (isStopRequested()) {
                return;
            }
            drove = true;
        }
    }

    /** Uses {@code while (opModeInInit())} to show a counter until PLAY. */
    public static class InitLoopCounter extends LinearOpMode {
        int initPasses;

        @Override
        public void runOpMode() {
            while (opModeInInit()) {
                initPasses++;
            }
        }
    }

    /** Crashes right after PLAY. */
    public static class CrashesAfterStart extends LinearOpMode {
        @Override
        public void runOpMode() {
            waitForStart();
            throw new IllegalStateException("arm jammed");
        }
    }

    /** Waits for a button in a loop that never calls the SDK, which the watchdog must catch. */
    public static class BusyLoop extends LinearOpMode {
        /** Set by the test afterwards, so the stuck thread can end instead of spinning forever. */
        static volatile boolean release;

        @Override
        public void runOpMode() {
            waitForStart();
            while (!release && !gamepad1.a) {
                // no idle(), no opModeIsActive(): the harness can't move time on
            }
        }
    }

    // ---- Tests ------------------------------------------------------------------------------

    /**
     * WHAT: {@code sleep(1000)} lasts exactly 1000 ms of simulated time: the motor is on for
     * exactly 50 loops of 20 ms.
     *
     * <p>HOW: Records the motor's power after every runner step and counts the steps at full
     * power.</p>
     *
     * <p>WHY: Timed autonomous moves ("drive 1 second") are the most common LinearOpMode code.
     * Before RP6 a LinearOpMode silently did nothing, and a real-time sleep would make results
     * depend on the computer's speed.</p>
     */
    @Test
    void sleepOneSecondDrivesForExactlyFiftyLoops() {
        List<Double> powers = powersEveryStep(new SleepDrive());

        assertEquals(50, powers.stream().filter(p -> p == 1.0).count(), "powers: " + powers);
        assertEquals(0.0, powers.get(powers.size() - 1));
    }

    /** WHAT: {@code Thread.sleep} in robot code also follows simulated time (10 loops for 200 ms). */
    @Test
    void threadSleepInRobotCodeFollowsSimulatedTime() {
        List<Double> powers = powersEveryStep(new ThreadSleepLinearSample());

        assertEquals(10, powers.stream().filter(p -> p == 1.0).count(), "powers: " + powers);
    }

    /** WHAT: Every run of the same LinearOpMode gives exactly the same result. */
    @Test
    void theSameOpModeGivesTheSameResultEveryRun() {
        assertEquals(powersEveryStep(new SleepDrive()), powersEveryStep(new SleepDrive()));
    }

    /**
     * WHAT: Code after {@code waitForStart()} runs only after PLAY, however long INIT lasts.
     */
    @Test
    void waitForStartHoldsTheOpModeUntilPlay() {
        OpModeRunner runner = new OpModeRunner(new WaitsForStart(), new FakeHardwareMap());

        runner.init().initLoop(25);
        assertEquals(List.of("Ready"), runner.telemetry().lastFrame().lines());

        runner.start();
        assertEquals(List.of("Running"), runner.telemetry().lastFrame().lines());
        runner.stop();
    }

    /**
     * WHAT: {@code while (opModeIsActive())} makes one pass per loop, and ends when STOP is
     * pressed, so the code after it runs.
     */
    @Test
    void opModeIsActiveLoopsOncePerLoopUntilStop() {
        LoopsUntilStop opMode = new LoopsUntilStop();
        OpModeRunner runner = new OpModeRunner(opMode, new FakeHardwareMap());

        runner.init().start().loop(10);
        assertEquals(10, opMode.passes);
        assertFalse(opMode.endedCleanly);

        runner.stop();
        assertTrue(opMode.endedCleanly);
    }

    /** WHAT: {@code while (opModeInInit())} makes one pass per init loop. */
    @Test
    void opModeInInitLoopsOncePerInitLoop() {
        InitLoopCounter opMode = new InitLoopCounter();
        OpModeRunner runner = new OpModeRunner(opMode, new FakeHardwareMap());

        runner.init();
        int afterInit = opMode.initPasses;
        runner.initLoop(10);

        assertEquals(10, opMode.initPasses - afterInit);
        runner.start().stop();
    }

    /**
     * WHAT: STOP during INIT ends the OpMode: {@code waitForStart()} returns,
     * {@code isStopRequested()} is true, and nothing after it runs.
     */
    @Test
    void stopDuringInitEndsTheOpModeWithoutRunningIt() {
        ChecksStopAfterInit opMode = new ChecksStopAfterInit();
        OpModeRunner runner = new OpModeRunner(opMode, new FakeHardwareMap());

        runner.init().initLoop(5).stop();

        assertFalse(opMode.drove);
        assertTrue(opMode.isStopRequested());
    }

    /**
     * WHAT: A crash in {@code runOpMode()} goes into the RunResult, with the stage it happened
     * in, like a crash in an iterative OpMode.
     */
    @Test
    void aCrashInRunOpModeGoesIntoTheRunResult() {
        RunResult result = new OpModeRunner(new CrashesAfterStart(), new FakeHardwareMap()).run(0.5, 2.0);

        assertTrue(result.crashed());
        assertEquals("arm jammed", result.exception().getMessage());
        assertEquals(RunResult.Stage.START, result.exceptionStage());
    }

    /**
     * WHAT: When {@code runOpMode()} returns by itself, the run ends early, as on the robot
     * (the SDK calls {@code requestOpModeStop()}).
     */
    @Test
    void anOpModeThatFinishesEndsTheRunEarly() {
        RunResult result = new OpModeRunner(new ChecksStopAfterInit(), new FakeHardwareMap()).run(0.5, 30.0);

        assertFalse(result.crashed());
        assertTrue(result.stopRequestedByOpMode());
        assertTrue(result.matchSeconds() < 1.0, "match seconds: " + result.matchSeconds());
    }

    /**
     * WHAT: A loop that never calls the SDK is caught by the watchdog, with a message saying
     * what to look for, instead of hanging the tests.
     *
     * <p>This test takes about 3 seconds of real time (the watchdog's wait).</p>
     */
    @Test
    void theWatchdogCatchesALoopThatNeverCallsTheSdk() {
        BusyLoop.release = false;
        OpModeRunner runner = new OpModeRunner(new BusyLoop(), new FakeHardwareMap());
        try {
            runner.init().initLoop(2);
            IllegalStateException error = assertThrows(IllegalStateException.class, runner::start);

            assertTrue(error.getMessage().contains("didn't call idle(), opModeIsActive()"), error.getMessage());
            assertTrue(error.getMessage().contains("while (!gamepad1.a)"), error.getMessage());
        } finally {
            BusyLoop.release = true;   // let the stuck thread end
        }
    }

    /**
     * WHAT: A LinearOpMode found by {@link OpModeCatalog} runs a whole TeleOp period.
     */
    @Test
    void aLinearOpModeFoundInTheCatalogRuns() {
        OpModeCatalog.Entry entry = OpModeCatalog.find(List.of("pvi.harness.runtime.catalogsample")).stream()
                .filter(e -> e.name().equals("Sample Linear"))
                .findFirst()
                .orElseThrow();

        RunResult result = new OpModeRunner(entry.newOpMode(), new FakeHardwareMap()).run(MatchPeriod.TELEOP);

        result.assertNoException();
        // One pass per loop, plus one at STOP: opModeIsActive() had already said "yes" (and
        // called idle()) just before STOP was pressed, exactly as on the robot.
        assertTrue(result.loopCount() > 5000, "loops: " + result.loopCount());
        assertEquals(String.valueOf(result.loopCount() + 1), result.telemetry().lastValue("Sample Linear loops"));
    }

    // ------------------------------------------------------------------------------------------

    /**
     * INIT, 1 s of init loops, PLAY, 3 s of loops, STOP, on a robot with one motor called
     * "motor"; returns the motor's power after every step.
     */
    private static List<Double> powersEveryStep(LinearOpMode opMode) {
        FakeHardwareMap robot = new FakeHardwareMap();
        robot.addMotor("motor");
        List<Double> powers = new ArrayList<>();
        new OpModeRunner(opMode, robot)
                .everyLoop(r -> powers.add(r.hardwareMap().motor("motor").getPower()))
                .run(1.0, 3.0)
                .assertNoException();
        return powers;
    }
}
