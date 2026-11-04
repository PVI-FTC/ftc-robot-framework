package pvi.harness.viewer;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import pvi.harness.runtime.RunRecording;

/**
 * Draws one moment of a {@link RunRecording} into an image, off-screen. Works with no display,
 * so tests can check the drawing, and anyone can save a picture of a run as a PNG:
 *
 * <pre>{@code
 * Path png = FieldImage.savePng(recorder.recording(), FieldImage.LAST_LOOP, "teamAAutonomous");
 * // build/field-images/teamAAutonomous.png
 * }</pre>
 */
public final class FieldImage {
    /** Pass as {@code loopIndex} to draw the end of the run. */
    public static final int LAST_LOOP = -1;
    /** Where {@link #savePng} puts files, relative to the folder tests run in. */
    public static final Path DEFAULT_FOLDER = Path.of("build", "field-images");
    /** Default size: 5 pixels per inch (720 px field) with a 30 px margin. */
    public static final FieldCoordinates DEFAULT_COORDINATES = new FieldCoordinates(720, 30);

    /** Only static methods here; nothing to create. */
    private FieldImage() {
    }

    /**
     * Draws the robot as it was after loop entry {@code loopIndex}, with its trail up to then.
     *
     * @param loopIndex index into {@code recording.loops()}, or {@link #LAST_LOOP}
     */
    public static BufferedImage render(RunRecording recording, int loopIndex, FieldCoordinates coords) {
        RecordingView view = new RecordingView(recording);
        int index = loopIndex == LAST_LOOP ? view.size() - 1 : loopIndex;
        return render(view, index, coords);
    }

    /** Like {@link #render(RunRecording, int, FieldCoordinates)}, reusing a prepared view. */
    static BufferedImage render(RecordingView view, int index, FieldCoordinates coords) {
        int size = coords.totalPixels();
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            FieldPainter.paint(g, coords, view.box(), view.loop(index).pose(), view.trail(index), view.caption(index));
        } finally {
            g.dispose();
        }
        return image;
    }

    /**
     * Saves {@link #render} (default size) as {@code build/field-images/<name>.png}.
     *
     * @param name file name without ".png": letters, digits, '-', '_' and '.' only
     * @return the full path of the saved file
     */
    public static Path savePng(RunRecording recording, int loopIndex, String name) {
        if (name == null || !name.matches("[A-Za-z0-9_.-]+") || name.startsWith(".")) {
            throw new IllegalArgumentException("Image file name \"" + name + "\" is not allowed. Use letters,"
                    + " digits, '-', '_' and '.' only (not starting with '.'), without \".png\".");
        }
        BufferedImage image = render(recording, loopIndex, DEFAULT_COORDINATES);
        Path file = DEFAULT_FOLDER.resolve(name + ".png").toAbsolutePath();
        try {
            Files.createDirectories(file.getParent());
            ImageIO.write(image, "png", file.toFile());
        } catch (IOException e) {
            throw new UncheckedIOException("Could not save the field image to " + file, e);
        }
        return file;
    }
}
