package org.firstinspires.ftc.teamcode.core.robot;

/**
 * A robot feature that has a predictable lifecycle managed by a {@link Robot}.
 *
 * <p>A subsystem owns one focused responsibility. The robot calls {@link #initialize()} once,
 * {@link #update()} once during each FTC loop, and {@link #stop()} when the robot stops.</p>
 */
public interface Subsystem {
    /**
     * Prepares this subsystem for use. Robot calls this method once before updates begin.
     */
    void initialize();

    /**
     * Performs this subsystem's work for one FTC loop.
     */
    void update();

    /**
     * Stops this subsystem and places its outputs in a safe state.
     */
    void stop();

    /**
     * Returns a short, human-readable name for telemetry and error messages.
     *
     * @return this subsystem's name
     */
    String getName();
}
