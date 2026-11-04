package pvi.harness.viewer;

import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import pvi.harness.runtime.RunRecordingJson;

/**
 * Opens the viewer after {@code gradlew test} finishes, when {@code viewer.open=true} in
 * {@code harness.properties} (Step 15, option A, the user's choice). {@code build.gradle} runs
 * this right after the test task, passing the time the tests started.
 *
 * <p>Which recording it opens ({@link #choose}): if a test failed, the newest recording from this
 * run whose name starts with that test class's name (tests save recordings as
 * {@code <TestClass>-<name>.json}); otherwise the newest recording from this run. The dropdown
 * lists every recording, this run's first.</p>
 *
 * <p>With no screen (a build server, or {@code java.awt.headless=true}) it prints one line and
 * does nothing. The tests themselves never open a window.</p>
 */
public final class AfterTestsViewer {
    /** Matches the start of a JUnit result file: {@code <testsuite name="pvi.harness.X" ... failures="1" errors="0"}. */
    private static final Pattern SUITE = Pattern.compile(
            "<testsuite name=\"([^\"]+)\"[^>]*?failures=\"(\\d+)\"[^>]*?errors=\"(\\d+)\"");

    /** Only static methods here; nothing to create. */
    private AfterTestsViewer() {
    }

    /**
     * {@code args[0]}: when the test run started (milliseconds since 1970, as
     * {@code System.currentTimeMillis()}). {@code args[1]}: the JUnit results folder.
     */
    public static void main(String[] args) {
        if (GraphicsEnvironment.isHeadless()) {
            System.out.println("viewer.open is true, but there is no screen here, so the viewer stays closed.");
            return;
        }
        long runStart = Long.parseLong(args[0]);
        Path results = Path.of(args[1]);
        Optional<Path> file = choose(RunRecordingJson.DEFAULT_FOLDER, runStart, failedTestClasses(results, runStart));
        if (file.isEmpty()) {
            System.out.println("viewer.open is true, but this test run saved no recordings, so the viewer stays closed.");
            return;
        }
        System.out.println("Opening the viewer on " + file.get().getFileName() + " (close the window to finish).");
        RecordingViewer.main(new String[] {file.get().toString()});
    }

    /**
     * The recording to open after a test run.
     *
     * @param folder            where recordings are saved ({@code build/recordings})
     * @param runStartMillis    when the run started; older files are from earlier runs
     * @param failedTestClasses full names of the test classes that failed (may be empty)
     * @return the file, or empty if the run saved no recordings
     */
    public static Optional<Path> choose(Path folder, long runStartMillis, List<String> failedTestClasses) {
        List<Path> thisRun = recordingsSince(folder, runStartMillis);
        for (String failed : failedTestClasses) {
            String prefix = failed.substring(failed.lastIndexOf('.') + 1) + "-";
            Optional<Path> match = thisRun.stream()
                    .filter(f -> f.getFileName().toString().startsWith(prefix)).findFirst();
            if (match.isPresent()) {
                return match;
            }
        }
        return thisRun.stream().findFirst();
    }

    /**
     * The {@code .json} recordings in {@code folder} changed at or after {@code sinceMillis},
     * newest first (ties by name, so the answer never depends on the order the files are listed in).
     */
    static List<Path> recordingsSince(Path folder, long sinceMillis) {
        if (!Files.isDirectory(folder)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(folder)) {
            return files.filter(f -> f.getFileName().toString().endsWith(".json"))
                    .filter(f -> modifiedMillis(f) >= sinceMillis)
                    .sorted(Comparator.comparingLong(AfterTestsViewer::modifiedMillis).reversed()
                            .thenComparing(f -> f.getFileName().toString()))
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not list " + folder, e);
        }
    }

    /**
     * The test classes with a failure or an error, from JUnit's {@code TEST-*.xml} result files
     * written at or after {@code sinceMillis} (older ones are from earlier runs), in name order.
     */
    public static List<String> failedTestClasses(Path resultsFolder, long sinceMillis) {
        TreeSet<String> failed = new TreeSet<>();
        if (!Files.isDirectory(resultsFolder)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(resultsFolder)) {
            for (Path file : files.filter(f -> f.getFileName().toString().startsWith("TEST-"))
                    .filter(f -> modifiedMillis(f) >= sinceMillis).toList()) {
                Matcher m = SUITE.matcher(Files.readString(file));
                if (m.find() && (Integer.parseInt(m.group(2)) > 0 || Integer.parseInt(m.group(3)) > 0)) {
                    failed.add(m.group(1));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read the test results in " + resultsFolder, e);
        }
        return new ArrayList<>(failed);
    }

    private static long modifiedMillis(Path file) {
        try {
            return Files.getLastModifiedTime(file).toMillis();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read the time of " + file, e);
        }
    }
}
