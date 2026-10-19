package pvi.harness.viewer;

import org.firstinspires.ftc.teamcode.opmodes.teleop.TeamATeleOp;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.imageio.ImageIO;

import pvi.harness.ds.Axis;
import pvi.harness.ds.Button;
import pvi.harness.ds.GamepadScript;
import pvi.harness.hardware.RobotConfigs;
import pvi.harness.runtime.OpModeRunner;
import pvi.harness.runtime.RunRecorder;
import pvi.harness.runtime.RunRecording;
import pvi.harness.runtime.RunRecordingJson;
import pvi.harness.runtime.RunResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The viewer's gamepad picture: the driver's and operator's controllers, with held buttons lit.
 *
 * <p>The recording tests check that gamepad input is recorded and read back; the drawing tests
 * check single pixels at known button positions (their centers, where edge smoothing can't
 * change the color).</p>
 */
class GamepadIndicatorTest {

    /** A gamepad state with only these buttons held and everything else at rest. */
    private static RunRecording.GamepadState holding(Button... buttons) {
        return new RunRecording.GamepadState(List.of(buttons), 0, 0, 0, 0, 0, 0);
    }

    /** Draws one controller at full size (no scaling) at the image's top-left corner. */
    private static BufferedImage drawController(RunRecording.GamepadState pad) {
        BufferedImage image = new BufferedImage((int) GamepadIndicator.CONTROLLER_WIDTH,
                (int) GamepadIndicator.CONTROLLER_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        GamepadIndicator.paintController(g, 0, 0, pad, "Driver");
        g.dispose();
        return image;
    }

    /** The color at a point given in controller design pixels. */
    private static Color colorAt(BufferedImage image, Point2D p) {
        return new Color(image.getRGB((int) Math.floor(p.getX()), (int) Math.floor(p.getY())));
    }

    /** Team A's TeleOp with the driver holding A from 1.0 to 1.5 s and the left stick forward. */
    private static RunRecording driverHoldsAWhileDriving() {
        GamepadScript driver = new GamepadScript()
                .at(0.8).stick(Axis.LEFT_STICK_Y, -1.0)
                .at(1.0).hold(Button.A, 0.5)
                .at(2.0).releaseAll();
        OpModeRunner runner = new OpModeRunner(new TeamATeleOp(), RobotConfigs.teamA()).gamepad1(driver);
        RunRecorder recorder = RunRecorder.attachTo(runner);
        runner.run(0.2, 2.5).assertNoException();
        return recorder.recording();
    }

    /** The first loop at or after {@code matchSeconds} of match time. */
    private static RunRecording.LoopRecord loopAt(RunRecording recording, double matchSeconds) {
        return recording.loops().stream()
                .filter(l -> l.stage() == RunResult.Stage.LOOP && l.matchTimeSeconds() >= matchSeconds - 1e-9)
                .findFirst().orElseThrow();
    }

    // ---- Recording the gamepads ---------------------------------------------------------------

    /**
     * WHAT: Each loop records which buttons the driver held and where the sticks were.
     *
     * <p>HOW: The driver pushes the left stick forward from 0.8 s and holds A from 1.0 to 1.5 s.
     * At 1.2 s gamepad 1 shows A held and the stick at -1 (up is negative); at 0.5 s and 1.8 s
     * nothing is held; the operator's gamepad is idle throughout. The recording must also read
     * back from JSON unchanged.</p>
     *
     * <p>WHY: The viewer's controller picture is drawn from this. It must match what the
     * OpMode saw in that loop.</p>
     */
    @Test
    void recordingKeepsWhatTheDriverPressedEachLoop() {
        RunRecording recording = driverHoldsAWhileDriving();

        RunRecording.LoopRecord holdingA = loopAt(recording, 1.2);
        assertEquals(List.of(Button.A), holdingA.gamepad1().buttons());
        assertEquals(-1.0, holdingA.gamepad1().leftStickY(), 1e-9);
        assertTrue(loopAt(recording, 0.5).gamepad1().buttons().isEmpty());
        assertTrue(loopAt(recording, 1.8).gamepad1().buttons().isEmpty(), "A released at 1.5 s");
        assertTrue(recording.loops().stream().allMatch(l -> l.gamepad2().isIdle()), "operator never touched");

        String json = RunRecordingJson.format(recording);
        assertEquals(json, RunRecordingJson.format(RunRecordingJson.parse(json)));
    }

    // ---- Drawing the controller -----------------------------------------------------------------

    /**
     * WHAT: A held face button lights up in its own color; the others stay dim.
     *
     * <p>HOW: With A held, the A button's center is bright green and B's center is dark red.</p>
     */
    @Test
    void heldFaceButtonLightsUpInItsColor() {
        BufferedImage image = drawController(holding(Button.A));

        assertEquals(GamepadIndicator.A_GREEN, colorAt(image, GamepadIndicator.center(Button.A)));
        assertEquals(GamepadIndicator.dim(GamepadIndicator.B_RED), colorAt(image, GamepadIndicator.center(Button.B)));
    }

    /**
     * WHAT: Other held buttons (bumpers, D-pad, Start...) light orange; unheld ones stay gray.
     */
    @Test
    void heldBumperAndDpadLightOrange() {
        BufferedImage image = drawController(holding(Button.LEFT_BUMPER, Button.DPAD_UP));

        assertEquals(GamepadIndicator.LIT, colorAt(image, GamepadIndicator.center(Button.LEFT_BUMPER)));
        assertEquals(GamepadIndicator.LIT, colorAt(image, GamepadIndicator.center(Button.DPAD_UP)));
        assertEquals(GamepadIndicator.UNPRESSED, colorAt(image, GamepadIndicator.center(Button.RIGHT_BUMPER)));
        assertEquals(GamepadIndicator.UNPRESSED, colorAt(image, GamepadIndicator.center(Button.DPAD_DOWN)));
    }

    /**
     * WHAT: A stick's knob moves the way the stick is pushed, and lights up.
     *
     * <p>HOW: Left stick fully up (y = -1). The knob, 7 px above the base center, is lit; the
     * spot 7 px below the center (where "down" would put it) shows only the dark base.</p>
     */
    @Test
    void stickKnobMovesTheWayTheStickIsPushed() {
        RunRecording.GamepadState up = new RunRecording.GamepadState(List.of(), 0, -1, 0, 0, 0, 0);
        BufferedImage image = drawController(up);

        Point2D base = GamepadIndicator.center(Button.LEFT_STICK_BUTTON);
        assertEquals(GamepadIndicator.LIT, colorAt(image, GamepadIndicator.knobCenter(Button.LEFT_STICK_BUTTON, 0, -1)));
        assertEquals(GamepadIndicator.STICK_BASE, colorAt(image, new Point2D.Double(base.getX(), base.getY() + 7)));
    }

    /**
     * WHAT: A trigger fills up as far as it is pressed.
     *
     * <p>HOW: Right trigger at 0.5: the left part of its bar is lit, the right part isn't.</p>
     */
    @Test
    void triggerFillsAsFarAsItIsPressed() {
        RunRecording.GamepadState halfTrigger = new RunRecording.GamepadState(List.of(), 0, 0, 0, 0, 0, 0.5);
        BufferedImage image = drawController(halfTrigger);

        // Right trigger bar: x 134 to 170, y 18 to 28.
        assertEquals(GamepadIndicator.LIT, colorAt(image, new Point2D.Double(140, 23)));
        assertEquals(GamepadIndicator.UNPRESSED, colorAt(image, new Point2D.Double(165, 23)));
    }

    /**
     * WHAT: In the viewer's bottom-right panel, the driver's controller (on the left) lights A
     * while A is held in the replay.
     *
     * <p>HOW: Paints the gamepad panel at its normal size at 1.2 s, while A is held. The two
     * controllers fill the panel inside an 8 px border, driver first, so the driver's A button
     * is at (8 + 145, 8 + 78). The picture is saved to
     * build/field-images/GamepadIndicatorTest-panel.png to look at.</p>
     */
    @Test
    void gamepadPanelShowsTheDriversHeldButton() throws IOException {
        RunRecording recording = driverHoldsAWhileDriving();
        GamepadPanel panel = new GamepadPanel(new RecordingView(recording));
        panel.setSize(panel.getPreferredSize());
        panel.show(recording.loops().indexOf(loopAt(recording, 1.2)));
        BufferedImage image = new BufferedImage(panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_RGB);
        panel.paint(image.createGraphics());
        Path png = Path.of("build", "field-images", "GamepadIndicatorTest-panel.png").toAbsolutePath();
        Files.createDirectories(png.getParent());
        ImageIO.write(image, "png", png.toFile());
        System.out.println("Panel picture saved to " + png);

        Point2D a = GamepadIndicator.center(Button.A);
        Point2D operatorA = new Point2D.Double(a.getX() + GamepadIndicator.CONTROLLER_WIDTH, a.getY());
        int inset = GamepadPanel.INSET_PIXELS;
        assertEquals(GamepadIndicator.A_GREEN, new Color(image.getRGB((int) (inset + a.getX()), (int) (inset + a.getY()))));
        assertEquals(GamepadIndicator.dim(GamepadIndicator.A_GREEN),
                new Color(image.getRGB((int) (inset + operatorA.getX()), (int) (inset + operatorA.getY()))),
                "the operator's A is not held");
    }
}
