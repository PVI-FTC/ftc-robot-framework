package pvi.harness.viewer;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.studiohartman.jamepad.Configuration;
import com.studiohartman.jamepad.ControllerManager;
import com.studiohartman.jamepad.ControllerState;

import pvi.harness.ds.Button;

/**
 * Real USB and wireless controllers, read with Jamepad (a Java wrapper around SDL, the
 * controller library many games use). Xbox, PlayStation (DualShock 4 / DualSense) and Logitech
 * F310/F710 (switch on "X") controllers all work, wired or Bluetooth, once Windows sees them.
 *
 * <p>SDL is started on the first {@link #poll()}, on the live drive's thread, and stopped in
 * {@link #close()}. If it can't start, controllers are simply off: {@link #problem()} says why
 * and the keyboard still works.</p>
 *
 * <p>SDL already reports stick Y as "down is positive", which is FTC's "up is negative", so the
 * values are used as they are.</p>
 */
public final class JamepadControllers implements ControllerPoller {
    /** Gamepad 1 and gamepad 2. */
    private static final int SLOTS = 2;

    private ControllerManager manager;
    private boolean triedToStart;
    private volatile String problem;

    @Override
    public List<ControllerReading> poll() {
        if (!triedToStart) {
            triedToStart = true;
            start();
        }
        if (manager == null) {
            return List.of();
        }
        List<ControllerReading> readings = new ArrayList<>();
        try {
            manager.update();
            for (int slot = 0; slot < SLOTS; slot++) {
                readings.add(convert(manager.getState(slot)));
            }
        } catch (RuntimeException e) {
            problem = "Reading the controllers failed: " + e.getMessage();
        }
        return readings;
    }

    @Override
    public String problem() {
        return problem;
    }

    @Override
    public void close() {
        if (manager != null) {
            manager.quitSDLGamepad();
            manager = null;
        }
    }

    /** Starts SDL; on failure, leaves controllers off and records why. */
    private void start() {
        try {
            Configuration configuration = new Configuration();
            configuration.maxNumControllers = SLOTS;
            // Our own (comment-only) mappings file: Jamepad's default one isn't in its jar, and
            // without a file it prints an alarming error. SDL's built-in mappings do the work.
            ControllerManager started = new ControllerManager(configuration, "/harness-controller-mappings.txt");
            started.initSDLGamepad();
            manager = started;
        } catch (RuntimeException | LinkageError e) {
            problem = "Controllers are off (the controller library could not start: " + e + ").";
        }
    }

    /** Jamepad's state, in FTC terms. Button positions match: Jamepad's a/b/x/y are FTC's. */
    static ControllerReading convert(ControllerState s) {
        if (!s.isConnected) {
            return ControllerReading.DISCONNECTED;
        }
        Set<Button> held = EnumSet.noneOf(Button.class);
        addIf(held, s.a, Button.A);
        addIf(held, s.b, Button.B);
        addIf(held, s.x, Button.X);
        addIf(held, s.y, Button.Y);
        addIf(held, s.dpadUp, Button.DPAD_UP);
        addIf(held, s.dpadDown, Button.DPAD_DOWN);
        addIf(held, s.dpadLeft, Button.DPAD_LEFT);
        addIf(held, s.dpadRight, Button.DPAD_RIGHT);
        addIf(held, s.lb, Button.LEFT_BUMPER);
        addIf(held, s.rb, Button.RIGHT_BUMPER);
        addIf(held, s.leftStickClick, Button.LEFT_STICK_BUTTON);
        addIf(held, s.rightStickClick, Button.RIGHT_STICK_BUTTON);
        addIf(held, s.start, Button.START);
        addIf(held, s.back, Button.BACK);
        addIf(held, s.guide, Button.GUIDE);
        addIf(held, s.touchpadButton, Button.TOUCHPAD);
        return new ControllerReading(true, s.controllerType, s.leftStickX, s.leftStickY,
                s.rightStickX, s.rightStickY, s.leftTrigger, s.rightTrigger, held);
    }

    private static void addIf(Set<Button> held, boolean pressed, Button button) {
        if (pressed) {
            held.add(button);
        }
    }
}
