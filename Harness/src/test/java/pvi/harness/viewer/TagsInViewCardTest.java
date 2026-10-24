package pvi.harness.viewer;

import org.firstinspires.ftc.teamcode.opmodes.teleop.TeamATeleOp;
import org.firstinspires.ftc.teamcode.opmodes.testing.TeamAAprilTagVisionTestOpMode;
import org.junit.jupiter.api.Test;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

import pvi.harness.hardware.AprilTagScript;
import pvi.harness.hardware.FakeHardwareMap;
import pvi.harness.hardware.RobotConfigs;
import pvi.harness.hardware.SeenAprilTag;
import pvi.harness.runtime.OpModeRunner;
import pvi.harness.runtime.RunRecorder;
import pvi.harness.runtime.RunRecording;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The side panel's "Tags in view" card: what the fake webcam saw at the moment shown. Drawn
 * off-screen, so no window is needed.
 */
class TagsInViewCardTest {
    private static final String WEBCAM = "logitechVisionWebcam";

    /** Team A's vision test OpMode with tag 22 in view from 0.5 s to 1.5 s after PLAY. */
    private static RunRecording visionRun() {
        FakeHardwareMap hw = RobotConfigs.teamA();
        hw.addWebcam(WEBCAM).script(new AprilTagScript()
                .at(0.5).see(SeenAprilTag.at(22, 2.0, 30.0, 5.0))
                .at(1.5).lose(22));
        OpModeRunner runner = new OpModeRunner(new TeamAAprilTagVisionTestOpMode(), hw);
        RunRecorder recorder = RunRecorder.attachTo(runner);
        runner.init().start();
        runner.loopUntil(2.0);
        return recorder.recording();
    }

    /** The first loop at or after this many seconds after PLAY. */
    private static int indexAt(RecordingView view, double matchSeconds) {
        for (int i = 0; i < view.size(); i++) {
            if (view.loop(i).matchTimeSeconds() >= matchSeconds - 1e-9 && view.loop(i).stage() != null
                    && view.loop(i).stage().name().equals("LOOP")) {
                return i;
            }
        }
        throw new IllegalStateException("no loop at " + matchSeconds + " s");
    }

    /**
     * WHAT: While a tag is in view, the card shows its id, range and bearing (as the SDK's
     * ftcPose would) and the camera; otherwise "none".
     */
    @Test
    void theCardShowsEachTagInViewWithRangeAndBearing() {
        RecordingView view = new RecordingView(visionRun());

        // 2 in right and 30 in ahead: range 30.07 in, bearing -3.81 deg (right is negative).
        assertEquals(List.of("Tag 22    30.1 in   -3.8 deg  " + WEBCAM),
                TelemetryPanel.aprilTagLines(view.loop(indexAt(view, 1.0))));
        assertEquals(List.of("none"), TelemetryPanel.aprilTagLines(view.loop(indexAt(view, 0.2))));
    }

    /**
     * WHAT: The card appears only for runs that ever saw a tag, so other runs' panels are
     * unchanged.
     */
    @Test
    void theCardOnlyAppearsForRunsThatSawATag() {
        OpModeRunner teleOp = new OpModeRunner(new TeamATeleOp(), RobotConfigs.teamA());
        RunRecorder recorder = RunRecorder.attachTo(teleOp);
        teleOp.init().start();
        teleOp.loop(5);

        assertTrue(TelemetryPanel.hasAprilTags(visionRun()));
        assertFalse(TelemetryPanel.hasAprilTags(recorder.recording()));
    }

    /**
     * WHAT: The panel draws the tag card: the same run is taller with its tags than with them
     * removed.
     *
     * <p>WHY: The cards are drawn by hand; this catches a card that is never drawn.</p>
     */
    @Test
    void thePanelDrawsTheTagCard() {
        RunRecording withTags = visionRun();
        RunRecording withoutTags = new RunRecording(withTags.header(),
                withTags.loops().stream().map(loop -> new RunRecording.LoopRecord(loop.sinceInitSeconds(),
                        loop.matchTimeSeconds(), loop.stage(), loop.pose(), loop.motors(), loop.servos(),
                        loop.autoStep(), loop.telemetry(), loop.gamepad1(), loop.gamepad2())).toList(),
                withTags.gamepad1Rumbles(), withTags.gamepad2Rumbles(), withTags.events());

        assertTrue(heightAt(withTags, 1.0) > heightAt(withoutTags, 1.0), "the tag card should add height");
    }

    /** The panel's height when showing the moment this many seconds after PLAY. */
    private static int heightAt(RunRecording recording, double matchSeconds) {
        RecordingView view = new RecordingView(recording);
        TelemetryPanel panel = new TelemetryPanel(view);
        panel.show(indexAt(view, matchSeconds));
        return paint(panel).getHeight();
    }

    /** Paints the panel into an image at its preferred size (twice, so the height settles). */
    private static BufferedImage paint(TelemetryPanel panel) {
        BufferedImage image = null;
        for (int pass = 0; pass < 2; pass++) {
            panel.setSize(TelemetryPanel.WIDTH, panel.getPreferredSize().height);
            image = new BufferedImage(panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            panel.paint(g);
            g.dispose();
        }
        return image;
    }
}
