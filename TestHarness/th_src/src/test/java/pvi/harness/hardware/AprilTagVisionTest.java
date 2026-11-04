package pvi.harness.hardware;

import org.firstinspires.ftc.teamcode.opmodes.testing.TeamAAprilTagVisionTestOpMode;
import org.firstinspires.ftc.vision.VisionPortal;
import org.firstinspires.ftc.vision.apriltag.AprilTagGameDatabase;
import org.firstinspires.ftc.vision.apriltag.AprilTagMetadata;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import pvi.harness.runtime.OpModeRunner;
import pvi.harness.runtime.RunRecorder;
import pvi.harness.runtime.RunRecording;
import pvi.harness.runtime.RunRecordingJson;
import pvi.harness.runtime.RunResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 4: scripted AprilTags. A {@link FakeWebcam} "sees" the tags an {@link AprilTagScript}
 * says, and the real, unchanged Team A vision code (VisionPortal, AprilTagProcessor,
 * VisionHardware, the VisionSubsystem state machine and TeamAAprilTagVisionTestOpMode)
 * reacts as if a camera saw them.
 */
class AprilTagVisionTest {
    /** The webcam name TeamAAprilTagVisionTestOpMode looks up (a private constant there). */
    private static final String WEBCAM = "logitechVisionWebcam";

    /** Team A's camera mount, from TeamAAprilTagVisionRobot: right, forward and up from the robot center. */
    private static final double MOUNT_RIGHT_INCHES = 6.875;
    private static final double MOUNT_FORWARD_INCHES = 4.875;
    private static final double MOUNT_UP_INCHES = 19.0;

    /** Team A's robot with the vision webcam, running the vision test OpMode. */
    private static OpModeRunner visionRunner(AprilTagScript tags) {
        FakeHardwareMap hw = RobotConfigs.teamA();
        hw.addWebcam(WEBCAM).script(tags);
        return new OpModeRunner(new TeamAAprilTagVisionTestOpMode(), hw);
    }

    /** One moment of the vision state machine, as the Driver Station showed it. */
    private record StateAt(double matchSeconds, String state) {
    }

    /** Runs to {@code untilSeconds} and returns every change of "Vision State" after PLAY. */
    private static List<StateAt> stateChanges(OpModeRunner runner, double untilSeconds) {
        List<StateAt> changes = new ArrayList<>();
        runner.everyLoop(r -> {
            if (r.stage() != RunResult.Stage.LOOP) {
                return;
            }
            String state = r.telemetry().lastValue("Vision State");
            if (changes.isEmpty() || !changes.get(changes.size() - 1).state().equals(state)) {
                changes.add(new StateAt(r.clock().sinceStart(), state));
            }
        });
        runner.init().start();
        runner.loopUntil(untilSeconds);
        return changes;
    }

    // ---- The script on its own ---------------------------------------------------------------

    /**
     * WHAT: A tag stays in view from the moment the script sees it until it loses it.
     *
     * <p>WHY: That is the rule every vision test relies on.</p>
     */
    @Test
    void aTagStaysInViewUntilTheScriptLosesIt() {
        AprilTagScript tags = new AprilTagScript().at(1.5).see(21).at(3.0).lose(21);

        assertEquals(List.of(), tags.tagsInView(10.0, 1.4));
        assertEquals(List.of(SeenAprilTag.straightAhead(21)), tags.tagsInView(10.0, 1.5));
        assertEquals(List.of(SeenAprilTag.straightAhead(21)), tags.tagsInView(10.0, 2.99));
        assertEquals(List.of(), tags.tagsInView(10.0, 3.0));
        // Before PLAY, a script written for after PLAY shows nothing.
        assertEquals(List.of(), tags.tagsInView(10.0, -1.0));
    }

    /**
     * WHAT: Tags scripted with duringInit() are in view before PLAY, and stay in view after it.
     */
    @Test
    void tagsSeenDuringInitAreInViewBeforeAndAfterPlay() {
        AprilTagScript tags = new AprilTagScript().duringInit().at(0.5).see(20).afterStart().at(1.0).see(24);

        assertEquals(List.of(SeenAprilTag.straightAhead(20)), tags.tagsInView(0.6, -1.0));
        assertEquals(List.of(SeenAprilTag.straightAhead(20), SeenAprilTag.straightAhead(24)),
                tags.tagsInView(5.0, 1.0));
    }

    /**
     * WHAT: The webcam takes 30 pictures a second, so a 20 ms loop sometimes gets no new picture.
     *
     * <p>WHY: getFreshDetections() then returns null on the robot too; robot code must cope.</p>
     */
    @Test
    void theWebcamTakesThirtyPicturesASecondOfSimulatedTime() {
        OpModeRunner runner = visionRunner(new AprilTagScript());
        runner.init().start();
        runner.loopUntil(1.0);

        FakeWebcam camera = runner.hardwareMap().webcam(WEBCAM);
        assertTrue(camera.isInUse(), "the vision OpMode should have built a VisionPortal with the webcam");
        // About 1.04 s since INIT, starting one frame after INIT: 30 or 31 pictures.
        assertTrue(camera.framesTaken() >= 29 && camera.framesTaken() <= 31,
                "expected about 30 pictures in a second, got " + camera.framesTaken());
    }

    /**
     * WHAT: The harness's own VisionPortal is the one robot code uses, not the SDK's.
     *
     * <p>WHY: The SDK's version needs Android and a real camera. If the class path order ever
     * changed, vision tests would quietly see "camera unavailable" instead of the script.</p>
     */
    @Test
    void robotCodeGetsTheHarnessVisionPortal() {
        String location = VisionPortal.class.getProtectionDomain().getCodeSource().getLocation().toString();
        assertTrue(!location.contains("Vision-12"), "VisionPortal was loaded from the SDK jar: " + location);
    }

    // ---- The real Team A vision code ---------------------------------------------------------

    /**
     * WHAT: With the webcam in the hardware map, Team A's vision test OpMode finds a camera and
     * starts searching after PLAY.
     */
    @Test
    void withTheWebcamVisionIsAvailableAndSearching() {
        OpModeRunner runner = visionRunner(new AprilTagScript());
        runner.init().start();
        runner.loopUntil(0.5);

        assertEquals("true", runner.telemetry().lastValue("Vision Available"));
        assertEquals("Searching", runner.telemetry().lastValue("Vision State"));
        assertEquals("0", runner.telemetry().lastValue("Detection Count"));
    }

    /**
     * WHAT: Without the webcam, the OpMode still runs, says vision is unavailable and stays
     * disabled (TeamCode treats the camera as optional).
     */
    @Test
    void withoutTheWebcamVisionIsUnavailableAndDisabled() {
        // The config robot has Team A's webcam (its camera file), so take it off for this test.
        OpModeRunner runner = new OpModeRunner(new TeamAAprilTagVisionTestOpMode(),
                RobotConfigs.teamA().without(WEBCAM));
        runner.init().start();
        runner.loopUntil(0.5);

        assertEquals("false", runner.telemetry().lastValue("Vision Available"));
        assertEquals("VisionDisabled", runner.telemetry().lastValue("Vision State"));
    }

    /**
     * WHAT: The vision state machine follows a tag that is seen at 1.5 s and lost at 3 s:
     * Searching, TargetAcquired (one loop), Tracking, LostTarget (one loop), Searching.
     *
     * <p>HOW: Records every change of "Vision State" on the Driver Station screen.</p>
     *
     * <p>WHY: This is VisionSubsystem's whole job, tested without a camera.</p>
     */
    @Test
    void visionStateMachineFollowsATagThatIsSeenThenLost() {
        OpModeRunner runner = visionRunner(new AprilTagScript().at(1.5).see(21).at(3.0).lose(21));

        List<StateAt> changes = stateChanges(runner, 4.0);

        List<String> states = changes.stream().map(StateAt::state).toList();
        assertEquals(List.of("Searching", "TargetAcquired", "Tracking", "LostTarget", "Searching"), states,
                "state changes: " + changes);
        // The first picture after 1.5 s (at most a frame plus a loop later) shows the tag; the
        // state machine moves on the loop after that.
        assertBetween(1.5, 1.5 + 0.1, changes.get(1).matchSeconds(), "TargetAcquired starts");
        assertEquals(0.02, changes.get(2).matchSeconds() - changes.get(1).matchSeconds(), 1e-6,
                "TargetAcquired lasts exactly one loop");
        assertBetween(3.0, 3.0 + 0.1, changes.get(3).matchSeconds(), "LostTarget starts");
        assertEquals(0.02, changes.get(4).matchSeconds() - changes.get(3).matchSeconds(), 1e-6,
                "LostTarget lasts exactly one loop");
    }

    /**
     * WHAT: While a tag is in view, the OpMode shows its id and one detection; after it is
     * lost, none.
     */
    @Test
    void theOpModeShowsTheTagIdWhileTheTagIsInView() {
        OpModeRunner runner = visionRunner(new AprilTagScript().at(1.0).see(23).at(2.0).lose(23));
        runner.init().start();

        runner.loopUntil(1.5);
        assertEquals("1", runner.telemetry().lastValue("Detection Count"));
        assertEquals("23", runner.telemetry().lastValue("Tag ID"));

        runner.loopUntil(2.5);
        assertEquals("0", runner.telemetry().lastValue("Detection Count"));
    }

    /**
     * WHAT: For tag 22 (the one Team A verified), the OpMode shows the camera position the
     * script gave and the robot position worked out with Team A's camera mount.
     *
     * <p>HOW: Tag 22 is 2 in right, 30 in ahead and 5 in above the lens. The robot position
     * should be those plus the mount: 6.875 right, 4.875 forward, 19 up.</p>
     *
     * <p>WHY: This checks TeamCode's camera-to-robot conversion end to end.</p>
     */
    @Test
    void tag22GivesTheCameraAndRobotPositionsThroughTeamAsMount() {
        OpModeRunner runner = visionRunner(new AprilTagScript().at(0.5).see(SeenAprilTag.at(22, 2.0, 30.0, 5.0)));
        runner.init().start();
        runner.loopUntil(1.0);

        assertEquals("true", runner.telemetry().lastValue("Pose Available"));
        assertEquals("2.00, 30.00, 5.00 in", runner.telemetry().lastValue("Camera XYZ"));
        assertEquals(String.format("%.2f, %.2f, %.2f in", 2.0 + MOUNT_RIGHT_INCHES,
                        30.0 + MOUNT_FORWARD_INCHES, 5.0 + MOUNT_UP_INCHES),
                runner.telemetry().lastValue("Robot XYZ"));
        // Range and bearing as the SDK reports them: along the floor, left is positive.
        assertEquals(String.format("%.2f in / %.2f deg", Math.hypot(2.0, 30.0), Math.toDegrees(Math.atan2(-2.0, 30.0))),
                runner.telemetry().lastValue("Camera Range / Bearing"));
    }

    /**
     * WHAT: A tag Team A hasn't verified (21) is detected, but without a pose, and the OpMode
     * says why.
     */
    @Test
    void anUnverifiedTagIsDetectedWithoutAPose() {
        OpModeRunner runner = visionRunner(new AprilTagScript().at(0.5).see(21));
        runner.init().start();
        runner.loopUntil(1.0);

        assertEquals("21", runner.telemetry().lastValue("Tag ID"));
        assertEquals("false", runner.telemetry().lastValue("Pose Available"));
        assertTrue(runner.telemetry().lastValue("Quality").contains("only DECODE tag 22 is verified"),
                runner.telemetry().lastValue("Quality"));
    }

    /**
     * WHAT: Seeing a goal tag (20) gives an accepted field position for the robot, the same
     * distance from the tag as the camera saw it.
     *
     * <p>HOW: Tag 20 is scripted 2 in right, 60 in ahead and 10.5 in above the lens. Whatever
     * way the robot is turned, its center must then be sqrt(8.875² + 64.875² + 29.5²) inches
     * from tag 20's field position (from the SDK's DECODE tag library), and on the floor.</p>
     *
     * <p>WHY: This runs the SDK's own field-pose math and TeamCode's candidate checks together.</p>
     */
    @Test
    void aGoalTagGivesAnAcceptedFieldPositionTheRightDistanceFromTheTag() {
        OpModeRunner runner = visionRunner(new AprilTagScript().at(0.5).see(SeenAprilTag.at(20, 2.0, 60.0, 10.5)));
        runner.init().start();
        runner.loopUntil(1.0);

        assertEquals("true", runner.telemetry().lastValue("Field Candidate Available"),
                runner.telemetry().lastValue("Field Candidate Status"));
        double[] robot = parseThreeNumbers(runner.telemetry().lastValue("Field Candidate XYZ"));
        AprilTagMetadata tag20 = AprilTagGameDatabase.getDecodeTagLibrary().lookupTag(20);
        double distance = Math.sqrt(square(robot[0] - tag20.fieldPosition.get(0))
                + square(robot[1] - tag20.fieldPosition.get(1))
                + square(robot[2] - tag20.fieldPosition.get(2)));
        double expected = Math.sqrt(square(2.0 + MOUNT_RIGHT_INCHES) + square(60.0 + MOUNT_FORWARD_INCHES)
                + square(10.5 + MOUNT_UP_INCHES));
        assertEquals(expected, distance, 0.05, "robot at " + runner.telemetry().lastValue("Field Candidate XYZ"));
        assertEquals(0.0, robot[2], 0.05, "the robot's center should be on the floor");
    }

    /**
     * WHAT: An obelisk tag (21) is not a field-position tag, so no field position is accepted.
     */
    @Test
    void anObeliskTagGivesNoFieldPosition() {
        OpModeRunner runner = visionRunner(new AprilTagScript().at(0.5).see(21));
        runner.init().start();
        runner.loopUntil(1.0);

        assertEquals("false", runner.telemetry().lastValue("Field Candidate Available"));
        assertEquals("Rejected: tag is not a configured fixed-field localization tag.",
                runner.telemetry().lastValue("Field Candidate Status"));
    }

    // ---- Tags in the run recording (format v3) ------------------------------------------------

    /** Records a vision run: tag 22 in view from 0.5 s to 1.5 s after PLAY. */
    private static RunRecording recordTag22() {
        OpModeRunner runner = visionRunner(new AprilTagScript()
                .at(0.5).see(SeenAprilTag.at(22, 2.0, 30.0, 5.0))
                .at(1.5).lose(22));
        RunRecorder recorder = RunRecorder.attachTo(runner);
        runner.init().start();
        runner.loopUntil(2.0);
        return recorder.recording();
    }

    /**
     * WHAT: A scripted vision run with AprilTags in view is saved as
     * {@code build/recordings/scripted-apriltag-vision.json}, with the tags in it.
     *
     * <p>The tags are DECODE's (20, 24) placed relative to the camera, not on the BIOBUZZ field,
     * so the name starts with "scripted-": it checks saving, and isn't one to watch in Unity
     * (that's {@code vision-field-demo.json}, from {@code TagsInViewCardTest}).</p>
     *
     * <p>HOW: Tag 20 is in view 0.5-2.0 s after PLAY, tag 24 joins at 1.0 s, both are lost at
     * 2.5 s; the run lasts 3 s. The test checks the saved file has the tags.</p>
     */
    @Test
    void aVisionRunIsSavedForTheViewerAndUnity() throws java.io.IOException {
        OpModeRunner runner = visionRunner(new AprilTagScript()
                .at(0.5).see(SeenAprilTag.at(20, -6.0, 36.0, 4.0))
                .at(1.0).see(SeenAprilTag.at(24, 8.0, 48.0, 4.0))
                .at(2.0).lose(20)
                .at(2.5).loseAll());
        RunRecorder recorder = RunRecorder.attachTo(runner);
        runner.init().start();
        runner.loopUntil(3.0);

        java.nio.file.Path saved = recorder.save("scripted-apriltag-vision");

        String json = java.nio.file.Files.readString(saved);
        assertTrue(json.contains("\"aprilTags\""), "the recording should hold the tags in view");
        assertTrue(json.contains("\"id\": 24") || json.contains("\"id\":24"), "tag 24 should be in the recording");
    }

    /** The recorded loop closest to this many seconds after PLAY. */
    private static RunRecording.LoopRecord loopAt(RunRecording recording, double matchSeconds) {
        RunRecording.LoopRecord closest = null;
        for (RunRecording.LoopRecord loop : recording.loops()) {
            if (loop.stage() == RunResult.Stage.LOOP && (closest == null
                    || Math.abs(loop.matchTimeSeconds() - matchSeconds) < Math.abs(closest.matchTimeSeconds() - matchSeconds))) {
                closest = loop;
            }
        }
        return closest;
    }

    /**
     * WHAT: The run recording saves the tags in view each loop, with the camera and where the
     * tag is from the lens; none before the tag comes into view or after it is lost.
     *
     * <p>WHY: The viewer and Unity show what the camera saw at each moment of a replay.</p>
     */
    @Test
    void theRecordingSavesTheTagsInViewEachLoop() {
        RunRecording recording = recordTag22();

        assertEquals(List.of(), loopAt(recording, 0.3).aprilTags());
        assertEquals(List.of(new RunRecording.AprilTagState(WEBCAM, 22, 2.0, 30.0, 5.0, 0.0, 0.0, 0.0)),
                loopAt(recording, 1.0).aprilTags());
        assertEquals(List.of(), loopAt(recording, 1.8).aprilTags());
    }

    /** WHAT: Tags in view survive saving as JSON and loading again, exactly. */
    @Test
    void tagsInViewSurviveSavingAndLoading() {
        RunRecording recording = recordTag22();

        RunRecording loaded = RunRecordingJson.parse(RunRecordingJson.format(recording));

        assertEquals(recording.loops().stream().map(RunRecording.LoopRecord::aprilTags).toList(),
                loaded.loops().stream().map(RunRecording.LoopRecord::aprilTags).toList());
    }

    /**
     * WHAT: Version 2 recordings (saved before tags were recorded) still load, with no tags.
     */
    @Test
    void versionTwoRecordingsStillLoadWithNoTags() {
        String versionTwo = RunRecordingJson.format(recordTag22())
                .replace("\"version\": " + RunRecording.VERSION + ",", "\"version\": 2,")
                .replaceAll(", \"aprilTags\": \\[[^\\]]*\\]", "");
        // (The cameras list's "aprilTags": "script" setting is a different field and may stay.)
        assertTrue(!versionTwo.contains("\"aprilTags\": ["), "the test should have removed every aprilTags list");

        RunRecording old = RunRecordingJson.parse(versionTwo);

        assertTrue(old.loops().stream().allMatch(loop -> loop.aprilTags().isEmpty()));
    }

    // ---- Small helpers -----------------------------------------------------------------------

    private static void assertBetween(double low, double high, double actual, String what) {
        assertTrue(actual >= low - 1e-9 && actual <= high + 1e-9,
                what + " at " + actual + " s, expected between " + low + " and " + high);
    }

    private static double square(double value) {
        return value * value;
    }

    /** Reads "1.00, -2.50, 3.00 in" into three numbers. */
    private static double[] parseThreeNumbers(String text) {
        String[] parts = text.replace(" in", "").split(",");
        return new double[] {
                Double.parseDouble(parts[0].trim()),
                Double.parseDouble(parts[1].trim()),
                Double.parseDouble(parts[2].trim()),
        };
    }
}
