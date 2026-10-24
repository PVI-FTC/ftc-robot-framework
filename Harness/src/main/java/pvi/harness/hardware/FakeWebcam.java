package pvi.harness.hardware;

import android.content.Context;

import com.qualcomm.robotcore.util.SerialNumber;

import org.firstinspires.ftc.robotcore.external.function.Consumer;
import org.firstinspires.ftc.robotcore.external.function.Continuation;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraCharacteristics;
import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.robotcore.internal.system.Deadline;

import java.util.ArrayList;
import java.util.List;

import pvi.harness.agent.SimTime;

/**
 * A fake USB webcam (for example a Logitech C920) that "sees" the AprilTags an
 * {@link AprilTagScript} says, instead of looking at a real field.
 *
 * <p>Robot code finds it the normal way, {@code hardwareMap.get(WebcamName.class, name)}, and
 * hands it to the SDK's {@code VisionPortal.Builder}. The harness's own copy of
 * {@code VisionPortal} (no Android, no real camera) connects the webcam to every
 * {@code AprilTagProcessor} in that portal. From then on the webcam takes a new "picture"
 * {@link #framesPerSecond(double) 30 times a second} of simulated time, and each processor
 * turns the tags in view into real SDK detections for {@code getFreshDetections()}. Loops that
 * run between two pictures get {@code null} from {@code getFreshDetections()}, as on the robot.</p>
 *
 * <pre>{@code
 * FakeHardwareMap hw = RobotConfigs.teamA();
 * hw.addWebcam("logitechVisionWebcam")
 *         .script(new AprilTagScript().at(1.5).see(21).at(3.0).lose(21));
 * }</pre>
 *
 * <p>A picture shows the tags that are in view at the moment it is taken, with no delay
 * (a real camera adds a few tens of milliseconds). The runner moves the webcam on before every
 * OpMode call ({@link #stepAll}).</p>
 */
public class FakeWebcam implements WebcamName {

    /** Pictures per second when nobody sets it: what a C920 gives at 640 x 480. */
    public static final double DEFAULT_FRAMES_PER_SECOND = 30.0;

    /**
     * Something that wants each new picture: the harness's VisionPortal, which passes it to
     * its AprilTag processors. Pictures are plain data: the tags in view and when.
     */
    public interface FrameReceiver {
        /**
         * One new picture.
         *
         * @param tagsInView     the tags in view, in the order they came into view
         * @param frameTimeNanos when the picture was taken, on the clock robot code reads
         *                       ({@code System.nanoTime()} during a run)
         */
        void receiveFrame(List<SeenAprilTag> tagsInView, long frameTimeNanos);
    }

    /** Configuration name, e.g. "logitechVisionWebcam". */
    private final String deviceName;
    /** Which tags are in view when; empty (no tags ever) until a test sets one. */
    private AprilTagScript script = new AprilTagScript();
    /** Seconds between two pictures. */
    private double framePeriodSeconds = 1.0 / DEFAULT_FRAMES_PER_SECOND;
    /** Who gets each picture (a VisionPortal using this webcam), in the order they connected. */
    private final List<FrameReceiver> receivers = new ArrayList<>();
    /** Seconds since INIT at the last step; the time a newly connected portal starts from. */
    private double lastStepSinceInitSeconds;
    /** Seconds since INIT when the next picture is due. */
    private double nextFrameSinceInitSeconds;
    /** Pictures taken so far (only while a portal is connected). */
    private int framesTaken;
    /** The tags in the last picture, as plain data. */
    private List<SeenAprilTag> lastFrameTags = List.of();

    /** A webcam under this configuration name. Usually made with {@code hardwareMap.addWebcam(name)}. */
    public FakeWebcam(String deviceName) {
        if (deviceName == null || deviceName.isBlank()) {
            throw new IllegalArgumentException("A webcam needs a configuration name, e.g. \"logitechVisionWebcam\".");
        }
        this.deviceName = deviceName;
    }

    /** Sets which AprilTags this webcam sees, and when. Replaces any earlier script. */
    public FakeWebcam script(AprilTagScript script) {
        if (script == null) {
            throw new IllegalArgumentException("script(...) needs an AprilTagScript, not null.");
        }
        this.script = script;
        return this;
    }

    /** How many pictures per second of simulated time this webcam takes (default 30). */
    public FakeWebcam framesPerSecond(double framesPerSecond) {
        if (!(framesPerSecond > 0) || Double.isInfinite(framesPerSecond)) {
            throw new IllegalArgumentException("framesPerSecond must be more than 0, but was " + framesPerSecond);
        }
        this.framePeriodSeconds = 1.0 / framesPerSecond;
        return this;
    }

    /** Pictures taken so far while a VisionPortal was using this webcam. */
    public int framesTaken() {
        return framesTaken;
    }

    /** The tags in the most recent picture (empty before the first one), as plain data. */
    public List<SeenAprilTag> lastFrameTags() {
        return lastFrameTags;
    }

    /** True while a VisionPortal is using this webcam (between building it and closing it). */
    public boolean isInUse() {
        return !receivers.isEmpty();
    }

    /**
     * Called by the harness's VisionPortal when robot code builds a portal with this webcam.
     * The first picture comes one frame period later, as a real camera needs a moment to start.
     */
    public void connect(FrameReceiver receiver) {
        if (receiver == null) {
            throw new IllegalArgumentException("connect(...) needs a receiver, not null.");
        }
        if (receivers.isEmpty()) {
            nextFrameSinceInitSeconds = lastStepSinceInitSeconds + framePeriodSeconds;
        }
        receivers.add(receiver);
    }

    /** Called by the harness's VisionPortal when robot code closes it. */
    public void disconnect(FrameReceiver receiver) {
        receivers.remove(receiver);
    }

    /**
     * Moves the webcam on to this moment: if a picture is due, it takes one and sends it to
     * every connected portal.
     *
     * @param sinceInitSeconds  seconds since INIT
     * @param sinceStartSeconds seconds since PLAY, or a negative number before PLAY
     */
    public void step(double sinceInitSeconds, double sinceStartSeconds) {
        lastStepSinceInitSeconds = sinceInitSeconds;
        if (receivers.isEmpty() || sinceInitSeconds < nextFrameSinceInitSeconds - 1e-9) {
            return;
        }
        // One picture now, however many frame periods passed (robot code only sees the newest).
        while (nextFrameSinceInitSeconds <= sinceInitSeconds + 1e-9) {
            nextFrameSinceInitSeconds += framePeriodSeconds;
        }
        lastFrameTags = script.tagsInView(sinceInitSeconds, sinceStartSeconds);
        framesTaken++;
        // On the clock robot code reads as System.nanoTime() during a run (see SimTime).
        long frameTimeNanos = SimTime.NANO_BASE + Math.round(sinceInitSeconds * 1e9);
        // Copy the list: a receiver may disconnect while handling the picture.
        for (FrameReceiver receiver : List.copyOf(receivers)) {
            receiver.receiveFrame(lastFrameTags, frameTimeNanos);
        }
    }

    /**
     * Moves every fake webcam in a hardware map on to this moment. The runner calls this
     * before every OpMode call.
     *
     * @param sinceInitSeconds  seconds since INIT
     * @param sinceStartSeconds seconds since PLAY, or a negative number before PLAY
     */
    public static void stepAll(FakeHardwareMap hardwareMap, double sinceInitSeconds, double sinceStartSeconds) {
        for (String name : hardwareMap.deviceNames()) {
            FakeWebcam webcam = hardwareMap.tryGet(FakeWebcam.class, name);
            if (webcam != null) {
                webcam.step(sinceInitSeconds, sinceStartSeconds);
            }
        }
    }

    // ---- WebcamName / HardwareDevice: what the SDK asks a webcam -----------------------------

    @Override public boolean isWebcam() { return true; }
    @Override public boolean isCameraDirection() { return false; }
    @Override public boolean isSwitchable() { return false; }
    @Override public boolean isUnknown() { return false; }
    @Override public boolean isAttached() { return true; }
    @Override public String getUsbDeviceNameIfAttached() { return "harness:" + deviceName; }
    @Override public SerialNumber getSerialNumber() { return SerialNumber.createFake(); }

    /** No Android permission dialog off-robot: permission is always granted at once. */
    @Override
    public void asyncRequestCameraPermission(Context context, Deadline deadline,
                                             Continuation<? extends Consumer<Boolean>> continuation) {
        continuation.dispatchHere(consumer -> consumer.accept(true));
    }

    @Override public boolean requestCameraPermission(Deadline deadline) { return true; }
    /** The harness doesn't model camera formats or resolutions. */
    @Override public CameraCharacteristics getCameraCharacteristics() { return null; }

    @Override public Manufacturer getManufacturer() { return Manufacturer.Other; }
    @Override public String getDeviceName() { return "Fake webcam (" + deviceName + ")"; }
    @Override public String getConnectionInfo() { return "harness fake webcam " + deviceName; }
    @Override public int getVersion() { return 1; }
    @Override public void resetDeviceConfigurationForOpMode() { }
    @Override public void close() { }
}
