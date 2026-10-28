package pvi.harness.hardware;

import com.qualcomm.robotcore.hardware.IMU;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.AngularVelocity;
import org.firstinspires.ftc.robotcore.external.navigation.AxesOrder;
import org.firstinspires.ftc.robotcore.external.navigation.AxesReference;
import org.firstinspires.ftc.robotcore.external.navigation.Orientation;
import org.firstinspires.ftc.robotcore.external.navigation.Quaternion;
import org.firstinspires.ftc.robotcore.external.navigation.YawPitchRollAngles;

import pvi.harness.agent.SimTime;

/**
 * The Control Hub's built-in IMU (the SDK's {@code IMU} interface, as on a BHI260AP or BNO055),
 * reading the robot's real turning in the simulation.
 *
 * <p>Yaw is the robot's heading, counter-clockwise positive, measured from where the robot
 * pointed when the IMU first saw it (or the last {@code resetYaw()}), like a real IMU after
 * power-on. Pitch and roll are always 0: the simulated field is flat. The yaw rate comes from
 * the heading change in each loop.</p>
 *
 * <p>For tests of imperfect IMUs, {@link #setDriftDegreesPerSecond} adds a slow drift and
 * {@link #setYawOffsetDegrees} a fixed error. When the harness isn't tracking the robot's
 * position (no drive motors), yaw stays 0 plus any drift and offset.</p>
 *
 * <p>Add one with {@code hardwareMap.addImu("imu")}.</p>
 */
public class FakeImu extends FakeDevice implements IMU, SimulatedSensor {
    /** The robot's true heading, in radians, at the latest step (NaN before the first). */
    private double headingRadians = Double.NaN;
    /** The true heading that counts as yaw 0. */
    private double zeroHeadingRadians;
    /** Yaw rate over the last step, radians per second (counter-clockwise positive). */
    private double yawRateRadiansPerSecond;
    /** Added drift so far, radians. */
    private double driftRadians;
    private double driftDegreesPerSecond;
    private double yawOffsetDegrees;
    /** The settings robot code last passed to initialize(), or null. */
    private Parameters parameters;

    public FakeImu(String name) {
        super(name);
    }

    /** Adds a slow drift to the yaw, as cheaper IMUs have (degrees per second; 0 = none). */
    public FakeImu setDriftDegreesPerSecond(double degreesPerSecond) {
        this.driftDegreesPerSecond = degreesPerSecond;
        return this;
    }

    /** Adds a fixed error to the yaw (degrees). */
    public FakeImu setYawOffsetDegrees(double degrees) {
        this.yawOffsetDegrees = degrees;
        return this;
    }

    /** The settings robot code passed to {@code initialize(...)}, or null if it never did. */
    public Parameters initializedWith() {
        return parameters;
    }

    @Override
    public void step(Step now) {
        double heading = now.robot() == null ? 0.0 : now.robot().headingRadians();
        if (Double.isNaN(headingRadians)) {
            zeroHeadingRadians = heading;   // like power-on: wherever the robot points is 0
            yawRateRadiansPerSecond = 0;
        } else if (now.loopSeconds() > 0) {
            yawRateRadiansPerSecond = wrap(heading - headingRadians) / now.loopSeconds();
        }
        headingRadians = heading;
        driftRadians += Math.toRadians(driftDegreesPerSecond) * now.loopSeconds();
    }

    /** Yaw in radians, counter-clockwise positive, between -pi and +pi. */
    private double yawRadians() {
        double heading = Double.isNaN(headingRadians) ? 0.0 : headingRadians;
        return wrap(heading - zeroHeadingRadians + driftRadians + Math.toRadians(yawOffsetDegrees));
    }

    private static double wrap(double radians) {
        return Math.IEEEremainder(radians, 2 * Math.PI);
    }

    // ---- IMU: what the SDK asks ---------------------------------------------------------------

    /** Accepts the hub's mounting settings. The simulated robot is flat, so they don't change the readings. */
    @Override
    public boolean initialize(Parameters parameters) {
        this.parameters = parameters;
        return true;
    }

    /** Makes the current heading yaw 0. */
    @Override
    public void resetYaw() {
        zeroHeadingRadians = Double.isNaN(headingRadians) ? 0.0 : headingRadians;
        driftRadians = 0;
        yawOffsetDegrees = 0;
    }

    @Override
    public YawPitchRollAngles getRobotYawPitchRollAngles() {
        return new YawPitchRollAngles(AngleUnit.DEGREES, Math.toDegrees(yawRadians()), 0.0, 0.0, SimTime.nanoTime());
    }

    @Override
    public Orientation getRobotOrientation(AxesReference reference, AxesOrder order, AngleUnit angleUnit) {
        return getRobotOrientationAsQuaternion().toOrientation(reference, order, angleUnit);
    }

    /** A turn of {@code yaw} about the vertical axis. */
    @Override
    public Quaternion getRobotOrientationAsQuaternion() {
        double half = yawRadians() / 2;
        return new Quaternion((float) Math.cos(half), 0f, 0f, (float) Math.sin(half), SimTime.nanoTime());
    }

    @Override
    public AngularVelocity getRobotAngularVelocity(AngleUnit angleUnit) {
        double rate = yawRateRadiansPerSecond + Math.toRadians(driftDegreesPerSecond);
        float z = (float) (angleUnit == AngleUnit.DEGREES ? Math.toDegrees(rate) : rate);
        return new AngularVelocity(angleUnit, 0f, 0f, z, SimTime.nanoTime());
    }
}
