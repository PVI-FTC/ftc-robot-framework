package pvi.harness.viewer;

import org.firstinspires.ftc.teamcode.opmodes.autonomous.TeamAAutoOpMode;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.imageio.ImageIO;

import pvi.harness.hardware.RobotConfigs;
import pvi.harness.physics.DriveModels;
import pvi.harness.physics.Pose;
import pvi.harness.runtime.OpModeRunner;
import pvi.harness.runtime.RunRecorder;
import pvi.harness.runtime.RunRecording;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Position estimator Step 12: drawing the field, robot box, heading arrow and trail.
 *
 * <p>Everything here draws into off-screen images, so no window ever opens and the tests run
 * on a computer with no display. The drawing tests check the color of single pixels at known
 * field positions (well inside a shape, where edge smoothing can't change the color).</p>
 */
class FieldDrawingTest {
    /** 5 pixels per inch with a 30 px margin, as in the saved images. */
    private final FieldCoordinates coords = new FieldCoordinates(720, 30);
    /** An 18 x 18 in robot. */
    private final FieldPainter.RobotBox box = new FieldPainter.RobotBox(18, 18, false, "Test bot");

    /** Draws one robot pose and trail, with no caption. */
    private BufferedImage draw(Pose pose, List<Pose> trail) {
        BufferedImage image = new BufferedImage(coords.totalPixels(), coords.totalPixels(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        FieldPainter.paint(g, coords, box, pose, trail, null);
        g.dispose();
        return image;
    }

    /** The color of the pixel at a field position, in inches. */
    private Color colorAt(BufferedImage image, double xInches, double yInches) {
        int px = (int) Math.floor(coords.screenX(xInches));
        int py = (int) Math.floor(coords.screenY(yInches));
        return new Color(image.getRGB(px, py));
    }

    // ---- Inches to pixels -------------------------------------------------------------------

    /**
     * WHAT: The field's corners and center land where they should, with +y pointing up.
     *
     * <p>HOW: 720 px field, 30 px margin. The top-left corner (-72, +72) is at (30, 30), the
     * bottom-right (+72, -72) at (750, 750), the center at (390, 390). One inch is 5 px.</p>
     *
     * <p>WHY: Every drawing goes through this one class. If the y flip were missing, the robot
     * would drive down the screen when it drives toward +y on the field.</p>
     */
    @Test
    void cornersAndCenterLandInTheRightPlaceWithYUp() {
        assertEquals(5.0, coords.pixelsPerInch(), 1e-12);
        assertEquals(780, coords.totalPixels());
        assertEquals(30.0, coords.screenX(-72), 1e-9);
        assertEquals(30.0, coords.screenY(72), 1e-9, "+72 in y is the TOP of the screen");
        assertEquals(750.0, coords.screenX(72), 1e-9);
        assertEquals(750.0, coords.screenY(-72), 1e-9);
        assertEquals(390.0, coords.screenX(0), 1e-9);
        assertEquals(390.0, coords.screenY(0), 1e-9);
        assertTrue(coords.screenY(10) < coords.screenY(0), "more field y means higher up the screen");
    }

    /**
     * WHAT: Pixels convert back to the same field inches.
     *
     * <p>WHY: Later steps (clicking on the field, the Pedro overlay) need the reverse.</p>
     */
    @Test
    void pixelsConvertBackToFieldInches() {
        assertEquals(12.5, coords.fieldX(coords.screenX(12.5)), 1e-9);
        assertEquals(-40.25, coords.fieldY(coords.screenY(-40.25)), 1e-9);
    }

    /** WHAT: A field drawn 0 pixels wide, or with a negative margin, is refused clearly. */
    @Test
    void refusesImpossibleSizes() {
        assertThrows(IllegalArgumentException.class, () -> new FieldCoordinates(0, 10));
        assertThrows(IllegalArgumentException.class, () -> new FieldCoordinates(100, -1));
    }

    // ---- What gets drawn --------------------------------------------------------------------

    /**
     * WHAT: The robot box is drawn at the robot's position, in the right half of the screen.
     *
     * <p>HOW: Robot at (0, +50) facing +x. The point 4 in behind and 4 in left of its center,
     * (-4, 54), is worked out by hand (not through FieldCoordinates, so this checks it too):
     * x = 30 + (72 - 4) × 5 = 370 px, y = 30 + (72 - 54) × 5 = 120 px, near the top. That pixel
     * is robot-colored. The mirror pixel near the bottom (y = 660 px, where a missing y flip
     * would put the robot) is not.</p>
     */
    @Test
    void robotBoxIsDrawnWhereTheRobotIs() {
        BufferedImage image = draw(Pose.inches(0, 50, 0), List.of());

        assertEquals(FieldPainter.ROBOT_FILL, new Color(image.getRGB(370, 120)));
        assertNotEquals(FieldPainter.ROBOT_FILL, new Color(image.getRGB(370, 660)));
    }

    /**
     * WHAT: The heading arrow points the way the robot faces.
     *
     * <p>HOW: Robot at the center facing 90° (+y, up the screen). 11 in in front of it (past
     * the 9 in front bumper) is arrow-colored; 11 in behind and 11 in to its side are not.</p>
     *
     * <p>WHY: Seeing which way the robot faces is the whole point of the arrow; a sign mistake
     * would make it point backward.</p>
     */
    @Test
    void headingArrowPointsTheWayTheRobotFaces() {
        BufferedImage image = draw(Pose.inches(0, 0, 90), List.of());

        assertEquals(FieldPainter.HEADING_ARROW, colorAt(image, 0, 11));
        assertNotEquals(FieldPainter.HEADING_ARROW, colorAt(image, 0, -11));
        assertNotEquals(FieldPainter.HEADING_ARROW, colorAt(image, 11, 0));
    }

    /**
     * WHAT: The trail is drawn along the path the robot drove.
     *
     * <p>HOW: A trail from (-60, -60) straight up to (-60, +60), robot at the end. A point
     * halfway along the trail is trail-colored; a point 10 in to the side is plain field.</p>
     */
    @Test
    void trailFollowsThePathDriven() {
        List<Pose> trail = List.of(Pose.inches(-60, -60, 90), Pose.inches(-60, 60, 90));
        BufferedImage image = draw(trail.get(1), trail);

        assertEquals(FieldPainter.TRAIL, colorAt(image, -60, 6));
        assertEquals(FieldPainter.FIELD, colorAt(image, -50, 6));
    }

    /**
     * WHAT: The legend says when the robot size is a placeholder.
     *
     * <p>WHY: 05-desktop-viewer.md asks for placeholder values to be visibly marked. Team A's
     * size is not measured yet (Step 10), so its legend must say so.</p>
     */
    @Test
    void legendMarksAPlaceholderRobotSize() {
        RunRecording.DriveModelValues teamA = RunRecording.DriveModelValues.of(DriveModels.teamA());

        String legend = FieldPainter.legendText(FieldPainter.RobotBox.of(teamA));

        assertTrue(legend.contains("Robot 18 x 18 in (Team A)"), legend);
        assertTrue(legend.contains("PLACEHOLDER size"), legend);
        assertTrue(FieldPainter.legendText(new FieldPainter.RobotBox(16, 17, true, "X")).contains("measured size"));
    }

    // ---- A real run -------------------------------------------------------------------------

    /**
     * WHAT: A real autonomous recording draws the robot at the start and at the end, and saves
     * as a PNG under build/field-images.
     *
     * <p>HOW: Records Team A's autonomous, which drives about 9.4 in forward. The first loop
     * shows the robot around the center; the last shows it 9.4 in further along +x. Open
     * Harness/build/field-images/FieldDrawingTest-teamAAutonomous.png to look at it.</p>
     */
    @Test
    void realAutonomousRecordingDrawsStartAndEndAndSavesAPng() throws IOException {
        // SET UP + RUN
        OpModeRunner runner = new OpModeRunner(new TeamAAutoOpMode(), RobotConfigs.teamA());
        RunRecorder recorder = RunRecorder.attachTo(runner);
        runner.run(1.0, 3.0).assertNoException();
        RunRecording recording = recorder.recording();

        BufferedImage start = FieldImage.render(recording, 0, coords);
        BufferedImage end = FieldImage.render(recording, FieldImage.LAST_LOOP, coords);
        Path png = FieldImage.savePng(recording, FieldImage.LAST_LOOP, "FieldDrawingTest-teamAAutonomous");
        System.out.println("Field image saved to " + png);

        // CHECK: 8 in behind the front bumper, 4 in to the left. At the start that point
        // (x = 1) is on the robot; at the end the robot has moved on past x = 1 + 9.4, so
        // x = 10 is on the robot and x = -7 (its old back end) is not.
        assertEquals(FieldPainter.ROBOT_FILL, colorAt(start, 1, 4));
        assertEquals(FieldPainter.ROBOT_FILL, colorAt(end, 10, 4));
        assertEquals(FieldPainter.ROBOT_FILL, colorAt(start, -7, 4));
        assertNotEquals(FieldPainter.ROBOT_FILL, colorAt(end, -7, 4));
        BufferedImage saved = ImageIO.read(png.toFile());
        assertEquals(780, saved.getWidth());
        assertTrue(Files.size(png) > 0);
    }

    /**
     * WHAT: Tests run headless, so the viewer can never open a window during {@code gradlew test}.
     *
     * <p>WHY: 05-desktop-viewer.md §3: a normal test run must never open a window and must pass
     * on a computer with no display. build.gradle turns headless mode on for tests.</p>
     */
    @Test
    void testsRunHeadless() {
        assertTrue(GraphicsEnvironment.isHeadless(), "java.awt.headless should be true for tests");
    }
}
