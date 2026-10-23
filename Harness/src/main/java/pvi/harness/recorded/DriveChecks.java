package pvi.harness.recorded;

import java.util.Locale;

import pvi.harness.physics.DisplayUnit;
import pvi.harness.physics.Pose;
import pvi.harness.runtime.RunRecording;
import pvi.harness.runtime.RunRecording.LoopRecord;
import pvi.harness.runtime.RunRecording.MotorState;

/**
 * Safety checks that make sense for <b>any</b> run, used by recorded-drive tests (step D4).
 *
 * <p>Each one looks at a {@link RunRecording} and throws an {@link AssertionError} with a
 * plain-words message when something is wrong, so it fails a JUnit test like
 * {@code assertEquals} does.</p>
 */
public final class DriveChecks {

    private DriveChecks() {
    }

    /** Fails if any motor was ever set to a power outside -1 to 1. */
    public static void assertMotorPowersInRange(RunRecording run) {
        for (LoopRecord loop : run.loops()) {
            for (MotorState motor : loop.motors()) {
                if (!(Math.abs(motor.power()) <= 1.0 + 1e-9)) {
                    throw new AssertionError(String.format(Locale.ROOT,
                            "Motor \"%s\" was set to power %s at %s (%.2f s after PLAY). Powers must be between -1 and 1.",
                            motor.name(), motor.power(), loop.stage(), loop.matchTimeSeconds()));
                }
            }
        }
    }

    /**
     * Fails the first time any corner of the robot box is outside the ±72 in field. The box
     * size comes from the recording's drive model. Does nothing if position wasn't tracked.
     */
    public static void assertRobotStaysOnField(RunRecording run) {
        RunRecording.DriveModelValues model = run.header().driveModel();
        if (model == null) {
            return;   // robot without the four drive motors: no position to check
        }
        double halfLength = model.length().value() / 2.0;
        double halfWidth = model.width().value() / 2.0;
        // Each corner as (inches toward the front, inches toward the left side).
        double[][] corners = {{halfLength, halfWidth}, {halfLength, -halfWidth},
                {-halfLength, halfWidth}, {-halfLength, -halfWidth}};
        String[] cornerNames = {"front-left", "front-right", "back-left", "back-right"};

        for (LoopRecord loop : run.loops()) {
            Pose pose = loop.pose();
            if (pose == null) {
                continue;
            }
            double cos = Math.cos(pose.headingRadians());
            double sin = Math.sin(pose.headingRadians());
            for (int i = 0; i < corners.length; i++) {
                double cornerX = pose.xInches() + corners[i][0] * cos - corners[i][1] * sin;
                double cornerY = pose.yInches() + corners[i][0] * sin + corners[i][1] * cos;
                if (Math.abs(cornerX) > Pose.FIELD_HALF_WIDTH_INCHES + 1e-9
                        || Math.abs(cornerY) > Pose.FIELD_HALF_WIDTH_INCHES + 1e-9) {
                    boolean placeholders = !model.length().measured() || !model.width().measured();
                    throw new AssertionError(String.format(Locale.ROOT,
                            "The robot left the field at %.2f s after PLAY: its %s corner was at x=%.2f in,"
                                    + " y=%.2f in (field edges are at +/-%.0f in). Robot center: %s.%s",
                            loop.matchTimeSeconds(), cornerNames[i], cornerX, cornerY,
                            Pose.FIELD_HALF_WIDTH_INCHES, pose.toString(DisplayUnit.INCHES),
                            placeholders ? " (The robot size is still a placeholder.)" : ""));
                }
            }
        }
    }

    /** Where the robot ended up: the last tracked pose, or null if position wasn't tracked. */
    public static Pose finalPose(RunRecording run) {
        for (int i = run.loops().size() - 1; i >= 0; i--) {
            if (run.loops().get(i).pose() != null) {
                return run.loops().get(i).pose();
            }
        }
        return null;
    }
}
