package org.firstinspires.ftc.teamcode.opmodes.autonomous;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;

import org.firstinspires.ftc.teamcode.common.autonomous.AutoSequence;
import org.firstinspires.ftc.teamcode.common.autonomous.TimedDriveStep;
import org.firstinspires.ftc.teamcode.robots.team4.Team4Robot;

/** Timed forward move toward the loading zone; calibrate duration for 11 inches. */
@Autonomous(name = "T4-LZ-11", group = "Team 4")
public class Team4LoadingZoneAuto extends OpMode {
    private static final double DRIVE_FORWARD_POWER = 0.20;
    // Student requested five times the initial 0.75-second trial; verify 11-inch travel.
    // New duration = old duration * 11.0 / measured travel in inches.
    private static final double DRIVE_DURATION_SECONDS = 3.75;

    private Team4Robot robot;
    private AutoSequence autoSequence;

    @Override
    public void init() {
        robot = new Team4Robot();
        robot.initialize(hardwareMap);
        autoSequence = new AutoSequence(new TimedDriveStep(robot,
                DRIVE_FORWARD_POWER, 0.0, 0.0, DRIVE_DURATION_SECONDS));
        telemetry.addData("Status", "Ready - press START to drive forward");
        telemetry.addLine("11-inch target: duration requires measured calibration.");
        telemetry.addData("Forward Power", DRIVE_FORWARD_POWER);
        telemetry.addData("Duration (seconds)", DRIVE_DURATION_SECONDS);
        telemetry.update();
    }

    @Override
    public void start() {
        robot.disableDrive();
        robot.stopIntake();
        autoSequence.start();
    }

    @Override
    public void loop() {
        autoSequence.update();
        if (autoSequence.isFinished()) {
            robot.disableDrive();
            robot.stopIntake();
        }
        robot.update();
        telemetry.addData("Status", autoSequence.isFinished() ? "Complete - stopped" : "Driving forward");
        telemetry.addData("Drive State", robot.getDriveStateName());
        telemetry.addData("Forward Power", DRIVE_FORWARD_POWER);
        telemetry.addData("Duration (seconds)", DRIVE_DURATION_SECONDS);
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
