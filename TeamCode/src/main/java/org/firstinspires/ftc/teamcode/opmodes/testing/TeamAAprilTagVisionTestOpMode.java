package org.firstinspires.ftc.teamcode.opmodes.testing;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.teamcode.common.vision.AprilTagObservation;
import org.firstinspires.ftc.teamcode.robots.teamA.TeamAAprilTagVisionRobot;

/** Stationary diagnostic for Team A's Logitech AprilTag observation pilot. */
@TeleOp(name = "Team A AprilTag Vision Test", group = "Team A Testing")
public class TeamAAprilTagVisionTestOpMode extends OpMode {
    private TeamAAprilTagVisionRobot robot;

    @Override
    public void init() {
        robot = new TeamAAprilTagVisionRobot();
        robot.initialize(hardwareMap);
        telemetry.addData("Camera Name", TeamAAprilTagVisionRobot.LOGITECH_VISION_WEBCAM_NAME);
        telemetry.addData("Vision Available", robot.isVisionAvailable());
        telemetry.update();
    }

    @Override
    public void start() {
        robot.enableVision();
    }

    @Override
    public void loop() {
        robot.update();
        telemetry.addData("Vision State", robot.getVisionStateName());
        telemetry.addData("Vision Available", robot.isVisionAvailable());
        telemetry.addData("AprilTags Detected", robot.getAprilTagObservations().size());
        for (AprilTagObservation observation : robot.getAprilTagObservations()) {
            telemetry.addData("Tag " + observation.getTagId(), observation.getQualityStatus());
        }
        telemetry.update();
    }

    @Override
    public void stop() {
        if (robot != null) {
            robot.stop();
        }
    }
}
