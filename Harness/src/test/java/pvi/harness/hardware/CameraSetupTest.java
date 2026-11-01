package pvi.harness.hardware;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;

import org.junit.jupiter.api.Test;

import pvi.harness.runtime.HarnessConfig;
import pvi.harness.runtime.OpModeRunner;
import pvi.harness.runtime.RunRecorder;
import pvi.harness.runtime.RunRecording;
import pvi.harness.runtime.RunRecordingJson;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * W19: each webcam's mount and view come from {@code harness.properties}
 * ({@code webcam.<name>.<fact>}, config version 4) and reach the displays as plain data, in the
 * run recording's header (format version 5).
 */
class CameraSetupTest {

    /** The real settings file with one webcam, "frontCam", 8 in right of center and tilted 15 degrees down. */
    private static String withFrontCam() throws IOException {
        StringBuilder keys = new StringBuilder("devices.webcams=frontCam\n");
        String[] values = {"6 | measured | tape", "-8 | measured | tape", "12 | measured | tape",
                "0 | measured | CAD", "-15 | measured | CAD", "0 | measured | CAD",
                "70 | placeholder | spec sheet", "50 | placeholder | spec sheet",
                "1280 | measured | robot code", "720 | measured | robot code", "96 | placeholder | a guess",
                "60 | placeholder | a guess"};
        for (int i = 0; i < values.length; i++) {
            keys.append("webcam.frontCam.").append(CameraSetup.FACT_NAMES.get(i)).append('=').append(values[i]).append('\n');
        }
        keys.append("webcam.frontCam.aprilTags=field\n");
        return Files.readString(Path.of(HarnessConfig.FILE_NAME)).replace("\r\n", "\n")
                .replaceAll("(?m)^webcam\\..*\n", "")
                .replaceAll("(?m)^devices\\.webcams=.*\n", keys.toString());
    }

    private static String problemsIn(String text) {
        return assertThrows(IllegalArgumentException.class, () -> HarnessConfig.fromText(text, "test")).getMessage();
    }

    /**
     * WHAT: A listed webcam's keys become its camera setup: the numbers (a negative "left" is
     * fine), which are measured, and which are still guesses.
     */
    @Test
    void aWebcamsKeysBecomeItsSetup() throws IOException {
        List<CameraSetup> cameras = HarnessConfig.fromText(withFrontCam(), "test").cameras();

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

    /** WHAT: A listed webcam with a missing key is refused, naming the key. */
    @Test
    void aListedWebcamNeedsEveryKey() throws IOException {
        String message = problemsIn(withFrontCam().replaceAll("(?m)^webcam\\.frontCam\\.maxRangeInches=.*\n", ""));

        assertTrue(message.contains("webcam.frontCam.maxRangeInches: missing"), message);
    }

    /** WHAT: Keys for a webcam that isn't in devices.webcams are refused (usually a typo in the name). */
    @Test
    void keysForAnUnlistedWebcamAreRefused() throws IOException {
        String message = problemsIn(withFrontCam().replace("webcam.frontCam.upInches", "webcam.frontcam.upInches"));

        assertTrue(message.contains("webcam.frontcam.upInches: not a harness setting"), message);
    }

    /** WHAT: A field of view of 180 degrees or more is refused with the camera's name. */
    @Test
    void aFieldOfViewMustBeBelow180Degrees() throws IOException {
        String message = problemsIn(withFrontCam().replace("horizontalFovDegrees=70", "horizontalFovDegrees=200"));

        assertTrue(message.contains("frontCam: a field of view must be between 0 and 180 degrees, got 200.0"), message);
    }

    /**
     * WHAT: The recording header lists each camera as plain numbers with the names of the
     * guessed facts, and reads them back the same.
     */
    @Test
    void theRecordingListsEachCamera() throws IOException {
        CameraSetup cam = HarnessConfig.fromText(withFrontCam(), "test").cameras().get(0);
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

    /** The header of a short run on a robot with the "frontCam" webcam (settings: field tags). */
    private static RunRecording.Header headerOfARun(boolean scriptTheWebcam) throws IOException {
        FakeHardwareMap robot = RobotConfigs.robot(HarnessConfig.fromText(withFrontCam(), "test"));
        if (scriptTheWebcam) {
            robot.addWebcam("frontCam").script(new AprilTagScript());
        }
        OpModeRunner runner = new OpModeRunner(new DoNothing(), robot);
        RunRecorder recorder = RunRecorder.attachTo(runner);
        runner.run(0.2, 0.2).assertNoException();
        return recorder.header();
    }

    /**
     * WHAT: The header says how the webcam really got its tags in that run: "field" as the
     * settings file says, or "script" when the test scripted it instead.
     *
     * <p>WHY: Unity tells scripted detections from field ones by this (C23).</p>
     */
    @Test
    void theRecordingSaysWhetherTheWebcamWasScripted() throws IOException {
        assertEquals(CameraSetup.FIELD_TAGS, headerOfARun(false).cameras().get(0).aprilTags());
        assertEquals(CameraSetup.SCRIPTED_TAGS, headerOfARun(true).cameras().get(0).aprilTags());
    }

    /** WHAT: A run on the settings file's robot records that robot's webcams, in the file's order. */
    @Test
    void aRunRecordsTheRobotsWebcams() {
        OpModeRunner runner = new OpModeRunner(new DoNothing(), RobotConfigs.teamA());
        RunRecorder recorder = RunRecorder.attachTo(runner);
        runner.run(0.2, 0.2).assertNoException();

        assertEquals(HarnessConfig.load().cameras(), recorder.header().cameras());
    }
}
