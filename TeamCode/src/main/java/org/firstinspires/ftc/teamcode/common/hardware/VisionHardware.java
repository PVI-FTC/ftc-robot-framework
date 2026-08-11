package org.firstinspires.ftc.teamcode.common.hardware;

import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.teamcode.common.vision.AprilTagObservation;

import java.util.ArrayList;
import java.util.Collections;
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
    private List<AprilTagObservation> observations = Collections.emptyList();

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
        observations = Collections.emptyList();
    }

    /** Starts a selected hardware source during robot initialization. */
    public void initialize(HardwareMap hardwareMap) {
        source.initialize(hardwareMap);
        observations = copyObservations(source.getObservations());
    }

    /** Updates the selected source once and saves its latest neutral observations. */
    public void update() {
        source.update();
        observations = copyObservations(source.getObservations());
    }

    /** Releases any selected source resources and clears stale observations. */
    public void stop() {
        source.stop();
        observations = Collections.emptyList();
    }

    /** Returns whether the selected source is available. */
    public boolean isAvailable() {
        return source.isAvailable();
    }

    /** Returns the latest immutable snapshot of neutral AprilTag observations. */
    public List<AprilTagObservation> getObservations() {
        return observations;
    }

    private List<AprilTagObservation> copyObservations(List<AprilTagObservation> sourceObservations) {
        if (sourceObservations == null || sourceObservations.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(sourceObservations));
    }

    /** Safe default source until a camera source is explicitly composed. */
    private static class UnavailableAprilTagVisionSource implements AprilTagVisionSource {
        @Override public void initialize(HardwareMap hardwareMap) { }
        @Override public void update() { }
        @Override public void stop() { }
        @Override public boolean isAvailable() { return false; }
        @Override public List<AprilTagObservation> getObservations() {
            return Collections.emptyList();
        }
    }
}
