package org.firstinspires.ftc.teamcode.common.localization;

/** Immutable robot pose in one named field coordinate system. */
public final class FieldPose {
    private final String referenceFrameName;
    private final double xInches;
    private final double yInches;
    private final double zInches;
    private final double pitchDegrees;
    private final double rollDegrees;
    private final double yawDegrees;

    public FieldPose(String referenceFrameName, double xInches, double yInches,
                     double zInches, double pitchDegrees, double rollDegrees,
                     double yawDegrees) {
        if (referenceFrameName == null || referenceFrameName.isEmpty()) {
            throw new IllegalArgumentException("A field pose needs a reference-frame name.");
        }
        if (!allFinite(xInches, yInches, zInches,
                pitchDegrees, rollDegrees, yawDegrees)) {
            throw new IllegalArgumentException("Field-pose values must be finite.");
        }

        this.referenceFrameName = referenceFrameName;
        this.xInches = xInches;
        this.yInches = yInches;
        this.zInches = zInches;
        this.pitchDegrees = pitchDegrees;
        this.rollDegrees = rollDegrees;
        this.yawDegrees = yawDegrees;
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
    public double getXInches() { return xInches; }
    public double getYInches() { return yInches; }
    public double getZInches() { return zInches; }
    public double getPitchDegrees() { return pitchDegrees; }
    public double getRollDegrees() { return rollDegrees; }
    public double getYawDegrees() { return yawDegrees; }
}
