package org.firstinspires.ftc.teamcode.common.localization;

/**
 * A library-neutral estimate of the robot position on the field.
 *
 * <p>Available estimates use inches for x and y and radians for heading. An unavailable estimate
 * lets simple robots report that they have no localization device without inventing a position.</p>
 */
public final class PoseEstimate {
    private static final PoseEstimate UNAVAILABLE = new PoseEstimate(false, 0.0, 0.0, 0.0);

    private final boolean available;
    private final double xInches;
    private final double yInches;
    private final double headingRadians;

    private PoseEstimate(boolean available, double xInches, double yInches, double headingRadians) {
        this.available = available;
        this.xInches = xInches;
        this.yInches = yInches;
        this.headingRadians = headingRadians;
    }

    /** Returns an estimate for a robot whose localization hardware has reported a pose. */
    public static PoseEstimate available(double xInches, double yInches, double headingRadians) {
        if (!isFinite(xInches) || !isFinite(yInches) || !isFinite(headingRadians)) {
            throw new IllegalArgumentException("Pose values must be finite.");
        }
        return new PoseEstimate(true, xInches, yInches, headingRadians);
    }

    /** Returns the shared safe estimate for a robot without available localization. */
    public static PoseEstimate unavailable() {
        return UNAVAILABLE;
    }

    public boolean isAvailable() {
        return available;
    }

    public double getXInches() {
        return xInches;
    }

    public double getYInches() {
        return yInches;
    }

    public double getHeadingRadians() {
        return headingRadians;
    }

    private static boolean isFinite(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }
}