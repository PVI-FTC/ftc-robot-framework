package org.firstinspires.ftc.teamcode.robots.teamA;

import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.teamcode.common.hardware.CameraFrameCaptureHardware;
import org.firstinspires.ftc.teamcode.core.robot.Robot;

/** Separate, stationary Team A camera-calibration composition. */
public class TeamACameraCalibrationRobot extends Robot {
    private final CameraFrameCaptureHardware camera;
    private boolean hardwareInitialized;

    public TeamACameraCalibrationRobot(String webcamHardwareName) {
        camera = new CameraFrameCaptureHardware(webcamHardwareName);
    }

    public void initialize(HardwareMap hardwareMap) {
        if (hardwareInitialized) {
            return;
        }
        camera.initialize(hardwareMap);
        hardwareInitialized = true;
        super.initialize();
    }

    public boolean requestFrameCapture() {
        return camera.requestFrameCapture();
    }

    public String getCameraStateName() {
        return camera.getCameraStateName();
    }

    public String getLastCaptureRequest() {
        return camera.getLastCaptureRequest();
    }

    @Override
    protected void onStop() {
        camera.stop();
    }
}
