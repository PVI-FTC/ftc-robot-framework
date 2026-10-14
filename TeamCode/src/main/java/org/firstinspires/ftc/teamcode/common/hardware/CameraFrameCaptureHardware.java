package org.firstinspires.ftc.teamcode.common.hardware;

import android.util.Size;

import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.vision.VisionPortal;

import java.util.Locale;

/** Camera-only frame capture for supervised calibration; no AprilTag or motor behavior. */
public class CameraFrameCaptureHardware {
    private final String webcamHardwareName;
    private VisionPortal portal;
    private String lastCaptureRequest = "None";

    public CameraFrameCaptureHardware(String webcamHardwareName) {
        this.webcamHardwareName = webcamHardwareName;
    }

    public void initialize(HardwareMap hardwareMap) {
        stop();
        WebcamName webcam = hardwareMap.get(WebcamName.class, webcamHardwareName);
        portal = new VisionPortal.Builder()
                .setCamera(webcam)
                .setCameraResolution(new Size(640, 480))
                .build();
    }

    /** Requests one raw frame; the asynchronous save must still be verified on the Control Hub. */
    public boolean requestFrameCapture() {
        if (portal == null || portal.getCameraState() != VisionPortal.CameraState.STREAMING) {
            return false;
        }
        String name = String.format(Locale.US, "TeamA-C920-640x480-%d",
                System.currentTimeMillis());
        portal.saveNextFrameRaw(name);
        lastCaptureRequest = name;
        return true;
    }

    public String getCameraStateName() {
        return portal == null ? "Unavailable" : portal.getCameraState().name();
    }

    public String getLastCaptureRequest() {
        return lastCaptureRequest;
    }

    public void stop() {
        if (portal != null) {
            portal.close();
            portal = null;
        }
    }
}
