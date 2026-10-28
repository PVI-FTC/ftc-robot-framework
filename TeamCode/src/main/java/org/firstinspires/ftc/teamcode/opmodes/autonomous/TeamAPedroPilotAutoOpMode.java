package org.firstinspires.ftc.teamcode.opmodes.autonomous;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import org.firstinspires.ftc.teamcode.common.autonomous.AutoSequence;
import org.firstinspires.ftc.teamcode.common.localization.PoseEstimate;
import org.firstinspires.ftc.teamcode.robots.teamA.TeamAPedroConfiguration;
import org.firstinspires.ftc.teamcode.robots.teamA.TeamAPedroPathRoute;
import org.firstinspires.ftc.teamcode.robots.teamA.TeamAPedroPathStep;
import org.firstinspires.ftc.teamcode.robots.teamA.TeamAPedroRobot;

/** Team A Pedro 3 autonomous route selector without the removed Pedro 2 telemetry selector. */
@Autonomous(name = "Team A Pedro Paths", group = "Team A Testing")
public final class TeamAPedroPilotAutoOpMode extends OpMode {
    private static final TeamAPedroPathRoute[] ROUTES = TeamAPedroPathRoute.values();
    private int routeIndex;
    private boolean previousUp;
    private boolean previousDown;
    private TeamAPedroPathRoute route;
    private TeamAPedroRobot robot;
    private AutoSequence sequence;
    private PoseEstimate observedStart = PoseEstimate.unavailable();
    private PoseEstimate observedEnd = PoseEstimate.unavailable();

    @Override
    public void init() {
        route = ROUTES[routeIndex];
        robot = new TeamAPedroRobot();
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
        sequence = new AutoSequence(new TeamAPedroPathStep(robot, route));
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
        telemetry.addData("Application Power", "%.2f", TeamAPedroConfiguration.APPLICATION_MAX_POWER);
        addPoseTelemetry("Current Pose", current);
        addPoseTelemetry("Observed Start", observedStart);
        addPoseTelemetry("Observed End", observedEnd);
        if (sequence != null) {
            telemetry.addData("Auto Step", sequence.getCurrentStepName());
            telemetry.addData("Path Active", robot.isPathFollowingActive());
            telemetry.addData("Sequence Complete", sequence.isFinished());
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
