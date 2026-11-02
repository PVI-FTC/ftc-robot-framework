package pvi.harness.viewer;

import java.util.List;

/**
 * Reads the game controllers. Slot 0 becomes gamepad 1 (driver), slot 1 gamepad 2 (operator).
 *
 * <p>A live drive calls it once per loop, always from its own thread, so a controller library
 * that must stay on one thread (SDL) is safe. Tests use a fake instead of real controllers.</p>
 */
public interface ControllerPoller {

    /** No controllers at all (keyboard only). */
    ControllerPoller NONE = List::of;

    /** The controllers right now, by slot. A missing slot means no controller there. */
    List<ControllerReading> poll();

    /** A problem that stopped controllers from working (e.g. the library couldn't load), or null. */
    default String problem() {
        return null;
    }

    /** Lets go of the controller library when the drive ends. */
    default void close() {
    }
}
