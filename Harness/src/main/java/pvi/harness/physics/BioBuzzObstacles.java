package pvi.harness.physics;

import java.util.List;

/**
 * What loose BIOBUZZ balls can bump into besides the walls: the HIVE frame's feet and foot-bar
 * lips, and each FLOWER's wall brackets and raised head.
 *
 * <p>Copied from {@code fields/biobuzz-2026.json}, list {@code "pieceObstacles"} (field format
 * version 2), which UnityBot measured in the official field CAD at ball-center height
 * (2026-10-27). The HIVE baskets are above 30 in and parts under 0.6 in tall (plates, washers)
 * are left out, because balls go under or over them. {@code BioBuzzObstaclesTest} checks these
 * numbers still equal the file, so a change there shows up here.</p>
 *
 * <p>Written into Java, like {@link BioBuzzPieces}, so the simulation never reads files.</p>
 */
public final class BioBuzzObstacles {
    /** Only static methods here; nothing to create. */
    private BioBuzzObstacles() {
    }

    /** Every obstacle, in the file's order. */
    public static List<PieceObstacle> all() {
        return List.of(
                PieceObstacle.box("frame-red-lip", -18.87, 18.87, -24.36, -24.16, 0),
                PieceObstacle.box("frame-red-foot-far", -18.86, -17.33, -24.05, -22.77, 0),
                PieceObstacle.box("frame-red-foot-audience", 17.33, 18.86, -24.05, -22.77, 0),
                PieceObstacle.box("frame-blue-lip", -18.87, 18.87, 24.16, 24.36, 0),
                PieceObstacle.box("frame-blue-foot-far", -18.86, -17.33, 22.79, 24.05, 0),
                PieceObstacle.box("frame-blue-foot-audience", 17.33, 18.86, 22.79, 24.05, 0),
                PieceObstacle.box("flower-far-bracket-1", -70.5, -69.22, -25.29, -24.01, 0.5),
                PieceObstacle.box("flower-far-bracket-2", -70.5, -69.22, -22.78, -21.5, 0.5),
                PieceObstacle.cylinder("flower-far-head", -68.04, -23.39, 3.13, 3.75),
                PieceObstacle.box("flower-blue-wall-bracket-1", -25.29, -24.01, 69.22, 70.5, 0.5),
                PieceObstacle.box("flower-blue-wall-bracket-2", -22.78, -21.5, 69.22, 70.5, 0.5),
                PieceObstacle.cylinder("flower-blue-wall-head", -23.39, 68.04, 3.13, 3.75),
                PieceObstacle.box("flower-red-wall-bracket-1", 24.01, 25.29, -70.5, -69.22, 0.5),
                PieceObstacle.box("flower-red-wall-bracket-2", 21.5, 22.78, -70.5, -69.22, 0.5),
                PieceObstacle.cylinder("flower-red-wall-head", 23.39, -68.04, 3.13, 3.75),
                PieceObstacle.box("flower-audience-bracket-1", 69.22, 70.5, 21.5, 22.78, 0.5),
                PieceObstacle.box("flower-audience-bracket-2", 69.22, 70.5, 24.01, 25.29, 0.5),
                PieceObstacle.cylinder("flower-audience-head", 68.04, 23.39, 3.13, 3.75));
    }
}
