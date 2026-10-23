package pvi.harness.viewer;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The keyboard keys held down right now, by {@link java.awt.event.KeyEvent} key code.
 *
 * <p>The Swing thread adds and removes keys as they are pressed and released. The live drive's
 * thread reads them once per loop (through {@link KeyboardGamepads}). A concurrent set keeps
 * that safe without locks.</p>
 */
public final class HeldKeys {
    private final Set<Integer> held = ConcurrentHashMap.newKeySet();

    /** A key went down. */
    public void press(int keyCode) {
        held.add(keyCode);
    }

    /** A key came up. */
    public void release(int keyCode) {
        held.remove(keyCode);
    }

    /** Lets go of every key, e.g. when the window loses focus, so no key stays "stuck". */
    public void releaseAll() {
        held.clear();
    }

    /** True if {@code keyCode} is held down. */
    public boolean isHeld(int keyCode) {
        return held.contains(keyCode);
    }
}
