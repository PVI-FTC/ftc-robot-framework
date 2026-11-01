package pvi.harness.runtime.catalogbroken;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

/** Test sample: a TeleOp next to {@link BrokenAuto} that always loads. */
@TeleOp(name = "Working TeleOp", group = "Samples")
public class WorkingTeleOp extends OpMode {
    @Override public void init() { }
    @Override public void loop() { }
}
