package org.firstinspires.ftc.teamcode.robots.teamA;

import com.pedropathing.follower.Follower;
import com.pedropathing.geometry.Pose;
import com.pedropathing.paths.PathChain;

/** Team A paths that can be selected from the Pedro autonomous menu. */
public enum TeamAPedroPathRoute {
    LP11_PILOT("LP-11 Pilot (24 in)", 0.0, 0.0, 0.0, 24.0, 0.0, 0.0),
    DECODE_ROUTE("DECODE Route", 56.0, 8.0, 90.0, 48.0, 81.0, 135.0),
    CURVING_TEST("Curving Test", 56.0, 8.0, 90.0,
            24.30746863099743, 93.82603876301096, 179.84797452163),
    ILLEGAL_PATH("Illegal Path", 56.0, 8.0, 90.0,
            103.42511867493754, 32.68189726856656, 90.0);

    private final String displayName;
    private final double startXInches;
    private final double startYInches;
    private final double startHeadingDegrees;
    private final double endXInches;
    private final double endYInches;
    private final double endHeadingDegrees;

    TeamAPedroPathRoute(String displayName, double startXInches, double startYInches,
            double startHeadingDegrees, double endXInches, double endYInches,
            double endHeadingDegrees) {
        this.displayName = displayName;
        this.startXInches = startXInches;
        this.startYInches = startYInches;
        this.startHeadingDegrees = startHeadingDegrees;
        this.endXInches = endXInches;
        this.endYInches = endYInches;
        this.endHeadingDegrees = endHeadingDegrees;
    }

    public String getDisplayName() { return displayName; }
    public double getStartXInches() { return startXInches; }
    public double getStartYInches() { return startYInches; }
    public double getStartHeadingRadians() { return Math.toRadians(startHeadingDegrees); }
    public double getEndXInches() { return endXInches; }
    public double getEndYInches() { return endYInches; }
    public double getEndHeadingRadians() { return Math.toRadians(endHeadingDegrees); }

    Pose createStartPose() {
        return new Pose(startXInches, startYInches, getStartHeadingRadians());
    }

    PathChain build(Follower follower) {
        switch (this) {
            case LP11_PILOT:
                return TeamAPedroPilotPath.build(follower);
            case DECODE_ROUTE:
                return TeamAPedroDecodePath.build(follower);
            case CURVING_TEST:
                return TeamAPedroCurvingTestPath.build(follower);
            case ILLEGAL_PATH:
                return TeamAPedroIllegalPath.build(follower);
            default:
                throw new IllegalStateException("No Team A Pedro path builder exists for " + this);
        }
    }
}
