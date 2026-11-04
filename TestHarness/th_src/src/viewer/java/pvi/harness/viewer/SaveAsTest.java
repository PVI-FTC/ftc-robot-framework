package pvi.harness.viewer;

import java.nio.file.Path;
import java.time.LocalDate;

import pvi.harness.recorded.RecordedTestWriter;
import pvi.harness.runtime.RunRecording;

/**
 * The viewer's "Save as test" button, without the window: turns the run on screen into a new
 * test file, the same way {@code gradlew makeTest} does ({@link RecordedTestWriter}).
 *
 * <p>Kept apart from {@link ViewerWindow} so tests can check it with no screen. Saving runs
 * the OpMode once more (for the snapshot checks), so it takes a few seconds; the window calls
 * it off the Swing thread.</p>
 */
public final class SaveAsTest {

    private SaveAsTest() {
    }

    /** The class name the dialog starts with, e.g. "RecordedTeamATeleOpDrive". */
    public static String suggestedClassName(RunRecording recording) {
        return RecordedTestWriter.defaultClassName(recording);
    }

    /**
     * Why a typed class name can't be used, or null if it can.
     * Java class names start with a capital letter here and use only letters, digits and _.
     */
    public static String problemWithClassName(String className) {
        if (className == null || className.isBlank()) {
            return "Type a name for the test class, e.g. RecordedTeamATeleOpDrive.";
        }
        if (!className.trim().matches("[A-Z][A-Za-z0-9_]*")) {
            return "\"" + className.trim() + "\" can't be a test class name: start with a capital letter and use"
                    + " only letters, digits and _ (no spaces).";
        }
        return null;
    }

    /**
     * Writes the test and returns the new file. Never replaces a file: if the name is taken,
     * the test becomes Name2, Name3, and so on.
     *
     * @param recording       the run on screen
     * @param className       the class name the user chose (see {@link #problemWithClassName})
     * @param sourceLabel     where the run came from, for the test's class comment (e.g. the file name)
     * @param folder          where to write it, normally {@link RecordedTestWriter#DEFAULT_FOLDER}
     * @param teamCodeSources where to find the OpMode's package, normally {@link RecordedTestWriter#TEAMCODE_SOURCES}
     */
    public static Path save(RunRecording recording, String className, String sourceLabel,
                            Path folder, Path teamCodeSources) {
        String problem = problemWithClassName(className);
        if (problem != null) {
            throw new IllegalArgumentException(problem);
        }
        String opModeClass = RecordedTestWriter.findOpModeClass(recording.header().opMode(), teamCodeSources);
        return RecordedTestWriter.save(recording, folder, new RecordedTestWriter.Options(
                className.trim(), opModeClass, sourceLabel, LocalDate.now().toString(), false));
    }

    /** The command that runs the new test, e.g. {@code .\gradlew.bat test --tests "pvi.harness.recorded.X"}. */
    public static String runCommand(Path testFile) {
        String className = testFile.getFileName().toString().replace(".java", "");
        return ".\\gradlew.bat test --tests \"" + RecordedTestWriter.PACKAGE + "." + className + "\"";
    }
}
