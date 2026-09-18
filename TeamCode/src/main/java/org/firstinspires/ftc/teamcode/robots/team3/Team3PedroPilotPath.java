package org.firstinspires.ftc.teamcode.robots.team3;

import com.pedropathing.follower.Follower;
import com.pedropathing.geometry.BezierLine;
import com.pedropathing.geometry.Pose;
import com.pedropathing.paths.PathChain;

/** Team 3 pilot path. */
public final class Team3PedroPilotPath {
    public static final double START_X_INCHES = 0.0;
    public static final double START_Y_INCHES = 0.0;
    public static final double START_HEADING_RADIANS = 0.0;
    public static final double END_X_INCHES = 24.0;
    public static final double END_Y_INCHES = 0.0;
    public static final double END_HEADING_RADIANS = 0.0;

    private Team3PedroPilotPath() {
    }

    static PathChain build(Follower follower) {
        if (follower == null) {
            throw new IllegalArgumentException("The Team 3 pilot path needs a Pedro follower.");
        }
        Pose start = new Pose(START_X_INCHES, START_Y_INCHES, START_HEADING_RADIANS);
        Pose end = new Pose(END_X_INCHES, END_Y_INCHES, END_HEADING_RADIANS);
        return follower.pathBuilder()
                .addPath(new BezierLine(start, end))
                .setConstantHeadingInterpolation(START_HEADING_RADIANS)
                .build();
    }
}
