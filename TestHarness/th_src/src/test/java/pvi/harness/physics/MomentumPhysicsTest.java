package pvi.harness.physics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 5 (Step 17): the harness's own physics backend. The robot speeds up and slows down
 * gradually (momentum), coasts at Pedro's measured rate on FLOAT, and stops at the walls.
 *
 * <p>Uses a round-number robot: 60 in/s forward and sideways, 180 °/s turning, 0.1 s response
 * time, 200 in/s² traction limit, 40 in/s² forward and 50 in/s² sideways coasting.</p>
 */
class MomentumPhysicsTest {
    private static final double DT = 0.020;
    private static final double SPEED = 60.0;

    private final DriveModel model = new DriveModel("Momentum bot",
            ModelValue.placeholder(SPEED, ModelValue.INCHES_PER_SECOND, "test"),
            ModelValue.placeholder(SPEED, ModelValue.INCHES_PER_SECOND, "test"),
            ModelValue.placeholder(180.0, ModelValue.DEGREES_PER_SECOND, "test"),
            ModelValue.placeholder(18.0, ModelValue.INCHES, "test"),
            ModelValue.placeholder(18.0, ModelValue.INCHES, "test"),
            new Dynamics(ModelValue.placeholder(0.1, Dynamics.SECONDS, "test"),
                    ModelValue.placeholder(200.0, Dynamics.INCHES_PER_SECOND_SQUARED, "test"),
                    ModelValue.placeholder(40.0, Dynamics.INCHES_PER_SECOND_SQUARED, "test"),
                    ModelValue.placeholder(50.0, Dynamics.INCHES_PER_SECOND_SQUARED, "test")));

    private static final PhysicsBackend.DriveCommand FULL_FORWARD =
            new PhysicsBackend.DriveCommand(new WheelPushes(1, 1, 1, 1), true);
    private static final PhysicsBackend.DriveCommand COAST =
            new PhysicsBackend.DriveCommand(WheelPushes.STOPPED, false);
    private static final PhysicsBackend.DriveCommand BRAKE =
            new PhysicsBackend.DriveCommand(WheelPushes.STOPPED, true);

    private static void run(PhysicsBackend physics, PhysicsBackend.DriveCommand command, double seconds) {
        for (long i = Math.round(seconds / DT); i > 0; i--) {
            physics.step(command, DT);
        }
    }

    private static double forwardSpeed(PhysicsBackend physics) {
        return physics.state().velocity().forwardInchesPerSecond();
    }

    /**
     * WHAT: From standing still, full power takes a few tenths of a second to reach full
     * speed, and the tires never speed it up faster than the traction limit.
     *
     * <p>WHY: This is the momentum Pedro's controllers are tuned for. Without it the robot
     * jumped to 60 in/s in one loop, and Pedro's braking pushed it back and forth.</p>
     */
    @Test
    void speedsUpGraduallyWithinTheTractionLimit() {
        MomentumPhysics physics = new MomentumPhysics(model, Pose.ORIGIN);
        double previous = 0;
        for (int i = 0; i < 10; i++) {
            physics.step(FULL_FORWARD, DT);
            double speed = forwardSpeed(physics);
            assertTrue(speed - previous <= 200.0 * DT + 1e-9, "faster than the traction limit at step " + i);
            previous = speed;
        }
        assertEquals(200.0 * 10 * DT, previous, 1e-6, "the first 0.2 s is traction-limited: 200 in/s² x 0.2 s");
        run(physics, FULL_FORWARD, 1.0);
        assertEquals(SPEED, forwardSpeed(physics), 0.1, "reaches full speed after a while");
    }

    /**
     * WHAT: With zero power on FLOAT the robot coasts and slows at the coasting rate; on BRAKE
     * it stops much sooner.
     *
     * <p>WHY: Pedro's zero-power acceleration tuners measured how far Team A's robot coasts;
     * its predictive braking relies on it.</p>
     */
    @Test
    void coastsOnFloatAndStopsSoonerOnBrake() {
        // Start near the -x wall, so there is room to speed up and coast without reaching +x.
        MomentumPhysics coasting = new MomentumPhysics(model, Pose.inches(-60, 0, 0));
        run(coasting, FULL_FORWARD, 1.0);
        double x0 = coasting.state().pose().xInches();
        double startSpeed = forwardSpeed(coasting);
        run(coasting, COAST, 0.5);
        assertEquals(startSpeed - 40.0 * 0.5, forwardSpeed(coasting), 1e-6, "slows by 40 in/s² for 0.5 s");
        run(coasting, COAST, 2.0);
        assertEquals(0.0, forwardSpeed(coasting), 0.0, "comes to rest");
        double coastDistance = coasting.state().pose().xInches() - x0;
        assertEquals(startSpeed * startSpeed / (2 * 40.0), coastDistance, 1.0, "v² / 2a = 45 in of coasting");

        MomentumPhysics braking = new MomentumPhysics(model, Pose.inches(-60, 0, 0));
        run(braking, FULL_FORWARD, 1.0);
        double b0 = braking.state().pose().xInches();
        run(braking, BRAKE, 2.5);
        assertEquals(0.0, forwardSpeed(braking), 0.0, "stops and stays stopped");
        double brakeDistance = braking.state().pose().xInches() - b0;
        assertTrue(brakeDistance < coastDistance / 2, "braking (" + brakeDistance + " in) stops much sooner than"
                + " coasting (" + coastDistance + " in)");
    }

    /**
     * WHAT: Sideways coasting uses the sideways rate, and turning slows too.
     *
     * <p>WHY: Mecanum robots coast differently sideways; Pedro measures both.</p>
     */
    @Test
    void sidewaysCoastingUsesItsOwnRate() {
        MomentumPhysics physics = new MomentumPhysics(model, Pose.inches(0, -60, 0));
        // Strafe left: front-left back, front-right forward, rear-left forward, rear-right back.
        run(physics, new PhysicsBackend.DriveCommand(new WheelPushes(-1, 1, 1, -1), true), 1.0);
        double startSpeed = physics.state().velocity().leftInchesPerSecond();
        assertEquals(SPEED, startSpeed, 0.1);
        run(physics, COAST, 0.5);
        assertEquals(startSpeed - 50.0 * 0.5, physics.state().velocity().leftInchesPerSecond(), 1e-6);
    }

    /**
     * WHAT: A wall stops the robot and cancels its speed into the wall, so it doesn't push on
     * through when the wall is gone (and slides along it when driving at an angle).
     *
     * <p>WHY: Momentum into a wall must not carry the robot through it.</p>
     */
    @Test
    void wallCancelsTheSpeedIntoIt() {
        MomentumPhysics physics = new MomentumPhysics(model, Pose.inches(50, 0, 0));
        run(physics, FULL_FORWARD, 1.0);
        PhysicsBackend.State state = physics.state();
        assertEquals(63.0, state.pose().xInches(), 1e-9);
        assertEquals("+x", state.wallContact());
        assertEquals(0.0, forwardSpeed(physics), 1e-9, "no speed left into the wall");
    }

    /**
     * WHAT: A drive model without dynamics changes speed instantly, so 1 s at full power is
     * exactly 1 s worth of distance.
     *
     * <p>WHY: Keeps simple math tests exact, and is what the round-number test robots use.</p>
     */
    @Test
    void withoutDynamicsSpeedChangesInstantly() {
        MomentumPhysics physics = new MomentumPhysics(model.withDynamics(null), Pose.ORIGIN);
        run(physics, FULL_FORWARD, 0.5);
        assertEquals(30.0, physics.state().pose().xInches(), 1e-9);
        run(physics, BRAKE, 0.02);
        assertEquals(30.0, physics.state().pose().xInches(), 1e-9, "stops dead");
    }
}
