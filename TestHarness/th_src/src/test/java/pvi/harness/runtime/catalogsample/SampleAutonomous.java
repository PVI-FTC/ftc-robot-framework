package pvi.harness.runtime.catalogsample;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;

/** Test-only OpMode for {@code OpModeCatalogTest}: an autonomous that picks a TeleOp to run next. */
@Autonomous(name = "Sample Autonomous", group = "Samples", preselectTeleOp = "Sample TeleOp")
public class SampleAutonomous extends OpMode {
    @Override
    public void init() {
    }

    @Override
    public void loop() {
    }
}
