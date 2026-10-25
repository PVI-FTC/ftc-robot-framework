package pvi.harness.runtime;

import org.firstinspires.ftc.teamcode.opmodes.autonomous.TeamAAutoOpMode;
import org.firstinspires.ftc.teamcode.opmodes.teleop.RumbleDemoTeleOp;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import pvi.harness.hardware.FakeHardwareMap;
import pvi.harness.hardware.RobotConfigs;
import pvi.harness.physics.DriveModels;
import pvi.harness.runtime.RunRecording.LoopRecord;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Position estimator Step 11: the full run recording (one entry per OpMode call), saved as JSON.
 *
 * <p>The recording is what the desktop viewer and the Unity bridge will read, so these tests
 * check its contents on real Team A runs, the JSON layout described in the README, and that
 * the same run always gives the same file, byte for byte.</p>
 */
class RunRecordingTest {
    /** One loop is 20 ms; allow one loop of slack on step boundaries. */
    private static final double ONE_LOOP = 0.021;

    /** Runs Team A's autonomous (1 s INIT, 2 s of match) with a recorder attached. */
    private static RunRecording recordTeamAAutonomous() {
        OpModeRunner runner = new OpModeRunner(new TeamAAutoOpMode(), RobotConfigs.teamA());
        RunRecorder recorder = RunRecorder.attachTo(runner);
        runner.run(1.0, 2.0).assertNoException();
        return recorder.recording();
    }

    /** A loop's power for one motor, failing clearly if the motor is missing. */
    private static double power(LoopRecord loop, String motor) {
        return loop.motors().stream().filter(m -> m.name().equals(motor)).findFirst()
                .orElseThrow(() -> new AssertionError("no motor " + motor + " in " + loop.motors())).power();
    }

    // ---- What gets recorded -----------------------------------------------------------------

    /**
     * WHAT: The recording has one entry per OpMode call, with the right stage on each.
     *
     * <p>HOW: Runs the autonomous with 1 s of INIT and 2 s of match. The stages must read
     * INIT, then init_loops, then exactly one START, then loops, then STOP, with the number of
     * LOOP entries equal to the runner's loop count.</p>
     *
     * <p>WHY: The viewer's timeline and stage display (INIT, LOOP, STOP...) are built from this.</p>
     */
    @Test
    void recordsOneEntryPerOpModeCallWithItsStage() {
        // SET UP + RUN
        OpModeRunner runner = new OpModeRunner(new TeamAAutoOpMode(), RobotConfigs.teamA());
        RunRecorder recorder = RunRecorder.attachTo(runner);
        runner.run(1.0, 2.0).assertNoException();
        List<LoopRecord> loops = recorder.recording().loops();

        // CHECK
        List<RunResult.Stage> stages = new ArrayList<>();
        for (LoopRecord loop : loops) {
            stages.add(loop.stage());
        }
        assertEquals(RunResult.Stage.INIT, stages.get(0));
        assertEquals(RunResult.Stage.STOP, stages.get(stages.size() - 1));
        assertEquals(1, stages.stream().filter(s -> s == RunResult.Stage.START).count(), "one START");
        assertEquals(runner.loopCount(), stages.stream().filter(s -> s == RunResult.Stage.LOOP).count(),
                "one LOOP entry per loop() call");
        int start = stages.indexOf(RunResult.Stage.START);
        assertTrue(stages.subList(1, start).stream().allMatch(s -> s == RunResult.Stage.INIT_LOOP),
                "only init_loops between INIT and START");
        assertEquals(1.0, loops.get(start).sinceInitSeconds(), 1e-9, "PLAY after 1 s of INIT");
        assertEquals(0.0, loops.get(start).matchTimeSeconds(), 1e-9);
    }

    /**
     * WHAT: Each entry holds the motor powers, the autonomous step and the estimated pose.
     *
     * <p>HOW: During TimedDrive every drive motor must be at 0.20, during TimedIntake the intake
     * must run, the steps must appear in order (TimedDrive, TimedIntake, Wait), and the last
     * pose must be about 9.4 in forward (0.20 power x 62.61 in/s x 0.75 s).</p>
     *
     * <p>WHY: These are the things the viewer draws: robot position, wheel powers, step names.</p>
     */
    @Test
    void entriesHoldMotorPowersTheAutoStepAndThePose() {
        // RUN
        List<LoopRecord> loops = recordTeamAAutonomous().loops();

        // CHECK
        List<String> stepOrder = new ArrayList<>();
        for (LoopRecord loop : loops) {
            String step = loop.autoStep();
            if (step != null && (stepOrder.isEmpty() || !stepOrder.get(stepOrder.size() - 1).equals(step))) {
                stepOrder.add(step);
            }
            if ("TimedDrive".equals(step) && loop.matchTimeSeconds() > ONE_LOOP) {
                for (String motor : RobotConfigs.driveMotorNames()) {
                    assertEquals(0.20, power(loop, motor), 1e-9, motor + " at " + loop.matchTimeSeconds());
                }
            }
            if ("TimedIntake".equals(step)) {
                assertTrue(power(loop, "intake") > 0, "intake should run at " + loop.matchTimeSeconds());
            }
        }
        assertEquals(List.of("TimedDrive", "TimedIntake", "Wait"), stepOrder.subList(0, 3));
        double expectedInches = 0.20 * DriveModels.teamA().forwardInchesPerSecond() * 0.75;
        assertEquals(expectedInches, loops.get(loops.size() - 1).pose().xInches(), 0.5);
        assertNull(loops.get(0).autoStep(), "no step during INIT");
    }

    /**
     * WHAT: Each entry keeps exactly the telemetry screens sent during that OpMode call.
     *
     * <p>HOW: The autonomous sends one screen per loop, so every LOOP entry has one screen,
     * and it shows the step name the way the Driver Station would ("Auto Step : ...").</p>
     *
     * <p>WHY: In replay, the viewer's telemetry panel shows the screen for the selected moment.
     * Screens counted twice, or in the wrong loop, would show the wrong text.</p>
     */
    @Test
    void entriesKeepTheTelemetrySentDuringThatCall() {
        // RUN
        List<LoopRecord> loops = recordTeamAAutonomous().loops();

        // CHECK
        for (LoopRecord loop : loops) {
            if (loop.stage() == RunResult.Stage.LOOP) {
                assertEquals(1, loop.telemetry().size(), "screens at " + loop.matchTimeSeconds());
                assertTrue(loop.telemetry().get(0).lines().contains("Auto Step : " + loop.autoStep()),
                        loop.telemetry().get(0).lines().toString());
            }
        }
        assertTrue(loops.get(0).telemetry().get(0).lines().contains("Status : Team A autonomous initialized"),
                "INIT screen: " + loops.get(0).telemetry());
    }

    /**
     * WHAT: The header names the robot, the OpMode and the drive model, flagging placeholders.
     *
     * <p>WHY: Anyone reading the file later must know what was run and which robot numbers
     * were guesses (Team A's turn rate and size are placeholders until Step 10).</p>
     */
    @Test
    void headerDescribesTheRunTheRobotAndItsModel() {
        RunRecording.Header header = recordTeamAAutonomous().header();

        assertEquals(4, header.version());
        assertEquals("inches", header.distanceUnit());
        assertEquals("radians", header.angleUnit());
        assertEquals("seconds", header.timeUnit());
        assertEquals("teamA", header.robotConfig());
        assertEquals("TeamAAutoOpMode", header.opMode());
        assertEquals(MatchPeriod.AUTONOMOUS, header.period(), "from @Autonomous on the OpMode");
        assertEquals(0.020, header.loopPeriodSeconds(), 1e-12);
        assertEquals("Team A", header.driveModel().name());
        assertTrue(header.driveModel().forward().measured(), "forward speed comes from Pedro tuning");
        assertFalse(header.driveModel().turn().measured(), "turn rate is still a placeholder");
        assertFalse(header.driveModel().length().measured(), "length is still a placeholder");
    }

    /**
     * WHAT: Rumbles are recorded with their times.
     *
     * <p>HOW: RumbleDemoTeleOp over a whole TeleOp match buzzes the driver 4 times (PLAY, 90 s,
     * 105 s, 115 s) and never the operator.</p>
     */
    @Test
    void rumblesAreRecorded() {
        OpModeRunner runner = new OpModeRunner(new RumbleDemoTeleOp(), RobotConfigs.teamA());
        RunRecorder recorder = RunRecorder.attachTo(runner);
        runner.run(MatchPeriod.TELEOP).assertNoException();

        RunRecording recording = recorder.recording();

        assertEquals(4, recording.gamepad1Rumbles().size(), recording.gamepad1Rumbles().toString());
        assertEquals(90.0, recording.gamepad1Rumbles().get(1).matchTimeSeconds(), ONE_LOOP);
        assertTrue(recording.gamepad2Rumbles().isEmpty());
    }

    /**
     * WHAT: Servos are recorded by name, and a servo the code never set reads as "not set".
     *
     * <p>HOW: Adds a "claw" servo that Team A's code doesn't use. It must appear in every
     * entry, with no position, and save as {@code "position": null}.</p>
     *
     * <p>WHY: JSON can't hold "not a number", and 0 would wrongly look like a real position.</p>
     */
    @Test
    void servosAreRecordedAndUnsetOnesSaveAsNull() {
        FakeHardwareMap robot = RobotConfigs.teamA();
        robot.addServo("claw");
        OpModeRunner runner = new OpModeRunner(new TeamAAutoOpMode(), robot);
        RunRecorder recorder = RunRecorder.attachTo(runner);
        runner.run(0.1, 0.1).assertNoException();

        RunRecording recording = recorder.recording();

        RunRecording.ServoState claw = recording.loops().get(0).servos().get(0);
        assertEquals("claw", claw.name());
        assertFalse(claw.isSet());
        assertTrue(RunRecordingJson.format(recording).contains("\"servos\": [{\"name\": \"claw\", \"position\": null}]"));
    }

    /**
     * WHAT: Attaching a recorder after INIT is refused with a clear message.
     *
     * <p>WHY: A recording that silently misses the start of the run would replay wrongly.</p>
     */
    @Test
    void recorderMustBeAttachedBeforeInit() {
        OpModeRunner runner = new OpModeRunner(new TeamAAutoOpMode(), RobotConfigs.teamA());
        runner.init();

        IllegalStateException error = assertThrows(IllegalStateException.class, () -> RunRecorder.attachTo(runner));

        assertTrue(error.getMessage().contains("before init()"), error.getMessage());
    }

    // ---- The JSON file ----------------------------------------------------------------------

    /**
     * WHAT: The saved file is valid JSON with the documented layout and version 1.
     *
     * <p>HOW: Saves a real autonomous run, reads it back with the harness JSON reader, and
     * checks the top-level keys, the header, and the keys of every loop entry, motor and
     * drive model value, exactly as the README lists them.</p>
     *
     * <p>WHY: The viewer and Unity read this file. Any renamed or missing field would break
     * them, so the layout is checked here and the version must go up when it changes.</p>
     */
    @Test
    @SuppressWarnings("unchecked")
    void savedJsonHasTheDocumentedShapeAndVersion() throws IOException {
        // SET UP + RUN
        OpModeRunner runner = new OpModeRunner(new TeamAAutoOpMode(), RobotConfigs.teamA());
        RunRecorder recorder = RunRecorder.attachTo(runner);
        runner.run(1.0, 2.0).assertNoException();
        Path saved = recorder.save("RunRecordingTest-teamAAutonomous");
        System.out.println("Recording saved to " + saved);

        // CHECK: where it went, and that it is JSON.
        assertTrue(saved.endsWith(Path.of("build", "recordings", "RunRecordingTest-teamAAutonomous.json")),
                saved.toString());
        Map<String, Object> json = (Map<String, Object>) JsonReader.parse(Files.readString(saved, StandardCharsets.UTF_8));

        // Top level and header.
        assertEquals(List.of("header", "gamepad1Rumbles", "gamepad2Rumbles", "events", "loops"), List.copyOf(json.keySet()));
        Map<String, Object> header = (Map<String, Object>) json.get("header");
        assertEquals(List.of("version", "distanceUnit", "angleUnit", "timeUnit", "robotConfig", "opMode",
                "period", "loopPeriodSeconds", "startPose", "driveModel", "pieces"), List.copyOf(header.keySet()));
        assertEquals("AUTONOMOUS", header.get("period"));
        assertEquals(4.0, header.get("version"));
        assertEquals("radians", header.get("angleUnit"));
        assertEquals(List.of("xInches", "yInches", "headingRadians"),
                List.copyOf(((Map<String, Object>) header.get("startPose")).keySet()));
        Map<String, Object> driveModel = (Map<String, Object>) header.get("driveModel");
        assertEquals(List.of("name", "forward", "strafe", "turn", "length", "width"), List.copyOf(driveModel.keySet()));
        Map<String, Object> turn = (Map<String, Object>) driveModel.get("turn");
        assertEquals(List.of("value", "unit", "measured", "source"), List.copyOf(turn.keySet()));
        assertEquals(Boolean.FALSE, turn.get("measured"));

        // Every loop entry.
        List<Object> loops = (List<Object>) json.get("loops");
        assertEquals(recorder.recording().loops().size(), loops.size());
        for (Object item : loops) {
            Map<String, Object> loop = (Map<String, Object>) item;
            assertEquals(List.of("sinceInitSeconds", "matchTimeSeconds", "stage", "pose", "motors", "servos",
                    "autoStep", "telemetry", "gamepad1", "gamepad2", "aprilTags", "pieces"), List.copyOf(loop.keySet()));
            assertEquals(List.of("buttons", "leftStickX", "leftStickY", "rightStickX", "rightStickY",
                    "leftTrigger", "rightTrigger"), List.copyOf(((Map<String, Object>) loop.get("gamepad1")).keySet()));
            List<Object> motors = (List<Object>) loop.get("motors");
            assertEquals(5, motors.size(), "four drive motors and the intake");
            assertEquals(List.of("name", "power"), List.copyOf(((Map<String, Object>) motors.get(0)).keySet()));
        }
        Map<String, Object> last = (Map<String, Object>) loops.get(loops.size() - 1);
        assertEquals("STOP", last.get("stage"));
        Map<String, Object> firstScreen = (Map<String, Object>)
                ((List<Object>) ((Map<String, Object>) loops.get(0)).get("telemetry")).get(0);
        assertEquals(List.of("lines", "log"), List.copyOf(firstScreen.keySet()));
    }

    /**
     * WHAT: Running the same test twice saves byte-identical files.
     *
     * <p>HOW: Records the autonomous twice into two files and compares every byte. The second
     * run is done with the PC's language set to German, which writes decimals with a comma.</p>
     *
     * <p>WHY: Replays and comparisons only make sense if the same input always gives the same
     * output, on every computer.</p>
     */
    @Test
    void theSameRunSavesByteIdenticalJson() throws IOException {
        OpModeRunner first = new OpModeRunner(new TeamAAutoOpMode(), RobotConfigs.teamA());
        RunRecorder firstRecorder = RunRecorder.attachTo(first);
        first.run(MatchPeriod.AUTONOMOUS).assertNoException();
        Path firstFile = firstRecorder.save("RunRecordingTest-repeat1");

        Locale before = Locale.getDefault();
        Path secondFile;
        try {
            Locale.setDefault(Locale.GERMANY);
            OpModeRunner second = new OpModeRunner(new TeamAAutoOpMode(), RobotConfigs.teamA());
            RunRecorder secondRecorder = RunRecorder.attachTo(second);
            second.run(MatchPeriod.AUTONOMOUS).assertNoException();
            secondFile = secondRecorder.save("RunRecordingTest-repeat2");
        } finally {
            Locale.setDefault(before);
        }

        assertArrayEquals(Files.readAllBytes(firstFile), Files.readAllBytes(secondFile));
    }

    // ---- Reading a saved file back -------------------------------------------------------------

    /**
     * WHAT: A saved recording reads back into exactly the same recording.
     *
     * <p>HOW: Records RumbleDemoTeleOp for a whole match (so there are rumbles too), saves it,
     * loads it, and writes the loaded copy as JSON again. The two texts must be identical, and
     * the loaded copy must have the same number of loops and rumbles. The autonomous, where the
     * robot moves, must also read back unchanged.</p>
     *
     * <p>WHY: The viewer replays saved files. If reading lost or changed anything, the replay
     * would not show what really happened.</p>
     */
    @Test
    void savedRecordingLoadsBackUnchanged() {
        OpModeRunner runner = new OpModeRunner(new RumbleDemoTeleOp(), RobotConfigs.teamA());
        RunRecorder recorder = RunRecorder.attachTo(runner);
        runner.run(MatchPeriod.TELEOP).assertNoException();
        RunRecording original = recorder.recording();
        Path saved = recorder.save("RunRecordingTest-roundTrip");

        RunRecording loaded = RunRecordingJson.load(saved);

        assertEquals(RunRecordingJson.format(original), RunRecordingJson.format(loaded));
        assertEquals(original.loops().size(), loaded.loops().size());
        assertEquals(4, loaded.gamepad1Rumbles().size());
        assertEquals(MatchPeriod.TELEOP, loaded.header().period());

        // That robot never moves, so also check a run where it does (x changes, y stays 0).
        RunRecording moving = recordTeamAAutonomous();
        assertEquals(RunRecordingJson.format(moving), RunRecordingJson.format(RunRecordingJson.parse(RunRecordingJson.format(moving))));
    }

    /**
     * WHAT: A file with another format version is refused, saying which versions are involved.
     *
     * <p>WHY: Reading a newer layout as if it were this one would show wrong values silently.</p>
     */
    @Test
    void refusesOtherFormatVersions() {
        int tooNew = RunRecording.VERSION + 1;
        String json = RunRecordingJson.format(recordTeamAAutonomous())
                .replace("\"version\": " + RunRecording.VERSION + ",", "\"version\": " + tooNew + ",");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> RunRecordingJson.parse(json));

        assertTrue(error.getMessage().contains("version " + tooNew), error.getMessage());
        assertTrue(error.getMessage().contains("reads versions 1 to " + RunRecording.VERSION), error.getMessage());
    }

    /**
     * WHAT: Events marked with note() are saved with their times and read back; version 1 files
     * (from before events existed) still load, with no events.
     *
     * <p>WHY: A live drive marks controller disconnects as events. Recordings saved before that
     * must keep working in the viewer.</p>
     */
    @Test
    void eventsRoundTripAndVersionOneStillLoads() {
        OpModeRunner runner = new OpModeRunner(new org.firstinspires.ftc.teamcode.opmodes.teleop.TeamATeleOp(),
                pvi.harness.hardware.RobotConfigs.teamA());
        RunRecorder recorder = RunRecorder.attachTo(runner);
        runner.init().start();
        runner.loop(5);
        recorder.note("Gamepad 1 controller disconnected");
        RunRecording recording = recorder.recording();

        RunRecording.Event event = recording.events().get(0);
        // Noted between loops: after START and 5 loops, the clock reads 6 x 20 ms since PLAY.
        assertEquals(0.12, event.matchTimeSeconds(), 1e-9);
        assertEquals("Gamepad 1 controller disconnected", event.text());
        assertEquals(recording.events(), RunRecordingJson.parse(RunRecordingJson.format(recording)).events());

        String versionOne = RunRecordingJson.format(recording)
                .replace("\"version\": " + RunRecording.VERSION + ",", "\"version\": 1,")
                .replaceFirst("  \"events\": \\[[^\\]]*\\],\n", "");
        assertFalse(versionOne.contains("\"events\""), "the test should have removed the events field");
        RunRecording old = RunRecordingJson.parse(versionOne);
        assertEquals(List.of(), old.events());
        assertEquals(recording.loops().size(), old.loops().size());
    }

    /**
     * WHAT: A broken file names the exact field that is wrong.
     *
     * <p>HOW: Removes the x position from the first loop's pose. The error must point at
     * {@code loops[0].pose.xInches}.</p>
     *
     * <p>WHY: Someone editing or generating a file (Unity, a script) must be able to find the
     * mistake without reading the harness code.</p>
     */
    @Test
    void brokenFileNamesTheFieldThatIsWrong() {
        String json = RunRecordingJson.format(recordTeamAAutonomous())
                .replaceFirst("\"pose\": \\{\"xInches\": [-0-9.]+, ", "\"pose\": {");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> RunRecordingJson.parse(json));

        assertTrue(error.getMessage().contains("loops[0].pose.xInches"), error.getMessage());
    }

    /**
     * WHAT: Numbers and text are written safely: rounded, with a dot, no "-0", escaped quotes.
     *
     * <p>WHY: Telemetry can contain any text, and a quote or newline that isn't escaped would
     * make the whole file unreadable. JSON has no NaN, so that is refused loudly.</p>
     */
    @Test
    void numbersAndTextAreWrittenSafely() {
        assertEquals("0.2", RunRecordingJson.number(0.2));
        assertEquals("1", RunRecordingJson.number(1.0));
        assertEquals("0", RunRecordingJson.number(-0.0000001));
        assertEquals("-3.141593", RunRecordingJson.number(-Math.PI));
        assertEquals("\"say \\\"hi\\\"\\nnext\\\\line\"", RunRecordingJson.string("say \"hi\"\nnext\\line"));
        assertEquals("say \"hi\"\nnext\\line", JsonReader.parse(RunRecordingJson.string("say \"hi\"\nnext\\line")));
        assertThrows(IllegalArgumentException.class, () -> RunRecordingJson.number(Double.NaN));
    }

    /**
     * WHAT: File names with folders or odd characters are refused, with a clear message.
     *
     * <p>WHY: A name like "../x" would write outside the build folder.</p>
     */
    @Test
    void refusesFileNamesThatCouldLeaveTheBuildFolder() {
        for (String badName : new String[] {"../escape", "a/b", "a\\b", "", ".hidden"}) {
            IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                    () -> RunRecordingJson.fileInDefaultFolder(badName), "should refuse \"" + badName + "\"");
            assertTrue(error.getMessage().contains("not allowed"), error.getMessage());
        }
    }
}
