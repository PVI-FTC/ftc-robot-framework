package org.firstinspires.ftc.teamcode.robots.team3;

import org.firstinspires.ftc.teamcode.core.fsm.State;

/** Intake-deploy state that moves the servo to the stowed position once on entry. */
public class Team3IntakeStowedState implements State {
    private final Team3IntakeDeploySubsystem deploySubsystem;

    public Team3IntakeStowedState(Team3IntakeDeploySubsystem deploySubsystem) {
        this.deploySubsystem = deploySubsystem;
    }

    @Override
    public void enter() {
        deploySubsystem.moveToStowed();
    }

    @Override
    public void update() {
        // The servo holds its position, so nothing is needed each loop.
    }

    @Override
    public void exit() {
        // The next active state controls the servo.
    }

    @Override
    public String getName() {
        return "Stowed";
    }
}
