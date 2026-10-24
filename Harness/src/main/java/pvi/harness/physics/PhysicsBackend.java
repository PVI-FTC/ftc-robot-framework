package pvi.harness.physics;

/**
 * Whatever moves the simulated robot: given what the wheels are told to do, it works out where
 * the robot goes, one step at a time.
 *
 * <p>The harness's own {@link MomentumPhysics} is the first one. Later, Unity can be another
 * (Phase 7, stage U3): the harness sends it the {@link DriveCommand} for each step and Unity's
 * physics sends back the {@link State}, with real walls, game pieces and pushing.</p>
 *
 * <p>Everything going in and out is plain data in the FTC field frame: inches, radians,
 * seconds (see ai-instructions/02-unity-readiness.md). The harness calls {@link #step} once per
 * loop, always from the thread that runs the OpMode, and never skips or repeats a step, so a
 * run gives the same result every time.</p>
 */
public interface PhysicsBackend {

    /**
     * What the drive wheels are told to do during one step.
     *
     * @param pushes  how hard each wheel pushes the robot forward, -1 to 1 (power after motor
     *                direction and wiring; see {@link WheelReader})
     * @param braking true if the drive motors are on BRAKE, so zero power stops the robot
     *                quickly; false for FLOAT, where the robot coasts
     */
    record DriveCommand(WheelPushes pushes, boolean braking) {
        /** All wheels off, braking. */
        public static final DriveCommand STOPPED = new DriveCommand(WheelPushes.STOPPED, true);
    }

    /**
     * Where the robot is and how it is moving after a step.
     *
     * @param pose        position and heading on the field
     * @param velocity    speeds in the robot's own frame (forward, left, counter-clockwise)
     * @param wallContact which walls the robot is pressed against: "+x", "-x", "+y", "-y", two
     *                    of them in a corner ("+x/-y"), or "" for none
     */
    record State(Pose pose, MecanumKinematics.RobotVelocity velocity, String wallContact) {
        public State {
            if (pose == null || velocity == null) {
                throw new IllegalArgumentException("A physics state needs a pose and a velocity.");
            }
            wallContact = wallContact == null ? "" : wallContact;
        }
    }

    /** Where the robot is now. */
    State state();

    /** Moves the robot {@code seconds} forward in time with the wheels doing {@code command}. */
    void step(DriveCommand command, double seconds);
}
