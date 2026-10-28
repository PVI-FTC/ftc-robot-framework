package pvi.harness.runtime.catalogsample;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

/**
 * Test-only OpMode for {@code OpModeCatalogTest} and {@code LinearOpModeTest}: a
 * {@code LinearOpMode} is still an OpMode, so it is listed, and the runner can run it (RP6).
 */
@TeleOp(name = "Sample Linear", group = "Samples")
public class SampleLinearOpMode extends LinearOpMode {
    @Override
    public void runOpMode() {
        telemetry.addLine("Sample Linear waiting for PLAY");
        telemetry.update();
        waitForStart();
        int loops = 0;
        while (opModeIsActive()) {
            loops++;
            telemetry.addData("Sample Linear loops", loops);
            telemetry.update();
        }
    }
}
