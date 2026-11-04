package org.firstinspires.ftc.teamcode.opmodes.autonomous;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.common.autonomous.AutoSequence;
import org.firstinspires.ftc.teamcode.common.localization.PoseEstimate;
import org.firstinspires.ftc.teamcode.common.vision.AprilTagObservation;
import org.firstinspires.ftc.teamcode.robots.team3.Team3PedroPathRoute;
import org.firstinspires.ftc.teamcode.robots.team3.Team3PedroPathStep;
import org.firstinspires.ftc.teamcode.robots.team3.Team3PedroRobot;
import org.firstinspires.ftc.teamcode.robots.team3.Team3RelocalizeStep;
import org.firstinspires.ftc.teamcode.robots.team3.Team3ScoreStep;

import java.util.List;

/**
 * Team 3 official autonomous OpMode.
 *
 * <p>During {@code init_loop()}, the webcam watches for AprilTags and latches the last seen
 * tag ID. On START, the robot drives 36 inches (3 feet) straight forward via Pedro Pathing,
 * optionally relocalizes from a visible Goal AprilTag, and executes {@link Team3ScoreStep}
 * (spins up the shooter and feeds with midtake). At 2.0 seconds into the run, the
 * intake-deploy servo is moved to its deployed position.</p>
 */
@Autonomous(name = "Team 3 Autonomous", group = "Team 3")
public class Team3AutonomousOpMode extends OpMode {
    private static final Team3PedroPathRoute ROUTE = Team3PedroPathRoute.FORWARD_36;
    private static final double SERVO_DEPLOY_DELAY_SECONDS = 2.0;

    private Team3PedroRobot robot;
    private AutoSequence autoSequence;
    private final ElapsedTime runtime = new ElapsedTime();
    private boolean servoDeployed;
    private int latchedPreStartTagId = -1;

    @Override
    public void init() {
        robot = new Team3PedroRobot();
        robot.initialize(hardwareMap);
        robot.enableVision();
        robot.preparePath(ROUTE);
        autoSequence = new AutoSequence(
                new Team3PedroPathStep(robot, ROUTE),
                new Team3RelocalizeStep(robot),
                new Team3ScoreStep(robot));
        servoDeployed = false;
        latchedPreStartTagId = -1;

        telemetry.addData("Status", "Team 3 Autonomous initialized");
        telemetry.addData("Vision", robot.isVisionAvailable() ? "READY" : "NOT FOUND");
        telemetry.update();
    }

    @Override
    public void init_loop() {
        robot.update();
        List<AprilTagObservation> observations = robot.getAprilTagObservations();
        for (AprilTagObservation obs : observations) {
            latchedPreStartTagId = obs.getTagId();
        }
        telemetry.addData("Status", "Waiting for START (Pre-Start Vision Active)");
        telemetry.addData("Vision", "%s (Latched Tag: %s)",
                robot.getVisionStateName(),
                latchedPreStartTagId >= 0 ? String.valueOf(latchedPreStartTagId) : "None yet");
        telemetry.update();
    }

    @Override
    public void start() {
        runtime.reset();
        robot.enableVision();
        autoSequence.start();
    }

    @Override
    public void loop() {
        // 2 seconds in, deploy the intake servo (same default position as A in TeleOp)
        if (!servoDeployed && runtime.seconds() >= SERVO_DEPLOY_DELAY_SECONDS) {
            robot.deployIntake();
            servoDeployed = true;
        }

        autoSequence.update();
        robot.update();
        publishTelemetry();
    }

    @Override
    public void stop() {
        if (autoSequence != null) {
            autoSequence.stop();
        }
        if (robot != null) {
            robot.stop();
        }
    }

    private void publishTelemetry() {
        PoseEstimate pose = robot.getPoseEstimate();
        telemetry.addData("Runtime", "%.2f s", runtime.seconds());
        telemetry.addData("Auto Step", autoSequence.getCurrentStepName());
        telemetry.addData("Sequence Complete", autoSequence.isFinished());
        telemetry.addData("Intake Deploy", robot.getIntakeDeployStateName());
        telemetry.addData("Path Active", robot.isPathFollowingActive());
        telemetry.addData("Drive State", robot.getDriveStateName());
        if (pose.isAvailable()) {
            telemetry.addData("Pose", "(%.2f, %.2f, %.1f deg)", pose.getXInches(),
                    pose.getYInches(), Math.toDegrees(pose.getHeadingRadians()));
        } else {
            telemetry.addData("Pose", "unavailable");
        }
        telemetry.addData("Target", "(%.1f, %.1f)", ROUTE.getEndXInches(),
                ROUTE.getEndYInches());
        telemetry.update();
    }
}
