package pvi.harness.runtime.catalogbroken;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;

/** Test sample: an autonomous that can't load when {@link NeededHelper} is missing. */
@Autonomous(name = "Broken Auto", group = "Samples")
public class BrokenAuto extends NeededHelper {
    @Override public void init() { }
    @Override public void loop() { }
}
