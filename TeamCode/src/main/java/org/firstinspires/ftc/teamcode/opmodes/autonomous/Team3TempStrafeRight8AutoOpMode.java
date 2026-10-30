package org.firstinspires.ftc.teamcode.opmodes.autonomous;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;

import org.firstinspires.ftc.teamcode.common.autonomous.AutoSequence;
import org.firstinspires.ftc.teamcode.common.localization.PoseEstimate;
import org.firstinspires.ftc.teamcode.robots.team3.Team3PedroPathRoute;
import org.firstinspires.ftc.teamcode.robots.team3.Team3PedroPathStep;
import org.firstinspires.ftc.teamcode.robots.team3.Team3PedroRobot;

/**
 * Temporary Team 3 autonomous OpMode that moves 8 inches directly to the right.
 * Can be safely deleted after testing.
 */
@Autonomous(name = "Team 3 Temp Strafe Right 8 in", group = "Team 3 Testing")
public class Team3TempStrafeRight8AutoOpMode extends OpMode {
    private static final Team3PedroPathRoute ROUTE = Team3PedroPathRoute.STRAFE_RIGHT_8;

    private Team3PedroRobot robot;
    private AutoSequence autoSequence;

    @Override
    public void init() {
        robot = new Team3PedroRobot();
        robot.initialize(hardwareMap);
        robot.preparePath(ROUTE);
        autoSequence = new AutoSequence(new Team3PedroPathStep(robot, ROUTE));

        telemetry.addData("Status", "Initialized - ready for Fwd 2 + Strafe Right 8");
        telemetry.addData("Deploy", robot.getIntakeDeployStateName());
        telemetry.update();
    }

    @Override
    public void start() {
        // At start (not init), deploy the intake servo - same behavior as pressing 'A' in TeleOp
        robot.deployIntake();
        autoSequence.start();
    }

    @Override
    public void loop() {
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
        telemetry.addData("Auto Step", autoSequence.getCurrentStepName());
        telemetry.addData("Sequence Complete", autoSequence.isFinished());
        telemetry.addData("Path Active", robot.isPathFollowingActive());
        telemetry.addData("Drive State", robot.getDriveStateName());
        telemetry.addData("Deploy", robot.getIntakeDeployStateName());
        if (pose.isAvailable()) {
            telemetry.addData("Pose", "(%.2f, %.2f in, %.1f deg)",
                    pose.getXInches(), pose.getYInches(), Math.toDegrees(pose.getHeadingRadians()));
        } else {
            telemetry.addData("Pose", "unavailable");
        }
        telemetry.addData("Target", "(%.1f, %.1f in)", ROUTE.getEndXInches(), ROUTE.getEndYInches());
        telemetry.update();
    }
}
