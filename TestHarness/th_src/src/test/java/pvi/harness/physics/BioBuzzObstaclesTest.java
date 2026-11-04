package pvi.harness.physics;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import pvi.harness.runtime.JsonReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Loose balls bounce off the HIVE frame and the FLOWERs ({@link BioBuzzObstacles}), and roll under
 * anything that starts above the ball's top.
 *
 * <p>Like {@code GamePiecesTest}, these drive {@link GamePieces} directly: an 18 × 18 in robot box
 * pushes one ball, then lets it roll.</p>
 */
class BioBuzzObstaclesTest {
    private static final double SIZE = 18.0;
    private static final double LOOP = 0.02;

    /** A field with one ball of {@code kind} at (x, y), and the BIOBUZZ obstacles. */
    private static GamePieces oneBallAt(String kind, double x, double y) {
        GamePiece ball = new GamePiece("ball", kind, GamePiece.FIELD, x, y, GamePiece.radiusInches(kind));
        return new GamePieces(List.of(ball), PieceModel.bioBuzz(), BioBuzzObstacles.all());
    }

    private static GamePiece ball(GamePieces pieces) {
        return pieces.snapshot().get(0);
    }

    /**
     * Drives the robot box from {@code from} in a straight line at {@code speed} in/s for
     * {@code seconds} (the box faces the way it drives), then lets the ball roll for
     * {@code rollSeconds}. Returns every ball position along the way.
     */
    private static List<GamePiece> pushThenRoll(GamePieces pieces, Pose from, double speed, double seconds,
                                                double rollSeconds) {
        List<GamePiece> path = new ArrayList<>();
        double cos = Math.cos(from.headingRadians());
        double sin = Math.sin(from.headingRadians());
        Pose robot = from;
        for (double t = 0; t < seconds - 1e-9; t += LOOP) {
            Pose next = new Pose(robot.xInches() + speed * LOOP * cos, robot.yInches() + speed * LOOP * sin,
                    from.headingRadians());
            pieces.step(robot, next, SIZE, SIZE, LOOP, true);
            robot = next;
            path.add(ball(pieces));
        }
        for (double t = 0; t < rollSeconds - 1e-9; t += LOOP) {
            pieces.step(robot, robot, SIZE, SIZE, LOOP, true);
            path.add(ball(pieces));
        }
        return path;
    }

    /** True if a ball at {@code p} overlaps any obstacle it can reach (a little slack for rounding). */
    private static boolean insideAnObstacle(GamePiece p) {
        double r = p.radiusInches() - 1e-6;
        for (PieceObstacle o : BioBuzzObstacles.all()) {
            if (!o.reachedBy(p.radiusInches())) {
                continue;
            }
            double distance = o.cylinder()
                    ? Math.hypot(p.xInches() - o.minXInches(), p.yInches() - o.minYInches()) - o.maxXInches()
                    : Math.hypot(p.xInches() - Math.max(o.minXInches(), Math.min(o.maxXInches(), p.xInches())),
                            p.yInches() - Math.max(o.minYInches(), Math.min(o.maxYInches(), p.yInches())));
            boolean centerInBox = !o.cylinder() && p.xInches() > o.minXInches() && p.xInches() < o.maxXInches()
                    && p.yInches() > o.minYInches() && p.yInches() < o.maxYInches();
            if (centerInBox || distance < r) {
                return true;
            }
        }
        return false;
    }

    /**
     * WHAT: The obstacles in Java are exactly the ones in {@code fields/biobuzz-2026.json}
     * ("pieceObstacles": same ids, order, shapes and numbers).
     *
     * <p>WHY: UnityBot measures them in the CAD and keeps the file; the Java copy must follow, so
     * Unity and the harness bounce balls off the same things.</p>
     */
    @Test
    @SuppressWarnings("unchecked")
    void obstaclesMatchTheFieldFile() throws IOException {
        Map<String, Object> field = (Map<String, Object>) JsonReader.parse(
                Files.readString(Path.of("fields/biobuzz-2026.json")));
        List<Object> listed = (List<Object>) field.get("pieceObstacles");
        List<PieceObstacle> java = BioBuzzObstacles.all();

        assertEquals(listed.size(), java.size(), "number of obstacles");
        for (int i = 0; i < java.size(); i++) {
            Map<String, Object> o = (Map<String, Object>) listed.get(i);
            PieceObstacle j = java.get(i);
            String id = (String) o.get("id");
            assertEquals(id, j.id(), "obstacle " + i);
            boolean cylinder = "cylinder".equals(o.get("shape"));
            assertEquals(cylinder, j.cylinder(), id + " shape");
            double[] file = cylinder
                    ? new double[] {num(o, "xInches"), num(o, "radiusInches"), num(o, "yInches"), 0}
                    : new double[] {num(o, "minXInches"), num(o, "maxXInches"), num(o, "minYInches"), num(o, "maxYInches")};
            double[] copy = {j.minXInches(), j.maxXInches(), j.minYInches(), j.maxYInches()};
            for (int k = 0; k < 4; k++) {
                assertEquals(file[k], copy[k], 1e-9, id + " number " + k);
            }
            assertEquals(num(o, "bottomInches"), j.bottomInches(), 1e-9, id + " bottom");
        }
    }

    private static double num(Map<String, Object> o, String key) {
        return ((Number) o.get(key)).doubleValue();
    }

    /**
     * WHAT: A POLLEN pushed into a FLOWER's wall bracket stops at its face, bounces back, and is
     * never inside any obstacle.
     *
     * <p>HOW: The robot pushes the ball along +x at the audience FLOWER's first bracket
     * (x 69.22 to 70.5, y 21.5 to 22.78), then lets it roll.</p>
     */
    @Test
    void aBallBouncesOffAFlowerBracket() {
        GamePieces pieces = oneBallAt(GamePiece.POLLEN, 55, 22.1);
        List<GamePiece> path = pushThenRoll(pieces, Pose.inches(40, 22.1, 0), 50, 0.3, 2.0);

        double furthest = path.stream().mapToDouble(GamePiece::xInches).max().orElseThrow();
        // Positions are sampled once per loop, so the touch itself may fall between two samples.
        assertTrue(furthest <= 69.22 - 1.4 + 1e-9, "never past the bracket's face: " + furthest);
        assertTrue(furthest > 69.22 - 1.4 - 0.5, "got right up to it: " + furthest);
        assertTrue(ball(pieces).xInches() < furthest - 0.5, "bounced back: " + ball(pieces));
        assertTrue(path.stream().noneMatch(BioBuzzObstaclesTest::insideAnObstacle), "never inside an obstacle");
    }

    /**
     * WHAT: A POLLEN pushed toward the HIVE (along -y, at the red frame's foot-bar lip at y -24.16)
     * bounces back off the lip and is never inside any obstacle.
     */
    @Test
    void aBallBouncesOffTheHiveFrame() {
        GamePieces pieces = oneBallAt(GamePiece.POLLEN, 0, -5);
        List<GamePiece> path = pushThenRoll(pieces, Pose.inches(0, 10, -90), 50, 0.3, 2.0);

        double lowest = path.stream().mapToDouble(GamePiece::yInches).min().orElseThrow();
        assertTrue(lowest >= -24.16 + 1.4 - 1e-9, "never past the lip: " + lowest);
        assertTrue(lowest < -24.16 + 1.4 + 0.5, "got right up to it: " + lowest);
        assertTrue(ball(pieces).yInches() > lowest + 0.5, "bounced back: " + ball(pieces));
        assertTrue(path.stream().noneMatch(BioBuzzObstaclesTest::insideAnObstacle), "never inside an obstacle");
    }

    /**
     * WHAT: A NECTAR rolls straight under a FLOWER's raised head (bottom 3.75 in, above the
     * ball's 3.6 in top) without changing direction.
     *
     * <p>WHY: The real FLOWER head is raised; only its wall brackets reach the floor.</p>
     */
    @Test
    void aNectarRollsUnderAFlowerHead() {
        // Along -y at x = 66, through the far FLOWER head (center -68.04, -23.39, radius 3.13) but
        // clear of its brackets (x -70.5 to -69.22): a mirror image, along x = -66.
        GamePieces pieces = oneBallAt(GamePiece.RED_NECTAR, -66, -12);
        List<GamePiece> path = pushThenRoll(pieces, Pose.inches(-66, 2, -90), 40, 0.3, 1.5);

        assertTrue(path.stream().anyMatch(p -> Math.hypot(p.xInches() + 68.04, p.yInches() + 23.39) < 3.13),
                "the ball passed under the head");
        assertTrue(path.stream().allMatch(p -> Math.abs(p.xInches() + 66) < 1e-9), "never knocked sideways");
    }

    /**
     * WHAT: The same push gives exactly the same path every time.
     *
     * <p>WHY: Replays and tests depend on it (02-unity-readiness §3).</p>
     */
    @Test
    void obstacleBouncesAreTheSameEveryRun() {
        List<GamePiece> first = pushThenRoll(oneBallAt(GamePiece.POLLEN, 55, 22.1), Pose.inches(40, 22.1, 0), 50, 0.3, 2.0);
        List<GamePiece> second = pushThenRoll(oneBallAt(GamePiece.POLLEN, 55, 22.1), Pose.inches(40, 22.1, 0), 50, 0.3, 2.0);

        assertEquals(first, second);
    }
}
