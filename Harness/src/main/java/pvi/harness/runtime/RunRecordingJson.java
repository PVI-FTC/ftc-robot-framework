package pvi.harness.runtime;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import pvi.harness.ds.Button;
import pvi.harness.ds.RumbleRecord;
import pvi.harness.physics.GamePiece;
import pvi.harness.physics.ModelValue;
import pvi.harness.physics.Pose;

/**
 * Writes a {@link RunRecording} as JSON, by hand, so the harness needs no JSON library.
 *
 * <p>The layout mirrors the records: field names are the record component names, records
 * become objects, lists become arrays, enums become their name as a string, and a missing pose
 * or drive model becomes {@code null}. Devices are arrays of {@code {name, ...}} objects rather
 * than name-to-value maps, because some JSON readers (Unity's JsonUtility) can't read maps.
 * The README describes every field.</p>
 *
 * <p>{@link #parse} and {@link #load} read a saved file back (the viewer uses them).</p>
 *
 * <p>The same recording always gives exactly the same text: numbers are rounded to 6 decimal
 * places and always use a dot, lines end in "\n", and list order is the recording's order.
 * The header and each list item are on their own line, so files compare well in a diff.</p>
 */
public final class RunRecordingJson {
    /** Where {@link RunRecorder#save(String)} puts files, relative to the folder tests run in. */
    public static final Path DEFAULT_FOLDER = Path.of("build", "recordings");
    /** Decimal places kept for every number (a millionth of an inch, radian or second). */
    private static final int DECIMALS = 6;

    /** Only static methods here; nothing to create. */
    private RunRecordingJson() {
    }

    /** The whole recording as JSON text. */
    public static String format(RunRecording recording) {
        if (recording == null) {
            throw new IllegalArgumentException("Cannot write JSON for a null recording.");
        }
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"header\": ").append(header(recording.header())).append(",\n");
        json.append("  \"gamepad1Rumbles\": ").append(rumbles(recording.gamepad1Rumbles())).append(",\n");
        json.append("  \"gamepad2Rumbles\": ").append(rumbles(recording.gamepad2Rumbles())).append(",\n");
        json.append("  \"events\": ").append(events(recording.events())).append(",\n");
        json.append("  \"loops\": [");
        List<RunRecording.LoopRecord> loops = recording.loops();
        for (int i = 0; i < loops.size(); i++) {
            json.append(i == 0 ? "\n    " : ",\n    ").append(loop(loops.get(i)));
        }
        json.append(loops.isEmpty() ? "]\n" : "\n  ]\n");
        json.append("}\n");
        return json.toString();
    }

    /**
     * Writes the recording to {@code file}, creating its folder if needed, and replacing any
     * older file with the same name.
     *
     * @return the full path of the saved file, to print or open
     */
    public static Path save(RunRecording recording, Path file) {
        String text = format(recording);
        Path fullPath = file.toAbsolutePath();
        try {
            if (fullPath.getParent() != null) {
                Files.createDirectories(fullPath.getParent());
            }
            Files.writeString(fullPath, text, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not save the run recording to " + fullPath, e);
        }
        return fullPath;
    }

    /**
     * {@code <name>.json} inside {@link #DEFAULT_FOLDER}. Only simple names are allowed, so a
     * test can't write outside the build folder by accident.
     */
    public static Path fileInDefaultFolder(String name) {
        if (name == null || !name.matches("[A-Za-z0-9_.-]+") || name.startsWith(".")) {
            throw new IllegalArgumentException("Recording file name \"" + name + "\" is not allowed. Use letters,"
                    + " digits, '-', '_' and '.' only (not starting with '.'), without \".json\","
                    + " e.g. \"teamAAutonomous\".");
        }
        return DEFAULT_FOLDER.resolve(name + ".json");
    }

    // ---- Reading a saved file back --------------------------------------------------------

    /** Reads a recording saved by {@link #save}, e.g. to replay it in the viewer. */
    public static RunRecording load(Path file) {
        try {
            return parse(Files.readString(file, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read the run recording " + file.toAbsolutePath(), e);
        }
    }

    /**
     * Turns JSON text from {@link #format} back into a {@link RunRecording}. Refuses other
     * format versions, and names the exact field when something is missing or the wrong type,
     * e.g. {@code loops[12].pose.xInches}.
     */
    public static RunRecording parse(String json) {
        Map<String, Object> top = object(JsonReader.parse(json), "the file");
        Map<String, Object> h = object(top.get("header"), "header");
        int version = whole(h.get("version"), "header.version");
        if (version < 1 || version > RunRecording.VERSION) {
            throw new IllegalArgumentException("This recording is format version " + version
                    + ", but this harness reads versions 1 to " + RunRecording.VERSION + ".");
        }
        String periodName = optionalText(h.get("period"), "header.period");
        RunRecording.Header header = new RunRecording.Header(version,
                text(h.get("distanceUnit"), "header.distanceUnit"),
                text(h.get("angleUnit"), "header.angleUnit"),
                text(h.get("timeUnit"), "header.timeUnit"),
                text(h.get("robotConfig"), "header.robotConfig"),
                text(h.get("opMode"), "header.opMode"),
                periodName == null ? null : MatchPeriod.valueOf(periodName),
                decimal(h.get("loopPeriodSeconds"), "header.loopPeriodSeconds"),
                readPose(h.get("startPose"), "header.startPose"),
                readDriveModel(h.get("driveModel"), "header.driveModel"),
                // Versions 1 to 3 have no game pieces.
                version >= 4 ? readPieces(h.get("pieces"), "header.pieces") : List.of());

        List<RunRecording.LoopRecord> loops = new ArrayList<>();
        List<Object> loopItems = array(top.get("loops"), "loops");
        for (int i = 0; i < loopItems.size(); i++) {
            loops.add(readLoop(loopItems.get(i), "loops[" + i + "]", version));
        }
        // Version 1 files have no events.
        List<RunRecording.Event> events = version >= 2 ? readEvents(top.get("events"), "events") : List.of();
        return new RunRecording(header, loops,
                readRumbles(top.get("gamepad1Rumbles"), "gamepad1Rumbles"),
                readRumbles(top.get("gamepad2Rumbles"), "gamepad2Rumbles"),
                events);
    }

    private static RunRecording.LoopRecord readLoop(Object value, String where, int version) {
        Map<String, Object> loop = object(value, where);
        List<RunRecording.MotorState> motors = new ArrayList<>();
        List<Object> motorItems = array(loop.get("motors"), where + ".motors");
        for (int i = 0; i < motorItems.size(); i++) {
            String at = where + ".motors[" + i + "]";
            Map<String, Object> m = object(motorItems.get(i), at);
            motors.add(new RunRecording.MotorState(text(m.get("name"), at + ".name"),
                    decimal(m.get("power"), at + ".power")));
        }
        List<RunRecording.ServoState> servos = new ArrayList<>();
        List<Object> servoItems = array(loop.get("servos"), where + ".servos");
        for (int i = 0; i < servoItems.size(); i++) {
            String at = where + ".servos[" + i + "]";
            Map<String, Object> s = object(servoItems.get(i), at);
            Object position = s.get("position");
            servos.add(new RunRecording.ServoState(text(s.get("name"), at + ".name"),
                    position == null ? Double.NaN : decimal(position, at + ".position")));
        }
        List<RunRecording.TelemetryScreen> screens = new ArrayList<>();
        List<Object> screenItems = array(loop.get("telemetry"), where + ".telemetry");
        for (int i = 0; i < screenItems.size(); i++) {
            String at = where + ".telemetry[" + i + "]";
            Map<String, Object> screen = object(screenItems.get(i), at);
            screens.add(new RunRecording.TelemetryScreen(texts(screen.get("lines"), at + ".lines"),
                    texts(screen.get("log"), at + ".log")));
        }
        // Versions 1 and 2 have no aprilTags.
        List<RunRecording.AprilTagState> aprilTags = new ArrayList<>();
        List<Object> tagItems = version >= 3 ? array(loop.get("aprilTags"), where + ".aprilTags") : List.of();
        for (int i = 0; i < tagItems.size(); i++) {
            String at = where + ".aprilTags[" + i + "]";
            Map<String, Object> t = object(tagItems.get(i), at);
            aprilTags.add(new RunRecording.AprilTagState(text(t.get("camera"), at + ".camera"),
                    whole(t.get("id"), at + ".id"),
                    decimal(t.get("rightInches"), at + ".rightInches"),
                    decimal(t.get("forwardInches"), at + ".forwardInches"),
                    decimal(t.get("upInches"), at + ".upInches"),
                    decimal(t.get("yawDegrees"), at + ".yawDegrees"),
                    decimal(t.get("pitchDegrees"), at + ".pitchDegrees"),
                    decimal(t.get("rollDegrees"), at + ".rollDegrees")));
        }
        // Versions 1 to 3 have no game pieces.
        List<GamePiece> pieces = version >= 4 ? readPieces(loop.get("pieces"), where + ".pieces") : List.of();
        String stage = optionalText(loop.get("stage"), where + ".stage");
        return new RunRecording.LoopRecord(
                decimal(loop.get("sinceInitSeconds"), where + ".sinceInitSeconds"),
                decimal(loop.get("matchTimeSeconds"), where + ".matchTimeSeconds"),
                stage == null ? null : RunResult.Stage.valueOf(stage),
                readPose(loop.get("pose"), where + ".pose"),
                motors, servos,
                optionalText(loop.get("autoStep"), where + ".autoStep"),
                screens,
                readGamepad(loop.get("gamepad1"), where + ".gamepad1"),
                readGamepad(loop.get("gamepad2"), where + ".gamepad2"),
                aprilTags, pieces);
    }

    private static List<GamePiece> readPieces(Object value, String where) {
        List<GamePiece> pieces = new ArrayList<>();
        List<Object> items = array(value, where);
        for (int i = 0; i < items.size(); i++) {
            String at = where + "[" + i + "]";
            Map<String, Object> p = object(items.get(i), at);
            String id = text(p.get("id"), at + ".id");
            String kind = text(p.get("kind"), at + ".kind");
            String state = text(p.get("state"), at + ".state");
            double x = decimal(p.get("xInches"), at + ".xInches");
            double y = decimal(p.get("yInches"), at + ".yInches");
            double z = decimal(p.get("zInches"), at + ".zInches");
            try {
                pieces.add(new GamePiece(id, kind, state, x, y, z));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Bad run recording: " + at + ": " + e.getMessage());
            }
        }
        return pieces;
    }

    private static RunRecording.GamepadState readGamepad(Object value, String where) {
        Map<String, Object> pad = object(value, where);
        List<Button> buttons = new ArrayList<>();
        for (String name : texts(pad.get("buttons"), where + ".buttons")) {
            try {
                buttons.add(Button.valueOf(name));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Bad run recording: " + where + ".buttons has unknown button '"
                        + name + "'.");
            }
        }
        return new RunRecording.GamepadState(buttons,
                decimal(pad.get("leftStickX"), where + ".leftStickX"),
                decimal(pad.get("leftStickY"), where + ".leftStickY"),
                decimal(pad.get("rightStickX"), where + ".rightStickX"),
                decimal(pad.get("rightStickY"), where + ".rightStickY"),
                decimal(pad.get("leftTrigger"), where + ".leftTrigger"),
                decimal(pad.get("rightTrigger"), where + ".rightTrigger"));
    }

    private static Pose readPose(Object value, String where) {
        if (value == null) {
            return null;
        }
        Map<String, Object> p = object(value, where);
        return new Pose(decimal(p.get("xInches"), where + ".xInches"),
                decimal(p.get("yInches"), where + ".yInches"),
                decimal(p.get("headingRadians"), where + ".headingRadians"));
    }

    private static RunRecording.DriveModelValues readDriveModel(Object value, String where) {
        if (value == null) {
            return null;
        }
        Map<String, Object> m = object(value, where);
        return new RunRecording.DriveModelValues(text(m.get("name"), where + ".name"),
                readModelValue(m.get("forward"), where + ".forward"),
                readModelValue(m.get("strafe"), where + ".strafe"),
                readModelValue(m.get("turn"), where + ".turn"),
                readModelValue(m.get("length"), where + ".length"),
                readModelValue(m.get("width"), where + ".width"));
    }

    private static ModelValue readModelValue(Object value, String where) {
        Map<String, Object> v = object(value, where);
        Object measured = v.get("measured");
        if (!(measured instanceof Boolean)) {
            throw wrongType(where + ".measured", "true or false", measured);
        }
        return new ModelValue(decimal(v.get("value"), where + ".value"), text(v.get("unit"), where + ".unit"),
                text(v.get("source"), where + ".source"), (Boolean) measured);
    }

    private static List<RumbleRecord> readRumbles(Object value, String where) {
        List<RumbleRecord> rumbles = new ArrayList<>();
        List<Object> items = array(value, where);
        for (int i = 0; i < items.size(); i++) {
            String at = where + "[" + i + "]";
            Map<String, Object> r = object(items.get(i), at);
            rumbles.add(new RumbleRecord(decimal(r.get("matchTimeSeconds"), at + ".matchTimeSeconds"),
                    whole(r.get("totalDurationMs"), at + ".totalDurationMs"),
                    whole(r.get("pulses"), at + ".pulses")));
        }
        return rumbles;
    }

    private static List<RunRecording.Event> readEvents(Object value, String where) {
        List<RunRecording.Event> events = new ArrayList<>();
        List<Object> items = array(value, where);
        for (int i = 0; i < items.size(); i++) {
            String at = where + "[" + i + "]";
            Map<String, Object> e = object(items.get(i), at);
            events.add(new RunRecording.Event(decimal(e.get("sinceInitSeconds"), at + ".sinceInitSeconds"),
                    decimal(e.get("matchTimeSeconds"), at + ".matchTimeSeconds"),
                    text(e.get("text"), at + ".text")));
        }
        return events;
    }

    // Type checks with clear messages. "where" is the field's path in the file.

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(Object value, String where) {
        if (!(value instanceof Map)) {
            throw wrongType(where, "an object {...}", value);
        }
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> array(Object value, String where) {
        if (!(value instanceof List)) {
            throw wrongType(where, "an array [...]", value);
        }
        return (List<Object>) value;
    }

    private static double decimal(Object value, String where) {
        if (!(value instanceof Double)) {
            throw wrongType(where, "a number", value);
        }
        return (Double) value;
    }

    private static int whole(Object value, String where) {
        double number = decimal(value, where);
        if (number != Math.rint(number)) {
            throw wrongType(where, "a whole number", value);
        }
        return (int) number;
    }

    private static String text(Object value, String where) {
        if (!(value instanceof String)) {
            throw wrongType(where, "a string", value);
        }
        return (String) value;
    }

    private static String optionalText(Object value, String where) {
        return value == null ? null : text(value, where);
    }

    private static List<String> texts(Object value, String where) {
        List<String> result = new ArrayList<>();
        List<Object> items = array(value, where);
        for (int i = 0; i < items.size(); i++) {
            result.add(text(items.get(i), where + "[" + i + "]"));
        }
        return result;
    }

    private static IllegalArgumentException wrongType(String where, String expected, Object got) {
        return new IllegalArgumentException("Bad run recording: " + where + " should be " + expected
                + ", but it is " + (got == null ? "missing or null" : "'" + got + "'") + ".");
    }

    // ---- Single pieces, for messages that carry one loop at a time (the Unity bridge) -----

    /** One loop as a single line of JSON, exactly as it appears in a saved recording's "loops". */
    public static String loopJson(RunRecording.LoopRecord loop) {
        return loop(loop);
    }

    /** A recording header as a single line of JSON, exactly as in a saved recording. */
    public static String headerJson(RunRecording.Header header) {
        return header(header);
    }

    /** Rumbles as a JSON array, as in a saved recording's "gamepad1Rumbles". */
    public static String rumblesJson(List<RumbleRecord> rumbles) {
        return rumbles(rumbles);
    }

    /** Events as a JSON array, as in a saved recording's "events". */
    public static String eventsJson(List<RunRecording.Event> events) {
        return events(events);
    }

    /**
     * A gamepad from already-parsed JSON (see {@link JsonReader}), in the same layout as a
     * recording's "gamepad1". Names the exact field when something is wrong.
     *
     * @param where the field's name for error messages, e.g. "step.gamepad1"
     */
    public static RunRecording.GamepadState gamepadFrom(Object parsed, String where) {
        return readGamepad(parsed, where);
    }

    // ---- One method per record ------------------------------------------------------------

    private static String header(RunRecording.Header h) {
        return "{"
                + field("version", Integer.toString(h.version())) + ", "
                + field("distanceUnit", string(h.distanceUnit())) + ", "
                + field("angleUnit", string(h.angleUnit())) + ", "
                + field("timeUnit", string(h.timeUnit())) + ", "
                + field("robotConfig", string(h.robotConfig())) + ", "
                + field("opMode", string(h.opMode())) + ", "
                + field("period", h.period() == null ? "null" : string(h.period().name())) + ", "
                + field("loopPeriodSeconds", number(h.loopPeriodSeconds())) + ", "
                + field("startPose", pose(h.startPose())) + ", "
                + field("driveModel", driveModel(h.driveModel())) + ", "
                + field("pieces", pieces(h.pieces()))
                + "}";
    }

    private static String driveModel(RunRecording.DriveModelValues m) {
        if (m == null) {
            return "null";
        }
        return "{"
                + field("name", string(m.name())) + ", "
                + field("forward", modelValue(m.forward())) + ", "
                + field("strafe", modelValue(m.strafe())) + ", "
                + field("turn", modelValue(m.turn())) + ", "
                + field("length", modelValue(m.length())) + ", "
                + field("width", modelValue(m.width()))
                + "}";
    }

    private static String modelValue(ModelValue v) {
        return "{"
                + field("value", number(v.value())) + ", "
                + field("unit", string(v.unit())) + ", "
                + field("measured", Boolean.toString(v.measured())) + ", "
                + field("source", string(v.source()))
                + "}";
    }

    private static String pose(Pose p) {
        if (p == null) {
            return "null";
        }
        return "{"
                + field("xInches", number(p.xInches())) + ", "
                + field("yInches", number(p.yInches())) + ", "
                + field("headingRadians", number(p.headingRadians()))
                + "}";
    }

    private static String rumbles(List<RumbleRecord> rumbles) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < rumbles.size(); i++) {
            RumbleRecord r = rumbles.get(i);
            json.append(i == 0 ? "" : ", ").append("{")
                    .append(field("matchTimeSeconds", number(r.matchTimeSeconds()))).append(", ")
                    .append(field("totalDurationMs", Integer.toString(r.totalDurationMs()))).append(", ")
                    .append(field("pulses", Integer.toString(r.pulses())))
                    .append("}");
        }
        return json.append("]").toString();
    }

    private static String events(List<RunRecording.Event> events) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < events.size(); i++) {
            RunRecording.Event e = events.get(i);
            json.append(i == 0 ? "" : ", ").append("{")
                    .append(field("sinceInitSeconds", number(e.sinceInitSeconds()))).append(", ")
                    .append(field("matchTimeSeconds", number(e.matchTimeSeconds()))).append(", ")
                    .append(field("text", string(e.text())))
                    .append("}");
        }
        return json.append("]").toString();
    }

    private static String loop(RunRecording.LoopRecord loop) {
        StringBuilder motors = new StringBuilder("[");
        for (int i = 0; i < loop.motors().size(); i++) {
            RunRecording.MotorState m = loop.motors().get(i);
            motors.append(i == 0 ? "" : ", ").append("{")
                    .append(field("name", string(m.name()))).append(", ")
                    .append(field("power", number(m.power()))).append("}");
        }
        motors.append("]");

        StringBuilder servos = new StringBuilder("[");
        for (int i = 0; i < loop.servos().size(); i++) {
            RunRecording.ServoState s = loop.servos().get(i);
            servos.append(i == 0 ? "" : ", ").append("{")
                    .append(field("name", string(s.name()))).append(", ")
                    .append(field("position", s.isSet() ? number(s.position()) : "null")).append("}");
        }
        servos.append("]");

        StringBuilder telemetry = new StringBuilder("[");
        for (int i = 0; i < loop.telemetry().size(); i++) {
            RunRecording.TelemetryScreen screen = loop.telemetry().get(i);
            telemetry.append(i == 0 ? "" : ", ").append("{")
                    .append(field("lines", strings(screen.lines()))).append(", ")
                    .append(field("log", strings(screen.log()))).append("}");
        }
        telemetry.append("]");

        StringBuilder aprilTags = new StringBuilder("[");
        for (int i = 0; i < loop.aprilTags().size(); i++) {
            RunRecording.AprilTagState t = loop.aprilTags().get(i);
            aprilTags.append(i == 0 ? "" : ", ").append("{")
                    .append(field("camera", string(t.camera()))).append(", ")
                    .append(field("id", Integer.toString(t.id()))).append(", ")
                    .append(field("rightInches", number(t.rightInches()))).append(", ")
                    .append(field("forwardInches", number(t.forwardInches()))).append(", ")
                    .append(field("upInches", number(t.upInches()))).append(", ")
                    .append(field("yawDegrees", number(t.yawDegrees()))).append(", ")
                    .append(field("pitchDegrees", number(t.pitchDegrees()))).append(", ")
                    .append(field("rollDegrees", number(t.rollDegrees()))).append("}");
        }
        aprilTags.append("]");

        return "{"
                + field("sinceInitSeconds", number(loop.sinceInitSeconds())) + ", "
                + field("matchTimeSeconds", number(loop.matchTimeSeconds())) + ", "
                + field("stage", loop.stage() == null ? "null" : string(loop.stage().name())) + ", "
                + field("pose", pose(loop.pose())) + ", "
                + field("motors", motors.toString()) + ", "
                + field("servos", servos.toString()) + ", "
                + field("autoStep", loop.autoStep() == null ? "null" : string(loop.autoStep())) + ", "
                + field("telemetry", telemetry.toString()) + ", "
                + field("gamepad1", gamepad(loop.gamepad1())) + ", "
                + field("gamepad2", gamepad(loop.gamepad2())) + ", "
                + field("aprilTags", aprilTags.toString()) + ", "
                + field("pieces", pieces(loop.pieces()))
                + "}";
    }

    private static String pieces(List<GamePiece> pieces) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < pieces.size(); i++) {
            GamePiece p = pieces.get(i);
            json.append(i == 0 ? "" : ", ").append("{")
                    .append(field("id", string(p.id()))).append(", ")
                    .append(field("kind", string(p.kind()))).append(", ")
                    .append(field("state", string(p.state()))).append(", ")
                    .append(field("xInches", number(p.xInches()))).append(", ")
                    .append(field("yInches", number(p.yInches()))).append(", ")
                    .append(field("zInches", number(p.zInches()))).append("}");
        }
        return json.append("]").toString();
    }

    private static String gamepad(RunRecording.GamepadState pad) {
        StringBuilder buttons = new StringBuilder("[");
        for (int i = 0; i < pad.buttons().size(); i++) {
            buttons.append(i == 0 ? "" : ", ").append(string(pad.buttons().get(i).name()));
        }
        buttons.append("]");
        return "{"
                + field("buttons", buttons.toString()) + ", "
                + field("leftStickX", number(pad.leftStickX())) + ", "
                + field("leftStickY", number(pad.leftStickY())) + ", "
                + field("rightStickX", number(pad.rightStickX())) + ", "
                + field("rightStickY", number(pad.rightStickY())) + ", "
                + field("leftTrigger", number(pad.leftTrigger())) + ", "
                + field("rightTrigger", number(pad.rightTrigger()))
                + "}";
    }

    // ---- JSON building blocks -------------------------------------------------------------

    /** {@code "name": value}, where value is already JSON. */
    private static String field(String name, String jsonValue) {
        return string(name) + ": " + jsonValue;
    }

    /** A list of strings as a JSON array. */
    private static String strings(List<String> values) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            json.append(i == 0 ? "" : ", ").append(string(values.get(i)));
        }
        return json.append("]").toString();
    }

    /**
     * A number rounded to {@value #DECIMALS} places, without trailing zeros: 0.2, 1, -3.141593.
     * JSON has no NaN or infinity, so those are refused with a clear message.
     */
    public static String number(double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("JSON can't hold " + value + "; a recorded number must be finite.");
        }
        // BigDecimal has no "-0", so tiny negatives come out as plain 0.
        BigDecimal rounded = BigDecimal.valueOf(value).setScale(DECIMALS, RoundingMode.HALF_EVEN).stripTrailingZeros();
        return rounded.signum() == 0 ? "0" : rounded.toPlainString();
    }

    /** A JSON string in quotes, with quotes, backslashes and control characters escaped. */
    public static String string(String value) {
        StringBuilder json = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> json.append("\\\"");
                case '\\' -> json.append("\\\\");
                case '\n' -> json.append("\\n");
                case '\r' -> json.append("\\r");
                case '\t' -> json.append("\\t");
                default -> {
                    if (c < 0x20) {
                        json.append(String.format(Locale.ROOT, "\\u%04x", (int) c));
                    } else {
                        json.append(c);
                    }
                }
            }
        }
        return json.append('"').toString();
    }
}
