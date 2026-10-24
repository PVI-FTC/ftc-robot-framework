package pvi.harness.hardware;

import com.qualcomm.robotcore.hardware.VoltageSensor;

/**
 * The battery voltage reading every Control Hub has.
 *
 * <p>Robot code (and Pedro, for voltage compensation) reads it with
 * {@code hardwareMap.voltageSensor.iterator().next().getVoltage()}. It reports a charged
 * battery, {@link #DEFAULT_VOLTS}, unless a test sets another value.</p>
 */
public class FakeVoltageSensor implements VoltageSensor {
    /** A freshly charged 12 V FTC battery under light load. */
    public static final double DEFAULT_VOLTS = 12.5;

    private double volts = DEFAULT_VOLTS;

    /** Sets the voltage robot code will read, for example 11.0 to test a tired battery. */
    public void setVoltage(double volts) {
        this.volts = volts;
    }

    @Override public double getVoltage() { return volts; }
    @Override public Manufacturer getManufacturer() { return Manufacturer.Other; }
    @Override public String getDeviceName() { return "Control Hub battery voltage (harness fake)"; }
    @Override public String getConnectionInfo() { return "harness"; }
    @Override public int getVersion() { return 1; }
    @Override public void resetDeviceConfigurationForOpMode() { }
    @Override public void close() { }
}
