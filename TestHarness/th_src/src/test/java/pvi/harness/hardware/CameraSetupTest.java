package pvi.harness.hardware;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import pvi.harness.runtime.HarnessConfig;
import pvi.harness.runtime.OpModeRunner;
import pvi.harness.runtime.RobotProfile;
import pvi.harness.runtime.RunRecorder;
import pvi.harness.runtime.RunRecording;
import pvi.harness.runtime.RunRecordingJson;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Each robot's webcams come from its own {@code robots/<id>/camera.properties} (D21, config version
 * 6) and reach the displays as plain data, in the run recording's header (format version 5).
 */
class CameraSetupTest {
    /** The robot these tests make, in a temporary robots folder. */
    private static final String ROBOT_ID = "cam-test";

    @TempDir
    Path robotsFolder;

    /** One webcam, "frontCam", 8 in right of center and tilted 15 degrees down. */
    private static String frontCam() {
        return frontCam("frontCam", -8, -15);
    }

    /** A camera file with one webcam, with this name, left offset and pitch. */
    private static String frontCam(String name, double leftInches, double pitchDegrees) {
        StringBuilder keys = new StringBuilder("devices.webcams=" + name + "\n");
        String[] values = {"6 | measured | tape", leftInches + " | measured | tape", "12 | measured | tape",
                "0 | measured | CAD", pitchDegrees + " | measured | CAD", "0 | measured | CAD",
                "70 | placeholder | spec sheet", "50 | placeholder | spec sheet",
                "1280 | measured | robot code", "720 | measured | robot code", "96 | placeholder | a guess",
                "60 | placeholder | a guess"};
        for (int i = 0; i < values.length; i++) {
            keys.append("webcam.").append(name).append('.').append(CameraSetup.FACT_NAMES.get(i)).append('=')
                    .append(values[i]).append('\n');
        }
        return keys.append("webcam.").append(name).append(".aprilTags=field\n").toString();
    }

    private void cameraFile(String robotId, String text) throws IOException {
        Files.createDirectories(robotsFolder.resolve(robotId));
        Files.writeString(robotsFolder.resolve(robotId).resolve(HarnessConfig.CAMERA_FILE_NAME), text);
    }

    /** The real settings file, for the robot {@link #ROBOT_ID} in the temporary folder. */
    private static String settings() throws IOException {
        return Files.readString(Path.of(HarnessConfig.FILE_NAME)).replace("\r\n", "\n")
                .replaceAll("(?m)^robot\\.id=.*$", "robot.id=" + ROBOT_ID);
    }

    /** The settings with this camera file. */
    private HarnessConfig withCameraFile(String text) throws IOException {
        cameraFile(ROBOT_ID, text);
        return HarnessConfig.fromText(settings(), "test", robotsFolder);
    }

    private String problemsIn(String cameraFileText) {
        return assertThrows(IllegalArgumentException.class, () -> withCameraFile(cameraFileText)).getMessage();
    }

    /**
     * WHAT: A robot's camera file becomes its setups: the numbers (a negative "left" is fine), which
     * are measured, and which are still guesses.
     */
    @Test
    void aRobotsCameraFileBecomesItsSetup() throws IOException {
        List<CameraSetup> cameras = withCameraFile(frontCam()).cameras();

        assertEquals(1, cameras.size());
        CameraSetup cam = cameras.get(0);
        assertEquals("frontCam", cam.name());
        assertEquals(-8.0, cam.leftInches().value(), 1e-9, "8 in right of center");
        assertEquals(-15.0, cam.pitchDegrees().value(), 1e-9, "tilted down");
        assertEquals(1280.0, cam.imageWidthPixels().value(), 1e-9);
        assertEquals(CameraSetup.DEGREES, cam.horizontalFovDegrees().unit());
        assertEquals(CameraSetup.FIELD_TAGS, cam.aprilTags());
        assertEquals(List.of("horizontalFovDegrees", "verticalFovDegrees", "maxRangeInches", "maxViewAngleDegrees"),
                cam.placeholders());
    }

    /** WHAT: No camera file, or an empty devices.webcams, means the robot has no webcam. */
    @Test
    void noCameraFileOrAnEmptyListMeansNoWebcam() throws IOException {
        assertEquals(List.of(), HarnessConfig.fromText(settings(), "test", robotsFolder).cameras(), "no file");
        assertEquals(List.of(), withCameraFile("devices.webcams=\n").cameras(), "empty list");
    }

    /** WHAT: A listed webcam with a missing key is refused, naming the key and the file. */
    @Test
    void aListedWebcamNeedsEveryKey() {
        String message = problemsIn(frontCam().replaceAll("(?m)^webcam\\.frontCam\\.maxRangeInches=.*\n", ""));

        assertTrue(message.contains("webcam.frontCam.maxRangeInches: missing"), message);
        assertTrue(message.contains(HarnessConfig.CAMERA_FILE_NAME), message);
    }

    /** WHAT: A misspelled key in a camera file (or one for an unlisted webcam) is refused, naming the file. */
    @Test
    void aMisspelledKeyInACameraFileIsRefused() {
        String message = problemsIn(frontCam().replace("webcam.frontCam.upInches", "webcam.frontcam.upInches"));

        assertTrue(message.contains("webcam.frontcam.upInches: not a camera setting"), message);
        assertTrue(message.contains(HarnessConfig.CAMERA_FILE_NAME), message);
    }

    /** WHAT: A field of view of 180 degrees or more is refused with the camera's name. */
    @Test
    void aFieldOfViewMustBeBelow180Degrees() {
        String message = problemsIn(frontCam().replace("horizontalFovDegrees=70", "horizontalFovDegrees=200"));

        assertTrue(message.contains("frontCam: a field of view must be between 0 and 180 degrees, got 200.0"), message);
    }

    /**
     * WHAT: Camera keys in the old place (harness.properties or a drive.properties) stop with one
     * line saying where to move them.
     */
    @Test
    void cameraKeysInTheOldPlaceSayWhereToMove() throws IOException {
        String expected = Path.of(ROBOT_ID, HarnessConfig.CAMERA_FILE_NAME).toString();

        String inSettings = assertThrows(IllegalArgumentException.class,
                () -> HarnessConfig.fromText(settings() + frontCam(), "test", robotsFolder)).getMessage();
        assertTrue(inSettings.contains("cameras don't go in this file") && inSettings.contains(expected), inSettings);
        assertEquals(2, inSettings.lines().count(), "one line, not one per key: " + inSettings);

        HarnessConfig main = HarnessConfig.fromText(settings(), "test", robotsFolder);
        Files.createDirectories(robotsFolder.resolve("other"));
        Files.writeString(robotsFolder.resolve("other").resolve(RobotProfile.FILE_NAME), "forOpModes=A\ndevices.webcams=\n");
        String inProfile = assertThrows(IllegalArgumentException.class,
                () -> RobotProfile.all(main, robotsFolder)).getMessage();
        assertTrue(inProfile.contains("cameras don't go in this file")
                && inProfile.contains(Path.of("other", HarnessConfig.CAMERA_FILE_NAME).toString()), inProfile);
    }

    /** WHAT: A robot.id with no folder in robots/ fails at load, so a typo doesn't just mean "no cameras". */
    @Test
    void aRobotIdWithNoFolderIsRefused() throws IOException {
        Path file = robotsFolder.resolve("settings.properties");
        Files.writeString(file, settings().replaceAll("(?m)^robot\\.id=.*$", "robot.id=no-such-robot"));

        String message = assertThrows(IllegalArgumentException.class, () -> HarnessConfig.load(file)).getMessage();

        assertTrue(message.contains("robot.id: there's no folder"), message);
    }

    /**
     * WHAT: The recording header lists each camera as plain numbers with the names of the
     * guessed facts, and reads them back the same.
     */
    @Test
    void theRecordingListsEachCamera() throws IOException {
        CameraSetup cam = withCameraFile(frontCam("frontCam", -8, -15).replace("-8.0", "-8").replace("-15.0", "-15"))
                .cameras().get(0);
        RunRecording.Header header = new RunRecording.Header(RunRecording.VERSION, "inches", "radians", "seconds",
                "teamA", "SomeAuto", null, 0.02, null, null, List.of(), List.of(cam));
        String json = RunRecordingJson.format(new RunRecording(header, List.of(), List.of(), List.of()));

        assertTrue(json.contains("\"cameras\": [{\"name\": \"frontCam\", \"forwardInches\": 6, \"leftInches\": -8,"
                + " \"upInches\": 12, \"yawDegrees\": 0, \"pitchDegrees\": -15, \"rollDegrees\": 0,"
                + " \"horizontalFovDegrees\": 70, \"verticalFovDegrees\": 50, \"imageWidthPixels\": 1280,"
                + " \"imageHeightPixels\": 720, \"maxRangeInches\": 96, \"maxViewAngleDegrees\": 60,"
                + " \"aprilTags\": \"field\", \"placeholders\": [\"horizontalFovDegrees\", \"verticalFovDegrees\","
                + " \"maxRangeInches\", \"maxViewAngleDegrees\"]}]"), json);
        CameraSetup back = RunRecordingJson.parse(json).header().cameras().get(0);
        assertEquals("frontCam", back.name());
        assertEquals(-15.0, back.pitchDegrees().value(), 1e-9);
        assertEquals(cam.placeholders(), back.placeholders());
        assertEquals(CameraSetup.FIELD_TAGS, back.aprilTags());
    }

    /** An OpMode that does nothing, so the test works with any team's robot code. */
    private static final class DoNothing extends OpMode {
        @Override
        public void init() {
        }

        @Override
        public void loop() {
        }
    }

    /** The header of a short run on this fake robot. */
    private static RunRecording.Header headerOfARun(FakeHardwareMap robot) {
        OpModeRunner runner = new OpModeRunner(new DoNothing(), robot);
        RunRecorder recorder = RunRecorder.attachTo(runner);
        runner.run(0.2, 0.2).assertNoException();
        return recorder.header();
    }

    /**
     * WHAT: The header says how the webcam really got its tags in that run: "field" as its camera
     * file says, or "script" when the test scripted it instead.
     *
     * <p>WHY: Unity tells scripted detections from field ones by this (C23).</p>
     */
    @Test
    void theRecordingSaysWhetherTheWebcamWasScripted() throws IOException {
        HarnessConfig config = withCameraFile(frontCam());
        assertEquals(CameraSetup.FIELD_TAGS, headerOfARun(RobotConfigs.robot(config)).cameras().get(0).aprilTags());

        FakeHardwareMap scripted = RobotConfigs.robot(config);
        scripted.addWebcam("frontCam").script(new AprilTagScript());
        assertEquals(CameraSetup.SCRIPTED_TAGS, headerOfARun(scripted).cameras().get(0).aprilTags());
    }

    /** WHAT: A run on the settings file's robot records that robot's webcams, in the file's order. */
    @Test
    void aRunRecordsTheRobotsWebcams() {
        assertEquals(HarnessConfig.load().cameras(), headerOfARun(RobotConfigs.teamA()).cameras());
    }

    /**
     * WHAT: Two robots with different mounts each get their own, on the fake webcam and in the
     * recording's header, and a profile with no camera file has no webcam even though the main
     * robot has one.
     *
     * <p>WHY: The whole point of D21: a camera belongs to one robot, and nothing is inherited.</p>
     */
    @Test
    void eachRobotHasItsOwnCameras() throws IOException {
        HarnessConfig main = withCameraFile(frontCam("mainCam", -8, -15));
        cameraFile("other", frontCam("otherCam", 3, 20));
        for (String id : List.of("other", "none")) {
            Files.createDirectories(robotsFolder.resolve(id));
            Files.writeString(robotsFolder.resolve(id).resolve(RobotProfile.FILE_NAME), "forOpModes=" + id + "\n");
        }
        List<RobotProfile> profiles = RobotProfile.all(main, robotsFolder);
        FakeHardwareMap none = RobotConfigs.robot(profiles.get(0).config());
        FakeHardwareMap other = RobotConfigs.robot(profiles.get(1).config());

        assertEquals(-15.0, headerOfARun(RobotConfigs.robot(main)).cameras().get(0).pitchDegrees().value(), 1e-9);
        assertEquals("mainCam", headerOfARun(RobotConfigs.robot(main)).cameras().get(0).name());

        assertEquals(20.0, headerOfARun(other).cameras().get(0).pitchDegrees().value(), 1e-9);
        assertEquals("otherCam", other.cameraSetups().get(0).name());
        assertEquals(3.0, other.cameraSetups().get(0).leftInches().value(), 1e-9);
        assertNull(other.webcam("mainCam"), "not the main robot's webcam");

        assertEquals(List.of(), headerOfARun(none).cameras());
        assertNull(none.webcam("mainCam"), "no camera file, no webcam, though the main robot has one");
    }
}
