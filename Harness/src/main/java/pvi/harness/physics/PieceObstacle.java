package pvi.harness.physics;

/**
 * Something on the field a loose ball can hit, as plain data: a part of the HIVE frame or a
 * FLOWER. Every obstacle stands upright with straight sides, so a ball hits it in the same place
 * whatever its height, as long as the obstacle reaches down to the ball.
 *
 * <p>A ball of radius {@code r} sits with its center at {@code r} and its top at {@code 2r}. It
 * can only hit an obstacle whose bottom is below the ball's top ({@code bottomInches < 2r}); it
 * rolls under anything higher (for example a FLOWER's raised head).</p>
 *
 * @param id           a fixed name, the same as in {@code fields/biobuzz-2026.json}
 * @param cylinder     true for an upright cylinder (center and radius), false for an
 *                     axis-aligned box (min and max x and y)
 * @param minXInches   box: the side nearest -x (FTC field frame, inches). Cylinder: center x.
 * @param maxXInches   box: the side nearest +x. Cylinder: radius.
 * @param minYInches   box: the side nearest -y. Cylinder: center y.
 * @param maxYInches   box: the side nearest +y. Cylinder: unused (0).
 * @param bottomInches height of its lowest point above the tiles
 */
public record PieceObstacle(String id, boolean cylinder, double minXInches, double maxXInches,
                            double minYInches, double maxYInches, double bottomInches) {

    /** An upright box, axis-aligned. */
    public static PieceObstacle box(String id, double minX, double maxX, double minY, double maxY, double bottom) {
        if (!(minX < maxX) || !(minY < maxY)) {
            throw new IllegalArgumentException("Obstacle " + id + ": a box needs min below max on both axes.");
        }
        return new PieceObstacle(id, false, minX, maxX, minY, maxY, bottom);
    }

    /** An upright cylinder. */
    public static PieceObstacle cylinder(String id, double x, double y, double radius, double bottom) {
        if (!(radius > 0)) {
            throw new IllegalArgumentException("Obstacle " + id + ": a cylinder needs a radius above zero.");
        }
        return new PieceObstacle(id, true, x, radius, y, 0, bottom);
    }

    /** True if a ball of this radius, sitting on the tiles, reaches this obstacle. */
    public boolean reachedBy(double ballRadiusInches) {
        return bottomInches < 2 * ballRadiusInches;
    }
}
