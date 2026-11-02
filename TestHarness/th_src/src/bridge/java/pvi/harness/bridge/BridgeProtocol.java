package pvi.harness.bridge;

/**
 * The live connection to Unity (Stage U2), message format version {@value #VERSION}. The full
 * description, with every field, is in Harness/README.md, "Live connection to Unity".
 *
 * <p>In short:</p>
 * <ul>
 *   <li>A local TCP connection on 127.0.0.1, port {@value #DEFAULT_PORT} by default. Unity
 *       connects; the harness listens. One client at a time.</li>
 *   <li>Each message is one line of UTF-8 JSON ending in "\n", with a {@code "type"} field.</li>
 *   <li>Unity sends a request; the harness answers each one with exactly one reply, then waits.
 *       The harness never runs ahead on its own: <b>Unity drives the clock</b> by asking for one
 *       OpMode call ("step") at a time, about every 20 ms of real time. So the same messages
 *       always give exactly the same replies.</li>
 *   <li>Robot state comes back in the run recording's own layout (one "loop" entry per step),
 *       so Unity reads it with the same classes it uses for saved recordings.</li>
 *   <li>Who drives the gamepads is chosen in "load": {@code "input": "controllers"} means the
 *       harness reads the real game controllers plugged into this computer (decided by the user,
 *       2026-10-24: Unity only displays); {@code "input": "messages"} means Unity sends them in
 *       each "step" (useful for scripted clients and tests). Version 2 added this.</li>
 *   <li>Who moves the robot is chosen in "load" too (version 3, Stage U3): {@code "physics":
 *       "harness"} (the default) uses the harness's own physics; {@code "physics": "unity"} means
 *       Unity's physics moves the robot. Then each frame carries the wheels' {@code driveCommand},
 *       Unity runs its physics for one loop with it, and sends the result as {@code physicsState}
 *       in its next "step" or "play" ({@link RemotePhysics}).</li>
 *   <li>Version 4 added the game pieces (POLLEN, NECTAR): {@code pieces} in the "loaded" header
 *       (every piece at INIT) and in each frame's loop (the pieces that changed in that loop), in
 *       the run recording's version 4 layout. The harness moves them, in either physics mode.</li>
 *   <li>Version 5 lets Unity's physics stop the robot at the field elements (HIVE frame, FLOWERs) as
 *       the harness's own physics does: "loaded" robotPhysics says {@code robotObstacles}, and each
 *       physicsState may carry {@code obstacleContact} ("HIVE frame", "far FLOWER"), which becomes a
 *       "Robot hit the ..." event, the same as with the harness's physics.</li>
 * </ul>
 *
 * <pre>
 *   Unity                          harness
 *   hello {version: 5}       -->
 *                            <--   welcome {opModes: [...]}
 *   load {opMode: "TeamATeleOp", input: "controllers"} -->
 *                            <--   loaded {header: {...}}
 *   init                     -->
 *                            <--   frame {loop: {...}}          (INIT)
 *   step                     -->
 *                            <--   frame {loop: {...}}          (INIT_LOOP, repeat)
 *   play                     -->
 *                            <--   frame {loop: {...}}          (START)
 *   step                     -->
 *                            <--   frame {loop: {...}}          (LOOP, repeat; finished at the end of the match)
 *   stop                     -->
 *                            <--   frame {loop: {...}, finished: true}   (STOP)
 *   save {name: "my-drive"}  -->
 *                            <--   saved {path: "..."}
 *   bye                      -->
 *                            <--   goodbye
 * </pre>
 */
public final class BridgeProtocol {
    /** Message format version. Raise it (and update the README) whenever any message changes. */
    public static final int VERSION = 5;
    /** The port the harness listens on unless told otherwise. */
    public static final int DEFAULT_PORT = 7465;

    // Message types Unity sends.
    public static final String HELLO = "hello";
    public static final String LOAD = "load";
    public static final String INIT = "init";
    public static final String STEP = "step";
    public static final String PLAY = "play";
    public static final String STOP = "stop";
    public static final String SAVE = "save";
    public static final String BYE = "bye";

    // Message types the harness sends.
    public static final String WELCOME = "welcome";
    public static final String LOADED = "loaded";
    public static final String FRAME = "frame";
    public static final String SAVED = "saved";
    public static final String GOODBYE = "goodbye";
    public static final String ERROR = "error";

    /** Only constants here; nothing to create. */
    private BridgeProtocol() {
    }
}
