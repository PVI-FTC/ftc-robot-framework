package pvi.harness.viewer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * The entries of the viewer's dropdown: first the ready-made scenarios to run, then every saved
 * recording to replay (newest first). Plain data, so tests can check the list without a window.
 */
public final class ViewerChoices {
    /** Text in front of a scenario's title. */
    public static final String RUN_PREFIX = "Run: ";
    /** Text in front of a recording's file name. */
    public static final String REPLAY_PREFIX = "Replay: ";

    /**
     * One dropdown entry: either a scenario to run or a recording file to replay.
     *
     * @param label    what the dropdown shows
     * @param scenario the scenario to run, or null for a recording
     * @param file     the recording to replay, or null for a scenario
     */
    public record Choice(String label, Scenarios.Scenario scenario, Path file) {

        /** True if picking this runs a scenario (false: it replays a file). */
        public boolean isScenario() {
            return scenario != null;
        }

        /** The dropdown shows the label. */
        @Override
        public String toString() {
            return label;
        }
    }

    /** Only static methods here; nothing to create. */
    private ViewerChoices() {
    }

    /** Scenarios first (in their order), then the recordings in {@code folder}, newest first. */
    public static List<Choice> list(List<Scenarios.Scenario> scenarios, Path folder) {
        List<Choice> choices = new ArrayList<>();
        for (Scenarios.Scenario scenario : scenarios) {
            choices.add(new Choice(RUN_PREFIX + scenario.title(), scenario, null));
        }
        for (Path file : recordingsNewestFirst(folder)) {
            choices.add(new Choice(REPLAY_PREFIX + file.getFileName(), null, file));
        }
        return choices;
    }

    /** Every {@code .json} file in {@code folder}, most recently changed first; empty if there is no folder. */
    public static List<Path> recordingsNewestFirst(Path folder) {
        if (!Files.isDirectory(folder)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(folder)) {
            return files.filter(f -> f.getFileName().toString().endsWith(".json"))
                    .sorted(Comparator.comparing(ViewerChoices::lastModified).reversed()
                            .thenComparing(Comparator.naturalOrder()))
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not list " + folder.toAbsolutePath(), e);
        }
    }

    /** When a file last changed, in milliseconds. */
    private static long lastModified(Path file) {
        try {
            return Files.getLastModifiedTime(file).toMillis();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read the date of " + file, e);
        }
    }
}
