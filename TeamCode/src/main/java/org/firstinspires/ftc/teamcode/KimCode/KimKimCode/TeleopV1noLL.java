package org.firstinspires.ftc.teamcode.KimCode.KimKimCode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.util.ElapsedTime;

@TeleOp
public class TeleopV1noLL extends LinearOpMode {
    private DcMotor fr;
    private DcMotor fl;
    private DcMotor br;
    private DcMotor bl;
    private DcMotorEx outtake;
    private DcMotorEx intake;
    private DcMotorEx tubing;
    private DcMotorEx transfer;
    private final ElapsedTime timer = new ElapsedTime();
    private final ElapsedTime shotclock = new ElapsedTime();
    private boolean shooting = false;

    @Override
    public void runOpMode() {
        fr = hardwareMap.get(DcMotor.class, "fr");
        fl = hardwareMap.get(DcMotor.class, "fl");
        br = hardwareMap.get(DcMotor.class, "br");
        bl = hardwareMap.get(DcMotor.class, "bl");

        fr.setDirection(DcMotorSimple.Direction.FORWARD);
        bl.setDirection(DcMotorSimple.Direction.REVERSE);
        fl.setDirection(DcMotorSimple.Direction.REVERSE);
        br.setDirection(DcMotorSimple.Direction.FORWARD);
        outtake = hardwareMap.get(DcMotorEx.class, "outtake");
        intake = hardwareMap.get(DcMotorEx.class, "intake");
        transfer = hardwareMap.get(DcMotorEx.class, "transfer");


        telemetry.addData("Status", "Initialized");
        telemetry.update();

        waitForStart();

        telemetry.addData("Status", "Running");
        telemetry.update();

        // run until the end of the match (driver presses STOP)
        while (opModeIsActive()) {
            // commands stuff

            if (gamepad1.right_trigger_pressed && !shooting) {
                shooting = true;
                shotclock.reset();
                outtake.setPower(1);
            }
            if (shooting && shotclock.seconds() >= 2.0) {
                transfer.setPower(1);
            }
            if (shooting && shotclock.seconds() >= 3.5) {
                transfer.setPower(0);
                outtake.setPower(0);
                shooting = false;
            }
            if (gamepad1.left_trigger_pressed) {
                intake.setPower(1);
            } else {
                intake.setPower(0);
            }
            /*
            if (gamepad1.left_trigger_pressed) {
                intake.setPower(-0.8);
            }
            if (gamepad1.right_trigger_pressed) {
                outtake.setPower(0.9);
                timer.reset();
            } else {
                outtake.setPower(0.0);
            }
            if (gamepad1.left_bumper) {
                intake.setPower(0.5);
            }
            if (!gamepad1.left_bumper && !gamepad1.right_trigger_pressed) {
                intake.setPower(0.0);
            }
             */

            //drive stuff
            double y = -gamepad1.left_stick_y;
            double x = gamepad1.left_stick_x;
            double turnInput = gamepad1.right_stick_x;
            double rx = Math.copySign(Math.pow(Math.abs(turnInput), 0.65), turnInput);
            double denominator = Math.max(Math.abs(y) + Math.abs(x) + Math.abs(rx), 1);
            double frontLeftPower  = (y + x + rx) / denominator;
            double backLeftPower   = (y - x + rx) / denominator;
            double frontRightPower = (y - x - rx) / denominator;
            double backRightPower  = (y + x - rx) / denominator;

            fl.setPower(frontLeftPower);
            bl.setPower(backLeftPower);
            fr.setPower(frontRightPower);
            br.setPower(backRightPower);
        }
    }
}