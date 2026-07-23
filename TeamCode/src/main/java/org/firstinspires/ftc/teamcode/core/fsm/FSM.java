package org.firstinspires.ftc.teamcode.core.fsm;

import java.util.ArrayList;
import java.util.List;

/**
 * A small finite-state machine with transitions evaluated in registration order.
 *
 * <p>Call {@link #initialize()} before {@link #update()}. Initialization calls the initial
 * state's {@link State#enter()} exactly once. During an update, this FSM checks transitions for
 * the active state in insertion order and fires at most one. When a transition fires, it calls
 * the old state's {@link State#exit()}, makes the target state current, calls its
 * {@link State#enter()}, and then calls the target state's {@link State#update()} in that same
 * update cycle.</p>
 */
public class FSM {
    private final List<Transition> transitions = new ArrayList<>();
    private State initialState;
    private State currentState;
    private boolean initialized;

    /**
     * Creates an FSM whose initial state can be set before initialization.
     */
    public FSM() {
        // An initial state may be supplied later with setInitialState.
    }

    /**
     * Creates an FSM with the supplied initial state.
     *
     * @param initialState the state to activate during initialization
     */
    public FSM(State initialState) {
        setInitialState(initialState);
    }

    /**
     * Sets the state that will be activated during initialization.
     *
     * @param initialState the starting state
     * @throws IllegalArgumentException if {@code initialState} is null
     * @throws IllegalStateException if the FSM is already initialized
     */
    public void setInitialState(State initialState) {
        if (initialState == null) {
            throw new IllegalArgumentException("An FSM needs a non-null initial state.");
        }
        if (initialized) {
            throw new IllegalStateException("The initial state cannot change after initialization.");
        }

        this.initialState = initialState;
    }

    /**
     * Registers a transition to evaluate after earlier registered transitions.
     *
     * @param transition the transition to register
     * @throws IllegalArgumentException if {@code transition} is null
     */
    public void addTransition(Transition transition) {
        if (transition == null) {
            throw new IllegalArgumentException("An FSM cannot register a null transition.");
        }

        transitions.add(transition);
    }

    /**
     * Activates the initial state once.
     *
     * <p>Later calls do nothing, so the initial state's {@link State#enter()} method cannot be
     * called twice by this FSM activation.</p>
     *
     * @throws IllegalStateException if no initial state has been set
     */
    public void initialize() {
        if (initialized) {
            return;
        }
        if (initialState == null) {
            throw new IllegalStateException("Set an initial state before initializing the FSM.");
        }

        currentState = initialState;
        initialized = true;
        currentState.enter();
    }

    /**
     * Updates the active state, firing at most one applicable transition first.
     *
     * @throws IllegalStateException if this FSM has not been initialized
     */
    public void update() {
        if (!initialized || currentState == null) {
            throw new IllegalStateException("Initialize the FSM with a valid initial state before updating.");
        }

        for (Transition transition : transitions) {
            if (transition.appliesTo(currentState) && transition.isConditionSatisfied()) {
                currentState.exit();
                currentState = transition.getTargetState();
                currentState.enter();
                break;
            }
        }

        currentState.update();
    }

    /**
     * Returns the active state, or null before initialization.
     *
     * @return the current state, or null when this FSM is not active
     */
    public State getCurrentState() {
        return currentState;
    }

    /**
     * Returns the active state's name for diagnostics.
     *
     * @return the current state name, or null before initialization
     */
    public String getCurrentStateName() {
        return currentState == null ? null : currentState.getName();
    }
}
