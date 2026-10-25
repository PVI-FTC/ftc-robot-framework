package pvi.harness.bridge;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import pvi.harness.ds.Button;
import pvi.harness.runtime.JsonReader;
import pvi.harness.viewer.ControllerPoller;
import pvi.harness.viewer.ControllerReading;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Stage U2 with {@code "input": "controllers"}: the harness reads the game controllers (the
 * user's choice, 2026-10-24) and Unity only displays. A fake controller stands in for a real
 * one, so no hardware is needed.
 */
class BridgeControllersTest {
    /** A connected controller holding the left stick fully forward (up is negative). */
    private static final ControllerReading FORWARD =
            new ControllerReading(true, "Test Pad", 0, -1, 0, 0, 0, 0, Set.of());

    /** A fake controller: plays back one reading per poll (one poll per loop); then nothing connected. */
    private static final class FakeControllers implements ControllerPoller {
        private final List<List<ControllerReading>> readings = new ArrayList<>();
        private String problem;
        private boolean closed;
        private int polls;

        FakeControllers connectedFor(int loops, ControllerReading reading) {
            for (int i = 0; i < loops; i++) {
                readings.add(List.of(reading));
            }
            return this;
        }

        @Override
        public List<ControllerReading> poll() {
            return polls < readings.size() ? readings.get(polls++) : List.of();
        }

        @Override
        public String problem() {
            return problem;
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    private FakeControllers fake = new FakeControllers();
    private final BridgeSession session = new BridgeSession(() -> fake);

    @SuppressWarnings("unchecked")
    private Map<String, Object> send(String line) {
        return (Map<String, Object>) JsonReader.parse(session.handle(line));
    }

    /** hello, load with controllers, init and play. */
    private void startWithControllers() {
        send("{\"type\": \"hello\", \"version\": 4}");
        Map<String, Object> loaded = send("{\"type\": \"load\", \"opMode\": \"teamA-teleop\", \"input\": \"controllers\"}");
        assertEquals("controllers", loaded.get("input"));
        send("{\"type\": \"init\"}");
        send("{\"type\": \"play\"}");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> loop(Map<String, Object> frame) {
        return (Map<String, Object>) frame.get("loop");
    }

    @SuppressWarnings("unchecked")
    private static double frontLeftPower(Map<String, Object> frame) {
        for (Object item : (List<Object>) loop(frame).get("motors")) {
            Map<String, Object> motor = (Map<String, Object>) item;
            if ("frontLeft".equals(motor.get("name"))) {
                return (Double) motor.get("power");
            }
        }
        throw new AssertionError("no frontLeft");
    }

    /**
     * WHAT: With "input": "controllers", the controller drives the real TeleOp, and each frame
     * says which controller drives each gamepad and shows what the OpMode saw.
     *
     * <p>HOW: The fake controller holds full forward for the whole run. After play and 10 steps,
     * the front-left wheel is at full power, the frame says the pad drives gamepad 1 (its D-pad
     * standing in for gamepad 2's buttons), and the
     * recorded gamepad 1 shows the stick forward.</p>
     *
     * <p>WHY: This is the user's chosen setup: the harness reads the controller, Unity draws.</p>
     */
    @Test
    @SuppressWarnings("unchecked")
    void controllerDrivesTheTeleOpAndFramesSayWhichController() {
        fake.connectedFor(1000, FORWARD);
        startWithControllers();

        Map<String, Object> frame = null;
        for (int i = 0; i < 10; i++) {
            frame = send("{\"type\": \"step\"}");
        }

        assertEquals(1.0, frontLeftPower(frame), 1e-6);
        // One controller: its D-pad also works the operator's face buttons (as in the Swing live drive).
        assertEquals(List.of("Test Pad (D-pad = operator)", "Test Pad D-pad"), frame.get("controllers"));
        Map<String, Object> gamepad1 = (Map<String, Object>) loop(frame).get("gamepad1");
        assertEquals(-1.0, gamepad1.get("leftStickY"));
    }

    /**
     * WHAT: A controller that disconnects mid-drive releases every input in the same loop, and the
     * moment is marked as an event.
     *
     * <p>HOW: The fake controller holds full forward for the first few loops, then disappears.
     * The first frame without it has the drive at 0, says "none", and carries the event
     * "Gamepad 1: controller disconnected; inputs released".</p>
     *
     * <p>WHY: A wireless controller can drop out (battery, sleep, range). The robot must never keep
     * "holding" the last stick position.</p>
     */
    @Test
    @SuppressWarnings("unchecked")
    void disconnectReleasesEverythingAndIsMarked() {
        // Polls: INIT, START, then 3 loops still connected.
        fake.connectedFor(5, FORWARD);
        startWithControllers();
        for (int i = 0; i < 3; i++) {
            assertEquals(1.0, frontLeftPower(send("{\"type\": \"step\"}")), 1e-6, "still connected");
        }

        Map<String, Object> dropped = send("{\"type\": \"step\"}");

        assertEquals(0.0, frontLeftPower(dropped), 1e-9, "released in the same loop");
        assertEquals(List.of("none", "none"), dropped.get("controllers"));
        List<Object> events = (List<Object>) dropped.get("events");
        assertTrue(events.stream().anyMatch(e -> ((Map<String, Object>) e).get("text").toString()
                .equals("Gamepad 1: controller disconnected; inputs released")), events.toString());
    }

    /**
     * WHAT: When the harness reads the controllers, a step that carries gamepads is refused.
     * WHY: Two sources fighting over one gamepad would make the drive unpredictable.
     */
    @Test
    void stepsMustNotCarryGamepadsWhenControllersAreRead() {
        fake.connectedFor(100, FORWARD);
        startWithControllers();

        Map<String, Object> reply = send("{\"type\": \"step\", \"gamepad1\": {\"buttons\": []}}");

        assertEquals("error", reply.get("type"));
        assertTrue(reply.get("message").toString().contains("reads the game controllers"), reply.toString());
    }

    /**
     * WHAT: A controller problem (e.g. the controller library couldn't start) is shown in each
     * frame, and an unknown "input" choice is refused.
     */
    @Test
    void controllerProblemIsReportedAndBadInputRefused() {
        fake.problem = "Controllers are off: SDL failed to start";
        startWithControllers();

        Map<String, Object> frame = send("{\"type\": \"step\"}");
        assertEquals("Controllers are off: SDL failed to start", frame.get("controllerProblem"));

        Map<String, Object> bad = send("{\"type\": \"load\", \"opMode\": \"teamA-teleop\", \"input\": \"mind\"}");
        assertEquals("error", bad.get("type"));
        assertTrue(bad.get("message").toString().contains("\"input\" must be"), bad.toString());
    }

    /**
     * WHAT: The controllers are let go when the client says bye, or when a new run is loaded.
     * WHY: SDL holds the controllers; a later run (or the Swing viewer) must be able to use them.
     */
    @Test
    void controllersAreReleasedOnByeAndOnReload() {
        startWithControllers();
        FakeControllers first = fake;
        fake = new FakeControllers();

        send("{\"type\": \"load\", \"opMode\": \"teamA-teleop\", \"input\": \"controllers\"}");
        assertTrue(first.closed, "the first run's controllers are released when a new run loads");

        send("{\"type\": \"bye\"}");
        assertTrue(fake.closed, "released on bye");
    }

    /**
     * WHAT: With "input": "messages" (the default), the frame's "controllers" is null.
     */
    @Test
    void messagesInputHasNoControllers() {
        send("{\"type\": \"hello\", \"version\": 4}");
        Map<String, Object> loaded = send("{\"type\": \"load\", \"opMode\": \"teamA-teleop\"}");
        assertEquals("messages", loaded.get("input"));

        Map<String, Object> frame = send("{\"type\": \"init\"}");

        assertEquals(null, frame.get("controllers"));
        assertTrue(frame.containsKey("controllers"));
    }
}
