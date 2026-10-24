package pvi.harness.bridge;

import pvi.harness.physics.MecanumKinematics;
import pvi.harness.physics.PhysicsBackend;
import pvi.harness.physics.Pose;

/**
 * Stage U3: the robot moved by Unity's physics instead of the harness's own
 * ({@link pvi.harness.physics.MomentumPhysics}). Used when Unity loads an OpMode with
 * {@code "physics": "unity"}.
 *
 * <p>How one loop goes (Unity drives the clock, so nothing here waits):</p>
 * <ol>
 *   <li>Each frame the harness sends tells Unity the wheels' {@code driveCommand}.</li>
 *   <li>Unity runs its physics for one loop period with that command (walls and field parts
 *       really block the robot) and puts the result in its next {@code step}:
 *       {@code physicsState}. The session hands it to {@link #supply}.</li>
 *   <li>The runner then calls {@link #step} for that same loop period; Unity has already done
 *       the moving, so this only counts the step. {@link #state} returns Unity's result, which
 *       the position estimate, encoders, Pinpoint and the recording all read.</li>
 * </ol>
 *
 * <p>This keeps exactly the order the built-in physics has (step, then sensors, then the
 * OpMode), so a run behaves the same apart from how the robot moves.</p>
 */
final class RemotePhysics implements PhysicsBackend {
    private State state;
    private int steps;

    /** A robot standing still at {@code start} until Unity says otherwise. */
    RemotePhysics(Pose start) {
        this.state = new State(start, MecanumKinematics.RobotVelocity.STOPPED, "");
    }

    /** Unity's newest result: where its physics put the robot. */
    void supply(State fromUnity) {
        if (fromUnity == null) {
            throw new IllegalArgumentException("Unity's physics state is missing.");
        }
        this.state = fromUnity;
    }

    /** How many loop steps the runner has asked for (one per loop once the wheels were read). */
    int steps() {
        return steps;
    }

    @Override
    public State state() {
        return state;
    }

    @Override
    public void step(DriveCommand command, double seconds) {
        // Unity already moved the robot for this step (its physicsState came with the step message).
        steps++;
    }
}
