package org.firstinspires.ftc.teamcode;

import com.qualcomm.hardware.limelightvision.LLResult;
import com.qualcomm.hardware.limelightvision.Limelight3A;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.hardware.Blinker;
import com.qualcomm.robotcore.hardware.DistanceSensor;
import com.qualcomm.robotcore.hardware.Gyroscope;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.util.ElapsedTime;
import org.firstinspires.ftc.robotcore.external.navigation.Pose3D;

@TeleOp
public class TeleopV1 extends LinearOpMode {
    private Blinker control_Hub;
    private DcMotor fr;
    private DcMotor fl;
    private DcMotor br;
    private DcMotor bl;
    private DcMotorEx outtake;
    private DcMotorEx intake;
    private final ElapsedTime forwardTimer = new ElapsedTime();
    private Limelight3A limelight;

    @Override
    public void runOpMode() {
        control_Hub = hardwareMap.get(Blinker.class, "Control Hub");
        fr = hardwareMap.get(DcMotor.class, "fr");
        fl = hardwareMap.get(DcMotor.class, "fl");
        br = hardwareMap.get(DcMotor.class, "br");
        bl = hardwareMap.get(DcMotor.class, "bl");

        fr.setDirection(DcMotorSimple.Direction.FORWARD);
        bl.setDirection(DcMotorSimple.Direction.REVERSE);
        fl.setDirection(DcMotorSimple.Direction.REVERSE);
        br.setDirection(DcMotorSimple.Direction.FORWARD);
        //outtake = hardwareMap.get(DcMotorEx.class, "pollen");
        //intake = hardwareMap.get(DcMotorEx.class, "intake");

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

            // SIMPLE STUFF HERE (Commented out)
            /*
            if (gamepad1.left_trigger_pressed) {
                intake.setPower(-0.8);
            }
            if (gamepad1.right_trigger_pressed) {
                outtake.setPower(-0.9);
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

            // MECHANUM DRIVE STUFF DOWN HERE
            double y = -gamepad1.left_stick_y; // Remember: Y stick is negative when pushed forward
            double x = gamepad1.left_stick_x;  // Counteract imperfect strafing
            double turnInput = gamepad1.right_stick_x;
            // A sub-linear curve makes moderate stick movements produce a quicker turn response.
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

    // FIXED: Moved the method outside of runOpMode() and added parameters/semicolons
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
