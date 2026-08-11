package org.firstinspires.ftc.teamcode.opmodes.testing;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.teamcode.common.localization.PoseEstimate;
import org.firstinspires.ftc.teamcode.core.input.InputManager;
import org.firstinspires.ftc.teamcode.robots.teamA.TeamAPedroConfiguration;
import org.firstinspires.ftc.teamcode.robots.teamA.TeamAPedroFollowerFactory;
import org.firstinspires.ftc.teamcode.robots.teamA.TeamAPedroRobot;

/** Narrow LP-09 diagnostic for unpowered localization and restricted raised-wheel checks. */
@TeleOp(name = "Team A Pedro Diagnostic", group = "Testing")
public final class TeamAPedroDiagnostic extends OpMode {
    private static final double RESTRICTED_COMMAND = 0.20;

    private TeamAPedroRobot robot;
    private InputManager input;

    @Override
    public void init() {
        robot = new TeamAPedroRobot(
                TeamAPedroConfiguration.restrictedManualTestConfiguration(),
                new TeamAPedroFollowerFactory());
        robot.initialize(hardwareMap);
        robot.disableDrive();
        input = new InputManager(gamepad1);

        telemetry.addData("Status", "Initialized with drive disabled");
        telemetry.addLine("Do not press drive controls during the unpowered pose test.");
        telemetry.update();
    }

    @Override
    public void start() {
        input.update();
        robot.disableDrive();
    }

    @Override
    public void loop() {
        input.update();
        applyRestrictedDriveRequest();
        robot.update();
        publishTelemetry();
    }

    @Override
    public void stop() {
        if (robot != null) {
            robot.stop();
        }
    }

    private void applyRestrictedDriveRequest() {
        boolean forward = input.isAHeld();
        boolean left = input.isBHeld();
        boolean counterClockwise = input.isYHeld();
        int requestedMotions = (forward ? 1 : 0) + (left ? 1 : 0)
                + (counterClockwise ? 1 : 0);

        if (input.isXHeld() || !input.isRightBumperHeld() || requestedMotions != 1) {
            robot.drive(0.0, 0.0, 0.0);
            robot.disableDrive();
            return;
        }

        robot.enableManualDrive();
        if (forward) {
            robot.drive(RESTRICTED_COMMAND, 0.0, 0.0);
        } else if (left) {
            // Pedro's TeleOp strafe input is negative for robot-left movement.
            robot.drive(0.0, -RESTRICTED_COMMAND, 0.0);
        } else {
            // LP-09 raised-wheel observation verified positive input as counterclockwise.
            robot.drive(0.0, 0.0, RESTRICTED_COMMAND);
        }
    }

    private void publishTelemetry() {
        PoseEstimate pose = robot.getPoseEstimate();
        telemetry.addData("Drive State", robot.getDriveStateName());
        telemetry.addData("Pose Available", pose.isAvailable());
        telemetry.addData("X (in)", pose.getXInches());
        telemetry.addData("Y (in)", pose.getYInches());
        telemetry.addData("Heading (deg)", Math.toDegrees(pose.getHeadingRadians()));
        telemetry.addData("Manual Test Gate", robot.isRestrictedManualDriveReady());
        telemetry.addData("Path Gate", robot.isPathFollowingReady());
        telemetry.addLine("STOP/cancel: release RB or hold X");
        telemetry.addLine("Raised wheels only: hold RB + A forward, B left, or Y rotate CCW");
        telemetry.update();
    }
}
