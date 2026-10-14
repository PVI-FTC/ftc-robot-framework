package org.firstinspires.ftc.teamcode.robots.team3;

import com.pedropathing.follower.Follower;
import com.pedropathing.geometry.BezierLine;
import com.pedropathing.geometry.Pose;
import com.pedropathing.paths.PathChain;

/**
 * Team 3 path that strafes 6 inches to the right while holding a constant heading.
 *
 * <p>In Pedro's coordinate system a positive Y value is to the robot's left when the heading is
 * 0, so moving right means Y gets smaller.</p>
 */
public final class Team3PedroStrafeRightPath {
    public static final double START_X_INCHES = 0.0;
    public static final double START_Y_INCHES = 0.0;
    public static final double START_HEADING_RADIANS = 0.0;
    /** How far to move to the right, in inches. */
    public static final double DISTANCE_INCHES = 6.0;
    public static final double END_X_INCHES = 0.0;
    public static final double END_Y_INCHES = -DISTANCE_INCHES;
    public static final double END_HEADING_RADIANS = 0.0;

    private Team3PedroStrafeRightPath() {
    }

    static PathChain build(Follower follower) {
        if (follower == null) {
            throw new IllegalArgumentException("The Team 3 strafe-right path needs a Pedro follower.");
        }
        Pose start = new Pose(START_X_INCHES, START_Y_INCHES, START_HEADING_RADIANS);
        Pose end = new Pose(END_X_INCHES, END_Y_INCHES, END_HEADING_RADIANS);
        return follower.pathBuilder()
                .addPath(new BezierLine(start, end))
                .setConstantHeadingInterpolation(START_HEADING_RADIANS)
                .build();
    }
}
