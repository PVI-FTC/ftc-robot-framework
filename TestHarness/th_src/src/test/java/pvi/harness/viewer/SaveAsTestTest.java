package pvi.harness.viewer;

import org.firstinspires.ftc.teamcode.opmodes.teleop.TeamATeleOp;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

import pvi.harness.ds.Axis;
import pvi.harness.ds.Button;
import pvi.harness.ds.GamepadScript;
import pvi.harness.hardware.RobotConfigs;
import pvi.harness.recorded.RecordedTestWriter;
import pvi.harness.runtime.OpModeRunner;
import pvi.harness.runtime.RunRecorder;
import pvi.harness.runtime.RunRecording;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * D4's viewer button, "Save as test" ({@link SaveAsTest}), checked without a window: the run on
 * screen becomes a new test file, as with {@code gradlew makeTest}.
 */
class SaveAsTestTest {
    /** Where these tests write; inside build/, so nothing is left in src/. */
    private static final Path WORK = Path.of("build", "save-as-test-work");

    /** A short Team A TeleOp drive: forward for a second, tap A. */
    private static RunRecording shortDrive() {
        GamepadScript driver = new GamepadScript()
                .at(0.0).stick(Axis.LEFT_STICK_Y, -1.0)
                .at(1.0).stick(Axis.LEFT_STICK_Y, 0.0);
        GamepadScript operator = new GamepadScript().at(0.5).tap(Button.A);
        OpModeRunner runner = new OpModeRunner(new TeamATeleOp(), RobotConfigs.teamA())
                .gamepad1(driver).gamepad2(operator);
        RunRecorder recorder = RunRecorder.attachTo(runner);
        runner.init().start();
        runner.loopUntil(1.5);
        runner.stop();
        return recorder.recording();
    }

    /** An empty folder for one test. */
    private static Path emptyFolder(String name) throws IOException {
        Path folder = WORK.resolve(name);
        if (Files.exists(folder)) {
            try (Stream<Path> files = Files.walk(folder)) {
                for (Path file : files.sorted(Comparator.reverseOrder()).toList()) {
                    Files.delete(file);
                }
            }
        }
        return Files.createDirectories(folder);
    }

    /**
     * WHAT: Saving writes a test class with the chosen name, the OpMode and the recorded
     * gamepad inputs.
     */
    @Test
    void savingWritesATestWithTheChosenNameAndTheRecordedInputs() throws IOException {
        Path folder = emptyFolder("chosenName");

        Path file = SaveAsTest.save(shortDrive(), "MyIntakeDrive", "practice drive (not saved)",
                folder, RecordedTestWriter.TEAMCODE_SOURCES);

        assertEquals(folder.resolve("MyIntakeDrive.java"), file);
        String source = Files.readString(file);
        assertTrue(source.contains("class MyIntakeDrive"), source);
        assertTrue(source.contains("new TeamATeleOp()"), "should run the same OpMode:\n" + source);
        assertTrue(source.contains("Axis.LEFT_STICK_Y"), "should contain the recorded stick:\n" + source);
        assertTrue(source.contains("practice drive (not saved)"), "should say where the run came from:\n" + source);
    }

    /**
     * WHAT: Saving twice with the same name never replaces the first test; the second becomes
     * Name2.
     */
    @Test
    void savingTwiceNeverReplacesTheFirstTest() throws IOException {
        Path folder = emptyFolder("twice");
        RunRecording drive = shortDrive();

        Path first = SaveAsTest.save(drive, "TwiceDrive", "test", folder, RecordedTestWriter.TEAMCODE_SOURCES);
        String firstSource = Files.readString(first);
        Path second = SaveAsTest.save(drive, "TwiceDrive", "test", folder, RecordedTestWriter.TEAMCODE_SOURCES);

        assertEquals(folder.resolve("TwiceDrive2.java"), second);
        assertEquals(firstSource, Files.readString(first), "the first test must be unchanged");
    }

    /**
     * WHAT: The dialog suggests the same name as makeTest, and refuses names Java can't use,
     * saying why.
     */
    @Test
    void theDialogSuggestsMakeTestsNameAndRefusesBadNames() {
        RunRecording drive = shortDrive();

        assertEquals("RecordedTeamATeleOpDrive", SaveAsTest.suggestedClassName(drive));
        assertNull(SaveAsTest.problemWithClassName("Good_Name2"));
        assertNotNull(SaveAsTest.problemWithClassName(""));
        assertNotNull(SaveAsTest.problemWithClassName("lowercase"));
        assertTrue(SaveAsTest.problemWithClassName("Has Space").contains("no spaces"));
        assertThrows(IllegalArgumentException.class, () -> SaveAsTest.save(drive, "2Bad", "test",
                WORK.resolve("never"), RecordedTestWriter.TEAMCODE_SOURCES));
    }

    /** WHAT: After saving, the window shows the command that runs the new test. */
    @Test
    void theRunCommandNamesTheNewTestClass() {
        assertEquals(".\\gradlew.bat test --tests \"pvi.harness.recorded.TwiceDrive2\"",
                SaveAsTest.runCommand(Path.of("src", "test", "java", "pvi", "harness", "recorded", "TwiceDrive2.java")));
    }
}
