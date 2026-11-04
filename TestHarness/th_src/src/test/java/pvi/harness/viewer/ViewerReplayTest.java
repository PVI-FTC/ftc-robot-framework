package pvi.harness.viewer;

import org.firstinspires.ftc.teamcode.opmodes.autonomous.TeamAAutoOpMode;
import org.firstinspires.ftc.teamcode.opmodes.teleop.RumbleDemoTeleOp;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.Color;
import java.awt.HeadlessException;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.Optional;

import pvi.harness.hardware.FakeHardwareMap;
import pvi.harness.hardware.RobotConfigs;
import pvi.harness.physics.Pose;
import pvi.harness.runtime.MatchPeriod;
import pvi.harness.runtime.OpModeRunner;
import pvi.harness.runtime.RunRecorder;
import pvi.harness.runtime.RunRecording;
import pvi.harness.runtime.RunRecordingJson;
import pvi.harness.runtime.RunResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Position estimator Steps 13 and 14: the viewer window's side panel and playback.
 *
 * <p>The window itself needs a screen, so these tests check the parts it is built from, all of
 * which work headless: the playback timing ({@link Playback}), the side panel text
 * ({@link SidePanelText}), the field panel ({@link FieldPanel}) and choosing a file
 * ({@link RecordingViewer}). To see the real window, run {@code .\gradlew.bat viewRecording}.</p>
 */
class ViewerReplayTest {

    /** Team A's autonomous, 1 s INIT and 2 s of match, recorded. */
    private static RunRecording teamAAutonomous() {
        OpModeRunner runner = new OpModeRunner(new TeamAAutoOpMode(), RobotConfigs.teamA());
        RunRecorder recorder = RunRecorder.attachTo(runner);
        runner.run(1.0, 2.0).assertNoException();
        return recorder.recording();
    }

    /** First loop index at or after {@code matchSeconds} of match time, once PLAY has happened. */
    private static int indexAtMatchTime(RunRecording recording, double matchSeconds) {
        List<RunRecording.LoopRecord> loops = recording.loops();
        for (int i = 0; i < loops.size(); i++) {
            RunRecording.LoopRecord loop = loops.get(i);
            boolean afterPlay = loop.stage() == RunResult.Stage.LOOP || loop.stage() == RunResult.Stage.STOP;
            if (afterPlay && loop.matchTimeSeconds() >= matchSeconds - 1e-9) {
                return i;
            }
        }
        throw new AssertionError("no loop at " + matchSeconds + " s");
    }

    // ---- Step 14: playback ----------------------------------------------------------------------

    /**
     * WHAT: Playing at 1x moves through the run at real speed.
     *
     * <p>HOW: The recording has one loop every 20 ms. Starting at the beginning, playing, and
     * letting 0.5 s of real time pass (as 10 ticks of 50 ms) must land on the loop 0.5 s after
     * INIT: entry 25.</p>
     *
     * <p>WHY: Replays must show the run at its real pace, or a fast move would look slow.</p>
     */
    @Test
    void playingAtNormalSpeedFollowsRealTime() {
        Playback playback = new Playback(teamAAutonomous());

        playback.play();
        for (int i = 0; i < 10; i++) {
            playback.advance(0.050);
        }

        assertEquals(25, playback.index());
        assertTrue(playback.isPlaying());
    }

    /**
     * WHAT: At 2x the same real time covers twice the run; paused, nothing moves.
     */
    @Test
    void speedScalesPlaybackAndPauseStopsIt() {
        Playback playback = new Playback(teamAAutonomous());
        playback.faster();
        assertEquals(2.0, playback.speed());

        playback.play();
        playback.advance(0.5);
        assertEquals(50, playback.index(), "0.5 s at 2x = 1 s of run = 50 loops");

        playback.pause();
        assertFalse(playback.advance(1.0));
        assertEquals(50, playback.index());
    }

    /**
     * WHAT: Playback stops at the end, and pressing play again starts over.
     *
     * <p>WHY: A replay that ran past the end would crash; one that couldn't restart would be
     * annoying to watch twice.</p>
     */
    @Test
    void playbackStopsAtTheEndAndPlayStartsOver() {
        Playback playback = new Playback(teamAAutonomous());

        playback.play();
        playback.advance(100.0);
        assertTrue(playback.atEnd());
        assertFalse(playback.isPlaying(), "stops by itself at the end");

        playback.play();
        assertEquals(0, playback.index(), "play at the end starts from the beginning");
        assertTrue(playback.isPlaying());
    }

    /**
     * WHAT: Stepping and jumping pause, move the right amount, and stay inside the recording.
     *
     * <p>HOW: One step forward is the next loop. A 1 s jump is 50 loops (20 ms each). Steps
     * past either end stop at the first or last loop. The speed can't go past the list.</p>
     */
    @Test
    void stepsAndJumpsMoveTheRightAmountAndStayInside() {
        Playback playback = new Playback(teamAAutonomous());
        playback.play();

        playback.step(1);
        assertEquals(1, playback.index());
        assertFalse(playback.isPlaying(), "stepping pauses");

        playback.jumpSeconds(1.0);
        assertEquals(51, playback.index());

        playback.step(-1000);
        assertEquals(0, playback.index());
        playback.step(1_000_000);
        assertEquals(playback.size() - 1, playback.index());

        for (int i = 0; i < 10; i++) {
            playback.faster();
        }
        assertEquals(4.0, playback.speed());
        for (int i = 0; i < 10; i++) {
            playback.slower();
        }
        assertEquals(0.25, playback.speed());
    }

    // ---- Step 13: the side panel -------------------------------------------------------------

    /**
     * WHAT: During the drive step, the side panel shows the OpMode, period, stage, time, step,
     * motor powers and that moment's Driver Station screen.
     *
     * <p>HOW: Picks the loop at 0.40 s of match time (inside TimedDrive) and checks the lines.</p>
     *
     * <p>WHY: This is what a student reads to understand what the robot was doing at that
     * moment; each part must match the recording.</p>
     */
    @Test
    void sidePanelShowsTheChosenMoment() {
        RunRecording recording = teamAAutonomous();
        RecordingView view = new RecordingView(recording);
        int index = indexAtMatchTime(recording, 0.40);

        List<String> lines = SidePanelText.describe(view, index);

        assertEquals("TeamAAutoOpMode   AUTONOMOUS   robot: teamA", lines.get(0));
        assertTrue(lines.get(1).startsWith("Stage: LOOP"), lines.get(1));
        assertTrue(lines.get(2).startsWith("Match time: 0.40 s   (since INIT 1.40 s)"), lines.get(2));
        assertTrue(lines.contains("Auto step: TimedDrive"), lines.toString());
        assertTrue(lines.contains("  frontLeft      0.20"), lines.toString());
        assertTrue(lines.contains("Driver Station (sent at 0.40 s)"), lines.toString());
        assertTrue(lines.contains("  Auto Step : TimedDrive"), lines.toString());
    }

    /**
     * WHAT: In replay, the panel shows the screen for the selected moment, not the last one.
     *
     * <p>HOW: At the very first entry (INIT) the screen is the INIT message; at the end it is
     * the autonomous status with "Sequence Complete : true".</p>
     *
     * <p>WHY: 05-desktop-viewer.md §7: moving the slider back must show what the Driver
     * Station showed back then.</p>
     */
    @Test
    void sidePanelShowsTheScreenOfThatMomentNotTheLast() {
        RecordingView view = new RecordingView(teamAAutonomous());

        List<String> atInit = SidePanelText.describe(view, 0);
        List<String> atEnd = SidePanelText.describe(view, view.size() - 1);

        assertTrue(atInit.contains("  Status : Team A autonomous initialized"), atInit.toString());
        assertFalse(atInit.contains("  Sequence Complete : true"), atInit.toString());
        assertTrue(atEnd.contains("  Sequence Complete : true"), atEnd.toString());
    }

    /**
     * WHAT: The panel counts rumbles up to the selected moment only.
     *
     * <p>HOW: RumbleDemoTeleOp buzzes the driver at PLAY, 90 s, 105 s and 115 s. At 100 s the
     * panel must say 2 rumbles, the latest being the 2-pulse one at 90 s.</p>
     */
    @Test
    void sidePanelCountsRumblesUpToThatMoment() {
        OpModeRunner runner = new OpModeRunner(new RumbleDemoTeleOp(), RobotConfigs.teamA());
        RunRecorder recorder = RunRecorder.attachTo(runner);
        runner.run(MatchPeriod.TELEOP).assertNoException();
        RunRecording recording = recorder.recording();

        List<String> lines = SidePanelText.describe(new RecordingView(recording), indexAtMatchTime(recording, 100.0));

        assertTrue(lines.stream().anyMatch(l -> l.startsWith("Gamepad 1 rumbles: 2 (latest") && l.contains("2 pulses at 90.00 s")),
                lines.toString());
        assertTrue(lines.contains("Gamepad 2 rumbles: none yet"), lines.toString());
    }

    /**
     * WHAT: Servos are listed, and an unset one says "not set".
     */
    @Test
    void sidePanelListsServos() {
        FakeHardwareMap robot = RobotConfigs.teamA();
        robot.addServo("claw");
        OpModeRunner runner = new OpModeRunner(new TeamAAutoOpMode(), robot);
        RunRecorder recorder = RunRecorder.attachTo(runner);
        runner.run(0.1, 0.1).assertNoException();

        List<String> lines = SidePanelText.describe(new RecordingView(recorder.recording()), 0);

        assertTrue(lines.contains("Servos (position)"), lines.toString());
        assertTrue(lines.contains("  claw         not set"), lines.toString());
    }

    // ---- Step 13: the field panel and the window ---------------------------------------------

    /**
     * WHAT: The field panel fits the field into a wide panel, centered, and draws the robot.
     *
     * <p>HOW: Paints a 1000 x 700 panel into an image. The field must be 700 - 60 = 640 px,
     * centered left to right, and the robot (at the start, at the field center) must be at the
     * middle of the panel.</p>
     *
     * <p>WHY: Resizing the window must keep the field square and in view.</p>
     */
    @Test
    void fieldPanelFitsAndCentersTheField() {
        FieldPanel panel = new FieldPanel(new RecordingView(teamAAutonomous()));
        panel.setSize(1000, 700);
        panel.show(0);
        BufferedImage image = new BufferedImage(1000, 700, BufferedImage.TYPE_INT_RGB);
        panel.paint(image.createGraphics());

        assertEquals(640.0, FieldPanel.coordinatesFor(1000, 700).fieldPixels(), 1e-9);
        // Robot center is (0, 0); look 4 in behind and 4 in to its left (5 px per inch here:
        // 640 / 144 = 4.44 px per inch, so about 18 px), away from the heading arrow.
        assertEquals(FieldPainter.ROBOT_FILL, new Color(image.getRGB(500 - 18, 350 - 18)));
        assertEquals(FieldPainter.BACKGROUND, new Color(image.getRGB(20, 350)), "left of the centered field");
    }

    /**
     * WHAT: Opening the window without a display fails instead of showing anything.
     *
     * <p>WHY: Tests are headless (05-desktop-viewer.md §3); this proves the window can't pop
     * up during {@code gradlew test}.</p>
     */
    @Test
    void windowCannotOpenDuringTests() {
        RunRecording recording = teamAAutonomous();

        assertThrows(HeadlessException.class, () -> new ViewerWindow(recording, "test"));
    }

    /**
     * WHAT: A TeleOp drive (forward, strafe, turn, then a curve, with the operator pressing A
     * then B for the intake) saves a recording worth watching.
     *
     * <p>HOW: Runs the viewer's "demo drive" scenario (Team A's real TeleOp with scripted
     * gamepads) and saves it. The saved file must load back, with the robot getting more than
     * 40 in from the start at some point and ending turned.</p>
     *
     * <p>WHY: Gives everyone a recording that moves, to try the viewer on:
     * {@code .\gradlew.bat viewRecording -Pfile=build\recordings\ViewerReplayTest-teleOpDrive.json}</p>
     */
    @Test
    void teleOpDriveSavesARecordingToWatch() {
        RunRecording recording = PviScenarios.teleOpDemoDrive().perform();

        Path saved = RunRecordingJson.save(recording, RunRecordingJson.fileInDefaultFolder("ViewerReplayTest-teleOpDrive"));
        System.out.println("Recording to watch: " + saved);
        RunRecording loaded = RunRecordingJson.load(saved);

        double farthestInches = 0;
        for (RunRecording.LoopRecord loop : loaded.loops()) {
            farthestInches = Math.max(farthestInches, loop.pose().distanceInchesTo(Pose.ORIGIN));
        }
        Pose end = loaded.loops().get(loaded.loops().size() - 1).pose();
        assertTrue(farthestInches > 40, "should get well away from the start, farthest was " + farthestInches + " in");
        assertTrue(Math.abs(end.headingDegrees()) > 20, "should have turned: " + end);
    }

    /**
     * WHAT: With no file named, the viewer opens the newest recording; with none at all it has
     * nothing to open (and runs a scenario instead); a missing file gives a clear message.
     */
    @Test
    void viewerPicksTheNewestRecordingOrNone(@TempDir Path folder) throws IOException {
        Path older = Files.writeString(folder.resolve("older.json"), "{}");
        Path newer = Files.writeString(folder.resolve("newer.json"), "{}");
        Files.writeString(folder.resolve("notes.txt"), "not a recording");
        Files.setLastModifiedTime(older, FileTime.fromMillis(1_000_000));
        Files.setLastModifiedTime(newer, FileTime.fromMillis(2_000_000));

        assertEquals(Optional.of(newer), RecordingViewer.chooseRecording(new String[0], folder));
        assertEquals(Optional.of(older), RecordingViewer.chooseRecording(new String[] {older.toString()}, folder));
        assertEquals(Optional.empty(), RecordingViewer.chooseRecording(new String[0], folder.resolve("empty")));

        IllegalArgumentException missing = assertThrows(IllegalArgumentException.class,
                () -> RecordingViewer.chooseRecording(new String[] {folder.resolve("nope.json").toString()}, folder));
        assertTrue(missing.getMessage().contains("No recording file"), missing.getMessage());
    }
}
