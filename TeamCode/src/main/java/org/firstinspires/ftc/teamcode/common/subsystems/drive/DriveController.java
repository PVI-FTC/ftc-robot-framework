package org.firstinspires.ftc.teamcode.common.subsystems.drive;

import org.firstinspires.ftc.teamcode.common.localization.PoseEstimate;

/**
 * Supplies drive output and optional localization without exposing a vendor library to the drive FSM.
 */
public interface DriveController {
    /** Updates manual robot-relative drive output for one FTC loop. */
    void updateManualDrive(double forward, double strafe, double rotate);

    /**
     * Updates driver translation while maintaining a captured heading when the controller supports
     * localization.
     *
     * <p>Controllers without a heading source retain ordinary manual-drive behavior. This keeps
     * the shared drivetrain usable on robots that do not have an IMU or localizer.</p>
     */
    default void updateHeadingHold(double forward, double strafe, double rotate) {
        updateManualDrive(forward, strafe, rotate);
    }

    /** Captures the heading that a localization-capable controller should maintain. */
    default void startHeadingHold() {
        // Simple controllers have no heading source to capture.
    }

    /** Starts heading hold toward a requested absolute heading when supported. */
    default void startHeadingHold(double targetHeadingRadians) {
        startHeadingHold();
    }

    /** Updates the active heading target without changing the drive mode. */
    default void setHeadingTarget(double targetHeadingRadians) {
        // Controllers without a heading source ignore preset targets.
    }

    /** Updates an already-requested path-following operation for one FTC loop. */
    void updatePathFollowing();

    /** Cancels drive output and leaves the controller safe. */
    void stop();

    /** Returns the most recent pose estimate, or an unavailable estimate when none exists. */
    PoseEstimate getPoseEstimate();

    /** Returns whether the controller is still following a requested path. */
    boolean isPathFollowingActive();
}
