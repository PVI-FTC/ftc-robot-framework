package org.firstinspires.ftc.teamcode.common.hardware;

import android.util.Size;

import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.robotcore.external.navigation.Position;
import org.firstinspires.ftc.robotcore.external.navigation.YawPitchRollAngles;
import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.teamcode.common.localization.AprilTagFieldPoseCandidate;
import org.firstinspires.ftc.teamcode.common.localization.AprilTagLocalizationConfiguration;
import org.firstinspires.ftc.teamcode.common.localization.FieldPose;
import org.firstinspires.ftc.teamcode.common.vision.AprilTagObservation;
import org.firstinspires.ftc.teamcode.common.vision.AprilTagObservationSnapshot;
import org.firstinspires.ftc.teamcode.common.vision.AprilTagPose;
import org.firstinspires.ftc.vision.VisionPortal;
import org.firstinspires.ftc.vision.apriltag.AprilTagDetection;
import org.firstinspires.ftc.vision.apriltag.AprilTagSingleDetection;
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
    private static final String FIELD_POSE_ACCEPTED =
            "Accepted fresh fixed-tag field-pose candidate; not a localization estimate.";

    private final AprilTagCameraConfiguration configuration;
    private final AprilTagLocalizationConfiguration localizationConfiguration;
    private AprilTagProcessor aprilTagProcessor;
    private VisionPortal visionPortal;
    private AprilTagObservationSnapshot snapshot = AprilTagObservationSnapshot.unavailable();
    private boolean available;

    VisionPortalAprilTagSource(AprilTagCameraConfiguration configuration) {
        this(configuration, null);
    }

    VisionPortalAprilTagSource(AprilTagCameraConfiguration configuration,
                               AprilTagLocalizationConfiguration localizationConfiguration) {
        if (configuration == null) {
            throw new IllegalArgumentException("Vision source needs a camera configuration.");
        }
        this.configuration = configuration;
        this.localizationConfiguration = localizationConfiguration;
    }

    @Override
    public void initialize(HardwareMap hardwareMap) {
        stop();
        try {
            WebcamName webcam = hardwareMap.get(WebcamName.class,
                    configuration.getWebcamHardwareName());
            AprilTagProcessor.Builder processorBuilder = new AprilTagProcessor.Builder()
                    .setTagFamily(AprilTagProcessor.TagFamily.TAG_36h11)
                    .setTagLibrary(AprilTagGameDatabase.getDecodeTagLibrary())
                    .setOutputUnits(DistanceUnit.INCH, AngleUnit.DEGREES);
            if (canConfigureSdkCameraPose()) {
                processorBuilder.setCameraPose(
                        new Position(DistanceUnit.INCH,
                                configuration.getMountRightInches(),
                                configuration.getMountForwardInches(),
                                configuration.getMountUpInches(), 0),
                        new YawPitchRollAngles(AngleUnit.DEGREES,
                                0, -90, 0, 0));
            }
            aprilTagProcessor = processorBuilder.build();
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
            // This source verifies individual tags only; SDK 12 cluster detections are skipped.
            if (detection instanceof AprilTagSingleDetection) {
                latest.add(createFreshObservation((AprilTagSingleDetection) detection));
            }
        }
        snapshot = AprilTagObservationSnapshot.fresh(latest);
    }

    private AprilTagObservation createFreshObservation(AprilTagSingleDetection detection) {
        AprilTagObservation relativeObservation = createRelativeObservation(detection);
        FieldPoseCandidateResult fieldResult = createFieldPoseCandidate(detection);
        return new AprilTagObservation(relativeObservation.getTagId(),
                relativeObservation.getTimestampNanos(),
                relativeObservation.getQualityStatus(),
                relativeObservation.getCameraRelativePose(),
                relativeObservation.getRobotRelativePose(),
                fieldResult.candidate, fieldResult.status);
    }

    private AprilTagObservation createRelativeObservation(AprilTagSingleDetection detection) {
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

    private FieldPoseCandidateResult createFieldPoseCandidate(AprilTagSingleDetection detection) {
        String rejectionReason = getFieldPoseRejectionReason(detection);
        if (rejectionReason != null) {
            return FieldPoseCandidateResult.rejected(rejectionReason);
        }

        Position robotPosition = detection.robotPose.getPosition();
        FieldPose fieldPose = new FieldPose(localizationConfiguration.getFieldFrameName(),
                robotPosition.x, robotPosition.y, robotPosition.z,
                detection.robotPose.getOrientation().getPitch(AngleUnit.DEGREES),
                detection.robotPose.getOrientation().getRoll(AngleUnit.DEGREES),
                detection.robotPose.getOrientation().getYaw(AngleUnit.DEGREES));
        return FieldPoseCandidateResult.accepted(new AprilTagFieldPoseCandidate(
                detection.id, detection.frameAcquisitionNanoTime, fieldPose));
    }

    private String getFieldPoseRejectionReason(AprilTagSingleDetection detection) {
        if (localizationConfiguration == null) {
            return "Rejected: no fixed-tag localization configuration is selected.";
        }
        if (detection.frameAcquisitionNanoTime <= 0) {
            return "Rejected: frame acquisition timestamp is invalid.";
        }
        long ageNanos = System.nanoTime() - detection.frameAcquisitionNanoTime;
        if (ageNanos < 0 || ageNanos > localizationConfiguration.getMaxCandidateAgeNanos()) {
            return "Rejected: fresh detection is older than the configured candidate limit.";
        }
        if (!localizationConfiguration.allowsTagId(detection.id)) {
            return "Rejected: tag is not a configured fixed-field localization tag.";
        }
        if (!hasVerifiedFixedTagMetadata(detection)) {
            return "Rejected: fixed-tag metadata is missing or does not match the SDK entry.";
        }
        if (!configuration.isCalibrationVerified()
                || configuration.getStreamWidth() != 640
                || configuration.getStreamHeight() != 480) {
            return "Rejected: matching C920 640-by-480 calibration is not verified.";
        }
        if (!canConfigureSdkCameraPose()) {
            return "Rejected: the verified camera mount cannot be supplied to the SDK.";
        }
        if (detection.robotPose == null) {
            return "Rejected: the FTC processor supplied no robot field pose.";
        }
        Position position = detection.robotPose.getPosition();
        double pitch = detection.robotPose.getOrientation().getPitch(AngleUnit.DEGREES);
        double roll = detection.robotPose.getOrientation().getRoll(AngleUnit.DEGREES);
        double yaw = detection.robotPose.getOrientation().getYaw(AngleUnit.DEGREES);
        if (position == null || !allFinite(position.x, position.y, position.z,
                pitch, roll, yaw)) {
            return "Rejected: the FTC robot field pose contains a non-finite value.";
        }
        return null;
    }

    private boolean hasVerifiedFixedTagMetadata(AprilTagSingleDetection detection) {
        AprilTagMetadata metadata = detection.metadata;
        if (metadata == null || metadata.id != detection.id || metadata.distanceUnit == null
                || metadata.fieldPosition == null || metadata.fieldOrientation == null) {
            return false;
        }
        String expectedName = detection.id == 20 ? "BlueTarget"
                : detection.id == 24 ? "RedTarget" : "";
        double tagSizeInches = DistanceUnit.INCH.fromUnit(
                metadata.distanceUnit, metadata.tagsize);
        return expectedName.equals(metadata.name) && isFinite(tagSizeInches)
                && Math.abs(tagSizeInches - VERIFIED_TAG_SIZE_INCHES) <= COMPARISON_TOLERANCE;
    }

    private boolean canConfigureSdkCameraPose() {
        return localizationConfiguration != null && configuration.isMountVerified()
                && isZero(configuration.getMountYawDegrees())
                && isZero(configuration.getMountPitchDegrees())
                && isZero(configuration.getMountRollDegrees());
    }

    private String getPoseUnavailableReason(AprilTagSingleDetection detection) {
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
                    observation.getTimestampNanos(), RETAINED_ID_ONLY_QUALITY,
                    null, null, null,
                    "Rejected: snapshot is retained rather than a fresh camera frame."));
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

    private static final class FieldPoseCandidateResult {
        private final AprilTagFieldPoseCandidate candidate;
        private final String status;

        private FieldPoseCandidateResult(AprilTagFieldPoseCandidate candidate, String status) {
            this.candidate = candidate;
            this.status = status;
        }

        private static FieldPoseCandidateResult accepted(AprilTagFieldPoseCandidate candidate) {
            return new FieldPoseCandidateResult(candidate, FIELD_POSE_ACCEPTED);
        }

        private static FieldPoseCandidateResult rejected(String reason) {
            return new FieldPoseCandidateResult(null, reason);
        }
    }
}
