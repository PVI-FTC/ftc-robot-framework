package pvi.harness.hardware;

import com.qualcomm.robotcore.hardware.DistanceSensor;

import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;

/**
 * A distance sensor (the SDK's {@code DistanceSensor}, as on the REV 2 m sensor), reading what
 * the {@link SensorScript} says: {@code script.distanceInches("frontDistance", 12)}.
 *
 * <p>Until the script sets a distance, or after {@code outOfRange(...)}, it sees nothing in
 * range and reads {@code DistanceUnit.infinity}, as the SDK documents for an out-of-range
 * reading. {@link #setDistanceInches} sets a reading directly, for quick tests.</p>
 *
 * <p>Add one with {@code hardwareMap.addDistanceSensor("frontDistance")}.</p>
 */
public class FakeDistanceSensor extends FakeDevice implements DistanceSensor, SimulatedSensor {
    /** The current reading in inches; infinite means nothing in range. */
    private double inches = Double.POSITIVE_INFINITY;

    public FakeDistanceSensor(String name) {
        super(name);
    }

    /** Sets the reading directly (inches; use {@code Double.POSITIVE_INFINITY} for nothing in range). */
    public FakeDistanceSensor setDistanceInches(double inches) {
        this.inches = inches;
        return this;
    }

    @Override
    public void step(Step now) {
        Double value = now.script().valueAt(getName(), Double.class, now.sinceInitSeconds(), now.sinceStartSeconds());
        if (value != null) {
            inches = value;
        }
    }

    @Override
    public double getDistance(DistanceUnit unit) {
        return readDistance(inches, unit);
    }

    /** A distance in inches as the SDK would report it in {@code unit}. */
    static double readDistance(double inches, DistanceUnit unit) {
        if (Double.isInfinite(inches)) {
            return DistanceUnit.infinity;
        }
        return unit.fromInches(inches);
    }
}
