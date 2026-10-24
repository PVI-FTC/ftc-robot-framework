package org.firstinspires.ftc.vision;

import android.util.Size;

import org.firstinspires.ftc.robotcore.external.function.Consumer;
import org.firstinspires.ftc.robotcore.external.function.Continuation;
import org.firstinspires.ftc.robotcore.external.hardware.camera.BuiltinCameraDirection;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraName;
import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.robotcore.external.hardware.camera.controls.CameraControl;
import org.firstinspires.ftc.robotcore.external.stream.CameraStreamSource;
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessorImpl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import pvi.harness.hardware.FakeWebcam;
import pvi.harness.hardware.SeenAprilTag;

/**
 * HARNESS FILE, not the FTC SDK. The harness's own copy of the SDK class with this name.
 *
 * <p>The real {@code VisionPortal} opens a camera through Android and shows its picture on the
 * Robot Controller screen; just loading it asks Android for the screen layout, which fails on a
 * laptop. This copy has the same public methods, so TeamCode compiles and runs unchanged, but
 * {@link Builder#build()} connects the portal to a {@link FakeWebcam} from the fake hardware
 * map instead. Each picture the webcam takes goes to every enabled AprilTag processor in the
 * portal (see the harness's {@code AprilTagProcessorImpl}).</p>
 *
 * <p>What it models: building needs a camera (same error as the SDK), streaming starts on
 * build unless turned off, {@code stopStreaming()} / {@code resumeStreaming()} and
 * {@code setProcessorEnabled(...)} stop and start pictures at once (the SDK takes a moment),
 * and {@code close()} disconnects the webcam. Live view, camera controls, frame saving and the
 * phone's built-in cameras do nothing or are not available.</p>
 *
 * <p>Java uses this class instead of the SDK's because the harness's classes come first on
 * the class path. Its public methods must keep matching the SDK version the harness builds
 * against ({@code Vision:12.0.0} in {@code build.gradle}).</p>
 */
public abstract class VisionPortal implements CameraStreamSource {

    /** The SDK reads this from the Robot Controller app's screen layout; there is no screen here. */
    public static final int DEFAULT_VIEW_CONTAINER_ID = 0;

    /** Only applicable if using a webcam. Accepted and ignored by the harness. */
    public enum StreamFormat { YUY2, MJPEG }

    /** How live views share the screen. Accepted and ignored by the harness. */
    public enum MultiPortalLayout { VERTICAL, HORIZONTAL }

    /** No screen to split in the harness: every portal gets "no live view" (0). */
    public static int[] makeMultiPortalView(int numPortals, MultiPortalLayout mpl) {
        return new int[numPortals];
    }

    public static VisionPortal easyCreateWithDefaults(BuiltinCameraDirection cameraDirection, VisionProcessor... processors) {
        return new Builder()
                .setCamera(cameraDirection)
                .addProcessors(processors)
                .build();
    }

    public static VisionPortal easyCreateWithDefaults(CameraName cameraName, VisionProcessor... processors) {
        return new Builder()
                .setCamera(cameraName)
                .addProcessors(processors)
                .build();
    }

    /** Same methods as the SDK's builder. */
    public static class Builder {
        private CameraName camera;
        private boolean autoStartStreamOnBuild = true;
        private final List<VisionProcessor> processors = new ArrayList<>();

        public Builder setCamera(CameraName camera) {
            this.camera = camera;
            return this;
        }

        /** The harness has no phone cameras, only fake webcams. */
        public Builder setCamera(BuiltinCameraDirection cameraDirection) {
            throw new UnsupportedOperationException("The harness has no built-in phone cameras (" + cameraDirection
                    + "). Use a webcam: add a FakeWebcam to the fake hardware map.");
        }

        public Builder setStreamFormat(StreamFormat streamFormat) { return this; }
        public Builder enableLiveView(boolean enableLiveView) { return this; }
        public Builder setAutoStopLiveView(boolean autoPause) { return this; }
        public Builder setLiveViewContainerId(int liveViewContainerId) { return this; }
        public Builder setCameraResolution(Size cameraResolution) { return this; }
        public Builder setShowStatsOverlay(boolean showStatsOverlay) { return this; }

        public Builder setAutoStartStreamOnBuild(boolean autoStartStreamOnBuild) {
            this.autoStartStreamOnBuild = autoStartStreamOnBuild;
            return this;
        }

        public Builder addProcessor(VisionProcessor processor) {
            if (processors.contains(processor)) {
                throw new RuntimeException("This VisionProcessor has already been attached to a VisionPortal, either a different one or perhaps even this same portal.");
            }
            processors.add(processor);
            return this;
        }

        public Builder addProcessors(VisionProcessor... processors) {
            for (VisionProcessor p : processors) {
                addProcessor(p);
            }
            return this;
        }

        public VisionPortal build() {
            if (camera == null) {
                throw new RuntimeException("You can't build a vision portal without setting a camera!");
            }
            if (!(camera instanceof FakeWebcam webcam)) {
                throw new IllegalStateException("The harness can only build a VisionPortal for a FakeWebcam, but the"
                        + " camera was a " + camera.getClass().getName() + ".");
            }
            VisionPortal portal = new HarnessVisionPortal(webcam, processors, autoStartStreamOnBuild);
            // Clear this list to allow safe re-use of the builder object (as the SDK does).
            processors.clear();
            return portal;
        }
    }

    public abstract void setProcessorEnabled(VisionProcessor processor, boolean enabled);

    public abstract boolean getProcessorEnabled(VisionProcessor processor);

    /** The various states that the camera may be in at any given time (same as the SDK). */
    public enum CameraState {
        OPENING_CAMERA_DEVICE,
        CAMERA_DEVICE_READY,
        STARTING_STREAM,
        STREAMING,
        STOPPING_STREAM,
        CLOSING_CAMERA_DEVICE,
        CAMERA_DEVICE_CLOSED,
        ERROR
    }

    public abstract CameraState getCameraState();

    public abstract void saveNextFrameRaw(String filename);

    public abstract void stopStreaming();

    public abstract void resumeStreaming();

    public abstract void stopLiveView();

    public abstract void resumeLiveView();

    public abstract float getFps();

    public abstract <T extends CameraControl> T getCameraControl(Class<T> controlType);

    public abstract void setActiveCamera(WebcamName webcamName);

    public abstract WebcamName getActiveCamera();

    public abstract void close();

    /** The portal {@link Builder#build()} returns: a FakeWebcam feeding AprilTag processors. */
    private static final class HarnessVisionPortal extends VisionPortal implements FakeWebcam.FrameReceiver {
        private final FakeWebcam webcam;
        /** Each processor and whether it is enabled, in the order they were added. */
        private final Map<VisionProcessor, Boolean> enabled = new LinkedHashMap<>();
        private CameraState state;

        HarnessVisionPortal(FakeWebcam webcam, List<VisionProcessor> processors, boolean startStreaming) {
            this.webcam = webcam;
            for (VisionProcessor processor : processors) {
                enabled.put(processor, true);
            }
            webcam.connect(this);
            state = startStreaming ? CameraState.STREAMING : CameraState.CAMERA_DEVICE_READY;
        }

        @Override
        public void receiveFrame(List<SeenAprilTag> tagsInView, long frameTimeNanos) {
            if (state != CameraState.STREAMING) {
                return;
            }
            for (Map.Entry<VisionProcessor, Boolean> entry : enabled.entrySet()) {
                if (entry.getValue() && entry.getKey() instanceof AprilTagProcessorImpl aprilTags) {
                    aprilTags.receiveHarnessFrame(tagsInView, frameTimeNanos);
                }
            }
        }

        @Override
        public void setProcessorEnabled(VisionProcessor processor, boolean on) {
            requireProcessor(processor);
            enabled.put(processor, on);
        }

        @Override
        public boolean getProcessorEnabled(VisionProcessor processor) {
            requireProcessor(processor);
            return enabled.get(processor);
        }

        private void requireProcessor(VisionProcessor processor) {
            if (!enabled.containsKey(processor)) {
                throw new IllegalArgumentException("Processor not attached to this portal!");
            }
        }

        @Override public CameraState getCameraState() { return state; }

        @Override
        public void stopStreaming() {
            if (state == CameraState.STREAMING) {
                state = CameraState.CAMERA_DEVICE_READY;
            }
        }

        @Override
        public void resumeStreaming() {
            if (state == CameraState.CAMERA_DEVICE_READY) {
                state = CameraState.STREAMING;
            }
        }

        @Override
        public void close() {
            webcam.disconnect(this);
            state = CameraState.CAMERA_DEVICE_CLOSED;
        }

        /** There are no real pictures to save. */
        @Override public void saveNextFrameRaw(String filename) { }
        @Override public void stopLiveView() { }
        @Override public void resumeLiveView() { }
        @Override public float getFps() { return state == CameraState.STREAMING ? 30f : 0f; }
        /** The fake webcam has no exposure, focus or other controls. */
        @Override public <T extends CameraControl> T getCameraControl(Class<T> controlType) { return null; }
        @Override public void setActiveCamera(WebcamName webcamName) {
            throw new IllegalStateException("setActiveCamera() is only for switchable cameras, which the harness doesn't have.");
        }
        @Override public WebcamName getActiveCamera() { return webcam; }

        /** No Driver Station camera stream in the harness. */
        @Override
        public void getFrameBitmap(Continuation<? extends Consumer<android.graphics.Bitmap>> continuation) { }
    }
}
