package org.firstinspires.ftc.teamcode.robots.team3;

import com.pedropathing.api.Paths;
import com.pedropathing.follower.Follower;
import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;

/** Team 3 Curving Test route adapted to the Pedro 3 API. */
final class Team3PedroCurvingTestPath {
    private Team3PedroCurvingTestPath() {}

    static Path build(Follower follower) {
        if (follower == null) throw new IllegalArgumentException("The Team 3 curving test needs a Pedro follower.");
        Pose p0 = pose(56, 8, 90), p1 = pose(41.025306748466264, 34.4808282208589, 180);
        Pose p2 = point(20.073817928504972, 35.017456308372076), p3 = point(47.31624851510303, 94.012192096908);
        Pose p4 = point(37.32734218074718, 58.67702313481274), p5 = point(23.82704236479486, 58.41928192273228);
        Pose p6 = point(47.14772349843902, 94.0235901770676), p7 = point(36.80970932863983, 82.0405733605933);
        Pose p8 = point(24.816915151482426, 82.56958701991805), p9 = point(47.73074737639657, 93.76388857423315);
        Pose p10 = point(24.30746863099743, 93.82603876301096);
        return Paths.path(
                Paths.curve(p0, point(62.944785276073624, 36.45552147239262), p1).linear(p0, p1),
                Paths.line(p1, p2).tangent(),
                Paths.curve(p2, point(73.25523260830707, 46.97418003086089), p3).tangent(),
                Paths.curve(p3, point(65.24127387553247, 56.40173951770086), p4).tangent(),
                Paths.line(p4, p5).tangent(),
                Paths.curve(p5, point(70.80064980278259, 51.480638503887654), p6).tangent(),
                Paths.curve(p6, point(54.504406597588506, 81.93200508171387), p7).tangent(),
                Paths.line(p7, p8).tangent(),
                Paths.curve(p8, point(65.92107052774318, 61.883378901370094), p9).tangent(),
                Paths.line(p9, p10).tangent());
    }

    private static Pose point(double x, double y) { return new Pose(x, y); }
    private static Pose pose(double x, double y, double headingDegrees) {
        return new Pose(x, y, Math.toRadians(headingDegrees));
    }
}
