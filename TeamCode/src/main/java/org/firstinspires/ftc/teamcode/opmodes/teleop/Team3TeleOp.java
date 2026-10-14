package org.firstinspires.ftc.teamcode.opmodes.teleop;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.teamcode.core.input.InputManager;
import org.firstinspires.ftc.teamcode.robots.team3.Team3Robot;

/**
 * Team 3's iterative TeleOp for four-wheel mecanum drive.
 *
 * <p>
 * This OpMode maps gamepad input to Team 3's public robot API. The robot and
 * subsystem
 * layers own all behavior and hardware access.
 * </p>
 *
 * <p>
 * <b>Driver (Gamepad 1):</b><br>
 * Left Stick Y/X = forward / strafe.<br>
 * Right Stick X = rotate.<br>
 * Left Trigger (hold) = Green Machine Roller intake forward.<br>
 * Left Bumper (hold) = Green Machine Roller reverse (eject / clear jams).<br>
 * Right Trigger (hold) = outtake (shoot); power matches how far you pull the
 * trigger.<br>
 * Right Bumper (hold) = midtake feed (pushes the game piece into the
 * outtake).<br>
 * A = toggle intake-deploy servo between stowed and deployed.<br>
 * D-pad Down = toggle intake auto-run idle (starts OFF, runs at {@code IDLE_INTAKE_POWER} when ON).<br>
 * Y = toggle heading hold ON / OFF (locks heading using Pinpoint odometry; starts OFF).<br>
 * </p>
 *
 * <p>
 * <b>Operator (Gamepad 2):</b><br>
 * Vision: Left Bumper = off, Right Bumper = on.
 * </p>
 */
@TeleOp(name = "Team 3 Mecanum Drive", group = "Team 3")
public class Team3TeleOp extends OpMode {
    /** How far a trigger must be pulled before it counts as pressed. */
    private static final double TRIGGER_THRESHOLD = 0.2;
    /** Intake power while auto-run idle is toggled ON. 0.0 turns it off. */
    private static final double IDLE_INTAKE_POWER = 0.30;

    private Team3Robot robot;
    private InputManager driverInput;
    private InputManager operatorInput;
    private boolean idleIntakeEnabled;
    private boolean headingHoldEnabled;

    @Override
    public void init() {
        robot = new Team3Robot();
        robot.initialize(hardwareMap);
        driverInput = new InputManager(gamepad1);
        operatorInput = new InputManager(gamepad2);
        idleIntakeEnabled = false;
        headingHoldEnabled = false;

        telemetry.addData("Status", "Team 3 robot initialized");
        telemetry.addData("Pinpoint", robot.isPinpointAvailable() ? "READY" : "NOT CONNECTED");
        telemetry.addData("Intake", robot.isIntakeAvailable() ? "READY" : "MISSING");
        telemetry.addData("Mechanisms", robot.areMechanismsAvailable() ? "READY" : "MISSING");
        telemetry.addData("Vision", robot.isVisionAvailable() ? "READY" : "DISABLED");
        telemetry.update();
    }

    @Override
    public void start() {
        driverInput.update();
        operatorInput.update();
        robot.enableManualDrive();
        idleIntakeEnabled = false;
        headingHoldEnabled = false;
        robot.setIntakeIdlePower(0.0);
    }

    @Override
    public void loop() {
        driverInput.update();
        operatorInput.update();

        // Heading hold toggle: press Y to switch heading hold ON or OFF. Starts OFF.
        if (driverInput.wasYJustPressed()) {
            headingHoldEnabled = !headingHoldEnabled;
            if (headingHoldEnabled) {
                robot.enableHeadingHold();
            } else {
                robot.enableManualDrive();
            }
        }

        // Operator vision controls.
        if (operatorInput.wasLeftBumperJustPressed()) {
            robot.disableVision();
        } else if (operatorInput.wasRightBumperJustPressed()) {
            robot.enableVision();
        }

        // Intake: hold left trigger = forward, hold left bumper = eject, neither =
        // stop / idle.
        if (driverInput.getLeftTrigger() > TRIGGER_THRESHOLD) {
            robot.startIntake();
        } else if (driverInput.isLeftBumperHeld()) {
            robot.ejectIntake();
        } else {
            robot.stopIntake();
        }

        // Intake auto-run idle toggle: press D-pad Down to toggle between ON and OFF (starts OFF).
        if (driverInput.wasDpadDownJustPressed()) {
            idleIntakeEnabled = !idleIntakeEnabled;
            robot.setIntakeIdlePower(idleIntakeEnabled ? IDLE_INTAKE_POWER : 0.0);
        }

        // Shooter: right trigger sets outtake power (0 to 1), right bumper feeds the
        // midtake.
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
        if (robot.isPinpointAvailable()) {
            telemetry.addData("Drive", "%s | H: %.1f deg %s", robot.getDriveStateName(),
                    robot.getHeadingDegrees(),
                    robot.isHeadingHoldActive() ? String.format("[LOCKED: %.1f deg]", robot.getTargetHeadingDegrees()) : "[MANUAL]");
        } else {
            telemetry.addData("Drive", "%s | Pinpoint: NOT CONNECTED", robot.getDriveStateName());
        }
        telemetry.addData("Intake", "%s (Auto-Run: %s)", robot.getIntakeStateName(),
                idleIntakeEnabled ? String.format("%.0f%%", IDLE_INTAKE_POWER * 100.0) : "OFF");
        telemetry.addData("Deploy", robot.getIntakeDeployStateName());
        telemetry.addData("Midtake", robot.getMidtakeStateName());
        telemetry.addData("Outtake", "%s (%.0f%%)", robot.getOuttakeStateName(), robot.getOuttakePower() * 100.0);
        telemetry.addData("Vision", robot.getVisionStateName());
        telemetry.update();
    }
}
