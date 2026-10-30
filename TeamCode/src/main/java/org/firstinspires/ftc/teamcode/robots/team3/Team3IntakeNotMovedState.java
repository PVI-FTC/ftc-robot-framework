package org.firstinspires.ftc.teamcode.robots.team3;

import org.firstinspires.ftc.teamcode.core.fsm.State;

/**
 * Starting intake-deploy state that never commands the servo.
 *
 * <p>The servo stays unpowered (and does not move) at init and at start until the first deploy,
 * stow, or toggle request is made.</p>
 */
public class Team3IntakeNotMovedState implements State {
    @Override
    public void enter() {
        // Intentionally empty: the servo must not move until a request is made.
    }

    @Override
    public void update() {
        // Intentionally empty.
    }

    @Override
    public void exit() {
        // The next active state controls the servo.
    }

    @Override
    public String getName() {
        return "Not Moved";
    }
}
