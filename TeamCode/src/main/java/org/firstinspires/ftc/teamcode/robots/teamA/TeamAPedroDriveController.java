package org.firstinspires.ftc.teamcode.robots.teamA;

import com.pedropathing.follower.Follower;
import com.pedropathing.geometry.Pose;
import org.firstinspires.ftc.teamcode.common.localization.PoseEstimate;
import org.firstinspires.ftc.teamcode.common.subsystems.drive.DriveController;

/** Team A adapter that keeps Pedro types out of the shared drive API. */
public final class TeamAPedroDriveController implements DriveController {
    /** Recorded Team A Pedro heading P gain, limited by the application's 0.20 power ceiling. */
    private static final double HEADING_HOLD_P_GAIN = 2.2;
    private static final double MAX_HEADING_HOLD_ROTATION = 1.0;

    private Follower follower;
    private boolean teleOpStarted;
    private double heldHeadingRadians;
    private boolean headingHoldActive;

    void setFollower(Follower follower) {
        if (follower == null) throw new IllegalArgumentException("Pedro drive controller needs a follower.");
        this.follower = follower;
    }

    void preparePath(TeamAPedroPathRoute route) {
        requireFollower();
        if (route == null) throw new IllegalArgumentException("Select a Team A Pedro route first.");
        follower.breakFollowing();
        teleOpStarted = false;
        follower.setStartingPose(route.createStartPose());
    }

    void startPath(TeamAPedroPathRoute route) {
        requireFollower();
        if (route == null) throw new IllegalArgumentException("Select a Team A Pedro route first.");
        follower.breakFollowing();
        teleOpStarted = false;
        follower.followPath(route.build(follower));
    }

    @Override public void updateManualDrive(double forward, double strafe, double rotate) {
        requireFollower();
        headingHoldActive = false;
        if (!teleOpStarted) {
            // Pedro initializes its internal teleop pose during startup. Starting before setting
            // field-oriented input avoids reading that pose while it is still null.
            follower.startTeleOpDrive();
            teleOpStarted = true;
            follower.setTeleOpDrive(forward, strafe, rotate, true);
            return;
        }
        follower.setTeleOpDrive(forward, strafe, rotate, true);
        follower.update();
    }

    @Override public void startHeadingHold() {
        requireFollower();
        follower.startTeleOpDrive();
        teleOpStarted = true;

        Pose pose = follower.getPose();
        if (isFinite(pose.getHeading())) {
            heldHeadingRadians = pose.getHeading();
            headingHoldActive = true;
        } else {
            headingHoldActive = false;
        }
    }

    @Override public void updateHeadingHold(double forward, double strafe, double rotate) {
        requireFollower();
        if (!teleOpStarted) {
            startHeadingHold();
        }

        Pose pose = follower.getPose();
        if (!headingHoldActive || !isFinite(pose.getHeading())) {
            // A missing or invalid heading must not leave a previous rotation command active.
            follower.setTeleOpDrive(0.0, 0.0, 0.0, true);
            follower.update();
            return;
        }

        double headingError = normalizeSignedAngle(heldHeadingRadians - pose.getHeading());
        double correction = clamp(HEADING_HOLD_P_GAIN * headingError,
                -MAX_HEADING_HOLD_ROTATION, MAX_HEADING_HOLD_ROTATION);
        follower.setTeleOpDrive(forward, strafe, correction, true);
        follower.update();
    }
    @Override public void updatePathFollowing() { requireFollower(); teleOpStarted = false; follower.update(); }
    @Override public void stop() {
        if (follower != null) {
            follower.breakFollowing();
            // Keep localization current while the disabled state commands zero drivetrain output.
            follower.updatePose();
        }
        teleOpStarted = false;
        headingHoldActive = false;
    }
    @Override public PoseEstimate getPoseEstimate() {
        if (follower == null) return PoseEstimate.unavailable();
        Pose pose = follower.getPose();
        return PoseEstimate.available(pose.getX(), pose.getY(), pose.getHeading());
    }
    @Override public boolean isPathFollowingActive() { return follower != null && follower.isBusy(); }
    private void requireFollower() { if (follower == null) throw new IllegalStateException("Initialize Team A Pedro robot before driving."); }

    private double normalizeSignedAngle(double angleRadians) {
        return Math.atan2(Math.sin(angleRadians), Math.cos(angleRadians));
    }

    private double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private boolean isFinite(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }
}
