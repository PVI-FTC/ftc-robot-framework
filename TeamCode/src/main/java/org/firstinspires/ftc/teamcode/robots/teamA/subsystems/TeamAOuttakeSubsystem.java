package org.firstinspires.ftc.teamcode.robots.teamA.subsystems;

import org.firstinspires.ftc.teamcode.core.fsm.FSM;
import org.firstinspires.ftc.teamcode.core.fsm.State;
import org.firstinspires.ftc.teamcode.core.fsm.Transition;
import org.firstinspires.ftc.teamcode.core.robot.Subsystem;
import org.firstinspires.ftc.teamcode.robots.teamA.hardware.TeamAOuttakeHardware;

/** Alternates one Team A flywheel outtake between idle and launch velocity. */
public class TeamAOuttakeSubsystem implements Subsystem {
    private enum RequestedMode {
        STOPPED,
        IDLE,
        LAUNCH
    }

    private final String name;
    private final TeamAOuttakeHardware hardware;
    private final State stoppedState = new OuttakeState(RequestedMode.STOPPED);
    private final State idleState = new OuttakeState(RequestedMode.IDLE);
    private final State launchState = new OuttakeState(RequestedMode.LAUNCH);
    private final FSM fsm = new FSM(stoppedState);
    private RequestedMode requestedMode = RequestedMode.STOPPED;

    public TeamAOuttakeSubsystem(String name, TeamAOuttakeHardware hardware) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Outtake subsystem needs a name.");
        }
        if (hardware == null) {
            throw new IllegalArgumentException("Outtake subsystem needs outtake hardware.");
        }
        this.name = name;
        this.hardware = hardware;
        addTransitions();
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
        requestedMode = RequestedMode.STOPPED;
        hardware.stop();
    }

    @Override
    public String getName() {
        return name;
    }

    /** Selects idle on the first request, then alternates idle and launch velocity. */
    public void toggleVelocity() {
        requestedMode = requestedMode == RequestedMode.IDLE
                ? RequestedMode.LAUNCH : RequestedMode.IDLE;
    }

    public String getCurrentStateName() {
        String stateName = fsm.getCurrentStateName();
        return stateName == null ? stoppedState.getName() : stateName;
    }

    public boolean isAvailable() {
        return hardware.isAvailable();
    }

    private void addTransitions() {
        fsm.addTransition(new Transition(stoppedState, idleState,
                () -> requestedMode == RequestedMode.IDLE));
        fsm.addTransition(new Transition(idleState, stoppedState,
                () -> requestedMode == RequestedMode.STOPPED));
        fsm.addTransition(new Transition(idleState, launchState,
                () -> requestedMode == RequestedMode.LAUNCH));
        fsm.addTransition(new Transition(launchState, stoppedState,
                () -> requestedMode == RequestedMode.STOPPED));
        fsm.addTransition(new Transition(launchState, idleState,
                () -> requestedMode == RequestedMode.IDLE));
    }

    private class OuttakeState implements State {
        private final RequestedMode mode;

        OuttakeState(RequestedMode mode) {
            this.mode = mode;
        }

        @Override
        public void enter() {
            applyOutput();
        }

        @Override
        public void update() {
            applyOutput();
        }

        @Override
        public void exit() {
            // The next state applies its velocity immediately.
        }

        @Override
        public String getName() {
            if (mode == RequestedMode.IDLE) {
                return "IdleVelocity";
            }
            if (mode == RequestedMode.LAUNCH) {
                return "LaunchVelocity";
            }
            return "Stopped";
        }

        private void applyOutput() {
            if (mode == RequestedMode.IDLE) {
                hardware.runAtIdleVelocity();
            } else if (mode == RequestedMode.LAUNCH) {
                hardware.runAtLaunchVelocity();
            } else {
                hardware.stop();
            }
        }
    }
}
