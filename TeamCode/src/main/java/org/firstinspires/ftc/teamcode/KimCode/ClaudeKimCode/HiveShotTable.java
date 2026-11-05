package org.firstinspires.ftc.teamcode.KimCode.ClaudeKimCode;

/**
 * Turns a distance to the CELL into a launcher speed.
 *
 * <p>PLACEHOLDER: every number in this table is a guess. Use {@code HiveLauncherTest} to measure
 * which speed lands a shot from each distance, then replace them. Distances must go from smallest
 * to largest, and each distance needs one speed.</p>
 */
final class HiveShotTable {
    private static final double[] DISTANCES_INCHES = {24.0, 36.0, 48.0, 60.0};
    private static final double[] SPEEDS_TICKS_PER_SECOND = {1200.0, 1350.0, 1500.0, 1650.0};

    private HiveShotTable() {}

    /** Reads between table rows. Outside the table, it uses the closest row. */
    static double speedForDistance(double distanceInches) {
        int last = DISTANCES_INCHES.length - 1;
        if (distanceInches <= DISTANCES_INCHES[0]) {
            return SPEEDS_TICKS_PER_SECOND[0];
        }
        if (distanceInches >= DISTANCES_INCHES[last]) {
            return SPEEDS_TICKS_PER_SECOND[last];
        }
        int row = 0;
        while (distanceInches > DISTANCES_INCHES[row + 1]) {
            row++;
        }
        double share = (distanceInches - DISTANCES_INCHES[row])
                / (DISTANCES_INCHES[row + 1] - DISTANCES_INCHES[row]);
        return SPEEDS_TICKS_PER_SECOND[row]
                + share * (SPEEDS_TICKS_PER_SECOND[row + 1] - SPEEDS_TICKS_PER_SECOND[row]);
    }
}
