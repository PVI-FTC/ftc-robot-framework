package pvi.harness.viewer;

import java.util.EnumSet;
import java.util.Set;

import com.qualcomm.robotcore.hardware.Gamepad;

import pvi.harness.ds.Axis;
import pvi.harness.ds.Button;

/**
 * One game controller's input at one moment, already in FTC terms: sticks -1..1 with
 * <b>up negative</b>, triggers 0..1, and FTC button names. Plain data, so the mapping can be
 * tested without a controller.
 *
 * <p>PlayStation controllers use the same positions: cross = A, circle = B, square = X,
 * triangle = Y.</p>
 *
 * @param connected true if a controller is plugged in (or paired) in this slot
 * @param type      what the controller says it is, e.g. "PS5 Controller"; "" if not connected
 * @param buttons   the buttons held down
 */
public record ControllerReading(boolean connected, String type,
                                double leftStickX, double leftStickY, double rightStickX, double rightStickY,
                                double leftTrigger, double rightTrigger, Set<Button> buttons) {

    /**
     * Stick values smaller than this count as 0, so a slightly worn stick that never quite
     * centers doesn't creep the robot.
     */
    public static final double STICK_DEADZONE = 0.05;

    /** No controller in this slot. */
    public static final ControllerReading DISCONNECTED =
            new ControllerReading(false, "", 0, 0, 0, 0, 0, 0, Set.of());

    /** Copies the button set, so the reading can't change after it is made. */
    public ControllerReading {
        type = type == null ? "" : type;
        buttons = buttons.isEmpty() ? Set.of() : Set.copyOf(EnumSet.copyOf(buttons));
    }

    /**
     * Sets every input on {@code pad} from this reading (sticks with the dead zone, triggers,
     * and every button, so released buttons are released).
     */
    public void applyTo(Gamepad pad) {
        Axis.LEFT_STICK_X.set(pad, deadzone(leftStickX));
        Axis.LEFT_STICK_Y.set(pad, deadzone(leftStickY));
        Axis.RIGHT_STICK_X.set(pad, deadzone(rightStickX));
        Axis.RIGHT_STICK_Y.set(pad, deadzone(rightStickY));
        Axis.LEFT_TRIGGER.set(pad, leftTrigger);
        Axis.RIGHT_TRIGGER.set(pad, rightTrigger);
        for (Button button : Button.values()) {
            button.set(pad, buttons.contains(button));
        }
    }

    /** 0 inside the dead zone, otherwise the value unchanged. */
    static double deadzone(double value) {
        return Math.abs(value) < STICK_DEADZONE ? 0.0 : value;
    }
}
