package org.firstinspires.ftc.teamcode.common.hardware;

import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.teamcode.common.vision.AprilTagObservation;
import org.firstinspires.ftc.vision.VisionPortal;
import org.firstinspires.ftc.vision.apriltag.AprilTagDetection;
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Logitech/UVC AprilTag source implemented behind the hardware boundary. */
class VisionPortalAprilTagSource implements AprilTagVisionSource {
    private static final String ID_ONLY_QUALITY =
            "ID detected; robot-relative pose unavailable until calibration and mount are recorded.";

    private final String webcamHardwareName;
    private AprilTagProcessor aprilTagProcessor;
    private VisionPortal visionPortal;
    private List<AprilTagObservation> observations = Collections.emptyList();
    private boolean available;

    VisionPortalAprilTagSource(String webcamHardwareName) {
        if (webcamHardwareName == null || webcamHardwareName.isEmpty()) {
            throw new IllegalArgumentException("Vision source needs a webcam hardware name.");
        }
        this.webcamHardwareName = webcamHardwareName;
    }

    @Override
    public void initialize(HardwareMap hardwareMap) {
        stop();
        try {
            WebcamName webcam = hardwareMap.get(WebcamName.class, webcamHardwareName);
            aprilTagProcessor = AprilTagProcessor.easyCreateWithDefaults();
            visionPortal = VisionPortal.easyCreateWithDefaults(webcam, aprilTagProcessor);
            available = true;
        } catch (RuntimeException ignored) {
            // A missing or unavailable optional camera is safe for this diagnostic robot.
            stop();
        }
    }

    @Override
    public void update() {
        if (!available || aprilTagProcessor == null) {
            observations = Collections.emptyList();
            return;
        }

        List<AprilTagObservation> latest = new ArrayList<>();
        for (AprilTagDetection detection : aprilTagProcessor.getDetections()) {
            latest.add(new AprilTagObservation(detection.id, System.nanoTime(), ID_ONLY_QUALITY));
        }
        observations = Collections.unmodifiableList(latest);
    }

    @Override
    public void stop() {
        if (visionPortal != null) {
            visionPortal.close();
        }
        visionPortal = null;
        aprilTagProcessor = null;
        observations = Collections.emptyList();
        available = false;
    }

    @Override public boolean isAvailable() { return available; }
    @Override public List<AprilTagObservation> getObservations() { return observations; }
}
