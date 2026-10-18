package pvi.harness.ds;

import com.qualcomm.robotcore.hardware.Gamepad;

/**
 * Analog gamepad inputs.
 *
 * <p>Remember the FTC convention: pushing a stick <b>up</b> gives a <b>negative</b> Y value.</p>
 */
public enum Axis {
    LEFT_STICK_X, LEFT_STICK_Y, RIGHT_STICK_X, RIGHT_STICK_Y, LEFT_TRIGGER, RIGHT_TRIGGER;

    /** Writes this axis on a gamepad, clamped to its legal range (sticks -1..1, triggers 0..1). */
    void set(Gamepad pad, double value) {
        boolean trigger = this == LEFT_TRIGGER || this == RIGHT_TRIGGER;
        float clamped = (float) Math.max(trigger ? 0.0 : -1.0, Math.min(1.0, value));
        switch (this) {
            case LEFT_STICK_X: pad.left_stick_x = clamped; break;
            case LEFT_STICK_Y: pad.left_stick_y = clamped; break;
            case RIGHT_STICK_X: pad.right_stick_x = clamped; break;
            case RIGHT_STICK_Y: pad.right_stick_y = clamped; break;
            case LEFT_TRIGGER: pad.left_trigger = clamped; break;
            case RIGHT_TRIGGER: pad.right_trigger = clamped; break;
            default: throw new IllegalStateException("Unhandled axis " + this);
        }
    }
}
