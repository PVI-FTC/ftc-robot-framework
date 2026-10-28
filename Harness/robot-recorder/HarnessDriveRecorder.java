package org.firstinspires.ftc.teamcode.harness;

import com.qualcomm.robotcore.hardware.Gamepad;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Records a drive on the REAL robot as a harness run recording, so it can be replayed in the
 * harness viewer or turned into a test with {@code gradlew makeTest}.
 *
 * <p>ROBOT CODE, not harness code. Copy it into TeamCode (see
 * {@code Harness/robot-recorder/README.md}). It only watches the gamepads: it never moves a motor
 * and never changes what your TeleOp does.</p>
 *
 * <p>How it works: the harness simulates time in fixed 20 ms steps. The real robot's loop is
 * faster or slower than that and never exactly even, so this class writes one entry per 20 ms
 * of real time ("ticks"), each holding what the gamepads looked like at that moment. The saved
 * file is then the same shape as a drive recorded live in the harness viewer.</p>
 *
 * <p>The file is saved when the OpMode stops, to
 * {@code /sdcard/FIRST/harness/drive-<OpMode>-<date-time>.json} on the Control Hub.</p>
 */
public final class HarnessDriveRecorder {
    /** The harness's loop period: one entry per 20 ms, like a drive recorded in the viewer. */
    private static final double TICK_SECONDS = 0.02;
    /**
     * Where recordings go on the Control Hub. "/sdcard/FIRST" is the SDK's own FIRST folder
     * (where robot configurations live). A plain path, not the SDK's AppUtil, so this class can
     * also be created off the robot (the harness compiles and runs all of TeamCode).
     */
    private static final File ROBOT_FOLDER = new File("/sdcard/FIRST/harness");

    private final String opModeName;
    private final String robotConfig;
    private final File folder;
    private final StringBuilder loops = new StringBuilder();

    private long initNanos = -1;
    private int ticksWritten = 0;
    private int playTick = -1;
    private boolean stopped = false;
    private String savedPath = null;

    /**
     * @param opModeName  the TeleOp's class name as the harness knows it, e.g. "TeamATeleOp"
     * @param robotConfig the harness robot to replay on: "teamA", "teamB", "teamC" or "driveOnly"
     */
    public HarnessDriveRecorder(String opModeName, String robotConfig) {
        this(opModeName, robotConfig, ROBOT_FOLDER);
    }

    /** Same, but saves into another folder. */
    public HarnessDriveRecorder(String opModeName, String robotConfig, File folder) {
        this.opModeName = opModeName;
        this.robotConfig = robotConfig;
        this.folder = folder;
    }

    /** Call at the end of init(). */
    public void init(Gamepad gamepad1, Gamepad gamepad2) {
        initNanos = System.nanoTime();
        addEntry("INIT", gamepad1, gamepad2);
    }

    /** Call at the end of init_loop(). */
    public void initLoop(Gamepad gamepad1, Gamepad gamepad2) {
        catchUp("INIT_LOOP", gamepad1, gamepad2);
    }

    /** Call at the end of start() (PLAY was pressed). */
    public void start(Gamepad gamepad1, Gamepad gamepad2) {
        catchUp("INIT_LOOP", gamepad1, gamepad2);
        playTick = ticksWritten;
        addEntry("START", gamepad1, gamepad2);
    }

    /** Call at the end of loop(). */
    public void loop(Gamepad gamepad1, Gamepad gamepad2) {
        catchUp("LOOP", gamepad1, gamepad2);
    }

    /**
     * Call in stop(). Writes the file and returns where it went, or a short error message if it
     * couldn't be written (the robot keeps working either way).
     */
    public String stop(Gamepad gamepad1, Gamepad gamepad2) {
        if (stopped) {
            return savedPath;
        }
        stopped = true;
        if (initNanos < 0) {
            return savedPath = "Harness recorder: init() was never called, nothing saved.";
        }
        catchUp(playTick < 0 ? "INIT_LOOP" : "LOOP", gamepad1, gamepad2);
        addEntry("STOP", gamepad1, gamepad2);
        return savedPath = save();
    }

    /** Where the file was saved (or why not), after stop(); null before. */
    public String savedPath() {
        return savedPath;
    }

    // ---- Internals ------------------------------------------------------------------------

    /** Adds one entry per 20 ms that has passed since the last entry. */
    private void catchUp(String stage, Gamepad gamepad1, Gamepad gamepad2) {
        if (initNanos < 0 || stopped) {
            return;
        }
        double elapsed = (System.nanoTime() - initNanos) / 1e9;
        while (ticksWritten * TICK_SECONDS <= elapsed) {
            addEntry(stage, gamepad1, gamepad2);
        }
    }

    private void addEntry(String stage, Gamepad g1, Gamepad g2) {
        double sinceInit = ticksWritten * TICK_SECONDS;
        double matchTime = playTick < 0 ? 0 : (ticksWritten - playTick) * TICK_SECONDS;
        if (loops.length() > 0) {
            loops.append(",\n");
        }
        loops.append("{\"sinceInitSeconds\":").append(num(sinceInit))
                .append(",\"matchTimeSeconds\":").append(num(matchTime))
                .append(",\"stage\":\"").append(stage).append('"')
                .append(",\"pose\":null,\"motors\":[],\"servos\":[],\"autoStep\":null,\"telemetry\":[]")
                .append(",\"gamepad1\":").append(gamepad(g1))
                .append(",\"gamepad2\":").append(gamepad(g2))
                .append(",\"aprilTags\":[],\"pieces\":[]}");
        ticksWritten++;
    }

    /** One gamepad in the harness's format (README, "Run recording format"). */
    private static String gamepad(Gamepad g) {
        StringBuilder b = new StringBuilder("{\"buttons\":[");
        // Same names and order as the harness's README.
        String[] names = {"A", "B", "X", "Y", "DPAD_UP", "DPAD_DOWN", "DPAD_LEFT", "DPAD_RIGHT",
                "LEFT_BUMPER", "RIGHT_BUMPER", "LEFT_STICK_BUTTON", "RIGHT_STICK_BUTTON",
                "START", "BACK", "GUIDE", "TOUCHPAD"};
        boolean[] held = {g.a, g.b, g.x, g.y, g.dpad_up, g.dpad_down, g.dpad_left, g.dpad_right,
                g.left_bumper, g.right_bumper, g.left_stick_button, g.right_stick_button,
                g.start, g.back, g.guide, g.touchpad};
        boolean first = true;
        for (int i = 0; i < names.length; i++) {
            if (held[i]) {
                b.append(first ? "" : ",").append('"').append(names[i]).append('"');
                first = false;
            }
        }
        return b.append("],\"leftStickX\":").append(num(g.left_stick_x))
                .append(",\"leftStickY\":").append(num(g.left_stick_y))
                .append(",\"rightStickX\":").append(num(g.right_stick_x))
                .append(",\"rightStickY\":").append(num(g.right_stick_y))
                .append(",\"leftTrigger\":").append(num(g.left_trigger))
                .append(",\"rightTrigger\":").append(num(g.right_trigger))
                .append('}').toString();
    }

    private String save() {
        String stamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new Date());
        File file = new File(folder, "drive-" + opModeName + "-" + stamp + ".json");
        String header = "{\"header\":{\"version\":4,\"distanceUnit\":\"inches\",\"angleUnit\":\"radians\","
                + "\"timeUnit\":\"seconds\",\"robotConfig\":\"" + robotConfig + "\",\"opMode\":\"" + opModeName
                + "\",\"period\":\"TELEOP\",\"loopPeriodSeconds\":" + num(TICK_SECONDS)
                + ",\"startPose\":null,\"driveModel\":null,\"pieces\":[]},\n"
                + "\"gamepad1Rumbles\":[],\"gamepad2Rumbles\":[],\n"
                + "\"events\":[{\"sinceInitSeconds\":0,\"matchTimeSeconds\":0,"
                + "\"text\":\"Recorded on the real robot (HarnessDriveRecorder)\"}],\n"
                + "\"loops\":[\n";
        if (folder == ROBOT_FOLDER && !new File("/sdcard/FIRST").isDirectory()) {
            // Not on a Control Hub (e.g. running inside the harness on a laptop): don't create
            // a stray /sdcard folder there.
            return "Harness recorder: not on a robot (no /sdcard/FIRST), nothing saved.";
        }
        if (!folder.isDirectory() && !folder.mkdirs()) {
            return "Harness recorder: couldn't make the folder " + folder;
        }
        Writer out = null;
        try {
            out = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8);
            out.write(header);
            out.write(loops.toString());
            out.write("\n]}\n");
            return file.getAbsolutePath();
        } catch (IOException e) {
            return "Harness recorder: couldn't save " + file + ": " + e.getMessage();
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ignored) {
                    // Nothing more to do: the message above already says what happened.
                }
            }
        }
    }

    /** A number the way the harness writes them: at most 6 decimals, dot, no trailing zeros. */
    private static String num(double value) {
        if (value == 0 || Double.isNaN(value)) {
            return "0";
        }
        String text = String.format(Locale.US, "%.6f", value).replaceAll("0+$", "");
        return text.endsWith(".") ? text.substring(0, text.length() - 1) : text;
    }
}
