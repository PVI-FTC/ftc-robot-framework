package org.firstinspires.ftc.teamcode.opmodes.teleop;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.teamcode.core.input.InputManager;
import org.firstinspires.ftc.teamcode.robots.team4.Team4Robot;

/** Initial Team 4 TeleOp with cautious shared mecanum drive controls. */
@TeleOp(name = "Team 4 TeleOp", group = "Team 4")
public class Team4TeleOp extends OpMode {
    private Team4Robot robot;
    private InputManager driverInput;
    private InputManager operatorInput;
    private boolean operatorReleased;
    private boolean intakeButtonsReleased;

    @Override
    public void init() {
        robot = new Team4Robot();
        robot.initialize(hardwareMap);
        driverInput = new InputManager(gamepad1);
        operatorInput = new InputManager(gamepad2);

        telemetry.addData("Status", "Team 4 robot initialized");
        telemetry.update();
    }

    @Override
    public void start() {
        driverInput.update();
        operatorInput.update();
        operatorReleased = false;
        intakeButtonsReleased = false;
        robot.enableManualDrive();
    }

    @Override
    public void loop() {
        driverInput.update();
        operatorInput.update();

        // Common drive mapping: left stick drives and strafes; right stick rotates.
        robot.drive(-driverInput.getLeftStickY(), driverInput.getLeftStickX(),
                driverInput.getRightStickX());

        mapIntakeControls();
        mapConveyorControls();
        // TODO: Add Team 4 mode-selection mappings only when the team approves them.

        robot.update();
        publishTelemetry();
    }

    private void mapIntakeControls() {
        if (!intakeButtonsReleased) {
            intakeButtonsReleased = !driverInput.isAHeld() && !driverInput.isYHeld();
            robot.stopIntake();
        } else if (driverInput.isAHeld() == driverInput.isYHeld()) {
            robot.stopIntake();
        } else if (driverInput.isYHeld()) {
            robot.startIntake();
        } else {
            robot.ejectIntake();
        }
    }

    private void mapConveyorControls() {
        // Require neutral controls once after START to avoid unexpected mechanism movement.
        if (!operatorReleased) {
            operatorReleased = !operatorInput.isAHeld() && !operatorInput.isYHeld()
                    && Math.abs(operatorInput.getLeftStickY()) <= 0.10;
            robot.stopConveyor();
            robot.moveConveyorLift(0.0);
            return;
        }
        if (operatorInput.isAHeld() == operatorInput.isYHeld()) {
            robot.stopConveyor();
        } else if (operatorInput.isAHeld()) {
            robot.runConveyorForward();
        } else {
            robot.runConveyorReverse();
        }
        // FTC stick Y is negative when pushed up; select an endpoint without B/X setup.
        robot.moveConveyorLift(-operatorInput.getLeftStickY());
    }

    @Override
    public void stop() {
        if (robot != null) {
            robot.stop();
        }
    }

    private void publishTelemetry() {
        telemetry.addData("Drive State", robot.getDriveStateName());
        telemetry.addData("Intake State", robot.getIntakeStateName());
        telemetry.addData("Intake Available", robot.isIntakeAvailable());
        telemetry.addLine("G1: hold Y intake / A reverse; release or both stops (intake4Servo)");
        telemetry.addData("Vision State", robot.getVisionStateName());
        telemetry.addData("Vision Available", robot.isVisionAvailable());
        telemetry.addData("conveyor-4 belt", robot.getConveyorStateName());
        telemetry.addData("Conveyor motor available", robot.isConveyorMotorAvailable());
        telemetry.addData("Conveyor lift available", robot.isConveyorLiftAvailable());
        telemetry.addData("Lift command", robot.getConveyorLiftStateName());
        telemetry.addData("G2 lift stick", "%.2f", -operatorInput.getLeftStickY());
        telemetry.addData("Lift target (not measured)", "%.3f", robot.getConveyorLiftTarget());
        telemetry.addLine("G2: A/Y belt forward/reverse (hold); left stick up/down lift");
        telemetry.addLine("Lift: up FULL extend / down FULL retract; B and X unused");
        telemetry.addLine("Release keeps endpoint: motion may continue. SPM stays powered after STOP.");
        telemetry.update();
    }
}
