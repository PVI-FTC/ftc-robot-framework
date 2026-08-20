package org.firstinspires.ftc.teamcode.robots.teamA;

import org.firstinspires.ftc.teamcode.common.autonomous.AutoStep;

/** Starts one Team A pilot path, observes completion on later loops, and cancels safely. */
public final class TeamAPedroPilotPathStep implements AutoStep {
    private final TeamAPedroRobot robot;
    private boolean started;

    public TeamAPedroPilotPathStep(TeamAPedroRobot robot) {
        if (robot == null) {
            throw new IllegalArgumentException("The Team A pilot step needs a Pedro robot.");
        }
        this.robot = robot;
    }

    @Override
    public void start() {
        robot.startPilotPath();
        started = true;
    }

    @Override
    public void update() {
        // Robot.update() advances the active path once per FTC loop.
    }

    @Override
    public boolean isFinished() {
        return started && robot.isPathFollowingComplete();
    }

    @Override
    public void stop() {
        robot.cancelPathFollowing();
    }

    @Override
    public String getName() {
        return "TeamAPedroPilotPath";
    }
}
