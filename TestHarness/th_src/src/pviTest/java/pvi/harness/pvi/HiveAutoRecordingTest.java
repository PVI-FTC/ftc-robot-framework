package pvi.harness.pvi;

import java.util.ArrayList;
import java.util.List;

import com.qualcomm.hardware.limelightvision.Limelight3A;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;

import org.firstinspires.ftc.teamcode.KimCode.HiveAutoBlue;
import org.firstinspires.ftc.teamcode.KimCode.HiveAutoRed;
import org.junit.jupiter.api.Test;

import pvi.harness.hardware.FakeDcMotorEx;
import pvi.harness.hardware.FakeHardwareMap;
import pvi.harness.hardware.FakeLimelight;
import pvi.harness.hardware.FakeWebcam;
import pvi.harness.hardware.LimelightView;
import pvi.harness.hardware.RobotConfigs;
import pvi.harness.hardware.SeenAprilTag;
import pvi.harness.physics.DriveModels;
import pvi.harness.physics.Pose;
import pvi.harness.physics.PoseEstimator;
import pvi.harness.physics.WheelReader;
import pvi.harness.runtime.HarnessConfig;
import pvi.harness.runtime.OpModeCatalog;
import pvi.harness.runtime.OpModeRunner;
import pvi.harness.runtime.RunRecorder;
import pvi.harness.runtime.RunResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PVI extra: Hive Auto on {@code robots/hive}, with a simulated Limelight. Each test checks where the
 * robot fires from and where it ends up. The Red and Blue runs also save recordings to watch
 * ({@code build/recordings/HiveAuto-Red.json} and {@code HiveAuto-Blue.json}).
 *
 * <p>A CELL's opening points along the field's x axis, away from the HIVE's pivot, so the robot has
 * to fire along x. These tests exist partly because an earlier version fired along y, into the CELL's
 * side wall.</p>
 *
 * <p>The results depend on the motor directions in {@code robots/hive/drive.properties}, which are
 * the ones in {@code pedro/Constants.java}. If the team changes those, change both together.</p>
 */
class HiveAutoRecordingTest {
    private static final double START_X_INCHES = -12.0;
    private static final double START_Y_INCHES = -61.5;
    private static final double LAUNCH_X_INCHES = 52.0;
    private static final double CELL_Y_INCHES = -12.75;
    private static final double POSITION_TOLERANCE_INCHES = 3.0;
    private static final double HEADING_TOLERANCE_DEGREES = 3.0;

    /** The tags' y on each CELL's cluster, for Red. The field file has only the start tilt. */
    private static final double[] FAR_CELL_TAG_Y_INCHES = {-6.24, -9.99, -15.49, -19.24};
    private static final double[] AUDIENCE_CELL_TAG_Y_INCHES = {-19.24, -15.49, -9.99, -6.24};
    private static final double CAMERA_FORWARD_INCHES = 8.0;
    private static final double CAMERA_UP_INCHES = 20.0;
    private static final double CAMERA_TILT_DEGREES = 25.0;

    /** What one run did. {@code firstShot} is null if the robot never fired. */
    private record Outcome(Pose firstShot, Pose end, int wallHits, int obstacleHits, String stage, String shot) {
    }

    private static FakeHardwareMap robotFor(String simpleName) {
        HarnessConfig config = HarnessConfig.load();
        return RobotConfigs.forOpMode(OpModeCatalog.fromConfig(config).stream()
                .filter(e -> e.simpleName().equals(simpleName)).findFirst().orElseThrow(), config);
    }

    /** Runs a whole AUTO. {@code recordingName} may be null to save nothing. */
    private static Outcome run(OpMode opMode, String simpleName, boolean blue, boolean tipped, String recordingName) {
        double sign = blue ? -1 : 1;
        Pose start = new Pose(sign * START_X_INCHES, sign * START_Y_INCHES, Math.toRadians(blue ? -90 : 90));
        FakeHardwareMap map = robotFor(simpleName);
        OpModeRunner runner = new OpModeRunner(opMode, map);
        PoseEstimator estimator = PoseEstimator.attachTo(runner, DriveModels.fromConfig(map.settings()),
                WheelReader.fromConfig(map.settings()), start);
        RunRecorder recorder = RunRecorder.attachTo(runner);
        FakeLimelight limelight = (FakeLimelight) map.get(Limelight3A.class, "limelight");
        FakeWebcam camera = map.webcam("limelightCamera");
        // The Auto reads the Limelight, not a VisionPortal, so this keeps the camera taking pictures.
        camera.connect((tags, frameTimeNanos) -> { });
        FakeDcMotorEx feed = map.motor("intake");
        Pose[] firstShot = {null};
        runner.everyLoop(r -> {
            Pose now = r.positionEstimator().pose();
            limelight.setView(tipped ? tippedView(now, blue) : viewOf(camera.lastFrameTags()));
            if (firstShot[0] == null && Math.abs(feed.getPower()) > 0.01) {
                firstShot[0] = now;
            }
        });

        RunResult result = runner.run(1.0, 30.0);
        result.assertNoException();
        if (recordingName != null) {
            System.out.println("Recording to watch: " + recorder.save(recordingName));
        }
        return new Outcome(firstShot[0], estimator.pose(), estimator.wallHits().size(),
                estimator.obstacleHits().size(), result.telemetry().lastValue("Stage"),
                result.telemetry().lastValue("Shot"));
    }

    private static LimelightView viewOf(List<SeenAprilTag> tags) {
        return tags.isEmpty() ? LimelightView.nothing() : LimelightView.tags(tags.toArray(new SeenAprilTag[0]));
    }

    /**
     * What the camera sees with the HIVE tipped, worked out by hand because the field file has only
     * the start tilt. The far CELL's tags are raised, and every tag faces toward -x and down (for Red).
     */
    private static LimelightView tippedView(Pose robot, boolean blue) {
        List<SeenAprilTag> seen = new ArrayList<>();
        double heading = robot.headingRadians();
        double cameraX = robot.xInches() + CAMERA_FORWARD_INCHES * Math.cos(heading);
        double cameraY = robot.yInches() + CAMERA_FORWARD_INCHES * Math.sin(heading);
        double tilt = Math.toRadians(CAMERA_TILT_DEGREES);
        double sign = blue ? -1 : 1;
        for (int i = 0; i < 8; i++) {
            boolean raised = i < 4;
            double dx = sign * (raised ? -11.74 : 10.23) - cameraX;
            double dy = sign * (raised ? FAR_CELL_TAG_Y_INCHES[i] : AUDIENCE_CELL_TAG_Y_INCHES[i - 4]) - cameraY;
            double dz = (raised ? 48.99 : 36.3) - CAMERA_UP_INCHES;
            double forward = dx * Math.cos(heading) + dy * Math.sin(heading);
            double right = dx * Math.sin(heading) - dy * Math.cos(heading);
            double lensForward = forward * Math.cos(tilt) + dz * Math.sin(tilt);
            double lensUp = -forward * Math.sin(tilt) + dz * Math.cos(tilt);
            double facing = (sign * 0.5 * dx + 0.866 * dz) / Math.sqrt(dx * dx + dy * dy + dz * dz);
            boolean inPicture = lensForward > 0
                    && Math.abs(Math.toDegrees(Math.atan2(right, lensForward))) <= 27.25
                    && Math.abs(Math.toDegrees(Math.atan2(lensUp, lensForward))) <= 21.0;
            if (inPicture && Math.toDegrees(Math.acos(facing)) <= 60.0) {
                seen.add(SeenAprilTag.at(30 + (blue ? 8 : 0) + i, right, lensForward, lensUp));
            }
        }
        return viewOf(seen);
    }

    private static void assertFiredFrom(Outcome outcome, double xInches, double yInches, double headingDegrees) {
        assertNotNull(outcome.firstShot(), "the robot never fired; it stopped in stage " + outcome.stage());
        Pose shot = outcome.firstShot();
        assertEquals(xInches, shot.xInches(), POSITION_TOLERANCE_INCHES, "x: " + shot);
        assertEquals(yInches, shot.yInches(), POSITION_TOLERANCE_INCHES, "y: " + shot);
        double headingError = Math.IEEEremainder(Math.toDegrees(shot.headingRadians()) - headingDegrees, 360.0);
        assertEquals(0.0, headingError, HEADING_TOLERANCE_DEGREES, "heading: " + shot);
    }

    /** Written for Red; Blue's numbers are rotated first. 9 in is half of the robot's placeholder size. */
    private static void assertParkedWithoutHittingAnything(Outcome outcome, boolean blue) {
        double sign = blue ? -1 : 1;
        double x = sign * outcome.end().xInches();
        double y = sign * outcome.end().yInches();
        assertTrue(x - 9 < -23.9 && x + 9 > -46.6 && y - 9 < -59.1, "in the LOADING ZONE: " + outcome.end());
        assertEquals("DONE", outcome.stage());
        assertEquals(0, outcome.wallHits(), "wall hits");
        assertEquals(0, outcome.obstacleHits(), "obstacle hits");
    }

    /** WHAT: Red fires along x into the opening of the CELL raised at the start, then parks. */
    @Test
    void redFiresIntoTheRaisedCellsOpeningAndParks() {
        Outcome outcome = run(new HiveAutoRed(), "HiveAutoRed", false, false, "HiveAuto-Red");

        assertFiredFrom(outcome, LAUNCH_X_INCHES, CELL_Y_INCHES, 180.0);
        assertParkedWithoutHittingAnything(outcome, false);
    }

    /** WHAT: Blue does the same as Red, rotated 180 degrees around the field's center. */
    @Test
    void blueFiresIntoTheRaisedCellsOpeningAndParks() {
        Outcome outcome = run(new HiveAutoBlue(), "HiveAutoBlue", true, false, "HiveAuto-Blue");

        assertFiredFrom(outcome, -LAUNCH_X_INCHES, -CELL_Y_INCHES, 0.0);
        assertParkedWithoutHittingAnything(outcome, true);
    }

    /** WHAT: With the HIVE tipped, Red sees that the other CELL is raised and fires from that side. */
    @Test
    void redFiresAtTheOtherCellWhenTheHiveHasTipped() {
        Outcome outcome = run(new HiveAutoRed(), "HiveAutoRed", false, true, null);

        assertTrue(outcome.shot().contains("start-lowered"), outcome.shot());
        assertFiredFrom(outcome, -LAUNCH_X_INCHES, CELL_Y_INCHES, 0.0);
        assertParkedWithoutHittingAnything(outcome, false);
    }

    /** WHAT: With the HIVE tipped, Blue also fires from the other side. */
    @Test
    void blueFiresAtTheOtherCellWhenTheHiveHasTipped() {
        Outcome outcome = run(new HiveAutoBlue(), "HiveAutoBlue", true, true, null);

        assertTrue(outcome.shot().contains("start-lowered"), outcome.shot());
        assertFiredFrom(outcome, LAUNCH_X_INCHES, -CELL_Y_INCHES, 180.0);
        assertParkedWithoutHittingAnything(outcome, true);
    }
}
