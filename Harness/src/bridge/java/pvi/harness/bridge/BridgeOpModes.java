package pvi.harness.bridge;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import pvi.harness.hardware.FakeHardwareMap;
import pvi.harness.hardware.RobotConfigs;
import pvi.harness.runtime.HarnessConfig;
import pvi.harness.runtime.MatchPeriod;
import pvi.harness.runtime.OpModeCatalog;

/**
 * The OpModes Unity can run over the live connection: the real, unchanged TeamCode classes, each
 * on its fake robot. Unity picks one by its {@code id} in a "load" message.
 *
 * <p>There is no list to edit: every OpMode the harness finds in TeamCode ({@link OpModeCatalog},
 * the {@code @TeleOp} / {@code @Autonomous} classes in {@code teamCode.opModePackages}) is offered,
 * in Driver Station name order. Each runs on the fake robot {@link RobotConfigs#forOpMode} picks:
 * the robot described in {@code harness.properties}, except that with {@code pvi.extras=true}
 * PVI's Team B and Team C OpModes keep their own (manager decision D7, 2026-10-27).</p>
 */
public final class BridgeOpModes {

    /**
     * One OpMode Unity can run.
     *
     * @param id        what Unity sends in "load": the class's simple name, e.g. "TeamATeleOp"
     *                  (the full class name if two OpModes share a simple name)
     * @param title     what Unity shows in its menu: the Driver Station name, e.g. "Team A Mecanum Drive"
     * @param period    AUTONOMOUS (30 s) or TELEOP (2:00): the match length, after which the harness stops it
     * @param opMode    makes a fresh OpMode
     * @param robot     makes its fake robot
     */
    public record Entry(String id, String title, MatchPeriod period, Supplier<OpMode> opMode,
                        Supplier<FakeHardwareMap> robot) {
    }

    /** Only static methods here; nothing to create. */
    private BridgeOpModes() {
    }

    /** Every OpMode Unity can run, for the settings in {@code harness.properties}. */
    public static List<Entry> all() {
        return all(HarnessConfig.load());
    }

    /** Every OpMode Unity can run with these settings, in Driver Station name order. */
    public static List<Entry> all(HarnessConfig config) {
        List<OpModeCatalog.Entry> found = OpModeCatalog.fromConfig(config);
        Set<String> simpleNames = new HashSet<>();
        Set<String> sharedNames = new HashSet<>();
        for (OpModeCatalog.Entry e : found) {
            if (!simpleNames.add(e.simpleName())) {
                sharedNames.add(e.simpleName());
            }
        }
        List<Entry> entries = new ArrayList<>();
        for (OpModeCatalog.Entry e : found) {
            String id = sharedNames.contains(e.simpleName()) ? e.className() : e.simpleName();
            entries.add(new Entry(id, e.name(), e.period(), e::newOpMode, () -> RobotConfigs.forOpMode(e, config)));
        }
        return entries;
    }

    /** The entry with this id, or null if there is none. */
    public static Entry find(String id) {
        for (Entry entry : all()) {
            if (entry.id().equals(id)) {
                return entry;
            }
        }
        return null;
    }
}
