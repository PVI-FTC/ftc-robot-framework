package pvi.harness.runtime.catalogsample;

import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

/** Test-only OpMode for {@code OpModeCatalogTest}: marked {@code @Disabled}, so it must not be listed. */
@TeleOp(name = "Sample Disabled", group = "Samples")
@Disabled
public class SampleDisabledTeleOp extends OpMode {
    @Override
    public void init() {
    }

    @Override
    public void loop() {
    }
}
