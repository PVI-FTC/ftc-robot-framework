package pvi.harness.viewer;

import java.awt.event.KeyEvent;
import java.util.List;
import java.util.Map;

import com.qualcomm.robotcore.hardware.Gamepad;

import pvi.harness.ds.Axis;
import pvi.harness.ds.Button;
import pvi.harness.ds.GamepadSource;

/**
 * The keyboard layout for live driving: turns held keys into gamepad 1 (driver) and gamepad 2
 * (operator) input.
 *
 * <pre>
 * GAMEPAD 1 (driver)                      GAMEPAD 2 (operator)
 *   Left stick     W A S D                  A B X Y   J K U I
 *   Right stick X  Left / Right arrow       LB / RB   O / P
 *   Right stick Y  Up / Down arrow          LT / RT   N / M
 *   Hold Shift     sticks at 40%            D-pad     7 8 9 0 (up down left right)
 *   A B X Y        F G R T                  Sticks    not on the keyboard
 *   LB / RB        Q / E
 *   LT / RT        Z / C (full press)
 *   D-pad          1 2 3 4 (up down left right)
 * </pre>
 *
 * <p>Every loop each source sets <b>every</b> input it controls, so letting go of a key always
 * releases it. Stick up is negative, as in FTC. Pressing both keys of a direction (W and S)
 * cancels out to 0.</p>
 */
public final class KeyboardGamepads {
    /** How far the sticks go while Shift is held, for fine control. */
    public static final double SHIFT_STICK_SCALE = 0.4;

    /** Driver buttons. */
    private static final Map<Button, Integer> DRIVER_BUTTONS = Map.ofEntries(
            Map.entry(Button.A, KeyEvent.VK_F), Map.entry(Button.B, KeyEvent.VK_G),
            Map.entry(Button.X, KeyEvent.VK_R), Map.entry(Button.Y, KeyEvent.VK_T),
            Map.entry(Button.LEFT_BUMPER, KeyEvent.VK_Q), Map.entry(Button.RIGHT_BUMPER, KeyEvent.VK_E),
            Map.entry(Button.DPAD_UP, KeyEvent.VK_1), Map.entry(Button.DPAD_DOWN, KeyEvent.VK_2),
            Map.entry(Button.DPAD_LEFT, KeyEvent.VK_3), Map.entry(Button.DPAD_RIGHT, KeyEvent.VK_4));
    /** Driver triggers (full press while held). */
    private static final Map<Axis, Integer> DRIVER_TRIGGERS = Map.of(
            Axis.LEFT_TRIGGER, KeyEvent.VK_Z, Axis.RIGHT_TRIGGER, KeyEvent.VK_C);

    /** Operator buttons. */
    private static final Map<Button, Integer> OPERATOR_BUTTONS = Map.ofEntries(
            Map.entry(Button.A, KeyEvent.VK_J), Map.entry(Button.B, KeyEvent.VK_K),
            Map.entry(Button.X, KeyEvent.VK_U), Map.entry(Button.Y, KeyEvent.VK_I),
            Map.entry(Button.LEFT_BUMPER, KeyEvent.VK_O), Map.entry(Button.RIGHT_BUMPER, KeyEvent.VK_P),
            Map.entry(Button.DPAD_UP, KeyEvent.VK_7), Map.entry(Button.DPAD_DOWN, KeyEvent.VK_8),
            Map.entry(Button.DPAD_LEFT, KeyEvent.VK_9), Map.entry(Button.DPAD_RIGHT, KeyEvent.VK_0));
    /** Operator triggers. */
    private static final Map<Axis, Integer> OPERATOR_TRIGGERS = Map.of(
            Axis.LEFT_TRIGGER, KeyEvent.VK_N, Axis.RIGHT_TRIGGER, KeyEvent.VK_M);

    /** Window key: press PLAY after INIT. */
    public static final int PLAY_KEY = KeyEvent.VK_ENTER;
    /** Window key: press STOP, save the drive and replay it. */
    public static final int STOP_KEY = KeyEvent.VK_ESCAPE;

    /** Only static methods here; nothing to create. */
    private KeyboardGamepads() {
    }

    /** Gamepad 1 (driver), read from {@code keys}. */
    public static GamepadSource driver(HeldKeys keys) {
        return (pad, sinceInit, sinceStart) -> {
            double scale = keys.isHeld(KeyEvent.VK_SHIFT) ? SHIFT_STICK_SCALE : 1.0;
            Axis.LEFT_STICK_X.set(pad, scale * direction(keys, KeyEvent.VK_A, KeyEvent.VK_D));
            Axis.LEFT_STICK_Y.set(pad, scale * direction(keys, KeyEvent.VK_W, KeyEvent.VK_S));
            Axis.RIGHT_STICK_X.set(pad, scale * direction(keys, KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT));
            Axis.RIGHT_STICK_Y.set(pad, scale * direction(keys, KeyEvent.VK_UP, KeyEvent.VK_DOWN));
            apply(pad, keys, DRIVER_BUTTONS, DRIVER_TRIGGERS);
        };
    }

    /** Gamepad 2 (operator), read from {@code keys}. Its sticks stay centered. */
    public static GamepadSource operator(HeldKeys keys) {
        return (pad, sinceInit, sinceStart) -> {
            for (Axis stick : List.of(Axis.LEFT_STICK_X, Axis.LEFT_STICK_Y, Axis.RIGHT_STICK_X, Axis.RIGHT_STICK_Y)) {
                stick.set(pad, 0.0);
            }
            apply(pad, keys, OPERATOR_BUTTONS, OPERATOR_TRIGGERS);
        };
    }

    /**
     * True if the live drive uses {@code keyCode}: a gamepad key, Shift, or the window's PLAY
     * and STOP keys. While driving, these keys go to the robot instead of the playback controls.
     */
    public static boolean isUsed(int keyCode) {
        return keyCode == KeyEvent.VK_SHIFT || keyCode == PLAY_KEY || keyCode == STOP_KEY
                || List.of(KeyEvent.VK_W, KeyEvent.VK_A, KeyEvent.VK_S, KeyEvent.VK_D,
                        KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, KeyEvent.VK_UP, KeyEvent.VK_DOWN).contains(keyCode)
                || DRIVER_BUTTONS.containsValue(keyCode) || DRIVER_TRIGGERS.containsValue(keyCode)
                || OPERATOR_BUTTONS.containsValue(keyCode) || OPERATOR_TRIGGERS.containsValue(keyCode);
    }

    /** The layout as text, for the side panel while driving. */
    public static List<String> help() {
        return List.of(
                "KEYBOARD DRIVING",
                "  Enter = PLAY   Esc = STOP and save",
                "Gamepad 1 (driver)",
                "  Left stick   W A S D",
                "  Right stick  arrow keys",
                "  Shift        sticks at 40%",
                "  A B X Y      F G R T",
                "  LB / RB      Q / E",
                "  LT / RT      Z / C",
                "  D-pad        1 2 3 4 (up down left right)",
                "Gamepad 2 (operator)",
                "  A B X Y      J K U I",
                "  LB / RB      O / P",
                "  LT / RT      N / M",
                "  D-pad        7 8 9 0 (up down left right)");
    }

    /** -1 if only {@code negativeKey} is held, +1 if only {@code positiveKey}, else 0. */
    private static double direction(HeldKeys keys, int negativeKey, int positiveKey) {
        return (keys.isHeld(positiveKey) ? 1.0 : 0.0) - (keys.isHeld(negativeKey) ? 1.0 : 0.0);
    }

    /** Sets every listed button and trigger from the held keys. */
    private static void apply(Gamepad pad, HeldKeys keys, Map<Button, Integer> buttons, Map<Axis, Integer> triggers) {
        for (Button button : Button.values()) {
            Integer key = buttons.get(button);
            button.set(pad, key != null && keys.isHeld(key));
        }
        triggers.forEach((trigger, key) -> trigger.set(pad, keys.isHeld(key) ? 1.0 : 0.0));
    }
}
