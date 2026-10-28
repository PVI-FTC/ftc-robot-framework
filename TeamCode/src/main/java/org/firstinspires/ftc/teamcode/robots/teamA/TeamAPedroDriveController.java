package org.firstinspires.ftc.teamcode.robots.teamA;

import com.pedropathing.follower.Follower;
import com.pedropathing.math.Pose;
import org.firstinspires.ftc.teamcode.common.localization.PoseEstimate;
import org.firstinspires.ftc.teamcode.common.subsystems.drive.DriveController;

/** Team A adapter that keeps Pedro 3 types out of the shared drive API. */
public final class TeamAPedroDriveController implements DriveController {
    private Follower follower;
    private boolean manualStarted;

    void setFollower(Follower follower) {
        if (follower == null) throw new IllegalArgumentException("Pedro drive controller needs a follower.");
        this.follower = follower;
    }

    void preparePath(TeamAPedroPathRoute route) {
        requireFollower();
        if (route == null) throw new IllegalArgumentException("Select a Team A Pedro route first.");
        follower.stop();
        manualStarted = false;
        follower.setPose(route.createStartPose());
    }

    void startPath(TeamAPedroPathRoute route) {
        requireFollower();
        if (route == null) throw new IllegalArgumentException("Select a Team A Pedro route first.");
        follower.stop();
        manualStarted = false;
        follower.follow(route.build(follower));
    }

    @Override public void updateManualDrive(double forward, double strafe, double rotate) {
        requireFollower();
        follower.manual(forward, strafe, rotate);
        manualStarted = true;
        follower.update();
    }

    @Override public void updatePathFollowing() {
        requireFollower();
        manualStarted = false;
        follower.update();
    }

    @Override public void stop() {
        if (follower != null) follower.stop();
        manualStarted = false;
    }

    @Override public PoseEstimate getPoseEstimate() {
        if (follower == null) return PoseEstimate.unavailable();
        Pose pose = follower.pose();
        return PoseEstimate.available(pose.x(), pose.y(), pose.heading());
    }

    @Override public boolean isPathFollowingActive() {
        return follower != null && follower.isBusy();
    }

    private void requireFollower() {
        if (follower == null) throw new IllegalStateException(
                "Initialize Team A Pedro robot before driving.");
    }
}
