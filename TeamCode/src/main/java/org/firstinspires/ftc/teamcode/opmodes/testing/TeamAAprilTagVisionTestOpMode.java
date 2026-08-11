package org.firstinspires.ftc.teamcode.opmodes.testing;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.teamcode.common.vision.AprilTagObservation;
import org.firstinspires.ftc.teamcode.robots.teamA.TeamAAprilTagVisionRobot;

import java.util.List;

/** Stationary diagnostic for Team A's Logitech/UVC AprilTag pilot. */
@TeleOp(name = "Team A AprilTag Vision Test", group = "Testing")
public class TeamAAprilTagVisionTestOpMode extends OpMode {
    private static final String WEBCAM_HARDWARE_NAME = "logitechVisionWebcam";

    private TeamAAprilTagVisionRobot robot;

    @Override
    public void init() {
        robot = new TeamAAprilTagVisionRobot(WEBCAM_HARDWARE_NAME);
        robot.initialize(hardwareMap);
        telemetry.addData("Status", "AprilTag pilot initialized");
        telemetry.update();
    }

    @Override
    public void start() {
        robot.enableVision();
    }

    @Override
    public void loop() {
        robot.update();
        publishObservations(robot.getAprilTagObservations());
    }

    @Override
    public void stop() {
        if (robot != null) {
            robot.stop();
        }
    }

    private void publishObservations(List<AprilTagObservation> observations) {
        telemetry.addData("Vision State", robot.getVisionStateName());
        telemetry.addData("Vision Available", robot.isVisionAvailable());
        telemetry.addData("Detection Count", observations.size());
        for (AprilTagObservation observation : observations) {
            telemetry.addData("Tag " + observation.getTagId(), observation.getQualityStatus());
        }
        telemetry.update();
    }
}
