package org.firstinspires.ftc.teamcode.opmodes.autonomous;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;

import org.firstinspires.ftc.teamcode.common.autonomous.AutoSequence;
import org.firstinspires.ftc.teamcode.common.localization.PoseEstimate;
import org.firstinspires.ftc.teamcode.robots.teamA.TeamAPedroConfiguration;
import org.firstinspires.ftc.teamcode.robots.teamA.TeamAPedroPilotPath;
import org.firstinspires.ftc.teamcode.robots.teamA.TeamAPedroPilotPathStep;
import org.firstinspires.ftc.teamcode.robots.teamA.TeamAPedroRobot;

/** Thin LP-11 OpMode for one reviewed, restricted-power Team A Pedro pilot path. */
@Autonomous(name = "Team A Pedro LP-11 Pilot", group = "Team A Testing")
public final class TeamAPedroPilotAutoOpMode extends OpMode {
    private TeamAPedroRobot robot;
    private AutoSequence sequence;
    private PoseEstimate observedStart = PoseEstimate.unavailable();
    private PoseEstimate observedEnd = PoseEstimate.unavailable();

    @Override
    public void init() {
        robot = new TeamAPedroRobot();
        robot.initialize(hardwareMap);
        robot.disableDrive();
        sequence = new AutoSequence(new TeamAPedroPilotPathStep(robot));
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
        if (sequence != null) {
            sequence.stop();
        }
        if (robot != null) {
            robot.stop();
        }
    }

    private void publishTelemetry() {
        PoseEstimate current = robot.getPoseEstimate();
        telemetry.addData("Path", "TeamA_LP11_Pilot");
        telemetry.addData("Application Power", "%.2f", TeamAPedroConfiguration.APPLICATION_MAX_POWER);
        telemetry.addData("Expected Start", "(%.1f, %.1f, %.1f deg)",
                TeamAPedroPilotPath.START_X_INCHES, TeamAPedroPilotPath.START_Y_INCHES,
                Math.toDegrees(TeamAPedroPilotPath.START_HEADING_RADIANS));
        telemetry.addData("Expected End", "(%.1f, %.1f, %.1f deg)",
                TeamAPedroPilotPath.END_X_INCHES, TeamAPedroPilotPath.END_Y_INCHES,
                Math.toDegrees(TeamAPedroPilotPath.END_HEADING_RADIANS));
        addPoseTelemetry("Observed Start", observedStart);
        addPoseTelemetry("Current Pose", current);
        addPoseTelemetry("Observed End", observedEnd);
        telemetry.addData("Auto Step", sequence.getCurrentStepName());
        telemetry.addData("Path Active", robot.isPathFollowingActive());
        telemetry.addData("Sequence Complete", sequence.isFinished());
        telemetry.addData("Drive State", robot.getDriveStateName());
        if (observedEnd.isAvailable()) {
            telemetry.addData("End Position Error", "%.3f in", Math.hypot(
                    observedEnd.getXInches() - TeamAPedroPilotPath.END_X_INCHES,
                    observedEnd.getYInches() - TeamAPedroPilotPath.END_Y_INCHES));
            telemetry.addData("End Heading Error", "%.3f deg", Math.toDegrees(
                    observedEnd.getHeadingRadians() - TeamAPedroPilotPath.END_HEADING_RADIANS));
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
