package org.firstinspires.ftc.teamcode.robots.team3;

import com.pedropathing.api.Paths;
import com.pedropathing.follower.Follower;
import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;

/**
 * Team 3 5-segment square lap test path imported from {@code path.pp (1).json},
 * maintaining the robot's initial heading (90 degrees) throughout the path.
 */
final class Team3PedroSquareTestPath {
    public static final double START_X_INCHES = 132.9527412048758;
    public static final double START_Y_INCHES = 10.326021481866706;
    public static final double START_HEADING_DEGREES = 180.0;
    public static final double END_X_INCHES = 106.44214431835131;
    public static final double END_Y_INCHES = 15.952465537441725;
    public static final double END_HEADING_DEGREES = 180.0;

    private Team3PedroSquareTestPath() {
    }

    static Path build(Follower follower) {
        if (follower == null) {
            throw new IllegalArgumentException("The Team 3 square test path needs a Pedro follower.");
        }

        double headingRad = Math.toRadians(START_HEADING_DEGREES);
        Pose p0 = new Pose(START_X_INCHES, START_Y_INCHES, headingRad);
        Pose p1 = new Pose(112.40613924313898, 10.02600965107957, headingRad);
        Pose p2 = new Pose(113.10580838224078, 117.82944724526197, headingRad);
        Pose p3 = new Pose(29.191526394269864, 118.7754050769004, headingRad);
        Pose p4 = new Pose(20.364575514271408, 23.925752860795615, headingRad);
        Pose p5 = new Pose(END_X_INCHES, END_Y_INCHES, headingRad);

        return Paths.path(
                Paths.line(p0, p1).constant(headingRad),
                Paths.line(p1, p2).constant(headingRad),
                Paths.line(p2, p3).constant(headingRad),
                Paths.line(p3, p4).constant(headingRad),
                Paths.line(p4, p5).constant(headingRad));
    }
}

