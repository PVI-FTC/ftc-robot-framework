package org.firstinspires.ftc.teamcode.opmodes.autonomous;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;

import org.firstinspires.ftc.teamcode.common.autonomous.AutoSequence;
import org.firstinspires.ftc.teamcode.common.autonomous.TimedDriveStep;
import org.firstinspires.ftc.teamcode.robots.teamA.TeamARobot;

/** Moves Team A's robot forward, left from the wall, then forward into the stopping zone. */
@Autonomous(name = "Team A Basic Auto", group = "Team A")
public class TeamAAutoOpMode extends OpMode {
    private static final double DRIVE_POWER = 0.50;

    // Adjust these three times after measuring the robot's travel on the field.
    private static final double INITIAL_FORWARD_SECONDS = 0.3;
    private static final double MOVE_LEFT_SECONDS = 2.2;
    private static final double MOVE_FORWARD_SECONDS = 3.20;

    private TeamARobot robot;
    private AutoSequence autoSequence;

    @Override
    public void init() {
        robot = new TeamARobot();
        robot.initialize(hardwareMap);
        autoSequence = new AutoSequence(
                new TimedDriveStep(robot, -(DRIVE_POWER), 0.0, 0.0,
                        INITIAL_FORWARD_SECONDS),
                // Physical testing showed positive strafe moves this auto robot-left.
                new TimedDriveStep(robot, 0.0, DRIVE_POWER, 0.0, MOVE_LEFT_SECONDS),
                new TimedDriveStep(robot, -(DRIVE_POWER), 0.0, 0.0, MOVE_FORWARD_SECONDS));

        telemetry.addData("Status", "Team A autonomous initialized");
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
        telemetry.addData("Auto Step", autoSequence.getCurrentStepName());
        telemetry.addData("Sequence Complete", autoSequence.isFinished());
        telemetry.addData("Drive State", robot.getDriveStateName());
        telemetry.update();
    }
}
