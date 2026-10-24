package pvi.harness.bridge;

import org.junit.jupiter.api.Test;

import java.util.Map;

import pvi.harness.physics.DriveModels;
import pvi.harness.runtime.JsonReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Stage U3, harness side: with {@code "physics": "unity"}, Unity's physics moves the robot. The
 * harness tells Unity the wheels' command in each frame and takes the robot state Unity sends
 * back with the next step (no Unity needed: these tests play Unity's part).
 */
class BridgeUnityPhysicsTest {
    /** Full forward on the left stick (up is negative). */
    private static final String FORWARD = "{\"buttons\": [], \"leftStickX\": 0, \"leftStickY\": -1, \"rightStickX\": 0,"
            + " \"rightStickY\": 0, \"leftTrigger\": 0, \"rightTrigger\": 0}";

    private final BridgeSession session = new BridgeSession();

    @SuppressWarnings("unchecked")
    private Map<String, Object> send(String line) {
        return (Map<String, Object>) JsonReader.parse(session.handle(line));
    }

    /** A physicsState with the robot at (x, y), facing heading, moving forward at speed in/s. */
    private static String state(double x, double y, double headingRadians, double forwardSpeed, String wall) {
        return "{\"xInches\": " + x + ", \"yInches\": " + y + ", \"headingRadians\": " + headingRadians
                + ", \"forwardInchesPerSecond\": " + forwardSpeed + ", \"leftInchesPerSecond\": 0"
                + ", \"counterClockwiseRadiansPerSecond\": 0, \"wallContact\": \"" + wall + "\"}";
    }

    private Map<String, Object> loadTeleOp(String physics) {
        send("{\"type\": \"hello\", \"version\": 3}");
        return send("{\"type\": \"load\", \"opMode\": \"teamA-teleop\", \"physics\": \"" + physics + "\"}");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> pose(Map<String, Object> frame) {
        return (Map<String, Object>) ((Map<String, Object>) frame.get("loop")).get("pose");
    }

    /**
     * WHAT: The robot is wherever Unity's physics says: after Unity reports (10, 4) at 30°, the
     * frame's pose (the same one the recording and the encoders use) is exactly that.
     * WHY: This is the whole point of U3: Unity, not the harness, decides where the robot ends up
     * (walls and field parts included).
     */
    @Test
    void theRobotIsWhereUnitysPhysicsPutsIt() {
        assertEquals("unity", loadTeleOp("unity").get("physics"));
        send("{\"type\": \"init\"}");
        send("{\"type\": \"play\", \"physicsState\": " + state(0, 0, 0, 0, "") + "}");

        Map<String, Object> frame = send("{\"type\": \"step\", \"physicsState\": " + state(10, 4, Math.toRadians(30), 12, "") + "}");

        assertEquals("frame", frame.get("type"), String.valueOf(frame));
        assertEquals(10.0, (Double) pose(frame).get("xInches"), 1e-9);
        assertEquals(4.0, (Double) pose(frame).get("yInches"), 1e-9);
        assertEquals(30.0, Math.toDegrees((Double) pose(frame).get("headingRadians")), 1e-3, "frames keep 6 decimals");
    }

    /**
     * WHAT: Each frame tells Unity what the wheels are doing: full forward on the stick gives four
     * positive pushes, and whether the motors brake.
     */
    @Test
    @SuppressWarnings("unchecked")
    void framesTellUnityTheDriveCommand() {
        loadTeleOp("unity");
        send("{\"type\": \"init\"}");
        send("{\"type\": \"play\", \"physicsState\": " + state(0, 0, 0, 0, "") + "}");

        Map<String, Object> frame = send("{\"type\": \"step\", \"gamepad1\": " + FORWARD
                + ", \"physicsState\": " + state(0, 0, 0, 0, "") + "}");

        Map<String, Object> command = (Map<String, Object>) frame.get("driveCommand");
        assertNotNull(command, String.valueOf(frame));
        for (String wheel : new String[] {"frontLeft", "frontRight", "rearLeft", "rearRight"}) {
            assertTrue((Double) command.get(wheel) > 0.5, wheel + " pushes forward: " + command);
        }
        assertTrue(command.get("braking") instanceof Boolean, "braking is true or false: " + command);
    }

    /**
     * WHAT: "loaded" tells Unity how to move this robot like the harness would: Team A's speeds,
     * response and traction numbers, size and start pose.
     */
    @Test
    @SuppressWarnings("unchecked")
    void loadedDescribesTheRobotForUnitysPhysics() {
        Map<String, Object> robot = (Map<String, Object>) loadTeleOp("unity").get("robotPhysics");

        assertEquals(DriveModels.teamA().forwardInchesPerSecond(), (Double) robot.get("forwardInchesPerSecond"), 1e-5);
        assertEquals(DriveModels.teamA().dynamics().maxAcceleration().value(),
                (Double) robot.get("maxAccelerationInchesPerSecondSquared"), 1e-5);
        assertEquals(DriveModels.teamA().lengthInches(), (Double) robot.get("lengthInches"), 1e-5);
        assertEquals(0.0, (Double) ((Map<String, Object>) robot.get("start")).get("xInches"), 1e-9);
    }

    /**
     * WHAT: A wall contact Unity reports becomes a wall hit in the run's events, as with the
     * harness's own physics.
     */
    @Test
    @SuppressWarnings("unchecked")
    void aWallContactFromUnityIsAWallHit() {
        loadTeleOp("unity");
        send("{\"type\": \"init\"}");
        send("{\"type\": \"play\", \"physicsState\": " + state(0, 0, 0, 0, "") + "}");

        Map<String, Object> frame = send("{\"type\": \"step\", \"physicsState\": " + state(63, 0, 0, 0, "+x") + "}");

        assertTrue(String.valueOf(frame.get("events")).contains("+x wall"), "events: " + frame.get("events"));
    }

    /**
     * WHAT: Mix-ups get clear errors and the session carries on: a step without physicsState under
     * Unity's physics, a physicsState under the harness's own, a wrong "physics" value, a
     * non-number in physicsState.
     */
    @Test
    void mixUpsGetClearErrors() {
        loadTeleOp("unity");
        send("{\"type\": \"init\"}");
        Map<String, Object> missing = send("{\"type\": \"step\"}");
        assertEquals("error", missing.get("type"));
        assertTrue(((String) missing.get("message")).contains("needs a \"physicsState\""), String.valueOf(missing));

        Map<String, Object> badNumber = send("{\"type\": \"step\", \"physicsState\": {\"xInches\": \"far\"}}");
        assertTrue(((String) badNumber.get("message")).contains("physicsState.xInches must be a number"), String.valueOf(badNumber));

        Map<String, Object> wrong = send("{\"type\": \"load\", \"opMode\": \"teamA-teleop\", \"physics\": \"real\"}");
        assertTrue(((String) wrong.get("message")).contains("\"physics\" must be"), String.valueOf(wrong));

        assertEquals("harness", send("{\"type\": \"load\", \"opMode\": \"teamA-teleop\"}").get("physics"));
        send("{\"type\": \"init\"}");
        Map<String, Object> notWanted = send("{\"type\": \"step\", \"physicsState\": " + state(0, 0, 0, 0, "") + "}");
        assertTrue(((String) notWanted.get("message")).contains("must not carry"), String.valueOf(notWanted));
        assertEquals("frame", send("{\"type\": \"step\"}").get("type"), "the session carries on");
    }
}
