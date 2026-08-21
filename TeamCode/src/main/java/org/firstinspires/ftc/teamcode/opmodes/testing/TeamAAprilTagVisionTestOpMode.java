package org.firstinspires.ftc.teamcode.opmodes.testing;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.teamcode.common.vision.AprilTagObservation;
import org.firstinspires.ftc.teamcode.common.vision.AprilTagObservationSnapshot;
import org.firstinspires.ftc.teamcode.common.vision.AprilTagPose;
import org.firstinspires.ftc.teamcode.robots.teamA.TeamAAprilTagVisionRobot;

import java.util.Collections;
import java.util.List;

/** Stationary diagnostic for Team A's Logitech/UVC AprilTag pilot. */
@TeleOp(name = "Team A AprilTag Vision Test", group = "Testing")
public class TeamAAprilTagVisionTestOpMode extends OpMode {
    private static final String WEBCAM_HARDWARE_NAME = "logitechVisionWebcam";

    private TeamAAprilTagVisionRobot robot;
    private List<AprilTagObservation> previousObservations = Collections.emptyList();
    private int retainedTimestampChecks;
    private int retainedTimestampFailures;

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
        publishObservations(robot.getAprilTagObservationSnapshot());
    }

    @Override
    public void stop() {
        if (robot != null) {
            robot.stop();
        }
    }

    private void publishObservations(AprilTagObservationSnapshot snapshot) {
        List<AprilTagObservation> observations = snapshot.getObservations();
        String retainedTimestampResult = "Not checked this loop";
        if (snapshot.isRetained() && !observations.isEmpty()) {
            retainedTimestampChecks++;
            boolean timestampsPreserved = haveSameIdsAndTimestamps(
                    previousObservations, observations);
            if (!timestampsPreserved) {
                retainedTimestampFailures++;
            }
            retainedTimestampResult = timestampsPreserved ? "PASS" : "FAIL";
        }

        telemetry.addData("Vision State", robot.getVisionStateName());
        telemetry.addData("Vision Available", robot.isVisionAvailable());
        telemetry.addData("Frame Status", snapshot.getFrameStatus());
        telemetry.addData("Retained Timestamp Check", retainedTimestampResult);
        telemetry.addData("Retained Checks / Failures", "%d / %d",
                retainedTimestampChecks, retainedTimestampFailures);
        telemetry.addData("Detection Count", observations.size());
        for (AprilTagObservation observation : observations) {
            telemetry.addData("Tag " + observation.getTagId(), observation.getQualityStatus());
            telemetry.addData("Tag " + observation.getTagId() + " Pose Available",
                    observation.isPoseAvailable());
            publishPose("Camera", observation.getCameraRelativePose());
            publishPose("Robot", observation.getRobotRelativePose());
            telemetry.addData("Tag " + observation.getTagId() + " Acquisition Timestamp (ns)",
                    observation.getTimestampNanos());
            telemetry.addData("Tag " + observation.getTagId() + " Observation Age (ms)",
                    getObservationAgeMillis(observation));
        }
        telemetry.update();
        previousObservations = observations;
    }

    private boolean haveSameIdsAndTimestamps(List<AprilTagObservation> previous,
                                             List<AprilTagObservation> current) {
        if (previous.size() != current.size()) {
            return false;
        }
        for (int index = 0; index < current.size(); index++) {
            AprilTagObservation oldObservation = previous.get(index);
            AprilTagObservation newObservation = current.get(index);
            if (oldObservation.getTagId() != newObservation.getTagId()
                    || oldObservation.getTimestampNanos() != newObservation.getTimestampNanos()) {
                return false;
            }
        }
        return true;
    }

    private void publishPose(String label, AprilTagPose pose) {
        if (pose == null) {
            telemetry.addData(label + " Pose", "Unavailable");
            return;
        }
        telemetry.addData(label + " Frame", pose.getReferenceFrameName());
        telemetry.addData(label + " XYZ (in)", "%.2f, %.2f, %.2f",
                pose.getRightInches(), pose.getForwardInches(), pose.getUpInches());
        telemetry.addData(label + " Range/Bearing/Elevation", "%.2f in, %.2f deg, %.2f deg",
                pose.getRangeInches(), pose.getBearingDegrees(), pose.getElevationDegrees());
    }

    private double getObservationAgeMillis(AprilTagObservation observation) {
        long ageNanos = Math.max(0, System.nanoTime() - observation.getTimestampNanos());
        return ageNanos / 1_000_000.0;
    }
}
