package org.firstinspires.ftc.teamcode.KimCode;

import com.pedropathing.follower.Follower;
import com.qualcomm.hardware.limelightvision.LLResult;
import com.qualcomm.hardware.limelightvision.Limelight3A;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.HardwareMap;
import org.firstinspires.ftc.teamcode.core.input.InputManager;
import org.firstinspires.ftc.teamcode.pedro.Constants;

/** Kim's Pedro 3 TeleOp adaptation of the standalone Pedro3 TeleopV1 example. */
@TeleOp(name = "KimCode TeleopV1", group = "KimCode")
public final class TeleopV1 extends LinearOpMode {
    private final class TeleOpController {
        private Follower follower;
        private Limelight3A limelight;

        void initialize(HardwareMap hardwareMap) {
            follower = Constants.create(hardwareMap);
            limelight = hardwareMap.get(Limelight3A.class, "limelight");
            // LIMELIGHT STUFF --------------
            limelight.pipelineSwitch(0);
            limelight.start();
            // ----------------------------------
        }

        void update(double forward, double strafe, double rotate) {
            follower.manual(forward, strafe, rotate);
            follower.update();
        }

        LLResult getLatestVisionResult() {
            return limelight.getLatestResult();
        }

        void stop() {
            if (follower != null) follower.stop();
            if (limelight != null) limelight.stop();
        }
    }

    @Override
    public void runOpMode() {
        TeleOpController controller = new TeleOpController();
        InputManager input = new InputManager(gamepad1);
        controller.initialize(hardwareMap);
        telemetry.setMsTransmissionInterval(11);
        telemetry.addData("Status", "Initialized");
        telemetry.update();

        waitForStart();
        telemetry.addData("Status", "Running");
        telemetry.update();

        // run until the end of the match (driver presses STOP)
        try {
            while (opModeIsActive()) {
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
                input.update();
                // Remember: Y stick is negative when pushed forward.
                // Counteract imperfect strafing.
                controller.update(-input.getLeftStickY(), -input.getLeftStickX(), -input.getRightStickX());

                LLResult result = controller.getLatestVisionResult();
                if (result != null && result.isValid()) {
                    telemetry.addData("tx", result.getTx());
                    telemetry.addData("ty", result.getTy());
                    // FIXED: Pass the 'result' variable into the method
                    telemetry.addData("distance", getDistance(result));
                }
                telemetry.update();
            }
        } finally {
            controller.stop();
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
