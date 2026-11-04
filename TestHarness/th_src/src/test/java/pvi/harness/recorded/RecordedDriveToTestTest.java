package pvi.harness.recorded;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import org.firstinspires.ftc.teamcode.opmodes.teleop.TeamATeleOp;
import org.junit.jupiter.api.Test;

import pvi.harness.ds.Axis;
import pvi.harness.ds.Button;
import pvi.harness.ds.GamepadScript;
import pvi.harness.ds.GamepadSource;
import pvi.harness.hardware.RobotConfigs;
import pvi.harness.runtime.OpModeRunner;
import pvi.harness.runtime.RunRecorder;
import pvi.harness.runtime.RunRecording;
import pvi.harness.runtime.RunRecording.GamepadState;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Drive and record, steps D3 and D4: a recorded drive becomes {@link GamepadScript} lines
 * ({@link RecordedInputs}) and then a new test file ({@link RecordedTestWriter}).
 *
 * <p>The "recorded drives" here are made by running the real Team A TeleOp with a script or a
 * live {@link GamepadSource}, exactly what the viewer saves when you drive with Record on.</p>
 */
class RecordedDriveToTestTest {
    /** Where these tests compile generated tests; inside build/, so nothing is left in src/. */
    private static final Path WORK = Path.of("build", "recorded-test-work");

    // ---- D3: recording into script lines ----------------------------------------------------

    /**
     * WHAT: A press and release become one "hold" line, and a stick only gets a line when it
     * changes.
     *
     * <p>WHY: This is what makes a long drive readable: one line per change, not one per loop.</p>
     */
    @Test
    void pressAndReleaseBecomeAHoldAndSticksOnlyWhenTheyChange() {
        GamepadScript script = new GamepadScript()
                .at(0.0).stick(Axis.LEFT_STICK_Y, -1.0)
                .at(1.0).hold(Button.A, 0.2)
                .at(2.0).stick(Axis.LEFT_STICK_Y, 0.0);
        RunRecording recording = drive(script, null, 10, 150);

        List<String> lines = RecordedInputs.from(recording).gamepad1().stream()
                .map(RecordedInputs.Step::toString).toList();

        assertEquals(List.of(
                "at 0.0: stick LEFT_STICK_Y = -1.0",
                "at 1.0: hold A for 0.2 s",
                "at 2.0: stick LEFT_STICK_Y = 0.0"), lines);
        assertTrue(RecordedInputs.from(recording).gamepad2().isEmpty(), "gamepad 2 was never touched");
    }

    /**
     * WHAT: Replaying the script made from a live drive gives the robot code the same input on
     * every loop: buttons exactly, sticks within {@link RecordedInputs#STICK_STEP}.
     *
     * <p>HOW: A live source waves the sticks (like a real controller) and presses buttons before
     * and after PLAY on both gamepads. The script made from that recording is replayed for the
     * same number of loops, and both recordings are compared loop by loop.</p>
     *
     * <p>WHY: If the replay didn't match, a recorded test wouldn't be testing the drive you did.</p>
     */
    @Test
    void replayingTheScriptGivesTheSameInputEveryLoop() {
        RunRecording original = drive(null, wavingSticksAndButtons(), 25, 200);

        RecordedInputs.Inputs inputs = RecordedInputs.from(original);
        RunRecording replay = drive(inputs.gamepad1Script(), inputs.gamepad2Script(), null, 25, 200);

        assertEquals(original.loops().size(), replay.loops().size());
        for (int i = 0; i < original.loops().size(); i++) {
            String where = "loop " + i + " (" + original.loops().get(i).stage() + ", "
                    + original.loops().get(i).sinceInitSeconds() + " s since INIT)";
            assertSame(original.loops().get(i).gamepad1(), replay.loops().get(i).gamepad1(), "gamepad 1 at " + where);
            assertSame(original.loops().get(i).gamepad2(), replay.loops().get(i).gamepad2(), "gamepad 2 at " + where);
        }
    }

    /**
     * WHAT: Tiny stick wobble around a value doesn't add lines.
     *
     * <p>WHY: Real controllers are never perfectly still. Without this, holding a stick at half
     * forward would write a line every loop.</p>
     */
    @Test
    void smallStickWobbleAddsNoLines() {
        GamepadSource wobble = (pad, sinceInit, sinceStart) -> {
            double noise = 0.02 * Math.sin(sinceInit * 37.0);
            Axis.LEFT_STICK_X.set(pad, noise);                          // inside the dead zone
            Axis.LEFT_STICK_Y.set(pad, sinceStart >= 0 ? -0.5 + noise : 0.0);   // half forward, wobbling
        };
        RunRecording recording = drive(null, wobble, 5, 100);

        List<String> lines = RecordedInputs.from(recording).gamepad1().stream()
                .map(RecordedInputs.Step::toString).toList();

        assertEquals(1, lines.size(), "expected one line for the half-forward stick, got " + lines);
        assertTrue(lines.get(0).startsWith("at 0.0: stick LEFT_STICK_Y = -0.5")
                        || lines.get(0).startsWith("at 0.0: stick LEFT_STICK_Y = -0.4"), lines.get(0));
    }

    /**
     * WHAT: A marked moment, such as a controller disconnect, becomes a note on that gamepad.
     *
     * <p>WHY: The generated test shows it as a comment, so you can see why the inputs stop there.</p>
     */
    @Test
    void markedMomentsBecomeNotes() {
        OpModeRunner runner = new OpModeRunner(new TeamATeleOp(), RobotConfigs.teamA());
        RunRecorder recorder = RunRecorder.attachTo(runner);
        runner.init().start();
        runner.loop(10);
        recorder.note("Gamepad 2 controller disconnected");
        runner.loop(1);
        runner.stop();

        List<RecordedInputs.Step> pad2 = RecordedInputs.from(recorder.recording()).gamepad2();

        assertEquals(1, pad2.size());
        assertEquals(RecordedInputs.Kind.NOTE, pad2.get(0).kind());
        assertEquals("Gamepad 2 controller disconnected", pad2.get(0).note());
        assertFalse(RecordedInputs.Inputs.hasInputs(pad2), "a note is not an input");
    }

    // ---- D4: the generated test -------------------------------------------------------------

    /**
     * WHAT: A test generated from a live drive compiles, runs and passes, including its
     * snapshot checks.
     *
     * <p>HOW: The drive is long and wavy enough to need more than one script method per
     * gamepad, and includes inputs before PLAY. The test is written to build/, compiled with
     * the JDK's compiler, and its test method is called.</p>
     *
     * <p>WHY: A generated test that doesn't compile or fails straight away is no use to students.</p>
     */
    @Test
    void generatedTestCompilesRunsAndPasses() throws Exception {
        RunRecording recording = drive(null, wavingSticksAndButtons(), 25, 400);
        String source = RecordedTestWriter.source(recording, options("GeneratedPasses", true));

        assertTrue(source.contains("driverInputsPart2(s);"), "long drives are split into parts");
        assertTrue(source.contains("s.duringInit();"), "inputs before PLAY are kept");
        assertTrue(source.contains("// TODO: add your own checks here"), "there is a place for your own checks");
        assertTrue(source.contains("runner.initLoop(25);") && source.contains("runner.loop(400);"),
                "the generated test runs the same number of loops");

        runGeneratedTest("GeneratedPasses", source);   // throws if anything fails
    }

    /**
     * WHAT: Snapshot checks are written as comments unless asked for.
     *
     * <p>WHY: They fail whenever the robot code changes on purpose, so students turn them on
     * only when they want them.</p>
     */
    @Test
    void snapshotChecksAreCommentedOutByDefault() {
        RunRecording recording = drive(new GamepadScript().at(0.0).stick(Axis.LEFT_STICK_Y, -0.5)
                .at(1.0).stick(Axis.LEFT_STICK_Y, 0.0), null, 5, 100);

        String source = RecordedTestWriter.source(recording, options("Snapshots", false));

        assertTrue(source.contains("        // Pose end = DriveChecks.finalPose(run);"), source);
        assertFalse(source.contains("\n        Pose end ="), "no live snapshot code by default");
    }

    /**
     * WHAT: Breaking one recorded input makes the generated test's snapshot check fail.
     *
     * <p>HOW: The line that lets go of the stick is deleted from the generated test, so the
     * robot keeps driving and ends somewhere else.</p>
     *
     * <p>WHY: Proves the snapshot checks really check the drive.</p>
     */
    @Test
    void changingARecordedInputFailsTheSnapshotCheck() throws Exception {
        RunRecording recording = drive(new GamepadScript()
                .at(0.0).stick(Axis.LEFT_STICK_Y, -0.5)
                .at(0.5).stick(Axis.LEFT_STICK_Y, 0.0), null, 5, 100);
        String source = RecordedTestWriter.source(recording, options("BrokenInput", true));
        String letGo = "        s.at(0.5).stick(Axis.LEFT_STICK_Y, 0.0);\n";
        assertTrue(source.contains(letGo), source);

        AssertionError failure = assertThrows(AssertionError.class,
                () -> runGeneratedTest("BrokenInput", source.replace(letGo, "")));
        assertTrue(failure.getMessage().contains("final x") || failure.getMessage().contains("final y"),
                failure.getMessage());
    }

    /**
     * WHAT: The robot-on-field safety check fails when the robot drives off the field.
     *
     * <p>WHY: It's one of the checks every generated test gets for free.</p>
     */
    @Test
    void onFieldCheckFailsWhenTheRobotLeavesTheField() {
        RunRecording offTheField = drive(new GamepadScript().at(0.0).stick(Axis.LEFT_STICK_Y, -1.0), null, 5, 250);

        AssertionError failure = assertThrows(AssertionError.class,
                () -> DriveChecks.assertRobotStaysOnField(offTheField));
        assertTrue(failure.getMessage().startsWith("The robot left the field at "), failure.getMessage());
        DriveChecks.assertMotorPowersInRange(offTheField);   // the powers themselves were fine
    }

    /**
     * WHAT: If the recorded drive already breaks a safety check (here: 5 s of full forward
     * drives it into the +x wall), the generated test has that check turned off with the
     * reason, and still passes.
     *
     * <p>WHY: A brand-new test should start green. The comment shows the problem so the student
     * can decide whether it's a real bug or just how they drove.</p>
     */
    @Test
    void safetyCheckTheDriveAlreadyFailsIsTurnedOffWithTheReason() throws Exception {
        RunRecording intoTheWall = drive(new GamepadScript().at(0.0).stick(Axis.LEFT_STICK_Y, -1.0), null, 5, 250);
        String source = RecordedTestWriter.source(intoTheWall, options("DroveOffTheField", false));

        assertTrue(source.contains("        // Turned off because the recorded drive already fails it:\n"
                + "        //   The robot hit the +x wall at "), source);
        assertTrue(source.contains("        // DriveChecks.assertRobotDoesNotHitAWall(run);\n"), source);
        assertTrue(source.contains("\n        DriveChecks.assertMotorPowersInRange(run);\n"),
                "checks the drive passes stay on");

        runGeneratedTest("DroveOffTheField", source);   // passes
    }

    /**
     * WHAT: Saving never replaces a file: the second save gets a number added.
     *
     * <p>WHY: Students edit generated tests. Recording another drive must not wipe their work.</p>
     */
    @Test
    void savingNeverOverwritesAnExistingTest() throws IOException {
        Path folder = WORK.resolve("no-overwrite");
        deleteFolder(folder);
        RunRecording recording = drive(new GamepadScript().at(0.0).tap(Button.A), null, 5, 20);

        Path first = RecordedTestWriter.save(recording, folder, options("SavedTwice", false));
        Files.writeString(first, Files.readString(first) + "// a student's edit\n");
        Path second = RecordedTestWriter.save(recording, folder, options("SavedTwice", false));

        assertEquals("SavedTwice.java", first.getFileName().toString());
        assertEquals("SavedTwice2.java", second.getFileName().toString());
        assertTrue(Files.readString(first).endsWith("// a student's edit\n"), "the first file was not touched");
        assertTrue(Files.readString(second).contains("class SavedTwice2 {"));
    }

    /**
     * WHAT: The OpMode's package is found among the robot code's annotated OpModes
     * ({@code OpModeCatalog}); an unknown name fails and says where it looked.
     *
     * <p>WHY: A recording only stores the OpMode's short name, but the test needs an import.</p>
     */
    @Test
    void findsTheOpModePackageInTeamCode() {
        assertEquals(TeamATeleOp.class.getName(), RecordedTestWriter.findOpModeClass("TeamATeleOp"));
        assertEquals(TeamATeleOp.class.getName(),
                RecordedTestWriter.findOpModeClass("TeamATeleOp", RecordedTestWriter.TEAMCODE_SOURCES),
                "the older form gives the same answer");
        IllegalStateException unknown = assertThrows(IllegalStateException.class,
                () -> RecordedTestWriter.findOpModeClass("NoSuchOpMode"));
        assertTrue(unknown.getMessage().contains("org.firstinspires.ftc.teamcode"), unknown.getMessage());
    }

    // ---- Helpers ----------------------------------------------------------------------------

    /** Runs Team A's TeleOp for an exact number of loops, like a viewer drive, and records it. */
    private static RunRecording drive(GamepadScript script1, GamepadSource source1, int initLoops, int loops) {
        return drive(script1, null, source1, initLoops, loops);
    }

    private static RunRecording drive(GamepadScript script1, GamepadScript script2, GamepadSource source1,
                                      int initLoops, int loops) {
        OpModeRunner runner = new OpModeRunner(new TeamATeleOp(), RobotConfigs.teamA());
        // The field walls would stop the robot; these tests need drives that can leave the
        // field, to prove the on-field check fails when it should.
        runner.positionEstimator().withoutWalls();
        if (script1 != null) {
            runner.gamepad1(script1);
        }
        if (script2 != null) {
            runner.gamepad2(script2);
        }
        if (source1 != null) {
            runner.gamepad1(source1);
            runner.gamepad2(source1 instanceof BothGamepads both ? both.second() : (pad, i, s) -> { });
        }
        RunRecorder recorder = RunRecorder.attachTo(runner);
        runner.init();
        runner.initLoop(initLoops);
        runner.start();
        runner.loop(loops);
        runner.stop();
        return recorder.recording();
    }

    /** A live source for gamepad 1 that also carries one for gamepad 2. */
    private interface BothGamepads extends GamepadSource {
        GamepadSource second();
    }

    /**
     * Sticks waving like a real controller (small enough to stay on the field), a trigger, and
     * buttons pressed before and after PLAY, on both gamepads.
     */
    private static BothGamepads wavingSticksAndButtons() {
        return new BothGamepads() {
            @Override
            public void update(com.qualcomm.robotcore.hardware.Gamepad pad, double sinceInit, double sinceStart) {
                Axis.LEFT_STICK_Y.set(pad, sinceStart < 0 ? 0.0 : -0.6 * Math.sin(sinceStart * 3.1));
                Axis.LEFT_STICK_X.set(pad, sinceStart < 0 ? 0.0 : 0.4 * Math.sin(sinceStart * 2.3 + 1.0));
                Axis.RIGHT_STICK_X.set(pad, sinceStart < 0 ? 0.0 : 0.5 * Math.sin(sinceStart * 1.7));
                Button.DPAD_DOWN.set(pad, sinceInit >= 0.10 && sinceInit < 0.20);   // before PLAY
                Button.B.set(pad, sinceStart >= 1.0 && sinceStart < 1.5);
                // Held across PLAY: pressed during INIT, released after PLAY.
                Button.LEFT_BUMPER.set(pad, sinceInit >= 0.30 && (sinceStart < 0 || sinceStart < 0.4));
            }

            @Override
            public GamepadSource second() {
                return (pad, sinceInit, sinceStart) -> {
                    Axis.RIGHT_TRIGGER.set(pad, sinceStart < 0 ? 0.0 : Math.max(0.0, Math.sin(sinceStart * 2.0)));
                    Button.A.set(pad, sinceStart >= 0.5 && sinceStart < 0.62);
                    Button.Y.set(pad, sinceStart >= 2.0);   // never let go
                };
            }
        };
    }

    /** Buttons equal, sticks and triggers within the script's rounding step. */
    private static void assertSame(GamepadState expected, GamepadState actual, String where) {
        assertEquals(expected.buttons(), actual.buttons(), where + ": buttons");
        double tolerance = RecordedInputs.STICK_STEP + 0.006;   // step + rounding to 2 decimals (+ float error)
        assertEquals(expected.leftStickX(), actual.leftStickX(), tolerance, where + ": left stick x");
        assertEquals(expected.leftStickY(), actual.leftStickY(), tolerance, where + ": left stick y");
        assertEquals(expected.rightStickX(), actual.rightStickX(), tolerance, where + ": right stick x");
        assertEquals(expected.rightStickY(), actual.rightStickY(), tolerance, where + ": right stick y");
        assertEquals(expected.leftTrigger(), actual.leftTrigger(), tolerance, where + ": left trigger");
        assertEquals(expected.rightTrigger(), actual.rightTrigger(), tolerance, where + ": right trigger");
    }

    private static RecordedTestWriter.Options options(String className, boolean snapshots) {
        return new RecordedTestWriter.Options(className, TeamATeleOp.class.getName(),
                "a test recording", "2026-10-23", snapshots);
    }

    /**
     * Compiles a generated test into build/ and calls its test method. A failing check comes
     * out as the AssertionError it threw.
     */
    private static void runGeneratedTest(String className, String source) throws Exception {
        Path folder = WORK.resolve(className);
        deleteFolder(folder);
        Path sourceFile = folder.resolve("src").resolve("pvi/harness/recorded/" + className + ".java");
        Path classes = folder.resolve("classes");
        Files.createDirectories(sourceFile.getParent());
        Files.createDirectories(classes);
        Files.writeString(sourceFile, source);

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StringWriter errors = new StringWriter();
        boolean compiled = compiler.getTask(errors, null, null,
                List.of("-classpath", System.getProperty("java.class.path"), "-d", classes.toString(),
                        "-proc:none", "-nowarn"),
                null, compiler.getStandardFileManager(null, null, null).getJavaFileObjects(sourceFile.toFile()))
                .call();
        assertTrue(compiled, "the generated test did not compile:\n" + errors + "\n" + numbered(source));

        try (URLClassLoader loader = new URLClassLoader(new URL[]{classes.toUri().toURL()},
                RecordedDriveToTestTest.class.getClassLoader())) {
            Class<?> testClass = loader.loadClass(RecordedTestWriter.PACKAGE + "." + className);
            var constructor = testClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            Object test = constructor.newInstance();
            Method method = testClass.getDeclaredMethod("replaysTheRecordedDrive");
            method.setAccessible(true);
            try {
                method.invoke(test);
            } catch (InvocationTargetException e) {
                if (e.getCause() instanceof Error error) {
                    throw error;
                }
                throw e;
            }
        }
    }

    private static String numbered(String source) {
        String[] lines = source.split("\n");
        return java.util.stream.IntStream.range(0, lines.length)
                .mapToObj(i -> String.format("%4d  %s", i + 1, lines[i])).collect(Collectors.joining("\n"));
    }

    private static void deleteFolder(Path folder) throws IOException {
        if (!Files.exists(folder)) {
            return;
        }
        try (var paths = Files.walk(folder)) {
            for (Path p : paths.sorted(java.util.Comparator.reverseOrder()).toList()) {
                Files.delete(p);
            }
        }
    }
}
