package pvi.harness.viewer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import pvi.harness.physics.Pose;
import pvi.harness.runtime.RunRecording;

/**
 * What the field drawing needs for any moment of a recording: the robot's pose, its trail up
 * to then, the robot box and a caption. Works these out once, so moving through a long
 * recording stays quick.
 *
 * <p>Read-only: it never changes the recording, so it is safe to use from the Swing thread.</p>
 */
public final class RecordingView {
    /** The recording being shown. */
    private final RunRecording recording;
    /** Every recorded pose (skipping loops without one), oldest first. */
    private final List<Pose> poses = new ArrayList<>();
    /** For each loop index: how many entries of {@link #poses} belong to loops up to and including it. */
    private final int[] posesUpTo;
    /** The robot's size, from the recording's drive model. */
    private final FieldPainter.RobotBox box;
    /** For each loop index: how the robot was moving, worked out from the recorded poses. */
    private final Motion[] motions;

    /**
     * How the robot was moving at one moment, worked out from where it was in neighbouring loops
     * (the recording saves positions, not speeds).
     *
     * @param speedInchesPerSecond    speed over the ground
     * @param speedChangePerSecond    how fast that speed is changing, in in/s²: positive while
     *                                speeding up, negative while slowing down
     * @param turnDegreesPerSecond    turn rate, counter-clockwise positive
     */
    public record Motion(double speedInchesPerSecond, double speedChangePerSecond, double turnDegreesPerSecond) {
        /** Not moving (or position not tracked). */
        public static final Motion STILL = new Motion(0, 0, 0);
    }

    /** Prepares a recording for drawing. It must have at least one loop. */
    public RecordingView(RunRecording recording) {
        if (recording == null || recording.loops().isEmpty()) {
            throw new IllegalArgumentException("The recording has no loops to show.");
        }
        this.recording = recording;
        List<RunRecording.LoopRecord> loops = recording.loops();
        posesUpTo = new int[loops.size()];
        for (int i = 0; i < loops.size(); i++) {
            if (loops.get(i).pose() != null) {
                poses.add(loops.get(i).pose());
            }
            posesUpTo[i] = poses.size();
        }
        box = FieldPainter.RobotBox.of(recording.header().driveModel());
        motions = workOutMotions(loops);
    }

    /**
     * Speed, change of speed and turn rate for every loop, from the poses: each loop's movement
     * since the one before, divided by the time between them.
     */
    private static Motion[] workOutMotions(List<RunRecording.LoopRecord> loops) {
        Motion[] result = new Motion[loops.size()];
        result[0] = Motion.STILL;
        for (int i = 1; i < loops.size(); i++) {
            RunRecording.LoopRecord before = loops.get(i - 1);
            RunRecording.LoopRecord now = loops.get(i);
            double dt = now.sinceInitSeconds() - before.sinceInitSeconds();
            if (now.pose() == null || before.pose() == null || dt <= 0) {
                result[i] = dt <= 0 ? result[i - 1] : Motion.STILL;
                continue;
            }
            double speed = Math.hypot(now.pose().xInches() - before.pose().xInches(),
                    now.pose().yInches() - before.pose().yInches()) / dt;
            double turn = Math.toDegrees(Math.IEEEremainder(
                    now.pose().headingRadians() - before.pose().headingRadians(), 2 * Math.PI)) / dt;
            result[i] = new Motion(speed, (speed - result[i - 1].speedInchesPerSecond()) / dt, turn);
        }
        return result;
    }

    /** How the robot was moving at loop {@code index}. */
    public Motion motion(int index) {
        checkIndex(index);
        return motions[index];
    }

    /** Text at the start of the event the recorder adds for a wall hit ("Robot hit the +x wall"). */
    static final String WALL_HIT_EVENT = "Robot hit the ";

    /**
     * Which wall the robot hit during the {@code seconds} before loop {@code index} ("+x", "-y",
     * "+x/-y" in a corner), or null if it hit none then. Used to flash the wall on the field.
     */
    public String wallHitWithin(int index, double seconds) {
        double now = loop(index).sinceInitSeconds();
        String wall = null;
        for (RunRecording.Event event : recording.events()) {
            if (event.text().startsWith(WALL_HIT_EVENT) && event.sinceInitSeconds() <= now + 1e-9
                    && event.sinceInitSeconds() > now - seconds) {
                wall = event.text().substring(WALL_HIT_EVENT.length()).replace(" wall", "").trim();
            }
        }
        return wall;
    }

    /** The recording being shown. */
    public RunRecording recording() {
        return recording;
    }

    /** How many loop entries the recording has. */
    public int size() {
        return posesUpTo.length;
    }

    /** The loop entry at {@code index}. */
    public RunRecording.LoopRecord loop(int index) {
        checkIndex(index);
        return recording.loops().get(index);
    }

    /** The robot's size for drawing. */
    public FieldPainter.RobotBox box() {
        return box;
    }

    /** Every pose from the start up to and including loop {@code index}. */
    public List<Pose> trail(int index) {
        checkIndex(index);
        return Collections.unmodifiableList(poses.subList(0, posesUpTo[index]));
    }

    /** Top-of-field caption, e.g. {@code TeamAAutoOpMode  LOOP  t=1.24 s}. */
    public String caption(int index) {
        RunRecording.LoopRecord loop = loop(index);
        return String.format(Locale.ROOT, "%s  %s  t=%.2f s", recording.header().opMode(), loop.stage(),
                loop.matchTimeSeconds());
    }

    /** Fails clearly for an index outside the recording. */
    private void checkIndex(int index) {
        if (index < 0 || index >= posesUpTo.length) {
            throw new IllegalArgumentException("Loop index " + index + " is outside the recording (0 to "
                    + (posesUpTo.length - 1) + ").");
        }
    }
}
