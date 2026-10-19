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
