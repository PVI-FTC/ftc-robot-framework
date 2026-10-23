package pvi.harness.viewer;

import java.util.List;

import com.qualcomm.robotcore.hardware.Gamepad;

import pvi.harness.ds.GamepadSource;

/**
 * Gamepad 1 and gamepad 2 for a live drive: a controller when one is connected in that slot,
 * otherwise the keyboard ({@link KeyboardGamepads}).
 *
 * <p>Controllers are read once per loop (when gamepad 1 is updated, which the harness always
 * does first), so both gamepads see the same moment.</p>
 *
 * <p><b>Disconnects.</b> If a controller drops out mid-drive (battery, sleep, out of range), that
 * gamepad falls back to the keyboard in the same loop. The keyboard sets every input, so
 * everything the controller was holding is released at once: the robot never keeps "holding" a
 * stick. The {@link Listener} is told, so the drive can mark it in the recording and warn the
 * driver.</p>
 */
public final class LiveGamepads {

    /** Told when a controller connects or disconnects. Called on the live drive's thread. */
    public interface Listener {
        /**
         * @param gamepadNumber 1 or 2
         * @param reading       the controller now ({@link ControllerReading#DISCONNECTED} if it left)
         * @param connected     true if it just connected, false if it just disconnected
         */
        void controllerChanged(int gamepadNumber, ControllerReading reading, boolean connected);
    }

    private final ControllerPoller controllers;
    private final Listener listener;
    private final GamepadSource[] keyboard;
    private final boolean[] connected = new boolean[2];
    private final String[] status = {"keyboard", "keyboard"};
    /** This loop's controller readings, by slot. */
    private List<ControllerReading> readings = List.of();

    public LiveGamepads(HeldKeys keys, ControllerPoller controllers, Listener listener) {
        this.controllers = controllers;
        this.listener = listener;
        this.keyboard = new GamepadSource[] {KeyboardGamepads.driver(keys), KeyboardGamepads.operator(keys)};
    }

    /** Gamepad 1 (driver). Reads the controllers, then sets the gamepad. */
    public GamepadSource gamepad1() {
        return (pad, sinceInit, sinceStart) -> {
            readings = controllers.poll();
            apply(0, pad, sinceInit, sinceStart);
        };
    }

    /** Gamepad 2 (operator), from the readings gamepad 1 just took. */
    public GamepadSource gamepad2() {
        return (pad, sinceInit, sinceStart) -> apply(1, pad, sinceInit, sinceStart);
    }

    /** What drives each gamepad right now, e.g. "PS5 Controller" or "keyboard". Safe from any thread. */
    public synchronized List<String> status() {
        return List.of(status[0], status[1]);
    }

    /** Sets gamepad {@code slot + 1} from its controller, or from the keyboard if there is none. */
    private void apply(int slot, Gamepad pad, double sinceInit, double sinceStart) {
        ControllerReading reading = slot < readings.size() ? readings.get(slot) : ControllerReading.DISCONNECTED;
        if (reading.connected()) {
            reading.applyTo(pad);
        } else {
            keyboard[slot].update(pad, sinceInit, sinceStart);
        }
        if (reading.connected() != connected[slot]) {
            connected[slot] = reading.connected();
            synchronized (this) {
                status[slot] = reading.connected() ? describe(reading) : "keyboard";
            }
            listener.controllerChanged(slot + 1, reading, reading.connected());
        }
    }

    /** "PS5 Controller", or "controller" if it gave no name. */
    static String describe(ControllerReading reading) {
        return reading.type().isBlank() ? "controller" : reading.type();
    }
}
