package org.firstinspires.ftc.teamcode.common.hardware;

/**
 * Placeholder lifecycle for optional vision hardware.
 *
 * <p>This baseline deliberately does not create or access a camera, VisionPortal, OpenCV, or
 * AprilTag processor. It remains unavailable until a later implementation supplies one.</p>
 */
public class VisionHardware {
    /** Starts the safe no-camera vision lifecycle. */
    public void initialize() {
        // Vision support is intentionally deferred.
    }

    /** Performs one no-op vision lifecycle update. */
    public void update() {
        // Vision support is intentionally deferred.
    }

    /** Stops any future vision work; there is no baseline camera resource to release. */
    public void stop() {
        // Vision support is intentionally deferred.
    }

    /** Returns false because this baseline does not initialize a vision device. */
    public boolean isAvailable() {
        return false;
    }
}
