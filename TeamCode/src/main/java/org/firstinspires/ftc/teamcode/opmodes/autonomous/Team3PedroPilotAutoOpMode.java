package org.firstinspires.ftc.teamcode.opmodes.autonomous;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;

import org.firstinspires.ftc.teamcode.common.autonomous.AutoSequence;
import org.firstinspires.ftc.teamcode.common.localization.PoseEstimate;
import org.firstinspires.ftc.teamcode.robots.team3.Team3PedroConfiguration;
import org.firstinspires.ftc.teamcode.robots.team3.Team3PedroPathRoute;
import org.firstinspires.ftc.teamcode.robots.team3.Team3PedroPathStep;
import org.firstinspires.ftc.teamcode.robots.team3.Team3PedroRobot;

/** Team 3 Pedro 3 autonomous route selector without the removed Pedro 2 telemetry selector. */
@Autonomous(name = "Team 3 Pedro Paths", group = "Team 3 Testing")
public final class Team3PedroPilotAutoOpMode extends OpMode {
    private static final Team3PedroPathRoute[] ROUTES = Team3PedroPathRoute.values();
    private int routeIndex;
    private boolean previousUp;
    private boolean previousDown;
    private Team3PedroPathRoute route;
    private Team3PedroRobot robot;
    private AutoSequence sequence;
    private PoseEstimate observedStart = PoseEstimate.unavailable();
    private PoseEstimate observedEnd = PoseEstimate.unavailable();

    @Override
    public void init() {
        route = ROUTES[routeIndex];
        robot = new Team3PedroRobot();
        robot.initialize(hardwareMap);
        robot.preparePath(route);
        publishTelemetry();
    }

    @Override
    public void init_loop() {
        boolean up = gamepad1.dpad_up;
        boolean down = gamepad1.dpad_down;
        if (up && !previousUp) {
            routeIndex = (routeIndex + ROUTES.length - 1) % ROUTES.length;
            selectRoute();
        } else if (down && !previousDown) {
            routeIndex = (routeIndex + 1) % ROUTES.length;
            selectRoute();
        }
        previousUp = up;
        previousDown = down;
        publishTelemetry();
    }

    @Override
    public void start() {
        observedStart = robot.getPoseEstimate();
        sequence = new AutoSequence(new Team3PedroPathStep(robot, route));
        sequence.start();
    }

    @Override
    public void loop() {
        sequence.update();
        robot.update();
        if (sequence.isFinished() && !observedEnd.isAvailable()) {
            observedEnd = robot.getPoseEstimate();
        }
        publishTelemetry();
    }

    @Override
    public void stop() {
        if (sequence != null) sequence.stop();
        if (robot != null) robot.stop();
    }

    private void selectRoute() {
        route = ROUTES[routeIndex];
        robot.preparePath(route);
    }

    private void publishTelemetry() {
        if (robot == null || route == null) return;
        PoseEstimate current = robot.getPoseEstimate();
        telemetry.addData("Selected Route", route.getDisplayName());
        telemetry.addData("Use D-pad", "Up/Down to select");
        telemetry.addData("Application Power", "%.2f", Team3PedroConfiguration.APPLICATION_MAX_POWER);
        telemetry.addData("Expected Start", "(%.1f, %.1f, %.1f deg)",
                route.getStartXInches(), route.getStartYInches(),
                Math.toDegrees(route.getStartHeadingRadians()));
        telemetry.addData("Expected End", "(%.1f, %.1f, %.1f deg)",
                route.getEndXInches(), route.getEndYInches(),
                Math.toDegrees(route.getEndHeadingRadians()));
        addPoseTelemetry("Current Pose", current);
        addPoseTelemetry("Observed Start", observedStart);
        addPoseTelemetry("Observed End", observedEnd);
        if (sequence != null) {
            telemetry.addData("Auto Step", sequence.getCurrentStepName());
            telemetry.addData("Path Active", robot.isPathFollowingActive());
            telemetry.addData("Sequence Complete", sequence.isFinished());
            telemetry.addData("Drive State", robot.getDriveStateName());
            if (observedEnd.isAvailable()) {
                telemetry.addData("End Position Error", "%.3f in", Math.hypot(
                        observedEnd.getXInches() - route.getEndXInches(),
                        observedEnd.getYInches() - route.getEndYInches()));
                telemetry.addData("End Heading Error", "%.3f deg", Math.toDegrees(
                        observedEnd.getHeadingRadians() - route.getEndHeadingRadians()));
            }
        }
        telemetry.update();
    }

    private void addPoseTelemetry(String caption, PoseEstimate pose) {
        if (!pose.isAvailable()) {
            telemetry.addData(caption, "unavailable");
            return;
        }
        telemetry.addData(caption, "(%.3f, %.3f, %.3f deg)", pose.getXInches(),
                pose.getYInches(), Math.toDegrees(pose.getHeadingRadians()));
    }
}
