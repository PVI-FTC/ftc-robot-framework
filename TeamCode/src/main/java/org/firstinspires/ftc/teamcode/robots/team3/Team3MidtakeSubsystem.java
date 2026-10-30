package org.firstinspires.ftc.teamcode.robots.team3;

import org.firstinspires.ftc.teamcode.core.fsm.FSM;
import org.firstinspires.ftc.teamcode.core.fsm.Transition;
import org.firstinspires.ftc.teamcode.core.robot.Subsystem;

/** Controls Team 3's midtake (feeder) motor with a two-state finite-state machine. */
public class Team3MidtakeSubsystem implements Subsystem {
    /** Change this value to tune feed speed. */
    public static final double MIDTAKE_POWER = 0.5;

    private final Team3MechanismHardware hardware;
    private final Team3MidtakeIdleState idleState;
    private final Team3MidtakeFeedingState feedingState;
    private final Team3MidtakeReversingState reversingState;
    private final FSM fsm;

    private boolean feedRequested;
    private boolean reverseRequested;

    public Team3MidtakeSubsystem(Team3MechanismHardware hardware) {
        if (hardware == null) {
            throw new IllegalArgumentException("Midtake subsystem needs mechanism hardware.");
        }
        this.hardware = hardware;
        idleState = new Team3MidtakeIdleState(this);
        feedingState = new Team3MidtakeFeedingState(this);
        reversingState = new Team3MidtakeReversingState(this);
        fsm = new FSM(idleState);
        fsm.addTransition(new Transition(idleState, feedingState, () -> feedRequested && !reverseRequested));
        fsm.addTransition(new Transition(idleState, reversingState, () -> reverseRequested));
        fsm.addTransition(new Transition(feedingState, idleState, () -> !feedRequested && !reverseRequested));
        fsm.addTransition(new Transition(feedingState, reversingState, () -> reverseRequested));
        fsm.addTransition(new Transition(reversingState, idleState, () -> !reverseRequested && !feedRequested));
        fsm.addTransition(new Transition(reversingState, feedingState, () -> feedRequested && !reverseRequested));
    }

    @Override
    public void initialize() {
        fsm.initialize();
    }

    @Override
    public void update() {
        fsm.update();
    }

    @Override
    public void stop() {
        feedRequested = false;
        reverseRequested = false;
        hardware.setMidtakePower(0.0);
    }

    @Override
    public String getName() {
        return "Midtake";
    }

    /** Requests that the midtake feed forward (true) or stop (false). */
    public void setFeeding(boolean feeding) {
        feedRequested = feeding;
    }

    /** Requests that the midtake reverse (true) or stop (false). */
    public void setReversing(boolean reversing) {
        reverseRequested = reversing;
    }

    public String getCurrentStateName() {
        String name = fsm.getCurrentStateName();
        return name == null ? idleState.getName() : name;
    }

    public boolean isAvailable() {
        return hardware.isMidtakeAvailable();
    }

    void runMidtake() {
        hardware.setMidtakePower(MIDTAKE_POWER);
    }

    void reverseMidtake() {
        hardware.setMidtakePower(-MIDTAKE_POWER);
    }

    void stopOutput() {
        hardware.setMidtakePower(0.0);
    }
}
