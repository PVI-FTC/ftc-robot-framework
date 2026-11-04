package pvi.harness.pvi;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import com.qualcomm.robotcore.hardware.DcMotorSimple.Direction;

import org.firstinspires.ftc.teamcode.opmodes.testing.TeamAAprilTagVisionTestOpMode;
import org.junit.jupiter.api.Test;

import pvi.harness.hardware.FakeHardwareMap;
import pvi.harness.hardware.RobotConfigs;
import pvi.harness.physics.DriveModels;
import pvi.harness.physics.Pose;
import pvi.harness.physics.PoseEstimator;
import pvi.harness.physics.WheelReader;
import pvi.harness.runtime.HarnessConfig;
import pvi.harness.runtime.OpModeRunner;
import pvi.harness.runtime.RunRecorder;
import pvi.harness.runtime.RunRecording;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PVI extra: Team 2's "Team A AprilTag Vision Test" OpMode on the settings file's robot, whose
 * webcam sees the field's tags by itself ({@code webcam.logitechVisionWebcam.aprilTags=field}),
 * while the robot turns slowly through a full circle near the field's audience side. Tags come
 * into and go out of view as it turns. The recording ({@code build/recordings/vision-field-tags.json})
 * is for checking the camera view in Unity and the viewer (C23).
 *
 * <p>The vision OpMode never drives, so the test turns the robot by setting the drive motors'
 * powers itself after every OpMode call: the left wheels backward and the right wheels forward
 * (each motor's power times its {@code drive.*.direction} sign, as {@code WheelReader} reads it).</p>
 */
class VisionFieldTagsRecordingTest {
    /**
     * Toward the audience, facing the red tags (-x), about 38 in from the raised red AUDIENCE tags
     * 34-37, which the camera's 18 degree tilt up (C24) brings into view with the lower SCORING
     * tags 30-33.
     */
    private static final Pose START = Pose.inches(55, -12, 180);
    /** Drive power for the slow turn: about 45 degrees a second. */
    private static final double TURN_POWER = 0.25;

    /** Runs the turn and returns the recording. */
    private static RunRecording turnThroughACircle() {
        FakeHardwareMap robot = RobotConfigs.teamA();
        OpModeRunner runner = new OpModeRunner(new TeamAAprilTagVisionTestOpMode(), robot);
        PoseEstimator.attachTo(runner, DriveModels.teamA(), WheelReader.teamA(), START);
        RunRecorder recorder = RunRecorder.attachTo(runner);
        HarnessConfig.DriveMotorNames names = HarnessConfig.load().driveMotors();
        HarnessConfig.DriveMotorDirections directions = HarnessConfig.load().driveDirections();
        runner.everyLoop(r -> {
            r.hardwareMap().motor(names.frontLeft()).setPower(-TURN_POWER * sign(directions.frontLeft()));
            r.hardwareMap().motor(names.rearLeft()).setPower(-TURN_POWER * sign(directions.rearLeft()));
            r.hardwareMap().motor(names.frontRight()).setPower(TURN_POWER * sign(directions.frontRight()));
            r.hardwareMap().motor(names.rearRight()).setPower(TURN_POWER * sign(directions.rearRight()));
        });
        runner.run(0.5, 9.0).assertNoException();
        Path saved = recorder.save("vision-field-tags");
        System.out.println("Recording to watch: " + saved);
        return recorder.recording();
    }

    /** +1 if positive power drives this wheel forward (its configured direction is FORWARD), else -1. */
    private static double sign(Direction drivesForward) {
        return drivesForward == Direction.REVERSE ? -1 : 1;
    }

    /**
     * WHAT: Turning a full circle, the webcam sees several of the field's tags, each one coming
     * into view and going out of it again.
     */
    @Test
    void tagsComeIntoAndGoOutOfViewAsTheRobotTurns() {
        RunRecording recording = turnThroughACircle();

        TreeSet<Integer> seen = new TreeSet<>();
        int changes = 0;
        List<Integer> previous = List.of();
        double turnedDegrees = 0;
        double lastHeading = START.headingRadians();
        for (RunRecording.LoopRecord loop : recording.loops()) {
            List<Integer> now = new ArrayList<>();
            loop.aprilTags().forEach(tag -> now.add(tag.id()));
            seen.addAll(now);
            if (!now.equals(previous)) {
                changes++;
            }
            previous = now;
            if (loop.pose() != null) {
                turnedDegrees += Math.toDegrees(Math.IEEEremainder(loop.pose().headingRadians() - lastHeading, 2 * Math.PI));
                lastHeading = loop.pose().headingRadians();
            }
        }

        assertTrue(turnedDegrees > 340, "turned a full circle counter-clockwise, but turned " + turnedDegrees);
        assertTrue(seen.size() >= 3, "several tags seen: " + seen);
        assertTrue(seen.containsAll(List.of(34, 35, 36, 37)), "the raised red AUDIENCE tags too: " + seen);
        assertTrue(changes >= 4, "tags came and went (" + changes + " changes in view)");
        assertEquals(1, recording.header().cameras().size(), "the webcam is in the header");
    }
}
