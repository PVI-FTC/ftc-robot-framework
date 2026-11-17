package org.firstinspires.ftc.teamcode.robots.team4;

import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.DcMotor;

import org.firstinspires.ftc.teamcode.common.hardware.DriveHardware;
import org.firstinspires.ftc.teamcode.common.autonomous.AutonomousRobotControl;
import org.firstinspires.ftc.teamcode.common.hardware.RobotHardware;
import org.firstinspires.ftc.teamcode.common.subsystems.drive.DriveSubsystem;
import org.firstinspires.ftc.teamcode.common.subsystems.intake.IntakeSubsystem;
import org.firstinspires.ftc.teamcode.common.subsystems.vision.VisionSubsystem;
import org.firstinspires.ftc.teamcode.core.robot.Robot;
import org.firstinspires.ftc.teamcode.robots.team4.conveyor.Conveyor4Hardware;
import org.firstinspires.ftc.teamcode.robots.team4.conveyor.Conveyor4Subsystem;

/** Initial Team 4 composition using safe shared baseline components. */
public class Team4Robot extends Robot implements AutonomousRobotControl {
    private final RobotHardware hardware;
    private final DriveSubsystem drive;
    private final IntakeSubsystem intake;
    private final VisionSubsystem vision;
    private final Conveyor4Hardware conveyorHardware = new Conveyor4Hardware();
    private final Conveyor4Subsystem conveyor = new Conveyor4Subsystem(conveyorHardware);
    private boolean hardwareInitialized;

    public Team4Robot() {
        // Raised-wheel report: front-left was correct; reverse the other three motors.
        this(new RobotHardware(new DriveHardware(
                DcMotor.Direction.REVERSE, DcMotor.Direction.REVERSE,
                DcMotor.Direction.FORWARD, DcMotor.Direction.REVERSE), new Team4IntakeHardware()));
    }
    public Team4Robot(RobotHardware hardware) {
        if (hardware == null) throw new IllegalArgumentException("Team 4 robot needs robot hardware.");
        this.hardware = hardware;
        drive = new DriveSubsystem(hardware.getDriveHardware());
        intake = new IntakeSubsystem(hardware.getIntakeHardware());
        vision = new VisionSubsystem(hardware.getVisionHardware());
        registerSubsystem(drive); registerSubsystem(intake); registerSubsystem(vision);
        registerSubsystem(conveyor);
    }
    public void initialize(HardwareMap hardwareMap) {
        if (hardwareInitialized) return;
        hardware.initialize(hardwareMap);
        conveyorHardware.initialize(hardwareMap);
        hardwareInitialized = true; super.initialize();
    }
    public void drive(double forward, double strafe, double rotate) { drive.drive(forward, strafe, rotate); }
    public void enableManualDrive() { drive.enableManualDrive(); }
    public void enableRequestedDrive() { drive.enableRequestedDrive(); }
    public void disableDrive() { drive.disableDrive(); }
    public void enableHeadingHold() { drive.enableHeadingHold(); }
    public void startIntake() { intake.startIntake(); }
    public void stopIntake() { intake.stopIntake(); }
    public void holdIntake() { intake.hold(); }
    public void ejectIntake() { intake.eject(); }
    public void enableVision() { vision.enableVision(); }
    public void disableVision() { vision.disableVision(); }
    public String getDriveStateName() { return drive.getCurrentStateName(); }
    public String getIntakeStateName() { return intake.getCurrentStateName(); }
    public boolean isIntakeAvailable() { return intake.isAvailable(); }
    public String getVisionStateName() { return vision.getCurrentStateName(); }
    public boolean isVisionAvailable() { return vision.isAvailable(); }
    public void runConveyorForward() { conveyor.forward(); }
    public void runConveyorReverse() { conveyor.reverse(); }
    public void stopConveyor() { conveyor.stopBelt(); }
    public void moveConveyorLift(double input) { conveyor.requestLift(input); }
    public String getConveyorStateName() { return conveyor.getBeltStateName(); }
    public String getConveyorLiftStateName() { return conveyor.getLiftStateName(); }
    public double getConveyorLiftTarget() { return conveyor.getLiftTarget(); }
    public boolean isConveyorMotorAvailable() { return conveyor.isMotorAvailable(); }
    public boolean isConveyorLiftAvailable() { return conveyor.isLiftAvailable(); }
    @Override protected void onStop() { hardware.stopAll(); }

    // TODO: Replace wrappers, directions, and subsystem constants after Team 4 hardware is known.
    // TODO: Add Team 4-specific mechanisms when their geometry is defined.
}
