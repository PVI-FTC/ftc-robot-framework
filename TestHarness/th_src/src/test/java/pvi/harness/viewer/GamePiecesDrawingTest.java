package pvi.harness.viewer;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

import org.firstinspires.ftc.teamcode.opmodes.teleop.TeamATeleOp;
import org.junit.jupiter.api.Test;

import pvi.harness.ds.Axis;
import pvi.harness.ds.GamepadScript;
import pvi.harness.hardware.RobotConfigs;
import pvi.harness.physics.GamePiece;
import pvi.harness.physics.Pose;
import pvi.harness.runtime.OpModeRunner;
import pvi.harness.runtime.RunRecorder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * The game pieces in the desktop viewer: the view works out every piece at every moment from
 * the recording's changes, and the field drawing shows loose balls in their colors.
 */
class GamePiecesDrawingTest {
    /** 20 pixels per inch. */
    private final FieldCoordinates coords = new FieldCoordinates(2880, 30);

    /**
     * WHAT: At the last moment of a run, the view's pieces are exactly where the simulation left
     * them; at the first, where they started.
     *
     * <p>WHY: The recording only lists changed pieces, so the viewer has to add them up correctly.</p>
     */
    @Test
    void theViewKnowsEveryPieceAtEveryMoment() {
        GamepadScript driver = new GamepadScript()
                .at(0.0).stick(Axis.LEFT_STICK_Y, 1.0)
                .at(1.0).stick(Axis.LEFT_STICK_Y, 0.0);
        OpModeRunner runner = new OpModeRunner(new TeamATeleOp(), RobotConfigs.teamA()).gamepad1(driver);
        RunRecorder recorder = RunRecorder.attachTo(runner);
        runner.run(0.2, 2.5);
        RecordingView view = new RecordingView(recorder.recording());

        assertEquals(recorder.recording().header().pieces(), view.pieces(0));
        List<GamePiece> simulated = runner.positionEstimator().pieces().snapshot();
        List<GamePiece> shown = view.pieces(view.size() - 1);
        assertEquals(simulated.size(), shown.size());
        for (int i = 0; i < shown.size(); i++) {
            assertEquals(simulated.get(i).id(), shown.get(i).id());
            assertEquals(simulated.get(i).xInches(), shown.get(i).xInches(), 0.011, simulated.get(i).id());
            assertEquals(simulated.get(i).yInches(), shown.get(i).yInches(), 0.011, simulated.get(i).id());
        }
    }

    /**
     * WHAT: A loose POLLEN ball is drawn yellow and a loose red NECTAR ball red, where they are.
     *
     * <p>WHY: You can follow the balls the robot pushes in the viewer, not only in Unity.</p>
     */
    @Test
    void looseBallsAreDrawnInTheirColors() {
        List<GamePiece> pieces = List.of(
                new GamePiece("p", GamePiece.POLLEN, GamePiece.FIELD, 30, 30, 1.4),
                new GamePiece("n", GamePiece.RED_NECTAR, GamePiece.FIELD, -30, 30, 1.8));
        BufferedImage image = new BufferedImage(coords.totalPixels(), coords.totalPixels(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        FieldPainter.paint(g, coords, FieldPainter.RobotBox.UNKNOWN, Pose.ORIGIN, List.of(), null, null, null, pieces);
        g.dispose();

        assertEquals(FieldPainter.POLLEN, colorAt(image, 30, 30));
        assertEquals(FieldPainter.RED_NECTAR, colorAt(image, -30, 30));
        assertNotEquals(FieldPainter.POLLEN, colorAt(image, 30, -30), "nothing there");
    }

    private Color colorAt(BufferedImage image, double xInches, double yInches) {
        return new Color(image.getRGB((int) Math.floor(coords.screenX(xInches)), (int) Math.floor(coords.screenY(yInches))));
    }
}
