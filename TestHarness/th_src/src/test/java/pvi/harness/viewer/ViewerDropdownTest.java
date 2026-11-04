package pvi.harness.viewer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import pvi.harness.physics.Pose;
import pvi.harness.runtime.MatchPeriod;
import pvi.harness.runtime.RunRecording;
import pvi.harness.runtime.RunRecordingJson;
import pvi.harness.runtime.RunResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The viewer's dropdown: ready-made scenarios to run, and saved recordings to replay.
 *
 * <p>Every scenario is run here, so a TeamCode change that breaks one is caught by
 * {@code gradlew test}, not first noticed in the window.</p>
 */
class ViewerDropdownTest {

    /**
     * WHAT: Every scenario runs the real OpMode to the end without crashing, and its recording
     * saves and reads back.
     *
     * <p>WHY: Picking an entry in the dropdown must never fail for a student. Each scenario's
     * id must also be unique and usable as a file name, because the run is saved under it.</p>
     */
    @Test
    void everyScenarioRunsAndItsRecordingSaves() {
        Set<String> ids = new HashSet<>();
        for (Scenarios.Scenario scenario : Scenarios.all()) {
            RunRecording recording = scenario.perform();

            assertTrue(ids.add(scenario.id()), "duplicate scenario id " + scenario.id());
            assertTrue(recording.loops().size() > 50, scenario.title() + " should record a run");
            assertEquals(RunResult.Stage.STOP, last(recording).stage(),
                    scenario.title() + " should run to STOP");
            Path saved = ViewerWindow.saveScenarioRun(scenario, recording);
            assertTrue(saved.endsWith(Path.of("build", "recordings", "viewer-" + scenario.id() + ".json")), saved.toString());
            assertEquals(RunRecordingJson.format(recording), RunRecordingJson.format(RunRecordingJson.load(saved)));
        }
    }

    /**
     * WHAT: The scenarios do what their titles say.
     *
     * <p>HOW: The autonomous is a full 30 s AUTONOMOUS run ending about 9.4 in forward; the
     * turn scenario turns clockwise in place; the miswired one curves counter-clockwise (left);
     * the rumble demo buzzes the driver 4 times.</p>
     */
    @Test
    void scenariosDoWhatTheirTitlesSay() {
        RunRecording autonomous = PviScenarios.teamAAutonomous().perform();
        assertEquals(MatchPeriod.AUTONOMOUS, autonomous.header().period());
        assertEquals(30.0, last(autonomous).matchTimeSeconds(), 0.03);
        assertEquals(9.4, last(autonomous).pose().xInches(), 0.5);

        Pose turned = last(PviScenarios.teleOpTurnInPlace().perform()).pose();
        assertTrue(turned.headingDegrees() < -45, "clockwise turn: " + turned);
        assertEquals(0.0, turned.distanceInchesTo(Pose.ORIGIN), 1e-6, "in place");

        Pose miswired = last(PviScenarios.teleOpMiswiredFrontLeft().perform()).pose();
        assertTrue(miswired.headingDegrees() > 30, "curves left: " + miswired);

        assertEquals(4, PviScenarios.rumbleDemoMatch().perform().gamepad1Rumbles().size());
    }

    /**
     * WHAT: The dropdown lists the scenarios first, then the saved recordings, newest first.
     *
     * <p>HOW: A folder with two recordings and a text file. The list must be every scenario
     * ("Run: ..."), then "Replay: newer.json", then "Replay: older.json", and no text file.</p>
     */
    @Test
    void dropdownListsScenariosThenRecordingsNewestFirst(@TempDir Path folder) throws IOException {
        Path older = Files.writeString(folder.resolve("older.json"), "{}");
        Path newer = Files.writeString(folder.resolve("newer.json"), "{}");
        Files.writeString(folder.resolve("notes.txt"), "not a recording");
        Files.setLastModifiedTime(older, FileTime.fromMillis(1_000_000));
        Files.setLastModifiedTime(newer, FileTime.fromMillis(2_000_000));
        List<Scenarios.Scenario> scenarios = Scenarios.all();

        List<ViewerChoices.Choice> choices = ViewerChoices.list(scenarios, folder);

        assertEquals(scenarios.size() + 2, choices.size());
        for (int i = 0; i < scenarios.size(); i++) {
            assertTrue(choices.get(i).isScenario());
            assertEquals("Run: " + scenarios.get(i).title(), choices.get(i).label());
        }
        assertEquals("Replay: newer.json", choices.get(scenarios.size()).label());
        assertEquals(newer, choices.get(scenarios.size()).file());
        assertEquals("Replay: older.json", choices.get(scenarios.size() + 1).label());
        assertFalse(choices.get(scenarios.size() + 1).isScenario());
    }

    /** WHAT: With no recordings folder yet, the dropdown still lists the scenarios. */
    @Test
    void dropdownWorksBeforeAnyRecordingExists(@TempDir Path folder) {
        List<ViewerChoices.Choice> choices = ViewerChoices.list(Scenarios.all(), folder.resolve("missing"));

        assertEquals(Scenarios.all().size(), choices.size());
    }

    private static RunRecording.LoopRecord last(RunRecording recording) {
        return recording.loops().get(recording.loops().size() - 1);
    }
}
