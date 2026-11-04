package pvi.harness.runtime;

import pvi.harness.ds.FakeTelemetry;
import pvi.harness.ds.RumbleRecord;

import java.util.List;

/**
 * What happened during a full run started with {@link OpModeRunner#run}.
 *
 * <p>A crash inside the OpMode does not fail the test by itself; it is stored here, the
 * OpMode is stopped the way the Robot Controller would stop it, and the test decides
 * what to check. Call {@link #assertNoException()} when a crash should fail the test.</p>
 */
public final class RunResult {
    /** How far through the OpMode lifecycle the run got. */
    public enum Stage { INIT, INIT_LOOP, START, LOOP, STOP }

    /** The OpMode's crash, or null if it didn't crash. */
    private final Throwable exception;
    /** Which lifecycle call crashed, or null. */
    private final Stage exceptionStage;
    /** Simulated seconds between INIT and PLAY. */
    private final double initSeconds;
    /** Simulated seconds between PLAY and the end of the run. */
    private final double matchSeconds;
    /** How many loop() calls ran. */
    private final int loopCount;
    /** True when the OpMode called requestOpModeStop(). */
    private final boolean stopRequestedByOpMode;
    /** The run's telemetry, including every frame sent. */
    private final FakeTelemetry telemetry;
    /** Rumbles sent to gamepad 1 (driver), in order. */
    private final List<RumbleRecord> gamepad1Rumbles;
    /** Rumbles sent to gamepad 2 (operator), in order. */
    private final List<RumbleRecord> gamepad2Rumbles;

    /** Made by OpModeRunner at the end of a run; tests only read it. */
    RunResult(Throwable exception, Stage exceptionStage, double initSeconds, double matchSeconds,
              int loopCount, boolean stopRequestedByOpMode, FakeTelemetry telemetry,
              List<RumbleRecord> gamepad1Rumbles, List<RumbleRecord> gamepad2Rumbles) {
        this.exception = exception;
        this.exceptionStage = exceptionStage;
        this.initSeconds = initSeconds;
        this.matchSeconds = matchSeconds;
        this.loopCount = loopCount;
        this.stopRequestedByOpMode = stopRequestedByOpMode;
        this.telemetry = telemetry;
        this.gamepad1Rumbles = List.copyOf(gamepad1Rumbles);
        this.gamepad2Rumbles = List.copyOf(gamepad2Rumbles);
    }

    /** True when the OpMode threw an exception during the run. */
    public boolean crashed() { return exception != null; }
    /** The OpMode's exception (with its stack trace), or null. */
    public Throwable exception() { return exception; }
    /** Which lifecycle call threw, or null when nothing did. */
    public Stage exceptionStage() { return exceptionStage; }
    /** Simulated seconds spent between INIT and PLAY. */
    public double initSeconds() { return initSeconds; }
    /** Simulated seconds between PLAY and STOP. */
    public double matchSeconds() { return matchSeconds; }
    /** How many loop() calls ran (a full TeleOp at 20 ms is about 6000). */
    public int loopCount() { return loopCount; }
    /** True when the OpMode ended itself with requestOpModeStop(). */
    public boolean stopRequestedByOpMode() { return stopRequestedByOpMode; }
    /** Everything shown on the Driver Station during the run. */
    public FakeTelemetry telemetry() { return telemetry; }
    /** Rumbles sent to the driver's gamepad. */
    public List<RumbleRecord> gamepad1Rumbles() { return gamepad1Rumbles; }
    /** Rumbles sent to the operator's gamepad. */
    public List<RumbleRecord> gamepad2Rumbles() { return gamepad2Rumbles; }

    /** Fails with the OpMode's own exception (and stack trace) if it crashed. */
    public RunResult assertNoException() {
        if (exception != null) {
            throw new AssertionError("OpMode crashed during " + exceptionStage
                    + " at match time " + String.format("%.2f", matchSeconds) + " s", exception);
        }
        return this;
    }

    /** One-line summary for test output, e.g. RunResult[init=1.00s match=120.00s loops=6000 crashed=false]. */
    @Override
    public String toString() {
        return String.format("RunResult[init=%.2fs match=%.2fs loops=%d crashed=%s%s]",
                initSeconds, matchSeconds, loopCount, crashed(),
                crashed() ? " in " + exceptionStage + ": " + exception : "");
    }
}
