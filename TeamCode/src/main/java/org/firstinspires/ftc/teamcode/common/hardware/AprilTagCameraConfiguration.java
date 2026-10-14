package org.firstinspires.ftc.teamcode.common.hardware;

/** Neutral configuration facts for one VisionPortal AprilTag camera. */
public final class AprilTagCameraConfiguration {
    private final String webcamHardwareName;
    private final int streamWidth;
    private final int streamHeight;
    private final boolean calibrationVerified;
    private final boolean mountVerified;
    private final String robotFrameName;
    private final double mountRightInches;
    private final double mountForwardInches;
    private final double mountUpInches;
    private final double mountYawDegrees;
    private final double mountPitchDegrees;
    private final double mountRollDegrees;

    public AprilTagCameraConfiguration(String webcamHardwareName, int streamWidth,
                                       int streamHeight, boolean calibrationVerified,
                                       boolean mountVerified, String robotFrameName,
                                       double mountRightInches, double mountForwardInches,
                                       double mountUpInches, double mountYawDegrees,
                                       double mountPitchDegrees, double mountRollDegrees) {
        if (webcamHardwareName == null || webcamHardwareName.isEmpty()) {
            throw new IllegalArgumentException("Camera configuration needs a hardware name.");
        }
        if (streamWidth <= 0 || streamHeight <= 0) {
            throw new IllegalArgumentException("Camera stream dimensions must be positive.");
        }
        if (robotFrameName == null || robotFrameName.isEmpty()) {
            throw new IllegalArgumentException("Camera configuration needs a robot-frame name.");
        }
        if (!allFinite(mountRightInches, mountForwardInches, mountUpInches,
                mountYawDegrees, mountPitchDegrees, mountRollDegrees)) {
            throw new IllegalArgumentException("Camera mount values must be finite.");
        }

        this.webcamHardwareName = webcamHardwareName;
        this.streamWidth = streamWidth;
        this.streamHeight = streamHeight;
        this.calibrationVerified = calibrationVerified;
        this.mountVerified = mountVerified;
        this.robotFrameName = robotFrameName;
        this.mountRightInches = mountRightInches;
        this.mountForwardInches = mountForwardInches;
        this.mountUpInches = mountUpInches;
        this.mountYawDegrees = mountYawDegrees;
        this.mountPitchDegrees = mountPitchDegrees;
        this.mountRollDegrees = mountRollDegrees;
    }

    private static boolean allFinite(double... values) {
        for (double value : values) {
            if (Double.isNaN(value) || Double.isInfinite(value)) {
                return false;
            }
        }
        return true;
    }

    public String getWebcamHardwareName() { return webcamHardwareName; }
    public int getStreamWidth() { return streamWidth; }
    public int getStreamHeight() { return streamHeight; }
    public boolean isCalibrationVerified() { return calibrationVerified; }
    public boolean isMountVerified() { return mountVerified; }
    public String getRobotFrameName() { return robotFrameName; }
    public double getMountRightInches() { return mountRightInches; }
    public double getMountForwardInches() { return mountForwardInches; }
    public double getMountUpInches() { return mountUpInches; }
    public double getMountYawDegrees() { return mountYawDegrees; }
    public double getMountPitchDegrees() { return mountPitchDegrees; }
    public double getMountRollDegrees() { return mountRollDegrees; }
}
