package pvi.harness.bridge;

import java.util.List;

import pvi.harness.runtime.HarnessConfig;
import pvi.harness.runtime.MatchPeriod;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Which of this repo's OpModes the bridge tests drive, so they work in any team's repo (manager
 * decision D13), not just PVI's: the TeleOp named by {@code viewer.defaultTeleOp} in
 * harness.properties (its class name or Driver Station name), else the first TeleOp found; and the
 * first autonomous. Checks that only make sense for PVI's own robot code go behind
 * {@link #assumePvi()}.
 */
final class BridgeTestOpModes {
    /** Only static methods here; nothing to create. */
    private BridgeTestOpModes() {
    }

    /** The TeleOp the tests drive. */
    static BridgeOpModes.Entry teleOp() {
        List<BridgeOpModes.Entry> all = BridgeOpModes.all();
        String wanted = HarnessConfig.load().viewerDefaultTeleOp();
        for (BridgeOpModes.Entry e : all) {
            if (e.period() == MatchPeriod.TELEOP && wanted != null && (e.id().equals(wanted) || e.title().equals(wanted))) {
                return e;
            }
        }
        return all.stream().filter(e -> e.period() == MatchPeriod.TELEOP).findFirst()
                .orElseThrow(() -> new IllegalStateException("This repo has no TeleOp for the bridge tests to drive."));
    }

    /** The first autonomous; skips the test if the repo has none. */
    static BridgeOpModes.Entry autonomous() {
        BridgeOpModes.Entry auto = BridgeOpModes.all().stream().filter(e -> e.period() == MatchPeriod.AUTONOMOUS)
                .findFirst().orElse(null);
        assumeTrue(auto != null, "this repo has no autonomous OpMode");
        return auto;
    }

    /** The front-left drive motor's configuration name, from harness.properties ("frontLeft" for PVI). */
    static String frontLeft() {
        return HarnessConfig.load().driveMotors().frontLeft();
    }

    /** True in PVI's own repo (pvi.extras=true), where PVI's OpModes and robots are there to check. */
    static boolean pvi() {
        return HarnessConfig.load().pviExtras();
    }

    /** Skips the rest of a test outside PVI's repo. */
    static void assumePvi() {
        assumeTrue(pvi(), "PVI's own robot code only (pvi.extras=true)");
    }
}
