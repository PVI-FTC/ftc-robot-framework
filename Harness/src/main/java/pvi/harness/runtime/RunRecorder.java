package pvi.harness.runtime;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.Gamepad;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pvi.harness.ds.Axis;
import pvi.harness.ds.Button;
import pvi.harness.ds.TelemetryFrame;
import pvi.harness.hardware.FakeDcMotorEx;
import pvi.harness.hardware.FakeHardwareMap;
import pvi.harness.hardware.FakeServo;
import pvi.harness.hardware.FakeWebcam;
import pvi.harness.hardware.SeenAprilTag;
import pvi.harness.physics.GamePiece;
import pvi.harness.physics.PoseEstimator;

/**
 * Records a whole run, one {@link RunRecording.LoopRecord} per OpMode call, so it can be saved
 * as JSON and replayed in the viewer (or sent to Unity later).
 *
 * <p>Use it like this:</p>
 * <pre>{@code
 * OpModeRunner runner = new OpModeRunner(new TeamAAutoOpMode(), RobotConfigs.teamA());
 * RunRecorder recorder = RunRecorder.attachTo(runner);
 * runner.run(MatchPeriod.AUTONOMOUS);
 * Path file = recorder.save("teamAAutonomous");   // build/recordings/teamAAutonomous.json
 * }</pre>
 *
 * <p>It only watches through the runner's {@link OpModeRunner#everyLoop} hook. It never
 * changes the run, so a test gives the same result with or without a recorder.</p>
 */
public final class RunRecorder {
    /**
     * The telemetry caption autonomous OpModes use for their current step (TeamAAutoOpMode
     * sends it every loop). Its value is copied into {@link RunRecording.LoopRecord#autoStep()}.
     */
    public static final String AUTO_STEP_CAPTION = "Auto Step";

    /** The run being recorded. */
    private final OpModeRunner runner;
    /** One entry per OpMode call so far. */
    private final List<RunRecording.LoopRecord> loops = new ArrayList<>();
    /** How many telemetry frames had been sent when the last entry was recorded. */
    private int framesSeen;
    /** Marked moments, oldest first (see {@link #note}). */
    private final List<RunRecording.Event> events = new ArrayList<>();
    /** Each game piece as last written to the recording, by id (only looked up, never listed). */
    private final Map<String, GamePiece> piecesRecorded = new HashMap<>();
    /** A piece counts as moved once it is more than this far (inches) from where it was last recorded. */
    static final double PIECE_MOVE_INCHES = 0.01;

    private RunRecorder(OpModeRunner runner) {
        this.runner = runner;
    }

    /**
     * Starts recording a runner. Call it before {@code init()}, so the recording starts at INIT.
     */
    public static RunRecorder attachTo(OpModeRunner runner) {
        if (runner == null) {
            throw new IllegalArgumentException("A run recorder needs a runner.");
        }
        if (runner.stage() != null) {
            throw new IllegalStateException("Attach the run recorder before init(), so it sees the whole run.");
        }
        RunRecorder recorder = new RunRecorder(runner);
        runner.everyLoop(r -> recorder.recordLoop());
        return recorder;
    }

    /** Called after every OpMode call: copies the robot's state into a new loop entry. */
    private void recordLoop() {
        FakeHardwareMap hardwareMap = runner.hardwareMap();
        List<RunRecording.MotorState> motors = new ArrayList<>();
        List<RunRecording.ServoState> servos = new ArrayList<>();
        List<RunRecording.AprilTagState> aprilTags = new ArrayList<>();
        for (String name : hardwareMap.deviceNames()) {
            FakeDcMotorEx motor = hardwareMap.motor(name);
            if (motor != null) {
                motors.add(new RunRecording.MotorState(name, motor.getPower()));
            }
            FakeServo servo = hardwareMap.servo(name);
            if (servo != null) {
                servos.add(new RunRecording.ServoState(name, servo.getPosition()));
            }
            // The tags in each webcam's latest picture: what robot code could detect this call.
            FakeWebcam webcam = hardwareMap.webcam(name);
            if (webcam != null && webcam.isInUse()) {
                for (SeenAprilTag tag : webcam.lastFrameTags()) {
                    aprilTags.add(new RunRecording.AprilTagState(name, tag.id(), tag.rightInches(),
                            tag.forwardInches(), tag.upInches(), tag.yawDegrees(), tag.pitchDegrees(),
                            tag.rollDegrees()));
                }
            }
        }

        // Telemetry screens sent during this call are the ones added since the last entry.
        List<TelemetryFrame> allFrames = runner.telemetry().frames();
        List<RunRecording.TelemetryScreen> screens = new ArrayList<>();
        String autoStep = null;
        for (TelemetryFrame frame : allFrames.subList(framesSeen, allFrames.size())) {
            screens.add(new RunRecording.TelemetryScreen(frame.lines(), frame.log()));
            if (frame.hasCaption(AUTO_STEP_CAPTION)) {
                autoStep = frame.value(AUTO_STEP_CAPTION);
            }
        }
        framesSeen = allFrames.size();

        PoseEstimator estimator = runner.positionEstimator();
        loops.add(new RunRecording.LoopRecord(
                runner.clock().sinceInit(),
                runner.clock().sinceStart(),
                runner.stage(),
                estimator == null ? null : estimator.pose(),
                motors, servos, autoStep, screens,
                gamepadState(runner.opMode().gamepad1), gamepadState(runner.opMode().gamepad2),
                aprilTags, changedPieces(estimator)));
    }

    /**
     * The game pieces that moved more than {@link #PIECE_MOVE_INCHES} or changed state since they
     * were last recorded (since the header's list, the first time), in the header's order.
     */
    private List<GamePiece> changedPieces(PoseEstimator estimator) {
        if (estimator == null) {
            return List.of();
        }
        if (piecesRecorded.isEmpty()) {
            for (GamePiece p : estimator.pieces().initial()) {
                piecesRecorded.put(p.id(), p);
            }
        }
        List<GamePiece> changed = new ArrayList<>();
        for (GamePiece now : estimator.pieces().snapshot()) {
            GamePiece was = piecesRecorded.get(now.id());
            boolean moved = was == null || !was.state().equals(now.state())
                    || Math.sqrt(square(now.xInches() - was.xInches()) + square(now.yInches() - was.yInches())
                    + square(now.zInches() - was.zInches())) > PIECE_MOVE_INCHES;
            if (moved) {
                changed.add(now);
                piecesRecorded.put(now.id(), now);
            }
        }
        return changed;
    }

    private static double square(double value) {
        return value * value;
    }

    /** Copies a gamepad's buttons and sticks into plain data. */
    static RunRecording.GamepadState gamepadState(Gamepad pad) {
        List<Button> held = new ArrayList<>();
        for (Button button : Button.values()) {
            if (button.isPressed(pad)) {
                held.add(button);
            }
        }
        return new RunRecording.GamepadState(held,
                Axis.LEFT_STICK_X.read(pad), Axis.LEFT_STICK_Y.read(pad),
                Axis.RIGHT_STICK_X.read(pad), Axis.RIGHT_STICK_Y.read(pad),
                Axis.LEFT_TRIGGER.read(pad), Axis.RIGHT_TRIGGER.read(pad));
    }

    /**
     * Marks a moment in the recording, e.g. "Gamepad 1 controller disconnected", stamped with the
     * runner's current simulated time. Call it from the thread that runs the OpMode.
     */
    public void note(String text) {
        double matchTime = runner.clock().hasStarted() ? runner.clock().sinceStart() : 0.0;
        events.add(new RunRecording.Event(runner.clock().sinceInit(), matchTime, text));
    }

    /** The newest loop entry, or null before the first OpMode call. Cheap: nothing is copied. */
    public RunRecording.LoopRecord lastLoop() {
        return loops.isEmpty() ? null : loops.get(loops.size() - 1);
    }

    /** How many loop entries have been recorded so far. */
    public int loopCount() {
        return loops.size();
    }

    /** The marked events so far (including wall hits), in time order. */
    public List<RunRecording.Event> events() {
        return List.copyOf(eventsWithWallHits(runner.positionEstimator()));
    }

    /** The recording so far, as an immutable snapshot (call it again later for more loops). */
    public RunRecording recording() {
        PoseEstimator estimator = runner.positionEstimator();
        return new RunRecording(header(), loops, runner.driver1().rumbles(), runner.driver2().rumbles(),
                eventsWithWallHits(estimator));
    }

    /** What is being run, on which robot, with which drive model: the recording's header. */
    public RunRecording.Header header() {
        PoseEstimator estimator = runner.positionEstimator();
        return new RunRecording.Header(
                RunRecording.VERSION, "inches", "radians", "seconds",
                runner.hardwareMap().configName(),
                runner.opMode().getClass().getSimpleName(),
                periodOf(runner.opMode().getClass()),
                runner.loopPeriodSeconds(),
                estimator == null ? null : estimator.start(),
                estimator == null ? null : RunRecording.DriveModelValues.of(estimator.model()),
                estimator == null ? List.of() : estimator.pieces().initial());
    }

    /** The noted events plus every time the robot ran into a field wall, in time order. */
    private List<RunRecording.Event> eventsWithWallHits(PoseEstimator estimator) {
        if (estimator == null || estimator.wallHits().isEmpty()) {
            return events;
        }
        List<RunRecording.Event> all = new ArrayList<>(events);
        for (PoseEstimator.WallHit hit : estimator.wallHits()) {
            all.add(new RunRecording.Event(hit.sinceInitSeconds(), hit.matchTimeSeconds(),
                    "Robot hit the " + hit.wall() + " wall"));
        }
        all.sort(java.util.Comparator.comparingDouble(RunRecording.Event::sinceInitSeconds));
        return all;
    }

    /** AUTONOMOUS or TELEOP from the OpMode's annotation (what the Driver Station lists it under), or null. */
    static MatchPeriod periodOf(Class<?> opModeClass) {
        if (opModeClass.isAnnotationPresent(Autonomous.class)) {
            return MatchPeriod.AUTONOMOUS;
        }
        if (opModeClass.isAnnotationPresent(TeleOp.class)) {
            return MatchPeriod.TELEOP;
        }
        return null;
    }

    /**
     * Saves the recording as {@code build/recordings/<name>.json} (inside {@code Harness/} when
     * tests run through Gradle). See {@link RunRecordingJson} and the README for the format.
     *
     * @param name file name without ".json": letters, digits, '-', '_' and '.' only
     * @return the full path of the saved file
     */
    public Path save(String name) {
        return RunRecordingJson.save(recording(), RunRecordingJson.fileInDefaultFolder(name));
    }
}
