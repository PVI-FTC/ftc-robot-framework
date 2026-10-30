package org.firstinspires.ftc.teamcode.opmodes.teleop;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.teamcode.core.input.InputManager;
import org.firstinspires.ftc.teamcode.robots.team3.Team3DriveAssistLevel;
import org.firstinspires.ftc.teamcode.robots.team3.Team3Robot;

/**
 * Team 3's iterative TeleOp for four-wheel mecanum drive.
 *
 * <p>
 * <b>Driver (Gamepad 1):</b><br>
 * Left Stick Y/X = forward / strafe.<br>
 * Right Stick X = rotate.<br>
 * Back (hold) = brake: drive motors get zero power so BRAKE mode stops the robot.<br>
 * Left Trigger (hold) = intake IN (forward) at 75% power.<br>
 * Left Bumper (hold) = intake OUT (eject) at 15% power.<br>
 * Right Trigger (hold) = midtake forward at 50% power.<br>
 * Right Bumper (hold) = midtake reverse at 50% power.<br>
 * A = toggle intake-deploy servo between stowed and deployed (servo does not move until A is pressed; first press stows).<br>
 * Y = toggle full pose lock ON / OFF (starts OFF; when ON, resists all displacement and returns to original spot).<br>
 * </p>
 */
@TeleOp(name = "Team 3 Mecanum Drive", group = "Team 3")
public class Team3TeleOp extends OpMode {
    /** How far a trigger must be pulled before it counts as pressed. */
    private static final double TRIGGER_THRESHOLD = 0.2;
    private static final double INTAKE_FORWARD_POWER = 0.75;
    private static final double INTAKE_EJECT_POWER = 0.15;

    private Team3Robot robot;
    private InputManager driverInput;
    private boolean midtakeFeed;
    private boolean midtakeReverse;

    @Override
    public void init() {
        robot = new Team3Robot();
        robot.initialize(hardwareMap);
        driverInput = new InputManager(gamepad1);
        midtakeFeed = false;
        midtakeReverse = false;

        robot.setIntakePower(INTAKE_FORWARD_POWER);
        robot.setIntakeEjectPower(INTAKE_EJECT_POWER);
        robot.setIntakeIdlePower(0.0);

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
        robot.setDriveAssistLevel(Team3DriveAssistLevel.HEADING);
        robot.setIntakeIdlePower(0.0);
        robot.setIntakePower(INTAKE_FORWARD_POWER);
        robot.setIntakeEjectPower(INTAKE_EJECT_POWER);
    }

    @Override
    public void loop() {
        driverInput.update();

        // Y toggle: toggles additional XY position hold ON (FULL) or OFF (HEADING only).
        // Direction holding always remains active!
        if (driverInput.wasYJustPressed()) {
            Team3DriveAssistLevel newLevel = robot.toggleDriveAssist();
            if (newLevel == Team3DriveAssistLevel.FULL) {
                gamepad1.rumbleBlips(2); // 2 blips: FULL (XY + Heading lock ON)
            } else {
                gamepad1.rumbleBlips(1); // 1 blip: HEADING (Direction hold only, XY manual)
            }
        }

        // Intake: hold left trigger = forward (75%), hold left bumper = eject (15%), neither = stop.
        if (driverInput.getLeftTrigger() > TRIGGER_THRESHOLD || gamepad1.left_trigger > TRIGGER_THRESHOLD) {
            robot.startIntake();
        } else if (driverInput.isLeftBumperHeld() || gamepad1.left_bumper) {
            robot.ejectIntake();
        } else {
            robot.stopIntake();
        }

        // Midtake: hold right trigger = forward (50%), hold right bumper = reverse (50%), neither = stop.
        midtakeFeed = driverInput.getRightTrigger() > TRIGGER_THRESHOLD || gamepad1.right_trigger > TRIGGER_THRESHOLD;
        midtakeReverse = driverInput.isRightBumperHeld() || gamepad1.right_bumper;
        robot.setMidtakeFeeding(midtakeFeed);
        robot.setMidtakeReversing(midtakeReverse);

        // Outtake disregarded for now.
        robot.setOuttakePower(0.0);

        // Intake-deploy servo: press A to switch between stowed and deployed.
        // Servo does not move at init or start until A is pressed; first press stows the deployed intake.
        if (driverInput.wasAJustPressed()) {
            robot.toggleIntakeDeploy();
        }

        // Brake: hold Back button to stop the drivetrain and reset the anchor. Sticks are ignored while held.
        if (gamepad1.back) {
            robot.brake();
        } else {
            robot.drive(-driverInput.getLeftStickY(), driverInput.getLeftStickX(),
                    driverInput.getRightStickX());
        }
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
            boolean xyLockActive = robot.isPoseLockActive();
            telemetry.addData("Heading Hold", "ALWAYS ON (Target: %.1f deg)", robot.getTargetHeadingDegrees());
            telemetry.addData("XY Pose Lock", "%s [%s] (Y to toggle) | Loop: %.1f ms",
                    xyLockActive ? "ON (HOLDING XY)" : "OFF (MANUAL XY)",
                    xyLockActive ? robot.getPoseLockTranslationStateName() : "MANUAL",
                    robot.getLoopTimeMs());
            telemetry.addData("Pinpoint (Odo+IMU)", "(%.1f, %.1f in, %.1f deg) | Speed: %.1f in/s",
                    robot.getXInches(), robot.getYInches(), robot.getHeadingDegrees(), robot.getRobotSpeedIps());
            if (xyLockActive) {
                telemetry.addData("Locked Origin", "(%.1f, %.1f in, %.1f deg)",
                        robot.getTargetXInches(), robot.getTargetYInches(), robot.getTargetHeadingDegrees());
                telemetry.addData("Disturbance Err", "%.2f in, %.1f deg",
                        robot.getPoseLockErrorInches(), robot.getPoseLockHeadingErrorDegrees());
            }
        } else {
            telemetry.addData("Drive", "%s | Pinpoint: NOT CONNECTED", robot.getDriveStateName());
        }
        if (gamepad1.back) {
            telemetry.addData("Brake", "ON (holding Back button)");
        }
        telemetry.addData("Intake", "%s (LT: 75%% In | LB: 15%% Out)", robot.getIntakeStateName());
        telemetry.addData("Deploy", robot.getIntakeDeployStateName());
        telemetry.addData("Midtake", "%s (RT: 50%% Fwd | RB: 50%% Rev)", robot.getMidtakeStateName());
        telemetry.update();
    }
}
