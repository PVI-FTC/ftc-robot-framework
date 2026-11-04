package pvi.harness.hardware;

import com.qualcomm.robotcore.hardware.VoltageSensor;

/**
 * The battery voltage reading every Control Hub has.
 *
 * <p>Robot code (and Pedro, for voltage compensation) reads it with
 * {@code hardwareMap.voltageSensor.iterator().next().getVoltage()}. It reports a charged
 * battery, {@link #DEFAULT_VOLTS}, unless a test sets another value.</p>
 *
 * <p>The battery can also <b>sag</b> over a match ({@link #setSag}): the voltage then drops
 * steadily with simulated time, as a real battery does under a match's load. The harness's
 * physics reads the same voltage, so a tired battery really makes the simulated robot slower.</p>
 */
public class FakeVoltageSensor implements VoltageSensor {
    /** A freshly charged 12 V FTC battery under light load. */
    public static final double DEFAULT_VOLTS = 12.5;

    /** The lowest a sagging battery goes; below this the robot's hub would brown out anyway. */
    public static final double LOWEST_VOLTS = 9.0;

    private double volts = DEFAULT_VOLTS;
    /** How fast the voltage drops, in volts per minute of simulated time (0 = no sag). */
    private double sagVoltsPerMinute;
    /** Simulated seconds since the sag started. */
    private double sagSeconds;

    /** Sets the voltage robot code will read, for example 11.0 to test a tired battery. */
    public void setVoltage(double volts) {
        this.volts = volts;
    }

    /**
     * Makes the voltage drop by {@code voltsPerMinute} for every minute of simulated time from
     * now (0 turns sag off), never below {@link #LOWEST_VOLTS}. A 2:00 TeleOp on a battery that
     * sags 0.5 V per minute ends 1 V lower than it started.
     */
    public void setSag(double voltsPerMinute) {
        if (voltsPerMinute < 0) {
            throw new IllegalArgumentException("Sag must be 0 or more volts per minute, got " + voltsPerMinute);
        }
        this.sagVoltsPerMinute = voltsPerMinute;
        this.sagSeconds = 0;
    }

    /** Moves simulated time on (the runner does this every loop), so a sagging battery drops. */
    public void step(double seconds) {
        if (seconds > 0) {
            sagSeconds += seconds;
        }
    }

    @Override public double getVoltage() {
        return Math.max(LOWEST_VOLTS, volts - sagVoltsPerMinute * sagSeconds / 60.0);
    }
    @Override public Manufacturer getManufacturer() { return Manufacturer.Other; }
    @Override public String getDeviceName() { return "Control Hub battery voltage (harness fake)"; }
    @Override public String getConnectionInfo() { return "harness"; }
    @Override public int getVersion() { return 1; }
    @Override public void resetDeviceConfigurationForOpMode() { }
    @Override public void close() { }
}
