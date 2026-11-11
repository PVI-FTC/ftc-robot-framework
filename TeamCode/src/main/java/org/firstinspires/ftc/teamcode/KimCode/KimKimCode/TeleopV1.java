package org.firstinspires.ftc.teamcode.KimCode.KimKimCode;

import com.qualcomm.hardware.limelightvision.LLResult;
import com.qualcomm.hardware.limelightvision.Limelight3A;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.util.ElapsedTime;

@TeleOp
public class TeleopV1 extends LinearOpMode {
    private DcMotor fr;
    private DcMotor fl;
    private DcMotor br;
    private DcMotor bl;
    private DcMotorEx outtake;
    private DcMotorEx intake;
    private DcMotorEx transfer;
    private final ElapsedTime timer = new ElapsedTime();
    private final ElapsedTime shotclock = new ElapsedTime();
    private Limelight3A limelight;
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

        // LIMELIGHT STUFF --------------
        limelight = hardwareMap.get(Limelight3A.class, "limelight");
        telemetry.setMsTransmissionInterval(11);
        limelight.pipelineSwitch(0);
        limelight.start();
        // ----------------------------------

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
            if (gamepad1.right_bumper) {
                transfer.setPower(1);
            } else {
                transfer.setPower(0);
            }

            if (gamepad1.a) {
                outtake.setPower(1);
            } else {
                outtake.setPower(0);
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

            //limelight stuff

            LLResult result = limelight.getLatestResult();
            if (result != null) {
                if (result.isValid()) {
                    telemetry.addData("tx", result.getTx());
                    telemetry.addData("ty", result.getTy());

                    // FIXED: Pass the 'result' variable into the method
                    telemetry.addData("distance", getDistance(result));

                    telemetry.update();
                }
            }
        }
    }
    public double getDistance(LLResult result) {
        double targetOffsetAngle_Vertical = result.getTy();
        // how many degrees back is your limelight rotated from perfectly vertical? | CHANGE THIS
        double limelightMountAngleDegrees = 25.0;
        // distance from the center of the Limelight lens to the floor | CHANGE THIS
        double limelightLensHeightInches = 20.0;
        // distance from the target to the floor | CHANGE THIS
        double goalHeightInches = 60.0;

        double angleToGoalDegrees = limelightMountAngleDegrees + targetOffsetAngle_Vertical;
        double angleToGoalRadians = angleToGoalDegrees * (3.14159 / 180.0);

        // calculate distance
        double distanceFromLimelightToGoalInches = (goalHeightInches - limelightLensHeightInches) / Math.tan(angleToGoalRadians);
        return distanceFromLimelightToGoalInches;
    }
}