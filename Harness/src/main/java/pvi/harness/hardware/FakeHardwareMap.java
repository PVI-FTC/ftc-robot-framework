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
    /** A short name for this configuration, e.g. "teamA", shown in run recordings. */
    private String configName = "custom";
    /** Port number for the next motor added (display only). */
    private int nextMotorPort;
    /** Port number for the next servo added (display only). */
    private int nextServoPort;

    /** The hub's battery voltage reading, which every real robot has. */
    private final FakeVoltageSensor batteryVoltage = new FakeVoltageSensor();

    /**
     * A hardware map with no configured devices, only the Control Hub's battery voltage reading
     * (as on a real robot). Add devices with addMotor/addServo, or start from RobotConfigs.
     */
    public FakeHardwareMap() {
        // No Android Context or OpMode manager exists on a desktop JVM.
        super(null, null);
        // Listed only in the SDK's voltage sensor list, where hardwareMap.voltageSensor finds it.
        voltageSensor.put("Control Hub", batteryVoltage);
    }

    /** The battery voltage robot code reads; set it to test a tired battery. */
    public FakeVoltageSensor batteryVoltage() {
        return batteryVoltage;
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

    /**
     * Adds a fake goBILDA Pinpoint odometry computer (see {@link FakePinpoint}). Robot code and
     * Pedro find it with {@code hardwareMap.get(GoBildaPinpointDriver.class, name)}.
     */
    public FakePinpoint addPinpoint(String configName) {
        FakePinpoint pinpoint = new FakePinpoint(configName);
        addDevice(configName, pinpoint);
        return pinpoint;
    }

    /** Returns a previously added fake Pinpoint, or null. */
    public FakePinpoint pinpoint(String configName) {
        return tryGet(FakePinpoint.class, configName);
    }

    /**
     * Moves every sensor on for {@code seconds} of simulated time: motor encoders turn with
     * their power, and each Pinpoint moves to {@code robot}, the robot's true pose. The runner
     * calls this after every loop.
     *
     * @param robot   where the robot really is now, or null if position isn't tracked (the
     *                Pinpoints then stay where they are)
     * @param seconds simulated time since the last step
     */
    public void stepSensors(pvi.harness.physics.Pose robot, double seconds) {
        for (List<HardwareDevice> devices : devicesByName.values()) {
            for (HardwareDevice device : devices) {
                if (device instanceof FakeDcMotorEx motor) {
                    motor.step(seconds);
                } else if (device instanceof FakePinpoint pinpoint && robot != null) {
                    pinpoint.step(robot, seconds);
                }
            }
        }
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

    /** A short name for this configuration, e.g. "teamA"; "custom" unless {@link #named} set one. */
    public String configName() {
        return configName;
    }

    /** Gives this configuration a short name, e.g. "teamA", shown in run recordings. */
    public FakeHardwareMap named(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("A robot configuration name must not be empty.");
        }
        this.configName = name;
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
