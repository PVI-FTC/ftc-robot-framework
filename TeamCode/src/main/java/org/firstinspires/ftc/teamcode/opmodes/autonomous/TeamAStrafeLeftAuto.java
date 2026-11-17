package org.firstinspires.ftc.teamcode.opmodes.autonomous;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;

import org.firstinspires.ftc.teamcode.common.autonomous.AutoSequence;
import org.firstinspires.ftc.teamcode.common.autonomous.TimedDriveStep;
import org.firstinspires.ftc.teamcode.robots.teamA.TeamARobot;

/** Strafes the Team A robot left briefly, then stops. */
@Autonomous(name = "Team A Strafe Left 0.5ft", group = "Team A")
public class TeamAStrafeLeftAuto extends OpMode {
    private static final double LEFT_STRAFE_POWER = 0.20;
    private static final double STRAFE_DURATION_SECONDS = 1.25;

    private TeamARobot robot;
    private AutoSequence autoSequence;

    @Override
    public void init() {
        robot = new TeamARobot();
        robot.initialize(hardwareMap);
        autoSequence = new AutoSequence(
                new TimedDriveStep(robot, 0.0, LEFT_STRAFE_POWER, 0.0,
                        STRAFE_DURATION_SECONDS));

        telemetry.addData("Status", "Team A left-strafe autonomous initialized");
        telemetry.update();
    }

    @Override
    public void start() {
        robot.disableDrive();
        autoSequence.start();
    }

    @Override
    public void loop() {
        autoSequence.update();
        if (autoSequence.isFinished()) {
            robot.disableDrive();
        }

        robot.update();
        telemetry.addData("Auto Step", autoSequence.getCurrentStepName());
        telemetry.addData("Sequence Complete", autoSequence.isFinished());
        telemetry.addData("Drive State", robot.getDriveStateName());
        telemetry.update();
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
}
