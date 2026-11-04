package pvi.harness.hardware;

import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver;
import com.qualcomm.robotcore.hardware.I2cDeviceSynchSimple;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.robotcore.external.navigation.Pose2D;
import org.firstinspires.ftc.robotcore.external.navigation.Quaternion;
import org.firstinspires.ftc.robotcore.external.navigation.UnnormalizedAngleUnit;

import java.lang.reflect.Proxy;

import pvi.harness.physics.Pose;

/**
 * A fake goBILDA Pinpoint odometry computer that reports where the harness thinks the robot is.
 *
 * <p>On the robot, the Pinpoint reads two odometry pods and an IMU and works out the robot's
 * position. Here, the harness's position estimate (the {@code PoseEstimator}) plays the part of
 * the real robot: after every loop the runner calls {@link #step(Pose, double)} with the
 * robot's true pose, and this fake turns it into what a correctly set up Pinpoint would
 * report. Pedro's {@code PinpointLocalizer} then reads it exactly as on the robot, so Pedro
 * paths can run in tests.</p>
 *
 * <p>Like the real device:</p>
 * <ul>
 *   <li>getters return the values read at the last {@link #update()}, so code that forgets to
 *       call {@code update()} sees old values, as on the robot;</li>
 *   <li>position is measured from where the robot was at the last {@link #resetPosAndIMU()}
 *       (X forward, Y left, heading counter-clockwise), until {@link #setPosition} moves it;</li>
 *   <li>velocities are in that same frame, and the raw pod counts ({@link #getEncoderX()},
 *       {@link #getEncoderY()}) follow the pod offsets and resolution the code set.</li>
 * </ul>
 *
 * <p>It assumes the pods and directions are set up correctly (the code's settings match the
 * real robot). It doesn't model pod slip, IMU drift or the yaw scalar. The settings the code
 * made can be read back ({@link #xPodOffsetInches()}, {@link #forwardDirection()}, ...) so tests
 * can check them.</p>
 */
public class FakePinpoint extends GoBildaPinpointDriver {
    /** goBILDA's published pod resolutions, in encoder counts per millimetre. */
    static final double SWINGARM_TICKS_PER_MM = 13.26291192;
    static final double FOUR_BAR_TICKS_PER_MM = 19.89436789;
    private static final double MM_PER_INCH = 25.4;

    private final String name;

    // ---- Settings the robot code made ----------------------------------------------------
    /** How far left of center the X (forward) pod is, in inches (setOffsets' first value). */
    private double xPodOffsetInches;
    /** How far forward of center the Y (strafe) pod is, in inches (setOffsets' second value). */
    private double yPodOffsetInches;
    private double ticksPerMm = FOUR_BAR_TICKS_PER_MM;
    private EncoderDirection forwardDirection = EncoderDirection.FORWARD;
    private EncoderDirection strafeDirection = EncoderDirection.FORWARD;
    private double yawScalar = 1.0;

    // ---- What the "real robot" is doing (set by the harness every loop) ------------------
    /** The robot's true pose at the last step, or null before the first. */
    private Pose truth;
    /** True pose at which the Pinpoint frame was last set, and what it reported there. */
    private Pose frameTruth;
    private Pose frameReported = Pose.ORIGIN;
    /** Live values, as the device itself would have them right now. */
    private Pose live = Pose.ORIGIN;
    private double liveVelX;
    private double liveVelY;
    private double liveHeadingVel;
    /** Heading without wrapping at ±180°, as getHeading(UnnormalizedAngleUnit) gives it. */
    private double liveTotalHeading;
    private double xPodTravelMm;
    private double yPodTravelMm;

    // ---- Values read at the last update() --------------------------------------------------
    private Pose read = Pose.ORIGIN;
    private double readVelX;
    private double readVelY;
    private double readHeadingVel;
    private double readTotalHeading;
    private int readEncoderX;
    private int readEncoderY;
    private int updateCount;

    /** A fake Pinpoint with this configuration name (usually "pinpoint"). */
    public FakePinpoint(String name) {
        super(noI2c(), false);
        this.name = name;
    }

    // ---- Harness side ------------------------------------------------------------------------

    /**
     * Moves the fake to the robot's new true pose. Called by the runner after every loop.
     *
     * @param robot   where the robot really is now (field frame, inches, radians)
     * @param seconds simulated time since the previous step (for velocities)
     */
    public void step(Pose robot, double seconds) {
        if (truth == null || frameTruth == null) {
            truth = robot;
            frameTruth = robot;
            live = frameReported;
            return;
        }
        Pose before = truth;
        truth = robot;

        // Change in the robot's own frame (forward, left, turn) since the last step.
        double dxField = robot.xInches() - before.xInches();
        double dyField = robot.yInches() - before.yInches();
        double dHeading = angleDifference(robot.headingRadians(), before.headingRadians());
        double midHeading = before.headingRadians() + dHeading / 2.0;
        double forward = dxField * Math.cos(midHeading) + dyField * Math.sin(midHeading);
        double left = -dxField * Math.sin(midHeading) + dyField * Math.cos(midHeading);

        // Each pod rolls with the robot, plus the turn times its distance from the center.
        xPodTravelMm += (forward - xPodOffsetInches * dHeading) * MM_PER_INCH;
        yPodTravelMm += (left + yPodOffsetInches * dHeading) * MM_PER_INCH;

        live = toPinpointFrame(robot);
        liveTotalHeading += dHeading;
        if (seconds > 0) {
            // Velocities in the Pinpoint's frame (turned by the same amount as its position).
            double turn = frameReported.headingRadians() - frameTruth.headingRadians();
            double vx = dxField / seconds;
            double vy = dyField / seconds;
            liveVelX = vx * Math.cos(turn) - vy * Math.sin(turn);
            liveVelY = vx * Math.sin(turn) + vy * Math.cos(turn);
            liveHeadingVel = dHeading / seconds;
        }
    }

    /** Configuration name, e.g. "pinpoint". */
    public String name() { return name; }
    /** How many times the robot code called update(). */
    public int updateCount() { return updateCount; }
    /** The X (forward) pod's sideways offset the code set, in inches (left is positive). */
    public double xPodOffsetInches() { return xPodOffsetInches; }
    /** The Y (strafe) pod's forward offset the code set, in inches (forward is positive). */
    public double yPodOffsetInches() { return yPodOffsetInches; }
    /** The forward pod direction the code set. */
    public EncoderDirection forwardDirection() { return forwardDirection; }
    /** The strafe pod direction the code set. */
    public EncoderDirection strafeDirection() { return strafeDirection; }
    /** The pod resolution the code set, in counts per millimetre. */
    public double ticksPerMm() { return ticksPerMm; }

    // ---- The Pinpoint API robot code uses ------------------------------------------------

    /** Reads the latest values, like the real device's bulk read. */
    @Override
    public void update() {
        read = live;
        readVelX = liveVelX;
        readVelY = liveVelY;
        readHeadingVel = liveHeadingVel;
        readTotalHeading = liveTotalHeading;
        readEncoderX = (int) Math.round(xPodTravelMm * ticksPerMm * sign(forwardDirection));
        readEncoderY = (int) Math.round(yPodTravelMm * ticksPerMm * sign(strafeDirection));
        updateCount++;
    }

    /** Same as {@link #update()}: the fake always reads everything. */
    @Override
    public void update(ReadData data) {
        update();
    }

    @Override
    public void setOffsets(double xOffset, double yOffset, DistanceUnit unit) {
        xPodOffsetInches = unit.toInches(xOffset);
        yPodOffsetInches = unit.toInches(yOffset);
    }

    @Override
    public void setEncoderResolution(GoBildaOdometryPods pods) {
        ticksPerMm = pods == GoBildaOdometryPods.goBILDA_SWINGARM_POD ? SWINGARM_TICKS_PER_MM : FOUR_BAR_TICKS_PER_MM;
    }

    @Override
    public void setEncoderResolution(double ticksPerUnit, DistanceUnit unit) {
        ticksPerMm = ticksPerUnit / unit.toMm(1.0);
    }

    @Override
    public void setEncoderDirections(EncoderDirection xEncoder, EncoderDirection yEncoder) {
        forwardDirection = xEncoder;
        strafeDirection = yEncoder;
    }

    @Override
    public void setYawScalar(double yawScalar) {
        this.yawScalar = yawScalar;
    }

    @Override
    public float getYawScalar() {
        return (float) yawScalar;
    }

    /** Zeroes position and heading where the robot is now, and the pod counts. */
    @Override
    public void resetPosAndIMU() {
        setFrame(Pose.ORIGIN);
        xPodTravelMm = 0;
        yPodTravelMm = 0;
        update();
    }

    /** The IMU calibration takes no time here, so this does nothing. */
    @Override
    public void recalibrateIMU() {
    }

    @Override
    public void setPosition(Pose2D pos) {
        setFrame(new Pose(pos.getX(DistanceUnit.INCH), pos.getY(DistanceUnit.INCH),
                pos.getHeading(AngleUnit.RADIANS)));
    }

    @Override
    public void setPosX(double posX, DistanceUnit unit) {
        setFrame(new Pose(unit.toInches(posX), live.yInches(), live.headingRadians()));
    }

    @Override
    public void setPosY(double posY, DistanceUnit unit) {
        setFrame(new Pose(live.xInches(), unit.toInches(posY), live.headingRadians()));
    }

    @Override
    public void setHeading(double heading, AngleUnit unit) {
        setFrame(new Pose(live.xInches(), live.yInches(), unit.toRadians(heading)));
    }

    @Override public Pose2D getPosition() {
        return new Pose2D(DistanceUnit.INCH, read.xInches(), read.yInches(), AngleUnit.RADIANS, read.headingRadians());
    }
    @Override public double getPosX(DistanceUnit unit) { return unit.fromInches(read.xInches()); }
    @Override public double getPosY(DistanceUnit unit) { return unit.fromInches(read.yInches()); }
    @Override public double getHeading(AngleUnit unit) { return unit.fromRadians(read.headingRadians()); }
    @Override public double getHeading(UnnormalizedAngleUnit unit) { return unit.fromRadians(readTotalHeading); }
    @Override public double getVelX(DistanceUnit unit) { return unit.fromInches(readVelX); }
    @Override public double getVelY(DistanceUnit unit) { return unit.fromInches(readVelY); }
    @Override public double getHeadingVelocity(UnnormalizedAngleUnit unit) { return unit.fromRadians(readHeadingVel); }
    @Override public int getEncoderX() { return readEncoderX; }
    @Override public int getEncoderY() { return readEncoderY; }
    @Override public float getXOffset(DistanceUnit unit) { return (float) unit.fromInches(xPodOffsetInches); }
    @Override public float getYOffset(DistanceUnit unit) { return (float) unit.fromInches(yPodOffsetInches); }
    @Override public DeviceStatus getDeviceStatus() { return DeviceStatus.READY; }
    @Override public int getDeviceID() { return 2; }
    @Override public int getDeviceVersion() { return 1; }
    @Override public int getLoopTime() { return 667; }      // microseconds, about 1500 Hz like the real one
    @Override public double getFrequency() { return 1500.0; }
    @Override public Quaternion getQuaternion() { return new Quaternion(1, 0, 0, 0, 0); }
    @Override public double getPitch(AngleUnit unit) { return 0.0; }
    @Override public double getRoll(AngleUnit unit) { return 0.0; }
    @Override public void setBulkReadScope(Register... registers) { }
    @Override public void setErrorDetectionType(ErrorDetectionType type) { }
    @Override protected synchronized boolean doInitialize() { return true; }
    @Override public Manufacturer getManufacturer() { return Manufacturer.Other; }
    @Override public String getDeviceName() { return "goBILDA Pinpoint (harness fake)"; }
    @Override public String toString() { return "FakePinpoint(" + name + ")"; }

    // ---- Internals ---------------------------------------------------------------------------

    /** Makes the Pinpoint report {@code reported} where the robot is now. */
    private void setFrame(Pose reported) {
        frameTruth = truth;
        frameReported = reported;
        live = reported;
        read = reported;
        // The unwrapped heading restarts from the new heading.
        liveTotalHeading = reported.headingRadians();
        readTotalHeading = liveTotalHeading;
    }

    /** The robot's true pose, as this Pinpoint reports it. */
    private Pose toPinpointFrame(Pose robot) {
        if (frameTruth == null) {
            return frameReported;
        }
        // Where the robot is relative to frameTruth, in frameTruth's own directions...
        double dx = robot.xInches() - frameTruth.xInches();
        double dy = robot.yInches() - frameTruth.yInches();
        double c = Math.cos(-frameTruth.headingRadians());
        double s = Math.sin(-frameTruth.headingRadians());
        double localX = dx * c - dy * s;
        double localY = dx * s + dy * c;
        // ...then placed at frameReported.
        double c2 = Math.cos(frameReported.headingRadians());
        double s2 = Math.sin(frameReported.headingRadians());
        return new Pose(frameReported.xInches() + localX * c2 - localY * s2,
                frameReported.yInches() + localX * s2 + localY * c2,
                frameReported.headingRadians() + angleDifference(robot.headingRadians(), frameTruth.headingRadians()));
    }

    private static double sign(EncoderDirection direction) {
        return direction == EncoderDirection.REVERSED ? -1.0 : 1.0;
    }

    /** a - b, as the smallest turn (-π to π). */
    private static double angleDifference(double a, double b) {
        return Math.atan2(Math.sin(a - b), Math.cos(a - b));
    }

    /**
     * A stand-in I2C connection. The real driver's constructor sets an address on it; nothing
     * else ever reaches it, because every method that would talk to the chip is replaced above.
     */
    private static I2cDeviceSynchSimple noI2c() {
        return (I2cDeviceSynchSimple) Proxy.newProxyInstance(FakePinpoint.class.getClassLoader(),
                new Class<?>[]{I2cDeviceSynchSimple.class}, (proxy, method, args) -> {
                    Class<?> type = method.getReturnType();
                    if (method.getName().equals("toString")) {
                        return "harness fake I2C";
                    }
                    if (type == boolean.class) {
                        return false;
                    }
                    if (type == int.class || type == short.class || type == byte.class || type == long.class) {
                        return type == long.class ? 0L : type == short.class ? (short) 0 : type == byte.class ? (byte) 0 : 0;
                    }
                    if (type == float.class || type == double.class) {
                        return type == float.class ? 0f : 0d;
                    }
                    if (type == byte[].class) {
                        return new byte[0];
                    }
                    return null;
                });
    }
}
