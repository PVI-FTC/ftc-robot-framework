package org.firstinspires.ftc.teamcode.common.hardware;

import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.teamcode.common.vision.AprilTagObservation;
import org.firstinspires.ftc.teamcode.common.vision.AprilTagObservationSnapshot;

import java.util.List;

/**
 * Optional lifecycle and observation boundary for vision hardware.
 *
 * <p>The default source remains unavailable and returns no observations. A later hardware-only
 * source may use VisionPortal or Limelight behind {@link AprilTagVisionSource}; neither FTC type
 * is exposed above this wrapper.</p>
 */
public class VisionHardware {
    private final AprilTagVisionSource source;
    private AprilTagObservationSnapshot snapshot = AprilTagObservationSnapshot.unavailable();

    /** Creates safe optional vision hardware with no configured camera source. */
    public VisionHardware() {
        this(new UnavailableAprilTagVisionSource());
    }

    /** Creates Logitech/UVC AprilTag hardware for one configured webcam name. */
    public VisionHardware(String webcamHardwareName) {
        this(new VisionPortalAprilTagSource(webcamHardwareName));
    }

    /** Allows a hardware-layer source to be supplied by a later vision composition. */
    VisionHardware(AprilTagVisionSource source) {
        if (source == null) {
            throw new IllegalArgumentException("Vision hardware needs a vision source.");
        }
        this.source = source;
    }

    /** Starts the safe no-camera lifecycle used by the existing simple robots. */
    public void initialize() {
        snapshot = AprilTagObservationSnapshot.unavailable();
    }

    /** Starts a selected hardware source during robot initialization. */
    public void initialize(HardwareMap hardwareMap) {
        source.initialize(hardwareMap);
        snapshot = copySnapshot(source.getSnapshot());
    }

    /** Updates the selected source once and saves its latest neutral observations. */
    public void update() {
        source.update();
        snapshot = copySnapshot(source.getSnapshot());
    }

    /** Releases any selected source resources and clears stale observations. */
    public void stop() {
        source.stop();
        snapshot = AprilTagObservationSnapshot.unavailable();
    }

    /** Returns whether the selected source is available. */
    public boolean isAvailable() {
        return source.isAvailable();
    }

    /** Returns the latest immutable snapshot of neutral AprilTag observations. */
    public List<AprilTagObservation> getObservations() {
        return snapshot.getObservations();
    }

    /** Returns the latest immutable result and its neutral frame status. */
    public AprilTagObservationSnapshot getSnapshot() {
        return snapshot;
    }

    private AprilTagObservationSnapshot copySnapshot(AprilTagObservationSnapshot sourceSnapshot) {
        if (sourceSnapshot == null) {
            return AprilTagObservationSnapshot.unavailable();
        }
        return new AprilTagObservationSnapshot(
                sourceSnapshot.getFrameStatus(), sourceSnapshot.getObservations());
    }

    /** Safe default source until a camera source is explicitly composed. */
    private static class UnavailableAprilTagVisionSource implements AprilTagVisionSource {
        @Override public void initialize(HardwareMap hardwareMap) { }
        @Override public void update() { }
        @Override public void stop() { }
        @Override public boolean isAvailable() { return false; }
        @Override public AprilTagObservationSnapshot getSnapshot() {
            return AprilTagObservationSnapshot.unavailable();
        }
    }
}
