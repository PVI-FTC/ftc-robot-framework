package org.firstinspires.ftc.teamcode.opmodes.autonomous;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.common.autonomous.AutoSequence;
import org.firstinspires.ftc.teamcode.common.localization.PoseEstimate;
import org.firstinspires.ftc.teamcode.robots.team3.Team3PedroPathRoute;
import org.firstinspires.ftc.teamcode.robots.team3.Team3PedroPathStep;
import org.firstinspires.ftc.teamcode.robots.team3.Team3PedroRobot;

/**
 * Team 3 official autonomous OpMode.
 *
 * <p>On START, the robot drives 36 inches (3 feet) straight forward via Pedro Pathing.
 * At 2.0 seconds into the run, the intake-deploy servo is moved to its deployed
 * position (the same default value controlled by button A in TeleOp).</p>
 */
@Autonomous(name = "Team 3 Autonomous", group = "Team 3")
public class Team3AutonomousOpMode extends OpMode {
    private static final Team3PedroPathRoute ROUTE = Team3PedroPathRoute.FORWARD_36;
    private static final double SERVO_DEPLOY_DELAY_SECONDS = 2.0;

    private Team3PedroRobot robot;
    private AutoSequence autoSequence;
    private final ElapsedTime runtime = new ElapsedTime();
    private boolean servoDeployed;

    @Override
    public void init() {
        robot = new Team3PedroRobot();
        robot.initialize(hardwareMap);
        robot.preparePath(ROUTE);
        autoSequence = new AutoSequence(new Team3PedroPathStep(robot, ROUTE));
        servoDeployed = false;

        telemetry.addData("Status", "Team 3 Autonomous initialized");
        telemetry.update();
    }

    @Override
    public void start() {
        runtime.reset();
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
