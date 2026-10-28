package org.firstinspires.ftc.teamcode.opmodes.teleop;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.teamcode.core.input.InputManager;
import org.firstinspires.ftc.teamcode.robots.teamA.TeamARobot;

/**
 * Team A's iterative TeleOp for four-wheel mecanum drive.
 *
 * <p>This OpMode maps gamepad input to Team A's public robot API. The robot and drivetrain
 * subsystem own the drive behavior and hardware access.</p>
 */
@TeleOp(name = "Team A Mecanum Drive", group = "Team A")
public class TeamATeleOp extends OpMode {
    private static final double TRIGGER_DEADZONE = 0.05;

    private TeamARobot robot;
    private InputManager driverInput;

    @Override
    public void init() {
        robot = new TeamARobot();
        robot.initialize(hardwareMap);
        driverInput = new InputManager(gamepad1);

        telemetry.addData("Status", "Team A robot initialized");
        telemetry.update();
    }

    @Override
    public void start() {
        driverInput.update();
        robot.enableManualDrive();
    }

    @Override
    public void loop() {
        driverInput.update();

        if (driverInput.wasYJustPressed()) {
            robot.enableHeadingHold();
        } else if (driverInput.wasXJustPressed()) {
            robot.enableManualDrive();
        }

        if (driverInput.wasLeftBumperJustPressed()) {
            robot.disableVision();
        } else if (driverInput.wasRightBumperJustPressed()) {
            robot.enableVision();
        }

        double intakePower = driverInput.getRightTrigger();
        if (intakePower > TRIGGER_DEADZONE) {
            robot.startIntake(intakePower);
        } else {
            robot.stopIntake();
        }

        robot.drive(driverInput.getLeftStickY(), -driverInput.getLeftStickX(),
                driverInput.getRightStickX());
        robot.update();
        publishDriveTelemetry();
    }

    @Override
    public void stop() {
        if (robot != null) {
            robot.stop();
        }
    }

    private void publishDriveTelemetry() {
        telemetry.addData("Drive State", robot.getDriveStateName());
        telemetry.addData("Forward", robot.getRequestedForward());
        telemetry.addData("Strafe", robot.getRequestedStrafe());
        telemetry.addData("Rotate", robot.getRequestedRotate());
        telemetry.addData("Front Left Power", robot.getFrontLeftMotorPower());
        telemetry.addData("Front Right Power", robot.getFrontRightMotorPower());
        telemetry.addData("Rear Left Power", robot.getRearLeftMotorPower());
        telemetry.addData("Rear Right Power", robot.getRearRightMotorPower());
        telemetry.addData("Intake State", robot.getIntakeStateName());
        telemetry.addData("Intake Available", robot.isIntakeAvailable());
        telemetry.addData("Vision State", robot.getVisionStateName());
        telemetry.addData("Vision Available", robot.isVisionAvailable());
        telemetry.update();
    }
}
