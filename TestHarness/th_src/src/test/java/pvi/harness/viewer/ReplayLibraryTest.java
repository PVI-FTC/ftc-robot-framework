package pvi.harness.viewer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Replays are kept apart from the dropdown: the dropdown has only drives and scenarios, and the
 * Replays window lists the saved recordings with what made them.
 */
class ReplayLibraryTest {

    private static Path file(Path folder, String name, int bytes, long secondsAgo) throws IOException {
        Path file = folder.resolve(name);
        Files.write(file, new byte[bytes]);
        Files.setLastModifiedTime(file, FileTime.from(Instant.now().minusSeconds(secondsAgo)));
        return file;
    }

    /**
     * WHAT: Every recording is listed newest first, with its kind (from the file name), size and
     * name without ".json".
     *
     * <p>WHY: The newest drive is usually the one you want; the kind tells your own drives apart
     * from scenario runs and test output.</p>
     */
    @Test
    void listsRecordingsNewestFirstWithTheirKind(@TempDir Path folder) throws IOException {
        file(folder, "RunRecordingTest-teamAAutonomous.json", 2048, 300);
        file(folder, "viewer-teamA-autonomous.json", 3 * 1024 * 1024, 200);
        file(folder, "drive-teamA-teleop-20261023-181935.json", 812 * 1024, 100);
        file(folder, "notes.txt", 10, 0);   // not a recording

        List<ReplayLibrary.Entry> entries = ReplayLibrary.list(folder);

        assertEquals(3, entries.size());
        assertEquals("drive-teamA-teleop-20261023-181935", entries.get(0).name());
        assertEquals(ReplayLibrary.Kind.DRIVE, entries.get(0).kind());
        assertEquals("812 KB", entries.get(0).sizeText());
        assertEquals(ReplayLibrary.Kind.SCENARIO, entries.get(1).kind());
        assertEquals("3.0 MB", entries.get(1).sizeText());
        assertEquals(ReplayLibrary.Kind.TEST, entries.get(2).kind());
        assertEquals(List.of(), ReplayLibrary.list(folder.resolve("missing")));
    }

    /**
     * WHAT: The filter matches the name or the kind, ignoring case; blank matches everything.
     *
     * <p>WHY: Typing "drive" shows just your drives; typing "teamB" finds Team B's.</p>
     */
    @Test
    void filterMatchesNameOrKind(@TempDir Path folder) throws IOException {
        ReplayLibrary.Entry drive = new ReplayLibrary.Entry(folder.resolve("x.json"), "drive-teamB-teleop-1",
                ReplayLibrary.Kind.DRIVE, Instant.now(), 1);
        assertTrue(drive.matches(""));
        assertTrue(drive.matches("TEAMb"));
        assertTrue(drive.matches(" drive "));
        assertFalse(drive.matches("scenario"));
    }

    /**
     * WHAT: The dropdown lists only drives and scenarios, never saved recordings.
     *
     * <p>WHY: Replays have their own window; mixing them into the dropdown made it long and hard
     * to use.</p>
     */
    @Test
    void dropdownHasOnlyDrivesAndScenarios() {
        List<ViewerChoices.Choice> choices = ViewerChoices.runnable(LiveDrive.options(), Scenarios.all());

        assertEquals(LiveDrive.options().size() + Scenarios.all().size(), choices.size());
        assertTrue(choices.stream().allMatch(c -> c.isDrive() || c.isScenario()));
        assertTrue(choices.stream().noneMatch(c -> c.label().startsWith(ViewerChoices.REPLAY_PREFIX)));
    }
}
