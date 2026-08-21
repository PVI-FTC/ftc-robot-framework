package org.firstinspires.ftc.teamcode.robots.teamA;

import org.firstinspires.ftc.teamcode.common.autonomous.AutoStep;

/** Starts one selected Team A Pedro path, observes completion, and cancels safely. */
public final class TeamAPedroPathStep implements AutoStep {
    private final TeamAPedroRobot robot;
    private final TeamAPedroPathRoute route;
    private boolean started;

    public TeamAPedroPathStep(TeamAPedroRobot robot, TeamAPedroPathRoute route) {
        if (robot == null || route == null) {
            throw new IllegalArgumentException("A Team A Pedro path step needs a robot and route.");
        }
        this.robot = robot;
        this.route = route;
    }

    @Override
    public void start() {
        robot.startPath(route);
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
        return route.getDisplayName();
    }
}
