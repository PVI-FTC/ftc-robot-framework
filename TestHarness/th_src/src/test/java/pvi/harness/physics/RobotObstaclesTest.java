package pvi.harness.physics;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * W11: the robot box is stopped by the BIOBUZZ field elements (the HIVE frame's lips and feet,
 * the FLOWERs) the way the walls stop it: pushed back out, speed into the element taken away,
 * sliding along it.
 *
 * <p>These drive {@link MomentumPhysics} directly with a round-number 18 × 18 in test robot
 * (5 ft/s, instant speed changes), full power forward, so where it stops is easy to work out.</p>
 */
class RobotObstaclesTest {
    private static final double LOOP = 0.02;
    private static final PhysicsBackend.DriveCommand FULL_FORWARD =
            new PhysicsBackend.DriveCommand(new WheelPushes(1, 1, 1, 1), true);

    /** The test robot at {@code start}, with the BIOBUZZ field elements. */
    private static MomentumPhysics robotAt(Pose start) {
        MomentumPhysics physics = new MomentumPhysics(DriveModel.forTesting("test", 5, 5, 180), start);
        physics.setObstacles(BioBuzzObstacles.all());
        return physics;
    }

    /** Drives full forward for {@code seconds}; returns every state, one per loop. */
    private static List<PhysicsBackend.State> driveForward(MomentumPhysics physics, double seconds) {
        List<PhysicsBackend.State> states = new ArrayList<>();
        for (double t = 0; t < seconds - 1e-9; t += LOOP) {
            physics.step(FULL_FORWARD, LOOP);
            states.add(physics.state());
        }
        return states;
    }

    /**
     * WHAT: Driving straight at the blue HIVE frame's lip (+y, its face at y 24.16), the robot
     * stops with its front edge on the face, stops moving that way, and touches the "HIVE frame".
     */
    @Test
    void theRobotStopsAtTheHiveFrameLip() {
        MomentumPhysics physics = robotAt(Pose.inches(0, 0, 90));   // facing +y
        List<PhysicsBackend.State> states = driveForward(physics, 1.0);

        PhysicsBackend.State last = states.get(states.size() - 1);
        assertEquals(24.16 - 9, last.pose().yInches(), 1e-6, "front edge on the lip's face");
        assertEquals(0.0, last.velocity().forwardInchesPerSecond(), 1e-9, "no speed into the frame");
        assertEquals("HIVE frame", last.obstacleContact());
        assertTrue(states.stream().allMatch(s -> s.pose().yInches() <= 24.16 - 9 + 1e-6), "never past the face");
    }

    /**
     * WHAT: Even a far too fast robot (100 ft/s: 24 in per 20 ms loop, more than the box's
     * half-length) can't jump through the 0.2 in lip; it stops on the near side.
     *
     * <p>WHY: Checked only once per loop, its center could land past the lip, and the shortest
     * way out would then be through it. The physics checks every 0.1 in of motion instead.</p>
     */
    @Test
    void evenAVeryFastRobotCantJumpThroughTheLip() {
        MomentumPhysics physics = new MomentumPhysics(DriveModel.forTesting("fast", 100, 100, 180), Pose.inches(0, 5, 90));
        physics.setObstacles(BioBuzzObstacles.all());
        List<PhysicsBackend.State> states = driveForward(physics, 0.2);

        assertEquals(24.16 - 9, states.get(states.size() - 1).pose().yInches(), 1e-6, "stopped on the near side");
    }

    /**
     * WHAT: Driving at the lip at an angle (heading 60 degrees), the robot is stopped across it
     * but keeps sliding along it (+x), as at a wall.
     */
    @Test
    void theRobotSlidesAlongTheLipAtAnAngle() {
        MomentumPhysics physics = robotAt(Pose.inches(-10, 5, 60));
        List<PhysicsBackend.State> states = driveForward(physics, 1.5);

        PhysicsBackend.State touch = states.stream().filter(s -> !s.obstacleContact().isEmpty()).findFirst().orElseThrow();
        PhysicsBackend.State last = states.get(states.size() - 1);
        assertTrue(last.pose().xInches() > touch.pose().xInches() + 5, "slid along: from " + touch.pose() + " to " + last.pose());
        // (Later it meets the blue HIVE foot, which may push it back a little, away from the frame.)
        assertTrue(states.stream().allMatch(s -> s.pose().yInches() <= touch.pose().yInches() + 1e-6),
                "but never further toward the frame than where it touched");
    }

    /**
     * WHAT: Driving into a HIVE foot (red, far: x -18.86 to -17.33, y -24.05 to -22.77), which
     * stands in front of the lip, stops the robot at the foot.
     */
    @Test
    void theRobotStopsAtAHiveFoot() {
        MomentumPhysics physics = robotAt(Pose.inches(-18.1, 0, -90));   // facing -y, centered on the foot
        PhysicsBackend.State last = driveForward(physics, 1.0).get(49);

        assertEquals(-22.77 + 9, last.pose().yInches(), 1e-6, "front edge on the foot");
        assertEquals("HIVE frame", last.obstacleContact());
    }

    /**
     * WHAT: Driving into the far FLOWER (head radius 3.13 in at -68.04, -23.39; robots can't pass
     * under it) stops the robot before the wall, with its front edge on the head.
     */
    @Test
    void theRobotStopsAtAFlower() {
        MomentumPhysics physics = robotAt(Pose.inches(-45, -23.39, 180));   // facing -x
        PhysicsBackend.State last = driveForward(physics, 1.0).get(49);

        assertEquals(-68.04 + 3.13 + 9, last.pose().xInches(), 1e-6, "front edge on the head");
        assertEquals("far FLOWER", last.obstacleContact());
        assertEquals("", last.wallContact(), "stopped before the wall");
    }

    /**
     * WHAT: Without field elements (field.robotObstacles=false), the same drive goes straight
     * through the lip, as before W11.
     */
    @Test
    void withoutFieldElementsTheRobotPassesThrough() {
        MomentumPhysics physics = new MomentumPhysics(DriveModel.forTesting("test", 5, 5, 180), Pose.inches(0, 0, 90));
        List<PhysicsBackend.State> states = driveForward(physics, 1.0);

        assertTrue(states.get(states.size() - 1).pose().yInches() > 24.16 + 9, "drove past the frame");
        assertTrue(states.stream().allMatch(s -> s.obstacleContact().isEmpty()));
    }

    /**
     * WHAT: The same drive gives exactly the same states every run.
     *
     * <p>WHY: Replays and tests depend on it (02-unity-readiness §3).</p>
     */
    @Test
    void obstacleStopsAreTheSameEveryRun() {
        assertEquals(driveForward(robotAt(Pose.inches(-10, 5, 60)), 1.5),
                driveForward(robotAt(Pose.inches(-10, 5, 60)), 1.5));
    }
}
