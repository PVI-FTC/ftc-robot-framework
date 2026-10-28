package org.firstinspires.ftc.teamcode.robots.team3;

import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.common.hardware.DriveHardware;
import org.firstinspires.ftc.teamcode.common.localization.PoseEstimate;
import org.firstinspires.ftc.teamcode.common.subsystems.drive.DriveController;
import org.firstinspires.ftc.teamcode.common.subsystems.drive.MecanumDriveController;

/**
 * Team 3 drivetrain controller that provides real-time closed-loop pose stabilization.
 *
 * <p>Wraps the standard mecanum drive calculation and applies active error correction
 * via {@link Team3PoseLock} using the goBILDA Pinpoint Odometry Computer.</p>
 */
public class Team3StabilizedDriveController implements DriveController {
    private final MecanumDriveController mecanumDriveController;
    private final Team3PinpointHardware pinpointHardware;
    private final Team3PoseLock poseLock = new Team3PoseLock();
    private final ElapsedTime loopTimer = new ElapsedTime();
    private double lastLoopTimeMs;

    /**
     * Creates a stabilized drive controller for Team 3.
     *
     * @param driveHardware hardware wrapper for the four mecanum drive motors
     * @param pinpointHardware hardware wrapper for the Pinpoint odometry computer
     */
    public Team3StabilizedDriveController(DriveHardware driveHardware, Team3PinpointHardware pinpointHardware) {
        if (driveHardware == null) {
            throw new IllegalArgumentException("Stabilized drive controller needs drive hardware.");
        }
        if (pinpointHardware == null) {
            throw new IllegalArgumentException("Stabilized drive controller needs pinpoint hardware.");
        }

        this.mecanumDriveController = new MecanumDriveController(driveHardware);
        this.pinpointHardware = pinpointHardware;
        this.loopTimer.reset();
        this.lastLoopTimeMs = 0.0;
    }

    @Override
    public void updateManualDrive(double forward, double strafe, double rotate) {
        // Read Pinpoint odometry once per loop right before motor calculations
        pinpointHardware.update();

        double dtSeconds = loopTimer.seconds();
        loopTimer.reset();
        lastLoopTimeMs = dtSeconds * 1000.0;

        if (pinpointHardware.isAvailable()) {
            double[] stabilized = poseLock.update(
                    forward,
                    strafe,
                    rotate,
                    pinpointHardware.getXInches(),
                    pinpointHardware.getYInches(),
                    pinpointHardware.getHeadingRadians(),
                    dtSeconds);

            double cmdFwd = stabilized[0];
            double cmdStrafe = stabilized[1];
            double cmdRotate = stabilized[2];

            // Heading Priority Desaturation:
            // Ensure rotation correction is never starved by translation saturation.
            // When translating and correcting heading simultaneously, translation is scaled
            // down to preserve full rotation authority so strafing cannot cause unwanted yaw.
            double maxTrans = Math.max(0.20, 1.0 - Math.abs(cmdRotate));
            double transMag = Math.max(Math.abs(cmdFwd), Math.abs(cmdStrafe));
            if (transMag > maxTrans && transMag > 0.0) {
                double scale = maxTrans / transMag;
                cmdFwd *= scale;
                cmdStrafe *= scale;
            }

            mecanumDriveController.updateManualDrive(cmdFwd, cmdStrafe, cmdRotate);
        } else {
            mecanumDriveController.updateManualDrive(forward, strafe, rotate);
        }
    }

    @Override
    public void updatePathFollowing() {
        mecanumDriveController.updatePathFollowing();
    }

    @Override
    public void stop() {
        mecanumDriveController.stop();
        if (pinpointHardware.isAvailable()) {
            poseLock.resetAnchor(
                    pinpointHardware.getXInches(),
                    pinpointHardware.getYInches(),
                    pinpointHardware.getHeadingRadians());
        }
    }

    /**
     * Re-anchors the pose lock to current robot physical coordinates and commands zero motor power.
     */
    public void brake() {
        mecanumDriveController.stop();
        if (pinpointHardware.isAvailable()) {
            poseLock.resetAnchor(
                    pinpointHardware.getXInches(),
                    pinpointHardware.getYInches(),
                    pinpointHardware.getHeadingRadians());
        }
    }

    @Override
    public PoseEstimate getPoseEstimate() {
        if (!pinpointHardware.isAvailable()) {
            return PoseEstimate.unavailable();
        }
        return PoseEstimate.available(
                pinpointHardware.getXInches(),
                pinpointHardware.getYInches(),
                pinpointHardware.getHeadingRadians());
    }

    @Override
    public boolean isPathFollowingActive() {
        return false;
    }

    /** Returns the underlying pose lock math controller. */
    public Team3PoseLock getPoseLock() {
        return poseLock;
    }

    /** Returns the duration of the last control loop in milliseconds. */
    public double getLastLoopTimeMs() {
        return lastLoopTimeMs;
    }
}
