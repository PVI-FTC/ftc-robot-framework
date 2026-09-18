package org.firstinspires.ftc.teamcode.robots.team3;

import com.pedropathing.follower.Follower;
import com.pedropathing.geometry.Pose;
import org.firstinspires.ftc.teamcode.common.localization.PoseEstimate;
import org.firstinspires.ftc.teamcode.common.subsystems.drive.DriveController;

/** Team 3 adapter that keeps Pedro types out of the shared drive API. */
public final class Team3PedroDriveController implements DriveController {
    private Follower follower;
    private boolean teleOpStarted;

    void setFollower(Follower follower) {
        if (follower == null) throw new IllegalArgumentException("Pedro drive controller needs a follower.");
        this.follower = follower;
    }

    void preparePath(Team3PedroPathRoute route) {
        requireFollower();
        if (route == null) throw new IllegalArgumentException("Select a Team 3 Pedro route first.");
        follower.breakFollowing();
        teleOpStarted = false;
        follower.setStartingPose(route.createStartPose());
    }

    void startPath(Team3PedroPathRoute route) {
        requireFollower();
        if (route == null) throw new IllegalArgumentException("Select a Team 3 Pedro route first.");
        follower.breakFollowing();
        teleOpStarted = false;
        follower.followPath(route.build(follower));
    }

    @Override public void updateManualDrive(double forward, double strafe, double rotate) {
        requireFollower();
        if (!teleOpStarted) {
            follower.startTeleOpDrive();
            teleOpStarted = true;
            follower.setTeleOpDrive(forward, strafe, rotate, true);
            return;
        }
        follower.setTeleOpDrive(forward, strafe, rotate, true);
        follower.update();
    }
    @Override public void updatePathFollowing() { requireFollower(); teleOpStarted = false; follower.update(); }
    @Override public void stop() {
        if (follower != null) {
            follower.breakFollowing();
            follower.updatePose();
        }
        teleOpStarted = false;
    }
    @Override public PoseEstimate getPoseEstimate() {
        if (follower == null) return PoseEstimate.unavailable();
        Pose pose = follower.getPose();
        return PoseEstimate.available(pose.getX(), pose.getY(), pose.getHeading());
    }
    @Override public boolean isPathFollowingActive() { return follower != null && follower.isBusy(); }
    private void requireFollower() { if (follower == null) throw new IllegalStateException("Initialize Team 3 Pedro robot before driving."); }
}
