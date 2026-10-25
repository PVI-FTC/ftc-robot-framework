package pvi.harness.physics;

/**
 * One game piece (a ball) at one moment, as plain data: what it is, what it is doing, and where
 * its center is.
 *
 * <p>The run recording saves these (format version 4), and both the desktop viewer and Unity draw
 * them. A piece is the same physical ball for the whole run, so its {@code id} never changes;
 * only its position and state do.</p>
 *
 * @param id      a name for this ball that stays the same all run, e.g. "pollen-garden-red-1"
 * @param kind    {@link #POLLEN}, {@link #RED_NECTAR} or {@link #BLUE_NECTAR}
 * @param state   where it is in the game: {@link #FIELD} (loose on the tiles, can be pushed),
 *                {@link #FLOWER}, {@link #CELL}, {@link #ALLIANCE_AREA}, or {@link #HELD}
 *                (reserved for an intake later). Other states may be added later without a
 *                format change.
 * @param xInches center, FTC field frame (inches, +x toward the audience)
 * @param yInches center, FTC field frame (+y from the red wall toward the blue wall)
 * @param zInches height of the center above the tiles (a ball on the tiles is at its radius)
 */
public record GamePiece(String id, String kind, String state, double xInches, double yInches, double zInches) {

    /** A 2.8 in yellow ball. */
    public static final String POLLEN = "pollen";
    /** A 3.6 in red ball. */
    public static final String RED_NECTAR = "red-nectar";
    /** A 3.6 in blue ball. */
    public static final String BLUE_NECTAR = "blue-nectar";

    /** Loose on the tiles: the robot can push it. The only state that moves for now. */
    public static final String FIELD = "field";
    /** Stacked in a FLOWER. */
    public static final String FLOWER = "flower";
    /** In a HIVE CELL. */
    public static final String CELL = "cell";
    /** Waiting outside the field in an ALLIANCE AREA. */
    public static final String ALLIANCE_AREA = "alliance-area";
    /** Carried by the robot. Reserved for an intake; nothing makes a piece held yet. */
    public static final String HELD = "held";

    /** Refuses a missing id, kind or state, an unknown kind, and non-numbers. */
    public GamePiece {
        if (id == null || id.isBlank() || state == null || state.isBlank()) {
            throw new IllegalArgumentException("A game piece needs an id and a state.");
        }
        radiusInches(kind); // fails clearly for an unknown kind
        if (!Double.isFinite(xInches) || !Double.isFinite(yInches) || !Double.isFinite(zInches)) {
            throw new IllegalArgumentException("Game piece " + id + " has a position that isn't a number: ("
                    + xInches + ", " + yInches + ", " + zInches + ").");
        }
    }

    /** This ball's radius in inches. */
    public double radiusInches() {
        return radiusInches(kind);
    }

    /** The radius of a ball of this kind, in inches (from the BIOBUZZ manual, section 10.3.1). */
    public static double radiusInches(String kind) {
        if (POLLEN.equals(kind)) {
            return 1.4;
        }
        if (RED_NECTAR.equals(kind) || BLUE_NECTAR.equals(kind)) {
            return 1.8;
        }
        throw new IllegalArgumentException("Unknown game piece kind \"" + kind + "\". Expected " + POLLEN + ", "
                + RED_NECTAR + " or " + BLUE_NECTAR + ".");
    }

    /** The same piece somewhere else, or in another state. */
    public GamePiece movedTo(String newState, double x, double y, double z) {
        return new GamePiece(id, kind, newState, x, y, z);
    }
}
