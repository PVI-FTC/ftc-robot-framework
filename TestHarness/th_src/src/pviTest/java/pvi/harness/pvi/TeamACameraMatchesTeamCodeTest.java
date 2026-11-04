package pvi.harness.pvi;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import pvi.harness.hardware.CameraSetup;
import pvi.harness.physics.ModelValue;
import pvi.harness.runtime.HarnessConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PVI extra (built only with {@code pvi.extras=true}): Team A's webcam in {@code robots/team-a/camera.properties}
 * ({@code webcam.logitechVisionWebcam.*}) compared with the mount and picture size in Team A's
 * code.
 *
 * <p>The numbers are private constants in {@code TeamAAprilTagVisionRobot}, which no harness
 * file can read, so this test reads them from the source file. TeamCode's frame is +x right,
 * +y forward, +z up; the harness's is forward, left, up, so "right" becomes a negative "left".
 * TeamCode's mount angles 0, 0, 0 mean level and facing forward, as in the harness.</p>
 *
 * <p>{@code camera.properties} owns the camera's mount (the user's decision, 2026-11-01: for
 * example tilted down to see the bottom of the HIVE while TeamCode still says level). So a mount
 * number that differs from TeamCode is reported, not a failure, as long as its source doesn't
 * still claim to be copied from TeamCode. The picture size stays strict: robot code asks the
 * camera for that size, so any other size would describe a picture robot code never gets.</p>
 */
class TeamACameraMatchesTeamCodeTest {
    private static final String WEBCAM = "logitechVisionWebcam";
    private static final Path ROBOT_FILE = Path.of("org/firstinspires/ftc/teamcode/robots/teamA/TeamAAprilTagVisionRobot.java");
    private static final Path OPMODE_FILE =
            Path.of("org/firstinspires/ftc/teamcode/opmodes/testing/TeamAAprilTagVisionTestOpMode.java");
    private static final String FIX = " Update webcam." + WEBCAM + ".* in robots/team-a/camera.properties to match "
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
                .orElseThrow(() -> new AssertionError("robots/team-a/camera.properties has no webcam " + WEBCAM));
    }

    /**
     * The config's mount number next to TeamCode's: null if they agree, otherwise a note. A number
     * that differs must not have a source that still says it came from TeamCode.
     */
    static String differenceNote(String fact, double teamCode, ModelValue config) {
        if (Math.abs(teamCode - config.value()) < 1e-9) {
            return null;
        }
        assertFalse(config.source().startsWith("TeamCode"), "webcam." + WEBCAM + "." + fact + " is " + config.value()
                + " but TeamCode says " + teamCode + ", while its source still says it was copied from TeamCode."
                + " Either copy TeamCode's number or say where this one came from.");
        return fact + ": camera.properties " + config.value() + " (" + config.source() + "), TeamCode " + teamCode;
    }

    /**
     * WHAT: The webcam name in camera.properties is the one Team A's vision OpMode asks for.
     */
    @Test
    void theWebcamNameIsTheOneTheOpModeUses() throws IOException {
        assertTrue(source(OPMODE_FILE).contains("WEBCAM_HARDWARE_NAME = \"" + WEBCAM + "\""),
                "TeamAAprilTagVisionTestOpMode no longer uses " + WEBCAM + "." + FIX);
        camera();
    }

    /**
     * WHAT: Each mount number (forward, left, up, yaw, pitch, roll) either equals TeamCode's
     * (CAMERA_FORWARD_INCHES, minus CAMERA_RIGHT_INCHES, CAMERA_UP_INCHES and the angles it passes)
     * or differs on purpose: then it is printed as a note, and its source must not claim TeamCode.
     */
    @Test
    void theMountFollowsTeamCodeOrSaysWhereItCameFrom() throws IOException {
        String robot = source(ROBOT_FILE);
        CameraSetup cam = camera();
        Matcher angles = Pattern.compile("CAMERA_UP_INCHES,\\s*(-?[0-9.]+),\\s*(-?[0-9.]+),\\s*(-?[0-9.]+)\\)")
                .matcher(robot);
        assertTrue(angles.find(), "the mount angles after CAMERA_UP_INCHES not found in " + ROBOT_FILE);

        List<String> notes = new ArrayList<>();
        notes.add(differenceNote("forwardInches", constant(robot, "CAMERA_FORWARD_INCHES"), cam.forwardInches()));
        notes.add(differenceNote("leftInches", -constant(robot, "CAMERA_RIGHT_INCHES"), cam.leftInches()));
        notes.add(differenceNote("upInches", constant(robot, "CAMERA_UP_INCHES"), cam.upInches()));
        notes.add(differenceNote("yawDegrees", Double.parseDouble(angles.group(1)), cam.yawDegrees()));
        notes.add(differenceNote("pitchDegrees", Double.parseDouble(angles.group(2)), cam.pitchDegrees()));
        notes.add(differenceNote("rollDegrees", Double.parseDouble(angles.group(3)), cam.rollDegrees()));
        notes.removeIf(note -> note == null);
        if (!notes.isEmpty()) {
            System.out.println("NOTE: Team A's webcam mount in camera.properties differs from TeamCode on purpose:\n  "
                    + String.join("\n  ", notes));
        }
    }

    /**
     * WHAT: A mount number chosen in camera.properties is only reported, but one that differs
     * from TeamCode while its source still claims TeamCode is refused.
     */
    @Test
    void aChosenMountNumberIsReportedButAWrongCopyIsRefused() {
        assertNull(differenceNote("pitchDegrees", 0, ModelValue.measured(0, CameraSetup.DEGREES, "TeamCode")));
        String note = differenceNote("pitchDegrees", 0,
                ModelValue.placeholder(-15, CameraSetup.DEGREES, "chosen in camera.properties to see the HIVE"));
        assertTrue(note.contains("camera.properties -15.0") && note.contains("TeamCode 0.0"), note);
        assertThrows(AssertionError.class, () -> differenceNote("pitchDegrees", 0,
                ModelValue.measured(-15, CameraSetup.DEGREES, "TeamCode TeamAAprilTagVisionRobot mount angles")));
    }

    /**
     * WHAT: Team A's webcam sees the field's AprilTags by itself ({@code aprilTags=field}), so
     * live drive, the viewer and Unity show real tags. Tests that want a script still choose one.
     */
    @Test
    void theWebcamSeesTheFieldsTagsByDefault() {
        assertEquals(CameraSetup.FIELD_TAGS, camera().aprilTags(), "the user asked for tags to appear automatically");
    }

    /**
     * WHAT: The picture size equals TeamCode's STREAM_WIDTH and STREAM_HEIGHT (strict: robot code
     * asks the camera for that size).
     */
    @Test
    void thePictureSizeMatchesTeamCode() throws IOException {
        String robot = source(ROBOT_FILE);
        CameraSetup cam = camera();

        assertEquals(constant(robot, "STREAM_WIDTH"), cam.imageWidthPixels().value(), 1e-9, "width." + FIX);
        assertEquals(constant(robot, "STREAM_HEIGHT"), cam.imageHeightPixels().value(), 1e-9, "height." + FIX);
    }
}
