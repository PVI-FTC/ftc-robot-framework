package org.firstinspires.ftc.teamcode.KimCode;

import com.pedropathing.math.Pose;

/**
 * Field positions for the Hive Auto.
 *
 * <p>Every position is written once, for Red, in the FTC field frame: inches, origin at the field
 * center, +x toward the audience, +y from the Red wall toward the Blue wall, and heading
 * counterclockwise from +x. The BIOBUZZ field is the same when rotated 180 degrees, so Blue's
 * positions are worked out from Red's by {@link #pose}.</p>
 *
 * <p>The numbers come from the BIOBUZZ field CAD, not from a real field. Check each one on a real
 * field before trusting it.</p>
 */
final class HiveField {
    enum Alliance { RED, BLUE }

    /** Distance from the field center to the perimeter wall. */
    private static final double WALL_INCHES = 70.5;
    /** PLACEHOLDER: the FTC maximum, not measured on this robot. */
    private static final double ROBOT_LENGTH_INCHES = 18.0;

    /** Heading that points the front of the robot from the Red wall toward the HIVE. */
    static final double FACING_HIVE_DEGREES = 90.0;

    /**
     * Touching the Red wall, beside the LOADING ZONE. The zone's tape ends near x = -24, and half
     * of an 18 inch robot must stay clear of it, so the robot's center has to be above about -15.
     */
    static final double START_X_INCHES = -12.0;
    static final double START_Y_INCHES = -(WALL_INCHES - ROBOT_LENGTH_INCHES / 2.0);

    /** PLACEHOLDER: where the robot stops to look at the HIVE. It stays clear of the frame. */
    static final double VIEW_X_INCHES = 0.0;
    static final double VIEW_Y_INCHES = -44.0;

    /** Inside Red's LOADING ZONE, which runs from x = -46 to -24 and from y = -60 to the wall. */
    static final double PARK_X_INCHES = -34.0;
    static final double PARK_Y_INCHES = -60.0;

    private HiveField() {}

    /** Turns a position written for Red into the position for this alliance. */
    static Pose pose(Alliance alliance, double xInches, double yInches, double headingDegrees) {
        double heading = Math.toRadians(headingDegrees);
        if (alliance == Alliance.BLUE) {
            return new Pose(-xInches, -yInches, normalize(heading + Math.PI));
        }
        return new Pose(xInches, yInches, normalize(heading));
    }

    /** Keeps an angle between -pi and +pi so the same direction is always the same number. */
    private static double normalize(double radians) {
        return Math.atan2(Math.sin(radians), Math.cos(radians));
    }
}
