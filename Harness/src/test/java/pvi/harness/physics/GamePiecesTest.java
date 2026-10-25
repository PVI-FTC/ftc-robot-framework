package pvi.harness.physics;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The game pieces: where BIOBUZZ's start, and how the robot box pushes the loose ones, how they
 * roll to a stop, and how they bounce off the walls and each other.
 *
 * <p>These drive {@link GamePieces} directly with a robot box moved step by step, so the numbers
 * are easy to follow. {@code GamePiecesRecordingTest} checks the same through a real OpMode run.</p>
 */
class GamePiecesTest {
    /** An 18 × 18 in robot box. */
    private static final double SIZE = 18.0;
    /** One loop, in seconds. */
    private static final double LOOP = 0.02;

    private static GamePieces onePollenAt(double x, double y) {
        return new GamePieces(List.of(new GamePiece("ball", GamePiece.POLLEN, GamePiece.FIELD, x, y, 1.4)),
                PieceModel.bioBuzz());
    }

    /** Drives the robot box straight along +x at {@code speed} in/s for {@code seconds}, from x = {@code fromX}. */
    private static Pose driveAlongX(GamePieces pieces, double fromX, double speed, double seconds) {
        Pose robot = Pose.inches(fromX, 0, 0);
        for (double t = 0; t < seconds - 1e-9; t += LOOP) {
            Pose next = Pose.inches(robot.xInches() + speed * LOOP, 0, 0);
            pieces.step(robot, next, SIZE, SIZE, LOOP, true);
            robot = next;
        }
        return robot;
    }

    private static GamePiece piece(GamePieces pieces, String id) {
        return pieces.snapshot().stream().filter(p -> p.id().equals(id)).findFirst().orElseThrow();
    }

    /**
     * WHAT: BIOBUZZ starts with 44 pieces, each with its own id: 16 POLLEN in the FLOWERs, 8 in
     * the GARDENs, 4 pre-loads behind the robot, 3 NECTAR in each upward CELL and 5 in each
     * ALLIANCE AREA.
     *
     * <p>WHY: Unity and the viewer find each ball by its id all run, so ids must be unique.</p>
     */
    @Test
    void bioBuzzStartsWithEveryPieceOnce() {
        List<GamePiece> start = BioBuzzPieces.start(Pose.ORIGIN, SIZE);
        Set<String> ids = new HashSet<>();
        start.forEach(p -> ids.add(p.id()));

        assertEquals(44, start.size());
        assertEquals(44, ids.size(), "ids must be unique");
        assertEquals(16, start.stream().filter(p -> p.state().equals(GamePiece.FLOWER)).count());
        assertEquals(12, start.stream().filter(p -> p.state().equals(GamePiece.FIELD)).count());
        assertEquals(6, start.stream().filter(p -> p.state().equals(GamePiece.CELL)).count());
        assertEquals(10, start.stream().filter(p -> p.state().equals(GamePiece.ALLIANCE_AREA)).count());
    }

    /**
     * WHAT: The 4 pre-loads touch the robot's back edge; when the robot's back is against a wall,
     * they touch its front edge instead.
     *
     * <p>WHY: The plan places them against the robot, and never outside the field.</p>
     */
    @Test
    void preloadsTouchTheRobotsBackOrFront() {
        GamePiece behind = BioBuzzPieces.start(Pose.ORIGIN, SIZE).stream()
                .filter(p -> p.id().equals("pollen-preload-1")).findFirst().orElseThrow();
        assertEquals(-(9 + 1.4), behind.xInches(), 1e-9, "touching the back edge of a robot facing +x");
        assertEquals(-4.2, behind.yInches(), 1e-9, "spread across the robot (left offset -4.2 in)");

        // Facing +x with the back 1 in from the -x wall: no room behind.
        Pose againstWall = Pose.inches(-72 + 9 + 1, 0, 0);
        GamePiece front = BioBuzzPieces.start(againstWall, SIZE).stream()
                .filter(p -> p.id().equals("pollen-preload-1")).findFirst().orElseThrow();
        assertEquals(againstWall.xInches() + 9 + 1.4, front.xInches(), 1e-9);
    }

    /**
     * WHAT: Driving into a ball pushes it ahead: it never ends up inside the robot box, and it
     * leaves at least as fast as the robot.
     *
     * <p>WHY: This is the "level 3" behavior: the robot pushes the loose balls out of its way.</p>
     */
    @Test
    void theRobotPushesABallAhead() {
        GamePieces pieces = onePollenAt(14, 0);
        Pose robot = driveAlongX(pieces, 0, 40, 0.5);
        GamePiece ball = piece(pieces, "ball");

        assertTrue(ball.xInches() >= robot.xInches() + 9 + 1.4 - 1e-6, "in front of the robot: " + ball + " robot " + robot);
        assertTrue(pieces.speedInchesPerSecond("ball") >= 40 - 1e-6, "at least the robot's speed");
        assertEquals(0.0, ball.yInches(), 1e-9, "straight ahead");
    }

    /**
     * WHAT: A pushed ball that is left alone rolls on and slows to a stop, after about
     * v²/(2a) inches.
     *
     * <p>WHY: Rolling friction (a placeholder value) is what stops a ball on the tiles.</p>
     */
    @Test
    void aBallRollsToAStop() {
        GamePieces pieces = onePollenAt(14, 0);
        driveAlongX(pieces, 0, 20, 0.3);
        double speed = pieces.speedInchesPerSecond("ball");
        double from = piece(pieces, "ball").xInches();
        // Back the robot away so it no longer touches the ball, then wait.
        Pose robot = Pose.inches(-30, 0, 0);
        for (int i = 0; i < 300; i++) {
            pieces.step(robot, robot, SIZE, SIZE, LOOP, true);
        }
        double deceleration = PieceModel.bioBuzz().rollingDeceleration().value();
        assertEquals(0.0, pieces.speedInchesPerSecond("ball"), 0.0, "stopped");
        assertEquals(speed * speed / (2 * deceleration), piece(pieces, "ball").xInches() - from, 1.0);
    }

    /**
     * WHAT: A ball rolling into a wall stops at the wall (its edge at 72 in) and bounces back.
     *
     * <p>WHY: Balls must stay on the field, like the robot.</p>
     */
    @Test
    void aBallBouncesOffTheWall() {
        GamePieces pieces = onePollenAt(60, 0);
        driveAlongX(pieces, 40, 50, 0.3);
        Pose robot = Pose.inches(0, 0, 0);
        double furthest = 0;
        for (int i = 0; i < 100; i++) {
            pieces.step(robot, robot, SIZE, SIZE, LOOP, true);
            furthest = Math.max(furthest, piece(pieces, "ball").xInches());
        }
        assertEquals(72 - 1.4, furthest, 1e-9, "reaches the wall, no further");
        assertTrue(piece(pieces, "ball").xInches() < 72 - 1.4 - 0.5, "bounced back off it");
    }

    /**
     * WHAT: A pushed ball knocks into another one and sets it rolling.
     *
     * <p>WHY: Balls bounce off each other, so a push can move more than one.</p>
     */
    @Test
    void aBallKnocksAnotherOne() {
        GamePieces pieces = new GamePieces(List.of(
                new GamePiece("first", GamePiece.POLLEN, GamePiece.FIELD, 14, 0, 1.4),
                new GamePiece("second", GamePiece.RED_NECTAR, GamePiece.FIELD, 24, 0, 1.8)), PieceModel.bioBuzz());
        driveAlongX(pieces, 0, 40, 0.3);
        Pose robot = Pose.inches(-30, 0, 0);
        for (int i = 0; i < 20; i++) {
            pieces.step(robot, robot, SIZE, SIZE, LOOP, true);
        }
        GamePiece first = piece(pieces, "first");
        GamePiece second = piece(pieces, "second");
        assertTrue(second.xInches() > 24.5, "the second ball moved: " + second);
        assertTrue(second.xInches() - first.xInches() >= 1.4 + 1.8 - 1e-6, "they don't overlap");
    }

    /**
     * WHAT: Balls in a FLOWER, a CELL or an ALLIANCE AREA stay put, even with the robot box over them.
     *
     * <p>WHY: Only loose balls on the tiles move for now (no scoring or intake yet).</p>
     */
    @Test
    void onlyLooseBallsMove() {
        GamePiece inFlower = new GamePiece("f", GamePiece.POLLEN, GamePiece.FLOWER, 14, 0, 20);
        GamePieces pieces = new GamePieces(List.of(inFlower), PieceModel.bioBuzz());
        driveAlongX(pieces, 0, 40, 0.5);
        assertEquals(inFlower, piece(pieces, "f"));
    }

    /**
     * WHAT: The same pushes give exactly the same ball positions every time.
     *
     * <p>WHY: Replays and tests depend on a run always giving the same result.</p>
     */
    @Test
    void thePushesAreTheSameEveryTime() {
        GamePieces a = new GamePieces(BioBuzzPieces.start(Pose.ORIGIN, SIZE), PieceModel.bioBuzz());
        GamePieces b = new GamePieces(BioBuzzPieces.start(Pose.ORIGIN, SIZE), PieceModel.bioBuzz());
        driveAlongX(a, 0, -40, 1.0);
        driveAlongX(b, 0, -40, 1.0);
        assertEquals(a.snapshot(), b.snapshot());
        assertTrue(piece(a, "pollen-preload-1").xInches() < -12, "driving backward pushed the pre-loads");
    }
}
