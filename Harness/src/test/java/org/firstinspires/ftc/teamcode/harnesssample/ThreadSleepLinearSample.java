package org.firstinspires.ftc.teamcode.harnesssample;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotor;

/**
 * HARNESS FILE (test only), not robot code. A LinearOpMode that calls {@code Thread.sleep}
 * itself instead of the SDK's {@code sleep}, for {@code LinearOpModeTest}.
 *
 * <p>It lives in a TeamCode package on purpose: the harness's Java agent only rewrites
 * {@code Thread.sleep} in robot code (TeamCode, the SDK, Pedro), never in harness classes. It has
 * no {@code @TeleOp} annotation, so it never shows up in OpMode lists.</p>
 */
public class ThreadSleepLinearSample extends LinearOpMode {
    @Override
    public void runOpMode() throws InterruptedException {
        DcMotor motor = hardwareMap.get(DcMotor.class, "motor");
        waitForStart();
        motor.setPower(1.0);
        Thread.sleep(200);   // 200 ms of simulated time: 10 loops of 20 ms
        motor.setPower(0.0);
        while (opModeIsActive()) {
            idle();
        }
    }
}
