package org.firstinspires.ftc.teamcode.robots.team3;

import com.pedropathing.api.Paths;
import com.pedropathing.follower.Follower;
import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;

/** Team 3 autonomous strafe-right 6-inch path adapted to the Pedro 3 API. */
public final class Team3PedroStrafeRightPath {
    public static final double START_X_INCHES = 0.0;
    public static final double START_Y_INCHES = 0.0;
    public static final double START_HEADING_RADIANS = 0.0;
    public static final double END_X_INCHES = 0.0;
    public static final double END_Y_INCHES = -6.0;
    public static final double END_HEADING_RADIANS = 0.0;

    private Team3PedroStrafeRightPath() {}

    static Path build(Follower follower) {
        if (follower == null) throw new IllegalArgumentException("The Team 3 strafe right path needs a Pedro follower.");
        Pose start = new Pose(START_X_INCHES, START_Y_INCHES, START_HEADING_RADIANS);
        Pose end = new Pose(END_X_INCHES, END_Y_INCHES, END_HEADING_RADIANS);
        return Paths.line(start, end).constant(START_HEADING_RADIANS);
    }
}
