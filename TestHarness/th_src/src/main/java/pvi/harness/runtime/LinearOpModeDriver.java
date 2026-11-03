package pvi.harness.runtime;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpModeHarnessAccess;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import pvi.harness.agent.SimTime;

/**
 * Runs a {@link LinearOpMode}'s {@code runOpMode()} in step with the harness's loops, so it can
 * be tested like an iterative OpMode. {@link OpModeRunner} uses it automatically.
 *
 * <p><b>Why it's needed:</b> on the robot, a LinearOpMode runs {@code runOpMode()} on its own
 * thread, from top to bottom, while the Robot Controller keeps looping. Its {@code init()} and
 * {@code loop()} are empty. Without this class the harness would call those empty methods and
 * the OpMode would silently do nothing.</p>
 *
 * <p><b>How it works:</b> {@code runOpMode()} runs on one extra thread, but only one of the
 * two threads ever runs at a time, like passing a baton. Each runner step (init, init_loop,
 * start, loop, stop) hands the baton to the OpMode's thread and waits. The OpMode runs until it
 * reaches a <i>sync point</i>, then hands the baton back, and the runner moves simulated time
 * forward one loop. The sync points are the SDK's own waiting calls:</p>
 * <ul>
 *   <li>{@code waitForStart()}: one loop each time it checks for PLAY;</li>
 *   <li>{@code idle()}, and so {@code opModeIsActive()} while running: one loop;</li>
 *   <li>{@code sleep(ms)} (and {@code Thread.sleep}): as many loops as {@code ms} of simulated
 *       time, so {@code sleep(1000)} lasts exactly 50 loops of 20 ms;</li>
 *   <li>{@code isStarted()} and {@code isStopRequested()} (and so {@code opModeInInit()}): one
 *       loop for every {@value #CHECKS_PER_LOOP} checks that wait for something, so
 *       {@code while (opModeInInit()) {...}} also moves one loop per pass.</li>
 * </ul>
 * <p>The harness's Java agent sends those SDK calls here (see {@code HarnessAgent} and
 * {@code SimTime}). Because only one thread runs at a time, every run gives the same result.</p>
 *
 * <p><b>PLAY and STOP</b> set the SDK's own flags (through {@link LinearOpModeHarnessAccess}).
 * STOP also interrupts the OpMode's thread, as the Robot Controller does, so a {@code sleep}
 * ends early; the OpMode then gets up to {@value #MAX_STEPS_AFTER_STOP} more sync points to
 * finish, without time moving.</p>
 *
 * <p><b>Watchdog:</b> if the OpMode doesn't reach a sync point within
 * {@value #WATCHDOG_MILLIS} ms of real time (a loop like {@code while (!gamepad1.a) {}} that
 * never calls the SDK), the step fails with a message saying so. That thread can't be stopped
 * from outside, so it keeps running in the background until the tests end.</p>
 */
final class LinearOpModeDriver implements SimTime.OpModeThreadHooks {
    /** Real time the runner waits for the OpMode to reach a sync point before giving up. */
    static final long WATCHDOG_MILLIS = 3000;
    /** {@code isStarted()} / {@code isStopRequested()} checks per loop of simulated time. */
    static final int CHECKS_PER_LOOP = 2;
    /** Sync points the OpMode gets after STOP to finish before it counts as stuck. */
    static final int MAX_STEPS_AFTER_STOP = 50;

    private final LinearOpMode opMode;
    /** Runs {@code runOpMode()}. A daemon thread, so a stuck OpMode can't keep Java running. */
    private final Thread thread;
    /** The baton: released by the runner to let the OpMode thread run. */
    private final Semaphore opModeTurn = new Semaphore(0);
    /** The baton coming back: released by the OpMode thread at a sync point or when it ends. */
    private final Semaphore runnerTurn = new Semaphore(0);

    /** True once {@code runOpMode()} has returned or thrown. */
    private volatile boolean finished;
    /** What {@code runOpMode()} threw, or null. */
    private volatile Throwable crash;
    /** True once the crash has been passed to the runner, so it is reported only once. */
    private boolean crashReported;
    /** The watchdog's message once it has fired; every later step fails with it. */
    private String stuckMessage;
    /** Checks since the last sync point. Only the OpMode thread uses it. */
    private int checksSinceSync;

    LinearOpModeDriver(LinearOpMode opMode) {
        this.opMode = opMode;
        this.thread = new Thread(this::runOnOpModeThread, "LinearOpMode " + opMode.getClass().getSimpleName());
        thread.setDaemon(true);
    }

    // ---- Runner side: one call per runner step ----------------------------------------------

    /** INIT: starts {@code runOpMode()} and lets it run to its first sync point. */
    void init() {
        if (!SimTime.isAgentActive()) {
            throw new IllegalStateException("LinearOpModes need the harness Java agent, so sleep(),"
                    + " idle() and waitForStart() follow simulated time. Run the tests through Gradle"
                    + " (gradlew test, or Android Studio with Gradle as the test runner).");
        }
        thread.start();
        step();
    }

    /** One init_loop: the OpMode runs to its next sync point. */
    void initLoop() {
        step();
    }

    /** PLAY: {@code waitForStart()} returns, and the OpMode runs to its next sync point. */
    void start() {
        LinearOpModeHarnessAccess.pressPlay(opMode);
        step();
    }

    /** One loop: the OpMode runs to its next sync point. */
    void loop() {
        step();
    }

    /** STOP: asks the OpMode to stop and lets it run until {@code runOpMode()} returns. */
    void stop() {
        if (!thread.isAlive() && !finished) {
            return;   // never initialized
        }
        LinearOpModeHarnessAccess.pressStop(opMode);
        thread.interrupt();   // the Robot Controller interrupts the OpMode thread too
        for (int i = 0; i < MAX_STEPS_AFTER_STOP && !finished; i++) {
            step();
        }
        if (!finished) {
            throw new IllegalStateException(opMode.getClass().getSimpleName() + " was still running "
                    + MAX_STEPS_AFTER_STOP + " sync points after STOP. A LinearOpMode should end its"
                    + " loops when opModeIsActive() or isStopRequested() says so.");
        }
    }

    /** Hands the baton to the OpMode thread and waits (with the watchdog) until it comes back. */
    private void step() {
        if (stuckMessage != null) {
            throw new IllegalStateException(stuckMessage);
        }
        if (finished) {
            reportCrash();
            return;
        }
        // If a test uses two runners, robot code's sync points go to whichever one runs now.
        SimTime.installOpModeThreadHooks(this);
        opModeTurn.release();
        boolean cameBack;
        try {
            cameBack = runnerTurn.tryAcquire(WATCHDOG_MILLIS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for the OpMode", e);
        }
        if (!cameBack) {
            stuckMessage = opMode.getClass().getSimpleName() + " didn't call idle(), opModeIsActive(),"
                    + " sleep(), waitForStart(), isStarted() or isStopRequested() for "
                    + WATCHDOG_MILLIS / 1000 + " s of real time, so the harness can't move time on."
                    + " Is there a loop that never calls the SDK, like while (!gamepad1.a) { }?";
            LinearOpModeHarnessAccess.pressStop(opMode);
            thread.interrupt();
            throw new IllegalStateException(stuckMessage);
        }
        reportCrash();
    }

    /** Passes a crash in {@code runOpMode()} to the runner, once, as the robot would show it. */
    private void reportCrash() {
        Throwable thrown = crash;
        if (thrown == null || crashReported) {
            return;
        }
        crashReported = true;
        // On the robot, an InterruptedException after STOP is the normal way for runOpMode to end.
        if (thrown instanceof InterruptedException && opMode.isStopRequested()) {
            return;
        }
        if (thrown instanceof RuntimeException runtime) {
            throw runtime;
        }
        if (thrown instanceof Error error) {
            throw error;
        }
        throw new IllegalStateException(opMode.getClass().getSimpleName() + ".runOpMode() threw " + thrown, thrown);
    }

    // ---- OpMode thread side ------------------------------------------------------------------

    /** The OpMode thread's whole life: wait for the first baton, run runOpMode(), hand it back. */
    private void runOnOpModeThread() {
        opModeTurn.acquireUninterruptibly();
        try {
            LinearOpModeHarnessAccess.runOpMode(opMode);
        } catch (Throwable thrown) {
            crash = thrown;
        } finally {
            finished = true;
            runnerTurn.release();
        }
    }

    /** A sync point: hand the baton back to the runner and wait for the next step. */
    private void syncPoint() {
        checksSinceSync = 0;
        runnerTurn.release();
        // Uninterruptibly: STOP interrupts this thread, and the SDK reads that flag itself.
        opModeTurn.acquireUninterruptibly();
    }

    @Override
    public boolean isOpModeThread() {
        return Thread.currentThread() == thread;
    }

    @Override
    public void check() {
        checksSinceSync++;
        if (checksSinceSync >= CHECKS_PER_LOOP) {
            syncPoint();
        }
    }

    @Override
    public void idle() {
        syncPoint();
    }

    @Override
    public void waitOn(Object lock) throws InterruptedException {
        // waitForStart() waits here until PLAY; it checks isStarted() again after each loop.
        throwIfInterrupted();
        syncPoint();
        throwIfInterrupted();
    }

    @Override
    public void sleep(long millis) throws InterruptedException {
        if (millis < 0) {
            throw new IllegalArgumentException("timeout value is negative");   // as Thread.sleep says
        }
        throwIfInterrupted();
        if (millis == 0) {
            check();
            return;
        }
        long wakeAtNanos = SimTime.nanoTime() + millis * 1_000_000L;
        while (SimTime.nanoTime() < wakeAtNanos) {
            syncPoint();
            throwIfInterrupted();
        }
    }

    /** Like a real sleep or wait: if STOP interrupted the thread, clear the flag and throw. */
    private static void throwIfInterrupted() throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException("STOP was pressed");
        }
    }
}
