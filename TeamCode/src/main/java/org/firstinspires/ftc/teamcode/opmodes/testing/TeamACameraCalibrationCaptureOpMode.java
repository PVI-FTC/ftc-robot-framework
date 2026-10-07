package org.firstinspires.ftc.teamcode.opmodes.testing;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.teamcode.robots.teamA.TeamACameraCalibrationRobot;

/** Stationary capture utility for calibrating Team A's C920 at 640 by 480. */
@TeleOp(name = "Team A Camera Calibration Capture", group = "Testing")
public class TeamACameraCalibrationCaptureOpMode extends OpMode {
    private static final String WEBCAM_HARDWARE_NAME = "logitechVisionWebcam";

    private TeamACameraCalibrationRobot robot;
    private boolean lastX;
    private int captureRequests;

    @Override
    public void init() {
        robot = new TeamACameraCalibrationRobot(WEBCAM_HARDWARE_NAME);
        robot.initialize(hardwareMap);
        publishTelemetry();
    }

    @Override
    public void init_loop() {
        publishTelemetry();
    }

    private void publishTelemetry() {
        telemetry.addData("Camera State", robot.getCameraStateName());
        telemetry.addData("Resolution", "640 x 480");
        telemetry.addData("X Presses Accepted", captureRequests);
        telemetry.addData("Last Capture Request", robot.getLastCaptureRequest());
        telemetry.addLine("Saved image files must be verified on the Control Hub.");
        telemetry.update();
    }

    @Override
    public void loop() {
        robot.update();
        boolean x = gamepad1.x;
        if (x && !lastX && robot.requestFrameCapture()) {
            captureRequests++;
        }
        lastX = x;

        publishTelemetry();
    }

    @Override
    public void stop() {
        if (robot != null) {
            robot.stop();
        }
    }
}
