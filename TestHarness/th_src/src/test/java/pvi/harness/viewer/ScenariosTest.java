package pvi.harness.viewer;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import pvi.harness.runtime.HarnessConfig;
import pvi.harness.runtime.MatchPeriod;
import pvi.harness.runtime.OpModeCatalog;
import pvi.harness.runtime.RunRecording;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Manager decision D11: the viewer's scenarios. PVI keeps its hand-made ones; any other repo gets
 * one per OpMode from the robot code, so the viewer never has to name a team's OpModes.
 */
class ScenariosTest {

    /**
     * WHAT: With the PVI extras built, the dropdown starts with PVI's hand-made five, in the same
     * order as before D11.
     */
    @Test
    void pviKeepsItsHandMadeScenarios() {
        Assumptions.assumeTrue(HarnessConfig.load().pviExtras(), "PVI only");
        List<String> ids = Scenarios.all().stream().map(Scenarios.Scenario::id).toList();

        assertEquals(List.of("teamA-autonomous", "teamA-teleop-demo-drive", "teamA-teleop-turn",
                "teamA-teleop-miswired", "rumble-demo-match"), ids.subList(0, 5));
    }

    /**
     * WHAT: After them, PVI's dropdown offers every other autonomous in TeamCode (e.g. "Team A
     * Pedro Paths") as its generic 30 s run, once each; the one the hand-made scenario covers
     * (TeamAAutoOpMode) isn't offered twice, and no TeleOp is added.
     *
     * <p>WHY: W21: "Team A Pedro Paths" was never offered, so a new autonomous must show up by itself.</p>
     */
    @Test
    void pviAlsoOffersEveryOtherAutonomous() {
        HarnessConfig config = HarnessConfig.load();
        Assumptions.assumeTrue(config.pviExtras(), "PVI only");
        List<String> added = Scenarios.all().stream().skip(5).map(Scenarios.Scenario::id).toList();

        List<String> expected = OpModeCatalog.fromConfig(config).stream()
                .filter(e -> e.period() == MatchPeriod.AUTONOMOUS && !e.simpleName().equals("TeamAAutoOpMode")
                        && !config.smokeTestSkipOpModes().contains(e.name()))
                .map(e -> e.simpleName() + "-autonomous").toList();
        assertEquals(expected, added);
        assertTrue(added.contains("TeamAPedroPilotAutoOpMode-autonomous"), "Team A Pedro Paths: " + added);
    }

    /**
     * WHAT: The generic scenarios list every OpMode once (an autonomous runs 30 s, a TeleOp stands
     * still), leave out smokeTest.skipOpModes, and have unique ids. Works on any team's code.
     */
    @Test
    void genericScenariosComeFromTheRobotCode() {
        HarnessConfig config = HarnessConfig.load();
        List<Scenarios.Scenario> generic = Scenarios.fromCatalog(config);
        List<String> ids = generic.stream().map(Scenarios.Scenario::id).toList();
        List<OpModeCatalog.Entry> kept = OpModeCatalog.fromConfig(config).stream()
                .filter(e -> !config.smokeTestSkipOpModes().contains(e.name())).toList();

        assertEquals(kept.size(), generic.size(), "one per OpMode not on smokeTest.skipOpModes: " + ids);
        for (OpModeCatalog.Entry e : kept) {
            String expected = e.simpleName() + (e.period() == MatchPeriod.AUTONOMOUS ? "-autonomous" : "-idle");
            assertTrue(ids.contains(expected), expected + " in " + ids);
        }
        Set<String> unique = new HashSet<>(ids);
        assertEquals(ids.size(), unique.size(), "ids must be unique: " + ids);
    }

    /** WHAT: On PVI, "KimCode TeleopV1" (on smokeTest.skipOpModes) has no scenario. */
    @Test
    void pviSkipsKimCodesTeleOp() {
        Assumptions.assumeTrue(HarnessConfig.load().pviExtras(), "PVI only");
        List<String> ids = Scenarios.fromCatalog(HarnessConfig.load()).stream().map(Scenarios.Scenario::id).toList();

        assertTrue(ids.contains("TeamATeleOp-idle"), ids.toString());
        assertFalse(ids.contains("TeleopV1-idle"), "KimCode TeleopV1 is on smokeTest.skipOpModes: " + ids);
    }

    /**
     * WHAT: A generic TeleOp scenario really runs the OpMode: INIT, PLAY, a few seconds standing
     * still, no crash.
     */
    @Test
    void aGenericTeleOpScenarioRuns() {
        // The first TeleOp scenario, whatever the team's code calls it.
        Scenarios.Scenario teleOp = Scenarios.fromCatalog(HarnessConfig.load()).stream()
                .filter(s -> s.id().endsWith("-idle")).findFirst().orElseThrow();
        RunRecording recording = teleOp.perform();

        assertEquals(teleOp.id().replace("-idle", ""), recording.header().opMode());
        double lastSeconds = recording.loops().get(recording.loops().size() - 1).matchTimeSeconds();
        assertEquals(Scenarios.TELEOP_IDLE_SECONDS, lastSeconds, 0.05, "ran for the idle time");
    }
}
