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
    private final String qualityStatus;
    private final AprilTagPose cameraRelativePose;
    private final AprilTagPose robotRelativePose;

    /** Creates an ID-only observation with no metric pose. */
    public AprilTagObservation(int tagId, long timestampNanos, String qualityStatus) {
        this(tagId, timestampNanos, qualityStatus, null, null);
    }

    /** Creates an observation with camera-relative and robot-relative pose views. */
    public AprilTagObservation(int tagId, long timestampNanos, String qualityStatus,
                               AprilTagPose cameraRelativePose,
                               AprilTagPose robotRelativePose) {
        validateIdentity(tagId, timestampNanos, qualityStatus);

        this.tagId = tagId;
        this.timestampNanos = timestampNanos;
        this.qualityStatus = qualityStatus;
        this.cameraRelativePose = cameraRelativePose;
        this.robotRelativePose = robotRelativePose;
    }

    /** Creates a legacy observation whose metric getters describe one robot-relative pose. */
    public AprilTagObservation(int tagId, long timestampNanos, String referenceFrameName,
                               String qualityStatus, double rightInches, double forwardInches,
                               double upInches, double pitchDegrees, double rollDegrees,
                               double yawDegrees, double rangeInches, double bearingDegrees,
                               double elevationDegrees) {
        this(tagId, timestampNanos, qualityStatus,
                null,
                new AprilTagPose(referenceFrameName, rightInches, forwardInches, upInches,
                        pitchDegrees, rollDegrees, yawDegrees, rangeInches, bearingDegrees,
                        elevationDegrees));
    }

    private static void validateIdentity(int tagId, long timestampNanos, String qualityStatus) {
        if (tagId < 0) {
            throw new IllegalArgumentException("AprilTag ID cannot be negative.");
        }
        if (timestampNanos < 0) {
            throw new IllegalArgumentException("Observation timestamp cannot be negative.");
        }
        if (qualityStatus == null || qualityStatus.isEmpty()) {
            throw new IllegalArgumentException("Observation needs a quality status.");
        }
    }

    public int getTagId() { return tagId; }
    public long getTimestampNanos() { return timestampNanos; }
    public boolean isPoseAvailable() { return robotRelativePose != null; }
    public boolean isCameraRelativePoseAvailable() { return cameraRelativePose != null; }
    public boolean isRobotRelativePoseAvailable() { return robotRelativePose != null; }
    public AprilTagPose getCameraRelativePose() { return cameraRelativePose; }
    public AprilTagPose getRobotRelativePose() { return robotRelativePose; }
    public String getReferenceFrameName() {
        return robotRelativePose == null
                ? "Unverified robot frame"
                : robotRelativePose.getReferenceFrameName();
    }
    public String getQualityStatus() { return qualityStatus; }
    public double getRightInches() { return robotValue(PoseValue.RIGHT); }
    public double getForwardInches() { return robotValue(PoseValue.FORWARD); }
    public double getUpInches() { return robotValue(PoseValue.UP); }
    public double getPitchDegrees() { return robotValue(PoseValue.PITCH); }
    public double getRollDegrees() { return robotValue(PoseValue.ROLL); }
    public double getYawDegrees() { return robotValue(PoseValue.YAW); }
    public double getRangeInches() { return robotValue(PoseValue.RANGE); }
    public double getBearingDegrees() { return robotValue(PoseValue.BEARING); }
    public double getElevationDegrees() { return robotValue(PoseValue.ELEVATION); }

    private double robotValue(PoseValue value) {
        if (robotRelativePose == null) {
            return Double.NaN;
        }
        switch (value) {
            case RIGHT: return robotRelativePose.getRightInches();
            case FORWARD: return robotRelativePose.getForwardInches();
            case UP: return robotRelativePose.getUpInches();
            case PITCH: return robotRelativePose.getPitchDegrees();
            case ROLL: return robotRelativePose.getRollDegrees();
            case YAW: return robotRelativePose.getYawDegrees();
            case RANGE: return robotRelativePose.getRangeInches();
            case BEARING: return robotRelativePose.getBearingDegrees();
            case ELEVATION: return robotRelativePose.getElevationDegrees();
            default: throw new IllegalArgumentException("Unknown AprilTag pose value.");
        }
    }

    private enum PoseValue {
        RIGHT, FORWARD, UP, PITCH, ROLL, YAW, RANGE, BEARING, ELEVATION
    }
}
