package pvi.harness.hardware;

import com.qualcomm.robotcore.hardware.ColorSensor;
import com.qualcomm.robotcore.hardware.DistanceSensor;
import com.qualcomm.robotcore.hardware.I2cAddr;
import com.qualcomm.robotcore.hardware.NormalizedColorSensor;
import com.qualcomm.robotcore.hardware.NormalizedRGBA;

import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;

/**
 * A color sensor (as on the REV Color Sensor V3), seeing what the {@link SensorScript} says:
 * {@code script.color("intakeColor", 0.8, 0.1, 0.1)} for a red game piece.
 *
 * <p>Robot code can read it the three ways the REV sensor is read: as a
 * {@code NormalizedColorSensor} ({@code getNormalizedColors()}, each part 0..1, multiplied by
 * the gain and capped at 1), as a {@code ColorSensor} ({@code red()}, {@code green()},
 * {@code blue()} as 16-bit counts), or as a {@code DistanceSensor} for its short-range distance
 * part ({@code script.distanceInches("intakeColor", 1.5)} with the same name).</p>
 *
 * <p>Until the script sets a color it sees black (all 0), like a sensor with nothing in front
 * of it. {@link #setColor} sets one directly, for quick tests.</p>
 *
 * <p>Add one with {@code hardwareMap.addColorSensor("intakeColor")}.</p>
 */
public class FakeColorSensor extends FakeDevice
        implements NormalizedColorSensor, ColorSensor, DistanceSensor, SimulatedSensor {
    /** Full scale of the sensor's raw counts (16 bits). */
    private static final int MAX_COUNT = 65535;

    private SensorScript.Color color = new SensorScript.Color(0, 0, 0, 0);
    private double distanceInches = Double.POSITIVE_INFINITY;
    private float gain = 1.0f;
    private boolean ledOn = true;
    private I2cAddr address = I2cAddr.create7bit(0x52);   // the REV V3's address

    public FakeColorSensor(String name) {
        super(name);
    }

    /** Sets the color seen directly (each part 0..1). */
    public FakeColorSensor setColor(double red, double green, double blue) {
        this.color = new SensorScript.Color(red, green, blue, 1.0);
        return this;
    }

    /** True if robot code left the sensor's light on (the default). */
    public boolean isLedOn() {
        return ledOn;
    }

    @Override
    public void step(Step now) {
        SensorScript.Color seen = now.script().valueAt(getName(), SensorScript.Color.class,
                now.sinceInitSeconds(), now.sinceStartSeconds());
        if (seen != null) {
            color = seen;
        }
        Double inches = now.script().valueAt(getName(), Double.class, now.sinceInitSeconds(), now.sinceStartSeconds());
        if (inches != null) {
            distanceInches = inches;
        }
    }

    /** One color part after the gain, 0..1. */
    private float scaled(double part) {
        return (float) Math.min(1.0, part * gain);
    }

    // ---- NormalizedColorSensor ---------------------------------------------------------------

    @Override
    public NormalizedRGBA getNormalizedColors() {
        NormalizedRGBA colors = new NormalizedRGBA();
        colors.red = scaled(color.red());
        colors.green = scaled(color.green());
        colors.blue = scaled(color.blue());
        colors.alpha = scaled(color.alpha());
        return colors;
    }

    @Override public float getGain() { return gain; }

    /** The SDK requires a gain of at least 1. */
    @Override
    public void setGain(float newGain) {
        this.gain = Math.max(1.0f, newGain);
    }

    // ---- ColorSensor: raw 16-bit counts ----------------------------------------------------

    @Override public int red() { return Math.round(scaled(color.red()) * MAX_COUNT); }
    @Override public int green() { return Math.round(scaled(color.green()) * MAX_COUNT); }
    @Override public int blue() { return Math.round(scaled(color.blue()) * MAX_COUNT); }
    @Override public int alpha() { return Math.round(scaled(color.alpha()) * MAX_COUNT); }

    /** The color packed as 0xAARRGGBB, 8 bits each, like the SDK. */
    @Override
    public int argb() {
        return getNormalizedColors().toColor();
    }

    @Override public void enableLed(boolean enable) { this.ledOn = enable; }
    @Override public void setI2cAddress(I2cAddr newAddress) { this.address = newAddress; }
    @Override public I2cAddr getI2cAddress() { return address; }

    // ---- DistanceSensor ----------------------------------------------------------------------

    @Override
    public double getDistance(DistanceUnit unit) {
        return FakeDistanceSensor.readDistance(distanceInches, unit);
    }
}
