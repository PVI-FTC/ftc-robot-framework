package org.firstinspires.ftc.teamcode.opmodes.autonomous;

import com.pedropathing.telemetry.SelectableOpMode;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;

import org.firstinspires.ftc.teamcode.common.autonomous.AutoSequence;
import org.firstinspires.ftc.teamcode.common.localization.PoseEstimate;
import org.firstinspires.ftc.teamcode.robots.teamA.TeamAPedroConfiguration;
import org.firstinspires.ftc.teamcode.robots.teamA.TeamAPedroPathRoute;
import org.firstinspires.ftc.teamcode.robots.teamA.TeamAPedroPathStep;
import org.firstinspires.ftc.teamcode.robots.teamA.TeamAPedroRobot;

import java.util.List;

/** Team A autonomous path menu using the same controls as the Pedro tuning selector. */
@Autonomous(name = "Team A Pedro Paths", group = "Team A Testing")
public final class TeamAPedroPilotAutoOpMode extends SelectableOpMode {
    public TeamAPedroPilotAutoOpMode() {
        super("Select a Team A Path", menu -> {
            menu.add(TeamAPedroPathRoute.LP11_PILOT.getDisplayName(),
                    () -> new SelectedPathOpMode(TeamAPedroPathRoute.LP11_PILOT));
            menu.add(TeamAPedroPathRoute.DECODE_ROUTE.getDisplayName(),
                    () -> new SelectedPathOpMode(TeamAPedroPathRoute.DECODE_ROUTE));
            menu.add(TeamAPedroPathRoute.CURVING_TEST.getDisplayName(),
                    () -> new SelectedPathOpMode(TeamAPedroPathRoute.CURVING_TEST));
            menu.add(TeamAPedroPathRoute.ILLEGAL_PATH.getDisplayName(),
                    () -> new SelectedPathOpMode(TeamAPedroPathRoute.ILLEGAL_PATH));
        });
    }

    @Override
    public void onSelect() {
        // The selected inner OpMode initializes only its own robot and route.
    }

    @Override
    public void onLog(List<String> lines) {
        // Driver Station telemetry is published by SelectableOpMode and the selected path.
    }

    /** Thin autonomous shell for one route chosen by the outer Pedro selector. */
    private static final class SelectedPathOpMode extends OpMode {
        private final TeamAPedroPathRoute route;
        private TeamAPedroRobot robot;
        private AutoSequence sequence;
        private PoseEstimate observedStart = PoseEstimate.unavailable();
        private PoseEstimate observedEnd = PoseEstimate.unavailable();

        private SelectedPathOpMode(TeamAPedroPathRoute route) {
            this.route = route;
        }

        @Override
        public void init() {
            robot = new TeamAPedroRobot();
            robot.initialize(hardwareMap);
            robot.preparePath(route);
            sequence = new AutoSequence(new TeamAPedroPathStep(robot, route));
            publishTelemetry();
        }

        @Override
        public void start() {
            observedStart = robot.getPoseEstimate();
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

        private void publishTelemetry() {
            PoseEstimate current = robot.getPoseEstimate();
            telemetry.addData("Path", route.getDisplayName());
            telemetry.addData("Application Power", "%.2f",
                    TeamAPedroConfiguration.APPLICATION_MAX_POWER);
            telemetry.addData("Expected Start", "(%.1f, %.1f, %.1f deg)",
                    route.getStartXInches(), route.getStartYInches(),
                    Math.toDegrees(route.getStartHeadingRadians()));
            telemetry.addData("Expected End", "(%.1f, %.1f, %.1f deg)",
                    route.getEndXInches(), route.getEndYInches(),
                    Math.toDegrees(route.getEndHeadingRadians()));
            addPoseTelemetry("Observed Start", observedStart);
            addPoseTelemetry("Current Pose", current);
            addPoseTelemetry("Observed End", observedEnd);
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
}
