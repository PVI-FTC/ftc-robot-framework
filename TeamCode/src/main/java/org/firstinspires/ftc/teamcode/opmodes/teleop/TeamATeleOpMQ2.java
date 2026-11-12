package org.firstinspires.ftc.teamcode.opmodes.teleop;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.teamcode.core.input.InputManager;
import org.firstinspires.ftc.teamcode.robots.teamA.TeamARobot;

/**
 * Team A's MQ2 iterative TeleOp for four-wheel mecanum drive.
 *
 * <p>This OpMode maps gamepad input to Team A's public robot API. The robot and drivetrain
 * subsystem own the drive behavior and hardware access.</p>
 */
@TeleOp(name = "Team A Mecanum Drive MQ2", group = "Team A")
public class TeamATeleOpMQ2 extends OpMode {
    private TeamARobot robot;
    private InputManager driverInput;
    private TeamADpadInputManager dpadInput;
    private String intakeRequestStatus = "Not requested";
    private String countResetStatus = "Not requested";

    @Override
    public void init() {
        robot = new TeamARobot();
        robot.initialize(hardwareMap);
        driverInput = new InputManager(gamepad1);
        dpadInput = new TeamADpadInputManager(gamepad1);

        telemetry.addData("Status", "Team A robot initialized");
        telemetry.update();
    }

    @Override
    public void start() {
        driverInput.update();
        dpadInput.update();
        robot.enableManualDrive();
    }

    @Override
    public void loop() {
        driverInput.update();
        dpadInput.update();

        if (driverInput.wasYJustPressed()) {
            robot.enableHeadingHold();
        } else if (driverInput.wasXJustPressed()) {
            robot.enableManualDrive();
        }

        if (driverInput.wasAJustReleased()) {
            boolean accepted = robot.toggleIntakeLoop();
            intakeRequestStatus = accepted
                    ? "Accepted"
                    : "Blocked: outtake launching or robot holds 4 pieces";
        }

        if (!robot.isIntakeLoopActive()) {
            if (driverInput.wasRightBumperJustReleased()) {
                robot.cyclePollenOuttakeMode();
            }
            if (driverInput.wasLeftBumperJustReleased()) {
                robot.cycleNectarOuttakeMode();
            }
        }

        if (dpadInput.wasDownJustPressed()) {
            countResetStatus = robot.resetGamePieceCount()
                    ? "Counts reset"
                    : "Blocked: intake running or outtake launching";
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
        telemetry.addData("Intake Loop", robot.getIntakeLoopStateName());
        telemetry.addData("Intake Request", intakeRequestStatus);
        telemetry.addData("Game Pieces", robot.getGamePieceCount());
        telemetry.addData("Pollen Count", robot.getPollenCount());
        telemetry.addData("Nectar Count", robot.getNectarCount());
        telemetry.addData("Count Reset", countResetStatus);
        telemetry.addData("Intake Detection", robot.getIntakeSensorDetectionName());
        telemetry.addData("Intake Sensor Hue", robot.getIntakeSensorHueDegrees());
        telemetry.addData("Intake Sensor Saturation", robot.getIntakeSensorSaturation());
        telemetry.addData("Intake Sensor Value", robot.getIntakeSensorValue());
        telemetry.addData("Intake Sensor Distance cm", robot.getIntakeSensorDistanceCm());
        telemetry.addData("Pollen Detection", robot.getPollenSensorDetectionName());
        telemetry.addData("Pollen Sensor Hue", robot.getPollenSensorHueDegrees());
        telemetry.addData("Pollen Sensor Saturation", robot.getPollenSensorSaturation());
        telemetry.addData("Pollen Sensor Value", robot.getPollenSensorValue());
        telemetry.addData("Pollen Sensor Distance cm", robot.getPollenSensorDistanceCm());
        telemetry.addData("Pollen Outtake State", robot.getPollenOuttakeStateName());
        telemetry.addData("Nectar Outtake State", robot.getNectarOuttakeStateName());
        telemetry.addData("Vision State", robot.getVisionStateName());
        telemetry.addData("Vision Available", robot.isVisionAvailable());
        telemetry.update();
    }
}
