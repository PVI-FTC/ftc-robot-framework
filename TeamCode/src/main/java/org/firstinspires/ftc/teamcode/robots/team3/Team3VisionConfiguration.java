package org.firstinspires.ftc.teamcode.robots.team3;

import org.firstinspires.ftc.teamcode.common.hardware.AprilTagCameraConfiguration;
import org.firstinspires.ftc.teamcode.common.hardware.VisionHardware;
import org.firstinspires.ftc.teamcode.common.localization.AprilTagLocalizationConfiguration;

/**
 * Team 3 AprilTag camera and mounting configuration.
 *
 * <p>Centralizes Team 3's webcam hardware name and physical camera mounting offsets
 * so both {@link Team3Robot} and {@link Team3PedroRobot} can share the same vision setup.
 * If the webcam is not plugged in or not in the active hardware map, {@link VisionHardware}
 * fails safely without affecting drivetrain or mechanism operation.</p>
 */
public final class Team3VisionConfiguration {
    /** Default webcam name in the Driver Station hardware configuration. */
    public static final String DEFAULT_WEBCAM_NAME = "Webcam 1";

    public static final String ROBOT_FRAME_NAME =
            "Team 3 robot frame: drivetrain center floor origin, +X right, +Y forward, +Z up";
    public static final int STREAM_WIDTH = 640;
    public static final int STREAM_HEIGHT = 480;

    /** Camera mounting offsets from robot center in inches (tune when camera mount is finalized). */
    public static final double CAMERA_RIGHT_INCHES = 0.0;
    public static final double CAMERA_FORWARD_INCHES = 6.0;
    public static final double CAMERA_UP_INCHES = 12.0;

    private Team3VisionConfiguration() {
        // Utility class
    }

    /** Creates a configured {@link VisionHardware} wrapper for Team 3's default webcam. */
    public static VisionHardware createVisionHardware() {
        return createVisionHardware(DEFAULT_WEBCAM_NAME);
    }

    /** Creates a configured {@link VisionHardware} wrapper for the specified webcam hardware name. */
    public static VisionHardware createVisionHardware(String webcamHardwareName) {
        AprilTagCameraConfiguration cameraConfig = new AprilTagCameraConfiguration(
                webcamHardwareName,
                STREAM_WIDTH,
                STREAM_HEIGHT,
                true,
                true,
                ROBOT_FRAME_NAME,
                CAMERA_RIGHT_INCHES,
                CAMERA_FORWARD_INCHES,
                CAMERA_UP_INCHES,
                0.0,
                0.0,
                0.0);
        return new VisionHardware(cameraConfig, AprilTagLocalizationConfiguration.decodeGoalTags());
    }
}

