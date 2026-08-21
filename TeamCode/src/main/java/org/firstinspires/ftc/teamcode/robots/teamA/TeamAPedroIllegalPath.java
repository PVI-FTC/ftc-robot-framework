package org.firstinspires.ftc.teamcode.robots.teamA;

import com.pedropathing.follower.Follower;
import com.pedropathing.geometry.BezierCurve;
import com.pedropathing.geometry.BezierLine;
import com.pedropathing.geometry.Pose;
import com.pedropathing.paths.PathChain;

/** Team-owned path imported from the Team A Pedro Illegal Path Visualizer export. */
final class TeamAPedroIllegalPath {
    private TeamAPedroIllegalPath() {
        // The path is built through the selected TeamAPedroPathRoute.
    }

    static PathChain build(Follower follower) {
        if (follower == null) {
            throw new IllegalArgumentException("The Team A illegal path needs a Pedro follower.");
        }

        Pose p0 = pose(56.0, 8.0, 90.0);
        Pose p1 = pose(70.75766871165644, 117.60122699386507, 180.0);
        Pose p2 = pose(120.05288968162552, 70.19872520338755, 180.0);
        Pose p3 = pose(103.42511867493754, 32.68189726856656, 90.0);

        return follower.pathBuilder()
                .addPath(new BezierCurve(p0,
                        point(133.89685582822085, 68.55176380368098),
                        point(-0.17101226993865729, 51.476993865030686), p1))
                .setLinearHeadingInterpolation(p0.getHeading(), p1.getHeading())
                .addPath(new BezierCurve(p1,
                        point(80.82629146657963, 79.88693928881037), p2))
                .setLinearHeadingInterpolation(p1.getHeading(), p2.getHeading())
                .addPath(new BezierLine(p2, p3))
                .setLinearHeadingInterpolation(p2.getHeading(), p3.getHeading())
                .build();
    }

    private static Pose point(double x, double y) {
        return new Pose(x, y);
    }

    private static Pose pose(double x, double y, double headingDegrees) {
        return new Pose(x, y, Math.toRadians(headingDegrees));
    }
}
