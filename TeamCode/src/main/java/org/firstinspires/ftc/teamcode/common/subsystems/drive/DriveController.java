package org.firstinspires.ftc.teamcode.common.subsystems.drive;

import org.firstinspires.ftc.teamcode.common.localization.PoseEstimate;

/**
 * Supplies drive output and optional localization without exposing a vendor library to the drive FSM.
 */
public interface DriveController {
    /** Updates manual robot-relative drive output for one FTC loop. */
    void updateManualDrive(double forward, double strafe, double rotate);

    /** Updates an already-requested path-following operation for one FTC loop. */
    void updatePathFollowing();

    /** Cancels drive output and leaves the controller safe. */
    void stop();

    /** Returns the most recent pose estimate, or an unavailable estimate when none exists. */
    PoseEstimate getPoseEstimate();

    /** Returns whether the controller is still following a requested path. */
    boolean isPathFollowingActive();
}