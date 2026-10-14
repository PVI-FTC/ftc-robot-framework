package org.firstinspires.ftc.teamcode.common.subsystems.intake;

import org.firstinspires.ftc.teamcode.core.fsm.State;

/** Safe intake state that commands zero output, or the optional idle power when one is set. */
public class IdleIntakeState implements State {
    private final IntakeSubsystem intakeSubsystem;

    public IdleIntakeState(IntakeSubsystem intakeSubsystem) {
        this.intakeSubsystem = intakeSubsystem;
    }

    @Override
    public void enter() {
        intakeSubsystem.applyIdleOutput();
    }

    @Override
    public void update() {
        intakeSubsystem.applyIdleOutput();
    }

    @Override
    public void exit() {
        // The next active state applies its own output.
    }

    @Override
    public String getName() {
        return "IdleIntake";
    }
}
