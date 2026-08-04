package org.firstinspires.ftc.teamcode.common.subsystems.drive;

import org.firstinspires.ftc.teamcode.common.hardware.DriveHardware;
import org.firstinspires.ftc.teamcode.common.localization.PoseEstimate;

/**
 * The baseline mecanum implementation of the library-neutral drive controller.
 *
 * <p>This controller deliberately has no localization or path-following capability. It keeps
 * simple robots safe by stopping when path-following is requested.</p>
 */
public class MecanumDriveController implements DriveController {
    private final DriveHardware driveHardware;

    public MecanumDriveController(DriveHardware driveHardware) {
        if (driveHardware == null) {
            throw new IllegalArgumentException("Mecanum drive controller needs drive hardware.");
        }
        this.driveHardware = driveHardware;
    }

    @Override
    public void updateManualDrive(double forward, double strafe, double rotate) {
        double frontLeft = forward + strafe + rotate;
        double frontRight = forward - strafe - rotate;
        double rearLeft = forward - strafe + rotate;
        double rearRight = forward + strafe - rotate;

        double largestMagnitude = Math.max(Math.abs(frontLeft), Math.abs(frontRight));
        largestMagnitude = Math.max(largestMagnitude, Math.abs(rearLeft));
        largestMagnitude = Math.max(largestMagnitude, Math.abs(rearRight));

        if (largestMagnitude > 1.0) {
            frontLeft /= largestMagnitude;
            frontRight /= largestMagnitude;
            rearLeft /= largestMagnitude;
            rearRight /= largestMagnitude;
        }

        driveHardware.setMotorPowers(frontLeft, frontRight, rearLeft, rearRight);
    }

    @Override
    public void updatePathFollowing() {
        stop();
    }

    @Override
    public void stop() {
        driveHardware.stop();
    }

    @Override
    public PoseEstimate getPoseEstimate() {
        return PoseEstimate.unavailable();
    }

    @Override
    public boolean isPathFollowingActive() {
        return false;
    }
}