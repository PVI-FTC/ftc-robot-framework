package org.firstinspires.ftc.teamcode.core.fsm;

/**
 * One mode of behavior in a finite-state machine.
 *
 * <p>An {@link FSM} calls {@link #enter()} once when this state becomes active, calls
 * {@link #update()} once per active FSM update, and calls {@link #exit()} before leaving this
 * state.</p>
 */
public interface State {
    /**
     * Starts this state's behavior.
     */
    void enter();

    /**
     * Performs this state's work for one update cycle.
     */
    void update();

    /**
     * Ends this state's behavior before another state becomes active.
     */
    void exit();

    /**
     * Returns a short, human-readable name for diagnostics.
     *
     * @return this state's name
     */
    String getName();
}
