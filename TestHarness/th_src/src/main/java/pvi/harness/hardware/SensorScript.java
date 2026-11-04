package pvi.harness.hardware;

import java.util.ArrayList;
import java.util.List;

/**
 * A timeline of what the world shows the robot's sensors, the way {@code GamepadScript} scripts
 * the driver: "the distance sensor sees a wall 30 in away, then 8 in at 2 s", "the touch sensor
 * is pressed from 3 s", "the color sensor sees red". Devices are named by their configuration
 * names.
 *
 * <pre>{@code
 * SensorScript world = new SensorScript()
 *         .duringInit().at(0).distanceInches("frontDistance", 30)
 *         .afterStart().at(2.0).distanceInches("frontDistance", 8)
 *         .at(3.0).press("armLimit")
 *         .at(4.0).color("intakeColor", 0.8, 0.1, 0.1);
 * robot.sensorScript(world);
 * }</pre>
 *
 * <p>Times are seconds since INIT ({@link #duringInit()}) or since PLAY ({@link #afterStart()},
 * the default). Each value holds until the script changes it. Only world-driven sensors use the
 * script; ones the robot's motion decides (IMU yaw, dead wheels, OTOS) follow the physics
 * instead. It is plain data, so a future Unity scene could supply the same values.</p>
 */
public class SensorScript {
    /** Which clock an entry's time is on. */
    private enum Period { INIT, START }

    /** One change: from {@code seconds} on, {@code device} reads {@code value}. */
    private record Entry(Period period, double seconds, String device, Object value) {
    }

    /** A color as a REV color sensor reports it after normalizing: each part 0..1. */
    public record Color(double red, double green, double blue, double alpha) {
        public Color {
            for (double part : new double[] {red, green, blue, alpha}) {
                if (!(part >= 0 && part <= 1)) {
                    throw new IllegalArgumentException("Color parts must be between 0 and 1, got " + part);
                }
            }
        }
    }

    /** Every change, in the order the test wrote them. */
    private final List<Entry> entries = new ArrayList<>();
    private Period period = Period.START;
    private double seconds;

    /** The times that follow are seconds since INIT. */
    public SensorScript duringInit() {
        period = Period.INIT;
        seconds = 0;
        return this;
    }

    /** The times that follow are seconds since PLAY (the default). */
    public SensorScript afterStart() {
        period = Period.START;
        seconds = 0;
        return this;
    }

    /** The changes that follow happen at {@code seconds}. */
    public SensorScript at(double seconds) {
        if (!(seconds >= 0)) {
            throw new IllegalArgumentException("Script times must be 0 or more seconds, got " + seconds);
        }
        this.seconds = seconds;
        return this;
    }

    /** A distance sensor (or a color sensor's distance part) reads {@code inches}. */
    public SensorScript distanceInches(String device, double inches) {
        if (!(inches >= 0)) {
            throw new IllegalArgumentException("A distance must be 0 or more inches, got " + inches);
        }
        return add(device, inches);
    }

    /** A distance sensor sees nothing in range again (it reads {@code DistanceUnit.infinity}). */
    public SensorScript outOfRange(String device) {
        return add(device, Double.POSITIVE_INFINITY);
    }

    /** A touch sensor (or digital input) is pressed. */
    public SensorScript press(String device) {
        return add(device, Boolean.TRUE);
    }

    /** A touch sensor (or digital input) is released. */
    public SensorScript release(String device) {
        return add(device, Boolean.FALSE);
    }

    /** A color sensor sees this color (each part 0..1, fully lit). */
    public SensorScript color(String device, double red, double green, double blue) {
        return add(device, new Color(red, green, blue, 1.0));
    }

    /** An analog input reads {@code volts} (0 to 3.3 V on a REV hub). */
    public SensorScript volts(String device, double volts) {
        if (!(volts >= 0 && volts <= 3.3)) {
            throw new IllegalArgumentException("A REV hub's analog inputs read 0 to 3.3 V, got " + volts);
        }
        return add(device, volts);
    }

    /** Any other scripted value, for fakes that need their own kind (for example Limelight results). */
    public SensorScript set(String device, Object value) {
        return add(device, value);
    }

    private SensorScript add(String device, Object value) {
        if (device == null || device.isBlank()) {
            throw new IllegalArgumentException("A scripted sensor needs its configuration name.");
        }
        entries.add(new Entry(period, seconds, device.trim(), value));
        return this;
    }

    /**
     * What {@code device} reads now, of the given kind ({@code Double.class} for a distance or
     * volts, {@code Boolean.class} for pressed, {@link Color} for a color), or null if the script
     * hasn't set one yet. Each kind is kept apart, so one color sensor can have both a color and
     * a distance.
     *
     * @param sinceInitSeconds  simulated seconds since INIT
     * @param sinceStartSeconds simulated seconds since PLAY, or negative before PLAY
     */
    public <T> T valueAt(String device, Class<T> kind, double sinceInitSeconds, double sinceStartSeconds) {
        T value = null;
        double bestInit = -1;
        double bestStart = -1;
        // INIT entries come before every START entry; within each, the latest due one wins
        // (later lines win a tie, as written).
        for (Entry entry : entries) {
            if (!entry.device().equals(device) || !kind.isInstance(entry.value())) {
                continue;
            }
            if (entry.period() == Period.INIT && entry.seconds() <= sinceInitSeconds + 1e-9
                    && bestStart < 0 && entry.seconds() >= bestInit) {
                bestInit = entry.seconds();
                value = kind.cast(entry.value());
            } else if (entry.period() == Period.START && sinceStartSeconds >= 0
                    && entry.seconds() <= sinceStartSeconds + 1e-9 && entry.seconds() >= bestStart) {
                bestStart = entry.seconds();
                value = kind.cast(entry.value());
            }
        }
        return value;
    }
}
