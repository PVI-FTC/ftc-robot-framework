package org.firstinspires.ftc.teamcode.robots.team3;

import org.firstinspires.ftc.teamcode.core.fsm.State;

/** Outtake state that keeps the shooter motor stopped. */
public class Team3OuttakeIdleState implements State {
    private final Team3OuttakeSubsystem outtakeSubsystem;

    public Team3OuttakeIdleState(Team3OuttakeSubsystem outtakeSubsystem) {
        this.outtakeSubsystem = outtakeSubsystem;
    }

    @Override
    public void enter() {
        // Output is applied during update.
    }

    @Override
    public void update() {
        outtakeSubsystem.stopOutput();
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
