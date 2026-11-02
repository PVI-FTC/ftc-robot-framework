package pvi.harness.hardware;

import java.util.List;

/**
 * What a Limelight sees at one moment, as plain data, for {@link FakeLimelight}. Put it in a
 * {@link SensorScript} with the Limelight's configuration name:
 * <pre>{@code
 * script.at(1.0).set("limelight", LimelightView.target(-4.5, 12.0, 1.8));     // a color/retro target
 * script.at(2.0).set("limelight", LimelightView.tags(SeenAprilTag.straightAhead(20)));
 * script.at(3.0).set("limelight", LimelightView.nothing());
 * }</pre>
 *
 * @param valid        true when the Limelight sees a target (its {@code isValid()})
 * @param txDegrees    target's angle right of the crosshair (positive right)
 * @param tyDegrees    target's angle above the crosshair (positive up)
 * @param areaPercent  target's size as a percent of the picture
 * @param aprilTags    AprilTags in view (the same records the harness's webcam uses), for the
 *                     AprilTag pipeline's fiducial results
 */
public record LimelightView(boolean valid, double txDegrees, double tyDegrees, double areaPercent,
                            List<SeenAprilTag> aprilTags) {

    public LimelightView {
        aprilTags = List.copyOf(aprilTags);
    }

    /** Nothing in view. */
    public static LimelightView nothing() {
        return new LimelightView(false, 0, 0, 0, List.of());
    }

    /** One target (for example a color pipeline's blob) at these angles and size. */
    public static LimelightView target(double txDegrees, double tyDegrees, double areaPercent) {
        return new LimelightView(true, txDegrees, tyDegrees, areaPercent, List.of());
    }

    /**
     * AprilTags in view. The first tag is also the main target ({@code getTx()}/{@code getTy()}),
     * as on the real Limelight.
     */
    public static LimelightView tags(SeenAprilTag... tags) {
        if (tags.length == 0) {
            return nothing();
        }
        SeenAprilTag first = tags[0];
        // The Limelight measures tx positive to the right; an AprilTag bearing is positive to the left.
        return new LimelightView(true, -first.bearingDegrees(), first.elevationDegrees(), 1.0, List.of(tags));
    }
}
