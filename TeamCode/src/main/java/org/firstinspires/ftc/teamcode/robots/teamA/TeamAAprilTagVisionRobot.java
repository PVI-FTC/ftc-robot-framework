package org.firstinspires.ftc.teamcode.robots.teamA;

import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.teamcode.common.hardware.VisionHardware;
import org.firstinspires.ftc.teamcode.common.subsystems.vision.VisionSubsystem;
import org.firstinspires.ftc.teamcode.common.vision.AprilTagObservation;
import org.firstinspires.ftc.teamcode.core.robot.Robot;

import java.util.List;

/** Separate Team A composition for the stationary AprilTag observation pilot. */
public class TeamAAprilTagVisionRobot extends Robot {
    public static final String LOGITECH_VISION_WEBCAM_NAME = "logitechVisionWebcam";

    private final VisionHardware visionHardware;
    private final VisionSubsystem visionSubsystem;
    private boolean hardwareInitialized;

    public TeamAAprilTagVisionRobot() {
        this(new VisionHardware(LOGITECH_VISION_WEBCAM_NAME));
    }

    TeamAAprilTagVisionRobot(VisionHardware visionHardware) {
        if (visionHardware == null) {
            throw new IllegalArgumentException("AprilTag vision robot needs vision hardware.");
        }
        this.visionHardware = visionHardware;
        visionSubsystem = new VisionSubsystem(visionHardware);
        registerSubsystem(visionSubsystem);
    }

    /** Initializes only the optional vision hardware and its subsystem lifecycle. */
    public void initialize(HardwareMap hardwareMap) {
        if (hardwareInitialized) {
            return;
        }
        visionHardware.initialize(hardwareMap);
        hardwareInitialized = true;
        super.initialize();
    }

    public void enableVision() { visionSubsystem.enableVision(); }
    public void disableVision() { visionSubsystem.disableVision(); }
    public boolean isVisionAvailable() { return visionSubsystem.isAvailable(); }
    public String getVisionStateName() { return visionSubsystem.getCurrentStateName(); }
    public List<AprilTagObservation> getAprilTagObservations() {
        return visionSubsystem.getLatestObservations();
    }
}
