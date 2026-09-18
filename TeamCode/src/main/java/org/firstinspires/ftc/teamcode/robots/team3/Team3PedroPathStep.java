package org.firstinspires.ftc.teamcode.robots.team3;

import org.firstinspires.ftc.teamcode.common.autonomous.AutoStep;

/** Starts one selected Team 3 Pedro path, observes completion, and cancels safely. */
public final class Team3PedroPathStep implements AutoStep {
    private final Team3PedroRobot robot;
    private final Team3PedroPathRoute route;
    private boolean started;

    public Team3PedroPathStep(Team3PedroRobot robot, Team3PedroPathRoute route) {
        if (robot == null || route == null) {
            throw new IllegalArgumentException("A Team 3 Pedro path step needs a robot and route.");
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
