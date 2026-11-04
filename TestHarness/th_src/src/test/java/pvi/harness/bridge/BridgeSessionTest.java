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

    /**
     * WHAT: When the harness can't list its OpModes (here: harness.properties from an older copy),
     * "hello" answers with an error that says why, and the session keeps answering.
     * WHY: On a laptop with a mixed USB copy the bridge died before "welcome", so Unity showed no
     * OpModes and no reason (T16). The error text is what Unity shows the user.
     */
    @Test
    void aBrokenConfigIsReportedOnHello() {
        BridgeSession broken = new BridgeSession(() -> null, () -> {
            throw new IllegalArgumentException("Problems in harness.properties:\n  config.version is 3, but this harness reads 4");
        });

        String reply = broken.handle("{\"type\": \"hello\", \"version\": " + BridgeProtocol.VERSION + "}");

        assertTrue(reply.contains("\"type\": \"error\"") || reply.contains("\"type\":\"error\""), reply);
        assertTrue(reply.contains("can't list its OpModes"), reply);
        assertTrue(reply.contains("config.version is 3"), reply);
        assertTrue(broken.handle("{\"type\": \"load\", \"opMode\": \"x\"}").contains("hello"), "still answering");
    }

    /** Sends one message and returns the parsed reply. */
    @SuppressWarnings("unchecked")
    private Map<String, Object> send(String line) {
        return (Map<String, Object>) JsonReader.parse(session.handle(line));
    }

    /** The TeleOp these tests drive: this repo's (viewer.defaultTeleOp, else the first TeleOp found). */
    private static final String TELEOP = BridgeTestOpModes.teleOp().id();
    /** Its front-left drive motor, by its configuration name. */
    private static final String FRONT_LEFT = BridgeTestOpModes.frontLeft();

    /** hello + load, ready for "init". */
    private void connectAndLoad(String opMode) {
        assertEquals("welcome", send("{\"type\": \"hello\", \"version\": " + BridgeProtocol.VERSION + "}").get("type"));
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
    private static double yInches(Map<String, Object> frame) {
        return (Double) ((Map<String, Object>) loop(frame).get("pose")).get("yInches");
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
     * WHAT: "hello" with the current version gets "welcome" listing the OpModes Unity can run.
     * WHY: Unity builds its menu from this list, and must know both sides speak the same version.
     */
    @Test
    @SuppressWarnings("unchecked")
    void helloListsTheOpModes() {
        Map<String, Object> welcome = send("{\"type\": \"hello\", \"version\": " + BridgeProtocol.VERSION + ", \"client\": \"test\"}");

        assertEquals("welcome", welcome.get("type"));
        assertEquals((double) BridgeProtocol.VERSION, welcome.get("version"));
        assertEquals(0.02, welcome.get("loopPeriodSeconds"));
        List<Object> opModes = (List<Object>) welcome.get("opModes");
        Map<String, Object> teleOp = (Map<String, Object>) opModes.stream()
                .filter(o -> TELEOP.equals(((Map<String, Object>) o).get("id"))).findFirst().orElseThrow();
        assertEquals(BridgeTestOpModes.teleOp().title(), teleOp.get("title"));
        assertEquals("TELEOP", teleOp.get("period"));
        assertEquals(BridgeOpModes.all().size(), opModes.size());
        if (BridgeTestOpModes.pvi()) {
            assertEquals("Team A Mecanum Drive", teleOp.get("title"), "PVI's Team A TeleOp, by its Driver Station name");
        }
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
        assertTrue(message.contains("version " + BridgeProtocol.VERSION) && message.contains("version 99"), message);
        assertTrue(session.isClosing());
    }

    // ---- A run ----------------------------------------------------------------------------------

    /**
     * WHAT: A whole TeleOp run driven from "Unity": the robot drives forward while the gamepad
     * says full forward, and each frame carries the stage and pose.
     *
     * <p>HOW: load this repo's TeleOp (Team A's for PVI), init, 10 init_loop steps, play, then 50 steps (1 s) with the left
     * stick full forward, then stop. The robot ends well forward (over 30 in; Team A tops out at
     * 62.6 in/s), and the stop frame says finished.</p>
     *
     * <p>WHY: This is the whole point of Stage U2: Unity's gamepad drives the real TeamCode.</p>
     */
    @Test
    void unityGamepadDrivesTheRealTeleOp() {
        connectAndLoad(TELEOP);

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
        BridgeTestOpModes.assertFullStick(power(frame, FRONT_LEFT), "full forward");
        // How far exactly depends on the physics model (top speed 62.6 in/s); 1 s of full stick
        // must clearly move the robot: over 30 in forward for PVI's Team A, over 10 in some way for
        // another team's TeleOp.
        if (BridgeTestOpModes.pvi()) {
            assertTrue(xInches(frame) > 30, "drove forward: " + xInches(frame));
        } else {
            // Another team's TeleOp may drive this stick sideways, into the HIVE frame about 15 in away.
            assertTrue(Math.hypot(xInches(frame), yInches(frame)) > 10, "drove: " + xInches(frame) + ", " + yInches(frame));
        }
        assertEquals(Boolean.FALSE, frame.get("finished"));
        Map<String, Object> stopped = send("{\"type\": \"stop\"}");
        assertEquals("STOP", loop(stopped).get("stage"));
        assertEquals(Boolean.TRUE, stopped.get("finished"));
        if (BridgeTestOpModes.pvi()) {
            assertEquals(0.0, power(stopped, FRONT_LEFT), 1e-9, "PVI's TeamCode stop() zeroes the drive");
        }
    }

    /**
     * WHAT: A step without a gamepad keeps the last one (still held); a released gamepad stops the robot.
     * WHY: Unity only needs to send the gamepad when it changes, like a real controller staying put.
     */
    @Test
    void gamepadStaysHeldUntilUnitySendsANewOne() {
        connectAndLoad(TELEOP);
        send("{\"type\": \"init\"}");
        send("{\"type\": \"play\"}");

        send("{\"type\": \"step\", \"gamepad1\": " + FORWARD + "}");
        Map<String, Object> held = send("{\"type\": \"step\"}");
        BridgeTestOpModes.assertFullStick(power(held, FRONT_LEFT), "still full forward");

        Map<String, Object> released = send("{\"type\": \"step\", \"gamepad1\": " + RELEASED + "}");
        assertEquals(0.0, power(released, FRONT_LEFT), 1e-9);
    }

    /**
     * WHAT: The run stops by itself (at the end of the 30 s match at the latest), and stepping after
     * that is refused.
     * HOW: this repo's autonomous, stepping until the harness says finished. PVI's Team A
     * autonomous runs the whole match: PLAY takes the first 20 ms, so 1499 steps reach 30 s (the
     * same count as a normal 30 s test run) and that frame is STOP at 30.00 s. Another team's
     * autonomous may finish sooner (a LinearOpMode that returns), which also ends the run.
     */
    @Test
    void autonomousStopsByItselfAtTheEndOfTheMatch() {
        connectAndLoad(BridgeTestOpModes.autonomous().id());
        send("{\"type\": \"init\"}");
        send("{\"type\": \"play\"}");

        Map<String, Object> frame = null;
        int steps = 0;
        while (steps < 2000 && (frame == null || frame.get("finished") != Boolean.TRUE)) {
            frame = send("{\"type\": \"step\"}");
            steps++;
        }

        assertEquals("STOP", loop(frame).get("stage"));
        assertEquals(Boolean.TRUE, frame.get("finished"));
        assertTrue(steps <= 1499 && (Double) loop(frame).get("matchTimeSeconds") <= 30.0 + 1e-9,
                "stopped by 30 s: " + steps + " steps");
        if (BridgeTestOpModes.pvi()) {
            assertEquals(1499, steps, "Team A's autonomous runs the whole match");
            assertEquals(30.0, (Double) loop(frame).get("matchTimeSeconds"), 1e-9);
            assertEquals(9.4, xInches(frame), 0.5, "Team A's autonomous drives about 9.4 in");
        }
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
        BridgeTestOpModes.assumePvi(); // PVI's rumble demo
        connectAndLoad("RumbleDemoTeleOp");
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
        connectAndLoad(TELEOP);

        Map<String, Object> frame = send("{\"type\": \"init\"}");

        assertEquals(List.of("type", "loop", "gamepad1Rumbles", "gamepad2Rumbles", "events", "finished", "controllers",
                "controllerProblem", "driveCommand"),
                List.copyOf(frame.keySet()));
        assertEquals(List.of("sinceInitSeconds", "matchTimeSeconds", "stage", "pose", "motors", "servos", "autoStep",
                "telemetry", "gamepad1", "gamepad2", "aprilTags", "pieces"), List.copyOf(loop(frame).keySet()));
    }

    /**
     * WHAT: "save" writes the run as a normal recording that the viewers can replay.
     */
    @Test
    void saveWritesAReplayableRecording() throws Exception {
        connectAndLoad(TELEOP);
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
        assertEquals(TELEOP, recording.header().opMode());
    }

    // ---- Mistakes ---------------------------------------------------------------------------------

    /**
     * WHAT: Mistakes get a clear "error" reply, and the session keeps working afterwards.
     * WHY: A typo in Unity's code must not crash the harness or leave it stuck.
     */
    @Test
    void mistakesGetClearErrorsAndTheSessionCarriesOn() {
        assertTrue(message(send("{\"type\": \"load\", \"opMode\": \"" + TELEOP + "\"}")).contains("hello"));
        assertTrue(message(send("not json")).contains("Not valid JSON"));
        assertTrue(message(send("[1, 2]")).contains("JSON object"));
        assertTrue(message(send("{\"type\": \"jump\"}")).contains("Unknown message type"));
        send("{\"type\": \"hello\", \"version\": " + BridgeProtocol.VERSION + "}");
        assertTrue(message(send("{\"type\": \"step\"}")).contains("after \"init\""));
        assertTrue(message(send("{\"type\": \"load\", \"opMode\": \"nope\"}")).contains("Unknown OpMode \"nope\""));
        send("{\"type\": \"load\", \"opMode\": \"" + TELEOP + "\"}");
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
        List<String> script = new ArrayList<>(List.of("{\"type\": \"hello\", \"version\": " + BridgeProtocol.VERSION + "}",
                "{\"type\": \"load\", \"opMode\": \"" + TELEOP + "\"}", "{\"type\": \"init\"}", "{\"type\": \"play\"}"));
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
