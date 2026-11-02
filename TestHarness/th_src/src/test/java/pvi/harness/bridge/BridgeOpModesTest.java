package pvi.harness.bridge;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import pvi.harness.runtime.HarnessConfig;
import pvi.harness.runtime.MatchPeriod;
import pvi.harness.runtime.OpModeCatalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * The OpModes Unity can run live: found in TeamCode by annotation, with no hand-written list, and
 * each on the right fake robot (manager decision D7).
 */
class BridgeOpModesTest {

    private static BridgeOpModes.Entry entry(List<BridgeOpModes.Entry> all, String id) {
        return all.stream().filter(e -> e.id().equals(id)).findFirst().orElse(null);
    }

    /**
     * WHAT: Every enabled TeamCode OpMode is offered, under its class name as the id and its
     * Driver Station name as the title, each id only once.
     *
     * <p>WHY: A new OpMode shows up in Unity without anyone editing a list.</p>
     */
    @Test
    void everyTeamCodeOpModeIsOfferedByClassName() {
        List<BridgeOpModes.Entry> all = BridgeOpModes.all(HarnessConfig.load());

        Set<String> ids = new HashSet<>();
        all.forEach(e -> ids.add(e.id()));
        assertEquals(all.size(), ids.size(), "ids are unique");
        assertEquals(OpModeCatalog.fromConfig(HarnessConfig.load()).size(), all.size(), "one entry per OpMode found");

        BridgeOpModes.Entry teleOp = BridgeTestOpModes.teleOp();
        assertEquals(teleOp.id(), teleOp.opMode().get().getClass().getSimpleName(), "the id is the class name");
        assertNotNull(BridgeOpModes.find(teleOp.id()), "find() uses the same ids");

        BridgeTestOpModes.assumePvi(); // PVI's own OpModes, by name
        BridgeOpModes.Entry teamA = entry(all, "TeamATeleOp");
        assertNotNull(teamA);
        assertEquals("Team A Mecanum Drive", teamA.title());
        assertEquals(MatchPeriod.TELEOP, teamA.period());
        assertEquals(MatchPeriod.AUTONOMOUS, entry(all, "TeamAAutoOpMode").period());
        assertNotNull(entry(all, "RumbleDemoTeleOp"));
    }

    /**
     * WHAT: With pvi.extras on (PVI's harness.properties), the Team B and Team C OpModes keep
     * their own fake robots; everything else runs on the robot in harness.properties ("teamA").
     */
    @Test
    void teamBAndCKeepTheirRobotsWithPviExtras() {
        BridgeTestOpModes.assumePvi(); // PVI's Team B / Team C OpModes and robots
        List<BridgeOpModes.Entry> all = BridgeOpModes.all(HarnessConfig.load());

        assertEquals("teamB", entry(all, "TeamBTeleOp").robot().get().configName());
        assertEquals("teamC", entry(all, "TeamCTeleOp").robot().get().configName());
        assertEquals("teamA", entry(all, "TeamATeleOp").robot().get().configName());
        assertEquals("teamA", entry(all, "RumbleDemoTeleOp").robot().get().configName());
    }
}
