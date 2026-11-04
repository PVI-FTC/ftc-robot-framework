package org.firstinspires.ftc.teamcode.robots.team3;

import com.qualcomm.robotcore.hardware.HardwareMap;
import org.firstinspires.ftc.teamcode.common.hardware.IntakeHardware;
import org.firstinspires.ftc.teamcode.common.hardware.VisionHardware;
import org.firstinspires.ftc.teamcode.common.localization.PoseEstimate;
import org.firstinspires.ftc.teamcode.common.subsystems.drive.DriveSubsystem;
import org.firstinspires.ftc.teamcode.common.subsystems.intake.IntakeSubsystem;
import org.firstinspires.ftc.teamcode.common.subsystems.vision.VisionSubsystem;
import org.firstinspires.ftc.teamcode.common.vision.AprilTagObservation;
import org.firstinspires.ftc.teamcode.common.vision.AprilTagObservationSnapshot;
import org.firstinspires.ftc.teamcode.core.robot.Robot;

import java.util.List;

/** Separate Team 3 Pedro pilot composition. It never initializes the baseline DriveHardware wrapper. */
public class Team3PedroRobot extends Robot {
    private final Team3PedroConfiguration configuration;
    private final Team3PedroFollowerFactory followerFactory;
    private final Team3PedroDriveController driveController;
    private final DriveSubsystem driveSubsystem;
    private final IntakeHardware intakeHardware = new IntakeHardware();
    private final IntakeSubsystem intakeSubsystem;
    private final Team3MechanismHardware mechanismHardware = new Team3MechanismHardware();
    private final Team3IntakeDeploySubsystem intakeDeploySubsystem;
    private final Team3OuttakeSubsystem outtakeSubsystem;
    private final Team3MidtakeSubsystem midtakeSubsystem;
    private final VisionHardware visionHardware;
    private final VisionSubsystem visionSubsystem;
    private boolean hardwareInitialized;

    public Team3PedroRobot() {
        this(Team3PedroConfiguration.recordedTeam3Configuration(), new Team3PedroFollowerFactory());
    }

    public Team3PedroRobot(Team3PedroConfiguration configuration, Team3PedroFollowerFactory followerFactory) {
        if (configuration == null || followerFactory == null) {
            throw new IllegalArgumentException("Team 3 Pedro robot needs configuration and follower factory.");
        }
        this.configuration = configuration;
        this.followerFactory = followerFactory;
        driveController = new Team3PedroDriveController();
        driveSubsystem = new DriveSubsystem(driveController);
        intakeSubsystem = new IntakeSubsystem(intakeHardware);
        intakeDeploySubsystem = new Team3IntakeDeploySubsystem(mechanismHardware);
        outtakeSubsystem = new Team3OuttakeSubsystem(mechanismHardware);
        midtakeSubsystem = new Team3MidtakeSubsystem(mechanismHardware);
        visionHardware = Team3VisionConfiguration.createVisionHardware();
        visionSubsystem = new VisionSubsystem(visionHardware);
        registerSubsystem(driveSubsystem);
        registerSubsystem(intakeSubsystem);
        registerSubsystem(intakeDeploySubsystem);
        registerSubsystem(outtakeSubsystem);
        registerSubsystem(midtakeSubsystem);
        registerSubsystem(visionSubsystem);
    }

    public void initialize(HardwareMap hardwareMap) {
        if (hardwareInitialized) return;
        configuration.requireConfigured();
        intakeHardware.initialize(hardwareMap);
        mechanismHardware.initialize(hardwareMap);
        visionHardware.initialize(hardwareMap);
        driveController.setFollower(followerFactory.create(hardwareMap, configuration));
        hardwareInitialized = true;
        super.initialize();
    }

    public void drive(double forward, double strafe, double rotate) { driveSubsystem.drive(forward, strafe, rotate); }
    public void enableManualDrive() {
        driveSubsystem.disableDrive();
        configuration.requireRestrictedManualDriveReady();
        driveSubsystem.enableManualDrive();
    }
    /** Sets the localizer to the selected path's declared start while keeping drive disabled. */
    public void preparePath(Team3PedroPathRoute route) {
        driveSubsystem.disableDrive();
        configuration.requirePathFollowingReady();
        driveController.preparePath(route);
    }

    /** Starts a selected Team 3 path through the existing drive-mode request seam. */
    public void startPath(Team3PedroPathRoute route) {
        configuration.requirePathFollowingReady();
        driveController.startPath(route);
        driveSubsystem.enablePathFollowing();
    }

    /** Enables updates for a path already requested through a narrow Team 3 Robot method. */
    public void enablePathFollowing() {
        driveSubsystem.disableDrive();
        configuration.requirePathFollowingReady();
        driveSubsystem.enablePathFollowing();
    }
    public void cancelPathFollowing() { driveSubsystem.cancelPathFollowing(); }
    public void disableDrive() { driveSubsystem.disableDrive(); }
    public boolean isPathFollowingActive() { return driveSubsystem.isPathFollowingActive(); }
    public boolean isPathFollowingComplete() { return !driveSubsystem.isPathFollowingActive(); }
    public PoseEstimate getPoseEstimate() { return driveSubsystem.getPoseEstimate(); }
    public String getDriveStateName() { return driveSubsystem.getCurrentStateName(); }
    public boolean isSafeInitializationReady() { return configuration.isSafeInitializationReady(); }
    public boolean isRestrictedManualDriveReady() { return configuration.isRestrictedManualDriveReady(); }
    public boolean isPathFollowingReady() { return configuration.isPathFollowingReady(); }

    public void startIntake() { intakeSubsystem.startIntake(); }
    public void stopIntake() { intakeSubsystem.stopIntake(); }
    public void ejectIntake() { intakeSubsystem.eject(); }
    public void holdIntake() { intakeSubsystem.hold(); }
    public void setIntakePower(double power) { intakeSubsystem.setIntakePower(power); }
    public void setIntakeEjectPower(double power) { intakeSubsystem.setEjectPower(power); }
    public void setIntakeIdlePower(double power) { intakeSubsystem.setIdlePower(power); }
    public String getIntakeStateName() { return intakeSubsystem.getCurrentStateName(); }
    public boolean isIntakeAvailable() { return intakeSubsystem.isAvailable(); }

    public void deployIntake() { intakeDeploySubsystem.deploy(); }
    public void stowIntake() { intakeDeploySubsystem.stow(); }
    public void toggleIntakeDeploy() { intakeDeploySubsystem.toggleDeploy(); }
    public String getIntakeDeployStateName() { return intakeDeploySubsystem.getCurrentStateName(); }
    public void setOuttakePower(double power) { outtakeSubsystem.setPower(power); }
    public double getOuttakePower() { return outtakeSubsystem.getPower(); }
    public String getOuttakeStateName() { return outtakeSubsystem.getCurrentStateName(); }
    public void setMidtakeFeeding(boolean feeding) { midtakeSubsystem.setFeeding(feeding); }
    public void setMidtakeReversing(boolean reversing) { midtakeSubsystem.setReversing(reversing); }
    public String getMidtakeStateName() { return midtakeSubsystem.getCurrentStateName(); }

    public void enableVision() { visionSubsystem.enableVision(); }
    public void disableVision() { visionSubsystem.disableVision(); }
    public String getVisionStateName() { return visionSubsystem.getCurrentStateName(); }
    public boolean isVisionAvailable() { return visionSubsystem.isAvailable(); }
    public List<AprilTagObservation> getAprilTagObservations() { return visionSubsystem.getLatestObservations(); }
    public AprilTagObservationSnapshot getAprilTagObservationSnapshot() { return visionSubsystem.getLatestSnapshot(); }

    /**
     * Relocalizes the Pedro 3 follower pose if a fresh fixed-goal AprilTag field-pose candidate
     * (ID 20 Blue or ID 24 Red) is currently observed.
     *
     * @return the AprilTag ID used to relocalize, or {@code -1} if no valid field-pose candidate is available
     */
    public int relocalizeFromAprilTag() {
        for (AprilTagObservation obs : getAprilTagObservations()) {
            if (obs.isFieldPoseCandidateAvailable() && obs.getFieldPoseCandidate() != null) {
                org.firstinspires.ftc.teamcode.common.localization.FieldPose fieldPose =
                        obs.getFieldPoseCandidate().getFieldPose();
                driveController.setPose(
                        fieldPose.getXInches(),
                        fieldPose.getYInches(),
                        Math.toRadians(fieldPose.getYawDegrees()));
                return obs.getTagId();
            }
        }
        return -1;
    }

    @Override protected void onStop() {
        driveController.stop();
        intakeHardware.stop();
        mechanismHardware.stop();
        visionHardware.stop();
    }
}
