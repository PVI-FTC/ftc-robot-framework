package pvi.harness.recorded;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

import pvi.harness.runtime.RunRecording;
import pvi.harness.runtime.RunRecordingJson;

/**
 * Turns a saved run recording into a new test file. Run it with Gradle from {@code Harness\}:
 *
 * <pre>
 *   .\gradlew.bat makeTest                                               (newest drive-*.json)
 *   .\gradlew.bat makeTest "-Pfile=build\recordings\drive-....json"
 *   .\gradlew.bat makeTest "-Pfile=..." "-PtestName=IntakeDriveTest"     (choose the class name)
 * </pre>
 *
 * <p>The test is saved in {@code src/test/java/pvi/harness/recorded/}. An existing file is never
 * replaced. See {@link RecordedTestWriter} for what goes in it.</p>
 */
public final class MakeRecordedTest {

    private MakeRecordedTest() {
    }

    /** {@code args[0]}: recording file (optional); {@code args[1]}: class name (optional). */
    public static void main(String[] args) {
        Path file = args.length > 0 && !args[0].isBlank()
                ? Path.of(args[0])
                : newestDrive(RunRecordingJson.DEFAULT_FOLDER).orElseThrow(() -> new IllegalStateException(
                        "No drive recording found in " + RunRecordingJson.DEFAULT_FOLDER.toAbsolutePath()
                                + ". Record a drive in the viewer (Record on), or pass -Pfile=..."));
        if (!Files.isRegularFile(file)) {
            throw new IllegalArgumentException("Recording not found: " + file.toAbsolutePath());
        }
        RunRecording recording = RunRecordingJson.load(file);
        String className = args.length > 1 && !args[1].isBlank()
                ? args[1]
                : RecordedTestWriter.defaultClassName(recording);
        String opModeClass = RecordedTestWriter.findOpModeClass(recording.header().opMode());

        Path written = RecordedTestWriter.save(recording, RecordedTestWriter.DEFAULT_FOLDER,
                new RecordedTestWriter.Options(className, opModeClass, file.getFileName().toString(),
                        LocalDate.now().toString(), false));
        System.out.println("Saved a new test: " + written.toAbsolutePath());
        System.out.println("Run it with:  .\\gradlew.bat test --tests \"" + RecordedTestWriter.PACKAGE + "."
                + written.getFileName().toString().replace(".java", "") + "\"");
    }

    /** The most recently changed {@code drive-*.json} in a folder, if any. */
    static Optional<Path> newestDrive(Path folder) {
        if (!Files.isDirectory(folder)) {
            return Optional.empty();
        }
        try (Stream<Path> files = Files.list(folder)) {
            return files.filter(f -> f.getFileName().toString().startsWith("drive-")
                            && f.getFileName().toString().endsWith(".json"))
                    .max(Comparator.comparing(MakeRecordedTest::modified));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static long modified(Path file) {
        try {
            return Files.getLastModifiedTime(file).toMillis();
        } catch (IOException e) {
            return 0L;
        }
    }
}
