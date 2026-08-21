package org.firstinspires.ftc.teamcode.opmodes.teleop;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.teamcode.common.localization.PoseEstimate;
import org.firstinspires.ftc.teamcode.core.input.InputManager;
import org.firstinspires.ftc.teamcode.robots.teamA.TeamAPedroRobot;

/** Team A's Pinpoint-backed TeleOp, including active heading hold. */
@TeleOp(name = "Team A Pedro Drive", group = "Team A")
public final class TeamAPedroTeleOp extends OpMode {
    private TeamAPedroRobot robot;
    private InputManager driverInput;

    @Override
    public void init() {
        robot = new TeamAPedroRobot();
        robot.initialize(hardwareMap);
        driverInput = new InputManager(gamepad1);
        telemetry.addData("Status", "Team A Pedro robot initialized");
        telemetry.addLine("Y: capture/hold heading; X: manual rotation");
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

        robot.drive(-driverInput.getLeftStickY(), driverInput.getLeftStickX(),
                driverInput.getRightStickX());
        robot.update();
        publishTelemetry();
    }

    @Override
    public void stop() {
        if (robot != null) robot.stop();
    }

    private void publishTelemetry() {
        PoseEstimate pose = robot.getPoseEstimate();
        telemetry.addData("Drive State", robot.getDriveStateName());
        telemetry.addData("Pinpoint Heading (deg)", Math.toDegrees(pose.getHeadingRadians()));
        telemetry.addLine("Y: recapture heading | X: manual rotation");
        telemetry.update();
    }
}
