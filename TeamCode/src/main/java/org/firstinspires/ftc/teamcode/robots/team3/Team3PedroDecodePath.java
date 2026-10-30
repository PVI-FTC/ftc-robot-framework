package org.firstinspires.ftc.teamcode.robots.team3;

import com.pedropathing.api.Paths;
import com.pedropathing.follower.Follower;
import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;

/** Team 3 DECODE path adapted to the Pedro 3 API. */
final class Team3PedroDecodePath {
    private Team3PedroDecodePath() {
    }

    static Path build(Follower follower) {
        if (follower == null) {
            throw new IllegalArgumentException("The Team 3 DECODE path needs a Pedro follower.");
        }

        Pose p0 = pose(56, 8, 90), p1 = pose(57, 36, 180), p2 = pose(15, 36, 180);
        Pose p3 = pose(57, 57, 180), p4 = pose(57, 90, 135), p5 = pose(42, 60, 180);
        Pose p6 = pose(18, 60, 180), p7 = pose(57, 90, 135), p8 = pose(42, 84, 180);
        Pose p9 = pose(18, 84, 180), p10 = pose(57, 90, 135), p11 = pose(48, 81, 135);

        return Paths.path(
                Paths.line(p0, p1).linear(p0, p1), Paths.line(p1, p2).linear(p1, p2),
                Paths.line(p2, p3).linear(p2, p3), Paths.line(p3, p4).linear(p3, p4),
                Paths.line(p4, p5).linear(p4, p5), Paths.line(p5, p6).linear(p5, p6),
                Paths.line(p6, p7).linear(p6, p7), Paths.line(p7, p8).linear(p7, p8),
                Paths.line(p8, p9).linear(p8, p9), Paths.line(p9, p10).linear(p9, p10),
                Paths.line(p10, p11).linear(p10, p11));
    }

    private static Pose pose(double x, double y, double headingDegrees) {
        return new Pose(x, y, Math.toRadians(headingDegrees));
    }
}
