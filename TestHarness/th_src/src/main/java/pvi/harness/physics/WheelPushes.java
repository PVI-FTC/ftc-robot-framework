package pvi.harness.physics;

import java.util.Locale;

/**
 * How hard each mecanum wheel is pushing the robot <b>forward</b>, from -1 (full backward)
 * to +1 (full forward), after motor direction settings and wiring are taken into account.
 *
 * <p>This is what the robot would physically do, which can differ from the power the code
 * asked for (for example if a motor is miswired).</p>
 */
public record WheelPushes(double frontLeft, double frontRight, double rearLeft, double rearRight) {

    public static final WheelPushes STOPPED = new WheelPushes(0, 0, 0, 0);

    /** True when every wheel pushes forward (all greater than 0). */
    public boolean allForward() {
        return frontLeft > 0 && frontRight > 0 && rearLeft > 0 && rearRight > 0;
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "WheelPushes[FL=%+.2f FR=%+.2f RL=%+.2f RR=%+.2f]",
                frontLeft, frontRight, rearLeft, rearRight);
    }
}
