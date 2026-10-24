package pvi.harness.bridge;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import pvi.harness.runtime.JsonReader;
import pvi.harness.runtime.RunRecording;
import pvi.harness.runtime.RunRecordingJson;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Stage U2, harness side: the live Unity connection's messages, driven line by line exactly as
 * Unity would send them (no socket, no Unity needed). The format is in the README, "Live
 * connection to Unity".
 */
class BridgeSessionTest {
    /** Full forward on the left stick (up is negative), as Unity sends it. */
    private static final String FORWARD = "{\"buttons\": [], \"leftStickX\": 0, \"leftStickY\": -1, \"rightStickX\": 0,"
            + " \"rightStickY\": 0, \"leftTrigger\": 0, \"rightTrigger\": 0}";
    /** Nothing pressed. */
    private static final String RELEASED = "{\"buttons\": [], \"leftStickX\": 0, \"leftStickY\": 0, \"rightStickX\": 0,"
            + " \"rightStickY\": 0, \"leftTrigger\": 0, \"rightTrigger\": 0}";

    private final BridgeSession session = new BridgeSession();

    /** Sends one message and returns the parsed reply. */
    @SuppressWarnings("unchecked")
    private Map<String, Object> send(String line) {
        return (Map<String, Object>) JsonReader.parse(session.handle(line));
    }

    /** hello + load, ready for "init". */
    private void connectAndLoad(String opMode) {
        assertEquals("welcome", send("{\"type\": \"hello\", \"version\": 3}").get("type"));
        assertEquals("loaded", send("{\"type\": \"load\", \"opMode\": \"" + opMode + "\"}").get("type"));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> loop(Map<String, Object> frame) {
        return (Map<String, Object>) frame.get("loop");
    }

    @SuppressWarnings("unchecked")
    private static double xInches(Map<String, Object> frame) {
        return (Double) ((Map<String, Object>) loop(frame).get("pose")).get("xInches");
    }

    @SuppressWarnings("unchecked")
    private static double power(Map<String, Object> frame, String motor) {
        for (Object item : (List<Object>) loop(frame).get("motors")) {
            Map<String, Object> m = (Map<String, Object>) item;
            if (motor.equals(m.get("name"))) {
                return (Double) m.get("power");
            }
        }
        throw new AssertionError("no motor " + motor);
    }

    // ---- Handshake ------------------------------------------------------------------------------

    /**
     * WHAT: "hello" with version 3 gets "welcome" listing the OpModes Unity can run.
     * WHY: Unity builds its menu from this list, and must know both sides speak the same version.
     */
    @Test
    @SuppressWarnings("unchecked")
    void helloListsTheOpModes() {
        Map<String, Object> welcome = send("{\"type\": \"hello\", \"version\": 3, \"client\": \"test\"}");

        assertEquals("welcome", welcome.get("type"));
        assertEquals(3.0, welcome.get("version"));
        assertEquals(0.02, welcome.get("loopPeriodSeconds"));
        List<Object> opModes = (List<Object>) welcome.get("opModes");
        Map<String, Object> first = (Map<String, Object>) opModes.get(0);
        assertEquals("teamA-teleop", first.get("id"));
        assertEquals("TELEOP", first.get("period"));
        assertEquals(BridgeOpModes.all().size(), opModes.size());
    }

    /**
     * WHAT: Another protocol version is refused with both versions named, and the connection closes.
     * WHY: Talking past each other in different formats would give wrong robot positions silently.
     */
    @Test
    void wrongVersionIsRefusedAndCloses() {
        Map<String, Object> reply = send("{\"type\": \"hello\", \"version\": 99}");

        assertEquals("error", reply.get("type"));
        String message = (String) reply.get("message");
        assertTrue(message.contains("version 3") && message.contains("version 99"), message);
        assertTrue(session.isClosing());
    }

    // ---- A run ----------------------------------------------------------------------------------

    /**
     * WHAT: A whole TeleOp run driven from "Unity": the robot drives forward while the gamepad
     * says full forward, and each frame carries the stage and pose.
     *
     * <p>HOW: load Team A TeleOp, init, 10 init_loop steps, play, then 50 steps (1 s) with the left
     * stick full forward, then stop. The robot ends well forward (over 30 in; Team A tops out at
     * 62.6 in/s), and the stop frame says finished.</p>
     *
     * <p>WHY: This is the whole point of Stage U2: Unity's gamepad drives the real TeamCode.</p>
     */
    @Test
    void unityGamepadDrivesTheRealTeleOp() {
        connectAndLoad("teamA-teleop");

        Map<String, Object> init = send("{\"type\": \"init\"}");
        assertEquals("INIT", loop(init).get("stage"));
        for (int i = 0; i < 10; i++) {
            assertEquals("INIT_LOOP", loop(send("{\"type\": \"step\"}")).get("stage"));
        }
        Map<String, Object> start = send("{\"type\": \"play\"}");
        assertEquals("START", loop(start).get("stage"));
        Map<String, Object> frame = null;
        for (int i = 0; i < 50; i++) {
            frame = send("{\"type\": \"step\", \"gamepad1\": " + FORWARD + "}");
        }

        assertEquals("LOOP", loop(frame).get("stage"));
        assertEquals(1.0, (Double) loop(frame).get("matchTimeSeconds"), 1e-9);
        assertEquals(1.0, power(frame, "frontLeft"), 1e-6);
        // How far exactly depends on the physics model (top speed 62.6 in/s); 1 s of full
        // forward must clearly move the robot forward.
        assertTrue(xInches(frame) > 30, "drove forward: " + xInches(frame));
        assertEquals(Boolean.FALSE, frame.get("finished"));
        Map<String, Object> stopped = send("{\"type\": \"stop\"}");
        assertEquals("STOP", loop(stopped).get("stage"));
        assertEquals(Boolean.TRUE, stopped.get("finished"));
        assertEquals(0.0, power(stopped, "frontLeft"), 1e-9, "TeamCode's stop() zeroes the drive");
    }

    /**
     * WHAT: A step without a gamepad keeps the last one (still held); a released gamepad stops the robot.
     * WHY: Unity only needs to send the gamepad when it changes, like a real controller staying put.
     */
    @Test
    void gamepadStaysHeldUntilUnitySendsANewOne() {
        connectAndLoad("teamA-teleop");
        send("{\"type\": \"init\"}");
        send("{\"type\": \"play\"}");

        send("{\"type\": \"step\", \"gamepad1\": " + FORWARD + "}");
        Map<String, Object> held = send("{\"type\": \"step\"}");
        assertEquals(1.0, power(held, "frontLeft"), 1e-6, "still full forward");

        Map<String, Object> released = send("{\"type\": \"step\", \"gamepad1\": " + RELEASED + "}");
        assertEquals(0.0, power(released, "frontLeft"), 1e-9);
    }

    /**
     * WHAT: The run stops by itself at the end of the match, and stepping after that is refused.
     * HOW: Team A's autonomous, stepping until the harness says finished. PLAY takes the first
     * 20 ms, so 1499 steps reach 30 s (the same count as a normal 30 s test run); that frame is
     * STOP at 30.00 s.
     */
    @Test
    void autonomousStopsByItselfAtTheEndOfTheMatch() {
        connectAndLoad("teamA-autonomous");
        send("{\"type\": \"init\"}");
        send("{\"type\": \"play\"}");

        Map<String, Object> frame = null;
        int steps = 0;
        while (steps < 2000 && (frame == null || frame.get("finished") != Boolean.TRUE)) {
            frame = send("{\"type\": \"step\"}");
            steps++;
        }

        assertEquals(1499, steps);
        assertEquals("STOP", loop(frame).get("stage"));
        assertEquals(30.0, (Double) loop(frame).get("matchTimeSeconds"), 1e-9);
        assertEquals(Boolean.TRUE, frame.get("finished"));
        assertEquals(9.4, xInches(frame), 0.5, "the autonomous drives about 9.4 in");
        assertEquals("error", send("{\"type\": \"step\"}").get("type"));
    }

    /**
     * WHAT: Each rumble arrives in exactly one frame.
     * HOW: The rumble demo buzzes the driver once at PLAY; over PLAY and the next 20 steps, gamepad 1
     * rumbles total exactly one.
     * WHY: Unity buzzes the controller (or shows it) once per rumble, not every frame.
     */
    @Test
    @SuppressWarnings("unchecked")
    void eachRumbleArrivesOnce() {
        connectAndLoad("rumble-demo");
        send("{\"type\": \"init\"}");

        List<Map<String, Object>> frames = new ArrayList<>();
        frames.add(send("{\"type\": \"play\"}"));
        for (int i = 0; i < 20; i++) {
            frames.add(send("{\"type\": \"step\"}"));
        }

        int rumbles = 0;
        for (Map<String, Object> frame : frames) {
            rumbles += ((List<Object>) frame.get("gamepad1Rumbles")).size();
        }
        assertEquals(1, rumbles);
    }

    /**
     * WHAT: Frames use the run recording's loop layout, so Unity reads them with its recording classes.
     */
    @Test
    void framesUseTheRecordingLoopLayout() {
        connectAndLoad("teamA-teleop");

        Map<String, Object> frame = send("{\"type\": \"init\"}");

        assertEquals(List.of("type", "loop", "gamepad1Rumbles", "gamepad2Rumbles", "events", "finished", "controllers",
                "controllerProblem", "driveCommand"),
                List.copyOf(frame.keySet()));
        assertEquals(List.of("sinceInitSeconds", "matchTimeSeconds", "stage", "pose", "motors", "servos", "autoStep",
                "telemetry", "gamepad1", "gamepad2", "aprilTags"), List.copyOf(loop(frame).keySet()));
    }

    /**
     * WHAT: "save" writes the run as a normal recording that the viewers can replay.
     */
    @Test
    void saveWritesAReplayableRecording() throws Exception {
        connectAndLoad("teamA-teleop");
        send("{\"type\": \"init\"}");
        send("{\"type\": \"play\"}");
        for (int i = 0; i < 25; i++) {
            send("{\"type\": \"step\", \"gamepad1\": " + FORWARD + "}");
        }
        send("{\"type\": \"stop\"}");

        Map<String, Object> saved = send("{\"type\": \"save\", \"name\": \"BridgeSessionTest-live\"}");

        assertEquals("saved", saved.get("type"));
        Path file = Path.of((String) saved.get("path"));
        assertTrue(Files.exists(file), file.toString());
        RunRecording recording = RunRecordingJson.load(file);
        assertEquals(1 + 1 + 25 + 1, recording.loops().size(), "INIT, START, 25 loops, STOP");
        assertEquals("TeamATeleOp", recording.header().opMode());
    }

    // ---- Mistakes ---------------------------------------------------------------------------------

    /**
     * WHAT: Mistakes get a clear "error" reply, and the session keeps working afterwards.
     * WHY: A typo in Unity's code must not crash the harness or leave it stuck.
     */
    @Test
    void mistakesGetClearErrorsAndTheSessionCarriesOn() {
        assertTrue(message(send("{\"type\": \"load\", \"opMode\": \"teamA-teleop\"}")).contains("hello"));
        assertTrue(message(send("not json")).contains("Not valid JSON"));
        assertTrue(message(send("[1, 2]")).contains("JSON object"));
        assertTrue(message(send("{\"type\": \"jump\"}")).contains("Unknown message type"));
        send("{\"type\": \"hello\", \"version\": 3}");
        assertTrue(message(send("{\"type\": \"step\"}")).contains("after \"init\""));
        assertTrue(message(send("{\"type\": \"load\", \"opMode\": \"nope\"}")).contains("Unknown OpMode \"nope\""));
        send("{\"type\": \"load\", \"opMode\": \"teamA-teleop\"}");
        send("{\"type\": \"init\"}");
        send("{\"type\": \"play\"}");
        String badPad = "{\"type\": \"step\", \"gamepad1\": {\"buttons\": [], \"leftStickX\": \"left\"}}";
        assertTrue(message(send(badPad)).contains("gamepad1.leftStickX"), "names the bad field");
        assertTrue(message(send("{\"type\": \"save\", \"name\": \"../escape\"}")).contains("not allowed"));

        assertEquals("frame", send("{\"type\": \"step\"}").get("type"), "still works after the mistakes");
        assertFalse(session.isClosing());
    }

    private static String message(Map<String, Object> reply) {
        assertEquals("error", reply.get("type"), String.valueOf(reply));
        return (String) reply.get("message");
    }

    // ---- Repeatability ------------------------------------------------------------------------

    /**
     * WHAT: The same messages always give exactly the same replies.
     * HOW: Two fresh sessions get the same 60 messages (with gamepad changes); every reply matches.
     * WHY: Unity drives the clock, so a live drive can be replayed or tested exactly
     * (02-unity-readiness.md §2–§3).
     */
    @Test
    void sameMessagesGiveTheSameReplies() {
        List<String> script = new ArrayList<>(List.of("{\"type\": \"hello\", \"version\": 3}",
                "{\"type\": \"load\", \"opMode\": \"teamA-teleop\"}", "{\"type\": \"init\"}", "{\"type\": \"play\"}"));
        for (int i = 0; i < 56; i++) {
            script.add("{\"type\": \"step\", \"gamepad1\": " + (i % 20 < 10 ? FORWARD : RELEASED) + "}");
        }

        List<String> first = new ArrayList<>();
        BridgeSession a = new BridgeSession();
        script.forEach(line -> first.add(a.handle(line)));
        List<String> second = new ArrayList<>();
        BridgeSession b = new BridgeSession();
        script.forEach(line -> second.add(b.handle(line)));

        assertEquals(first, second);
    }
}
