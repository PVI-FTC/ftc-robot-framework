package org.firstinspires.ftc.teamcode.opmodes.testing;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.teamcode.core.input.InputManager;
import org.firstinspires.ftc.teamcode.robots.team3.Team3Robot;

/**
 * Bench / table testing copy of Team 3's TeleOp with all Pinpoint odometry and IMU
 * stabilization disabled ({@code enableManualDrive()} sets {@code Team3DriveAssistLevel.OFF}).
 *
 * <p>Use this OpMode when the robot is up on blocks or on a table so the drive wheels do not
 * spin to correct heading or position while testing intake, midtake, outtake, and servos.</p>
 *
 * <p>
 * <b>Driver (Gamepad 1):</b><br>
 * Left Stick Y/X = manual forward / strafe (open-loop, no odometry).<br>
 * Right Stick X = manual rotate (open-loop, no odometry).<br>
 * B (hold) = brake: drive motors get zero power.<br>
 * Left Trigger (hold) = intake IN (forward) at 75% power.<br>
 * Left Bumper (hold) = intake OUT (eject) at 15% power.<br>
 * Right Trigger (hold) = outtake forward (variable power matching trigger pull).<br>
 * Right Bumper (hold) = outtake reverse (-1.0 full power, or matches trigger pull if trigger is also held).<br>
 * D-pad Up (hold) = midtake forward at 50% power.<br>
 * D-pad Down (hold) = midtake reverse at 50% power.<br>
 * X = toggle intake-deploy servo between stowed and deployed (first press stows).<br>
 * </p>
 */
@TeleOp(name = "Team 3 Table Test (No Odo)", group = "Team 3 Testing")
public class Team3TableTestTeleOp extends OpMode {
    /** How far a trigger must be pulled before it counts as pressed. */
    private static final double TRIGGER_THRESHOLD = 0.2;
    /** Deadband on joysticks so resting stick drift never spins the wheels on a table. */
    private static final double STICK_DEADBAND = 0.08;
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

        telemetry.addData("Status", "Team 3 Table Test initialized (Odometry OFF)");
        telemetry.addData("Intake", robot.isIntakeAvailable() ? "READY" : "MISSING");
        telemetry.addData("Mechanisms", robot.areMechanismsAvailable() ? "READY" : "MISSING");
        telemetry.update();
    }

    @Override
    public void start() {
        driverInput.update();
        // Keep drive assist strictly OFF so Pinpoint/IMU never spin the wheels on a table
        robot.enableManualDrive();
        robot.setIntakeIdlePower(0.0);
        robot.setIntakePower(INTAKE_FORWARD_POWER);
        robot.setIntakeEjectPower(INTAKE_EJECT_POWER);
    }

    @Override
    public void loop() {
        driverInput.update();

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
        if (driverInput.wasXJustPressed()) {
            robot.toggleIntakeDeploy();
        }

        // Brake: hold B button to stop the drivetrain. Sticks are ignored while held.
        if (driverInput.isBHeld()) {
            robot.brake();
        } else {
            double forward = applyDeadband(-driverInput.getLeftStickY());
            double strafe = applyDeadband(driverInput.getLeftStickX());
            double rotate = applyDeadband(driverInput.getRightStickX());
            robot.drive(forward, strafe, rotate);
        }
        robot.update();
        publishTelemetry();
    }

    private double applyDeadband(double value) {
        return Math.abs(value) >= STICK_DEADBAND ? value : 0.0;
    }

    @Override
    public void stop() {
        if (robot != null) {
            robot.stop();
        }
    }

    private void publishTelemetry() {
        telemetry.addData("Mode", "TABLE TEST (Odometry / Heading Hold OFF)");
        telemetry.addData("Drive", "%s (Cmd F:%.2f S:%.2f R:%.2f)",
                robot.getDriveStateName(),
                robot.getRequestedForward(),
                robot.getRequestedStrafe(),
                robot.getRequestedRotate());
        if (driverInput.isBHeld()) {
            telemetry.addData("Brake", "ON (holding B button)");
        }
        telemetry.addData("Intake", "%s (LT: 75%% In | LB: 15%% Out)", robot.getIntakeStateName());
        telemetry.addData("Deploy", "%s (X to toggle)", robot.getIntakeDeployStateName());
        telemetry.addData("Midtake", "%s (D-Up: 50%% Fwd | D-Down: 50%% Rev)", robot.getMidtakeStateName());
        telemetry.addData("Outtake", "%s (%.0f%%) (RT: Fwd Var | RB: Rev)",
                robot.getOuttakeStateName(), robot.getOuttakePower() * 100.0);
        telemetry.update();
    }
}

