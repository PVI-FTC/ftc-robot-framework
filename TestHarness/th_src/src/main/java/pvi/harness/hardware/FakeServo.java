package pvi.harness.hardware;

import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.hardware.ServoController;

/**
 * A servo that records the position robot code asks for.
 *
 * <p>{@link #getPosition()} returns what the code set, as on the robot (a standard servo
 * gives no feedback). {@link #getCommandedPwmPosition()} shows where the servo would really
 * go after direction and {@code scaleRange} are applied.</p>
 *
 * <p>It implements the SDK's {@code Servo} interface, so TeamCode can't tell it from a real
 * servo. Add one with {@code hardwareMap.addServo("claw")}.</p>
 */
public class FakeServo implements Servo {
    /** Configuration name, e.g. "claw". */
    private final String name;
    /** Port number on the (imaginary) hub; only used for display. */
    private final int port;
    /** FORWARD or REVERSE, as set by the code. */
    private Direction direction = Direction.FORWARD;
    /** Last position the code set (0..1). NaN means "never set", like a servo that was never commanded. */
    private double position = Double.NaN;
    /** Lower end of the range set by scaleRange(); 0 by default. */
    private double scaleMin = MIN_POSITION;
    /** Upper end of the range set by scaleRange(); 1 by default. */
    private double scaleMax = MAX_POSITION;

    /**
     * @param name configuration name the robot code looks up
     * @param port hub port number, for display only
     */
    public FakeServo(String name, int port) {
        this.name = name;
        this.port = port;
    }

    /** The configuration name this servo was added under. */
    public String getName() {
        return name;
    }

    /** True once robot code has set a position. */
    public boolean hasBeenCommanded() {
        return !Double.isNaN(position);
    }

    /** The 0..1 position actually sent to the servo after direction and range scaling. */
    public double getCommandedPwmPosition() {
        // REVERSE mirrors the position (0.2 becomes 0.8)...
        double p = direction == Direction.REVERSE ? MAX_POSITION - position : position;
        // ...then scaleRange squeezes 0..1 into scaleMin..scaleMax.
        return scaleMin + p * (scaleMax - scaleMin);
    }

    /** No hub exists in the harness, so there is no controller object. */
    @Override public ServoController getController() { return null; }

    /** The port number given when the servo was added. */
    @Override public int getPortNumber() { return port; }

    /** Records FORWARD or REVERSE. */
    @Override public void setDirection(Direction direction) { this.direction = direction; }

    /** The direction the code last set. */
    @Override public Direction getDirection() { return direction; }

    /** Records the position, clamped to 0..1 exactly as the real SDK does. */
    @Override
    public void setPosition(double position) {
        this.position = Math.max(MIN_POSITION, Math.min(MAX_POSITION, position));
    }

    /** The position the code last set (NaN if never set). */
    @Override public double getPosition() { return position; }

    /** Limits the servo's travel to part of its range, with the same rules as the SDK. */
    @Override
    public void scaleRange(double min, double max) {
        if (min < MIN_POSITION || max > MAX_POSITION || min >= max) {
            throw new IllegalArgumentException("scaleRange needs 0 <= min < max <= 1");
        }
        scaleMin = min;
        scaleMax = max;
    }

    /** Not a real brand; reported as "Other". */
    @Override public Manufacturer getManufacturer() { return Manufacturer.Other; }

    /** Shown wherever the SDK lists devices. */
    @Override public String getDeviceName() { return "Harness fake servo"; }

    /** Describes where the device is connected (here, an imaginary port). */
    @Override public String getConnectionInfo() { return "harness servo port " + port; }

    /** Firmware version; fixed for the fake. */
    @Override public int getVersion() { return 1; }

    /** The SDK calls this between OpModes; nothing to reset in the fake. */
    @Override public void resetDeviceConfigurationForOpMode() { }

    /** Nothing to release: the fake holds no hardware connection. */
    @Override public void close() { }

    /** Readable summary for test failure messages. */
    @Override
    public String toString() {
        return String.format("FakeServo[%s position=%.3f dir=%s]", name, position, direction);
    }
}
