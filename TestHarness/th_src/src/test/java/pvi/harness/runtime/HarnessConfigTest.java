package pvi.harness.runtime;

import java.nio.file.Path;
import java.util.List;

import com.qualcomm.robotcore.hardware.DcMotorSimple.Direction;

import org.junit.jupiter.api.Assumptions;
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
            "config.version=" + HarnessConfig.VERSION,
            "teamCode.sourceDir=../../TeamCode/src/main/java",
            "teamCode.opModePackages=org.example.robot, org.example.shared",
            "host.dependenciesFile=../../build.dependencies.gradle",
            "host.skipLibraries=org.firstinspires.ftc:Blocks, androidx.appcompat:appcompat",
            "robot.name=Test Bot",
            "drive.frontLeft=fl",
            "drive.frontRight=fr",
            "drive.rearLeft=rl",
            "drive.rearRight=rr",
            "pinpoint=odo",
            "hardware.otherMotors=",
            "hardware.servos=",
            "devices.imu=imu",
            "devices.crServos=",
            "devices.distanceSensors=frontDistance, rearDistance",
            "devices.touchSensors=",
            "devices.colorSensors=",
            "devices.analogInputs=",
            "devices.otos=",
            "devices.limelights=",
            "devices.webcams=",
            "devices.deadWheels=leftOdo:parallel:7.0, strafeOdo:perpendicular:-3.5",
            "drive.frontLeft.direction=REVERSE",
            "drive.frontRight.direction=FORWARD",
            "drive.rearLeft.direction=REVERSE",
            "drive.rearRight.direction=FORWARD",
            "drive.forwardSpeedInchesPerSecond=60 | measured | velocity tuner",
            "drive.strafeSpeedInchesPerSecond=50 | measured | velocity tuner",
            "drive.forwardCoastInchesPerSecondSquared=40 | measured | zero power tuner",
            "drive.lateralCoastInchesPerSecondSquared=45 | measured | zero power tuner",
            "pvi.extras=false",
            "robot.lengthInches=17.5 | measured | tape measure, 2026-10-25",
            "robot.widthInches=18 | placeholder | not measured yet",
            "drive.turnRateDegreesPerSecond=180 | placeholder | not measured yet",
            "drive.responseTimeSeconds=0.1 | placeholder | not measured yet",
            "drive.maxAccelerationInchesPerSecondSquared=200 | placeholder | not measured yet",
            "drive.speedVoltage=12.5 | placeholder | not measured yet",
            "field=biobuzz-2026",
            "field.robotObstacles=true",
            "viewer.open=false",
            "viewer.defaultTeleOp=",
            "smokeTest.skipOpModes=KimCode TeleopV1, Old Test OpMode",
            "smokeTest.notDrivingTeleOps=Camera Tool");

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

        // Reading cleanly is the check in any repo; the values below are PVI's own robot.
        Assumptions.assumeTrue(config.pviExtras(), "PVI's values; another team's file has its own");
        assertEquals("../../TeamCode/src/main/java", config.teamCodeSourceDir());
        assertEquals(new HarnessConfig.DriveMotorNames("frontLeft", "frontRight", "rearLeft", "rearRight"),
                config.driveMotors());
        assertEquals("pinpoint", config.pinpointName());
        assertEquals("Team A", config.robotName());
        assertEquals(List.of("intake"), config.otherMotors());
        assertEquals(new HarnessConfig.DriveMotorDirections(Direction.REVERSE, Direction.FORWARD,
                Direction.REVERSE, Direction.FORWARD), config.driveDirections(), "left REVERSE, right FORWARD");
        assertTrue(config.forwardSpeed().measured(), "Team A's speeds come from Pedro's tuners");
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
     * WHAT: Each drive motor's forward direction is read as FORWARD or REVERSE, and anything else
     * fails with the key.
     *
     * <p>WHY: A wrong direction makes the simulated robot drive the wrong way, so a typo must stop
     * the run, not quietly count as FORWARD.</p>
     */
    @Test
    void aDriveDirectionMustBeForwardOrReverse() {
        HarnessConfig config = HarnessConfig.fromText(GOOD, "test.properties");
        assertEquals(new HarnessConfig.DriveMotorDirections(Direction.REVERSE, Direction.FORWARD,
                Direction.REVERSE, Direction.FORWARD), config.driveDirections());

        String message = messageFor(GOOD.replace("drive.rearLeft.direction=REVERSE", "drive.rearLeft.direction=backward"));
        assertTrue(message.contains("drive.rearLeft.direction: expected FORWARD or REVERSE, got \"backward\""), message);
    }

    /**
     * WHAT: A list such as {@code hardware.otherMotors} may be empty, but the key must be there.
     *
     * <p>WHY: An empty list is a real answer ("no other motors"); a missing key is usually a
     * forgotten line.</p>
     */
    @Test
    void aListMayBeEmptyButNotMissing() {
        assertEquals(List.of(), HarnessConfig.fromText(GOOD, "test.properties").otherMotors());
        assertEquals(List.of("KimCode TeleopV1", "Old Test OpMode"),
                HarnessConfig.fromText(GOOD, "test.properties").smokeTestSkipOpModes());
        assertEquals(List.of("Camera Tool"), HarnessConfig.fromText(GOOD, "test.properties").smokeTestNotDrivingTeleOps());
        assertEquals(List.of("org.example.robot", "org.example.shared"),
                HarnessConfig.fromText(GOOD, "test.properties").opModePackages());

        String message = messageFor(GOOD.replace("hardware.otherMotors=\n", ""));
        assertTrue(message.contains("hardware.otherMotors: missing"), message);
    }

    /**
     * WHAT: Dead-wheel pods are read as name:parallel|perpendicular:inches; a wrong kind or a
     * number that isn't one fails with the key and the item.
     */
    @Test
    void deadWheelsAreReadWithTheirKindAndOffset() {
        assertEquals(List.of(new HarnessConfig.DeadWheel("leftOdo", true, 7.0),
                        new HarnessConfig.DeadWheel("strafeOdo", false, -3.5)),
                HarnessConfig.fromText(GOOD, "test.properties").deadWheels());

        String sideways = messageFor(GOOD.replace("strafeOdo:perpendicular:-3.5", "strafeOdo:sideways:-3.5"));
        assertTrue(sideways.contains("devices.deadWheels: expected name:parallel:inches"), sideways);
        String notNumber = messageFor(GOOD.replace("leftOdo:parallel:7.0", "leftOdo:parallel:seven"));
        assertTrue(notNumber.contains("\"seven\" in \"leftOdo:parallel:seven\" is not a number"), notNumber);
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
        int other = HarnessConfig.VERSION + 1;
        String message = messageFor(GOOD.replace("config.version=" + HarnessConfig.VERSION, "config.version=" + other));

        assertTrue(message.contains("reads version " + HarnessConfig.VERSION + ", the file says " + other), message);
    }
}
