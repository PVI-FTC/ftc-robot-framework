package org.firstinspires.ftc.teamcode.robots.team3;

import org.firstinspires.ftc.teamcode.common.autonomous.AutoStep;

/**
 * Instant non-blocking {@link AutoStep} factory methods for controlling Team 3's
 * intake rollers, midtake feeder, outtake shooter, and intake-deploy servo inside
 * an {@link org.firstinspires.ftc.teamcode.common.autonomous.AutoSequence}.
 *
 * <h2>Example usage in an Autonomous OpMode:</h2>
 * <pre>
 *   autoSequence = new AutoSequence(
 *       Team3MechanismStep.deployIntake(robot),
 *       new Team3PedroPathStep(robot, Team3PedroPathRoute.FORWARD_36),
 *       new Team3RelocalizeStep(robot),
 *       new Team3ScoreStep(robot),
 *       Team3MechanismStep.startCollecting(robot),
 *       new Team3PedroPathStep(robot, Team3PedroPathRoute.STRAFE_RIGHT_8),
 *       Team3MechanismStep.stopCollecting(robot)
 *   );
 * </pre>
 */
public final class Team3MechanismStep implements AutoStep {
    private final String name;
    private final Runnable action;
    private boolean executed;

    private Team3MechanismStep(String name, Runnable action) {
        this.name = name;
        this.action = action;
    }

    /** Moves the intake-deploy servo to its deployed position. */
    public static Team3MechanismStep deployIntake(Team3PedroRobot robot) {
        requireRobot(robot);
        return new Team3MechanismStep("Deploy Intake", robot::deployIntake);
    }

    /** Moves the intake-deploy servo to its stowed position. */
    public static Team3MechanismStep stowIntake(Team3PedroRobot robot) {
        requireRobot(robot);
        return new Team3MechanismStep("Stow Intake", robot::stowIntake);
    }

    /** Toggles the intake-deploy servo (matching pressing X in TeleOp). */
    public static Team3MechanismStep toggleIntakeDeploy(Team3PedroRobot robot) {
        requireRobot(robot);
        return new Team3MechanismStep("Toggle Intake Deploy", robot::toggleIntakeDeploy);
    }

    /** Starts the front intake rollers at 75% power to collect a game piece while driving. */
    public static Team3MechanismStep startCollecting(Team3PedroRobot robot) {
        requireRobot(robot);
        return new Team3MechanismStep("Start Intake (75%)", () -> {
            robot.setIntakePower(0.75);
            robot.startIntake();
        });
    }

    /** Stops the front intake rollers and midtake feeder. */
    public static Team3MechanismStep stopCollecting(Team3PedroRobot robot) {
        requireRobot(robot);
        return new Team3MechanismStep("Stop Intake", () -> {
            robot.stopIntake();
            robot.setMidtakeFeeding(false);
            robot.setMidtakeReversing(false);
        });
    }

    /** Pre-spins the shooter (outtake) motor while the robot is still driving to the goal. */
    public static Team3MechanismStep preSpinOuttake(Team3PedroRobot robot, double power) {
        requireRobot(robot);
        return new Team3MechanismStep("Pre-Spin Outtake", () -> robot.setOuttakePower(power));
    }

    @Override
    public void start() {
        executed = false;
        action.run();
        executed = true;
    }

    @Override
    public void update() {
        // Instant command step; completes immediately on the first loop.
    }

    @Override
    public boolean isFinished() {
        return executed;
    }

    @Override
    public void stop() {
        // Subsystems retain their requested state until the next step changes it.
    }

    @Override
    public String getName() {
        return name;
    }

    private static void requireRobot(Team3PedroRobot robot) {
        if (robot == null) {
            throw new IllegalArgumentException("Team 3 mechanism step needs a robot.");
        }
    }
}

