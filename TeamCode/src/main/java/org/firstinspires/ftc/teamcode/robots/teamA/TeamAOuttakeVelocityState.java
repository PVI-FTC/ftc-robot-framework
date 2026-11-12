package org.firstinspires.ftc.teamcode.robots.teamA;

import org.firstinspires.ftc.teamcode.core.fsm.State;

/** One velocity-controlled state for a Team A outtake motor. */
final class TeamAOuttakeVelocityState implements State {
    private final TeamAOuttakeSubsystem outtakeSubsystem;
    private final TeamAOuttakeSubsystem.OuttakeMotor motor;
    private final String name;
    private final double velocityTicksPerSecond;

    TeamAOuttakeVelocityState(
            TeamAOuttakeSubsystem outtakeSubsystem,
            TeamAOuttakeSubsystem.OuttakeMotor motor,
            String name,
            double velocityTicksPerSecond) {
        this.outtakeSubsystem = outtakeSubsystem;
        this.motor = motor;
        this.name = name;
        this.velocityTicksPerSecond = velocityTicksPerSecond;
    }

    @Override
    public void enter() {
        outtakeSubsystem.applyVelocity(motor, velocityTicksPerSecond);
    }

    @Override
    public void update() {
        outtakeSubsystem.applyVelocity(motor, velocityTicksPerSecond);
    }

    @Override
    public void exit() {
        // The next active state applies its own velocity.
    }

    @Override
    public String getName() {
        return name;
    }
}
