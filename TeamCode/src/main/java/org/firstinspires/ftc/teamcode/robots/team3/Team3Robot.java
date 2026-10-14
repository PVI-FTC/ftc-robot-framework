package org.firstinspires.ftc.teamcode.robots.team3;

import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.teamcode.common.autonomous.AutonomousRobotControl;
import org.firstinspires.ftc.teamcode.common.hardware.DriveHardware;
import org.firstinspires.ftc.teamcode.common.hardware.RobotHardware;
import org.firstinspires.ftc.teamcode.common.subsystems.drive.DriveSubsystem;
import org.firstinspires.ftc.teamcode.common.subsystems.intake.IntakeSubsystem;
import org.firstinspires.ftc.teamcode.common.subsystems.vision.VisionSubsystem;
import org.firstinspires.ftc.teamcode.core.robot.Robot;

/**
 * Team 3's robot composition and public drivetrain API.
 *
 * <p>OpModes call this robot's public methods instead of manipulating mechanisms directly. This
 * keeps FTC entry points focused on mapping controls, while the robot and subsystem layers retain
 * responsibility for behavior and hardware access.</p>
 */
public class Team3Robot extends Robot implements AutonomousRobotControl {
    private final RobotHardware robotHardware;
    private final Team3MechanismHardware mechanismHardware = new Team3MechanismHardware();
    private final Team3PinpointHardware pinpointHardware = new Team3PinpointHardware();
    private final DriveSubsystem driveSubsystem;
    private final Team3HeadingHoldSubsystem headingHoldSubsystem;
    private final IntakeSubsystem intakeSubsystem;
    private final Team3IntakeDeploySubsystem intakeDeploySubsystem;
    private final Team3MidtakeSubsystem midtakeSubsystem;
    private final Team3OuttakeSubsystem outtakeSubsystem;
    private final VisionSubsystem visionSubsystem;
    private boolean hardwareInitialized;

    /** Creates Team 3's robot with the shared baseline hardware composition. */
    public Team3Robot() {
        this(new RobotHardware());
    }

    /**
     * Creates Team 3's robot with the supplied hardware composition.
     *
     * @param robotHardware the hardware wrappers owned by this robot
     */
    public Team3Robot(RobotHardware robotHardware) {
        if (robotHardware == null) {
            throw new IllegalArgumentException("Team 3 robot needs robot hardware.");
        }

        this.robotHardware = robotHardware;
        driveSubsystem = new DriveSubsystem(robotHardware.getDriveHardware());
        headingHoldSubsystem = new Team3HeadingHoldSubsystem(pinpointHardware);
        intakeSubsystem = new IntakeSubsystem(robotHardware.getIntakeHardware());
        intakeDeploySubsystem = new Team3IntakeDeploySubsystem(mechanismHardware);
        midtakeSubsystem = new Team3MidtakeSubsystem(mechanismHardware);
        outtakeSubsystem = new Team3OuttakeSubsystem(mechanismHardware);
        visionSubsystem = new VisionSubsystem(robotHardware.getVisionHardware());
        registerSubsystem(driveSubsystem);
        registerSubsystem(headingHoldSubsystem);
        registerSubsystem(intakeSubsystem);
        registerSubsystem(intakeDeploySubsystem);
        registerSubsystem(midtakeSubsystem);
        registerSubsystem(outtakeSubsystem);
        registerSubsystem(visionSubsystem);
    }

    /**
     * Initializes hardware first, then initializes every registered subsystem once.
     *
     * @param hardwareMap the FTC hardware map supplied by the OpMode
     */
    public void initialize(HardwareMap hardwareMap) {
        if (hardwareInitialized) {
            return;
        }

        robotHardware.initialize(hardwareMap);
        mechanismHardware.initialize(hardwareMap);
        pinpointHardware.initialize(hardwareMap);
        hardwareInitialized = true;
        super.initialize();
    }

    /** Requests mecanum drive values for the next robot update. */
    public void drive(double forward, double strafe, double rotate) {
        double effectiveRotate = headingHoldSubsystem.calculateRotateCommand(rotate);
        driveSubsystem.drive(forward, strafe, effectiveRotate);
    }

    /** Requests drivetrain behavior that applies the stored drive values. */
    public void enableRequestedDrive() {
        driveSubsystem.enableRequestedDrive();
    }

    /**
     * Requests normal manual drivetrain behavior.
     *
     * <p>This TeleOp-facing name is retained for compatibility. Autonomous code should use
     * {@link #enableRequestedDrive()}.</p>
     */
    public void enableManualDrive() {
        headingHoldSubsystem.setActive(false);
        enableRequestedDrive();
    }

    /** Requests the safe disabled drivetrain behavior. */
    public void disableDrive() {
        headingHoldSubsystem.setActive(false);
        driveSubsystem.disableDrive();
    }

    /** Requests Pinpoint-backed heading hold behavior. */
    public void enableHeadingHold() {
        headingHoldSubsystem.setActive(true);
        driveSubsystem.enableHeadingHold();
    }

    /** Returns whether heading hold is currently active. */
    public boolean isHeadingHoldActive() {
        return headingHoldSubsystem.isActive();
    }

    /** Returns current heading in degrees reported by Pinpoint. */
    public double getHeadingDegrees() {
        return pinpointHardware.getHeadingDegrees();
    }

    /** Returns locked target heading in degrees when heading hold is active. */
    public double getTargetHeadingDegrees() {
        return headingHoldSubsystem.getTargetHeadingDegrees();
    }

    /** Returns whether the Pinpoint computer was found and is available. */
    public boolean isPinpointAvailable() {
        return pinpointHardware.isAvailable();
    }

    /** Requests normal forward intake behavior. */
    public void startIntake() {
        intakeSubsystem.startIntake();
    }

    /** Requests the safe stopped intake behavior. */
    public void stopIntake() {
        intakeSubsystem.stopIntake();
    }

    /** Sets a low forward power the intake keeps running while idle (0.0 = stopped). */
    public void setIntakeIdlePower(double power) {
        intakeSubsystem.setIdlePower(power);
    }

    /** Requests the baseline intake holding behavior. */
    public void holdIntake() {
        intakeSubsystem.hold();
    }

    /** Switches the intake-deploy servo between stowed and deployed. */
    public void toggleIntakeDeploy() {
        intakeDeploySubsystem.toggleDeploy();
    }

    /** Requests the deployed position for the intake-deploy servo. */
    public void deployIntake() {
        intakeDeploySubsystem.deploy();
    }

    /** Requests the stowed position for the intake-deploy servo. */
    public void stowIntake() {
        intakeDeploySubsystem.stow();
    }

    /** Requests that the midtake feed (true) or stop (false). */
    public void setMidtakeFeeding(boolean feeding) {
        midtakeSubsystem.setFeeding(feeding);
    }

    /** Requests the outtake power from 0.0 (stopped) to 1.0 (full power). */
    public void setOuttakePower(double power) {
        outtakeSubsystem.setPower(power);
    }

    /** Requests reverse intake eject behavior. */
    public void ejectIntake() {
        intakeSubsystem.eject();
    }

    /** Requests vision searching when optional vision hardware is available. */
    public void enableVision() {
        visionSubsystem.enableVision();
    }

    /** Requests the safe disabled vision behavior. */
    public void disableVision() {
        visionSubsystem.disableVision();
    }

    /** Returns the active drivetrain state name for telemetry. */
    public String getDriveStateName() {
        return driveSubsystem.getCurrentStateName();
    }

    public double getRequestedForward() {
        return driveSubsystem.getRequestedForward();
    }

    public double getRequestedStrafe() {
        return driveSubsystem.getRequestedStrafe();
    }

    public double getRequestedRotate() {
        return driveSubsystem.getRequestedRotate();
    }

    public double getFrontLeftMotorPower() {
        return getDriveHardware().getFrontLeftPower();
    }

    public double getFrontRightMotorPower() {
        return getDriveHardware().getFrontRightPower();
    }

    public double getRearLeftMotorPower() {
        return getDriveHardware().getRearLeftPower();
    }

    public double getRearRightMotorPower() {
        return getDriveHardware().getRearRightPower();
    }

    /** Returns the active intake state name for telemetry. */
    public String getIntakeStateName() {
        return intakeSubsystem.getCurrentStateName();
    }

    /** Returns whether the optional intake hardware was found during initialization. */
    public boolean isIntakeAvailable() {
        return intakeSubsystem.isAvailable();
    }

    public String getIntakeDeployStateName() {
        return intakeDeploySubsystem.getCurrentStateName();
    }

    public String getMidtakeStateName() {
        return midtakeSubsystem.getCurrentStateName();
    }

    public String getOuttakeStateName() {
        return outtakeSubsystem.getCurrentStateName();
    }

    /** Returns the commanded power for the outtake motor (0.0 to 1.0). */
    public double getOuttakePower() {
        return outtakeSubsystem.getPower();
    }

    /** Returns whether the outtake, midtake, and intake-deploy devices were all found. */
    public boolean areMechanismsAvailable() {
        return outtakeSubsystem.isAvailable() && midtakeSubsystem.isAvailable()
                && intakeDeploySubsystem.isAvailable();
    }

    /** Returns the active vision state name for telemetry. */
    public String getVisionStateName() {
        return visionSubsystem.getCurrentStateName();
    }

    /** Returns whether optional vision hardware is available. */
    public boolean isVisionAvailable() {
        return visionSubsystem.isAvailable();
    }

    @Override
    protected void onStop() {
        robotHardware.stopAll();
        mechanismHardware.stop();
    }

    private DriveHardware getDriveHardware() {
        return robotHardware.getDriveHardware();
    }
}
