package org.firstinspires.ftc.teamcode.robots.team3;

import org.firstinspires.ftc.teamcode.common.hardware.AprilTagCameraConfiguration;
import org.firstinspires.ftc.teamcode.common.hardware.VisionHardware;
import org.firstinspires.ftc.teamcode.common.localization.AprilTagLocalizationConfiguration;

/**
 * Team 3 AprilTag camera, physical mounting, and HIVE goal anchor configuration.
 *
 * <p>Centralizes Team 3's webcam hardware name, physical camera mounting offsets, and the 4 HIVE
 * goal TeleOp shooting anchor parameters so both {@link Team3Robot} and {@link Team3PedroRobot}
 * share the same vision setup. If the webcam is not plugged in or not in the active hardware map,
 * {@link VisionHardware} fails safely without affecting drivetrain or mechanism operation.</p>
 */
public final class Team3VisionConfiguration {
    /** Default webcam name in the Driver Station hardware configuration. */
    public static final String DEFAULT_WEBCAM_NAME = "Webcam 1";

    public static final String ROBOT_FRAME_NAME =
            "Team 3 robot frame: drivetrain center floor origin, +X right, +Y forward, +Z up";
    public static final int STREAM_WIDTH = 640;
    public static final int STREAM_HEIGHT = 480;

    // =========================================================================
    // TODO: ENTER PHYSICAL WEBCAM MOUNTING OFFSETS HERE ONCE MEASURED ON ROBOT
    // Origin is the center of the robot drivetrain at floor level:
    //   +RIGHT   = inches to the right of robot center (negative = left)
    //   +FORWARD = inches forward of robot center (negative = behind center)
    //   +UP      = inches above the floor
    //   +YAW     = degrees rotated counter-clockwise / left (0 = facing straight ahead)
    //   +PITCH   = degrees tilted upward above horizontal (0 = level with floor)
    //   +ROLL    = degrees rolled clockwise
    // =========================================================================
    public static final double CAMERA_RIGHT_INCHES = 6.0;
    public static final double CAMERA_FORWARD_INCHES = 1.5;
    public static final double CAMERA_UP_INCHES = 11.5;
    public static final double CAMERA_YAW_DEGREES = 0.0;
    public static final double CAMERA_PITCH_DEGREES = 25.0;
    public static final double CAMERA_ROLL_DEGREES = 0.0;

    // =========================================================================
    // TELEOP HIVE GOAL SNAP ANCHOR DEFAULTS (Used by all 4 HIVE Goals in Team3HiveGoal)
    //   DEFAULT_GOAL_DISTANCE_INCHES: desired standoff distance from the center of the HIVE goal
    //   DEFAULT_GOAL_LATERAL_OFFSET_INCHES: lateral shift (+right / -left) if shooter is off-center
    //   DEFAULT_GOAL_HEADING_OFFSET_DEGREES: angular offset (+CCW / -CW) when aiming at the goal
    // =========================================================================
    public static final double DEFAULT_GOAL_DISTANCE_INCHES = 36.0;
    public static final double DEFAULT_GOAL_LATERAL_OFFSET_INCHES = 0.0;
    public static final double DEFAULT_GOAL_HEADING_OFFSET_DEGREES = 0.0;

    /** Fixed AprilTag IDs for all 4 BIOBUZZ HIVE Goals (30-45) plus DECODE Goals (20, 24). */
    private static final int[] ALL_GOAL_TAG_IDS = {
            20, 24,
            30, 31, 32, 33,
            34, 35, 36, 37,
            38, 39, 40, 41,
            42, 43, 44, 45
    };

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
                CAMERA_YAW_DEGREES,
                CAMERA_PITCH_DEGREES,
                CAMERA_ROLL_DEGREES);
        AprilTagLocalizationConfiguration localizationConfig = new AprilTagLocalizationConfiguration(
                "BIOBUZZ 4 HIVE Goals + DECODE Goal tags",
                ALL_GOAL_TAG_IDS,
                AprilTagLocalizationConfiguration.FTC_FIELD_FRAME_NAME,
                AprilTagLocalizationConfiguration.DEFAULT_MAX_CANDIDATE_AGE_NANOS);
        return VisionHardware.createMultiTagVisionHardware(cameraConfig, localizationConfig);
    }
}
