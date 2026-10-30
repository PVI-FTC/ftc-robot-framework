package org.firstinspires.ftc.teamcode.robots.team3;

import com.pedropathing.api.Paths;
import com.pedropathing.follower.Follower;
import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;

/** Temporary Team 3 autonomous path: move 2 inches forward, then 8 inches right. */
public final class Team3PedroStrafeRight8Path {
    public static final double START_X_INCHES = 0.0;
    public static final double START_Y_INCHES = 0.0;
    public static final double START_HEADING_RADIANS = 0.0;

    public static final double FORWARD_X_INCHES = 2.0;
    public static final double FORWARD_Y_INCHES = 0.0;

    public static final double END_X_INCHES = 2.0;
    public static final double END_Y_INCHES = -8.0;
    public static final double END_HEADING_RADIANS = 0.0;

    private Team3PedroStrafeRight8Path() {}

    static Path build(Follower follower) {
        if (follower == null) {
            throw new IllegalArgumentException("The Team 3 strafe right path needs a Pedro follower.");
        }
        Pose start = new Pose(START_X_INCHES, START_Y_INCHES, START_HEADING_RADIANS);
        Pose forward = new Pose(FORWARD_X_INCHES, FORWARD_Y_INCHES, START_HEADING_RADIANS);
        Pose end = new Pose(END_X_INCHES, END_Y_INCHES, END_HEADING_RADIANS);
        return Paths.path(
                Paths.line(start, forward).constant(START_HEADING_RADIANS),
                Paths.line(forward, end).constant(END_HEADING_RADIANS)
        );
    }
}
