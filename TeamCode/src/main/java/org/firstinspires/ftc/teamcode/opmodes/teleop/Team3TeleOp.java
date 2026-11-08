package org.firstinspires.ftc.teamcode.opmodes.teleop;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.teamcode.common.vision.AprilTagObservationSnapshot;
import org.firstinspires.ftc.teamcode.core.input.InputManager;
import org.firstinspires.ftc.teamcode.core.util.RumbleManager;
import org.firstinspires.ftc.teamcode.robots.team3.Team3DriveAssistLevel;
import org.firstinspires.ftc.teamcode.robots.team3.Team3HiveGoal;
import org.firstinspires.ftc.teamcode.robots.team3.Team3Robot;

/**
 * Team 3's iterative TeleOp for four-wheel mecanum drive.
 *
 * <p>
 * <b>Driver (Gamepad 1):</b><br>
 * Left Stick Y/X = forward / strafe.<br>
 * Right Stick X = rotate.<br>
 * B (hold) = brake: drive motors get zero power so BRAKE mode stops the robot.<br>
 * Left Trigger (hold) = intake IN (forward) at 75% power.<br>
 * Left Bumper (hold) = intake OUT (eject) at 15% power.<br>
 * Right Trigger (hold) = outtake forward (variable power matching trigger pull).<br>
 * Right Bumper (hold) = outtake reverse (-1.0 full power, or matches trigger pull if trigger is also held).<br>
 * D-pad Up (hold) = midtake forward at 50% power.<br>
 * D-pad Down (hold) = midtake reverse at 50% power.<br>
 * D-pad Left (press) = auto-aim robot heading (rotation only) toward visible AprilTag / HIVE Goal.<br>
 * D-pad Right (press) = snap &amp; drive in 3-DOF (distance + lateral center + angle) to the shooting spot in front of whichever of the 4 HIVE Goals is visible.<br>
 * X = toggle intake-deploy servo between stowed and deployed (servo does not move until X is pressed; first press stows).<br>
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
    private RumbleManager rumble;
    private boolean midtakeFeed;
    private boolean midtakeReverse;

    @Override
    public void init() {
        robot = new Team3Robot();
        robot.initialize(hardwareMap);
        driverInput = new InputManager(gamepad1);
        rumble = new RumbleManager(gamepad1);
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
        rumble.resetTimer();
        robot.enableVision();
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
                rumble.rumbleNow(250); // FULL (XY + Heading lock ON)
            } else {
                rumble.rumbleNow(120); // HEADING (Direction hold only, XY manual)
            }
        }

        // Intake: hold left trigger = forward (75%), hold left bumper = eject (15%), neither = stop.
        if (driverInput.getLeftTrigger() > TRIGGER_THRESHOLD) {
            robot.startIntake();
        } else if (driverInput.isLeftBumperHeld()) {
            robot.ejectIntake();
        } else {
            robot.stopIntake();
        }

        // Midtake: hold D-pad Up = forward (50%), hold D-pad Down = reverse (50%), neither = stop.
        midtakeFeed = driverInput.isDpadUpHeld();
        midtakeReverse = driverInput.isDpadDownHeld();
        robot.setMidtakeFeeding(midtakeFeed);
        robot.setMidtakeReversing(midtakeReverse);

        // Shooter (Outtake): hold Right Trigger = forward (variable 0 to 1), hold Right Bumper = reverse (-1.0 or trigger variable).
        double rightTriggerPower = driverInput.getRightTrigger();
        if (driverInput.isRightBumperHeld()) {
            double reversePower = rightTriggerPower > TRIGGER_THRESHOLD ? -rightTriggerPower : -1.0;
            robot.setOuttakePower(reversePower);
        } else if (rightTriggerPower > TRIGGER_THRESHOLD) {
            robot.setOuttakePower(rightTriggerPower);
        } else {
            robot.setOuttakePower(0.0);
        }

        // Intake-deploy servo: press X to switch between stowed and deployed.
        // Servo does not move at init or start until X is pressed; first press stows the deployed intake.
        if (driverInput.wasXJustPressed()) {
            robot.toggleIntakeDeploy();
        }

        // D-pad Left: auto-aim robot heading (rotation only) toward visible AprilTag / HIVE Goal.
        if (driverInput.wasDpadLeftJustPressed()) {
            int aimedTagId = robot.aimAtVisibleAprilTag();
            if (aimedTagId >= 0) {
                rumble.rumbleNow(150); // Quick buzz confirming AprilTag angle lock
            }
        } else if (driverInput.isDpadLeftHeld()
                && robot.getAprilTagObservationSnapshot().isFreshFrame()) {
            robot.aimAtVisibleAprilTag();
        }

        // D-pad Right: snap & navigate in 3-DOF (distance + lateral center + angle) to the shooting
        // spot in front of whichever of the 4 HIVE Goals (2 Red, 2 Blue) is currently in view.
        if (driverInput.wasDpadRightJustPressed()) {
            Team3HiveGoal snappedGoal = robot.snapToVisibleGoalSpot();
            if (snappedGoal != null) {
                rumble.rumbleNow(250); // Buzz confirming 3-DOF HIVE Goal anchor engaged
            }
        }

        // Brake: hold B button to stop the drivetrain and reset the anchor. Sticks are ignored while held.
        if (driverInput.isBHeld()) {
            robot.brake();
        } else {
            robot.drive(-driverInput.getLeftStickY(), driverInput.getLeftStickX(),
                    driverInput.getRightStickX());
        }
        robot.update();
        rumble.update();
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
        if (driverInput.isBHeld()) {
            telemetry.addData("Brake", "ON (holding B button)");
        }
        telemetry.addData("Intake", "%s (LT: 75%% In | LB: 15%% Out)", robot.getIntakeStateName());
        telemetry.addData("Deploy", robot.getIntakeDeployStateName());
        telemetry.addData("Midtake", "%s (D-Up: 50%% Fwd | D-Down: 50%% Rev)", robot.getMidtakeStateName());
        telemetry.addData("Outtake", "%s (%.0f%%) (RT: Fwd Var | RB: Rev)", robot.getOuttakeStateName(), robot.getOuttakePower() * 100.0);
        telemetry.addData("Vision", "%s | Tags: %d [%s]",
                robot.getVisionStateName(),
                robot.getAprilTagObservations().size(),
                robot.getVisibleTagsTelemetrySummary());
        telemetry.addData("Hive Goal Snap", robot.getLastSnappedGoalSummary());
        telemetry.update();
    }
}
