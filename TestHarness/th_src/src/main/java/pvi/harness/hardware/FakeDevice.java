package pvi.harness.hardware;

import com.qualcomm.robotcore.hardware.HardwareDevice;

/**
 * The parts every SDK device has ({@code getDeviceName()}, {@code close()} and so on), written
 * once for the harness's fake sensors. Each fake adds what its own SDK interface asks for.
 */
abstract class FakeDevice implements HardwareDevice {
    /** Configuration name, e.g. "imu". */
    private final String name;

    FakeDevice(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("A fake device needs its configuration name.");
        }
        this.name = name.trim();
    }

    /** The configuration name this device was added under. */
    public String getName() {
        return name;
    }

    @Override public Manufacturer getManufacturer() { return Manufacturer.Other; }
    @Override public String getDeviceName() { return getClass().getSimpleName() + " (harness)"; }
    @Override public String getConnectionInfo() { return "harness: " + name; }
    @Override public int getVersion() { return 1; }
    @Override public void resetDeviceConfigurationForOpMode() { }
    @Override public void close() { }

    /**
     * A stand-in for an SDK interface (such as an I2C bus or a hub's analog controller) that a
     * fake's SDK parent class needs but never really uses: every method does nothing and returns
     * 0, false or null. Like {@code FakePinpoint}'s fake I2C bus.
     */
    static <T> T inert(Class<T> type) {
        Object proxy = java.lang.reflect.Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type},
                (self, method, args) -> {
                    Class<?> returns = method.getReturnType();
                    if (method.getName().equals("toString")) {
                        return "harness stand-in " + type.getSimpleName();
                    }
                    if (returns == boolean.class) {
                        return false;
                    }
                    if (returns == long.class) {
                        return 0L;
                    }
                    if (returns == int.class) {
                        return 0;
                    }
                    if (returns == short.class) {
                        return (short) 0;
                    }
                    if (returns == byte.class) {
                        return (byte) 0;
                    }
                    if (returns == float.class) {
                        return 0f;
                    }
                    if (returns == double.class) {
                        return 0d;
                    }
                    if (returns == char.class) {
                        return (char) 0;
                    }
                    return null;
                });
        return type.cast(proxy);
    }
}
