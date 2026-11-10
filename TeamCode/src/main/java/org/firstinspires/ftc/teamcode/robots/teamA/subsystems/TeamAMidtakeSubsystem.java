package org.firstinspires.ftc.teamcode.robots.teamA.subsystems;

import org.firstinspires.ftc.teamcode.core.fsm.FSM;
import org.firstinspires.ftc.teamcode.core.fsm.State;
import org.firstinspires.ftc.teamcode.core.fsm.Transition;
import org.firstinspires.ftc.teamcode.core.robot.Subsystem;
import org.firstinspires.ftc.teamcode.robots.teamA.hardware.TeamAMidtakeHardware;

/** Toggles Team A's pollen midtake between stopped and running. */
public class TeamAMidtakeSubsystem implements Subsystem {
    private enum RequestedMode {
        STOPPED,
        RUNNING
    }

    private final TeamAMidtakeHardware hardware;
    private final State stoppedState = new MidtakeState(RequestedMode.STOPPED);
    private final State runningState = new MidtakeState(RequestedMode.RUNNING);
    private final FSM fsm = new FSM(stoppedState);
    private RequestedMode requestedMode = RequestedMode.STOPPED;

    public TeamAMidtakeSubsystem(TeamAMidtakeHardware hardware) {
        if (hardware == null) {
            throw new IllegalArgumentException("Midtake subsystem needs midtake hardware.");
        }
        this.hardware = hardware;
        fsm.addTransition(new Transition(stoppedState, runningState,
                () -> requestedMode == RequestedMode.RUNNING));
        fsm.addTransition(new Transition(runningState, stoppedState,
                () -> requestedMode == RequestedMode.STOPPED));
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
        return "Team A Midtake";
    }

    /** Changes stopped to running, or running to stopped. */
    public void toggle() {
        requestedMode = requestedMode == RequestedMode.STOPPED
                ? RequestedMode.RUNNING : RequestedMode.STOPPED;
    }

    public String getCurrentStateName() {
        String stateName = fsm.getCurrentStateName();
        return stateName == null ? stoppedState.getName() : stateName;
    }

    public boolean isAvailable() {
        return hardware.isAvailable();
    }

    private class MidtakeState implements State {
        private final RequestedMode mode;

        MidtakeState(RequestedMode mode) {
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
            // The next state applies its output immediately.
        }

        @Override
        public String getName() {
            return mode == RequestedMode.RUNNING ? "Running" : "Stopped";
        }

        private void applyOutput() {
            if (mode == RequestedMode.RUNNING) {
                hardware.run();
            } else {
                hardware.stop();
            }
        }
    }
}
