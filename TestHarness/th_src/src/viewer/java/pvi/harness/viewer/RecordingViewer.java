package pvi.harness.viewer;

import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import pvi.harness.runtime.RunRecording;
import pvi.harness.runtime.RunRecordingJson;

/**
 * Opens the viewer window. From {@code Harness/}:
 *
 * <pre>
 *   .\gradlew.bat viewRecording                                          (newest recording)
 *   .\gradlew.bat viewRecording -Pfile=build\recordings\teamAAutonomous.json
 * </pre>
 *
 * <p>With no file given, it opens straight into <b>Drive: Team A TeleOp</b> (INIT, waiting for
 * Enter). With a file, it replays that recording. In the window, the dropdown drives other
 * TeleOps, runs scenarios or replays other recordings.</p>
 *
 * <p>Or run this class's {@code main} from Android Studio (green ▶), optionally with the file
 * path as the program argument. Running scenarios needs the harness Java agent, which
 * {@code gradlew viewRecording} loads for you.</p>
 */
public final class RecordingViewer {
    /** Only the main method here; nothing to create. */
    private RecordingViewer() {
    }

    /**
     * With a file in {@code args[0]}: replays it. Without one: starts a live drive of the first
     * TeleOp (Team A), showing the newest saved recording (or the first scenario) behind it for
     * the moment before the drive's first loop.
     */
    public static void main(String[] args) {
        try {
            Optional<Path> file = chooseRecording(args, RunRecordingJson.DEFAULT_FOLDER);
            RunRecording recording;
            String name;
            if (file.isPresent()) {
                System.out.println("Opening " + file.get().toAbsolutePath());
                recording = RunRecordingJson.load(file.get());
                name = file.get().getFileName().toString();
            } else {
                Scenarios.Scenario first = Scenarios.all().get(0);
                System.out.println("No recordings yet; running scenario: " + first.title());
                recording = first.perform();
                name = ViewerWindow.saveScenarioRun(first, recording).getFileName().toString();
            }
            boolean fileGiven = args.length > 0 && !args[0].isBlank();
            if (fileGiven) {
                ViewerWindow.open(recording, name);
            } else {
                LiveDrive.Option teamA = LiveDrive.defaultOption();
                System.out.println("Starting live drive: " + teamA.title() + " (press Enter for PLAY)");
                ViewerWindow.openDriving(recording, name, teamA);
            }
        } catch (IllegalArgumentException | IllegalStateException | UncheckedIOException e) {
            System.err.println("Could not open the viewer: " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * The file to open: {@code args[0]} if given (it must exist), otherwise the most recently
     * changed {@code .json} file in {@code folder}, or empty if there is none.
     */
    static Optional<Path> chooseRecording(String[] args, Path folder) {
        if (args.length > 0 && !args[0].isBlank()) {
            Path file = Path.of(args[0]);
            if (!Files.isRegularFile(file)) {
                throw new IllegalArgumentException("No recording file at " + file.toAbsolutePath());
            }
            return Optional.of(file);
        }
        List<Path> recordings = ViewerChoices.recordingsNewestFirst(folder);
        return recordings.isEmpty() ? Optional.empty() : Optional.of(recordings.get(0));
    }
}
