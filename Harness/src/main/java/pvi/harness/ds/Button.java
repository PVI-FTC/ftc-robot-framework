package pvi.harness.ds;

import com.qualcomm.robotcore.hardware.Gamepad;

/** Gamepad buttons a script can press. Names follow the Logitech/Xbox labels used in FTC code. */
public enum Button {
    A, B, X, Y,
    DPAD_UP, DPAD_DOWN, DPAD_LEFT, DPAD_RIGHT,
    LEFT_BUMPER, RIGHT_BUMPER,
    LEFT_STICK_BUTTON, RIGHT_STICK_BUTTON,
    START, BACK, GUIDE, TOUCHPAD;

    /** True if this button is held down on {@code pad}. */
    public boolean isPressed(Gamepad pad) {
        switch (this) {
            case A: return pad.a;
            case B: return pad.b;
            case X: return pad.x;
            case Y: return pad.y;
            case DPAD_UP: return pad.dpad_up;
            case DPAD_DOWN: return pad.dpad_down;
            case DPAD_LEFT: return pad.dpad_left;
            case DPAD_RIGHT: return pad.dpad_right;
            case LEFT_BUMPER: return pad.left_bumper;
            case RIGHT_BUMPER: return pad.right_bumper;
            case LEFT_STICK_BUTTON: return pad.left_stick_button;
            case RIGHT_STICK_BUTTON: return pad.right_stick_button;
            case START: return pad.start;
            case BACK: return pad.back;
            case GUIDE: return pad.guide;
            case TOUCHPAD: return pad.touchpad;
            default: throw new IllegalStateException("Unhandled button " + this);
        }
    }

    /** Sets this button on a gamepad to pressed (true) or released (false). */
    void set(Gamepad pad, boolean pressed) {
        switch (this) {
            case A: pad.a = pressed; break;
            case B: pad.b = pressed; break;
            case X: pad.x = pressed; break;
            case Y: pad.y = pressed; break;
            case DPAD_UP: pad.dpad_up = pressed; break;
            case DPAD_DOWN: pad.dpad_down = pressed; break;
            case DPAD_LEFT: pad.dpad_left = pressed; break;
            case DPAD_RIGHT: pad.dpad_right = pressed; break;
            case LEFT_BUMPER: pad.left_bumper = pressed; break;
            case RIGHT_BUMPER: pad.right_bumper = pressed; break;
            case LEFT_STICK_BUTTON: pad.left_stick_button = pressed; break;
            case RIGHT_STICK_BUTTON: pad.right_stick_button = pressed; break;
            case START: pad.start = pressed; break;
            case BACK: pad.back = pressed; break;
            case GUIDE: pad.guide = pressed; break;
            case TOUCHPAD: pad.touchpad = pressed; break;
            default: throw new IllegalStateException("Unhandled button " + this);
        }
    }
}
