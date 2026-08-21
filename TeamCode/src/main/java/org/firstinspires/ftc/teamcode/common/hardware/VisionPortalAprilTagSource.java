package org.firstinspires.ftc.teamcode.common.hardware;

import android.util.Size;

import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.teamcode.common.vision.AprilTagObservation;
import org.firstinspires.ftc.teamcode.common.vision.AprilTagObservationSnapshot;
import org.firstinspires.ftc.teamcode.common.vision.AprilTagPose;
import org.firstinspires.ftc.vision.VisionPortal;
import org.firstinspires.ftc.vision.apriltag.AprilTagDetection;
import org.firstinspires.ftc.vision.apriltag.AprilTagGameDatabase;
import org.firstinspires.ftc.vision.apriltag.AprilTagMetadata;
import org.firstinspires.ftc.vision.apriltag.AprilTagPoseFtc;
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Logitech/UVC AprilTag source implemented behind the hardware boundary. */
class VisionPortalAprilTagSource implements AprilTagVisionSource {
    private static final int VERIFIED_TAG_ID = 22;
    private static final String VERIFIED_TAG_NAME = "Obelisk_PGP";
    private static final double VERIFIED_TAG_SIZE_INCHES = 6.5;
    private static final double COMPARISON_TOLERANCE = 0.000001;
    private static final String CAMERA_FRAME_NAME =
            "FTC camera frame: lens origin, +X right, +Y forward, +Z up";
    private static final String METRIC_QUALITY =
            "Experimental metric pose from fresh, calibrated, verified tag 22 data.";
    private static final String RETAINED_ID_ONLY_QUALITY =
            "Retained ID from last frame; metric pose requires a fresh frame.";

    private final AprilTagCameraConfiguration configuration;
    private AprilTagProcessor aprilTagProcessor;
    private VisionPortal visionPortal;
    private AprilTagObservationSnapshot snapshot = AprilTagObservationSnapshot.unavailable();
    private boolean available;

    VisionPortalAprilTagSource(AprilTagCameraConfiguration configuration) {
        if (configuration == null) {
            throw new IllegalArgumentException("Vision source needs a camera configuration.");
        }
        this.configuration = configuration;
    }

    @Override
    public void initialize(HardwareMap hardwareMap) {
        stop();
        try {
            WebcamName webcam = hardwareMap.get(WebcamName.class,
                    configuration.getWebcamHardwareName());
            aprilTagProcessor = new AprilTagProcessor.Builder()
                    .setTagFamily(AprilTagProcessor.TagFamily.TAG_36h11)
                    .setTagLibrary(AprilTagGameDatabase.getDecodeTagLibrary())
                    .setOutputUnits(DistanceUnit.INCH, AngleUnit.DEGREES)
                    .build();
            visionPortal = new VisionPortal.Builder()
                    .setCamera(webcam)
                    .setCameraResolution(new Size(
                            configuration.getStreamWidth(), configuration.getStreamHeight()))
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
            latest.add(createFreshObservation(detection));
        }
        snapshot = AprilTagObservationSnapshot.fresh(latest);
    }

    private AprilTagObservation createFreshObservation(AprilTagDetection detection) {
        String unavailableReason = getPoseUnavailableReason(detection);
        if (unavailableReason != null) {
            return new AprilTagObservation(detection.id, detection.frameAcquisitionNanoTime,
                    "ID detected; metric pose unavailable: " + unavailableReason);
        }

        AprilTagPoseFtc ftcPose = detection.ftcPose;
        AprilTagPose cameraPose = new AprilTagPose(CAMERA_FRAME_NAME,
                ftcPose.x, ftcPose.y, ftcPose.z,
                ftcPose.pitch, ftcPose.roll, ftcPose.yaw,
                ftcPose.range, ftcPose.bearing, ftcPose.elevation);

        double robotRight = ftcPose.x + configuration.getMountRightInches();
        double robotForward = ftcPose.y + configuration.getMountForwardInches();
        double robotUp = ftcPose.z + configuration.getMountUpInches();
        double robotRange = Math.hypot(robotRight, robotForward);
        double robotBearing = Math.toDegrees(Math.atan2(-robotRight, robotForward));
        double robotElevation = Math.toDegrees(Math.atan2(robotUp, robotForward));
        if (!allFinite(robotRight, robotForward, robotUp, robotRange,
                robotBearing, robotElevation)) {
            return new AprilTagObservation(detection.id, detection.frameAcquisitionNanoTime,
                    "ID detected; metric pose unavailable: transformed values are not finite.");
        }

        AprilTagPose robotPose = new AprilTagPose(configuration.getRobotFrameName(),
                robotRight, robotForward, robotUp,
                ftcPose.pitch, ftcPose.roll, ftcPose.yaw,
                robotRange, robotBearing, robotElevation);
        return new AprilTagObservation(detection.id, detection.frameAcquisitionNanoTime,
                METRIC_QUALITY, cameraPose, robotPose);
    }

    private String getPoseUnavailableReason(AprilTagDetection detection) {
        if (detection.frameAcquisitionNanoTime <= 0) {
            return "the frame acquisition timestamp is invalid.";
        }
        if (detection.id != VERIFIED_TAG_ID) {
            return "only DECODE tag 22 is verified for this experiment.";
        }
        AprilTagMetadata metadata = detection.metadata;
        if (metadata == null) {
            return "tag 22 metadata is missing.";
        }
        if (metadata.id != VERIFIED_TAG_ID || !VERIFIED_TAG_NAME.equals(metadata.name)
                || metadata.distanceUnit == null) {
            return "tag 22 metadata does not match the verified SDK entry.";
        }
        double tagSizeInches = DistanceUnit.INCH.fromUnit(
                metadata.distanceUnit, metadata.tagsize);
        if (!isFinite(tagSizeInches)
                || Math.abs(tagSizeInches - VERIFIED_TAG_SIZE_INCHES) > COMPARISON_TOLERANCE) {
            return "tag 22 black-square size is not the verified 6.5 inches.";
        }
        if (!configuration.isCalibrationVerified()
                || configuration.getStreamWidth() != 640
                || configuration.getStreamHeight() != 480) {
            return "matching Logitech C920 640-by-480 calibration is not verified.";
        }
        if (detection.ftcPose == null) {
            return "the FTC processor supplied no camera-relative pose.";
        }
        AprilTagPoseFtc pose = detection.ftcPose;
        if (!allFinite(pose.x, pose.y, pose.z, pose.pitch, pose.roll, pose.yaw,
                pose.range, pose.bearing, pose.elevation)) {
            return "the FTC camera-relative pose contains a non-finite value.";
        }
        if (!configuration.isMountVerified()) {
            return "the camera mount is not verified.";
        }
        if (!isZero(configuration.getMountYawDegrees())
                || !isZero(configuration.getMountPitchDegrees())
                || !isZero(configuration.getMountRollDegrees())) {
            return "nonzero camera-mount rotation conversion is not verified.";
        }
        return null;
    }

    private boolean allFinite(double... values) {
        for (double value : values) {
            if (!isFinite(value)) {
                return false;
            }
        }
        return true;
    }

    private boolean isFinite(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }

    private boolean isZero(double value) {
        return Math.abs(value) <= COMPARISON_TOLERANCE;
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
