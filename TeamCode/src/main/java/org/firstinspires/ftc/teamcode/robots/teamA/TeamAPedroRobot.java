package org.firstinspires.ftc.teamcode.robots.teamA;

import com.qualcomm.robotcore.hardware.HardwareMap;
import org.firstinspires.ftc.teamcode.common.localization.PoseEstimate;
import org.firstinspires.ftc.teamcode.common.subsystems.drive.DriveSubsystem;
import org.firstinspires.ftc.teamcode.core.robot.Robot;

/** Separate Team A Pedro pilot composition. It never initializes the baseline DriveHardware wrapper. */
public class TeamAPedroRobot extends Robot {
    private final TeamAPedroConfiguration configuration;
    private final TeamAPedroFollowerFactory followerFactory;
    private final TeamAPedroDriveController driveController;
    private final DriveSubsystem driveSubsystem;
    private boolean hardwareInitialized;

    public TeamAPedroRobot() { this(TeamAPedroConfiguration.unconfigured(), new TeamAPedroFollowerFactory()); }
    public TeamAPedroRobot(TeamAPedroConfiguration configuration, TeamAPedroFollowerFactory followerFactory) {
        if (configuration == null || followerFactory == null) throw new IllegalArgumentException("Team A Pedro robot needs configuration and follower factory.");
        this.configuration = configuration;
        this.followerFactory = followerFactory;
        driveController = new TeamAPedroDriveController();
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
    public void enableManualDrive() { driveSubsystem.enableManualDrive(); }
    /** No Team A path exists in LP-06, so this leaves drivetrain output disabled. */
    public void enablePathFollowing() { driveSubsystem.cancelPathFollowing(); }
    public void cancelPathFollowing() { driveSubsystem.cancelPathFollowing(); }
    public void disableDrive() { driveSubsystem.disableDrive(); }
    public boolean isPathFollowingActive() { return driveSubsystem.isPathFollowingActive(); }
    public boolean isPathFollowingComplete() { return !driveSubsystem.isPathFollowingActive(); }
    public PoseEstimate getPoseEstimate() { return driveSubsystem.getPoseEstimate(); }
    public String getDriveStateName() { return driveSubsystem.getCurrentStateName(); }
}