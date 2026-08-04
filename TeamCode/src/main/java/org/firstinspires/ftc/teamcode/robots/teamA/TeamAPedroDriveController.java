package org.firstinspires.ftc.teamcode.robots.teamA;

import com.pedropathing.follower.Follower;
import com.pedropathing.geometry.Pose;
import org.firstinspires.ftc.teamcode.common.localization.PoseEstimate;
import org.firstinspires.ftc.teamcode.common.subsystems.drive.DriveController;

/** Team A adapter that keeps Pedro types out of the shared drive API. */
public final class TeamAPedroDriveController implements DriveController {
    private Follower follower;
    private boolean teleOpStarted;

    void setFollower(Follower follower) {
        if (follower == null) throw new IllegalArgumentException("Pedro drive controller needs a follower.");
        this.follower = follower;
    }

    @Override public void updateManualDrive(double forward, double strafe, double rotate) {
        requireFollower();
        follower.setTeleOpDrive(forward, strafe, rotate, true);
        if (!teleOpStarted) {
            // Pedro performs this loop's first follower update during startup.
            follower.startTeleOpDrive();
            teleOpStarted = true;
            return;
        }
        follower.update();
    }
    @Override public void updatePathFollowing() { requireFollower(); teleOpStarted = false; follower.update(); }
    @Override public void stop() { if (follower != null) follower.breakFollowing(); teleOpStarted = false; }
    @Override public PoseEstimate getPoseEstimate() {
        if (follower == null) return PoseEstimate.unavailable();
        Pose pose = follower.getPose();
        return PoseEstimate.available(pose.getX(), pose.getY(), pose.getHeading());
    }
    @Override public boolean isPathFollowingActive() { return follower != null && follower.isBusy(); }
    private void requireFollower() { if (follower == null) throw new IllegalStateException("Initialize Team A Pedro robot before driving."); }
}