package org.firstinspires.ftc.teamcode.common.hardware;

import android.util.Size;

import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.teamcode.common.vision.AprilTagObservation;
import org.firstinspires.ftc.teamcode.common.vision.AprilTagObservationSnapshot;
import org.firstinspires.ftc.vision.VisionPortal;
import org.firstinspires.ftc.vision.apriltag.AprilTagDetection;
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Logitech/UVC AprilTag source implemented behind the hardware boundary. */
class VisionPortalAprilTagSource implements AprilTagVisionSource {
    private static final int STREAM_WIDTH = 640;
    private static final int STREAM_HEIGHT = 480;
    private static final String FRESH_ID_ONLY_QUALITY =
            "Fresh ID detected; metric pose is not implemented in MV-03.";
    private static final String RETAINED_ID_ONLY_QUALITY =
            "Retained ID from last frame; metric pose requires a fresh frame.";

    private final String webcamHardwareName;
    private AprilTagProcessor aprilTagProcessor;
    private VisionPortal visionPortal;
    private AprilTagObservationSnapshot snapshot = AprilTagObservationSnapshot.unavailable();
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
            aprilTagProcessor = new AprilTagProcessor.Builder().build();
            visionPortal = new VisionPortal.Builder()
                    .setCamera(webcam)
                    .setCameraResolution(new Size(STREAM_WIDTH, STREAM_HEIGHT))
                    .addProcessor(aprilTagProcessor)
                    .build();
            available = true;
            snapshot = AprilTagObservationSnapshot.retained(Collections.emptyList());
        } catch (RuntimeException ignored) {
            // A missing or unavailable optional camera is safe for this diagnostic robot.
            stop();
        }
    }

    @Override
    public void update() {
        if (!available || aprilTagProcessor == null) {
            snapshot = AprilTagObservationSnapshot.unavailable();
            return;
        }

        List<AprilTagDetection> freshDetections = aprilTagProcessor.getFreshDetections();
        if (freshDetections == null) {
            snapshot = AprilTagObservationSnapshot.retained(
                    copyAsRetainedIdOnly(snapshot.getObservations()));
            return;
        }

        List<AprilTagObservation> latest = new ArrayList<>();
        for (AprilTagDetection detection : freshDetections) {
            latest.add(new AprilTagObservation(
                    detection.id, detection.frameAcquisitionNanoTime, FRESH_ID_ONLY_QUALITY));
        }
        snapshot = AprilTagObservationSnapshot.fresh(latest);
    }

    private List<AprilTagObservation> copyAsRetainedIdOnly(
            List<AprilTagObservation> previousObservations) {
        List<AprilTagObservation> retained = new ArrayList<>();
        for (AprilTagObservation observation : previousObservations) {
            retained.add(new AprilTagObservation(observation.getTagId(),
                    observation.getTimestampNanos(), RETAINED_ID_ONLY_QUALITY));
        }
        return retained;
    }

    @Override
    public void stop() {
        if (visionPortal != null) {
            visionPortal.close();
        }
        visionPortal = null;
        aprilTagProcessor = null;
        snapshot = AprilTagObservationSnapshot.unavailable();
        available = false;
    }

    @Override public boolean isAvailable() { return available; }
    @Override public AprilTagObservationSnapshot getSnapshot() { return snapshot; }
}
