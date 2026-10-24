package org.firstinspires.ftc.vision.apriltag;

import android.graphics.Canvas;

import org.firstinspires.ftc.robotcore.external.matrices.GeneralMatrixF;
import org.firstinspires.ftc.robotcore.external.matrices.MatrixF;
import org.firstinspires.ftc.robotcore.external.matrices.OpenGLMatrix;
import org.firstinspires.ftc.robotcore.external.matrices.VectorF;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.AxesOrder;
import org.firstinspires.ftc.robotcore.external.navigation.AxesReference;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.robotcore.external.navigation.Orientation;
import org.firstinspires.ftc.robotcore.external.navigation.Pose3D;
import org.firstinspires.ftc.robotcore.external.navigation.Position;
import org.firstinspires.ftc.robotcore.external.navigation.Quaternion;
import org.firstinspires.ftc.robotcore.external.navigation.YawPitchRollAngles;
import org.firstinspires.ftc.robotcore.internal.camera.calibration.CameraCalibration;
import org.opencv.core.Mat;

import java.util.ArrayList;
import java.util.List;

import pvi.harness.hardware.SeenAprilTag;

/**
 * HARNESS FILE, not the FTC SDK. The harness's own copy of the SDK class with this name.
 *
 * <p>The real {@code AprilTagProcessorImpl} finds tags in camera pictures with native (C)
 * code, which can't run on a laptop. The SDK's own {@code AprilTagProcessor.Builder.build()}
 * still runs unchanged and creates this class instead, with the same constructor. Its pictures
 * come from a {@link pvi.harness.hardware.FakeWebcam} as plain data ({@link SeenAprilTag}s),
 * and it turns them into the same {@link AprilTagSingleDetection}s the SDK would make:</p>
 *
 * <ul>
 *   <li>{@code metadata} comes from the tag library robot code chose (unknown tags get
 *       {@code null} metadata and no pose, as in the SDK);</li>
 *   <li>{@code rawPose}, {@code ftcPose} and {@code robotPose} use the SDK's own formulas,
 *       copied below, including the camera position given to {@code setCameraPose(...)}
 *       and the output units given to {@code setOutputUnits(...)};</li>
 *   <li>{@code getFreshDetections()} returns each picture once, then {@code null} until the
 *       next picture, exactly like the SDK.</li>
 * </ul>
 *
 * <p>Not modelled: tag clusters, pixel corners and center (left {@code null}), hamming
 * (always 0) and decision margin (always {@link #FAKE_DECISION_MARGIN}).</p>
 *
 * <p>Java uses this class instead of the SDK's because the harness's classes come first on
 * the class path. The constructor and public methods must keep matching the SDK version the
 * harness builds against ({@code Vision:12.0.0} in {@code build.gradle}).</p>
 */
public class AprilTagProcessorImpl extends AprilTagProcessor {

    /** The SDK's "how clearly was the tag seen" number; a clear sighting, made up. */
    public static final float FAKE_DECISION_MARGIN = 100f;

    /** Where the robot center is, seen from the camera (from setCameraPose), as the SDK keeps it. */
    private final OpenGLMatrix robotInCameraFrame;
    private final DistanceUnit outputUnitsLength;
    private final AngleUnit outputUnitsAngle;
    private final AprilTagLibrary tagLibrary;

    /** Detections in the most recent picture. */
    private ArrayList<AprilTagDetection> detections = new ArrayList<>();
    /** Detections not yet collected with getFreshDetections(), or null when none are new. */
    private ArrayList<AprilTagDetection> detectionsUpdate = new ArrayList<>();

    /** The same constructor the SDK's AprilTagProcessor.Builder calls. Settings for drawing and threads are ignored. */
    public AprilTagProcessorImpl(OpenGLMatrix robotInCameraFrame, double fx, double fy, double cx, double cy,
                                 DistanceUnit outputUnitsLength, AngleUnit outputUnitsAngle, AprilTagLibrary tagLibrary,
                                 boolean drawAxes, boolean drawCube, boolean drawOutline, boolean drawTagID,
                                 TagFamily tagFamily, int threads, boolean suppressCalibrationWarnings) {
        this.robotInCameraFrame = robotInCameraFrame;
        this.outputUnitsLength = outputUnitsLength;
        this.outputUnitsAngle = outputUnitsAngle;
        this.tagLibrary = tagLibrary;
    }

    /**
     * HARNESS ONLY: one new picture from the fake webcam. The harness's VisionPortal calls this
     * for each picture while this processor is enabled.
     *
     * @param tagsInView     the tags in the picture
     * @param frameTimeNanos when the picture was taken (robot code's System.nanoTime() clock)
     */
    public void receiveHarnessFrame(List<SeenAprilTag> tagsInView, long frameTimeNanos) {
        ArrayList<AprilTagDetection> found = new ArrayList<>();
        for (SeenAprilTag tag : tagsInView) {
            found.add(detectionFor(tag, frameTimeNanos));
        }
        detections = found;
        detectionsUpdate = found;
    }

    /** Builds the detection the SDK would make for this tag, using the SDK's own math. */
    private AprilTagSingleDetection detectionFor(SeenAprilTag tag, long frameTimeNanos) {
        AprilTagMetadata metadata = tagLibrary.lookupTag(tag.id());
        AprilTagPoseRaw rawPose = null;
        AprilTagPoseFtc ftcPose = null;
        Pose3D robotPose = null;
        // As in the SDK: without metadata the tag's size is unknown, so there is no pose at all.
        if (metadata != null) {
            rawPose = rawPoseFor(tag);
            ftcPose = rawPoseToFtcPose(rawPose);
            robotPose = computeRobotPose(rawPose, metadata.fieldPosition, metadata.fieldOrientation, frameTimeNanos);
        }
        return new AprilTagSingleDetection(tag.id(), 0, FAKE_DECISION_MARGIN, null, null,
                metadata, ftcPose, rawPose, robotPose, frameTimeNanos, outputUnitsLength);
    }

    /**
     * The tag's pose in the camera's own (OpenCV) frame, the way the SDK's pose solver reports
     * it: x right, y down, z out of the lens, in the output units. This is the reverse of the
     * SDK's rawPoseToFtcPose below, so robot code gets back exactly the numbers the script gave.
     */
    private AprilTagPoseRaw rawPoseFor(SeenAprilTag tag) {
        OpenGLMatrix rotation4 = new Orientation(AxesReference.INTRINSIC, AxesOrder.YXZ, AngleUnit.DEGREES,
                (float) -tag.yawDegrees(), (float) tag.pitchDegrees(), (float) tag.rollDegrees(), 0)
                .getRotationMatrix();
        MatrixF rotation = new GeneralMatrixF(3, 3);
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                rotation.put(row, column, rotation4.get(row, column));
            }
        }
        return new AprilTagPoseRaw(
                outputUnitsLength.fromUnit(DistanceUnit.INCH, tag.rightInches()),
                outputUnitsLength.fromUnit(DistanceUnit.INCH, -tag.upInches()),
                outputUnitsLength.fromUnit(DistanceUnit.INCH, tag.forwardInches()),
                rotation);
    }

    // ---- Copied from the SDK's AprilTagProcessorImpl (Vision 12.0.0), unchanged ----------------

    private AprilTagPoseFtc rawPoseToFtcPose(AprilTagPoseRaw rawPose) {
        Orientation rot = Orientation.getOrientation(rawPose.R, AxesReference.INTRINSIC, AxesOrder.YXZ, outputUnitsAngle);

        return new AprilTagPoseFtc(
                rawPose.x,  // x   NB: These are *intentionally* not matched directly;
                rawPose.z,  // y       this is the mapping between the AprilTag coordinate
                -rawPose.y, // z       system and the FTC coordinate system
                -rot.firstAngle, // yaw
                rot.secondAngle, // pitch
                rot.thirdAngle,  // roll
                Math.hypot(rawPose.x, rawPose.z), // range
                outputUnitsAngle.fromUnit(AngleUnit.RADIANS, Math.atan2(-rawPose.x, rawPose.z)), // bearing
                outputUnitsAngle.fromUnit(AngleUnit.RADIANS, Math.atan2(-rawPose.y, rawPose.z))); // elevation
    }

    private Pose3D computeRobotPose(AprilTagPoseRaw rawPose, VectorF fieldPosition, Quaternion fieldOrientation, long acquisitionTime) {
        // Compute transformation matrix of tag pose in field reference frame
        float tagInFieldX = fieldPosition.get(0);
        float tagInFieldY = fieldPosition.get(1);
        float tagInFieldZ = fieldPosition.get(2);
        OpenGLMatrix tagInFieldR = new OpenGLMatrix(fieldOrientation.toMatrix());
        OpenGLMatrix tagInFieldFrame = OpenGLMatrix.identityMatrix()
                .translated(tagInFieldX, tagInFieldY, tagInFieldZ)
                .multiplied(tagInFieldR);

        // Compute transformation matrix of camera pose in tag reference frame
        float tagInCameraX = (float) DistanceUnit.INCH.fromUnit(outputUnitsLength, rawPose.x);
        float tagInCameraY = (float) DistanceUnit.INCH.fromUnit(outputUnitsLength, rawPose.y);
        float tagInCameraZ = (float) DistanceUnit.INCH.fromUnit(outputUnitsLength, rawPose.z);
        OpenGLMatrix tagInCameraR = new OpenGLMatrix((rawPose.R));
        OpenGLMatrix cameraInTagFrame = OpenGLMatrix.identityMatrix()
                .translated(tagInCameraX, tagInCameraY, tagInCameraZ)
                .multiplied(tagInCameraR)
                .inverted();

        // Compute transformation matrix of robot pose in field frame
        OpenGLMatrix robotInFieldFrame =
                tagInFieldFrame
                        .multiplied(cameraInTagFrame)
                        .multiplied(robotInCameraFrame);

        // Extract robot location
        VectorF robotInFieldTranslation = robotInFieldFrame.getTranslation();
        Position robotPosition = new Position(DistanceUnit.INCH,
                robotInFieldTranslation.get(0),
                robotInFieldTranslation.get(1),
                robotInFieldTranslation.get(2),
                acquisitionTime).toUnit(outputUnitsLength);

        // Extract robot orientation
        Orientation robotInFieldOrientation = Orientation.getOrientation(robotInFieldFrame,
                AxesReference.INTRINSIC, AxesOrder.ZXY, outputUnitsAngle);
        YawPitchRollAngles robotOrientation = new YawPitchRollAngles(outputUnitsAngle,
                robotInFieldOrientation.firstAngle,
                robotInFieldOrientation.secondAngle,
                robotInFieldOrientation.thirdAngle,
                acquisitionTime);

        return new Pose3D(robotPosition, robotOrientation);
    }

    // ---- The rest of the SDK's AprilTagProcessor API -----------------------------------------

    @Override
    public ArrayList<AprilTagDetection> getDetections() {
        return detections;
    }

    @Override
    public ArrayList<AprilTagDetection> getFreshDetections() {
        ArrayList<AprilTagDetection> fresh = detectionsUpdate;
        detectionsUpdate = null;
        return fresh;
    }

    /** No pictures to shrink: the setting is accepted and has no effect. */
    @Override public void setDecimation(float decimation) { }
    /** No pose solving happens: the setting is accepted and has no effect. */
    @Override public void setPoseSolver(PoseSolver poseSolver) { }
    @Override public int getPerTagAvgPoseSolveTime() { return 0; }

    /** Real camera pictures never arrive in the harness; FakeWebcam sends plain data instead. */
    @Override public void init(int width, int height, CameraCalibration calibration) { }
    @Override public Object processFrame(Mat input, long captureTimeNanos) { return null; }
    @Override public void onDrawFrame(Canvas canvas, int onscreenWidth, int onscreenHeight,
                                      float scaleBmpPxToCanvasPx, float scaleCanvasDensity, Object userContext) { }
}
