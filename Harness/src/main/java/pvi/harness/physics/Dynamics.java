package pvi.harness.physics;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.pedropathing.follower.FollowerConstants;

import org.firstinspires.ftc.teamcode.robots.teamA.TeamAPedroHarnessAccess;

/**
 * How a robot speeds up and slows down: the momentum part of the simulation.
 *
 * <p>A real robot doesn't jump to full speed the moment the code sets full power, and it doesn't
 * stop dead at zero power. The motors pull the robot's speed toward the commanded speed
 * gradually (a motor's response time), the tires can only push so hard before they slip (the
 * traction limit), and with zero power on FLOAT the robot coasts and friction slows it down.</p>
 *
 * @param responseTime     how quickly the motors pull the speed toward the commanded speed:
 *                         after this many seconds, about 63% of the change is done (a DC
 *                         motor's time constant). In seconds.
 * @param maxAcceleration  the most the robot can speed up or slow down before the wheels slip
 *                         (traction limit), in in/s². Turning shares this limit.
 * @param forwardCoast     how fast the robot slows down when coasting forward or backward with
 *                         zero power on FLOAT, in in/s² (a positive number)
 * @param lateralCoast     the same for sideways coasting, in in/s²
 */
public record Dynamics(ModelValue responseTime, ModelValue maxAcceleration,
                       ModelValue forwardCoast, ModelValue lateralCoast) {

    /** Unit label for accelerations. */
    public static final String INCHES_PER_SECOND_SQUARED = "in/s^2";
    /** Unit label for times. */
    public static final String SECONDS = "s";

    /** Checks units and that every value is above zero. */
    public Dynamics {
        check("response time", responseTime, SECONDS);
        check("maximum acceleration", maxAcceleration, INCHES_PER_SECOND_SQUARED);
        check("forward coasting deceleration", forwardCoast, INCHES_PER_SECOND_SQUARED);
        check("sideways coasting deceleration", lateralCoast, INCHES_PER_SECOND_SQUARED);
    }

    /**
     * Team A's robot. The coasting decelerations are read live from Team A's Pedro configuration
     * (Pedro's zero-power acceleration tuners measured them on the real robot), so re-tuning
     * updates the harness. The response time and traction limit are not measured yet: see the
     * calibration checklist in the README (Step 10).
     */
    public static Dynamics teamA() {
        FollowerConstants pedro = TeamAPedroHarnessAccess.recordedFollowerConstants();
        return new Dynamics(
                ModelValue.placeholder(0.10, SECONDS,
                        "not measured; typical for goBILDA 312 rpm drive motors. Measure: time from full"
                                + " power to 63% of full speed"),
                ModelValue.placeholder(200.0, INCHES_PER_SECOND_SQUARED,
                        "not measured; about half of gravity, a usual traction limit for mecanum wheels on"
                                + " foam tiles. Measure: fastest speed-up from rest at full power"),
                ModelValue.measured(Math.abs(pedro.forwardZeroPowerAcceleration), INCHES_PER_SECOND_SQUARED,
                        "Pedro forwardZeroPowerAcceleration in TeamAPedroConfiguration (Forward Zero Power"
                                + " Acceleration Tuner)"),
                ModelValue.measured(Math.abs(pedro.lateralZeroPowerAcceleration), INCHES_PER_SECOND_SQUARED,
                        "Pedro lateralZeroPowerAcceleration in TeamAPedroConfiguration (Lateral Zero Power"
                                + " Acceleration Tuner)"));
    }

    /** Names of values that are still placeholders. */
    public List<String> unmeasured() {
        List<String> names = new ArrayList<>();
        if (!responseTime.measured()) names.add("response time");
        if (!maxAcceleration.measured()) names.add("maximum acceleration");
        if (!forwardCoast.measured()) names.add("forward coasting");
        if (!lateralCoast.measured()) names.add("sideways coasting");
        return names;
    }

    /** A few lines for reports. */
    public String describe() {
        return String.format(Locale.ROOT,
                "  response time  %s%n"
                        + "  max accel      %s%n"
                        + "  forward coast  %s%n"
                        + "  sideways coast %s",
                responseTime, maxAcceleration, forwardCoast, lateralCoast);
    }

    private static void check(String what, ModelValue value, String unit) {
        if (value == null) {
            throw new IllegalArgumentException("Dynamics need a " + what + ".");
        }
        if (!unit.equals(value.unit())) {
            throw new IllegalArgumentException("The " + what + " must be in " + unit + ", got " + value.unit() + ".");
        }
        if (value.value() <= 0) {
            throw new IllegalArgumentException("The " + what + " must be greater than 0, got " + value.value() + ".");
        }
    }
}
