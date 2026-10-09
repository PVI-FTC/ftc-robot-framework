package org.firstinspires.ftc.teamcode.opmodes.teleop;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.teamcode.core.input.InputManager;
import org.firstinspires.ftc.teamcode.robots.team3.Team3Robot;

/**
 * Team 3's iterative TeleOp for four-wheel mecanum drive.
 *
 * <p>This OpMode maps gamepad input to Team 3's public robot API. The robot and subsystem
 * layers own all behavior and hardware access.</p>
 *
 * <p><b>Driver (Gamepad 1):</b><br>
 * Left Stick Y/X = forward / strafe.<br>
 * Right Stick X = rotate.<br>
 * Left Trigger (hold) = Green Machine Roller intake forward.<br>
 * Left Bumper (hold) = Green Machine Roller reverse (eject / clear jams).<br>
 * Right Trigger (hold) = outtake (shoot); power matches how far you pull the trigger.<br>
 * Right Bumper (hold) = midtake feed (pushes the game piece into the outtake).<br>
 * A = toggle intake-deploy servo between stowed and deployed.<br>
 * X = manual drive mode. Y = heading-hold mode.</p>
 *
 * <p><b>Operator (Gamepad 2):</b><br>
 * Vision: Left Bumper = off, Right Bumper = on.</p>
 */
@TeleOp(name = "Team 3 Mecanum Drive", group = "Team 3")
public class Team3TeleOp extends OpMode {
    /** How far a trigger must be pulled before it counts as pressed. */
    private static final double TRIGGER_THRESHOLD = 0.2;

    private Team3Robot robot;
    private InputManager driverInput;
    private InputManager operatorInput;

    @Override
    public void init() {
        robot = new Team3Robot();
        robot.initialize(hardwareMap);
        driverInput = new InputManager(gamepad1);
        operatorInput = new InputManager(gamepad2);

        telemetry.addData("Status", "Team 3 robot initialized");
        telemetry.update();
    }

    @Override
    public void start() {
        driverInput.update();
        operatorInput.update();
        robot.enableManualDrive();
    }

    @Override
    public void loop() {
        driverInput.update();
        operatorInput.update();

        // Drive mode selection.
        if (driverInput.wasYJustPressed()) {
            robot.enableHeadingHold();
        } else if (driverInput.wasXJustPressed()) {
            robot.enableManualDrive();
        }

        // Operator vision controls.
        if (operatorInput.wasLeftBumperJustPressed()) {
            robot.disableVision();
        } else if (operatorInput.wasRightBumperJustPressed()) {
            robot.enableVision();
        }

        // Intake: hold left trigger = forward, hold left bumper = eject, neither = stop.
        if (driverInput.getLeftTrigger() > TRIGGER_THRESHOLD) {
            robot.startIntake();
        } else if (driverInput.isLeftBumperHeld()) {
            robot.ejectIntake();
        } else {
            robot.stopIntake();
        }

        // Shooter: right trigger sets outtake power (0 to 1), right bumper feeds the midtake.
        robot.setOuttakePower(driverInput.getRightTrigger());
        robot.setMidtakeFeeding(driverInput.isRightBumperHeld());

        // Intake-deploy servo: press A to switch between stowed and deployed.
        if (driverInput.wasAJustPressed()) {
            robot.toggleIntakeDeploy();
        }

        robot.drive(-driverInput.getLeftStickY(), driverInput.getLeftStickX(),
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
        telemetry.addData("Green Machine Roller", robot.getIntakeStateName());
        telemetry.addData("Green Machine Roller Available", robot.isIntakeAvailable());
        telemetry.addData("Intake Deploy", robot.getIntakeDeployStateName());
        telemetry.addData("Midtake", robot.getMidtakeStateName());
        telemetry.addData("Outtake", robot.getOuttakeStateName());
        telemetry.addData("Mechanisms Available", robot.areMechanismsAvailable());
        telemetry.addData("Vision State", robot.getVisionStateName());
        telemetry.addData("Vision Available", robot.isVisionAvailable());
        telemetry.update();
    }
}
