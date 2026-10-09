package org.firstinspires.ftc.teamcode.robots.team3;

import org.firstinspires.ftc.teamcode.core.fsm.State;

/** Outtake state that spins the shooter motor at the configured power. */
public class Team3OuttakeSpinningState implements State {
    private final Team3OuttakeSubsystem outtakeSubsystem;

    public Team3OuttakeSpinningState(Team3OuttakeSubsystem outtakeSubsystem) {
        this.outtakeSubsystem = outtakeSubsystem;
    }

    @Override
    public void enter() {
        // Output is applied during update.
    }

    @Override
    public void update() {
        outtakeSubsystem.runOuttake();
    }

    @Override
    public void exit() {
        // The next active state controls the output.
    }

    @Override
    public String getName() {
        return "Spinning";
    }
}
