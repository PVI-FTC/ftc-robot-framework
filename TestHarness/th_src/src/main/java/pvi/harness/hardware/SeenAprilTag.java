package pvi.harness.hardware;

/**
 * One AprilTag that the fake camera sees, and where it is, as plain numbers.
 *
 * <p>The position is measured from the camera lens, in the same frame the FTC SDK uses for
 * {@code detection.ftcPose}: x to the right, y straight out of the lens (forward), z up, all in
 * inches. The angles are the tag's yaw, pitch and roll in degrees, as the SDK reports them
 * (all 0 when the tag faces the camera squarely).</p>
 *
 * <p>The fake camera turns this into a real SDK detection, so robot code gets the same
 * {@code ftcPose}, range, bearing and elevation it would get from a real camera seeing the tag
 * there, and (for tags with a known field position) the same field position of the robot.</p>
 *
 * @param id               the tag's number, e.g. 21
 * @param rightInches      how far right of the lens the tag is (negative = left)
 * @param forwardInches    how far in front of the lens the tag is (must be more than 0)
 * @param upInches         how far above the lens the tag is (negative = below)
 * @param yawDegrees       the tag's yaw as the SDK reports it
 * @param pitchDegrees     the tag's pitch as the SDK reports it
 * @param rollDegrees      the tag's roll as the SDK reports it
 */
public record SeenAprilTag(int id, double rightInches, double forwardInches, double upInches,
                           double yawDegrees, double pitchDegrees, double rollDegrees) {

    /** How far ahead {@link #straightAhead(int)} puts a tag: two field tiles. */
    public static final double DEFAULT_FORWARD_INCHES = 24.0;

    /** Checks the numbers, so a typo fails with a clear message instead of a strange detection. */
    public SeenAprilTag {
        if (id < 0) {
            throw new IllegalArgumentException("An AprilTag id cannot be negative: " + id);
        }
        checkFinite("rightInches", rightInches);
        checkFinite("forwardInches", forwardInches);
        checkFinite("upInches", upInches);
        checkFinite("yawDegrees", yawDegrees);
        checkFinite("pitchDegrees", pitchDegrees);
        checkFinite("rollDegrees", rollDegrees);
        if (forwardInches <= 0) {
            throw new IllegalArgumentException("A camera can only see a tag in front of it, so forwardInches must be"
                    + " more than 0, but was " + forwardInches + " for tag " + id + ".");
        }
    }

    /** A tag seen squarely, 24 inches straight in front of the lens. */
    public static SeenAprilTag straightAhead(int id) {
        return new SeenAprilTag(id, 0.0, DEFAULT_FORWARD_INCHES, 0.0, 0.0, 0.0, 0.0);
    }

    /** A tag seen squarely (no yaw, pitch or roll) at this spot in front of the lens. */
    public static SeenAprilTag at(int id, double rightInches, double forwardInches, double upInches) {
        return new SeenAprilTag(id, rightInches, forwardInches, upInches, 0.0, 0.0, 0.0);
    }

    /** Straight-line distance along the floor from the lens to the tag, as the SDK's {@code ftcPose.range}. */
    public double rangeInches() {
        return Math.hypot(rightInches, forwardInches);
    }

    /** Degrees the tag is to the left of straight ahead (right is negative), as the SDK's {@code ftcPose.bearing}. */
    public double bearingDegrees() {
        return Math.toDegrees(Math.atan2(-rightInches, forwardInches));
    }

    /** Degrees the tag is above straight ahead, as the SDK's {@code ftcPose.elevation}. */
    public double elevationDegrees() {
        return Math.toDegrees(Math.atan2(upInches, forwardInches));
    }

    private static void checkFinite(String name, double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be a normal number, but was " + value + ".");
        }
    }
}
