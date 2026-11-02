package pvi.harness.physics;

import java.util.Locale;

/**
 * The math that turns four mecanum wheel pushes into the motion of the whole robot.
 *
 * <p>TeamCode's {@code MecanumDriveController} <b>mixes</b> three driver requests into four
 * wheel powers:</p>
 * <pre>
 *   frontLeft  = forward + strafe + rotate
 *   frontRight = forward - strafe - rotate
 *   rearLeft   = forward - strafe + rotate
 *   rearRight  = forward + strafe - rotate
 * </pre>
 * <p>This class does the exact reverse (<b>un-mixes</b>), by adding and subtracting the four
 * wheels so that two of the three requests cancel out each time:</p>
 * <pre>
 *   forward = ( fl + fr + rl + rr) / 4
 *   strafe  = ( fl - fr - rl + rr) / 4      positive = to the robot's right
 *   rotate  = ( fl - fr + rl - rr) / 4      positive = clockwise (seen from above)
 * </pre>
 * <p>Those signs follow TeamCode: stick right means strafe right, right stick right means
 * turn clockwise. They assume the standard mecanum roller layout (rollers form an "X" seen
 * from above), which Team A's Pedro lateral tuning confirms by moving the right way.</p>
 */
public final class MecanumKinematics {
    /** Only static methods here; nothing to create. */
    private MecanumKinematics() {
    }

    /**
     * Robot motion as fractions of full power, in the same terms as TeamCode's drive request.
     *
     * @param forward       +1 = full forward, -1 = full backward
     * @param strafeRight   +1 = full slide to the right, -1 = full left
     * @param turnClockwise +1 = full clockwise turn, -1 = full counter-clockwise
     */
    public record ChassisPower(double forward, double strafeRight, double turnClockwise) {
        /** e.g. {@code ChassisPower[forward=+0.50 strafeRight=+0.00 turnClockwise=+0.00]}. */
        @Override
        public String toString() {
            return String.format(Locale.ROOT, "ChassisPower[forward=%+.2f strafeRight=%+.2f turnClockwise=%+.2f]",
                    forward, strafeRight, turnClockwise);
        }
    }

    /**
     * How fast the robot is moving, measured from the robot's own point of view and using
     * the standard math convention (left and counter-clockwise are positive).
     *
     * @param forwardInchesPerSecond           speed straight ahead (negative = backing up)
     * @param leftInchesPerSecond              speed sideways to the left (negative = to the right)
     * @param counterClockwiseRadiansPerSecond turn rate (negative = turning clockwise)
     */
    public record RobotVelocity(double forwardInchesPerSecond, double leftInchesPerSecond,
                                double counterClockwiseRadiansPerSecond) {

        /** A robot that isn't moving. */
        public static final RobotVelocity STOPPED = new RobotVelocity(0, 0, 0);

        /** Straight-line speed over the ground, ignoring turning, in inches per second. */
        public double speedInchesPerSecond() {
            return Math.hypot(forwardInchesPerSecond, leftInchesPerSecond);
        }
    }

    /** Un-mixes four wheel pushes into forward, strafe and turn (the formulas in the class description). */
    public static ChassisPower toChassisPower(WheelPushes w) {
        double forward = (w.frontLeft() + w.frontRight() + w.rearLeft() + w.rearRight()) / 4.0;
        double strafeRight = (w.frontLeft() - w.frontRight() - w.rearLeft() + w.rearRight()) / 4.0;
        double turnClockwise = (w.frontLeft() - w.frontRight() + w.rearLeft() - w.rearRight()) / 4.0;
        return new ChassisPower(forward, strafeRight, turnClockwise);
    }

    /**
     * Mixes forward, strafe and turn into four wheel pushes with the same formulas as
     * TeamCode's MecanumDriveController (without its "scale down if over 1" step). Used by
     * tests to set up wheel powers for a chosen motion.
     */
    public static WheelPushes toWheelPushes(ChassisPower c) {
        return new WheelPushes(
                c.forward() + c.strafeRight() + c.turnClockwise(),
                c.forward() - c.strafeRight() - c.turnClockwise(),
                c.forward() - c.strafeRight() + c.turnClockwise(),
                c.forward() + c.strafeRight() - c.turnClockwise());
    }

    /**
     * Converts power fractions into real speeds using the robot's drive model, and switches to
     * the standard math convention used for positions (left and counter-clockwise positive).
     * For example, forward 0.5 on a 60 in/s robot is 30 in/s forward.
     */
    public static RobotVelocity toVelocity(ChassisPower power, DriveModel model) {
        return new RobotVelocity(
                power.forward() * model.forwardInchesPerSecond(),
                // Right is negative "left".
                -power.strafeRight() * model.strafeInchesPerSecond(),
                // Clockwise is negative "counter-clockwise".
                -power.turnClockwise() * model.turnRadiansPerSecond());
    }
}
