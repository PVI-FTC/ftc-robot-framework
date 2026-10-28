package pvi.harness.viewer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Step 15, option A: which recording the viewer opens after {@code gradlew test} when
 * {@code viewer.open=true}. All of it is plain file logic, so it's tested without a screen; the
 * tests never open a window.
 */
class AfterTestsViewerTest {
    /** "Now" for these tests: the run started at this time (milliseconds). */
    private static final long RUN_START = 1_800_000_000_000L;

    @TempDir
    Path folder;

    /** Writes a file whose last-changed time is {@code secondsAfterStart} after the run started. */
    private Path file(String name, double secondsAfterStart) throws IOException {
        Path file = folder.resolve(name);
        Files.writeString(file, "{}");
        Files.setLastModifiedTime(file, FileTime.fromMillis(RUN_START + Math.round(secondsAfterStart * 1000)));
        return file;
    }

    private Optional<String> chosen(List<String> failed) {
        return AfterTestsViewer.choose(folder, RUN_START, failed).map(p -> p.getFileName().toString());
    }

    /**
     * WHAT: When every test passed, the newest recording from this run is opened; files from
     * before the run don't count.
     */
    @Test
    void opensThisRunsNewestRecording() throws IOException {
        file("OldTest-earlier.json", -60);      // an earlier run
        file("RunRecordingTest-auto.json", 5);
        file("ViewerReplayTest-drive.json", 9);
        file("notes.txt", 12);                   // not a recording

        assertEquals(Optional.of("ViewerReplayTest-drive.json"), chosen(List.of()));
    }

    /**
     * WHAT: When a test failed, its own newest recording is opened, even if a passing test saved a
     * newer one.
     *
     * <p>WHY: After a failure, the run worth watching is the failed one.</p>
     */
    @Test
    void opensTheFailedTestsRecording() throws IOException {
        file("PedroRouteTest-decode.json", 3);
        file("PedroRouteTest-curving.json", 4);
        file("ViewerReplayTest-drive.json", 9);

        assertEquals(Optional.of("PedroRouteTest-curving.json"),
                chosen(List.of("pvi.harness.physics.PedroRouteTest")));
    }

    /**
     * WHAT: A failed test that saved no recording falls back to the run's newest; a run with no
     * recordings at all gives nothing (the viewer stays closed).
     */
    @Test
    void fallsBackToNewestOrNothing() throws IOException {
        assertEquals(Optional.empty(), chosen(List.of()), "no recordings yet");

        file("RunRecordingTest-auto.json", 5);
        assertEquals(Optional.of("RunRecordingTest-auto.json"), chosen(List.of("pvi.harness.SomeOtherTest")));
    }

    /**
     * WHAT: Failed test classes are read from JUnit's result files written during this run;
     * passing classes and result files from an earlier run are left out.
     */
    @Test
    void readsFailedClassesFromThisRunsResults() throws IOException {
        Files.writeString(folder.resolve("TEST-pvi.harness.A.xml"),
                "<?xml version=\"1.0\"?>\n<testsuite name=\"pvi.harness.A\" tests=\"3\" skipped=\"0\" failures=\"1\" errors=\"0\">");
        Files.setLastModifiedTime(folder.resolve("TEST-pvi.harness.A.xml"), FileTime.fromMillis(RUN_START + 2000));
        Files.writeString(folder.resolve("TEST-pvi.harness.B.xml"),
                "<testsuite name=\"pvi.harness.B\" tests=\"2\" skipped=\"0\" failures=\"0\" errors=\"0\">");
        Files.setLastModifiedTime(folder.resolve("TEST-pvi.harness.B.xml"), FileTime.fromMillis(RUN_START + 2000));
        Files.writeString(folder.resolve("TEST-pvi.harness.C.xml"),
                "<testsuite name=\"pvi.harness.C\" tests=\"1\" skipped=\"0\" failures=\"0\" errors=\"1\">");
        Files.setLastModifiedTime(folder.resolve("TEST-pvi.harness.C.xml"), FileTime.fromMillis(RUN_START - 60_000));

        assertEquals(List.of("pvi.harness.A"), AfterTestsViewer.failedTestClasses(folder, RUN_START));
    }
}
