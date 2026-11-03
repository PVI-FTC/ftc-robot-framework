package pvi.harness.hardware;

import java.nio.file.Files;
import java.nio.file.Path;

import com.qualcomm.robotcore.hardware.Blinker;

import org.junit.jupiter.api.Test;

import pvi.harness.runtime.HarnessConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** D18: a hub's status light, found as a {@code Blinker}, as robot code does. */
class FakeBlinkerTest {

    /** WHAT: A name in devices.blinkers becomes a Blinker robot code can find by that name. */
    @Test
    void aListedBlinkerIsFoundByRobotCode() throws Exception {
        String text = Files.readString(Path.of(HarnessConfig.FILE_NAME)).replace("\r\n", "\n")
                .replaceAll("(?m)^devices\\.blinkers=.*$", "devices.blinkers=Control Hub");
        FakeHardwareMap robot = RobotConfigs.robot(HarnessConfig.fromText(text, "test"));

        Blinker blinker = robot.get(Blinker.class, "Control Hub");

        assertTrue(blinker instanceof FakeBlinker);
    }

    /** WHAT: The light remembers what it was told, and a pushed pattern comes back when popped. */
    @Test
    void itRemembersItsPattern() {
        FakeBlinker blinker = new FakeBlinker("Control Hub");
        blinker.setConstant(0x00FF00);
        assertEquals(0x00FF00, blinker.getPattern().iterator().next().getColor());

        blinker.pushPattern(java.util.List.of());
        assertTrue(blinker.getPattern().isEmpty());
        assertTrue(blinker.patternStackNotEmpty());

        assertTrue(blinker.popPattern());
        assertEquals(0x00FF00, blinker.getPattern().iterator().next().getColor());
        assertFalse(blinker.popPattern(), "nothing left to pop");
    }
}
