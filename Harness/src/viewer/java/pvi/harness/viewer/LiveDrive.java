package pvi.harness.viewer;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;

import org.firstinspires.ftc.teamcode.opmodes.teleop.TeamATeleOp;
import org.firstinspires.ftc.teamcode.opmodes.teleop.TeamBTeleOp;
import org.firstinspires.ftc.teamcode.opmodes.teleop.TeamCTeleOp;

import pvi.harness.hardware.FakeHardwareMap;
import pvi.harness.hardware.RobotConfigs;
import pvi.harness.runtime.MatchPeriod;
import pvi.harness.runtime.OpModeRunner;
import pvi.harness.runtime.RunRecorder;
import pvi.harness.runtime.RunRecording;
import pvi.harness.runtime.RunnerRegistry;

/**
 * Drives a real TeleOp live from the keyboard, like a practice match: INIT, wait for PLAY,
 * drive until STOP (or the end of the 2:00 match), then STOP.
 *
 * <p>The OpMode runs on its own thread so the window stays responsive. That thread is the only
 * one that touches the runner, fake robot and TeamCode. The window only reads immutable
 * {@link RunRecording} snapshots from {@link #latest()}.</p>
 *
 * <p><b>Time.</b> Robot code still runs on simulated time: each loop moves the clock exactly one
 * loop period (20 ms), as in tests. To make driving feel real, this thread waits between loops
 * until real time catches up. That waiting lives here, never in the runner. If the computer
 * falls behind, loops just run late; simulated time stays exact, so the saved recording replays
 * the same way every time.</p>
 */
public final class LiveDrive {

    /** Where the drive is. */
    public enum Phase { INIT, DRIVING, FINISHED }

    /**
     * A TeleOp that can be driven live.
     *
     * @param id     short name for saved files, e.g. "teamA-teleop"
     * @param title  what the dropdown shows
     * @param opMode makes a fresh OpMode
     * @param robot  makes its fake robot
     */
    public record Option(String id, String title, Supplier<OpMode> opMode, Supplier<FakeHardwareMap> robot) {
    }

    /** Every TeleOp the dropdown offers for live driving, in order. */
    public static List<Option> options() {
        return List.of(
                new Option("teamA-teleop", "Team A TeleOp (keyboard)", TeamATeleOp::new, RobotConfigs::teamA),
                new Option("teamB-teleop", "Team B TeleOp (keyboard)", TeamBTeleOp::new, RobotConfigs::teamB),
                new Option("teamC-teleop", "Team C TeleOp (keyboard)", TeamCTeleOp::new, RobotConfigs::teamC));
    }

    /** How often the window gets a new snapshot, in real seconds (about 25 per second). */
    private static final double PUBLISH_SECONDS = 0.04;

    private final OpModeRunner runner;
    private final RunRecorder recorder;
    /** Match length; the drive stops by itself at the end, like a real match. */
    private final double matchSeconds;
    /** True: wait for real time between loops. False (tests): run as fast as possible. */
    private final boolean paced;

    private volatile Phase phase = Phase.INIT;
    private volatile boolean playRequested;
    /** Whether to save the drive; set once, when PLAY is pressed. */
    private volatile boolean keepRecording;
    private volatile boolean stopRequested;
    private volatile RuntimeException error;
    private final AtomicReference<RunRecording> latest = new AtomicReference<>();
    private Thread thread;

    /**
     * Sets up a live drive. Nothing runs until {@link #start()}.
     *
     * @param option       which TeleOp and robot
     * @param keys         the held keyboard keys (gamepad 1 and 2 read them, see {@link KeyboardGamepads})
     * @param matchSeconds when to stop by itself, e.g. {@link MatchPeriod#TELEOP} seconds
     * @param paced        true to drive in real time; false for tests
     */
    public LiveDrive(Option option, HeldKeys keys, double matchSeconds, boolean paced) {
        // Each drive makes a new runner; forget earlier ones (only tests read them).
        RunnerRegistry.startTest();
        this.runner = new OpModeRunner(option.opMode().get(), option.robot().get())
                .gamepad1(KeyboardGamepads.driver(keys))
                .gamepad2(KeyboardGamepads.operator(keys));
        this.recorder = RunRecorder.attachTo(runner);
        this.matchSeconds = matchSeconds;
        this.paced = paced;
    }

    /** Presses INIT and starts the drive's thread. Returns at once. */
    public void start() {
        if (thread != null) {
            throw new IllegalStateException("This live drive was already started.");
        }
        thread = new Thread(this::runDrive, "harness-live-drive");
        thread.setDaemon(true);   // closing the viewer never waits for it
        thread.start();
    }

    /** Presses PLAY and keeps the recording (see {@link #requestPlay(boolean)}). */
    public void requestPlay() {
        requestPlay(true);
    }

    /**
     * Presses PLAY. {@code keepRecording} says whether this drive should be saved when it
     * finishes (the window's Record button). It is decided here, at PLAY, and can't change
     * afterwards. Only the first press counts. Safe from any thread.
     */
    public synchronized void requestPlay(boolean keepRecording) {
        if (playRequested || stopRequested) {
            return;
        }
        this.keepRecording = keepRecording;
        playRequested = true;
    }

    /**
     * True if this drive should be saved: PLAY was pressed with recording on. A drive stopped
     * before PLAY is never kept.
     */
    public boolean keepRecording() {
        return playRequested && keepRecording;
    }

    /** Presses STOP. The drive finishes after its current loop. Safe from any thread. */
    public void requestStop() {
        stopRequested = true;
    }

    /** Where the drive is now. */
    public Phase phase() {
        return phase;
    }

    /** The newest snapshot of the recording, or null before the first OpMode call. */
    public RunRecording latest() {
        return latest.get();
    }

    /** The robot code's crash, if it crashed (the drive then finishes early); otherwise null. */
    public RuntimeException error() {
        return error;
    }

    /** The match length this drive stops at, in seconds. */
    public double matchSeconds() {
        return matchSeconds;
    }

    /** Waits up to {@code timeoutSeconds} for the drive to finish. Returns true if it did. */
    public boolean awaitFinished(double timeoutSeconds) throws InterruptedException {
        thread.join(Math.max(1, (long) (timeoutSeconds * 1000)));
        return phase == Phase.FINISHED;
    }

    // ---- The drive's thread ----------------------------------------------------------------

    /** INIT, init_loop until PLAY, loop until STOP or the end of the match, STOP. */
    private void runDrive() {
        long realStart = System.nanoTime();
        long lastPublish = realStart;
        try {
            runner.init();
            publish();
            while (!playRequested && !stopRequested) {
                waitForRealTime(realStart);
                runner.initLoop(1);
                lastPublish = publishIfDue(lastPublish);
            }
            if (!stopRequested) {
                runner.start();
                phase = Phase.DRIVING;
                while (!stopRequested && !runner.isStopRequested()
                        && runner.clock().sinceStart() < matchSeconds - 1e-9) {
                    waitForRealTime(realStart);
                    runner.loop(1);
                    lastPublish = publishIfDue(lastPublish);
                }
            }
            runner.stop();
        } catch (RuntimeException e) {
            // Like the Robot Controller: a crash ends the OpMode. Keep what was recorded.
            error = e;
        } finally {
            publish();
            phase = Phase.FINISHED;
        }
    }

    /** Sleeps until real time has caught up with simulated time (only when paced). */
    private void waitForRealTime(long realStart) {
        if (!paced) {
            return;
        }
        long due = realStart + (long) (runner.clock().sinceInit() * 1e9);
        long wait = due - System.nanoTime();
        if (wait > 0) {
            try {
                TimeUnit.NANOSECONDS.sleep(wait);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                stopRequested = true;
            }
        }
    }

    /** Publishes a snapshot if enough real time has passed since the last one (always when not paced). */
    private long publishIfDue(long lastPublish) {
        long now = System.nanoTime();
        if (!paced || now - lastPublish >= PUBLISH_SECONDS * 1e9) {
            publish();
            return now;
        }
        return lastPublish;
    }

    /** Makes a new immutable snapshot for the window. */
    private void publish() {
        latest.set(recorder.recording());
    }
}
