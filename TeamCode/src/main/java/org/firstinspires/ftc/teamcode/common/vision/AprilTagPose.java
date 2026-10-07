package org.firstinspires.ftc.teamcode.common.vision;

/** Immutable, library-neutral AprilTag pose in one documented reference frame. */
public final class AprilTagPose {
    private final String referenceFrameName;
    private final double rightInches;
    private final double forwardInches;
    private final double upInches;
    private final double pitchDegrees;
    private final double rollDegrees;
    private final double yawDegrees;
    private final double rangeInches;
    private final double bearingDegrees;
    private final double elevationDegrees;

    public AprilTagPose(String referenceFrameName, double rightInches, double forwardInches,
                        double upInches, double pitchDegrees, double rollDegrees,
                        double yawDegrees, double rangeInches, double bearingDegrees,
                        double elevationDegrees) {
        if (referenceFrameName == null || referenceFrameName.isEmpty()) {
            throw new IllegalArgumentException("AprilTag pose needs a reference-frame name.");
        }
        if (!allFinite(rightInches, forwardInches, upInches, pitchDegrees, rollDegrees,
                yawDegrees, rangeInches, bearingDegrees, elevationDegrees)) {
            throw new IllegalArgumentException("AprilTag pose values must be finite.");
        }

        this.referenceFrameName = referenceFrameName;
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

    private static boolean allFinite(double... values) {
        for (double value : values) {
            if (Double.isNaN(value) || Double.isInfinite(value)) {
                return false;
            }
        }
        return true;
    }

    public String getReferenceFrameName() { return referenceFrameName; }
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
