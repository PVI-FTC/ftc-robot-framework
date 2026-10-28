package pvi.harness.hardware;

/** Steps every {@link SimulatedSensor} on a fake robot. The runner calls it once per loop. */
public final class SimulatedSensors {

    /** Only static methods here; nothing to create. */
    private SimulatedSensors() {
    }

    /** Calls {@link SimulatedSensor#step} on each simulated sensor, in the order they were added. */
    public static void stepAll(FakeHardwareMap hardwareMap, SimulatedSensor.Step now) {
        for (String name : hardwareMap.deviceNames()) {
            SimulatedSensor sensor = hardwareMap.tryGet(SimulatedSensor.class, name);
            if (sensor != null) {
                sensor.step(now);
            }
        }
    }
}
