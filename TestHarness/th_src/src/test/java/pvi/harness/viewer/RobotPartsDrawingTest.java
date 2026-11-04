package pvi.harness.viewer;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

import pvi.harness.physics.Pose;
import pvi.harness.runtime.RunRecording;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The robot's wheels and intake on the field drawing: each wheel fills toward the front (cyan)
 * or back (amber) with its power, and the intake lights cyan taking in, amber ejecting.
 *
 * <p>Drawn off-screen, large (20 pixels per inch) so single pixels well inside each part can be
 * checked. A close-up is saved to {@code build/field-images/} to look at.</p>
 */
class RobotPartsDrawingTest {
    /** 20 pixels per inch. */
    private final FieldCoordinates coords = new FieldCoordinates(2880, 30);
    private final FieldPainter.RobotBox box = new FieldPainter.RobotBox(18, 18, false, "Test bot");

    private BufferedImage draw(FieldPainter.RobotParts parts) {
        BufferedImage image = new BufferedImage(coords.totalPixels(), coords.totalPixels(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        FieldPainter.paint(g, coords, box, Pose.ORIGIN, List.of(), null, parts);
        g.dispose();
        return image;
    }

    /** The pixel at a field position in inches (the robot is at the origin facing +x, so x = forward, y = left). */
    private Color colorAt(BufferedImage image, double xInches, double yInches) {
        return new Color(image.getRGB((int) Math.floor(coords.screenX(xInches)), (int) Math.floor(coords.screenY(yInches))));
    }

    /**
     * WHAT: A wheel driving forward fills toward the front in cyan; one driving backward fills
     * toward the back in amber; a stopped wheel stays dark.
     *
     * <p>WHY: You can see at a glance which wheels are pushing which way, e.g. a miswired motor
     * spinning backwards while the others go forward.</p>
     */
    @Test
    void wheelsShowTheirPowerAndDirection() throws IOException {
        // Front-left full forward, rear-right full backward, the others stopped; intake taking in.
        BufferedImage image = draw(new FieldPainter.RobotParts(1.0, 0.0, 0.0, -1.0, true, 1.0));

        assertEquals(Theme.ACCENT, colorAt(image, 6.6, 7.6), "front-left: front half filled cyan");
        assertEquals(FieldPainter.WHEEL, colorAt(image, 5.4, 7.6), "front-left: back half not filled");
        assertEquals(FieldPainter.REVERSE, colorAt(image, -6.6, -7.6), "rear-right: back half filled amber");
        assertEquals(FieldPainter.WHEEL, colorAt(image, 6.6, -7.6), "front-right is stopped");
        assertEquals(Theme.ACCENT, colorAt(image, 6.4, 1.5), "the intake is running (taking in)");

        saveCloseUp(image, "RobotPartsDrawingTest-robot");
    }

    /**
     * WHAT: An ejecting intake lights amber; a stopped one stays dark.
     *
     * <p>WHY: Intaking and ejecting must look different.</p>
     */
    @Test
    void intakeShowsInOutAndStopped() {
        assertEquals(FieldPainter.REVERSE, colorAt(draw(new FieldPainter.RobotParts(0, 0, 0, 0, true, -1.0)), 6.4, 1.5));
        assertEquals(FieldPainter.WHEEL, colorAt(draw(new FieldPainter.RobotParts(0, 0, 0, 0, true, 0.0)), 6.4, 1.5));
        assertEquals(FieldPainter.ROBOT_FILL, colorAt(draw(new FieldPainter.RobotParts(0, 0, 0, 0, false, 0.0)), 6.4, 1.5),
                "no intake motor: nothing drawn there");
    }

    /**
     * WHAT: The parts are read from a loop's motors by name; without all four drive motors
     * there are no wheels to draw.
     *
     * <p>WHY: The names are Team A's configuration names; other robots may have no drive motors.</p>
     */
    @Test
    void partsComeFromTheMotorNames() {
        FieldPainter.RobotParts parts = FieldPainter.RobotParts.of(List.of(
                new RunRecording.MotorState("frontLeft", 0.1), new RunRecording.MotorState("frontRight", 0.2),
                new RunRecording.MotorState("rearLeft", 0.3), new RunRecording.MotorState("rearRight", 0.4),
                new RunRecording.MotorState("intake", -0.5)));
        assertNotNull(parts);
        assertEquals(0.4, parts.rearRight(), 1e-9);
        assertTrue(parts.hasIntake());
        assertEquals(-0.5, parts.intake(), 1e-9);
        assertNull(FieldPainter.RobotParts.of(List.of(new RunRecording.MotorState("intake", 1.0))));
    }

    /** Saves the 40 x 40 in square around the robot as a PNG to look at. */
    private void saveCloseUp(BufferedImage image, String name) throws IOException {
        int left = (int) coords.screenX(-20);
        int top = (int) coords.screenY(20);
        int size = (int) (40 * coords.pixelsPerInch());
        Path folder = Path.of("build", "field-images");
        Files.createDirectories(folder);
        Path file = folder.resolve(name + ".png");
        ImageIO.write(image.getSubimage(left, top, size, size), "png", file.toFile());
        System.out.println("Robot close-up saved to " + file.toAbsolutePath());
    }

    /**
     * WHAT: A wall the robot just hit is drawn red on the field border; the other walls aren't.
     *
     * <p>WHY: The viewer flashes the wall for a moment so a hit is easy to spot when replaying.</p>
     */
    @Test
    void theWallJustHitFlashesRed() {
        BufferedImage image = new BufferedImage(coords.totalPixels(), coords.totalPixels(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        FieldPainter.paint(g, coords, box, Pose.ORIGIN, List.of(), null, null, "+x");
        g.dispose();

        assertEquals(FieldPainter.WALL_FLASH, colorAt(image, 72.0, 30.0), "+x wall is red");
        assertEquals(FieldPainter.WALL_FLASH, colorAt(image, 71.9, -50.0), "along its whole length");
        assertNotEquals(FieldPainter.WALL_FLASH, colorAt(image, -72.0, 30.0), "the -x wall is not");
        assertNotEquals(FieldPainter.WALL_FLASH, colorAt(image, 30.0, 72.0), "the +y wall is not");
        assertNotEquals(FieldPainter.WALL_FLASH, colorAt(draw(null), 72.0, 30.0), "no flash without a hit");
    }
}
