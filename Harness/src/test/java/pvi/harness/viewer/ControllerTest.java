package pvi.harness.viewer;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.qualcomm.robotcore.hardware.Gamepad;

import org.junit.jupiter.api.Test;

import pvi.harness.ds.Axis;
import pvi.harness.ds.Button;
import pvi.harness.runtime.RunRecording;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Drive and record, steps D5 and D6: game controllers in live driving, and what happens when a
 * wireless one drops out.
 *
 * <p>No real controller is needed: a fake {@link ControllerPoller} plays the controllers.</p>
 */
class ControllerTest {

    /** A controller with the left stick fully up (forward) and A held. */
    private static final ControllerReading STICK_FORWARD_HOLDING_A =
            new ControllerReading(true, "PS5 Controller", 0, -1.0, 0, 0, 0, 0.5, Set.of(Button.A));

    /** A fake: plays a list of readings, one per poll, and repeats the last one. */
    private static final class FakeControllers implements ControllerPoller {
        private final List<List<ControllerReading>> polls;
        private int next;
        boolean closed;

        FakeControllers(List<List<ControllerReading>> polls) {
            this.polls = polls;
        }

        @Override
        public List<ControllerReading> poll() {
            return polls.get(Math.min(next++, polls.size() - 1));
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    /**
     * WHAT: A controller reading sets every input, ignores tiny stick drift, and releases what
     * isn't held.
     *
     * <p>WHY: A worn stick that rests at 0.03 must not creep the robot, and every button must be
     * set each loop or a released button would stay pressed.</p>
     */
    @Test
    void readingSetsEveryInputWithADeadzone() {
        Gamepad pad = new Gamepad();
        pad.b = true;   // left over from before: must be released
        new ControllerReading(true, "Xbox One Controller", 0.03, -0.8, -0.04, 0.5, 1.0, 0.25,
                Set.of(Button.A, Button.DPAD_UP)).applyTo(pad);

        assertEquals(0.0, Axis.LEFT_STICK_X.read(pad), 1e-6, "inside the dead zone");
        assertEquals(-0.8, Axis.LEFT_STICK_Y.read(pad), 1e-6, "up stays negative");
        assertEquals(0.0, Axis.RIGHT_STICK_X.read(pad), 1e-6);
        assertEquals(0.5, Axis.RIGHT_STICK_Y.read(pad), 1e-6);
        assertEquals(1.0, Axis.LEFT_TRIGGER.read(pad), 1e-6);
        assertEquals(0.25, Axis.RIGHT_TRIGGER.read(pad), 1e-6);
        assertTrue(Button.A.isPressed(pad));
        assertTrue(Button.DPAD_UP.isPressed(pad));
        assertFalse(Button.B.isPressed(pad), "not held, so released");
    }

    /**
     * WHAT: A controller in slot 0 drives gamepad 1; gamepad 2, with no controller, stays on
     * the keyboard.
     *
     * <p>WHY: With one controller, the driver uses it and the operator keys still work.</p>
     */
    @Test
    void controllerReplacesKeyboardOnlyForItsGamepad() {
        HeldKeys keys = new HeldKeys();
        keys.press(KeyEvent.VK_S);   // keyboard says "back" on gamepad 1: the controller must win
        keys.press(KeyEvent.VK_J);   // operator A on the keyboard
        LiveGamepads gamepads = new LiveGamepads(keys,
                new FakeControllers(List.of(List.of(STICK_FORWARD_HOLDING_A))), (n, r, c) -> { });
        Gamepad pad1 = new Gamepad();
        Gamepad pad2 = new Gamepad();
        gamepads.gamepad1().update(pad1, 0, 0);
        gamepads.gamepad2().update(pad2, 0, 0);

        assertEquals(-1.0, Axis.LEFT_STICK_Y.read(pad1), 1e-6);
        assertTrue(Button.A.isPressed(pad1));
        assertTrue(Button.A.isPressed(pad2), "gamepad 2 is still the keyboard");
        assertEquals(List.of("PS5 Controller (D-pad = operator)", "PS5 Controller D-pad + keyboard"),
                gamepads.status());
    }

    /**
     * WHAT: With one controller, its D-pad presses the operator's face buttons (up = Y,
     * down = A, left = X, right = B) and is not sent to the driver's gamepad.
     *
     * <p>WHY: One person with one controller can then run Team A's intake (operator A/B/X/Y)
     * while driving, without the keyboard. A D-pad press must not also reach the driver.</p>
     */
    @Test
    void oneControllerDpadWorksTheOperatorButtons() {
        ControllerReading dpadDownAndLeft = new ControllerReading(true, "PS5 Controller", 0, 0, 0, 0, 0, 0,
                Set.of(Button.DPAD_DOWN, Button.DPAD_LEFT, Button.Y));
        LiveGamepads gamepads = new LiveGamepads(new HeldKeys(),
                new FakeControllers(List.of(List.of(dpadDownAndLeft))), (n, r, c) -> { });
        Gamepad driver = new Gamepad();
        Gamepad operator = new Gamepad();
        gamepads.gamepad1().update(driver, 0, 0);
        gamepads.gamepad2().update(operator, 0, 0);

        assertTrue(Button.A.isPressed(operator), "D-pad down = operator A");
        assertTrue(Button.X.isPressed(operator), "D-pad left = operator X");
        assertFalse(Button.B.isPressed(operator));
        assertFalse(Button.Y.isPressed(operator), "the driver's own Y is not the operator's");
        assertTrue(Button.Y.isPressed(driver), "face buttons stay with the driver");
        assertFalse(Button.DPAD_DOWN.isPressed(driver), "the D-pad went to the operator instead");
        assertFalse(Button.DPAD_LEFT.isPressed(driver));
    }

    /**
     * WHAT: With a second controller, gamepad 2 comes from it and the D-pad is the driver's
     * again.
     *
     * <p>WHY: The D-pad sharing is only for one controller; two people with two controllers
     * get the normal layout.</p>
     */
    @Test
    void secondControllerTakesOverGamepadTwo() {
        ControllerReading driverDpadUp = new ControllerReading(true, "PS5 Controller", 0, 0, 0, 0, 0, 0,
                Set.of(Button.DPAD_UP));
        ControllerReading operatorB = new ControllerReading(true, "Xbox One Controller", 0, 0, 0, 0, 0, 0,
                Set.of(Button.B));
        LiveGamepads gamepads = new LiveGamepads(new HeldKeys(),
                new FakeControllers(List.of(List.of(driverDpadUp, operatorB))), (n, r, c) -> { });
        Gamepad driver = new Gamepad();
        Gamepad operator = new Gamepad();
        gamepads.gamepad1().update(driver, 0, 0);
        gamepads.gamepad2().update(operator, 0, 0);

        assertTrue(Button.DPAD_UP.isPressed(driver), "the driver keeps the D-pad");
        assertTrue(Button.B.isPressed(operator));
        assertFalse(Button.Y.isPressed(operator), "D-pad up is not the operator's Y any more");
        assertEquals(List.of("PS5 Controller", "Xbox One Controller"), gamepads.status());
    }

    /**
     * WHAT: When a wireless controller drops out mid-drive, the robot stops at once, the moment
     * is marked in the recording, and the driver gets a notice.
     *
     * <p>HOW: The controller holds the stick forward for 10 polls, then disconnects. The real
     * Team A TeleOp runs for 1 s.</p>
     *
     * <p>WHY: A dead battery must never leave the robot "holding" full forward.</p>
     */
    @Test
    void disconnectReleasesInputsAndIsMarked() throws InterruptedException {
        List<List<ControllerReading>> polls = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            polls.add(List.of(STICK_FORWARD_HOLDING_A));
        }
        polls.add(List.of(ControllerReading.DISCONNECTED));
        FakeControllers controllers = new FakeControllers(polls);
        LiveDrive drive = new LiveDrive(LiveDrive.options().get(0), new HeldKeys(), controllers, 1.0, false);
        drive.requestPlay();
        drive.start();
        assertTrue(drive.awaitFinished(30));
        assertNull(drive.error());

        RunRecording recording = drive.latest();
        List<RunRecording.Event> events = recording.events();
        assertEquals(2, events.size(), events.toString());
        assertEquals("Gamepad 1: PS5 Controller connected", events.get(0).text());
        assertTrue(events.get(1).text().startsWith("Gamepad 1: controller disconnected"), events.get(1).text());
        assertTrue(drive.notice().contains("disconnected"));
        assertEquals(2, drive.noticeCount());
        assertEquals(List.of("keyboard", "keyboard"), drive.inputStatus());

        // Polls 0-9 are INIT, START and the first 8 loops; from poll 10 on, nothing is held.
        RunRecording.LoopRecord beforeDrop = recording.loops().get(9);
        RunRecording.LoopRecord afterDrop = recording.loops().get(10);
        assertEquals(-1.0, beforeDrop.gamepad1().leftStickY(), 1e-6);
        assertEquals(0.0, afterDrop.gamepad1().leftStickY(), 1e-6, "released the moment it dropped");
        assertEquals(List.of(), afterDrop.gamepad1().buttons());
        assertEquals(afterDrop.sinceInitSeconds(), events.get(1).sinceInitSeconds(), 1e-9,
                "the event is stamped with the loop it happened in");
        RunRecording.LoopRecord last = recording.loops().get(recording.loops().size() - 1);
        assertTrue(last.motors().stream().allMatch(m -> m.power() == 0.0), "the robot has stopped: " + last.motors());
        assertTrue(controllers.closed, "the controller library is let go when the drive ends");
    }
}
