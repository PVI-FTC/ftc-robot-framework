package org.firstinspires.ftc.teamcode.robots.teamA;

import com.pedropathing.api.Paths;
import com.pedropathing.follower.Follower;
import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;

/** Team-owned Illegal Path route adapted to the Pedro 3 API. */
final class TeamAPedroIllegalPath {
    private TeamAPedroIllegalPath() {}

    static Path build(Follower follower) {
        if (follower == null) throw new IllegalArgumentException("The Team A illegal path needs a Pedro follower.");
        Pose p0 = pose(56, 8, 90), p1 = pose(70.75766871165644, 117.60122699386507, 180);
        Pose p2 = pose(120.05288968162552, 70.19872520338755, 180);
        Pose p3 = pose(103.42511867493754, 32.68189726856656, 90);
        return Paths.path(
                Paths.curve(p0, point(133.89685582822085, 68.55176380368098),
                        point(-0.17101226993865729, 51.476993865030686), p1).linear(p0, p1),
                Paths.curve(p1, point(80.82629146657963, 79.88693928881037), p2).linear(p1, p2),
                Paths.line(p2, p3).linear(p2, p3));
    }

    private static Pose point(double x, double y) { return new Pose(x, y); }
    private static Pose pose(double x, double y, double headingDegrees) {
        return new Pose(x, y, Math.toRadians(headingDegrees));
    }
}
