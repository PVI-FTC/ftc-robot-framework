package pvi.harness.hardware;

import pvi.harness.physics.Pose;

/**
 * A fake sensor that the runner moves on every loop: the IMU follows the robot's turning, a
 * distance sensor follows the {@link SensorScript}, and so on. {@link SimulatedSensors#stepAll}
 * calls {@link #step} for each one before the OpMode runs.
 */
public interface SimulatedSensor {

    /**
     * Everything a fake sensor may need to know about "now", as plain data.
     *
     * @param robot             where the robot really is (FTC field frame), or null when the
     *                          harness isn't tracking its position
     * @param sinceInitSeconds  simulated seconds since INIT
     * @param sinceStartSeconds simulated seconds since PLAY, or -1 before PLAY
     * @param loopSeconds       simulated seconds since the last step (0 on the first)
     * @param script            what the world shows the sensors (never null; may be empty)
     */
    record Step(Pose robot, double sinceInitSeconds, double sinceStartSeconds, double loopSeconds,
                SensorScript script) {
    }

    /** Moves this sensor on to {@code now}. */
    void step(Step now);
}
