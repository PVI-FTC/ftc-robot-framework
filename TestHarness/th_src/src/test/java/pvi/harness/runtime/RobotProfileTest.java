package pvi.harness.runtime;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.hardware.DcMotor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import pvi.harness.hardware.RobotConfigs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * D18: a robot profile ({@code robots/<id>/drive.properties}) is a second robot for the OpModes
 * it lists. These use made-up profiles in a temporary folder, so they work with any team's code.
 */
class RobotProfileTest {
    private static final HarnessConfig BASE = HarnessConfig.load();
    /** Drive motors with other names than any team's, and directions, for one profile. */
    private static final String OTHER_DRIVE = """
            drive.frontLeft=a
            drive.frontRight=b
            drive.rearLeft=c
            drive.rearRight=d
            drive.frontLeft.direction=FORWARD
            drive.frontRight.direction=FORWARD
            drive.rearLeft.direction=FORWARD
            drive.rearRight.direction=FORWARD
            """;

    @TempDir
    Path folder;

    private void profile(String id, String text) throws IOException {
        Files.createDirectories(folder.resolve(id));
        Files.writeString(folder.resolve(id).resolve(RobotProfile.FILE_NAME), text);
    }

    private String problems() {
        return assertThrows(IllegalArgumentException.class, () -> RobotProfile.all(BASE, folder)).getMessage();
    }

    /** WHAT: A profile changes only the keys it lists; everything else comes from harness.properties. */
    @Test
    void aProfileChangesOnlyTheKeysItLists() throws IOException {
        profile("other", "forOpModes=Some OpMode\nrobot.name=Other\n" + OTHER_DRIVE);

        RobotProfile other = RobotProfile.all(BASE, folder).get(0);

        assertEquals("other", other.id());
        assertEquals(List.of("Some OpMode"), other.forOpModes());
        assertEquals("Other", other.config().robotName());
        assertEquals("a", other.config().driveMotors().frontLeft());
        assertEquals(BASE.forwardSpeed(), other.config().forwardSpeed());
        assertEquals(BASE.field(), other.config().field());
    }

    /** WHAT: An OpMode is matched by Driver Station name or class name; any other gets no profile. */
    @Test
    void anOpModeIsMatchedByEitherName() throws IOException {
        profile("other", "forOpModes=Some OpMode, SomeClass\n");
        List<RobotProfile> profiles = RobotProfile.all(BASE, folder);

        assertEquals("other", RobotProfile.forOpMode(profiles, "Some OpMode", "NotListed").id());
        assertEquals("other", RobotProfile.forOpMode(profiles, "Not Listed", "SomeClass").id());
        assertNull(RobotProfile.forOpMode(profiles, "Not Listed", "NotListed"));
    }

    /** WHAT: A robot folder with no drive.properties (only a look for Unity) is not a profile. */
    @Test
    void aFolderWithOnlyALookIsNotAProfile() throws IOException {
        Files.createDirectories(folder.resolve("looks"));
        Files.writeString(folder.resolve("looks").resolve("robot.properties"), "model=Robots/looks\n");

        assertEquals(List.of(), RobotProfile.all(BASE, folder));
    }

    /** WHAT: A misspelled key is refused, naming the key and the file. */
    @Test
    void aMisspelledKeyIsRefused() throws IOException {
        profile("other", "forOpModes=Some OpMode\ndrive.frontLeeft=a\n");

        String message = problems();

        assertTrue(message.contains("drive.frontLeeft: not a harness setting"), message);
        assertTrue(message.contains(RobotProfile.FILE_NAME), message);
    }

    /** WHAT: A key for the whole harness (here the field) can't be changed by a profile. */
    @Test
    void aProfileCantChangeTheWholeHarness() throws IOException {
        profile("other", "forOpModes=Some OpMode\nfield=other-field\n");

        assertTrue(problems().contains("field: a robot profile can't change this"), problems());
    }

    /** WHAT: A profile that doesn't say which OpModes use it is refused. */
    @Test
    void aProfileNeedsItsOpModes() throws IOException {
        profile("other", "robot.name=Other\n");

        assertTrue(problems().contains("forOpModes: missing"), problems());
    }

    /** WHAT: Two profiles listing the same OpMode are refused, naming both. */
    @Test
    void anOpModeCantBeInTwoProfiles() throws IOException {
        profile("one", "forOpModes=Some OpMode\n");
        profile("two", "forOpModes=Some OpMode\n");

        String message = problems();

        assertTrue(message.contains("\"Some OpMode\"") && message.contains("one") && message.contains("two"), message);
    }

    /** WHAT: The same OpMode claimed by Driver Station name in one profile and class name in another is refused too. */
    @Test
    void anOpModeCantBeInTwoProfilesByItsTwoNames() throws IOException {
        profile("one", "forOpModes=Some OpMode\n");
        profile("two", "forOpModes=SomeClass\n");
        List<RobotProfile> profiles = RobotProfile.all(BASE, folder);

        String message = assertThrows(IllegalArgumentException.class,
                () -> RobotProfile.forOpMode(profiles, "Some OpMode", "SomeClass")).getMessage();

        assertTrue(message.contains("one") && message.contains("two"), message);
    }

    /**
     * WHAT: A profile that lists its own webcams doesn't inherit the main robot's webcam keys
     * (they would be "not a harness setting" for it), and one that doesn't keeps them.
     */
    @Test
    void aProfilesOwnWebcamListReplacesTheMainRobots() throws IOException {
        profile("none", "forOpModes=A\ndevices.webcams=\n");
        profile("same", "forOpModes=B\n");

        List<RobotProfile> profiles = RobotProfile.all(BASE, folder);

        assertEquals(List.of(), profiles.get(0).config().cameras());
        assertEquals(BASE.cameras(), profiles.get(1).config().cameras());
    }

    /** Full power on motors a, b, c and d. */
    private static final class DriveAbcd extends OpMode {
        @Override
        public void init() {
        }

        @Override
        public void loop() {
            for (String name : new String[] {"a", "b", "c", "d"}) {
                hardwareMap.get(DcMotor.class, name).setPower(1.0);
            }
        }
    }

    /**
     * WHAT: A robot made from a profile really drives: the position estimate reads that robot's
     * motors, directions and drive model, not the ones in harness.properties.
     */
    @Test
    void aProfilesRobotDrivesWithItsOwnWheelsAndModel() throws IOException {
        profile("other", "forOpModes=Some OpMode\nrobot.name=Other\n" + OTHER_DRIVE
                + "drive.forwardSpeedInchesPerSecond=10 | placeholder | test\n");
        HarnessConfig other = RobotProfile.all(BASE, folder).get(0).config();

        OpModeRunner runner = new OpModeRunner(new DriveAbcd(), RobotConfigs.robot(other));
        assertNotNull(runner.positionEstimator(), "motors a, b, c and d are this robot's drive");
        runner.run(0.0, 1.0).assertNoException();

        assertEquals("Other", runner.positionEstimator().model().name());
        double x = runner.positionEstimator().pose().xInches();
        assertTrue(x > 4 && x < 10.5, "about 10 in/s for 1 s after speeding up, went " + x);
    }
}
