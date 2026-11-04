package org.firstinspires.ftc.teamcode.KimCode;

import com.pedropathing.math.Pose;
import com.qualcomm.hardware.limelightvision.LLResult;
import com.qualcomm.hardware.limelightvision.LLResultTypes;
import com.qualcomm.hardware.limelightvision.Limelight3A;
import com.qualcomm.robotcore.hardware.HardwareMap;

import java.util.List;

/**
 * Reads the HIVE's AprilTags from the Limelight.
 *
 * <p>If the Limelight is missing from the robot configuration, this class reports nothing instead
 * of failing, so the robot can still finish its autonomous and park.</p>
 */
final class KimHiveVision {
    private static final String LIMELIGHT_NAME = "limelight";
    /** PLACEHOLDER: set this to the Limelight pipeline that detects 36h11 AprilTags. */
    private static final int APRILTAG_PIPELINE = 0;
    /** Results older than this describe where the robot was, not where it is. */
    private static final long MAX_RESULT_AGE_MILLISECONDS = 250;
    static final int TAGS_PER_CELL = 4;

    /** What the camera sees of one CELL's four tags in the latest frame. */
    static final class CellView {
        private int tagCount;
        private double targetXDegreesSum;
        private double targetYDegreesSum;

        private void clear() {
            tagCount = 0;
            targetXDegreesSum = 0.0;
            targetYDegreesSum = 0.0;
        }

        private void add(double targetXDegrees, double targetYDegrees) {
            tagCount++;
            targetXDegreesSum += targetXDegrees;
            targetYDegreesSum += targetYDegrees;
        }

        int tagCount() {
            return tagCount;
        }

        boolean isVisible() {
            return tagCount > 0;
        }

        /** Average sideways angle to the tags. Positive means right of the crosshair. */
        double targetXDegrees() {
            return tagCount == 0 ? 0.0 : targetXDegreesSum / tagCount;
        }

        /** Average vertical angle to the tags. Positive means above the crosshair. */
        double targetYDegrees() {
            return tagCount == 0 ? 0.0 : targetYDegreesSum / tagCount;
        }
    }

    private final Limelight3A limelight;
    private final HiveCellFinder finder = new HiveCellFinder();
    private final CellView startRaisedCell = new CellView();
    private final CellView startLoweredCell = new CellView();
    private HiveField.Alliance alliance;
    private long lastResultNanos;

    KimHiveVision(HardwareMap hardwareMap, HiveField.Alliance alliance) {
        limelight = hardwareMap.tryGet(Limelight3A.class, LIMELIGHT_NAME);
        this.alliance = alliance;
    }

    boolean isAvailable() {
        return limelight != null;
    }

    void start() {
        if (limelight != null) {
            limelight.pipelineSwitch(APRILTAG_PIPELINE);
            limelight.start();
        }
    }

    void stop() {
        if (limelight != null) {
            limelight.stop();
        }
    }

    /** Changes which HIVE to look at, and forgets what was seen of the old one. */
    void setAlliance(HiveField.Alliance alliance) {
        this.alliance = alliance;
        finder.reset();
    }

    /** Reads the newest camera frame. Call once per loop while looking at the HIVE. */
    void update(Pose robotPose) {
        LLResult result = limelight == null ? null : limelight.getLatestResult();
        if (result == null || !result.isValid()
                || result.getStaleness() > MAX_RESULT_AGE_MILLISECONDS) {
            startRaisedCell.clear();
            startLoweredCell.clear();
            return;
        }
        // The Limelight sends frames more slowly than this loop runs. Count each frame once.
        if (result.getControlHubTimeStampNanos() == lastResultNanos) {
            return;
        }
        lastResultNanos = result.getControlHubTimeStampNanos();

        readTags(result.getFiducialResults());
        addVote(true, startRaisedCell, robotPose);
        addVote(false, startLoweredCell, robotPose);
    }

    private void readTags(List<LLResultTypes.FiducialResult> tags) {
        int startRaisedFirstId = HiveField.startRaisedFirstTagId(alliance);
        int startLoweredFirstId = HiveField.startLoweredFirstTagId(alliance);
        startRaisedCell.clear();
        startLoweredCell.clear();
        for (LLResultTypes.FiducialResult tag : tags) {
            int id = tag.getFiducialId();
            if (id >= startRaisedFirstId && id < startRaisedFirstId + TAGS_PER_CELL) {
                startRaisedCell.add(tag.getTargetXDegrees(), tag.getTargetYDegrees());
            } else if (id >= startLoweredFirstId && id < startLoweredFirstId + TAGS_PER_CELL) {
                startLoweredCell.add(tag.getTargetXDegrees(), tag.getTargetYDegrees());
            }
        }
    }

    private void addVote(boolean isStartRaisedCell, CellView cell, Pose robotPose) {
        if (cell.isVisible()) {
            finder.addReading(isStartRaisedCell,
                    distanceToCellInches(isStartRaisedCell, robotPose), cell.targetYDegrees());
        }
    }

    /** Level distance from the camera to a CELL's tags, using the robot's Pinpoint position. */
    double distanceToCellInches(boolean isStartRaisedCell, Pose robotPose) {
        double cameraX = robotPose.x()
                + HiveField.CAMERA_FORWARD_INCHES * Math.cos(robotPose.heading());
        double cameraY = robotPose.y()
                + HiveField.CAMERA_FORWARD_INCHES * Math.sin(robotPose.heading());
        return HiveField.distanceToCellInches(alliance, isStartRaisedCell, cameraX, cameraY);
    }

    CellView startRaisedCell() {
        return startRaisedCell;
    }

    CellView startLoweredCell() {
        return startLoweredCell;
    }

    HiveCellFinder finder() {
        return finder;
    }
}
