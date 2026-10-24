package pvi.harness.bridge;

import com.qualcomm.robotcore.hardware.Gamepad;

import pvi.harness.ds.Axis;
import pvi.harness.ds.Button;
import pvi.harness.ds.GamepadSource;
import pvi.harness.runtime.RunRecording;

/**
 * One gamepad driven from Unity. Each "step" message can carry the gamepad's state; this holds
 * the newest one and, through the harness's single live-input entry point ({@link GamepadSource}),
 * puts it on the gamepad just before the OpMode runs. A step without a gamepad keeps the last
 * state (buttons stay held), like a real controller.
 *
 * <p>Everything happens on the bridge's one thread (read a message, then run the OpMode), so no
 * locking is needed.</p>
 */
final class BridgeGamepads implements GamepadSource {
    /** What Unity last said the driver is doing. */
    private RunRecording.GamepadState state = RunRecording.GamepadState.RELEASED;

    /** Sets what the driver is doing from now on. */
    void set(RunRecording.GamepadState newState) {
        state = newState;
    }

    /** What the driver is doing now. */
    RunRecording.GamepadState state() {
        return state;
    }

    @Override
    public void update(Gamepad pad, double sinceInit, double sinceStart) {
        for (Button button : Button.values()) {
            button.set(pad, state.buttons().contains(button));
        }
        Axis.LEFT_STICK_X.set(pad, state.leftStickX());
        Axis.LEFT_STICK_Y.set(pad, state.leftStickY());
        Axis.RIGHT_STICK_X.set(pad, state.rightStickX());
        Axis.RIGHT_STICK_Y.set(pad, state.rightStickY());
        Axis.LEFT_TRIGGER.set(pad, state.leftTrigger());
        Axis.RIGHT_TRIGGER.set(pad, state.rightTrigger());
    }
}
