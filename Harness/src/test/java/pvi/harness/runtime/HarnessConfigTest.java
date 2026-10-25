package pvi.harness.runtime;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The settings file, {@code harness.properties}: the real file reads cleanly, and a broken one
 * fails early with a message that says what to fix.
 */
class HarnessConfigTest {
    /** A complete, valid file, so each test can break one line of it. */
    private static final String GOOD = String.join("\n",
            "config.version=1",
            "teamCode.sourceDir=../TeamCode/src/main/java",
            "host.dependenciesFile=../build.dependencies.gradle",
            "host.skipLibraries=org.firstinspires.ftc:Blocks, androidx.appcompat:appcompat",
            "drive.frontLeft=fl",
            "drive.frontRight=fr",
            "drive.rearLeft=rl",
            "drive.rearRight=rr",
            "pinpoint=odo",
            "robot.lengthInches=17.5 | measured | tape measure, 2026-10-25",
            "robot.widthInches=18 | placeholder | not measured yet",
            "drive.turnRateDegreesPerSecond=180 | placeholder | not measured yet",
            "drive.responseTimeSeconds=0.1 | placeholder | not measured yet",
            "drive.maxAccelerationInchesPerSecondSquared=200 | placeholder | not measured yet",
            "drive.speedVoltage=12.5 | placeholder | not measured yet",
            "field=biobuzz-2026",
            "viewer.open=false");

    private static String messageFor(String text) {
        return assertThrows(IllegalArgumentException.class, () -> HarnessConfig.fromText(text, "test.properties"))
                .getMessage();
    }

    /**
     * WHAT: The real {@code harness.properties} reads without problems and describes PVI's Team A
     * robot (the same names and placeholders the harness used before the file existed).
     *
     * <p>WHY: Every Gradle run reads this file, so a broken line should show up here first.</p>
     */
    @Test
    void theRealSettingsFileReadsCleanly() {
        HarnessConfig config = HarnessConfig.load(Path.of(HarnessConfig.FILE_NAME));

        assertEquals("../TeamCode/src/main/java", config.teamCodeSourceDir());
        assertEquals(new HarnessConfig.DriveMotorNames("frontLeft", "frontRight", "rearLeft", "rearRight"),
                config.driveMotors());
        assertEquals("pinpoint", config.pinpointName());
        assertEquals(18.0, config.lengthInches().value());
        assertFalse(config.lengthInches().measured(), "the robot size is still a placeholder");
        assertEquals(5, config.skipLibraries().size(), "the five Android-only libraries");
        assertEquals("biobuzz-2026", config.field());
    }

    /**
     * WHAT: A robot fact keeps its number, unit, source and its measured or placeholder mark.
     *
     * <p>WHY: Reports must never show a guess as a measurement.</p>
     */
    @Test
    void aRobotFactKeepsWhetherItWasMeasured() {
        HarnessConfig config = HarnessConfig.fromText(GOOD, "test.properties");

        assertEquals(17.5, config.lengthInches().value());
        assertEquals("in", config.lengthInches().unit());
        assertTrue(config.lengthInches().measured());
        assertEquals("tape measure, 2026-10-25", config.lengthInches().source());
        assertFalse(config.widthInches().measured());
        assertEquals("s", config.responseTime().unit());
    }

    /**
     * WHAT: The skip list is split at commas, with the spaces around them removed.
     *
     * <p>WHY: build.gradle leaves exactly these libraries out of the host's list.</p>
     */
    @Test
    void theSkipListIsSplitAtCommas() {
        HarnessConfig config = HarnessConfig.fromText(GOOD, "test.properties");

        assertEquals(List.of("org.firstinspires.ftc:Blocks", "androidx.appcompat:appcompat"),
                config.skipLibraries());
    }

    /**
     * WHAT: A missing key fails, naming the key.
     *
     * <p>WHY: A half-filled file in another team's repo should say what's missing, not crash later.</p>
     */
    @Test
    void aMissingKeyIsNamed() {
        String message = messageFor(GOOD.replace("pinpoint=odo\n", ""));

        assertTrue(message.contains("pinpoint: missing"), message);
    }

    /**
     * WHAT: A key the harness doesn't know fails, so a typo isn't silently ignored.
     *
     * <p>WHY: A misspelled {@code robot.lenghtInches} would otherwise leave the old size in use.</p>
     */
    @Test
    void anUnknownKeyIsReportedAsAPossibleTypo() {
        String message = messageFor(GOOD + "\nrobot.lenghtInches=17 | measured | tape");

        assertTrue(message.contains("robot.lenghtInches: not a harness setting"), message);
    }

    /**
     * WHAT: A fact that isn't a number, or has neither "measured" nor "placeholder", fails with
     * the key and the bad text.
     *
     * <p>WHY: The person fixing the file should see exactly which value is wrong.</p>
     */
    @Test
    void aBadFactSaysWhatIsWrong() {
        assertTrue(messageFor(GOOD.replace("robot.widthInches=18 |", "robot.widthInches=wide |"))
                .contains("robot.widthInches: \"wide\" is not a number"));
        assertTrue(messageFor(GOOD.replace("| measured |", "| guessed |"))
                .contains("robot.lengthInches: the middle part must be measured or placeholder"));
        assertTrue(messageFor(GOOD.replace("drive.speedVoltage=12.5 | placeholder | not measured yet",
                "drive.speedVoltage=12.5")).contains("drive.speedVoltage: expected \"number |"));
    }

    /**
     * WHAT: Every problem is listed in one message, not just the first.
     *
     * <p>WHY: Fixing a file one error per run is slow.</p>
     */
    @Test
    void everyProblemIsListedAtOnce() {
        String message = messageFor(GOOD.replace("field=biobuzz-2026", "").replace("viewer.open=false", "viewer.open=yes"));

        assertTrue(message.contains("field: missing"), message);
        assertTrue(message.contains("viewer.open: expected true or false"), message);
    }

    /**
     * WHAT: A file written for another version of the harness fails, naming both versions.
     *
     * <p>WHY: Keys change between versions; reading an old file as if it were new would be wrong
     * without saying so.</p>
     */
    @Test
    void aDifferentConfigVersionIsRefused() {
        String message = messageFor(GOOD.replace("config.version=1", "config.version=2"));

        assertTrue(message.contains("reads version " + HarnessConfig.VERSION + ", the file says 2"), message);
    }
}
