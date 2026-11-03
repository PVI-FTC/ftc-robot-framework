package pvi.harness.hardware;

import com.qualcomm.hardware.limelightvision.LLResult;
import com.qualcomm.hardware.limelightvision.LLStatus;
import com.qualcomm.hardware.limelightvision.Limelight3A;

import org.firstinspires.ftc.robotcore.internal.usb.EthernetOverUsbSerialNumber;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.InetAddress;

/**
 * A Limelight 3A camera, showing robot code what the {@link SensorScript} says it sees
 * ({@link LimelightView}): {@code script.set("limelight", LimelightView.target(-4.5, 12.0, 1.8))}.
 *
 * <p>The real Limelight sends its results over the network as JSON; this fake builds the same
 * JSON from the script and hands it to the SDK's own {@code LLResult} parser, so robot code
 * reads it exactly as on the robot ({@code getTx()}, {@code getTy()}, {@code getTa()},
 * {@code isValid()}, {@code getFiducialResults()}...). Results come only while the camera is
 * started ({@code start()}); before that {@code getLatestResult()} is null, as on the robot
 * before its first update. No network is used.</p>
 *
 * <p>Not modelled: robot pose from AprilTags (MegaTag botpose, all zeros), snapshots, and the
 * Python, barcode, classifier and detector pipelines.</p>
 *
 * <p>Add one with {@code hardwareMap.addLimelight("limelight")}.</p>
 */
public class FakeLimelight extends Limelight3A implements SimulatedSensor {
    private final String name;
    private LimelightView view = LimelightView.nothing();
    private boolean running;
    private int pipeline;
    private int pollRateHz = 100;
    /** The result robot code reads, rebuilt each step while running; null before the first. */
    private LLResult latest;

    public FakeLimelight(String name) {
        // The SDK expects a Limelight's "USB ethernet" serial number; this one points nowhere.
        super(EthernetOverUsbSerialNumber.fromIpAddress("127.0.0.1", "harness"), name, InetAddress.getLoopbackAddress());
        this.name = name.trim();
    }

    /** The configuration name this camera was added under. */
    public String getName() {
        return name;
    }

    /** The pipeline robot code last switched to (0 at power-on). */
    public int pipeline() {
        return pipeline;
    }

    /** Sets what it sees directly, for quick tests. */
    public FakeLimelight setView(LimelightView view) {
        this.view = view;
        return this;
    }

    @Override
    public void step(Step now) {
        LimelightView scripted = now.script().valueAt(name, LimelightView.class,
                now.sinceInitSeconds(), now.sinceStartSeconds());
        if (scripted != null) {
            view = scripted;
        }
        if (running) {
            latest = parse(toJson(view, pipeline));
        }
    }

    /** The JSON a Limelight would send for this view (only the keys the SDK reads). */
    static JSONObject toJson(LimelightView view, int pipeline) {
        try {
            JSONObject json = new JSONObject();
            json.put("v", view.valid() ? 1 : 0);
            json.put("pID", pipeline);
            json.put("tx", view.txDegrees());
            json.put("ty", view.tyDegrees());
            json.put("txnc", view.txDegrees());
            json.put("tync", view.tyDegrees());
            json.put("ta", view.areaPercent());
            json.put("pipelineType", view.aprilTags().isEmpty() ? "pipe_color" : "pipe_fiducial");
            JSONArray zeros = new JSONArray(new double[] {0, 0, 0, 0, 0, 0});
            for (String pose : new String[] {"botpose", "botpose_wpiblue", "botpose_wpired",
                    "botpose_orb", "botpose_orb_wpiblue", "botpose_orb_wpired"}) {
                json.put(pose, zeros);
            }
            JSONArray fiducials = new JSONArray();
            for (SeenAprilTag tag : view.aprilTags()) {
                JSONObject fiducial = new JSONObject();
                fiducial.put("fID", tag.id());
                fiducial.put("fam", "36H11");
                fiducial.put("tx", -tag.bearingDegrees());   // tx is positive to the right
                fiducial.put("ty", tag.elevationDegrees());
                fiducial.put("ta", view.areaPercent());
                fiducials.put(fiducial);
            }
            json.put("Fiducial", fiducials);
            return json;
        } catch (JSONException e) {
            throw new IllegalStateException("Could not build the fake Limelight result", e);
        }
    }

    /** Uses the SDK's own parser, so results read exactly as on the robot. */
    private static LLResult parse(JSONObject json) {
        try {
            return new ScriptedResult(json);
        } catch (JSONException e) {
            throw new IllegalStateException("The SDK could not read the fake Limelight result " + json, e);
        }
    }

    /** {@code LLResult}'s JSON constructor is protected, so the fake reaches it through a subclass. */
    private static final class ScriptedResult extends LLResult {
        ScriptedResult(JSONObject json) throws JSONException {
            super(json);
        }
    }

    // ---- Limelight3A: what robot code calls (no network, no threads) -------------------------

    @Override public synchronized void start() { running = true; }
    @Override public synchronized void pause() { running = false; }
    @Override public synchronized void stop() { running = false; }
    @Override public boolean isRunning() { return running; }
    @Override public synchronized void setPollRateHz(int rateHz) { this.pollRateHz = rateHz; }
    @Override public long getTimeSinceLastUpdate() { return latest == null ? Long.MAX_VALUE : 0; }
    @Override public boolean isConnected() { return true; }
    @Override public LLResult getLatestResult() { return latest; }
    @Override public LLStatus getStatus() { return new LLStatus(); }
    @Override public boolean reloadPipeline() { return true; }

    @Override
    public boolean pipelineSwitch(int index) {
        this.pipeline = index;
        return true;
    }

    @Override public boolean captureSnapshot(String snapshotName) { return false; }
    @Override public boolean deleteSnapshots() { return false; }
    @Override public boolean deleteSnapshot(String snapshotName) { return false; }
    @Override public boolean updatePythonInputs(double a, double b, double c, double d, double e, double f, double g, double h) { return true; }
    @Override public boolean updatePythonInputs(double[] inputs) { return true; }
    @Override public boolean updateRobotOrientation(double yaw) { return true; }
    @Override public void shutdown() { running = false; }
    @Override public void close() { running = false; }
    @Override public String getConnectionInfo() { return "harness: " + name; }
}
