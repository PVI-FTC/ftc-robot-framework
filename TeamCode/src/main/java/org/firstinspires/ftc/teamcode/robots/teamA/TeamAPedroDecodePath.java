package org.firstinspires.ftc.teamcode.robots.teamA;

import com.pedropathing.follower.Follower;
import com.pedropathing.geometry.BezierLine;
import com.pedropathing.geometry.Pose;
import com.pedropathing.paths.PathChain;

/** Team-owned path imported from the Team A Pedro Route DECODE Visualizer export. */
final class TeamAPedroDecodePath {
    private TeamAPedroDecodePath() {
        // The path is built through the selected TeamAPedroPathRoute.
    }

    static PathChain build(Follower follower) {
        if (follower == null) {
            throw new IllegalArgumentException("The Team A DECODE path needs a Pedro follower.");
        }

        Pose p0 = pose(56, 8, 90);
        Pose p1 = pose(57, 36, 180);
        Pose p2 = pose(15, 36, 180);
        Pose p3 = pose(57, 57, 180);
        Pose p4 = pose(57, 90, 135);
        Pose p5 = pose(42, 60, 180);
        Pose p6 = pose(18, 60, 180);
        Pose p7 = pose(57, 90, 135);
        Pose p8 = pose(42, 84, 180);
        Pose p9 = pose(18, 84, 180);
        Pose p10 = pose(57, 90, 135);
        Pose p11 = pose(48, 81, 135);

        return follower.pathBuilder()
                .addPath(new BezierLine(p0, p1))
                .setLinearHeadingInterpolation(p0.getHeading(), p1.getHeading())
                .addPath(new BezierLine(p1, p2))
                .setLinearHeadingInterpolation(p1.getHeading(), p2.getHeading())
                .addPath(new BezierLine(p2, p3))
                .setLinearHeadingInterpolation(p2.getHeading(), p3.getHeading())
                .addPath(new BezierLine(p3, p4))
                .setLinearHeadingInterpolation(p3.getHeading(), p4.getHeading())
                .addPath(new BezierLine(p4, p5))
                .setLinearHeadingInterpolation(p4.getHeading(), p5.getHeading())
                .addPath(new BezierLine(p5, p6))
                .setLinearHeadingInterpolation(p5.getHeading(), p6.getHeading())
                .addPath(new BezierLine(p6, p7))
                .setLinearHeadingInterpolation(p6.getHeading(), p7.getHeading())
                .addPath(new BezierLine(p7, p8))
                .setLinearHeadingInterpolation(p7.getHeading(), p8.getHeading())
                .addPath(new BezierLine(p8, p9))
                .setLinearHeadingInterpolation(p8.getHeading(), p9.getHeading())
                .addPath(new BezierLine(p9, p10))
                .setLinearHeadingInterpolation(p9.getHeading(), p10.getHeading())
                .addPath(new BezierLine(p10, p11))
                .setLinearHeadingInterpolation(p10.getHeading(), p11.getHeading())
                .build();
    }

    private static Pose pose(double x, double y, double headingDegrees) {
        return new Pose(x, y, Math.toRadians(headingDegrees));
    }
}
