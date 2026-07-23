package org.firstinspires.ftc.teamcode.core.fsm;

import java.util.function.BooleanSupplier;

/**
 * A possible change from one {@link State} to another.
 */
public class Transition {
    private final State sourceState;
    private final State targetState;
    private final BooleanSupplier condition;

    /**
     * Creates a transition that fires when its condition is true while its source is active.
     *
     * @param sourceState the state this transition starts from
     * @param targetState the state that becomes active when this transition fires
     * @param condition the condition that allows this transition to fire
     * @throws IllegalArgumentException if any argument is null
     */
    public Transition(State sourceState, State targetState, BooleanSupplier condition) {
        if (sourceState == null) {
            throw new IllegalArgumentException("A transition needs a source state.");
        }
        if (targetState == null) {
            throw new IllegalArgumentException("A transition needs a target state.");
        }
        if (condition == null) {
            throw new IllegalArgumentException("A transition needs a condition.");
        }

        this.sourceState = sourceState;
        this.targetState = targetState;
        this.condition = condition;
    }

    /**
     * Returns whether this transition can be evaluated for the supplied active state.
     *
     * @param currentState the FSM's current state
     * @return true when the supplied state is this transition's source state
     */
    public boolean appliesTo(State currentState) {
        return sourceState == currentState;
    }

    /**
     * Returns whether this transition's condition is currently satisfied.
     *
     * @return true when this transition may fire
     */
    public boolean isConditionSatisfied() {
        return condition.getAsBoolean();
    }

    /**
     * Returns this transition's source state.
     *
     * @return the source state
     */
    public State getSourceState() {
        return sourceState;
    }

    /**
     * Returns this transition's target state.
     *
     * @return the target state
     */
    public State getTargetState() {
        return targetState;
    }
}
