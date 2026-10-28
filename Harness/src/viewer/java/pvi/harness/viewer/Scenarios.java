package pvi.harness.viewer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import pvi.harness.hardware.RobotConfigs;
import pvi.harness.runtime.HarnessConfig;
import pvi.harness.runtime.MatchPeriod;
import pvi.harness.runtime.OpModeCatalog;
import pvi.harness.runtime.OpModeRunner;
import pvi.harness.runtime.RunRecorder;
import pvi.harness.runtime.RunRecording;
import pvi.harness.runtime.RunnerRegistry;

/**
 * Ready-made runs the viewer's dropdown can perform: each runs the real, unchanged robot code in
 * the harness (fake hardware, scripted gamepads, simulated time) and returns its recording.
 *
 * <p>With {@code pvi.extras=true}, these are PVI's hand-made ones ({@code PviScenarios}, in
 * {@code src/pviViewer/java}). In any other repo they come from the robot code's own OpModes
 * ({@link OpModeCatalog}): each autonomous runs for a whole autonomous period, and each TeleOp
 * stands still for a few seconds after PLAY (manager decision D11). This class never names a team's
 * OpModes, so the viewer builds in any repo.</p>
 */
public final class Scenarios {

    /** The PVI extra's class, built only with {@code pvi.extras=true}. */
    private static final String PVI_SCENARIOS = "pvi.harness.viewer.PviScenarios";
    /** How long a generic TeleOp scenario runs after PLAY, in seconds. */
    static final double TELEOP_IDLE_SECONDS = 3.0;

    /**
     * One ready-made run.
     *
     * @param id          short name, used for the saved file, e.g. "teamA-autonomous"
     * @param title       what the dropdown shows, e.g. "Team A autonomous (30 s)"
     * @param description one sentence on what happens
     * @param run         runs it and returns the recording
     */
    public record Scenario(String id, String title, String description, Supplier<RunRecording> run) {

        /**
         * Runs the scenario. Takes well under a second, because time is simulated. The robot
         * code reads simulated time only when the harness Java agent is loaded (as in tests and
         * {@code gradlew viewRecording}).
         */
        public RunRecording perform() {
            // Each run makes a new OpModeRunner; forget the previous ones (only tests read them).
            RunnerRegistry.startTest();
            return run.get();
        }
    }

    /** Only static methods here; nothing to create. */
    private Scenarios() {
    }

    /** Every scenario, in the order the dropdown lists them: PVI's if built, otherwise the generic ones. */
    public static List<Scenario> all() {
        List<Scenario> pvi = pviScenarios();
        return pvi != null ? pvi : fromCatalog(HarnessConfig.load());
    }

    /**
     * One scenario per OpMode the robot code has ({@code teamCode.opModePackages}), leaving out
     * {@code smokeTest.skipOpModes} (OpModes for a different robot): an autonomous runs for 30 s,
     * a TeleOp stands still for {@value #TELEOP_IDLE_SECONDS} s. Each runs on the robot
     * {@link RobotConfigs#forOpMode} picks.
     */
    static List<Scenario> fromCatalog(HarnessConfig config) {
        List<Scenario> scenarios = new ArrayList<>();
        for (OpModeCatalog.Entry e : OpModeCatalog.fromConfig(config)) {
            if (config.smokeTestSkipOpModes().contains(e.name())) {
                continue;
            }
            boolean autonomous = e.period() == MatchPeriod.AUTONOMOUS;
            double seconds = autonomous ? MatchPeriod.AUTONOMOUS.seconds() : TELEOP_IDLE_SECONDS;
            String title = e.name() + (autonomous ? " (30 s)" : " (standing still, 3 s)");
            String description = autonomous
                    ? e.name() + " for a whole autonomous period, on the robot in harness.properties."
                    : e.name() + ": INIT, PLAY and no gamepad input for a few seconds.";
            scenarios.add(new Scenario(e.simpleName() + (autonomous ? "-autonomous" : "-idle"), title, description,
                    () -> record(new OpModeRunner(e.newOpMode(), RobotConfigs.forOpMode(e, config)), seconds)));
        }
        return scenarios;
    }

    /**
     * PVI's hand-made scenarios, or null when the PVI extras aren't built. Looked up by name
     * because this always-built class can't refer to a class that may not exist.
     */
    @SuppressWarnings("unchecked")
    private static List<Scenario> pviScenarios() {
        try {
            return (List<Scenario>) Class.forName(PVI_SCENARIOS).getMethod("all").invoke(null);
        } catch (ClassNotFoundException e) {
            return null;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not read PVI's scenarios from " + PVI_SCENARIOS, e);
        }
    }

    /** Runs with 1 s of INIT and {@code matchSeconds} of match, and returns the recording. */
    static RunRecording record(OpModeRunner runner, double matchSeconds) {
        RunRecorder recorder = RunRecorder.attachTo(runner);
        runner.run(1.0, matchSeconds).assertNoException();
        return recorder.recording();
    }
}
