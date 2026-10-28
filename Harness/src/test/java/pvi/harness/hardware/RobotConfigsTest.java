package pvi.harness.hardware;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import pvi.harness.runtime.HarnessConfig;
import pvi.harness.runtime.MatchPeriod;
import pvi.harness.runtime.OpModeCatalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Which fake robot an OpMode found by {@code OpModeCatalog} runs on (manager decision D7): the one
 * in {@code harness.properties}, with PVI's Team B and Team C OpModes keeping their own names
 * when the PVI extras are on.
 */
class RobotConfigsTest {

    private static OpModeCatalog.Entry opModeInGroup(String group) {
        return new OpModeCatalog.Entry("org.example.SomeTeleOp", "Some TeleOp", group, MatchPeriod.TELEOP, "");
    }

    /** The real settings file with {@code pvi.extras} switched off, as in another team's repo. */
    private static HarnessConfig withoutPviExtras() throws IOException {
        String text = Files.readString(Path.of(HarnessConfig.FILE_NAME));
        return HarnessConfig.fromText(text.replace("pvi.extras=true", "pvi.extras=false"), "no-extras");
    }

    /**
     * WHAT: With the PVI extras on, a "Team B" or "Team C" OpMode gets the teamB / teamC robot,
     * and anything else (e.g. "Team A", "Testing") gets the settings file's robot.
     *
     * <p>WHY: PVI's recordings and the bridge say which team's robot a run used.</p>
     */
    @Test
    void pviTeamBAndCOpModesKeepTheirRobotNames() {
        HarnessConfig config = HarnessConfig.load();
        Assumptions.assumeTrue(config.pviExtras(), "PVI only: the Team B / Team C rule needs pvi.extras=true");

        assertEquals("teamB", RobotConfigs.forOpMode(opModeInGroup("Team B"), config).configName());
        assertEquals("teamC", RobotConfigs.forOpMode(opModeInGroup("Team C"), config).configName());
        assertEquals("teamA", RobotConfigs.forOpMode(opModeInGroup("Team A"), config).configName());
        assertEquals("teamA", RobotConfigs.forOpMode(opModeInGroup("Testing"), config).configName());
    }

    /**
     * WHAT: The {@code devices.*} lists in the settings file add those devices to the fake robot
     * (RP7), and PVI's file adds none.
     *
     * <p>WHY: Another team's robot code looks up its sensors by name; without them it can't start.</p>
     */
    @Test
    void theDeviceListsAddTheirDevices() throws IOException {
        String text = Files.readString(Path.of(HarnessConfig.FILE_NAME))
                .replace("devices.imu=\n", "devices.imu=imu\n")
                .replace("hardware.servos=\n", "hardware.servos=claw, wrist\n")
                .replace("devices.distanceSensors=\n", "devices.distanceSensors=frontDistance, rearDistance\n")
                .replace("devices.touchSensors=\n", "devices.touchSensors=button\n")
                .replace("devices.colorSensors=\n", "devices.colorSensors=intakeColor\n")
                .replace("devices.crServos=\n", "devices.crServos=spinner\n")
                .replace("devices.analogInputs=\n", "devices.analogInputs=pot\n")
                .replace("devices.otos=\n", "devices.otos=otos\n")
                .replace("devices.limelights=\n", "devices.limelights=limelight\n")
                .replaceAll("(?m)^devices\\.webcams=.*$", "devices.webcams=camera")
                .replace("devices.deadWheels=\n", "devices.deadWheels=leftOdo:parallel:7.0\n");
        FakeHardwareMap robot = RobotConfigs.robot(HarnessConfig.fromText(text.replace("\r\n", "\n"), "devices"));
        java.util.Set<String> names = robot.deviceNames();
        for (String name : new String[] {"imu", "frontDistance", "rearDistance", "button", "intakeColor", "spinner",
                "pot", "otos", "limelight", "camera", "leftOdo", "claw", "wrist"}) {
            assertTrue(names.contains(name), name + " should be on the robot: " + names);
        }

        java.util.Set<String> pvi = RobotConfigs.teamA().deviceNames();
        assertTrue(java.util.Collections.disjoint(pvi, java.util.Set.of("imu", "frontDistance")), "PVI adds none: " + pvi);
    }

    /**
     * WHAT: In another repo ({@code pvi.extras=false}) every OpMode gets the settings file's robot,
     * whatever its group is called.
     *
     * <p>WHY: Another team's "Team B" group means nothing to the harness.</p>
     */
    @Test
    void withoutPviExtrasEveryOpModeGetsTheConfigRobot() throws IOException {
        HarnessConfig config = withoutPviExtras();

        FakeHardwareMap robot = RobotConfigs.forOpMode(opModeInGroup("Team B"), config);
        assertEquals("teamA", robot.configName());
        assertNotNull(robot.pinpoint(config.pinpointName()), "it is the full robot, Pinpoint included");
    }
}
