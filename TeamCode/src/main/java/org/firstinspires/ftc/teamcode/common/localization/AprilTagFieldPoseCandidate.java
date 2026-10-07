package org.firstinspires.ftc.teamcode.common.localization;

/** One accepted, fresh AprilTag-derived robot field-pose measurement. */
public final class AprilTagFieldPoseCandidate {
    private final int tagId;
    private final long acquisitionTimestampNanos;
    private final FieldPose fieldPose;

    public AprilTagFieldPoseCandidate(int tagId, long acquisitionTimestampNanos,
                                      FieldPose fieldPose) {
        if (tagId < 0) {
            throw new IllegalArgumentException("A field-pose candidate needs a valid tag ID.");
        }
        if (acquisitionTimestampNanos <= 0) {
            throw new IllegalArgumentException(
                    "A field-pose candidate needs a positive acquisition timestamp.");
        }
        if (fieldPose == null) {
            throw new IllegalArgumentException("A field-pose candidate needs a field pose.");
        }

        this.tagId = tagId;
        this.acquisitionTimestampNanos = acquisitionTimestampNanos;
        this.fieldPose = fieldPose;
    }

    public int getTagId() { return tagId; }
    public long getAcquisitionTimestampNanos() { return acquisitionTimestampNanos; }
    public FieldPose getFieldPose() { return fieldPose; }
    public String getReferenceFrameName() { return fieldPose.getReferenceFrameName(); }
}
