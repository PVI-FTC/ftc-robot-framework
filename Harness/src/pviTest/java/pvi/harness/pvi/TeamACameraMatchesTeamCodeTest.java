package pvi.harness.pvi;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import pvi.harness.hardware.CameraSetup;
import pvi.harness.runtime.HarnessConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PVI extra (built only with {@code pvi.extras=true}): Team A's webcam in {@code harness.properties}
 * ({@code webcam.logitechVisionWebcam.*}) still matches the mount and picture size in Team A's
 * code.
 *
 * <p>The numbers are private constants in {@code TeamAAprilTagVisionRobot}, which no harness
 * file can read, so this test reads them from the source file. TeamCode's frame is +x right,
 * +y forward, +z up; the harness's is forward, left, up, so "right" becomes a negative "left".
 * TeamCode's mount angles 0, 0, 0 mean level and facing forward, as in the harness.</p>
 */
class TeamACameraMatchesTeamCodeTest {
    private static final String WEBCAM = "logitechVisionWebcam";
    private static final Path ROBOT_FILE = Path.of("org/firstinspires/ftc/teamcode/robots/teamA/TeamAAprilTagVisionRobot.java");
    private static final Path OPMODE_FILE =
            Path.of("org/firstinspires/ftc/teamcode/opmodes/testing/TeamAAprilTagVisionTestOpMode.java");
    private static final String FIX = " Update webcam." + WEBCAM + ".* in harness.properties to match "
            + "TeamAAprilTagVisionRobot.";

    private static String source(Path file) throws IOException {
        return Files.readString(Path.of(HarnessConfig.load().teamCodeSourceDir()).resolve(file));
    }

    /** The number after {@code NAME =} in the source. */
    private static double constant(String source, String name) {
        Matcher m = Pattern.compile("\\b" + name + "\\s*=\\s*(-?[0-9.]+)\\s*;").matcher(source);
        assertTrue(m.find(), name + " not found in " + ROBOT_FILE);
        return Double.parseDouble(m.group(1));
    }

    private static CameraSetup camera() {
        return HarnessConfig.load().cameras().stream().filter(c -> c.name().equals(WEBCAM)).findFirst()
                .orElseThrow(() -> new AssertionError("harness.properties has no webcam " + WEBCAM));
    }

    /**
     * WHAT: The webcam name in harness.properties is the one Team A's vision OpMode asks for.
     */
    @Test
    void theWebcamNameIsTheOneTheOpModeUses() throws IOException {
        assertTrue(source(OPMODE_FILE).contains("WEBCAM_HARDWARE_NAME = \"" + WEBCAM + "\""),
                "TeamAAprilTagVisionTestOpMode no longer uses " + WEBCAM + "." + FIX);
        camera();
    }

    /**
     * WHAT: forward, left and up, and the three mount angles, equal TeamCode's CAMERA_FORWARD_INCHES,
     * minus CAMERA_RIGHT_INCHES, CAMERA_UP_INCHES and the angles it passes (0, 0, 0).
     */
    @Test
    void theMountMatchesTeamCode() throws IOException {
        String robot = source(ROBOT_FILE);
        CameraSetup cam = camera();

        assertEquals(constant(robot, "CAMERA_FORWARD_INCHES"), cam.forwardInches().value(), 1e-9, "forward." + FIX);
        assertEquals(-constant(robot, "CAMERA_RIGHT_INCHES"), cam.leftInches().value(), 1e-9, "left = -right." + FIX);
        assertEquals(constant(robot, "CAMERA_UP_INCHES"), cam.upInches().value(), 1e-9, "up." + FIX);
        Matcher angles = Pattern.compile("CAMERA_UP_INCHES,\\s*(-?[0-9.]+),\\s*(-?[0-9.]+),\\s*(-?[0-9.]+)\\)")
                .matcher(robot);
        assertTrue(angles.find(), "the mount angles after CAMERA_UP_INCHES not found in " + ROBOT_FILE);
        assertEquals(Double.parseDouble(angles.group(1)), cam.yawDegrees().value(), 1e-9, "yaw." + FIX);
        assertEquals(Double.parseDouble(angles.group(2)), cam.pitchDegrees().value(), 1e-9, "pitch." + FIX);
        assertEquals(Double.parseDouble(angles.group(3)), cam.rollDegrees().value(), 1e-9, "roll." + FIX);
        assertTrue(cam.forwardInches().measured() && cam.leftInches().measured() && cam.upInches().measured(),
                "the mount is copied from TeamCode, so it counts as known");
    }

    /**
     * WHAT: Team A's webcam sees the field's AprilTags by itself ({@code aprilTags=field}), so
     * live drive, the viewer and Unity show real tags. Tests that want a script still choose one.
     */
    @Test
    void theWebcamSeesTheFieldsTagsByDefault() {
        assertEquals(CameraSetup.FIELD_TAGS, camera().aprilTags(), "the user asked for tags to appear automatically");
    }

    /** WHAT: The picture size equals TeamCode's STREAM_WIDTH and STREAM_HEIGHT. */
    @Test
    void thePictureSizeMatchesTeamCode() throws IOException {
        String robot = source(ROBOT_FILE);
        CameraSetup cam = camera();

        assertEquals(constant(robot, "STREAM_WIDTH"), cam.imageWidthPixels().value(), 1e-9, "width." + FIX);
        assertEquals(constant(robot, "STREAM_HEIGHT"), cam.imageHeightPixels().value(), 1e-9, "height." + FIX);
    }
}
