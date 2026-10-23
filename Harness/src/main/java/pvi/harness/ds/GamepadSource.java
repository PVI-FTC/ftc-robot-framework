package pvi.harness.ds;

import com.qualcomm.robotcore.hardware.Gamepad;

/**
 * Something outside the test that drives a gamepad live: the keyboard in the viewer window, a
 * real controller, or (later) Unity.
 *
 * <p>This is the one way live input reaches robot code. Every loop, just before the OpMode
 * runs, the harness calls {@link #update} on the runner's own thread with the gamepad "in the
 * driver's hands". The source sets it to what the driver is doing right now. The harness then
 * hands it to the OpMode through the SDK's {@code Gamepad.copy()}, exactly like scripted input,
 * so {@code aWasPressed()}-style edge detection works the same.</p>
 *
 * <p>A source that gets its input on another thread (the Swing window's, for example) must
 * keep it in a thread-safe form, and only touch {@code pad} inside {@link #update}.</p>
 */
public interface GamepadSource {

    /**
     * Sets {@code pad} to the driver's current input. Called once per loop, after any scripted
     * actions for that loop.
     *
     * @param pad        the gamepad to change (use {@link Button#set} and {@link Axis#set})
     * @param sinceInit  seconds since INIT
     * @param sinceStart seconds since PLAY, or a negative number before PLAY
     */
    void update(Gamepad pad, double sinceInit, double sinceStart);
}
