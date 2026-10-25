package pvi.harness.runtime;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.firstinspires.ftc.teamcode.opmodes.teleop.TeamATeleOp;
import org.junit.jupiter.api.Test;

import pvi.harness.ds.Axis;
import pvi.harness.ds.GamepadScript;
import pvi.harness.hardware.RobotConfigs;
import pvi.harness.physics.GamePiece;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Game pieces in a real run and its recording (format version 4): the header lists every piece
 * at INIT, each loop lists only the pieces that changed, and the file reads back the same.
 */
class GamePiecesRecordingTest {

    /** Team A's TeleOp driving full backward for 1 s from the field center, into its pre-loads. */
    private static RunRecording driveBackIntoThePreloads() {
        GamepadScript driver = new GamepadScript()
                .at(0.0).stick(Axis.LEFT_STICK_Y, 1.0)
                .at(1.0).stick(Axis.LEFT_STICK_Y, 0.0);
        OpModeRunner runner = new OpModeRunner(new TeamATeleOp(), RobotConfigs.teamA()).gamepad1(driver);
        RunRecorder recorder = RunRecorder.attachTo(runner);
        runner.run(0.2, 2.5);
        return recorder.recording();
    }

    /** Every piece at the end: the header's list updated by each loop's changes. */
    private static Map<String, GamePiece> piecesAtTheEnd(RunRecording recording) {
        Map<String, GamePiece> now = new LinkedHashMap<>();
        recording.header().pieces().forEach(p -> now.put(p.id(), p));
        recording.loops().forEach(loop -> loop.pieces().forEach(p -> now.put(p.id(), p)));
        return now;
    }

    /**
     * WHAT: The header lists all 44 BIOBUZZ pieces; driving backward pushes the pre-loads, so
     * some loops list them, and the balls end up behind where they started.
     *
     * <p>WHY: This is what Unity and the viewer replay: the robot pushing the balls.</p>
     */
    @Test
    void drivingBackwardPushesThePreloadsAndTheRecordingShowsIt() {
        RunRecording recording = driveBackIntoThePreloads();

        assertEquals(RunRecording.VERSION, recording.header().version());
        assertEquals(44, recording.header().pieces().size());
        assertTrue(recording.loops().stream().anyMatch(loop -> !loop.pieces().isEmpty()), "some loop lists a moved piece");
        GamePiece start = recording.header().pieces().stream()
                .filter(p -> p.id().equals("pollen-preload-2")).findFirst().orElseThrow();
        GamePiece end = piecesAtTheEnd(recording).get("pollen-preload-2");
        assertTrue(end.xInches() < start.xInches() - 20, "pushed well back: from " + start + " to " + end);
        RunRecording.LoopRecord lastLoop = recording.loops().get(recording.loops().size() - 1);
        assertTrue(end.xInches() < lastLoop.pose().xInches() - 9, "behind the robot's back edge: " + end);
    }

    /**
     * WHAT: A loop lists only the pieces that changed: nothing moves while the robot stands still,
     * and the balls in the FLOWERs and CELLs are never listed.
     *
     * <p>WHY: Listing all 44 pieces every loop would make long recordings much bigger.</p>
     */
    @Test
    void loopsListOnlyChangedPieces() {
        RunRecording recording = driveBackIntoThePreloads();

        assertEquals(List.of(), recording.loops().get(0).pieces(), "nothing moves during INIT");
        for (RunRecording.LoopRecord loop : recording.loops()) {
            for (GamePiece p : loop.pieces()) {
                assertEquals(GamePiece.FIELD, p.state(), "only loose balls move: " + p);
            }
        }
    }

    /**
     * WHAT: Saving and reading back gives the same pieces, and a second identical run gives
     * exactly the same file.
     *
     * <p>WHY: Replays must show exactly the same pushes as the run.</p>
     */
    @Test
    void piecesRoundTripAndRepeatExactly() {
        RunRecording recording = driveBackIntoThePreloads();
        String json = RunRecordingJson.format(recording);
        RunRecording back = RunRecordingJson.parse(json);

        assertEquals(recording.header().pieces().size(), back.header().pieces().size());
        assertEquals(json, RunRecordingJson.format(back), "reads back to the same text");
        assertTrue(json.contains("\"pieces\": [{\"id\": \"pollen-flower-far-1\", \"kind\": \"pollen\", \"state\": \"flower\""),
                "the header's pieces, in the agreed layout");
        assertEquals(json, RunRecordingJson.format(driveBackIntoThePreloads()), "the same run gives the same file");
    }

    /**
     * WHAT: A version 3 file (no pieces) still loads, with no pieces.
     *
     * <p>WHY: Recordings saved before game pieces existed must keep working.</p>
     */
    @Test
    void versionThreeStillLoadsWithoutPieces() {
        String versionThree = RunRecordingJson.format(driveBackIntoThePreloads())
                .replace("\"version\": " + RunRecording.VERSION + ",", "\"version\": 3,")
                .replaceAll(", \"pieces\": \\[[^\\]]*\\]", "");
        assertFalse(versionThree.contains("\"pieces\""), "the test should have removed every pieces field");

        RunRecording old = RunRecordingJson.parse(versionThree);

        assertEquals(List.of(), old.header().pieces());
        assertTrue(old.loops().stream().allMatch(loop -> loop.pieces().isEmpty()));
    }
}
