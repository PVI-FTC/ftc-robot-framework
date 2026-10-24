package pvi.harness.runtime;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.hardware.Gamepad;

import java.lang.reflect.Field;

import pvi.harness.ds.FakeTelemetry;
import pvi.harness.ds.GamepadDriver;
import pvi.harness.ds.GamepadScript;
import pvi.harness.ds.GamepadSource;
import pvi.harness.agent.SimTime;
import pvi.harness.hardware.FakeHardwareMap;
import pvi.harness.hardware.FakeWebcam;
import pvi.harness.physics.DriveModel;
import pvi.harness.physics.DriveModels;
import pvi.harness.physics.Pose;
import pvi.harness.physics.PoseEstimator;
import pvi.harness.physics.WheelReader;

/**
 * Runs an iterative {@link OpMode} the way the Robot Controller app does:
 * INIT, init_loop until PLAY, start, loop until STOP, stop.
 *
 * <p>Each call to the OpMode is one loop. Before every call the scripted gamepads are
 * delivered; after it, any unsent telemetry is sent and simulated time moves forward by
 * the loop period (20 ms by default).</p>
 *
 * <p>Two ways to use it:</p>
 * <ul>
 *   <li><b>Step by step</b> &ndash; {@code init()}, {@code initLoop(n)}, {@code start()},
 *       {@code loop(n)}, {@code stop()}. Exceptions from the OpMode go straight to the test.</li>
 *   <li><b>Whole run</b> &ndash; {@code run(MatchPeriod.TELEOP)} or {@code run(initSeconds,
 *       matchSeconds)}. Exceptions are caught into the returned {@link RunResult}.</li>
 * </ul>
 */
public class OpModeRunner {
    /** 20 ms per loop: a typical real robot loop time. Change with {@link #loopPeriod(double)}. */
    public static final double DEFAULT_LOOP_PERIOD_SECONDS = 0.020;
    /** How long {@link #run(MatchPeriod)} waits between INIT and PLAY. */
    public static final double DEFAULT_INIT_SECONDS = 1.0;

    /** SDK's non-public "has a stop been requested" flag, found once by name. */
    private static final Field STOP_REQUESTED = findOpModeField("stopRequested");
    /** SDK's non-public link to the Robot Controller's OpMode manager, found once by name. */
    private static final Field OPMODE_SERVICES = findOpModeField("internalOpModeServices");

    /** The OpMode under test (your real TeamCode class). */
    private final OpMode opMode;
    /** Stand-in for the Robot Controller's OpMode manager; hears requestOpModeStop(). */
    private final HarnessOpModeServices services = new HarnessOpModeServices();
    /** The fake hardware the OpMode looks devices up in. */
    private final FakeHardwareMap hardwareMap;
    /** Records everything the OpMode shows on the Driver Station. */
    private final FakeTelemetry telemetry = new FakeTelemetry();
    /** This run's simulated time. */
    private final SimClock clock = new SimClock();
    /** Feeds gamepad 1 (driver). */
    private final GamepadDriver driver1;
    /** Feeds gamepad 2 (operator). */
    private final GamepadDriver driver2;
    /** Seconds of simulated time per OpMode call. */
    private double loopPeriod = DEFAULT_LOOP_PERIOD_SECONDS;
    /** How many loop() calls have run (not counting init_loop). */
    private int loopCount;
    /** How far through the lifecycle we are; null before INIT. */
    private RunResult.Stage stage;
    /** Callbacks run after every OpMode call; see {@link #everyLoop}. */
    private final java.util.List<java.util.function.Consumer<OpModeRunner>> watchers = new java.util.ArrayList<>();
    /**
     * Tracks where the robot would be on the field. Created automatically when the fake robot
     * has the four drive motors; null otherwise (then {@link #untrackedReason} says why).
     */
    private PoseEstimator positionEstimator;
    /** Why position isn't tracked, when positionEstimator is null. */
    private String untrackedReason;
    /** Seconds since INIT when the sensors were last stepped, or -1 before the first call. */
    private double sensorsSteppedAt = -1.0;

    /** Team A's drive model, read once from the Pedro configuration and shared by every runner. */
    private static DriveModel defaultDriveModel;

    /**
     * Prepares an OpMode to run, the way the Robot Controller does before INIT.
     *
     * @param opMode      the OpMode to run, e.g. {@code new TeamATeleOp()}
     * @param hardwareMap the fake robot, e.g. {@code RobotConfigs.teamA()}
     */
    public OpModeRunner(OpMode opMode, FakeHardwareMap hardwareMap) {
        this.opMode = opMode;
        this.hardwareMap = hardwareMap;
        Gamepad opModeGamepad1 = new Gamepad();
        Gamepad opModeGamepad2 = new Gamepad();
        driver1 = new GamepadDriver(opModeGamepad1);
        driver2 = new GamepadDriver(opModeGamepad2);
        // Telemetry frames get stamped with simulated match time.
        telemetry.setMatchTimeSource(clock::sinceStart);

        // These are public fields on OpMode; the Robot Controller app fills them the same way.
        opMode.hardwareMap = hardwareMap;
        opMode.telemetry = telemetry;
        opMode.gamepad1 = opModeGamepad1;
        opMode.gamepad2 = opModeGamepad2;
        // Non-public SDK field: where requestOpModeStop() reports to the Robot Controller.
        setField(OPMODE_SERVICES, services);

        // From here on, ElapsedTime and other clocks in robot code follow this run's clock.
        SimTime.install(clock::nanoTime);
        warnIfAgentMissing();

        // Track the robot's position automatically, if it has drive motors to track.
        WheelReader wheels = WheelReader.teamA();
        java.util.List<String> missing = wheels.missingMotors(hardwareMap);
        if (missing.isEmpty()) {
            positionEstimator = new PoseEstimator(defaultDriveModel(), wheels, Pose.ORIGIN);
        } else {
            untrackedReason = "the fake robot has no drive motor(s) " + missing;
        }
        // So the final position can be printed when the test finishes.
        RunnerRegistry.register(this);
    }

    /** Team A's drive model, loaded the first time it is needed. */
    private static synchronized DriveModel defaultDriveModel() {
        if (defaultDriveModel == null) {
            defaultDriveModel = DriveModels.teamA();
        }
        return defaultDriveModel;
    }

    // ---- Setup ----------------------------------------------------------------

    /** Gives gamepad 1 (the driver) a script to follow. Returns this runner for chaining. */
    public OpModeRunner gamepad1(GamepadScript script) {
        driver1.setScript(script);
        return this;
    }

    /** Gives gamepad 2 (the operator) a script to follow. Returns this runner for chaining. */
    public OpModeRunner gamepad2(GamepadScript script) {
        driver2.setScript(script);
        return this;
    }

    /**
     * Lets a live source (keyboard, controller, later Unity) drive gamepad 1. It is applied
     * after any script each loop. Returns this runner for chaining.
     */
    public OpModeRunner gamepad1(GamepadSource source) {
        driver1.setSource(source);
        return this;
    }

    /** Lets a live source drive gamepad 2 (the operator). Returns this runner for chaining. */
    public OpModeRunner gamepad2(GamepadSource source) {
        driver2.setSource(source);
        return this;
    }

    /**
     * Calls {@code watcher} after every OpMode call (init, each init_loop, start, each loop,
     * stop), before time moves on. Use it to record how the robot changes over a run:
     * <pre>{@code runner.everyLoop(r -> powers.add(r.hardwareMap().motor("frontLeft").getPower()));}</pre>
     */
    public OpModeRunner everyLoop(java.util.function.Consumer<OpModeRunner> watcher) {
        watchers.add(watcher);
        return this;
    }

    /** Seconds per loop. Real robots usually run between 5 and 30 ms per loop. */
    public OpModeRunner loopPeriod(double seconds) {
        if (seconds <= 0) {
            throw new IllegalArgumentException("Loop period must be positive.");
        }
        this.loopPeriod = seconds;
        return this;
    }

    // ---- Step-by-step API ---------------------------------------------------------

    /** Presses INIT: calls the OpMode's init() once. Must be the first call. */
    public OpModeRunner init() {
        requireStage(null, "init()");
        stage = RunResult.Stage.INIT;
        cycle(() -> {
            // The OpMode was built before this run's clock existed; restart getRuntime() at INIT.
            opMode.resetRuntime();
            opMode.init();
        });
        return this;
    }

    /** Calls init_loop() {@code times} times, as happens while waiting for PLAY. */
    public OpModeRunner initLoop(int times) {
        requireStage(RunResult.Stage.INIT, "initLoop()");
        stage = RunResult.Stage.INIT_LOOP;
        for (int i = 0; i < times; i++) {
            cycle(opMode::init_loop);
        }
        return this;
    }

    /** Presses PLAY: marks match time 0, then calls the OpMode's start() once. */
    public OpModeRunner start() {
        requireStage(RunResult.Stage.INIT, "start()");
        stage = RunResult.Stage.START;
        clock.markStart();
        cycle(opMode::start);
        return this;
    }

    /** Calls loop() exactly {@code times} times (20 ms of simulated time each by default). */
    public OpModeRunner loop(int times) {
        requireStage(RunResult.Stage.START, "loop()");
        stage = RunResult.Stage.LOOP;
        for (int i = 0; i < times; i++) {
            cycle(opMode::loop);
            loopCount++;
        }
        return this;
    }

    /** Runs loops until {@code seconds} of match time have passed since PLAY, or the OpMode stops itself. */
    public OpModeRunner loopUntil(double matchSeconds) {
        requireStage(RunResult.Stage.START, "loopUntil()");
        stage = RunResult.Stage.LOOP;
        // The 1e-9 allowance stops decimal rounding from adding one extra loop.
        while (clock.sinceStart() < matchSeconds - 1e-9 && !isStopRequested()) {
            cycle(opMode::loop);
            loopCount++;
        }
        return this;
    }

    /** Runs loops for {@code seconds} more of match time. */
    public OpModeRunner loopFor(double seconds) {
        return loopUntil(clock.sinceStart() + seconds);
    }

    /** Presses STOP: calls the OpMode's stop() once. Calling it again does nothing. */
    public OpModeRunner stop() {
        if (stage == RunResult.Stage.STOP) {
            return this;
        }
        stage = RunResult.Stage.STOP;
        cycle(opMode::stop);
        return this;
    }

    // ---- Whole-run API --------------------------------------------------------------

    /** INIT, wait one second, PLAY, run for the length of the period (or until the OpMode stops), STOP. */
    public RunResult run(MatchPeriod period) {
        return run(DEFAULT_INIT_SECONDS, period.seconds());
    }

    /** INIT, init_loop for {@code initSeconds}, PLAY, loop for {@code matchSeconds}, STOP. */
    public RunResult run(double initSeconds, double matchSeconds) {
        Throwable failure = null;
        RunResult.Stage failedStage = null;
        try {
            init();
            stage = RunResult.Stage.INIT_LOOP;
            while (clock.sinceInit() < initSeconds - 1e-9) {
                cycle(opMode::init_loop);
            }
            start();
            loopUntil(matchSeconds);
            stop();
        } catch (RuntimeException | LinkageError | StackOverflowError exception) {
            // Like the Robot Controller, a crash ends the OpMode without calling stop().
            failure = exception;
            failedStage = stage;
        }
        return result(failure, failedStage);
    }

    /** A snapshot of the run so far. */
    public RunResult result() {
        return result(null, null);
    }

    /** Builds a RunResult from the current state, plus a crash if there was one. */
    private RunResult result(Throwable failure, RunResult.Stage failedStage) {
        // Time spent before PLAY: all of it if PLAY never happened.
        double initSeconds = clock.hasStarted() ? clock.sinceInit() - clock.sinceStart() : clock.sinceInit();
        return new RunResult(failure, failedStage, initSeconds, clock.sinceStart(), loopCount,
                isStopRequested(), telemetry, driver1.rumbles(), driver2.rumbles());
    }

    // ---- Accessors ----------------------------------------------------------------------

    /** The OpMode being run. */
    public OpMode opMode() { return opMode; }
    /** The fake hardware: use {@code hardwareMap().motor("frontLeft")} to check motors. */
    public FakeHardwareMap hardwareMap() { return hardwareMap; }
    /** Everything shown on the Driver Station, including history. */
    public FakeTelemetry telemetry() { return telemetry; }
    /** This run's simulated clock (sinceInit, sinceStart). */
    public SimClock clock() { return clock; }
    /** How many loop() calls have run. */
    public int loopCount() { return loopCount; }
    /** The OpMode call that ran last (INIT, INIT_LOOP, START, LOOP or STOP), or null before INIT. */
    public RunResult.Stage stage() { return stage; }
    /** Seconds of simulated time per OpMode call (see {@link #loopPeriod(double)}). */
    public double loopPeriodSeconds() { return loopPeriod; }

    /** Gamepad 1 as held by the driver. Change its fields directly for quick tests. */
    public Gamepad gamepad1() { return driver1.physical(); }

    /** Gamepad 2 as held by the operator. */
    public Gamepad gamepad2() { return driver2.physical(); }

    /** Gamepad 1's driver, including its recorded rumbles. */
    public GamepadDriver driver1() { return driver1; }
    /** Gamepad 2's driver, including its recorded rumbles. */
    public GamepadDriver driver2() { return driver2; }

    /**
     * The automatic position estimate (Team A speeds, starting at the field center), or null
     * when the fake robot has no drive motors. Read the result with {@code positionEstimator().pose()}.
     */
    public PoseEstimator positionEstimator() { return positionEstimator; }

    /**
     * Replaces the automatic position estimate, e.g. to use another drive model or start pose.
     * {@code PoseEstimator.attachTo(...)} calls this. Must be called before the run starts.
     */
    public OpModeRunner usePositionEstimator(PoseEstimator estimator) {
        if (stage != null) {
            throw new IllegalStateException("Set the position estimator before init(), so it sees the whole run.");
        }
        this.positionEstimator = estimator;
        this.untrackedReason = null;
        return this;
    }

    /** The final-position report printed after each test: the estimator's report, or why there is none. */
    public String positionReport() {
        String name = opMode.getClass().getSimpleName();
        if (positionEstimator == null) {
            return "Final robot position (" + name + "): not tracked, because " + untrackedReason + ".";
        }
        return "Final robot position (" + name + "):" + System.lineSeparator() + positionEstimator.report();
    }

    /** True once the OpMode called requestOpModeStop(). */
    public boolean isStopRequested() {
        if (services.isStopRequested()) {
            return true;
        }
        // Also check the SDK's own flag, in case a future SDK sets it directly.
        try {
            return STOP_REQUESTED != null && STOP_REQUESTED.getBoolean(opMode);
        } catch (IllegalAccessException e) {
            return false;
        }
    }

    // ---- Internals ------------------------------------------------------------------------

    /** One Robot Controller cycle: deliver gamepads, call the OpMode, send telemetry, advance time. */
    private void cycle(Runnable opModeCall) {
        // If a test uses two runners, robot code follows whichever one is running now.
        SimTime.install(clock::nanoTime);
        // 0. The robot moves up to now (physics, with the wheel powers from the last call), then
        //    sensors catch up: drive encoders turn with the wheels' real motion, other motors'
        //    encoders with their power, and the Pinpoint moves to the robot's new pose.
        double now = clock.sinceInit();
        if (positionEstimator != null) {
            positionEstimator.advanceTo(hardwareMap, now, clock.hasStarted() ? clock.sinceStart() : 0.0);
        }
        hardwareMap.stepSensors(positionEstimator == null ? null : positionEstimator.pose(),
                sensorsSteppedAt < 0 ? 0.0 : now - sensorsSteppedAt);
        sensorsSteppedAt = now;
        // 1. Deliver gamepad input that is due (-1 means "PLAY not pressed yet").
        double sinceStart = clock.hasStarted() ? clock.sinceStart() : -1.0;
        driver1.deliver(clock.sinceInit(), sinceStart);
        driver2.deliver(clock.sinceInit(), sinceStart);
        // Scripted AprilTag cameras follow simulated time.
        FakeWebcam.stepAll(hardwareMap, clock.sinceInit(), sinceStart);
        // The SDK keeps OpMode.time = seconds since INIT; some OpModes read it.
        opMode.time = clock.sinceInit();

        // 2. Call the OpMode (init, init_loop, start, loop or stop).
        opModeCall.run();

        // 3. The SDK sends any telemetry the OpMode changed but did not send itself.
        if (telemetry.isDirty()) {
            telemetry.update();
        }
        // 4. Update the position estimate, record rumbles, and let watchers look at the robot.
        if (positionEstimator != null) {
            positionEstimator.update(hardwareMap, clock.sinceInit(), clock.sinceStart());
        }
        driver1.collectRumbles(clock.sinceStart());
        driver2.collectRumbles(clock.sinceStart());
        for (java.util.function.Consumer<OpModeRunner> watcher : watchers) {
            watcher.accept(this);
        }
        // 5. Move simulated time forward one loop.
        clock.advance(loopPeriod);
    }

    /**
     * Throws a helpful error if a step is called out of order.
     *
     * @param expectedOrEarlier null = must not have started; INIT = INIT or INIT_LOOP allowed;
     *                          START = START or LOOP allowed
     */
    private void requireStage(RunResult.Stage expectedOrEarlier, String call) {
        boolean ok;
        if (expectedOrEarlier == null) {
            ok = stage == null;
        } else if (expectedOrEarlier == RunResult.Stage.INIT) {
            ok = stage == RunResult.Stage.INIT || stage == RunResult.Stage.INIT_LOOP;
        } else {
            ok = stage == RunResult.Stage.START || stage == RunResult.Stage.LOOP;
        }
        if (!ok) {
            throw new IllegalStateException("Cannot call " + call + " when the OpMode is at stage "
                    + (stage == null ? "not initialized" : stage)
                    + ". Order is init, initLoop, start, loop, stop.");
        }
    }

    /** So the missing-agent warning prints only once per test run, not once per runner. */
    private static boolean agentWarningShown;

    /** Robot code only follows simulated time when the test JVM loaded harness-agent.jar. */
    private static void warnIfAgentMissing() {
        if (!SimTime.isAgentActive() && !agentWarningShown) {
            agentWarningShown = true;
            System.err.println("[harness] WARNING: the harness Java agent is not loaded, so robot code"
                    + " reads the real clock. Run tests through Gradle (gradlew test, or Android Studio"
                    + " with Gradle as the test runner) so timed code is simulated.");
        }
    }

    /** True when robot code's clocks follow the harness's simulated time. */
    public static boolean isTimeSimulated() {
        return SimTime.isAgentActive();
    }

    /** Writes a non-public SDK field on the OpMode, with a clear error if the SDK changed. */
    private void setField(Field field, Object value) {
        if (field == null) {
            throw new IllegalStateException("This FTC SDK version has no field the harness expects."
                    + " The harness may need updating for the new SDK.");
        }
        try {
            field.set(opMode, value);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Could not set " + field.getName(), e);
        }
    }

    /** Finds a non-public field in OpMode or its SDK superclasses. */
    private static Field findOpModeField(String name) {
        for (Class<?> type = OpMode.class; type != null; type = type.getSuperclass()) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);   // allow reading/writing a non-public field
                return field;
            } catch (NoSuchFieldException e) {
                // keep looking in the superclass
            }
        }
        return null;
    }
}
