package pvi.harness.physics;

import java.util.ArrayList;
import java.util.List;

/**
 * Where the BIOBUZZ (2026-2027) game pieces start, for one simulated robot.
 *
 * <p>From the BIOBUZZ manual (TU06), sections 9 and 10.3.1, with the field positions checked
 * against FIRST's official field CAD (FLOWER centers, GARDEN tape, upward CELLs). Positions are
 * in the FTC field frame, in inches; z is the height of the ball's center. Some are approximate
 * (the stacked FLOWER balls and the balls in the CELLs) and are marked so below.</p>
 *
 * <p>Only one robot is simulated, so it gets 4 pre-loaded POLLEN (a real match has 16, 4 per
 * robot). They sit on the tiles touching the robot's back edge, or its front edge if the robot
 * starts with its back against a wall.</p>
 *
 * <p>The decision to write these into Java (not read them from {@code fields/*.json}) is the
 * user's, 2026-10-25.</p>
 */
public final class BioBuzzPieces {
    /** FLOWER centers, from the official CAD: far wall, blue wall, red wall, audience wall. */
    private static final double[][] FLOWERS = {{-68.04, -23.39}, {-23.39, 68.04}, {23.39, -68.04}, {68.04, 23.39}};
    private static final String[] FLOWER_NAMES = {"far", "blue-wall", "red-wall", "audience"};
    /** Ball center heights for the 4 POLLEN stacked in a FLOWER, top first (approximate). */
    private static final double[] FLOWER_HEIGHTS = {21.25, 18.45, 15.65, 12.85};
    /** Where the 4 GARDEN POLLEN sit along the wall, from the corner (y for red; mirrored for blue). */
    private static final double[] GARDEN_ALONG = {-70.6, -67.8, -65.0, -62.2};
    /** Sideways offsets of the 4 pre-loaded POLLEN across the robot (inches left of its center). */
    private static final double[] PRELOAD_OFFSETS = {-4.2, -1.4, 1.4, 4.2};
    /** x of the 3 NECTAR in each upward CELL (red; blue is mirrored), approximate. */
    private static final double[] CELL_X = {7.37, 10.97, 14.57};
    private static final double CELL_Y = 20.95;
    private static final double CELL_Z = 51.8;
    /** x of the 5 NECTAR waiting in each ALLIANCE AREA, just outside the field. */
    private static final double[] ALLIANCE_X = {-8, -4, 0, 4, 8};
    private static final double ALLIANCE_Y = 76;

    /** Only static methods here; nothing to create. */
    private BioBuzzPieces() {
    }

    /**
     * Every piece at the start of a match, in a fixed order.
     *
     * @param robotStart   where the robot starts (the pre-loads are placed against it)
     * @param lengthInches the robot box, front to back
     */
    public static List<GamePiece> start(Pose robotStart, double lengthInches) {
        List<GamePiece> pieces = new ArrayList<>();
        double pollen = GamePiece.radiusInches(GamePiece.POLLEN);
        double nectar = GamePiece.radiusInches(GamePiece.RED_NECTAR);
        for (int f = 0; f < FLOWERS.length; f++) {
            for (int i = 0; i < FLOWER_HEIGHTS.length; i++) {
                pieces.add(new GamePiece("pollen-flower-" + FLOWER_NAMES[f] + "-" + (i + 1), GamePiece.POLLEN,
                        GamePiece.FLOWER, FLOWERS[f][0], FLOWERS[f][1], FLOWER_HEIGHTS[i]));
            }
        }
        for (int i = 0; i < GARDEN_ALONG.length; i++) {
            pieces.add(new GamePiece("pollen-garden-red-" + (i + 1), GamePiece.POLLEN, GamePiece.FIELD,
                    70.6, GARDEN_ALONG[i], pollen));
        }
        for (int i = 0; i < GARDEN_ALONG.length; i++) {
            pieces.add(new GamePiece("pollen-garden-blue-" + (i + 1), GamePiece.POLLEN, GamePiece.FIELD,
                    -70.6, -GARDEN_ALONG[i], pollen));
        }
        pieces.addAll(preloads(robotStart, lengthInches, pollen));
        for (int i = 0; i < CELL_X.length; i++) {
            pieces.add(new GamePiece("red-nectar-cell-" + (i + 1), GamePiece.RED_NECTAR, GamePiece.CELL,
                    CELL_X[i], -CELL_Y, CELL_Z));
        }
        for (int i = 0; i < CELL_X.length; i++) {
            pieces.add(new GamePiece("blue-nectar-cell-" + (i + 1), GamePiece.BLUE_NECTAR, GamePiece.CELL,
                    -CELL_X[CELL_X.length - 1 - i], CELL_Y, CELL_Z));
        }
        for (int i = 0; i < ALLIANCE_X.length; i++) {
            pieces.add(new GamePiece("red-nectar-alliance-" + (i + 1), GamePiece.RED_NECTAR, GamePiece.ALLIANCE_AREA,
                    ALLIANCE_X[i], -ALLIANCE_Y, nectar));
        }
        for (int i = 0; i < ALLIANCE_X.length; i++) {
            pieces.add(new GamePiece("blue-nectar-alliance-" + (i + 1), GamePiece.BLUE_NECTAR, GamePiece.ALLIANCE_AREA,
                    ALLIANCE_X[i], ALLIANCE_Y, nectar));
        }
        return pieces;
    }

    /**
     * The 4 pre-loaded POLLEN in a row across the robot, touching its back edge; if any of them
     * wouldn't fit on the field there (the robot's back is against a wall), touching its front edge.
     */
    private static List<GamePiece> preloads(Pose robot, double lengthInches, double radius) {
        double behind = -(lengthInches / 2.0 + radius);
        List<GamePiece> back = preloadRow(robot, behind, radius);
        for (GamePiece p : back) {
            double limit = Pose.FIELD_HALF_WIDTH_INCHES - radius;
            if (Math.abs(p.xInches()) > limit + 1e-9 || Math.abs(p.yInches()) > limit + 1e-9) {
                return preloadRow(robot, -behind, radius);
            }
        }
        return back;
    }

    /** 4 POLLEN on the tiles, {@code forward} inches in front of the robot center, spread across it. */
    private static List<GamePiece> preloadRow(Pose robot, double forward, double radius) {
        double cos = Math.cos(robot.headingRadians());
        double sin = Math.sin(robot.headingRadians());
        List<GamePiece> row = new ArrayList<>();
        for (int i = 0; i < PRELOAD_OFFSETS.length; i++) {
            double left = PRELOAD_OFFSETS[i];
            row.add(new GamePiece("pollen-preload-" + (i + 1), GamePiece.POLLEN, GamePiece.FIELD,
                    robot.xInches() + forward * cos - left * sin,
                    robot.yInches() + forward * sin + left * cos, radius));
        }
        return row;
    }
}
