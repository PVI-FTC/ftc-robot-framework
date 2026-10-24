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
 * The entries of the viewer's dropdown: TeleOps to drive live from the keyboard, the ready-made
 * scenarios to run, then every saved recording to replay (newest first). Plain data, so tests
 * can check the list without a window.
 */
public final class ViewerChoices {
    /** Text in front of a scenario's title. */
    public static final String RUN_PREFIX = "Run: ";
    /** Text in front of a recording's file name. */
    public static final String REPLAY_PREFIX = "Replay: ";
    /** Text in front of a TeleOp to drive live. */
    public static final String DRIVE_PREFIX = "Drive: ";

    /**
     * One dropdown entry: a TeleOp to drive live, a scenario to run, or a recording to replay.
     * Exactly one of {@code scenario}, {@code file} and {@code drive} is set.
     *
     * @param label    what the dropdown shows
     * @param scenario the scenario to run, or null
     * @param file     the recording to replay, or null
     * @param drive    the TeleOp to drive live from the keyboard, or null
     */
    public record Choice(String label, Scenarios.Scenario scenario, Path file, LiveDrive.Option drive) {

        /** True if picking this runs a scenario. */
        public boolean isScenario() {
            return scenario != null;
        }

        /** True if picking this starts a live keyboard drive. */
        public boolean isDrive() {
            return drive != null;
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

    /**
     * The dropdown's entries: live drives, then scenarios. Saved recordings are not listed here;
     * the window's Replays button opens them ({@link ReplayPicker}).
     */
    public static List<Choice> runnable(List<LiveDrive.Option> drives, List<Scenarios.Scenario> scenarios) {
        List<Choice> choices = new ArrayList<>();
        for (LiveDrive.Option drive : drives) {
            choices.add(new Choice(DRIVE_PREFIX + drive.title(), null, null, drive));
        }
        for (Scenarios.Scenario scenario : scenarios) {
            choices.add(new Choice(RUN_PREFIX + scenario.title(), scenario, null, null));
        }
        return choices;
    }

    /** Scenarios first (in their order), then the recordings in {@code folder}, newest first. */
    public static List<Choice> list(List<Scenarios.Scenario> scenarios, Path folder) {
        return list(List.of(), scenarios, folder);
    }

    /** Live drives first, then scenarios (in their order), then the recordings in {@code folder}, newest first. */
    public static List<Choice> list(List<LiveDrive.Option> drives, List<Scenarios.Scenario> scenarios, Path folder) {
        List<Choice> choices = new ArrayList<>();
        for (LiveDrive.Option drive : drives) {
            choices.add(new Choice(DRIVE_PREFIX + drive.title(), null, null, drive));
        }
        for (Scenarios.Scenario scenario : scenarios) {
            choices.add(new Choice(RUN_PREFIX + scenario.title(), scenario, null, null));
        }
        for (Path file : recordingsNewestFirst(folder)) {
            choices.add(new Choice(REPLAY_PREFIX + file.getFileName(), null, file, null));
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
