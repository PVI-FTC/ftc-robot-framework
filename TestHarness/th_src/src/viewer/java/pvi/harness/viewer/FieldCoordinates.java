package pvi.harness.viewer;

import pvi.harness.physics.Pose;

/**
 * Converts between field inches and screen pixels. This is the <b>only</b> place in the viewer
 * that does that conversion, so the y flip lives here and nowhere else.
 *
 * <p>The field uses the FTC frame: origin at the center, x and y from -72 to +72 inches, +y
 * pointing up on the drawing. Screens count y <b>downward</b> from the top, so a bigger field y
 * becomes a smaller screen y.</p>
 *
 * <pre>
 *   screen (0,0) ┌──────────────────────┐
 *                │ margin               │
 *                │   (-72,+72)  (+72,+72)│
 *                │        field         │
 *                │   (-72,-72)  (+72,-72)│
 *                └──────────────────────┘
 * </pre>
 */
public final class FieldCoordinates {
    /** Width (and height) of the field square on screen, in pixels. */
    private final double fieldPixels;
    /** Empty space around the field square, in pixels. */
    private final double marginPixels;

    /**
     * @param fieldPixels  size of the field square on screen, in pixels
     * @param marginPixels empty space left on every side of it, in pixels
     */
    public FieldCoordinates(double fieldPixels, double marginPixels) {
        if (!(fieldPixels > 0) || !Double.isFinite(fieldPixels)) {
            throw new IllegalArgumentException("The field must be drawn bigger than 0 pixels, got " + fieldPixels);
        }
        if (!(marginPixels >= 0) || !Double.isFinite(marginPixels)) {
            throw new IllegalArgumentException("The margin can't be negative, got " + marginPixels);
        }
        this.fieldPixels = fieldPixels;
        this.marginPixels = marginPixels;
    }

    /** How many pixels one inch takes on screen. */
    public double pixelsPerInch() {
        return fieldPixels / (2 * Pose.FIELD_HALF_WIDTH_INCHES);
    }

    /** Size of the field square in pixels. */
    public double fieldPixels() {
        return fieldPixels;
    }

    /** Space around the field square in pixels. */
    public double marginPixels() {
        return marginPixels;
    }

    /** Total width (and height) needed: the field plus a margin on both sides. */
    public int totalPixels() {
        return (int) Math.ceil(fieldPixels + 2 * marginPixels);
    }

    /** Screen x (pixels from the left) for a field x in inches. */
    public double screenX(double xInches) {
        return marginPixels + (xInches + Pose.FIELD_HALF_WIDTH_INCHES) * pixelsPerInch();
    }

    /** Screen y (pixels from the top) for a field y in inches. Flipped: +y on the field is up. */
    public double screenY(double yInches) {
        return marginPixels + (Pose.FIELD_HALF_WIDTH_INCHES - yInches) * pixelsPerInch();
    }

    /** Field x in inches for a screen x (the reverse of {@link #screenX}), e.g. for a mouse click. */
    public double fieldX(double screenX) {
        return (screenX - marginPixels) / pixelsPerInch() - Pose.FIELD_HALF_WIDTH_INCHES;
    }

    /** Field y in inches for a screen y (the reverse of {@link #screenY}). */
    public double fieldY(double screenY) {
        return Pose.FIELD_HALF_WIDTH_INCHES - (screenY - marginPixels) / pixelsPerInch();
    }
}
