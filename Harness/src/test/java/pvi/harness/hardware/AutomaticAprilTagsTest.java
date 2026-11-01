package pvi.harness.hardware;

import org.firstinspires.ftc.teamcode.opmodes.testing.TeamAAprilTagVisionTestOpMode;
import org.junit.jupiter.api.Test;

import java.util.List;

import pvi.harness.physics.DriveModels;
import pvi.harness.physics.Pose;
import pvi.harness.physics.PoseEstimator;
import pvi.harness.physics.WheelReader;
import pvi.harness.runtime.OpModeRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The fake webcam's automatic mode (T14): it works out which of the field's AprilTags are in
 * view from where the robot really is, instead of a script saying so.
 *
 * <p>Most checks use DECODE's tag 20 (the blue goal tag), whose field position and orientation
 * come from the SDK, from the switched-off DECODE field file; Team 2's vision code uses the DECODE
 * tag library, so it can turn that tag into a field position. The BIOBUZZ check uses this
 * season's field file.</p>
 */
class AutomaticAprilTagsTest {
    /** Team 2's camera mount (TeamAAprilTagVisionRobot / VisionPortalAprilTagSource). */
    private static final double MOUNT_RIGHT = 6.875;
    private static final double MOUNT_FORWARD = 4.875;
    private static final double MOUNT_UP = 19.0;
    private static final String WEBCAM = "logitechVisionWebcam";
    private static final CameraView CAMERA = CameraView.lookingForward(MOUNT_RIGHT, MOUNT_FORWARD, MOUNT_UP);

    private static final List<FieldAprilTags.Tag> DECODE = FieldAprilTags.forField("decode-2025");

    private static FieldAprilTags.Tag tag(List<FieldAprilTags.Tag> tags, int id) {
        return tags.stream().filter(t -> t.id() == id).findFirst().orElseThrow();
    }

    /**
     * The robot pose that puts the lens {@code distance} inches straight out in front of tag 20
     * (level, on the tag's facing line), plus {@code sideways} inches to the robot's right,
     * looking at the tag (or {@code extraTurnDegrees} turned from that).
     */
    private static Pose inFrontOfTag20(double distance, double sideways, double extraTurnDegrees) {
        FieldAprilTags.Tag t = tag(DECODE, 20);
        double heading = Math.atan2(-t.facingY(), -t.facingX());   // looking back along its facing
        double lensX = t.xInches() + t.facingX() * distance;
        double lensY = t.yInches() + t.facingY() * distance;
        // Robot axes for that heading: right = (sin, -cos), forward = (cos, sin). Moving the
        // robot to its right puts the facing line further to the lens's left.
        double right = MOUNT_RIGHT - sideways;
        double x = lensX - right * Math.sin(heading) - MOUNT_FORWARD * Math.cos(heading);
        double y = lensY + right * Math.cos(heading) - MOUNT_FORWARD * Math.sin(heading);
        return new Pose(x, y, Math.IEEEremainder(heading + Math.toRadians(extraTurnDegrees), 2 * Math.PI));
    }

    // ---- Geometry ---------------------------------------------------------------------------

    /**
     * WHAT: Facing tag 20 from 48 in away, the camera sees it, with the right id and range and a
     * bearing of 0 (straight ahead), squarely (yaw 0).
     */
    @Test
    void aTagStraightAheadIsSeenWithTheRightRangeAndBearing() {
        List<SeenAprilTag> seen = CAMERA.tagsInView(inFrontOfTag20(48, 0, 0), DECODE);

        assertEquals(List.of(20), seen.stream().map(SeenAprilTag::id).toList());
        SeenAprilTag tag20 = seen.get(0);
        assertEquals(48.0, tag20.rangeInches(), 0.01);
        assertEquals(0.0, tag20.bearingDegrees(), 0.01);
        assertEquals(29.5 - MOUNT_UP, tag20.upInches(), 0.01);
        assertEquals(0.0, tag20.yawDegrees(), 0.01);
    }

    /**
     * WHAT: With the robot 10 in to the right, the tag is 10 in to the camera's left: bearing
     * positive (left), as the SDK's ftcPose defines it, and the tag turned by the same angle.
     */
    @Test
    void aTagToTheLeftHasAPositiveBearing() {
        SeenAprilTag tag20 = CAMERA.tagsInView(inFrontOfTag20(48, 10, 0), DECODE).get(0);

        assertEquals(-10.0, tag20.rightInches(), 0.01);
        assertEquals(Math.toDegrees(Math.atan2(10, 48)), tag20.bearingDegrees(), 0.01);
    }

    /** WHAT: Turned 180 degrees away from the tag, the camera doesn't see it. */
    @Test
    void aTagBehindTheCameraIsNotSeen() {
        assertEquals(List.of(), CAMERA.tagsInView(inFrontOfTag20(48, 0, 180), DECODE));
    }

    /**
     * WHAT: From 80 in, turned 40 degrees (more than half the 70 degree wide picture), the tag is
     * off the side of the screen; turned 30 degrees, it is still in view. (80 in keeps it well
     * inside the picture's height, so only the width decides.)
     */
    @Test
    void aTagOutsideTheFieldOfViewIsNotSeen() {
        assertEquals(List.of(), CAMERA.tagsInView(inFrontOfTag20(80, 0, 40), DECODE));
        assertEquals(1, CAMERA.tagsInView(inFrontOfTag20(80, 0, 30), DECODE).size());
    }

    /** WHAT: 150 in away (more than the 120 in range), the tag isn't read. */
    @Test
    void aTagTooFarAwayIsNotSeen() {
        assertEquals(List.of(), CAMERA.tagsInView(inFrontOfTag20(150, 0, 0), DECODE));
    }

    /**
     * WHAT: Looking at the tag from behind (its back faces the camera), it isn't read, even
     * though it is in front of the lens and in the field of view.
     */
    @Test
    void aTagSeenFromBehindIsNotSeen() {
        FieldAprilTags.Tag t = tag(DECODE, 20);
        // The lens 48 in behind the tag, looking along the tag's facing direction (at its back).
        double heading = Math.atan2(t.facingY(), t.facingX());
        double lensX = t.xInches() - t.facingX() * 48;
        double lensY = t.yInches() - t.facingY() * 48;
        Pose robot = new Pose(lensX - MOUNT_RIGHT * Math.sin(heading) - MOUNT_FORWARD * Math.cos(heading),
                lensY + MOUNT_RIGHT * Math.cos(heading) - MOUNT_FORWARD * Math.sin(heading), heading);

        assertEquals(List.of(), CAMERA.tagsInView(robot, List.of(t)));
    }

    /**
     * WHAT: BIOBUZZ's field file has its 16 HIVE tags; from 50 in on the scoring side of the
     * HIVE, looking at it, the camera sees the blue AUDIENCE tags that face it (38 first), and
     * never the red tags, whose printed sides face the other way.
     */
    @Test
    void biobuzzHiveTagsAreSeenOnlyFromTheSideTheyFace() {
        List<FieldAprilTags.Tag> biobuzz = FieldAprilTags.forField("biobuzz-2026");
        assertEquals(16, biobuzz.size());

        // Robot facing +x, its lens about 50 in short of the blue AUDIENCE tags (x = 10.23), the
        // camera tilted 20 degrees up (pitch -70) so the red SCORING tags, high up 30 in ahead,
        // are inside the picture too: only the way they face hides them.
        CameraView tiltedUp = new CameraView(MOUNT_RIGHT, MOUNT_FORWARD, MOUNT_UP, 0, -70, 0,
                CameraView.DEFAULT_HORIZONTAL_FOV_DEGREES, CameraView.DEFAULT_VERTICAL_FOV_DEGREES,
                CameraView.DEFAULT_MAX_RANGE_INCHES, CameraView.DEFAULT_MAX_VIEW_ANGLE_DEGREES);
        List<Integer> seen = tiltedUp.tagsInView(new Pose(-45, 3, 0), biobuzz).stream().map(SeenAprilTag::id).toList();

        assertTrue(seen.contains(38), "seen: " + seen);
        assertFalse(seen.stream().anyMatch(id -> id >= 30 && id <= 37), "the red tags face away; seen: " + seen);
    }

    /**
     * WHAT: The webcam described in {@code harness.properties} (forward/left/up, "0, 0, 0 =
     * looking forward") sees tag 20 exactly as the SDK-style mount from Team 2's TeamCode does.
     *
     * <p>WHY: The two conventions are converted in one place ({@code CameraView.from}); a sign
     * or a 90 degree slip there would make every automatic picture wrong.</p>
     */
    @Test
    void theSettingsFileWebcamSeesWhatTeamTwosMountSees() {
        CameraSetup setup = pvi.harness.runtime.HarnessConfig.load().cameras().stream()
                .filter(c -> c.name().equals(WEBCAM)).findFirst().orElseThrow();
        Pose robot = inFrontOfTag20(48, 10, 5);

        SeenAprilTag fromSettings = CameraView.from(setup).tagsInView(robot, DECODE).get(0);
        SeenAprilTag fromTeamCode = CAMERA.tagsInView(robot, DECODE).get(0);

        assertEquals(fromTeamCode.rightInches(), fromSettings.rightInches(), 1e-6);
        assertEquals(fromTeamCode.forwardInches(), fromSettings.forwardInches(), 1e-6);
        assertEquals(fromTeamCode.upInches(), fromSettings.upInches(), 1e-6);
        assertEquals(fromTeamCode.yawDegrees(), fromSettings.yawDegrees(), 1e-4);
    }

    // ---- With Team 2's real vision OpMode ---------------------------------------------------

    /**
     * WHAT: Team 2's AprilTag Vision OpMode, with the webcam in automatic mode and the robot
     * standing 40 in in front of tag 20, works out a field position (through the SDK's own pose
     * maths) that is the robot's true position.
     *
     * <p>WHY: This is the point of automatic mode: robot code's localisation is tested against
     * where the robot really is, with nobody scripting what the camera sees. (Closer than about
     * 35 in, the tag is above the top of the picture; the BIOBUZZ HIVE can push the robot a few
     * inches, so the check uses where it really ends
     * up either way.)</p>
     */
    @Test
    void teamTwosVisionOpModeFindsTheTrueFieldPosition() {
        Pose start = inFrontOfTag20(40, 0, 0);
        FakeHardwareMap robot = RobotConfigs.teamA();
        robot.addWebcam(WEBCAM).seeFieldTags(DECODE, CAMERA);
        OpModeRunner runner = new OpModeRunner(new TeamAAprilTagVisionTestOpMode(), robot);
        PoseEstimator estimator = PoseEstimator.attachTo(runner, DriveModels.teamA(), WheelReader.teamA(), start);

        runner.init().start();
        runner.loopUntil(1.0);
        Pose truth = estimator.pose();

        assertEquals("true", runner.telemetry().lastValue("Field Candidate Available"),
                runner.telemetry().lastValue("Field Candidate Status"));
        String[] xyz = runner.telemetry().lastValue("Field Candidate XYZ").replace(" in", "").split(",");
        assertEquals(truth.xInches(), Double.parseDouble(xyz[0].trim()), 0.1, "x");
        assertEquals(truth.yInches(), Double.parseDouble(xyz[1].trim()), 0.1, "y");
        assertEquals(0.0, Double.parseDouble(xyz[2].trim()), 0.1, "z (the robot's centre is on the floor)");
        // The SDK's robot yaw is 0 when the robot faces field +y; the harness heading is 0 facing +x.
        String[] pry = runner.telemetry().lastValue("Field Candidate PRY").replace(" deg", "").split(",");
        double expectedYaw = Math.toDegrees(Math.IEEEremainder(truth.headingRadians() - Math.PI / 2, 2 * Math.PI));
        assertEquals(expectedYaw, Double.parseDouble(pry[2].trim()), 0.1, "yaw");
    }

    /** WHAT: Turned away from every tag, the same OpMode finds no field position. */
    @Test
    void turnedAwayTheVisionOpModeFindsNoFieldPosition() {
        FakeHardwareMap robot = RobotConfigs.teamA();
        robot.addWebcam(WEBCAM).seeFieldTags(DECODE, CAMERA);
        OpModeRunner runner = new OpModeRunner(new TeamAAprilTagVisionTestOpMode(), robot);
        PoseEstimator.attachTo(runner, DriveModels.teamA(), WheelReader.teamA(), inFrontOfTag20(48, 0, 180));

        runner.init().start();
        runner.loopUntil(1.0);

        // With no tag in the picture the OpMode shows a count of 0 and no field candidate at all.
        assertEquals("0", runner.telemetry().lastValue("Detection Count"));
        assertNotEquals("true", runner.telemetry().lastValue("Field Candidate Available"));
    }
}
