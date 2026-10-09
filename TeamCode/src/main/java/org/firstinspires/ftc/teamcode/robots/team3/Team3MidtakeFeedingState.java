package org.firstinspires.ftc.teamcode.robots.team3;

import org.firstinspires.ftc.teamcode.core.fsm.State;

/** Midtake state that runs the feeder motor at the configured power. */
public class Team3MidtakeFeedingState implements State {
    private final Team3MidtakeSubsystem midtakeSubsystem;

    public Team3MidtakeFeedingState(Team3MidtakeSubsystem midtakeSubsystem) {
        this.midtakeSubsystem = midtakeSubsystem;
    }

    @Override
    public void enter() {
        // Output is applied during update.
    }

    @Override
    public void update() {
        midtakeSubsystem.runMidtake();
    }

    @Override
    public void exit() {
        // The next active state controls the output.
    }

    @Override
    public String getName() {
        return "Feeding";
    }
}
