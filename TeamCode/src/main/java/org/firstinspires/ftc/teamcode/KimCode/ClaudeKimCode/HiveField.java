package org.firstinspires.ftc.teamcode.KimCode.ClaudeKimCode;

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

    /**
     * PLACEHOLDER: where the robot stops to look at the HIVE. It stays clear of the frame and, with
     * the placeholder camera mount, far enough back that both CELLs should fit in the picture.
     */
    static final double VIEW_X_INCHES = 0.0;
    static final double VIEW_Y_INCHES = -50.0;

    /** Inside Red's LOADING ZONE, which runs from x = -46 to -24 and from y = -60 to the wall. */
    static final double PARK_X_INCHES = -34.0;
    static final double PARK_Y_INCHES = -60.0;

    /** Under the CELL that is raised at the start of a match, written for Red. */
    static final double START_RAISED_CELL_X_INCHES = 11.0;
    /** Under the CELL that is lowered at the start of a match, written for Red. */
    static final double START_LOWERED_CELL_X_INCHES = -11.0;
    static final double CELL_Y_INCHES = -12.75;

    /**
     * PLACEHOLDER: how far from the field's center line the robot launches. A CELL's opening points
     * along the x axis, away from the HIVE's pivot, so the robot stands on that side, level with
     * the CELL's y, and faces back toward the center. It has to be a distance the launcher can
     * reach, and the raised CELL's tags have to stay in the camera's picture so the robot can aim.
     * Closer than about x = 48 puts them above the top of the picture with the placeholder mount.
     */
    static final double LAUNCH_X_INCHES = 52.0;

    /** Going between the launch spot and the Red wall through here misses the HIVE frame's feet. */
    private static final double CLEAR_OF_FRAME_X_INCHES = 24.0;
    static final double CLEAR_OF_FRAME_Y_INCHES = -42.0;

    /** Height of a tag under a raised CELL, from the field CAD. */
    static final double RAISED_TAG_HEIGHT_INCHES = 49.0;
    /** Height of a tag under a lowered CELL, from the field CAD. */
    static final double LOWERED_TAG_HEIGHT_INCHES = 36.3;

    /** PLACEHOLDER: Limelight lens height from TeleopV1, not measured on this robot. */
    static final double CAMERA_HEIGHT_INCHES = 20.0;
    /** PLACEHOLDER: Limelight upward tilt from TeleopV1, not measured on this robot. */
    static final double CAMERA_TILT_DEGREES = 25.0;
    /** PLACEHOLDER: how far the lens sits in front of the robot's center. */
    static final double CAMERA_FORWARD_INCHES = 8.0;

    private HiveField() {}

    /** First of the four tag IDs on the CELL that is raised at the start of a match. */
    static int startRaisedFirstTagId(Alliance alliance) {
        return alliance == Alliance.RED ? 34 : 42;
    }

    /** First of the four tag IDs on the CELL that is lowered at the start of a match. */
    static int startLoweredFirstTagId(Alliance alliance) {
        return alliance == Alliance.RED ? 30 : 38;
    }

    /** The x of a CELL, written for Red. */
    static double cellXInches(boolean isStartRaisedCell) {
        return isStartRaisedCell ? START_RAISED_CELL_X_INCHES : START_LOWERED_CELL_X_INCHES;
    }

    /** The x of the spot this CELL is launched from, written for Red. */
    static double launchXInches(boolean isStartRaisedCell) {
        return isStartRaisedCell ? LAUNCH_X_INCHES : -LAUNCH_X_INCHES;
    }

    /** The heading that points the robot from the launch spot into this CELL, written for Red. */
    static double launchHeadingDegrees(boolean isStartRaisedCell) {
        return isStartRaisedCell ? 180.0 : 0.0;
    }

    /** The x of the spot that keeps the robot clear of the frame near this CELL, written for Red. */
    static double clearOfFrameXInches(boolean isStartRaisedCell) {
        return isStartRaisedCell ? CLEAR_OF_FRAME_X_INCHES : -CLEAR_OF_FRAME_X_INCHES;
    }

    /** Level distance from a point on the field to a CELL's tags. */
    static double distanceToCellInches(Alliance alliance, boolean isStartRaisedCell,
            double xInches, double yInches) {
        Pose cell = pose(alliance, cellXInches(isStartRaisedCell), CELL_Y_INCHES, 0.0);
        return Math.hypot(cell.x() - xInches, cell.y() - yInches);
    }

    /** Turns a position written for Red into the position for this alliance. */
    static Pose pose(Alliance alliance, double xInches, double yInches, double headingDegrees) {
        double heading = Math.toRadians(headingDegrees);
        if (alliance == Alliance.BLUE) {
            return new Pose(-xInches, -yInches, heading + Math.PI);
        }
        return new Pose(xInches, yInches, heading);
    }
}
