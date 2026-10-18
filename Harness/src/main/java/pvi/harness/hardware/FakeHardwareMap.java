package pvi.harness.hardware;

import com.qualcomm.robotcore.hardware.HardwareDevice;
import com.qualcomm.robotcore.hardware.HardwareMap;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The real FTC {@link HardwareMap}, filled with fake devices instead of a Control Hub.
 *
 * <p>Robot code still calls the SDK's own {@code get(Class, String)}, so a misspelled
 * or missing device name throws the same {@code IllegalArgumentException} and message
 * as on the robot. Only {@link #tryGet(Class, String)} is replaced: the SDK version
 * asks Android whether it is running on a REV Control Hub, which cannot work off-robot.</p>
 */
public class FakeHardwareMap extends HardwareMap {
    /**
     * Every fake device, by configuration name, in the order added. A list per name because
     * the SDK allows one name to hold more than one device type.
     */
    private final Map<String, List<HardwareDevice>> devicesByName = new LinkedHashMap<>();
    /** Port number for the next motor added (display only). */
    private int nextMotorPort;
    /** Port number for the next servo added (display only). */
    private int nextServoPort;

    /** An empty hardware map. Add devices with addMotor/addServo, or start from RobotConfigs. */
    public FakeHardwareMap() {
        // No Android Context or OpMode manager exists on a desktop JVM.
        super(null, null);
    }

    /** Adds a fake motor under its configuration name and returns it so tests can inspect it. */
    public FakeDcMotorEx addMotor(String configName) {
        FakeDcMotorEx motor = new FakeDcMotorEx(configName, nextMotorPort++);
        addDevice(configName, motor);
        // Also list it in the SDK's per-type map, which older code reads as hardwareMap.dcMotor.get(name).
        dcMotor.put(configName, motor);
        return motor;
    }

    /** Adds a fake servo under its configuration name and returns it so tests can inspect it. */
    public FakeServo addServo(String configName) {
        FakeServo fakeServo = new FakeServo(configName, nextServoPort++);
        addDevice(configName, fakeServo);
        // Also list it in the SDK's per-type map (hardwareMap.servo.get(name)).
        servo.put(configName, fakeServo);
        return fakeServo;
    }

    /** Returns a previously added fake servo. */
    public FakeServo servo(String configName) {
        return tryGet(FakeServo.class, configName);
    }

    /** Removes a device, for example to test how robot code copes with a missing part. */
    public FakeHardwareMap without(String configName) {
        List<HardwareDevice> removed = devicesByName.remove(configName.trim());
        if (removed != null) {
            // Remove it from the SDK's own lists too, so no lookup path can still find it.
            for (HardwareDevice device : removed) {
                remove(configName, device);
            }
            dcMotor.remove(configName);
            servo.remove(configName);
        }
        return this;
    }

    /**
     * Swaps a motor's power wires (see {@link FakeDcMotorEx#setMiswired(boolean)}), to test
     * what a wiring mistake would do to the robot.
     */
    public FakeHardwareMap miswire(String motorName) {
        FakeDcMotorEx motor = motor(motorName);
        if (motor == null) {
            throw new IllegalArgumentException("Cannot miswire \"" + motorName + "\": no such motor. Motors: "
                    + deviceNames());
        }
        motor.setMiswired(true);
        return this;
    }

    /** Configuration names of every device, in the order they were added. */
    public java.util.Set<String> deviceNames() {
        return java.util.Collections.unmodifiableSet(devicesByName.keySet());
    }

    /** Registers any fake device under a configuration name. */
    public void addDevice(String configName, HardwareDevice device) {
        // Our own lookup list (used by tryGet)...
        devicesByName.computeIfAbsent(configName.trim(), name -> new ArrayList<>()).add(device);
        // ...and the SDK's, so iterating over the hardware map also finds it.
        put(configName, device);
    }

    /** Returns a previously added fake motor. */
    public FakeDcMotorEx motor(String configName) {
        return tryGet(FakeDcMotorEx.class, configName);
    }

    /** Same rule as the SDK: the first device with this name that is the requested type, or null. */
    @Override
    public <T> T tryGet(Class<? extends T> classOrInterface, String deviceName) {
        List<HardwareDevice> candidates = devicesByName.get(deviceName.trim());
        if (candidates == null) {
            return null;
        }
        for (HardwareDevice device : candidates) {
            if (classOrInterface.isInstance(device)) {
                return classOrInterface.cast(device);
            }
        }
        return null;
    }
}
