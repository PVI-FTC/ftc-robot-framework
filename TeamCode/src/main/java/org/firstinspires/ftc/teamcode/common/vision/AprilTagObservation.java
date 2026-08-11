package org.firstinspires.ftc.teamcode.common.vision;

/**
 * Immutable AprilTag observation that does not expose an FTC or Limelight type.
 *
 * <p>When {@link #isPoseAvailable()} is false, every distance and angle getter returns
 * {@link Double#NaN}. This makes an ID-only detection clear without inventing camera calibration
 * or mount values. When pose is available, distances are inches and angles are degrees.</p>
 *
 * <p>The team must select and document the robot origin before a source reports a pose in the
 * robot frame. The required axis convention is +X right, +Y forward, and +Z up.</p>
 */
public final class AprilTagObservation {
    private final int tagId;
    private final long timestampNanos;
    private final boolean poseAvailable;
    private final String referenceFrameName;
    private final String qualityStatus;
    private final double rightInches;
    private final double forwardInches;
    private final double upInches;
    private final double pitchDegrees;
    private final double rollDegrees;
    private final double yawDegrees;
    private final double rangeInches;
    private final double bearingDegrees;
    private final double elevationDegrees;

    /** Creates an ID-only observation with no metric pose. */
    public AprilTagObservation(int tagId, long timestampNanos, String qualityStatus) {
        this(tagId, timestampNanos, false, "Unverified robot frame", qualityStatus,
                Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN,
                Double.NaN, Double.NaN, Double.NaN);
    }

    /** Creates an observation with a verified robot-relative pose. */
    public AprilTagObservation(int tagId, long timestampNanos, String referenceFrameName,
                               String qualityStatus, double rightInches, double forwardInches,
                               double upInches, double pitchDegrees, double rollDegrees,
                               double yawDegrees, double rangeInches, double bearingDegrees,
                               double elevationDegrees) {
        this(tagId, timestampNanos, true, referenceFrameName, qualityStatus, rightInches,
                forwardInches, upInches, pitchDegrees, rollDegrees, yawDegrees, rangeInches,
                bearingDegrees, elevationDegrees);
    }

    private AprilTagObservation(int tagId, long timestampNanos, boolean poseAvailable,
                                String referenceFrameName, String qualityStatus,
                                double rightInches, double forwardInches, double upInches,
                                double pitchDegrees, double rollDegrees, double yawDegrees,
                                double rangeInches, double bearingDegrees,
                                double elevationDegrees) {
        if (tagId < 0) {
            throw new IllegalArgumentException("AprilTag ID cannot be negative.");
        }
        if (timestampNanos < 0) {
            throw new IllegalArgumentException("Observation timestamp cannot be negative.");
        }
        if (referenceFrameName == null || referenceFrameName.isEmpty()) {
            throw new IllegalArgumentException("Observation needs a reference-frame name.");
        }
        if (qualityStatus == null || qualityStatus.isEmpty()) {
            throw new IllegalArgumentException("Observation needs a quality status.");
        }

        this.tagId = tagId;
        this.timestampNanos = timestampNanos;
        this.poseAvailable = poseAvailable;
        this.referenceFrameName = referenceFrameName;
        this.qualityStatus = qualityStatus;
        this.rightInches = rightInches;
        this.forwardInches = forwardInches;
        this.upInches = upInches;
        this.pitchDegrees = pitchDegrees;
        this.rollDegrees = rollDegrees;
        this.yawDegrees = yawDegrees;
        this.rangeInches = rangeInches;
        this.bearingDegrees = bearingDegrees;
        this.elevationDegrees = elevationDegrees;
    }

    public int getTagId() { return tagId; }
    public long getTimestampNanos() { return timestampNanos; }
    public boolean isPoseAvailable() { return poseAvailable; }
    public String getReferenceFrameName() { return referenceFrameName; }
    public String getQualityStatus() { return qualityStatus; }
    public double getRightInches() { return rightInches; }
    public double getForwardInches() { return forwardInches; }
    public double getUpInches() { return upInches; }
    public double getPitchDegrees() { return pitchDegrees; }
    public double getRollDegrees() { return rollDegrees; }
    public double getYawDegrees() { return yawDegrees; }
    public double getRangeInches() { return rangeInches; }
    public double getBearingDegrees() { return bearingDegrees; }
    public double getElevationDegrees() { return elevationDegrees; }
}
