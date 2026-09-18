package org.firstinspires.ftc.teamcode.robots.team3;

import com.qualcomm.robotcore.hardware.HardwareMap;
import org.firstinspires.ftc.teamcode.common.localization.PoseEstimate;
import org.firstinspires.ftc.teamcode.common.subsystems.drive.DriveSubsystem;
import org.firstinspires.ftc.teamcode.core.robot.Robot;

/** Separate Team 3 Pedro pilot composition. It never initializes the baseline DriveHardware wrapper. */
public class Team3PedroRobot extends Robot {
    private final Team3PedroConfiguration configuration;
    private final Team3PedroFollowerFactory followerFactory;
    private final Team3PedroDriveController driveController;
    private final DriveSubsystem driveSubsystem;
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
        registerSubsystem(driveSubsystem);
    }
    public void initialize(HardwareMap hardwareMap) {
        if (hardwareInitialized) return;
        configuration.requireConfigured();
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
    @Override protected void onStop() { driveController.stop(); }
}
