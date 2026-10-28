package pvi.harness.hardware;

import com.qualcomm.robotcore.hardware.AnalogInput;
import com.qualcomm.robotcore.hardware.AnalogInputController;

/**
 * An analog input on a REV hub (the SDK's {@code AnalogInput}), such as a potentiometer or an
 * analog distance sensor, reading the volts the {@link SensorScript} says:
 * {@code script.volts("armPot", 1.65)}. 0 V until the script sets a value; the hub's maximum
 * is 3.3 V. {@link #setVoltage} sets a reading directly, for quick tests.
 *
 * <p>Add one with {@code hardwareMap.addAnalogInput("armPot")}.</p>
 */
public class FakeAnalogInput extends AnalogInput implements SimulatedSensor {
    /** A REV hub's analog inputs read 0 to 3.3 V. */
    public static final double MAX_VOLTS = 3.3;

    private final String name;
    private double volts;

    public FakeAnalogInput(String name) {
        super(FakeDevice.inert(AnalogInputController.class), 0);
        this.name = name.trim();
    }

    /** The configuration name this input was added under. */
    public String getName() {
        return name;
    }

    /** Sets the reading directly (0 to 3.3 V). */
    public FakeAnalogInput setVoltage(double volts) {
        this.volts = Math.max(0, Math.min(MAX_VOLTS, volts));
        return this;
    }

    @Override
    public void step(Step now) {
        Double scripted = now.script().valueAt(name, Double.class, now.sinceInitSeconds(), now.sinceStartSeconds());
        if (scripted != null) {
            volts = scripted;
        }
    }

    @Override public double getVoltage() { return volts; }
    @Override public double getMaxVoltage() { return MAX_VOLTS; }
    @Override public Manufacturer getManufacturer() { return Manufacturer.Other; }
    @Override public String getDeviceName() { return "FakeAnalogInput (harness)"; }
    @Override public String getConnectionInfo() { return "harness: " + name; }
}
