package pvi.harness.bridge;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

import pvi.harness.ds.RumbleRecord;
import pvi.harness.physics.DriveModel;
import pvi.harness.physics.Dynamics;
import pvi.harness.physics.MecanumKinematics;
import pvi.harness.physics.PhysicsBackend;
import pvi.harness.physics.Pose;
import pvi.harness.physics.PoseEstimator;
import pvi.harness.physics.WheelPushes;
import pvi.harness.physics.WheelReader;
import pvi.harness.runtime.HarnessConfig;
import pvi.harness.runtime.JsonReader;
import pvi.harness.runtime.OpModeRunner;
import pvi.harness.runtime.RunRecorder;
import pvi.harness.runtime.RunRecording;
import pvi.harness.runtime.RunRecordingJson;
import pvi.harness.runtime.RunResult;
import pvi.harness.runtime.RunnerRegistry;
import pvi.harness.viewer.ControllerPoller;
import pvi.harness.viewer.ControllerReading;
import pvi.harness.viewer.HeldKeys;
import pvi.harness.viewer.JamepadControllers;
import pvi.harness.viewer.LiveGamepads;

/**
 * One Unity connection's conversation with the harness: takes one message line, does what it
 * asks, and returns one reply line (see {@link BridgeProtocol} and the README). No sockets here,
 * so tests can drive it line by line; {@link BridgeServer} just passes lines in and out.
 *
 * <p>Each OpMode call happens only when Unity asks for it, on the caller's thread, and simulated
 * time moves exactly one loop period per call. So the same messages always give exactly the same
 * replies (Harness/ai-instructions/02-unity-readiness.md §2–§3). With {@code "input":
 * "controllers"} the gamepads come from real controllers, so a live drive is only as repeatable
 * as the driver; its recording (save) replays exactly.</p>
 *
 * <p><b>Controllers.</b> The harness reads them with the same code the Swing viewer's live drive
 * uses ({@link LiveGamepads}, Jamepad), with no keyboard: a controller that disconnects releases
 * every input in the same loop, and the moment is marked in the recording.</p>
 */
public final class BridgeSession {

    /** How far through a run the session is. */
    private enum Phase {
        /** Waiting for "hello". */
        NEW,
        /** Said hello; waiting for "load". */
        READY,
        /** OpMode loaded; waiting for "init". */
        LOADED,
        /** INIT done: "step" runs init_loop, "play" presses PLAY. */
        INITIALIZED,
        /** PLAY pressed: "step" runs loop. */
        PLAYING,
        /** Stopped (or crashed): only "save", "load" and "bye" make sense. */
        STOPPED,
    }

    private Phase phase = Phase.NEW;
    private BridgeOpModes.Entry entry;
    private OpModeRunner runner;
    private RunRecorder recorder;
    private final BridgeGamepads driver = new BridgeGamepads();
    private final BridgeGamepads operator = new BridgeGamepads();
    /** How many rumbles and events have already been sent, so each frame carries only new ones. */
    private int sentRumbles1;
    private int sentRumbles2;
    private int sentEvents;
    /** True once the reply asks the server to close the connection. */
    private boolean closing;
    /** Makes the controller reader when a run asks for {@code "input": "controllers"}. */
    private final Supplier<ControllerPoller> controllerFactory;
    /** The controller reader for this run, or null when Unity sends the gamepads. */
    private ControllerPoller controllers;
    /** Gamepads from the controllers, or null when Unity sends the gamepads. */
    private LiveGamepads liveGamepads;
    /** Unity's physics when the run was loaded with "physics": "unity"; null for the harness's own. */
    private RemotePhysics remotePhysics;
    /**
     * Reads the drive wheels for each frame's driveCommand, the same way the position estimate does.
     * Made on "load", not when the session starts, so a broken harness.properties becomes an
     * "error" reply Unity can show instead of a session that never answers.
     */
    private WheelReader wheels;
    /** Every OpMode Unity can run ({@link BridgeOpModes#all()}), asked for on each "hello". */
    private final Supplier<List<BridgeOpModes.Entry>> opModeList;

    /** A session that reads real controllers (Jamepad) when a run asks for them. */
    public BridgeSession() {
        this(JamepadControllers::new);
    }

    /** A session with its own controller reader, e.g. a fake one in tests. */
    BridgeSession(Supplier<ControllerPoller> controllerFactory) {
        this(controllerFactory, BridgeOpModes::all);
    }

    /** A session with its own controller reader and OpMode list (tests use a list that fails like a broken harness.properties). */
    BridgeSession(Supplier<ControllerPoller> controllerFactory, Supplier<List<BridgeOpModes.Entry>> opModeList) {
        this.controllerFactory = controllerFactory;
        this.opModeList = opModeList;
    }

    /** Lets go of the controllers. The server calls this when the client disconnects. */
    public void close() {
        if (controllers != null) {
            controllers.close();
            controllers = null;
            liveGamepads = null;
        }
    }

    /** True after a reply that ends the connection ("goodbye", or a fatal error). */
    public boolean isClosing() {
        return closing;
    }

    /**
     * Handles one message line from Unity and returns the one reply line (without the "\n").
     * Never throws: a bad message or a crashing OpMode becomes an "error" reply.
     */
    public String handle(String line) {
        Map<String, Object> message;
        try {
            Object parsed = JsonReader.parse(line);
            if (!(parsed instanceof Map)) {
                return error("A message must be a JSON object like {\"type\": \"hello\", \"version\": "
                        + BridgeProtocol.VERSION + "}.");
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) parsed;
            message = map;
        } catch (IllegalArgumentException e) {
            return error("Not valid JSON: " + e.getMessage());
        }
        Object type = message.get("type");
        if (!(type instanceof String)) {
            return error("The message has no \"type\".");
        }
        try {
            return switch ((String) type) {
                case BridgeProtocol.HELLO -> hello(message);
                case BridgeProtocol.LOAD -> load(message);
                case BridgeProtocol.INIT -> init();
                case BridgeProtocol.STEP -> step(message);
                case BridgeProtocol.PLAY -> play(message);
                case BridgeProtocol.STOP -> stop();
                case BridgeProtocol.SAVE -> save(message);
                case BridgeProtocol.BYE -> bye();
                default -> error("Unknown message type \"" + type + "\". Known: hello, load, init, step, play, stop, save, bye.");
            };
        } catch (IllegalArgumentException e) {
            // A field with the wrong type, e.g. a gamepad written wrongly, or problems in harness.properties.
            return error(e.getMessage());
        } catch (IllegalStateException e) {
            // harness.properties is missing or can't be read.
            return error(e.getMessage());
        }
    }

    // ---- Messages ---------------------------------------------------------------------------------

    private String hello(Map<String, Object> message) {
        Object version = message.get("version");
        if (!(version instanceof Double) || ((Double) version).intValue() != BridgeProtocol.VERSION) {
            closing = true;
            return error("This harness speaks bridge version " + BridgeProtocol.VERSION + ", but the client sent "
                    + (version == null ? "no version" : "version " + jsonValue(version)) + ".");
        }
        List<BridgeOpModes.Entry> entries;
        try {
            entries = opModeList.get();
        } catch (IllegalArgumentException | IllegalStateException e) {
            // Usually harness.properties: an old or mixed copy, or a typo. Unity shows this text.
            return error("The harness can't list its OpModes: " + e.getMessage());
        }
        phase = Phase.READY;
        StringBuilder opModes = new StringBuilder("[");
        for (int i = 0; i < entries.size(); i++) {
            BridgeOpModes.Entry e = entries.get(i);
            opModes.append(i == 0 ? "" : ", ").append("{")
                    .append(field("id", RunRecordingJson.string(e.id()))).append(", ")
                    .append(field("title", RunRecordingJson.string(e.title()))).append(", ")
                    .append(field("period", RunRecordingJson.string(e.period().name()))).append("}");
        }
        opModes.append("]");
        return reply(BridgeProtocol.WELCOME,
                field("version", Integer.toString(BridgeProtocol.VERSION)),
                field("loopPeriodSeconds", RunRecordingJson.number(OpModeRunner.DEFAULT_LOOP_PERIOD_SECONDS)),
                field("opModes", opModes.toString()));
    }

    private String load(Map<String, Object> message) {
        if (phase == Phase.NEW) {
            return error("Send \"hello\" first.");
        }
        Object id = message.get("opMode");
        BridgeOpModes.Entry found = id instanceof String ? BridgeOpModes.find((String) id) : null;
        if (found == null) {
            return error("Unknown OpMode " + (id == null ? "(none given)" : jsonValue(id)) + ". The \"welcome\" reply lists them.");
        }
        Object input = message.getOrDefault("input", "messages");
        if (!"messages".equals(input) && !"controllers".equals(input)) {
            return error("\"input\" must be \"controllers\" (the harness reads the game controllers) or "
                    + "\"messages\" (each step carries the gamepads), not " + jsonValue(input) + ".");
        }
        Object physics = message.getOrDefault("physics", "harness");
        if (!"harness".equals(physics) && !"unity".equals(physics)) {
            return error("\"physics\" must be \"harness\" (the harness's own physics moves the robot) or "
                    + "\"unity\" (Unity's physics does), not " + jsonValue(physics) + ".");
        }
        // A fresh run: new OpMode, new fake robot, new clock. Earlier runners are forgotten.
        RunnerRegistry.startTest();
        close();
        entry = found;
        driver.set(RunRecording.GamepadState.RELEASED);
        operator.set(RunRecording.GamepadState.RELEASED);
        runner = new OpModeRunner(entry.opMode().get(), entry.robot().get());
        if ("controllers".equals(input)) {
            controllers = controllerFactory.get();
            // No keyboard here: an empty one means a missing controller presses nothing.
            liveGamepads = new LiveGamepads(new HeldKeys(), controllers, this::controllerChanged);
            runner.gamepad1(liveGamepads.gamepad1()).gamepad2(liveGamepads.gamepad2());
        } else {
            runner.gamepad1(driver).gamepad2(operator);
        }
        remotePhysics = null;
        if ("unity".equals(physics)) {
            PoseEstimator builtIn = runner.positionEstimator();
            if (builtIn == null) {
                runner = null;
                entry = null;
                phase = Phase.READY;
                return error("\"physics\": \"unity\" needs a robot with drive motors to move; this one has none.");
            }
            // Same robot, same start; only the backend that moves it is Unity's.
            remotePhysics = new RemotePhysics(builtIn.start());
            runner.usePositionEstimator(new PoseEstimator(builtIn.model(), wheels(), builtIn.start(), remotePhysics));
        }
        recorder = RunRecorder.attachTo(runner);
        sentRumbles1 = 0;
        sentRumbles2 = 0;
        sentEvents = 0;
        phase = Phase.LOADED;
        return reply(BridgeProtocol.LOADED,
                field("opMode", RunRecordingJson.string(entry.id())),
                field("input", RunRecordingJson.string(liveGamepads == null ? "messages" : "controllers")),
                field("physics", RunRecordingJson.string(remotePhysics == null ? "harness" : "unity")),
                field("robotPhysics", robotPhysicsJson()),
                field("matchSeconds", RunRecordingJson.number(entry.period().seconds())),
                field("header", RunRecordingJson.headerJson(recorder.header())));
    }

    private String init() {
        if (phase != Phase.LOADED) {
            return error(phase == Phase.READY || phase == Phase.NEW ? "Send \"load\" first." : "INIT was already pressed.");
        }
        return runCall(() -> runner.init(), Phase.INITIALIZED);
    }

    private String step(Map<String, Object> message) {
        if (phase != Phase.INITIALIZED && phase != Phase.PLAYING) {
            return error("\"step\" works after \"init\" (runs init_loop) or \"play\" (runs loop); the run is "
                    + phase.name().toLowerCase(Locale.ROOT) + ".");
        }
        readGamepads(message);
        readPhysicsState(message);
        if (phase == Phase.INITIALIZED) {
            return runCall(() -> runner.initLoop(1), Phase.INITIALIZED);
        }
        return runCall(() -> {
            runner.loop(1);
            // Like a real match, the run ends when time is up or the OpMode stops itself.
            if (runner.clock().sinceStart() >= entry.period().seconds() - 1e-9 || runner.isStopRequested()) {
                runner.stop();
            }
        }, Phase.PLAYING);
    }

    private String play(Map<String, Object> message) {
        if (phase != Phase.INITIALIZED) {
            return error(phase == Phase.PLAYING ? "PLAY was already pressed." : "Send \"init\" before \"play\".");
        }
        readGamepads(message);
        readPhysicsState(message);
        return runCall(() -> runner.start(), Phase.PLAYING);
    }

    private String stop() {
        if (phase != Phase.INITIALIZED && phase != Phase.PLAYING) {
            return error("Nothing is running to stop.");
        }
        return runCall(() -> runner.stop(), Phase.STOPPED);
    }

    private String save(Map<String, Object> message) {
        if (recorder == null) {
            return error("Nothing to save yet: send \"load\" first.");
        }
        Object name = message.get("name");
        if (!(name instanceof String)) {
            return error("\"save\" needs a \"name\", e.g. {\"type\": \"save\", \"name\": \"my-drive\"}.");
        }
        Path saved = recorder.save((String) name); // refuses names that could leave build/recordings
        return reply(BridgeProtocol.SAVED, field("path", RunRecordingJson.string(saved.toString())));
    }

    private String bye() {
        closing = true;
        close();
        return reply(BridgeProtocol.GOODBYE);
    }

    // ---- Helpers ----------------------------------------------------------------------------------

    /**
     * Runs one OpMode call and returns the frame. A crash ends the run (like the Robot Controller,
     * stop() is not called) and is reported as an error naming the stage.
     */
    private String runCall(Runnable call, Phase after) {
        try {
            call.run();
        } catch (RuntimeException | LinkageError e) {
            phase = Phase.STOPPED;
            RunResult.Stage stage = runner.stage();
            return reply(BridgeProtocol.ERROR,
                    field("message", RunRecordingJson.string(runner.opMode().getClass().getSimpleName() + " crashed during "
                            + stage + ": " + e)),
                    field("crashed", "true"));
        }
        phase = runner.stage() == RunResult.Stage.STOP ? Phase.STOPPED : after;
        return frame();
    }

    /** The newest loop, plus rumbles and events that happened since the last frame. */
    private String frame() {
        List<RumbleRecord> rumbles1 = runner.driver1().rumbles();
        List<RumbleRecord> rumbles2 = runner.driver2().rumbles();
        List<RunRecording.Event> events = recorder.events();
        String reply = reply(BridgeProtocol.FRAME,
                field("loop", RunRecordingJson.loopJson(recorder.lastLoop())),
                field("gamepad1Rumbles", RunRecordingJson.rumblesJson(rumbles1.subList(sentRumbles1, rumbles1.size()))),
                field("gamepad2Rumbles", RunRecordingJson.rumblesJson(rumbles2.subList(sentRumbles2, rumbles2.size()))),
                field("events", RunRecordingJson.eventsJson(events.subList(Math.min(sentEvents, events.size()), events.size()))),
                field("finished", Boolean.toString(phase == Phase.STOPPED)),
                field("controllers", controllerStatus()),
                field("controllerProblem", controllers == null || controllers.problem() == null ? "null"
                        : RunRecordingJson.string(controllers.problem())),
                field("driveCommand", driveCommandJson()));
        sentRumbles1 = rumbles1.size();
        sentRumbles2 = rumbles2.size();
        sentEvents = events.size();
        return reply;
    }

    private void readGamepads(Map<String, Object> message) {
        if (liveGamepads != null && (message.containsKey("gamepad1") || message.containsKey("gamepad2"))) {
            throw new IllegalArgumentException("This run reads the game controllers (\"input\": \"controllers\"), so "
                    + "steps must not carry gamepads. Load with \"input\": \"messages\" to send them.");
        }
        if (message.containsKey("gamepad1")) {
            driver.set(RunRecordingJson.gamepadFrom(message.get("gamepad1"), "gamepad1"));
        }
        if (message.containsKey("gamepad2")) {
            operator.set(RunRecordingJson.gamepadFrom(message.get("gamepad2"), "gamepad2"));
        }
    }

    /**
     * With Unity's physics, takes the robot state Unity worked out for the last frame's
     * driveCommand ("physicsState"); every "step" and "play" must carry one then. With the
     * harness's own physics it is refused, so a mix-up shows at once instead of being ignored.
     */
    @SuppressWarnings("unchecked")
    private void readPhysicsState(Map<String, Object> message) {
        Object raw = message.get("physicsState");
        if (remotePhysics == null) {
            if (raw != null) {
                throw new IllegalArgumentException("This run uses the harness's own physics, so steps must not carry "
                        + "\"physicsState\". Load with \"physics\": \"unity\" to send it.");
            }
            return;
        }
        if (!(raw instanceof Map)) {
            throw new IllegalArgumentException("This run uses Unity's physics (\"physics\": \"unity\"), so each "
                    + "\"step\" and \"play\" needs a \"physicsState\" object: {\"xInches\", \"yInches\", "
                    + "\"headingRadians\", \"forwardInchesPerSecond\", \"leftInchesPerSecond\", "
                    + "\"counterClockwiseRadiansPerSecond\", \"wallContact\", \"obstacleContact\" (optional)}.");
        }
        Map<String, Object> state = (Map<String, Object>) raw;
        Object contact = state.getOrDefault("wallContact", "");
        if (!(contact instanceof String)) {
            throw new IllegalArgumentException("physicsState.wallContact must be text like \"+x\", \"+x/-y\" or \"\".");
        }
        // Version 5: which field elements Unity's robot is pressed against ("HIVE frame", "far FLOWER").
        Object obstacles = state.getOrDefault("obstacleContact", "");
        if (!(obstacles instanceof String)) {
            throw new IllegalArgumentException("physicsState.obstacleContact must be text like \"HIVE frame\", "
                    + "\"HIVE frame/far FLOWER\" or \"\".");
        }
        remotePhysics.supply(new PhysicsBackend.State(
                new Pose(number(state, "xInches"), number(state, "yInches"), number(state, "headingRadians")),
                new MecanumKinematics.RobotVelocity(number(state, "forwardInchesPerSecond"),
                        number(state, "leftInchesPerSecond"), number(state, "counterClockwiseRadiansPerSecond")),
                (String) contact, (String) obstacles));
    }

    /** The wheel reader, read from harness.properties the first time it's needed. */
    private WheelReader wheels() {
        if (wheels == null) {
            wheels = WheelReader.fromConfig(HarnessConfig.load());
        }
        return wheels;
    }

    private static double number(Map<String, Object> object, String name) {
        Object value = object.get(name);
        if (!(value instanceof Double)) {
            throw new IllegalArgumentException("physicsState." + name + " must be a number, not " + jsonValue(value) + ".");
        }
        return (Double) value;
    }

    /**
     * The wheels' command after this call, for Unity's physics: each wheel's push (-1 to 1, after
     * motor direction) and whether the drive motors brake at zero power. Null without drive motors.
     */
    private String driveCommandJson() {
        if (runner.positionEstimator() == null) {
            return "null";
        }
        WheelPushes p = wheels().read(runner.hardwareMap());
        return "{" + field("frontLeft", RunRecordingJson.number(p.frontLeft()))
                + ", " + field("frontRight", RunRecordingJson.number(p.frontRight()))
                + ", " + field("rearLeft", RunRecordingJson.number(p.rearLeft()))
                + ", " + field("rearRight", RunRecordingJson.number(p.rearRight()))
                + ", " + field("braking", Boolean.toString(wheels().isBraking(runner.hardwareMap()))) + "}";
    }

    /**
     * What Unity's physics needs to move this robot like the harness would: full-power speeds,
     * size, start pose and (if the drive model has them) its response and traction numbers. Null
     * when the robot isn't tracked.
     */
    private String robotPhysicsJson() {
        PoseEstimator estimator = runner.positionEstimator();
        if (estimator == null) {
            return "null";
        }
        DriveModel m = estimator.model();
        Dynamics d = m.dynamics();
        Pose start = estimator.start();
        return "{" + field("forwardInchesPerSecond", RunRecordingJson.number(m.forwardInchesPerSecond()))
                + ", " + field("strafeInchesPerSecond", RunRecordingJson.number(m.strafeInchesPerSecond()))
                + ", " + field("turnRadiansPerSecond", RunRecordingJson.number(m.turnRadiansPerSecond()))
                + ", " + field("lengthInches", RunRecordingJson.number(m.lengthInches()))
                + ", " + field("widthInches", RunRecordingJson.number(m.widthInches()))
                + ", " + field("responseTimeSeconds", d == null ? "null" : RunRecordingJson.number(d.responseTime().value()))
                + ", " + field("maxAccelerationInchesPerSecondSquared",
                        d == null ? "null" : RunRecordingJson.number(d.maxAcceleration().value()))
                + ", " + field("forwardCoastInchesPerSecondSquared",
                        d == null ? "null" : RunRecordingJson.number(d.forwardCoast().value()))
                + ", " + field("lateralCoastInchesPerSecondSquared",
                        d == null ? "null" : RunRecordingJson.number(d.lateralCoast().value()))
                + ", " + field("robotObstacles", String.valueOf(HarnessConfig.load().robotObstacles()))
                + ", " + field("start", "{" + field("xInches", RunRecordingJson.number(start.xInches()))
                        + ", " + field("yInches", RunRecordingJson.number(start.yInches()))
                        + ", " + field("headingRadians", RunRecordingJson.number(start.headingRadians())) + "}")
                + "}";
    }

    /**
     * What drives each gamepad, e.g. ["PS5 Controller", "none"], or null when Unity sends the
     * gamepads. "none" means no controller in that slot (it presses nothing).
     */
    private String controllerStatus() {
        if (liveGamepads == null) {
            return "null";
        }
        List<String> names = new ArrayList<>();
        for (String status : liveGamepads.status()) {
            String name = status.equals("keyboard") ? "none" : status.replace(" + keyboard", "");
            names.add(RunRecordingJson.string(name));
        }
        return "[" + String.join(", ", names) + "]";
    }

    /** A controller connected or disconnected: mark it in the recording (Unity sees it in the next frame). */
    private void controllerChanged(int gamepadNumber, ControllerReading reading, boolean connected) {
        recorder.note("Gamepad " + gamepadNumber + ": " + (connected
                ? (reading.type().isBlank() ? "controller" : reading.type()) + " connected"
                : "controller disconnected; inputs released"));
    }

    private static String error(String text) {
        return reply(BridgeProtocol.ERROR, field("message", RunRecordingJson.string(text)));
    }

    /** {"type": "...", field, field, ...} on one line. */
    private static String reply(String type, String... fields) {
        StringBuilder json = new StringBuilder("{").append(field("type", RunRecordingJson.string(type)));
        for (String f : fields) {
            json.append(", ").append(f);
        }
        return json.append("}").toString();
    }

    private static String field(String name, String jsonValue) {
        return RunRecordingJson.string(name) + ": " + jsonValue;
    }

    /** A parsed JSON value shown in an error message. */
    private static String jsonValue(Object value) {
        if (value instanceof Double d && d == Math.rint(d)) {
            return Long.toString(d.longValue());
        }
        return value instanceof String s ? "\"" + s + "\"" : String.valueOf(value);
    }
}
