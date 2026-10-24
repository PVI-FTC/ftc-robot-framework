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
 * </ul>
 *
 * <pre>
 *   Unity                          harness
 *   hello {version: 1}       -->
 *                            <--   welcome {opModes: [...]}
 *   load {opMode: "teamA-teleop"} -->
 *                            <--   loaded {header: {...}}
 *   init                     -->
 *                            <--   frame {loop: {...}}          (INIT)
 *   step {gamepad1: {...}}   -->
 *                            <--   frame {loop: {...}}          (INIT_LOOP, repeat)
 *   play                     -->
 *                            <--   frame {loop: {...}}          (START)
 *   step {gamepad1: {...}}   -->
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
    public static final int VERSION = 1;
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
