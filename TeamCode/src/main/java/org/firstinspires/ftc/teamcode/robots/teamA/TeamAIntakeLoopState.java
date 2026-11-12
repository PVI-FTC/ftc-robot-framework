package org.firstinspires.ftc.teamcode.robots.teamA;

import org.firstinspires.ftc.teamcode.core.fsm.State;

/** One non-blocking state in Team A's sensor-driven intake loop. */
final class TeamAIntakeLoopState implements State {
    enum Mode {
        STOPPED,
        INTAKING,
        SORTING_POLLEN
    }

    private final TeamAIntakeLoopSubsystem subsystem;
    private final Mode mode;
    private final String name;

    TeamAIntakeLoopState(TeamAIntakeLoopSubsystem subsystem, Mode mode, String name) {
        this.subsystem = subsystem;
        this.mode = mode;
        this.name = name;
    }

    @Override
    public void enter() {
        subsystem.enterState(mode);
    }

    @Override
    public void update() {
        subsystem.updateState(mode);
    }

    @Override
    public void exit() {
        // The next active state applies its own outputs.
    }

    @Override
    public String getName() {
        return name;
    }
}
