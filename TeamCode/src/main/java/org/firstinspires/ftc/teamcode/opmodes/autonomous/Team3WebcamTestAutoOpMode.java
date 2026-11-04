package org.firstinspires.ftc.teamcode.opmodes.autonomous;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;

import org.firstinspires.ftc.teamcode.common.localization.AprilTagFieldPoseCandidate;
import org.firstinspires.ftc.teamcode.common.localization.FieldPose;
import org.firstinspires.ftc.teamcode.common.vision.AprilTagObservation;
import org.firstinspires.ftc.teamcode.common.vision.AprilTagObservationSnapshot;
import org.firstinspires.ftc.teamcode.common.vision.AprilTagPose;
import org.firstinspires.ftc.teamcode.robots.team3.Team3PedroRobot;

import java.util.List;

/**
 * Stationary Autonomous OpMode for testing Team 3's webcam and AprilTag detection
 * through {@link Team3PedroRobot} without moving the drivetrain.
 *
 * <p>Appears in the Driver Station Autonomous menu so you can test AprilTag detection
 * both during {@code init_loop()} (pre-match preview) and after pressing PLAY.</p>
 */
@Autonomous(name = "Team 3 Webcam Test (Auto)", group = "Team 3 Testing")
public class Team3WebcamTestAutoOpMode extends OpMode {
    private Team3PedroRobot robot;

    @Override
    public void init() {
        robot = new Team3PedroRobot();
        robot.initialize(hardwareMap);
        robot.disableDrive();
        robot.enableVision();

        telemetry.addData("Status", "Team 3 Webcam Auto Test initialized (Drive OFF)");
        telemetry.addData("Webcam (" + "Webcam 1" + ")",
                robot.isVisionAvailable() ? "CONNECTED" : "NOT FOUND in HardwareMap");
        telemetry.update();
    }

    @Override
    public void init_loop() {
        robot.update();
        publishVisionTelemetry("INIT (Waiting for START)", robot.getAprilTagObservationSnapshot());
    }

    @Override
    public void start() {
        robot.disableDrive();
        robot.enableVision();
    }

    @Override
    public void loop() {
        robot.update();
        publishVisionTelemetry("RUNNING (Drive Disabled)", robot.getAprilTagObservationSnapshot());
    }

    @Override
    public void stop() {
        if (robot != null) {
            robot.stop();
        }
    }

    private void publishVisionTelemetry(String phase, AprilTagObservationSnapshot snapshot) {
        List<AprilTagObservation> observations = snapshot.getObservations();
        telemetry.addData("Phase", phase);
        telemetry.addData("Webcam Available", robot.isVisionAvailable() ? "YES" : "NO (Check 'Webcam 1' in config)");
        telemetry.addData("Vision FSM State", robot.getVisionStateName());
        telemetry.addData("Frame Status", snapshot.getFrameStatus());
        telemetry.addData("Tags Detected", observations.size());

        for (AprilTagObservation observation : observations) {
            telemetry.addData("Tag ID", observation.getTagId());
            telemetry.addData("Quality", observation.getQualityStatus());
            AprilTagPose cameraPose = observation.getCameraRelativePose();
            if (cameraPose != null) {
                telemetry.addData("Camera Range / Bearing", "%.2f in / %.2f deg",
                        cameraPose.getRangeInches(), cameraPose.getBearingDegrees());
            }
            AprilTagPose robotPose = observation.getRobotRelativePose();
            if (robotPose != null) {
                telemetry.addData("Robot Range / Bearing", "%.2f in / %.2f deg",
                        robotPose.getRangeInches(), robotPose.getBearingDegrees());
                telemetry.addData("Robot XYZ (in)", "Right: %.2f, Fwd: %.2f, Up: %.2f",
                        robotPose.getRightInches(), robotPose.getForwardInches(), robotPose.getUpInches());
            }
            AprilTagFieldPoseCandidate candidate = observation.getFieldPoseCandidate();
            if (candidate != null) {
                FieldPose fieldPose = candidate.getFieldPose();
                telemetry.addData("Field Pose XYZ (in)", "(%.2f, %.2f, %.2f)",
                        fieldPose.getXInches(), fieldPose.getYInches(), fieldPose.getZInches());
                telemetry.addData("Field Yaw (deg)", "%.2f", fieldPose.getYawDegrees());
            } else {
                telemetry.addData("Field Candidate", observation.getFieldPoseStatus());
            }
        }
        telemetry.update();
    }
}

