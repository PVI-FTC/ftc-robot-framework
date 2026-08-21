package org.firstinspires.ftc.teamcode.robots.teamA;

import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.teamcode.common.hardware.VisionHardware;
import org.firstinspires.ftc.teamcode.common.subsystems.vision.VisionSubsystem;
import org.firstinspires.ftc.teamcode.common.vision.AprilTagObservation;
import org.firstinspires.ftc.teamcode.common.vision.AprilTagObservationSnapshot;
import org.firstinspires.ftc.teamcode.core.robot.Robot;

import java.util.List;

/** Separate, stationary Team A AprilTag pilot with no drivetrain or localization behavior. */
public class TeamAAprilTagVisionRobot extends Robot {
    private final VisionHardware visionHardware;
    private final VisionSubsystem visionSubsystem;
    private boolean hardwareInitialized;

    /** Creates the pilot for one configured Logitech/UVC webcam hardware name. */
    public TeamAAprilTagVisionRobot(String webcamHardwareName) {
        visionHardware = new VisionHardware(webcamHardwareName);
        visionSubsystem = new VisionSubsystem(visionHardware);
        registerSubsystem(visionSubsystem);
    }

    /** Initializes the optional camera source, then the vision subsystem lifecycle. */
    public void initialize(HardwareMap hardwareMap) {
        if (hardwareInitialized) {
            return;
        }
        visionHardware.initialize(hardwareMap);
        hardwareInitialized = true;
        super.initialize();
    }

    public void enableVision() {
        visionSubsystem.enableVision();
    }

    public void disableVision() {
        visionSubsystem.disableVision();
    }

    public String getVisionStateName() {
        return visionSubsystem.getCurrentStateName();
    }

    public boolean isVisionAvailable() {
        return visionSubsystem.isAvailable();
    }

    /** Returns the latest immutable, library-neutral AprilTag observations. */
    public List<AprilTagObservation> getAprilTagObservations() {
        return visionSubsystem.getLatestObservations();
    }

    /** Returns observations plus their neutral fresh, retained, or unavailable status. */
    public AprilTagObservationSnapshot getAprilTagObservationSnapshot() {
        return visionSubsystem.getLatestSnapshot();
    }

    @Override
    protected void onStop() {
        visionHardware.stop();
    }
}
