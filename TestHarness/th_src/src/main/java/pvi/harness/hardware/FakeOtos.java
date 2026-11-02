package pvi.harness.hardware;

import com.qualcomm.hardware.sparkfun.SparkFunOTOS;
import com.qualcomm.robotcore.hardware.I2cDeviceSynch;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;

import pvi.harness.physics.Pose;

/**
 * A SparkFun Optical Tracking Odometry Sensor (OTOS), reporting the robot's real motion in the
 * simulation, like {@link FakePinpoint} does.
 *
 * <p>It reports in its own frame, as the real sensor does: (0, 0, 0) where tracking started or
 * was last reset, x forward along the robot's heading at that moment, heading counter-clockwise
 * positive. {@code setPosition(...)} moves that frame so the current pose reads as given. Units
 * follow {@code setLinearUnit} / {@code setAngularUnit} (inches and degrees by default), and the
 * linear and angular scalars multiply the distances and turns, as on the real sensor. The
 * mounting offset ({@code setOffset}) is stored and returned but not applied: the harness
 * already reports the robot's center.</p>
 *
 * <p>Readings only: the harness's own position tracking still comes from the drive motors and
 * Pinpoint (mecanum + Pinpoint only, decided 2026-10-25). Before the robot is tracked (no drive
 * motors), it stays at its starting pose.</p>
 *
 * <p>Add one with {@code hardwareMap.addOtos("otos")}.</p>
 */
public class FakeOtos extends SparkFunOTOS implements SimulatedSensor {
    private final String name;
    private DistanceUnit linearUnit = DistanceUnit.INCH;
    private AngleUnit angularUnit = AngleUnit.DEGREES;
    private double linearScalar = 1.0;
    private double angularScalar = 1.0;
    private Pose2D offset = new Pose2D(0, 0, 0);

    /** The robot's true pose at the latest step, or null before the first. */
    private Pose truth;
    /** The true pose and the reported pose (inches, radians) when the frame was last set. */
    private Pose frameTruth;
    private Pose frameReported = Pose.ORIGIN;
    /** Latest reported velocity and acceleration (inches/s, radians/s, in the sensor's frame). */
    private double velX;
    private double velY;
    private double velH;
    private double accX;
    private double accY;
    private double accH;

    public FakeOtos(String name) {
        super(FakeDevice.inert(I2cDeviceSynch.class));
        this.name = name.trim();
    }

    /** The configuration name this sensor was added under. */
    public String getName() {
        return name;
    }

    @Override
    public void step(Step now) {
        Pose robot = now.robot() == null ? (truth == null ? Pose.ORIGIN : truth) : now.robot();
        if (truth == null) {
            truth = robot;
            frameTruth = robot;
            return;
        }
        Pose before = reported();
        double oldVx = velX;
        double oldVy = velY;
        double oldVh = velH;
        truth = robot;
        Pose after = reported();
        if (now.loopSeconds() > 0) {
            velX = (after.xInches() - before.xInches()) / now.loopSeconds();
            velY = (after.yInches() - before.yInches()) / now.loopSeconds();
            velH = Math.IEEEremainder(after.headingRadians() - before.headingRadians(), 2 * Math.PI) / now.loopSeconds();
            accX = (velX - oldVx) / now.loopSeconds();
            accY = (velY - oldVy) / now.loopSeconds();
            accH = (velH - oldVh) / now.loopSeconds();
        }
    }

    /** The current pose in the sensor's frame (inches, radians), with the scalars applied. */
    private Pose reported() {
        if (truth == null) {
            return frameReported;
        }
        double turn = frameReported.headingRadians() - frameTruth.headingRadians();
        double dx = (truth.xInches() - frameTruth.xInches()) * linearScalar;
        double dy = (truth.yInches() - frameTruth.yInches()) * linearScalar;
        double dh = Math.IEEEremainder(truth.headingRadians() - frameTruth.headingRadians(), 2 * Math.PI) * angularScalar;
        return new Pose(frameReported.xInches() + dx * Math.cos(turn) - dy * Math.sin(turn),
                frameReported.yInches() + dx * Math.sin(turn) + dy * Math.cos(turn),
                Math.IEEEremainder(frameReported.headingRadians() + dh, 2 * Math.PI));
    }

    /** Starts a new frame where the current pose reads as {@code pose} (inches, radians). */
    private void setFrame(Pose pose) {
        frameTruth = truth == null ? Pose.ORIGIN : truth;
        frameReported = pose;
        velX = velY = velH = accX = accY = accH = 0;
    }

    private Pose2D out(double xInches, double yInches, double radians) {
        return new Pose2D(linearUnit.fromInches(xInches), linearUnit.fromInches(yInches),
                angularUnit.fromRadians(radians));
    }

    // ---- SparkFunOTOS: what robot code calls ------------------------------------------------

    @Override public boolean begin() { return true; }
    @Override public boolean isConnected() { return true; }
    @Override public boolean selfTest() { return true; }
    @Override public boolean calibrateImu() { return true; }
    @Override public boolean calibrateImu(int numSamples, boolean waitUntilDone) { return true; }
    @Override public int getImuCalibrationProgress() { return 0; }

    @Override public DistanceUnit getLinearUnit() { return linearUnit; }
    @Override public void setLinearUnit(DistanceUnit unit) { this.linearUnit = unit; }
    @Override public AngleUnit getAngularUnit() { return angularUnit; }
    @Override public void setAngularUnit(AngleUnit unit) { this.angularUnit = unit; }

    @Override public double getLinearScalar() { return linearScalar; }
    @Override public double getAngularScalar() { return angularScalar; }

    /** Accepts 0.872 to 1.127, as the real sensor does. */
    @Override
    public boolean setLinearScalar(double scalar) {
        if (scalar < 0.872 || scalar > 1.127) {
            return false;
        }
        Pose now = reported();
        linearScalar = scalar;
        setFrame(now);
        return true;
    }

    /** Accepts 0.872 to 1.127, as the real sensor does. */
    @Override
    public boolean setAngularScalar(double scalar) {
        if (scalar < 0.872 || scalar > 1.127) {
            return false;
        }
        Pose now = reported();
        angularScalar = scalar;
        setFrame(now);
        return true;
    }

    /** Starts tracking again from (0, 0, 0). */
    @Override public void resetTracking() { setFrame(Pose.ORIGIN); }

    @Override public Pose2D getOffset() { return new Pose2D(offset.x, offset.y, offset.h); }
    @Override public void setOffset(Pose2D offset) { this.offset = new Pose2D(offset.x, offset.y, offset.h); }

    @Override
    public Pose2D getPosition() {
        Pose p = reported();
        return out(p.xInches(), p.yInches(), p.headingRadians());
    }

    /** Makes the current pose read as {@code pose} (in the current units). */
    @Override
    public void setPosition(Pose2D pose) {
        setFrame(new Pose(linearUnit.toInches(pose.x), linearUnit.toInches(pose.y),
                Math.IEEEremainder(angularUnit.toRadians(pose.h), 2 * Math.PI)));
    }

    @Override public Pose2D getVelocity() { return out(velX, velY, velH); }
    @Override public Pose2D getAcceleration() { return out(accX, accY, accH); }

    @Override
    public void getPosVelAcc(Pose2D position, Pose2D velocity, Pose2D acceleration) {
        position.set(getPosition());
        velocity.set(getVelocity());
        acceleration.set(getAcceleration());
    }
}
