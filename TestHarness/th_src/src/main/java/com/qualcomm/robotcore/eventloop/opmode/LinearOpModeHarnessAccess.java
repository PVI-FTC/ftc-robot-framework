package com.qualcomm.robotcore.eventloop.opmode;

/**
 * HARNESS FILE, not robot code and not the FTC SDK. Lets the harness reach the few
 * package-private parts of the SDK's {@link LinearOpMode} that the Robot Controller uses to run
 * one.
 *
 * <p>It lives in the SDK's package so it can use them directly (no reflection). If a new SDK
 * renames one, this file stops compiling, which says exactly what changed.</p>
 *
 * <p>PLAY and STOP set the SDK's own {@code isStarted} and {@code stopRequested} flags, the
 * same ones the Robot Controller sets. The SDK's {@code internalOnStart()} and
 * {@code internalOnStopRequested()} are not called: they wake the OpMode's thread with
 * {@code notifyAll()} and shut down the SDK's own thread pool, and in the harness the OpMode's
 * thread is woken by {@code pvi.harness.runtime.LinearOpModeDriver} instead.</p>
 */
public final class LinearOpModeHarnessAccess {

    /** Only static methods here; nothing to create. */
    private LinearOpModeHarnessAccess() {
    }

    /**
     * Runs the OpMode's {@code runOpMode()} the way the Robot Controller's OpMode thread does:
     * when it returns, the OpMode asks to stop ({@code requestOpModeStop()}), as on the robot.
     *
     * <p>The SDK's own {@code internalRunOpMode()} does the same but also writes to Android's
     * log, which doesn't work on a laptop, so this calls the two steps itself.</p>
     */
    public static void runOpMode(LinearOpMode opMode) throws InterruptedException {
        opMode.runOpMode();
        opMode.requestOpModeStop();
    }

    /** PLAY was pressed: {@code isStarted()} and {@code opModeIsActive()} now return true. */
    public static void pressPlay(LinearOpMode opMode) {
        opMode.stopRequested = false;
        opMode.isStarted = true;
    }

    /** STOP was pressed: {@code isStopRequested()} now returns true. */
    public static void pressStop(LinearOpMode opMode) {
        opMode.stopRequested = true;
    }
}
