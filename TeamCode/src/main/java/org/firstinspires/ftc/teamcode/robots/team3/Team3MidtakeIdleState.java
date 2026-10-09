package org.firstinspires.ftc.teamcode.robots.team3;

import org.firstinspires.ftc.teamcode.core.fsm.State;

/** Midtake state that keeps the feeder motor stopped. */
public class Team3MidtakeIdleState implements State {
    private final Team3MidtakeSubsystem midtakeSubsystem;

    public Team3MidtakeIdleState(Team3MidtakeSubsystem midtakeSubsystem) {
        this.midtakeSubsystem = midtakeSubsystem;
    }

    @Override
    public void enter() {
        // Output is applied during update.
    }

    @Override
    public void update() {
        midtakeSubsystem.stopOutput();
    }

    @Override
    public void exit() {
        // The next active state controls the output.
    }

    @Override
    public String getName() {
        return "Idle";
    }
}
