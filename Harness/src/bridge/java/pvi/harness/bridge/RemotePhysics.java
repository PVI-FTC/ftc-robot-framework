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
 *       {@code physicsState}. The session hands it to {@link #supply}, which keeps it waiting.</li>
 *   <li>The runner then calls {@link #step} for that same loop period. Unity has already done
 *       the moving, so this only takes Unity's result: from now on {@link #state} returns it, and
 *       the position estimate, encoders, Pinpoint and the recording all read it.</li>
 * </ol>
 *
 * <p>Unity's result only counts once {@link #step} runs, never as soon as it arrives. The runner
 * reads the robot's pose just before the step and just after it, and the game pieces are pushed
 * by that move (with the robot's speed). If the new pose counted on arrival, "before" and
 * "after" would be the same, so a pushed ball would get no speed and stop dead as soon as the
 * robot stopped touching it.</p>
 *
 * <p>This keeps exactly the order the built-in physics has (step, then sensors, then the
 * OpMode), so a run behaves the same apart from how the robot moves.</p>
 */
final class RemotePhysics implements PhysicsBackend {
    private State state;
    /** Unity's newest result, waiting for the next {@link #step}; null when there's none. */
    private State pending;
    private int steps;

    /** A robot standing still at {@code start} until Unity says otherwise. */
    RemotePhysics(Pose start) {
        this.state = new State(start, MecanumKinematics.RobotVelocity.STOPPED, "");
    }

    /** Unity's newest result: where its physics put the robot. It counts from the next {@link #step}. */
    void supply(State fromUnity) {
        if (fromUnity == null) {
            throw new IllegalArgumentException("Unity's physics state is missing.");
        }
        this.pending = fromUnity;
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
        // Unity already moved the robot for this step (its physicsState came with the step message):
        // take its result now, so the pose before this step and after it differ by Unity's move.
        if (pending != null) {
            state = pending;
            pending = null;
        }
        steps++;
    }
}
