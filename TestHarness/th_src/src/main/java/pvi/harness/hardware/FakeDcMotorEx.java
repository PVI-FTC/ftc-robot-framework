package pvi.harness.hardware;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorController;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.PIDCoefficients;
import com.qualcomm.robotcore.hardware.PIDFCoefficients;
import com.qualcomm.robotcore.hardware.configuration.typecontainers.MotorConfigurationType;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit;

/**
 * A motor that remembers everything robot code tells it.
 *
 * <p>Power, direction, run mode and zero-power behavior are stored exactly as the
 * robot code set them, so tests can check them.</p>
 *
 * <p><b>Encoder.</b> After every loop the runner calls {@link #step(double)}, which moves the
 * encoder the way the motor would turn: speed is proportional to power (like the position
 * estimator), {@link #FULL_SPEED_TICKS_PER_SECOND} at full power. As on the robot, positive power
 * gives rising counts whichever {@link Direction} is set, RUN_TO_POSITION drives toward the
 * target and stops there, {@code setVelocity()} in RUN_USING_ENCODER runs at that speed, and
 * STOP_AND_RESET_ENCODER zeroes it. A miswired motor counts the opposite way to its power.
 * The motor's numbers are placeholders (a goBILDA 5203 312 RPM motor) until the team gives
 * real ones; change them per motor with {@link #setEncoderModel(double, double)}.</p>
 *
 * <p>It implements the SDK's {@code DcMotorEx} interface, so TeamCode can't tell it apart
 * from a real motor: {@code hardwareMap.get(DcMotorEx.class, "frontLeft")} returns one of
 * these during tests.</p>
 */
public class FakeDcMotorEx implements DcMotorEx {
    /** Configuration name, e.g. "frontLeft". */
    private final String name;
    /** Port number on the (imaginary) hub; only used for display. */
    private final int port;

    /** Last power the code set, clamped to -1..1 like the real SDK. */
    private double power;
    /** FORWARD or REVERSE, as set by the code. Real motors start FORWARD. */
    private Direction direction = Direction.FORWARD;
    /** Run mode (e.g. RUN_WITHOUT_ENCODER) as set by the code. */
    private RunMode mode = RunMode.RUN_WITHOUT_ENCODER;
    /** BRAKE or FLOAT; UNKNOWN until the code chooses, like a freshly configured motor. */
    private ZeroPowerBehavior zeroPowerBehavior = ZeroPowerBehavior.UNKNOWN;
    /** False after setMotorDisable(): the hub cuts power to the motor. */
    private boolean enabled = true;
    /** Target for RUN_TO_POSITION mode, in encoder ticks. */
    private int targetPosition;
    /** How close (in ticks) counts as "at the target" for isBusy(). The SDK default is 10. */
    private int targetPositionTolerance = 10;
    /**
     * Placeholder until measured: encoder counts per output-shaft turn of a goBILDA 5203 series
     * 19.2:1 (312 RPM) motor, the usual FTC drive motor.
     */
    public static final double PLACEHOLDER_TICKS_PER_REV = 537.7;
    /** Placeholder until measured: that motor's free speed, in RPM. */
    public static final double PLACEHOLDER_FREE_SPEED_RPM = 312.0;
    /** Encoder speed at full power with the placeholder motor: about 2796 ticks per second. */
    public static final double FULL_SPEED_TICKS_PER_SECOND = PLACEHOLDER_TICKS_PER_REV * PLACEHOLDER_FREE_SPEED_RPM / 60.0;

    /** Encoder counts per output-shaft turn (for setVelocity/getVelocity with an AngleUnit). */
    private double ticksPerRev = PLACEHOLDER_TICKS_PER_REV;
    /** Encoder speed at full power, in ticks per second. */
    private double fullSpeedTicksPerSecond = FULL_SPEED_TICKS_PER_SECOND;
    /** Encoder reading in ticks, kept exact so slow motors still count up. */
    private double exactPosition;
    /** Encoder reading in ticks, as getCurrentPosition() reports it. */
    private int currentPosition;
    /** Encoder speed in ticks per second over the last step. */
    private double velocityTicksPerSecond;
    /** Last value passed to setVelocity(), in ticks per second, kept so tests can check it. */
    private double requestedVelocity;
    /** True when the last command was setVelocity() rather than setPower(). */
    private boolean velocityCommanded;
    /** The motor type robot code last set (Pedro sets one), or null until asked for. */
    private MotorConfigurationType motorType;
    /** Over-current warning level the code set, in amps. */
    private double currentAlertAmps = 0;
    /** How many times the code called setPower(). */
    private int setPowerCalls;
    /** True to simulate swapped power wires (see setMiswired). */
    private boolean miswired;
    /**
     * How fast the shaft is really turning, as a fraction of full speed in the FORWARD sense,
     * when the harness's physics knows (drive wheels); null to work it out from the power.
     */
    private Double physicalSpeed;

    /**
     * @param name configuration name the robot code looks up, e.g. "frontLeft"
     * @param port hub port number, for display only
     */
    public FakeDcMotorEx(String name, int port) {
        this.name = name;
        this.port = port;
    }

    // ---- Test helpers (not part of the SDK interface) ----------------------

    /** The configuration name this motor was added under. */
    public String getName() {
        return name;
    }

    /** How many times robot code called setPower(). Useful for "did anything command this?" checks. */
    public int getSetPowerCallCount() {
        return setPowerCalls;
    }

    /**
     * Simulates the motor's two power wires being swapped, so it physically spins the opposite
     * way. Robot code cannot tell: {@link #getPower()} still returns what the code set. Only
     * the physics model (e.g. the position estimator) sees the effect.
     */
    public void setMiswired(boolean miswired) {
        this.miswired = miswired;
    }

    /** True when this motor's wires are simulated as swapped. */
    public boolean isMiswired() {
        return miswired;
    }

    /** Sets what the encoder reports right now, for tests that need a starting reading. */
    public void simulateEncoder(int positionTicks, double velocityTicksPerSecond) {
        this.exactPosition = positionTicks;
        this.currentPosition = positionTicks;
        this.velocityTicksPerSecond = velocityTicksPerSecond;
    }

    /**
     * Changes this motor's numbers from the placeholders.
     *
     * @param ticksPerRev  encoder counts per output-shaft turn
     * @param freeSpeedRpm output-shaft speed at full power, in RPM
     */
    public FakeDcMotorEx setEncoderModel(double ticksPerRev, double freeSpeedRpm) {
        if (!(ticksPerRev > 0) || !(freeSpeedRpm > 0)) {
            throw new IllegalArgumentException("Ticks per turn and free speed must be positive.");
        }
        this.ticksPerRev = ticksPerRev;
        this.fullSpeedTicksPerSecond = ticksPerRev * freeSpeedRpm / 60.0;
        return this;
    }

    /** Encoder speed at full power, in ticks per second. */
    public double fullSpeedTicksPerSecond() {
        return fullSpeedTicksPerSecond;
    }

    /**
     * Turns the motor for {@code seconds} of simulated time and updates the encoder. Called by
     * the runner after every loop (see the class comment for the rules).
     */
    /**
     * Sets how fast the shaft is really turning (a fraction of full speed, positive = the way a
     * FORWARD motor turns on positive power), or null to go back to working it out from the
     * power. The harness's physics sets this for drive motors, so their encoders follow the
     * wheels' real motion (momentum, walls) instead of the power.
     */
    public void setPhysicalSpeed(Double fractionOfFullSpeed) {
        this.physicalSpeed = fractionOfFullSpeed;
    }

    public void step(double seconds) {
        double speed;
        if (physicalSpeed != null && mode != RunMode.STOP_AND_RESET_ENCODER && mode != RunMode.RUN_TO_POSITION) {
            // The physics says how the wheel really turned; the encoder reports it in the
            // code's direction (REVERSE counts the other way), whatever the wiring.
            speed = physicalSpeed * fullSpeedTicksPerSecond * (direction == Direction.REVERSE ? -1 : 1);
            exactPosition += speed * seconds;
            currentPosition = (int) Math.round(exactPosition);
            velocityTicksPerSecond = speed;
            return;
        }
        if (!enabled || mode == RunMode.STOP_AND_RESET_ENCODER) {
            speed = 0.0;
        } else if (mode == RunMode.RUN_TO_POSITION) {
            // Drive toward the target at |power|, and stop on it.
            double toGo = targetPosition - exactPosition;
            double maxStep = Math.abs(power) * fullSpeedTicksPerSecond * seconds;
            double move = Math.max(-maxStep, Math.min(maxStep, toGo));
            speed = seconds > 0 ? move / seconds : 0.0;
        } else if (velocityCommanded && mode == RunMode.RUN_USING_ENCODER) {
            speed = Math.max(-fullSpeedTicksPerSecond, Math.min(fullSpeedTicksPerSecond, requestedVelocity));
        } else {
            speed = power * fullSpeedTicksPerSecond;
        }
        if (miswired && mode != RunMode.RUN_TO_POSITION) {
            speed = -speed;   // the motor turns the other way; the encoder wires weren't swapped
        }
        exactPosition += speed * seconds;
        currentPosition = (int) Math.round(exactPosition);
        velocityTicksPerSecond = speed;
    }

    // ---- DcMotorSimple: the basic motor methods ----------------------------

    /** Records FORWARD or REVERSE. DriveHardware sets the left motors to REVERSE. */
    @Override public void setDirection(Direction direction) { this.direction = direction; }

    /** The direction the code last set. */
    @Override public Direction getDirection() { return direction; }

    /** Records the power, clamped to -1..1 exactly as the real SDK does, and counts the call. */
    @Override
    public void setPower(double power) {
        setPowerCalls++;
        this.power = Math.max(-1.0, Math.min(1.0, power));
        velocityCommanded = false;
    }

    /** The power the code last set (not affected by miswiring; the code can't see that). */
    @Override public double getPower() { return power; }

    // ---- DcMotor: encoder and configuration methods -------------------------

    /** A plain motor type with the placeholder counts per turn and RPM (Pedro reads and copies it). */
    @Override public MotorConfigurationType getMotorType() {
        if (motorType == null) {
            // Built directly: the SDK's getUnspecifiedMotorType() needs Android's configuration
            // manager, which can't start on a desktop JVM. Pedro reads and copies this.
            motorType = new MotorConfigurationType();
            motorType.setTicksPerRev(ticksPerRev);
            motorType.setMaxRPM(fullSpeedTicksPerSecond * 60.0 / ticksPerRev);
        }
        return motorType;
    }

    /** Ignored: the fake has no motor-type-specific behavior. */
    @Override public void setMotorType(MotorConfigurationType motorType) { this.motorType = motorType; }

    /** No hub exists in the harness, so there is no controller object. */
    @Override public DcMotorController getController() { return null; }

    /** The port number given when the motor was added. */
    @Override public int getPortNumber() { return port; }

    /** Records BRAKE or FLOAT, so tests can check what the code chose. */
    @Override public void setZeroPowerBehavior(ZeroPowerBehavior behavior) { this.zeroPowerBehavior = behavior; }

    /** The zero-power behavior the code last set. */
    @Override public ZeroPowerBehavior getZeroPowerBehavior() { return zeroPowerBehavior; }

    /** Old SDK method: same as choosing FLOAT and setting power to 0. */
    @Override
    @Deprecated
    public void setPowerFloat() {
        zeroPowerBehavior = ZeroPowerBehavior.FLOAT;
        setPower(0.0);
    }

    /** True when the motor is set to FLOAT and has no power, matching the SDK's meaning. */
    @Override public boolean getPowerFloat() { return zeroPowerBehavior == ZeroPowerBehavior.FLOAT && power == 0.0; }

    /** Records the RUN_TO_POSITION target, in encoder ticks. */
    @Override public void setTargetPosition(int position) { this.targetPosition = position; }

    /** The RUN_TO_POSITION target the code last set. */
    @Override public int getTargetPosition() { return targetPosition; }

    /** True while running to a position and not yet within the tolerance, like the real motor. */
    @Override
    public boolean isBusy() {
        return mode == RunMode.RUN_TO_POSITION
                && Math.abs(targetPosition - currentPosition) > targetPositionTolerance;
    }

    /** The simulated encoder reading, in ticks. */
    @Override public int getCurrentPosition() { return currentPosition; }

    /** Records the run mode. STOP_AND_RESET_ENCODER also zeroes the encoder and power, as on the robot. */
    @Override
    public void setMode(RunMode mode) {
        if (mode == RunMode.STOP_AND_RESET_ENCODER) {
            exactPosition = 0;
            currentPosition = 0;
            velocityTicksPerSecond = 0;
            power = 0;
        }
        this.mode = mode;
    }

    /** The run mode the code last set. */
    @Override public RunMode getMode() { return mode; }

    // ---- DcMotorEx: extended motor methods ----------------------------------

    /** Turns the motor back on after setMotorDisable(). */
    @Override public void setMotorEnable() { enabled = true; }

    /** Cuts power to the motor; the position estimator then treats its wheel as not pushing. */
    @Override public void setMotorDisable() { enabled = false; }

    /** False after setMotorDisable(). */
    @Override public boolean isMotorEnabled() { return enabled; }

    /** Records a velocity request (ticks per second). The fake does not run a velocity controller. */
    @Override public void setVelocity(double angularRate) {
        requestedVelocity = angularRate;
        velocityCommanded = true;
    }

    /** Records a velocity request in other units. The unit is not converted in the fake. */
    @Override public void setVelocity(double angularRate, AngleUnit unit) {
        // Converted by hand: AngleUnit's own conversions wrap to ±180°, which is wrong for speeds.
        double degreesPerSecond = unit == AngleUnit.RADIANS ? Math.toDegrees(angularRate) : angularRate;
        setVelocity(degreesPerSecond / 360.0 * ticksPerRev);
    }

    /** The simulated encoder speed in ticks per second. */
    @Override public double getVelocity() { return velocityTicksPerSecond; }

    /** The simulated encoder speed, as an angle per second of the output shaft. */
    @Override public double getVelocity(AngleUnit unit) {
        double degreesPerSecond = velocityTicksPerSecond / ticksPerRev * 360.0;
        return unit == AngleUnit.RADIANS ? Math.toRadians(degreesPerSecond) : degreesPerSecond;
    }

    /** The last value passed to setVelocity(), for tests. */
    public double getRequestedVelocity() { return requestedVelocity; }

    /** Ignored: the fake has no built-in motor controller to tune. */
    @Override public void setPIDCoefficients(RunMode mode, PIDCoefficients pidCoefficients) { }

    /** Ignored: the fake has no built-in motor controller to tune. */
    @Override public void setPIDFCoefficients(RunMode mode, PIDFCoefficients pidfCoefficients) { }

    /** Ignored: the fake has no built-in motor controller to tune. */
    @Override public void setVelocityPIDFCoefficients(double p, double i, double d, double f) { }

    /** Ignored: the fake has no built-in motor controller to tune. */
    @Override public void setPositionPIDFCoefficients(double p) { }

    /** Returns all-zero coefficients, since nothing was tuned. */
    @Override public PIDCoefficients getPIDCoefficients(RunMode mode) { return new PIDCoefficients(); }

    /** Returns all-zero coefficients, since nothing was tuned. */
    @Override public PIDFCoefficients getPIDFCoefficients(RunMode mode) { return new PIDFCoefficients(); }

    /** Records how close counts as "at the target" for isBusy(). */
    @Override public void setTargetPositionTolerance(int tolerance) { targetPositionTolerance = tolerance; }

    /** The tolerance used by isBusy(). */
    @Override public int getTargetPositionTolerance() { return targetPositionTolerance; }

    /** No current is simulated; always 0. */
    @Override public double getCurrent(CurrentUnit unit) { return 0; }

    /** The over-current warning level the code set. */
    @Override public double getCurrentAlert(CurrentUnit unit) { return currentAlertAmps; }

    /** Records the over-current warning level. */
    @Override public void setCurrentAlert(double current, CurrentUnit unit) { currentAlertAmps = current; }

    /** No current is simulated, so the motor is never over current. */
    @Override public boolean isOverCurrent() { return false; }

    // ---- HardwareDevice: identity methods every FTC device has ---------------

    /** Not a real brand; reported as "Other". */
    @Override public Manufacturer getManufacturer() { return Manufacturer.Other; }

    /** Shown wherever the SDK lists devices. */
    @Override public String getDeviceName() { return "Harness fake motor"; }

    /** Describes where the device is connected (here, an imaginary port). */
    @Override public String getConnectionInfo() { return "harness port " + port; }

    /** Firmware version; fixed for the fake. */
    @Override public int getVersion() { return 1; }

    /** The SDK calls this between OpModes; nothing to reset in the fake. */
    @Override public void resetDeviceConfigurationForOpMode() { }

    /** Nothing to release: the fake holds no hardware connection. */
    @Override public void close() { }

    /** Readable summary for test failure messages, e.g. {@code FakeDcMotorEx[frontLeft power=0.500 ...]}. */
    @Override
    public String toString() {
        return String.format("FakeDcMotorEx[%s power=%.3f dir=%s mode=%s zero=%s]",
                name, power, direction, mode, zeroPowerBehavior);
    }
}
