package org.firstinspires.ftc.teamcode.robots.team3;

import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.teamcode.common.autonomous.AutonomousRobotControl;
import org.firstinspires.ftc.teamcode.common.hardware.DriveHardware;
import org.firstinspires.ftc.teamcode.common.hardware.RobotHardware;
import org.firstinspires.ftc.teamcode.common.subsystems.drive.DriveSubsystem;
import org.firstinspires.ftc.teamcode.common.subsystems.intake.IntakeSubsystem;
import org.firstinspires.ftc.teamcode.common.subsystems.vision.VisionSubsystem;
import org.firstinspires.ftc.teamcode.core.robot.Robot;

import java.util.List;

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
    private final Team3StabilizedDriveController driveController;
    private final DriveSubsystem driveSubsystem;
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
        driveController = new Team3StabilizedDriveController(robotHardware.getDriveHardware(), pinpointHardware);
        driveSubsystem = new DriveSubsystem(driveController);
        intakeSubsystem = new IntakeSubsystem(robotHardware.getIntakeHardware());
        intakeDeploySubsystem = new Team3IntakeDeploySubsystem(mechanismHardware);
        midtakeSubsystem = new Team3MidtakeSubsystem(mechanismHardware);
        outtakeSubsystem = new Team3OuttakeSubsystem(mechanismHardware);
        visionSubsystem = new VisionSubsystem(robotHardware.getVisionHardware());
        registerSubsystem(driveSubsystem);
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

        // Enable Lynx bulk caching mode AUTO to minimize I2C/CAN bus latency and maximize loop speed
        try {
            List<LynxModule> allHubs = hardwareMap.getAll(LynxModule.class);
            for (LynxModule hub : allHubs) {
                hub.setBulkCachingMode(LynxModule.BulkCachingMode.AUTO);
            }
        } catch (Exception ignored) {
            // Safe fallback if running in a mock environment
        }

        robotHardware.initialize(hardwareMap);
        mechanismHardware.initialize(hardwareMap);
        pinpointHardware.initialize(hardwareMap);
        hardwareInitialized = true;
        super.initialize();
    }

    /** Requests mecanum drive values for the next robot update. */
    public void drive(double forward, double strafe, double rotate) {
        driveSubsystem.drive(forward, strafe, rotate);
    }

    /**
     * Requests zero drive power and resets the pose-lock anchor to current position.
     */
    public void brake() {
        driveController.brake();
        driveSubsystem.drive(0.0, 0.0, 0.0);
    }

    /** Requests drivetrain behavior that applies the stored drive values. */
    public void enableRequestedDrive() {
        driveSubsystem.enableRequestedDrive();
    }

    /**
     * Requests normal manual drivetrain behavior.
     */
    public void enableManualDrive() {
        driveController.getPoseLock().setAssistLevel(Team3DriveAssistLevel.OFF);
        enableRequestedDrive();
    }

    /** Requests the safe disabled drivetrain behavior. */
    public void disableDrive() {
        driveSubsystem.disableDrive();
    }

    /** Requests heading hold behavior (corrects rotation only). */
    public void enableHeadingHold() {
        driveController.getPoseLock().setAssistLevel(Team3DriveAssistLevel.HEADING);
        driveSubsystem.enableHeadingHold();
    }

    /** Sets the drive assist level (OFF, HEADING, or FULL). */
    public void setDriveAssistLevel(Team3DriveAssistLevel level) {
        driveController.getPoseLock().setAssistLevel(level);
        if (level == Team3DriveAssistLevel.OFF) {
            driveSubsystem.enableManualDrive();
        } else {
            driveSubsystem.enableHeadingHold();
        }
    }

    /** Returns current drive assist level. */
    public Team3DriveAssistLevel getDriveAssistLevel() {
        return driveController.getPoseLock().getAssistLevel();
    }

    /** Toggles XY pose lock between HEADING (direction holding only) and FULL (direction + XY hold). */
    public Team3DriveAssistLevel toggleDriveAssist() {
        if (getDriveAssistLevel() == Team3DriveAssistLevel.FULL) {
            setDriveAssistLevel(Team3DriveAssistLevel.HEADING);
        } else {
            setDriveAssistLevel(Team3DriveAssistLevel.FULL);
            if (pinpointHardware.isAvailable()) {
                driveController.getPoseLock().resetAnchor(
                        pinpointHardware.getXInches(),
                        pinpointHardware.getYInches(),
                        pinpointHardware.getHeadingRadians());
            }
        }
        return getDriveAssistLevel();
    }

    /** Returns whether full XY pose lock is currently active (FULL level). */
    public boolean isPoseLockActive() {
        return driveController.getPoseLock().getAssistLevel() == Team3DriveAssistLevel.FULL;
    }

    /** Cycles drive assist level: OFF -> HEADING -> FULL -> OFF. */
    public Team3DriveAssistLevel cycleDriveAssistLevel() {
        Team3DriveAssistLevel nextLevel = driveController.getPoseLock().getAssistLevel().next();
        setDriveAssistLevel(nextLevel);
        return nextLevel;
    }

    /**
     * Snaps the heading hold target to a specified angle in degrees and activates heading hold.
     *
     * @param headingDegrees target angle in degrees (e.g. 0 forward, 90 left, 180 back, -90 right)
     */
    public void snapToHeading(double headingDegrees) {
        driveController.getPoseLock().snapToHeading(
                Math.toRadians(headingDegrees),
                pinpointHardware.getXInches(),
                pinpointHardware.getYInches());
        driveSubsystem.enableHeadingHold();
    }

    /** Returns whether heading hold or full pose stabilization is currently active. */
    public boolean isHeadingHoldActive() {
        return driveController.getPoseLock().getAssistLevel() != Team3DriveAssistLevel.OFF;
    }

    /** Returns current X position in inches reported by Pinpoint. */
    public double getXInches() {
        return pinpointHardware.getXInches();
    }

    /** Returns current Y position in inches reported by Pinpoint. */
    public double getYInches() {
        return pinpointHardware.getYInches();
    }

    /** Returns locked target X in inches when assist is active. */
    public double getTargetXInches() {
        return driveController.getPoseLock().getTargetXInches();
    }

    /** Returns locked target Y in inches when assist is active. */
    public double getTargetYInches() {
        return driveController.getPoseLock().getTargetYInches();
    }

    /** Returns current heading in degrees reported by Pinpoint. */
    public double getHeadingDegrees() {
        return pinpointHardware.getHeadingDegrees();
    }

    /** Returns locked target heading in degrees when assist is active. */
    public double getTargetHeadingDegrees() {
        return driveController.getPoseLock().getTargetHeadingDegrees();
    }

    /** Returns current translational error in inches for telemetry. */
    public double getPoseLockErrorInches() {
        return driveController.getPoseLock().getErrorInches();
    }

    /** Returns current heading error in degrees for telemetry. */
    public double getPoseLockHeadingErrorDegrees() {
        return driveController.getPoseLock().getHeadingErrorDegrees();
    }

    /** Returns active pose lock translation state name for telemetry. */
    public String getPoseLockTranslationStateName() {
        return driveController.getPoseLock().getTranslationStateName();
    }

    /** Returns current physical speed in inches per second. */
    public double getRobotSpeedIps() {
        return driveController.getPoseLock().getRobotSpeedIps();
    }

    /** Returns the loop execution time in milliseconds. */
    public double getLoopTimeMs() {
        return driveController.getLastLoopTimeMs();
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

    /** Sets the forward intake power (0.0 to 1.0). */
    public void setIntakePower(double power) {
        intakeSubsystem.setIntakePower(power);
    }

    /** Sets the reverse eject intake power (0.0 to 1.0). */
    public void setIntakeEjectPower(double power) {
        intakeSubsystem.setEjectPower(power);
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

    /** Requests that the midtake reverse (true) or stop (false). */
    public void setMidtakeReversing(boolean reversing) {
        midtakeSubsystem.setReversing(reversing);
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
