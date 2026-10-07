package org.firstinspires.ftc.teamcode.common.hardware;

import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.teamcode.common.vision.AprilTagObservationSnapshot;

/**
 * Internal hardware-layer boundary for one AprilTag-capable camera source.
 *
 * <p>A source accesses its own hardware, collects neutral observations, and releases its own
 * resources. It does not read gamepads, select vision states, or command a robot.</p>
 */
interface AprilTagVisionSource {
    void initialize(HardwareMap hardwareMap);
    void update();
    void stop();
    boolean isAvailable();
    AprilTagObservationSnapshot getSnapshot();
}
