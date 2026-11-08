package org.firstinspires.ftc.teamcode.opmodes.testing;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.teamcode.common.localization.AprilTagFieldPoseCandidate;
import org.firstinspires.ftc.teamcode.common.localization.FieldPose;
import org.firstinspires.ftc.teamcode.common.vision.AprilTagObservation;
import org.firstinspires.ftc.teamcode.common.vision.AprilTagObservationSnapshot;
import org.firstinspires.ftc.teamcode.common.vision.AprilTagPose;
import org.firstinspires.ftc.teamcode.robots.team3.Team3HiveGoal;
import org.firstinspires.ftc.teamcode.robots.team3.Team3Robot;

import java.util.List;

/**
 * Stationary diagnostic OpMode for testing Team 3's AprilTag webcam vision.
 *
 * <p>Uses {@link Team3Robot} with drive disabled so you can verify camera connection,
 * AprilTag ID detection, range/bearing, and field-pose candidates on Driver Station telemetry.</p>
 */
@TeleOp(name = "Team 3 AprilTag Vision Test", group = "Team 3 Testing")
public class Team3AprilTagVisionTestOpMode extends OpMode {
    private Team3Robot robot;

    @Override
    public void init() {
        robot = new Team3Robot();
        robot.initialize(hardwareMap);

        telemetry.addData("Status", "Team 3 AprilTag Vision Test initialized");
        telemetry.addData("Vision Available", robot.isVisionAvailable() ? "READY" : "NOT FOUND (check Webcam 1)");
        telemetry.update();
    }

    @Override
    public void start() {
        robot.disableDrive();
        robot.enableVision();
    }

    @Override
    public void loop() {
        robot.update();
        publishVisionTelemetry(robot.getAprilTagObservationSnapshot());
    }

    @Override
    public void stop() {
        if (robot != null) {
            robot.stop();
        }
    }

    private void publishVisionTelemetry(AprilTagObservationSnapshot snapshot) {
        List<AprilTagObservation> observations = snapshot.getObservations();
        telemetry.addData("Vision State", robot.getVisionStateName());
        telemetry.addData("Vision Available", robot.isVisionAvailable());
        telemetry.addData("Frame Status", snapshot.getFrameStatus());
        telemetry.addData("Detection Count", observations.size());

        for (AprilTagObservation observation : observations) {
            Team3HiveGoal hiveGoal = Team3HiveGoal.fromTagId(observation.getTagId());
            telemetry.addData("Tag ID", "%d (%s)", observation.getTagId(),
                    hiveGoal != null ? hiveGoal.getDisplayName() : "Non-Goal Tag");
            telemetry.addData("Quality", observation.getQualityStatus());
            AprilTagPose robotPose = observation.getRobotRelativePose();
            if (robotPose != null) {
                telemetry.addData("Robot Range / Bearing", "%.2f in / %.2f deg",
                        robotPose.getRangeInches(), robotPose.getBearingDegrees());
                telemetry.addData("Robot XYZ (in)", "%.2f, %.2f, %.2f",
                        robotPose.getRightInches(), robotPose.getForwardInches(), robotPose.getUpInches());
            }
            AprilTagFieldPoseCandidate candidate = observation.getFieldPoseCandidate();
            if (candidate != null) {
                FieldPose fieldPose = candidate.getFieldPose();
                telemetry.addData("Field Pose XYZ (in)", "%.2f, %.2f, %.2f",
                        fieldPose.getXInches(), fieldPose.getYInches(), fieldPose.getZInches());
                telemetry.addData("Field Yaw (deg)", "%.2f", fieldPose.getYawDegrees());
            } else {
                telemetry.addData("Field Candidate Status", observation.getFieldPoseStatus());
            }
        }
        telemetry.update();
    }
}

