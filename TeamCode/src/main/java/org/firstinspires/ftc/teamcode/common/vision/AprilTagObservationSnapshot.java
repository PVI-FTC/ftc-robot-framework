package org.firstinspires.ftc.teamcode.common.vision;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable neutral result from one AprilTag source update. */
public final class AprilTagObservationSnapshot {
    private final AprilTagFrameStatus frameStatus;
    private final List<AprilTagObservation> observations;

    public AprilTagObservationSnapshot(AprilTagFrameStatus frameStatus,
                                       List<AprilTagObservation> observations) {
        if (frameStatus == null) {
            throw new IllegalArgumentException("AprilTag snapshot needs a frame status.");
        }
        if (observations == null) {
            throw new IllegalArgumentException("AprilTag snapshot needs an observation list.");
        }

        this.frameStatus = frameStatus;
        this.observations = observations.isEmpty()
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(observations));
    }

    public static AprilTagObservationSnapshot unavailable() {
        return new AprilTagObservationSnapshot(
                AprilTagFrameStatus.UNAVAILABLE, Collections.emptyList());
    }

    public static AprilTagObservationSnapshot retained(
            List<AprilTagObservation> observations) {
        return new AprilTagObservationSnapshot(AprilTagFrameStatus.RETAINED, observations);
    }

    public static AprilTagObservationSnapshot fresh(
            List<AprilTagObservation> observations) {
        return new AprilTagObservationSnapshot(AprilTagFrameStatus.FRESH, observations);
    }

    public AprilTagFrameStatus getFrameStatus() {
        return frameStatus;
    }

    public boolean isFreshFrame() {
        return frameStatus == AprilTagFrameStatus.FRESH;
    }

    public boolean isRetained() {
        return frameStatus == AprilTagFrameStatus.RETAINED;
    }

    public List<AprilTagObservation> getObservations() {
        return observations;
    }
}
