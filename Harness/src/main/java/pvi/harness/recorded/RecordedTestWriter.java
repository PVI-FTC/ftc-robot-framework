package pvi.harness.recorded;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;

import pvi.harness.hardware.FakeHardwareMap;
import pvi.harness.hardware.RobotConfigs;
import pvi.harness.physics.Pose;
import pvi.harness.recorded.RecordedInputs.Kind;
import pvi.harness.recorded.RecordedInputs.Period;
import pvi.harness.runtime.OpModeRunner;
import pvi.harness.runtime.RunRecorder;
import pvi.harness.recorded.RecordedInputs.Step;
import pvi.harness.runtime.RunRecording;
import pvi.harness.runtime.RunRecording.LoopRecord;
import pvi.harness.runtime.RunRecording.TelemetryScreen;
import pvi.harness.runtime.RunResult;

/**
 * Writes a recorded drive as a new JUnit test that students can open and edit (step D4 of
 * "Drive and record a test").
 *
 * <p>The test follows the usual four steps:</p>
 * <ol>
 *   <li><b>Script</b>: the recorded inputs as {@link pvi.harness.ds.GamepadScript} lines, one
 *       per change (see {@link RecordedInputs});</li>
 *   <li><b>Set up</b>: the same OpMode on the same fake robot;</li>
 *   <li><b>Run</b>: the same number of loops before and after PLAY as the recording;</li>
 *   <li><b>Check</b>: safety checks that suit any run (see {@link DriveChecks}), a marked place
 *       for your own checks, and snapshot checks of where the drive ended, commented out.</li>
 * </ol>
 *
 * <p>An existing file is never overwritten: a number is added to the class name instead.</p>
 */
public final class RecordedTestWriter {
    /** Package the generated tests go in. */
    public static final String PACKAGE = "pvi.harness.recorded";
    /** Where generated tests are saved, relative to {@code Harness/}. */
    public static final Path DEFAULT_FOLDER = Path.of("src", "test", "java", "pvi", "harness", "recorded");
    /** Where TeamCode's sources are, relative to {@code Harness/}; used to find the OpMode's package. */
    public static final Path TEAMCODE_SOURCES = Path.of("..", "TeamCode", "src", "main", "java");
    /** At most this many script lines per method, so very long drives still compile. */
    static final int LINES_PER_METHOD = 300;

    /** Snapshot tolerance for the final position, in inches. */
    static final double POSITION_TOLERANCE_INCHES = 2.0;
    /** Snapshot tolerance for the final heading, in degrees. */
    static final double HEADING_TOLERANCE_DEGREES = 3.0;

    /**
     * What to write, apart from the recording itself.
     *
     * @param className        the test class name, e.g. "RecordedTeamATeleOpDrive"
     * @param opModeClass      the OpMode's full class name, e.g.
     *                         "org.firstinspires.ftc.teamcode.opmodes.teleop.TeamATeleOp"
     * @param source           where the recording came from, for the class comment (e.g. its file name)
     * @param generatedOn      the date to put in the class comment, e.g. "2026-10-23"
     * @param snapshotsEnabled true to write the snapshot checks as real code instead of comments
     */
    public record Options(String className, String opModeClass, String source, String generatedOn,
                          boolean snapshotsEnabled) {
        public Options {
            if (className == null || !className.matches("[A-Z][A-Za-z0-9_]*")) {
                throw new IllegalArgumentException(
                        "A test class name starts with a capital letter and uses only letters, digits and _: " + className);
            }
            if (opModeClass == null || !opModeClass.contains(".")) {
                throw new IllegalArgumentException("Give the OpMode's full class name, with its package: " + opModeClass);
            }
        }
    }

    private RecordedTestWriter() {
    }

    // ---- Saving ----------------------------------------------------------------------------

    /**
     * Writes the test into {@code folder}, never overwriting a file: if {@code BaseName.java}
     * exists it tries {@code BaseName2.java}, {@code BaseName3.java}, and so on.
     *
     * @return the file written
     */
    public static Path save(RunRecording recording, Path folder, Options options) {
        try {
            Files.createDirectories(folder);
            String className = freeClassName(folder, options.className());
            Options named = new Options(className, options.opModeClass(), options.source(),
                    options.generatedOn(), options.snapshotsEnabled());
            Path file = folder.resolve(className + ".java");
            // CREATE_NEW refuses to replace a file, even if one appeared since the name was chosen.
            Files.writeString(file, source(recording, named), StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE_NEW);
            return file;
        } catch (IOException e) {
            throw new UncheckedIOException("Could not write the recorded test in " + folder, e);
        }
    }

    /** The first of Name, Name2, Name3, ... that has no file in {@code folder}. */
    static String freeClassName(Path folder, String baseName) {
        if (!Files.exists(folder.resolve(baseName + ".java"))) {
            return baseName;
        }
        for (int n = 2; ; n++) {
            String candidate = baseName + n;
            if (!Files.exists(folder.resolve(candidate + ".java"))) {
                return candidate;
            }
        }
    }

    /** A class name from the OpMode, e.g. "TeamATeleOp" gives "RecordedTeamATeleOpDrive". */
    public static String defaultClassName(RunRecording recording) {
        String opMode = recording.header().opMode().replaceAll("[^A-Za-z0-9_]", "");
        return "Recorded" + opMode + "Drive";
    }

    /**
     * Finds the OpMode's full class name by looking for {@code <SimpleName>.java} under TeamCode's
     * sources and reading its {@code package} line.
     *
     * @throws IllegalStateException if there is no such file, or more than one
     */
    public static String findOpModeClass(String simpleName, Path teamCodeSources) {
        List<Path> matches;
        try (Stream<Path> files = Files.walk(teamCodeSources)) {
            matches = files.filter(f -> f.getFileName().toString().equals(simpleName + ".java")).toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not search TeamCode in " + teamCodeSources.toAbsolutePath(), e);
        }
        if (matches.size() != 1) {
            throw new IllegalStateException(matches.isEmpty()
                    ? "Could not find " + simpleName + ".java in " + teamCodeSources.toAbsolutePath()
                    : "Found more than one " + simpleName + ".java in TeamCode: " + matches);
        }
        try {
            Matcher m = Pattern.compile("(?m)^\\s*package\\s+([\\w.]+)\\s*;").matcher(Files.readString(matches.get(0)));
            return m.find() ? m.group(1) + "." + simpleName : simpleName;
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + matches.get(0), e);
        }
    }

    // ---- The Java source -------------------------------------------------------------------

    /** The whole test file as text. */
    public static String source(RunRecording recording, Options options) {
        RunRecording.Header header = recording.header();
        String robotMethod = robotConfigMethod(header.robotConfig());
        String opModeSimple = options.opModeClass().substring(options.opModeClass().lastIndexOf('.') + 1);
        RecordedInputs.Inputs inputs = RecordedInputs.from(recording);
        boolean useDriver = RecordedInputs.Inputs.hasInputs(inputs.gamepad1());
        boolean useOperator = RecordedInputs.Inputs.hasInputs(inputs.gamepad2());

        int initLoops = count(recording, RunResult.Stage.INIT_LOOP);
        boolean started = count(recording, RunResult.Stage.START) > 0;
        int loops = count(recording, RunResult.Stage.LOOP);
        double period = header.loopPeriodSeconds();
        boolean defaultPeriod = Math.abs(period - 0.02) < 1e-9;
        // Run the generated inputs once now: the snapshot checks come from this run, and any
        // safety check it already fails is written commented out, so a new test starts green.
        Replay replay = Replay.of(recording, options.opModeClass());

        StringBuilder out = new StringBuilder();
        out.append("package ").append(PACKAGE).append(";\n\n");
        out.append("import ").append(options.opModeClass()).append(";\n");
        out.append("import org.junit.jupiter.api.Test;\n\n");
        out.append("import pvi.harness.ds.Axis;\n");
        out.append("import pvi.harness.ds.Button;\n");
        out.append("import pvi.harness.ds.GamepadScript;\n");
        out.append("import pvi.harness.hardware.RobotConfigs;\n");
        out.append("import pvi.harness.physics.Pose;\n");
        out.append("import pvi.harness.runtime.OpModeRunner;\n");
        out.append("import pvi.harness.runtime.RunRecorder;\n");
        out.append("import pvi.harness.runtime.RunRecording;\n\n");
        out.append("import static org.junit.jupiter.api.Assertions.assertEquals;\n\n");

        // Class comment: what, where from, and how to use it.
        out.append("/**\n");
        out.append(" * Recorded drive: ").append(opModeSimple).append(" on the ").append(header.robotConfig())
                .append(" robot.\n");
        out.append(" *\n");
        out.append(" * <p>Generated on ").append(javadoc(options.generatedOn())).append(" from ")
                .append(javadoc(options.source())).append(".\n");
        out.append(" * ").append(seconds(initLoops * period)).append(" s waiting for PLAY, then ")
                .append(started ? seconds(loops * period) + " s of driving" : "STOP before PLAY")
                .append(".</p>\n");
        out.append(" *\n");
        out.append(" * <p>The script lines are what the driver did, one line per change. Sticks are rounded to\n");
        out.append(" * two decimals, so a replay matches the recording to within ")
                .append(RecordedInputs.number(RecordedInputs.STICK_STEP)).append(". Edit it like any other\n");
        out.append(" * test: add your own checks where it says TODO, or change the inputs to try something new.</p>\n");
        out.append(" */\n");
        out.append("class ").append(options.className()).append(" {\n\n");

        // The test method.
        out.append("    @Test\n");
        out.append("    void replaysTheRecordedDrive() {\n");
        out.append("        // 1. SCRIPT: what the driver (gamepad 1) and operator (gamepad 2) did\n");
        if (useDriver) {
            out.append("        GamepadScript driver = driverInputs();\n");
        }
        if (useOperator) {
            out.append("        GamepadScript operator = operatorInputs();\n");
        }
        if (!useDriver && !useOperator) {
            out.append("        // (The recording has no gamepad input.)\n");
        }
        out.append("\n");
        out.append("        // 2. SET UP: the same OpMode on the same robot\n");
        out.append("        OpModeRunner runner = new OpModeRunner(new ").append(opModeSimple).append("(), RobotConfigs.")
                .append(robotMethod).append("())");
        if (!defaultPeriod) {
            out.append("\n                .loopPeriod(").append(RecordedInputs.number(period)).append(")");
        }
        if (useDriver) {
            out.append("\n                .gamepad1(driver)");
        }
        if (useOperator) {
            out.append("\n                .gamepad2(operator)");
        }
        out.append(";\n");
        out.append("        RunRecorder recorder = RunRecorder.attachTo(runner);\n\n");

        out.append("        // 3. RUN: the same number of loops as the recording\n");
        out.append("        runner.init();\n");
        out.append("        runner.initLoop(").append(initLoops).append(");   // ")
                .append(seconds(initLoops * period)).append(" s waiting for PLAY\n");
        if (started) {
            out.append("        runner.start();\n");
            out.append("        runner.loop(").append(loops).append(");   // ")
                    .append(seconds(loops * period)).append(" s of driving\n");
        }
        out.append("        runner.stop();\n\n");

        out.append("        // 4. CHECK\n");
        out.append("        RunRecording run = recorder.recording();\n");
        out.append("        // Safety checks that suit any run (a crash would already have failed the test above):\n");
        appendSafetyCheck(out, "DriveChecks.assertMotorPowersInRange(run);",
                replay == null || replay.recording() == null ? null
                        : failureOf(() -> DriveChecks.assertMotorPowersInRange(replay.recording())));
        appendSafetyCheck(out, "DriveChecks.assertRobotDoesNotHitAWall(run);",
                replay == null || replay.recording() == null ? null
                        : failureOf(() -> DriveChecks.assertRobotDoesNotHitAWall(replay.recording())));
        out.append("\n");
        out.append("        // TODO: add your own checks here, for example:\n");
        out.append("        //   assertEquals(\"Intaking\", runner.telemetry().lastValue(\"Intake State\"));\n");
        out.append("        //   assertTrue(runner.hardwareMap().motor(\"intake\").getPower() > 0);\n\n");
        appendSnapshots(out, replay, options.snapshotsEnabled());
        out.append("    }\n");

        // The input methods.
        if (useDriver) {
            appendInputMethods(out, "driverInputs", "the driver (gamepad 1)", inputs.gamepad1());
        }
        if (useOperator) {
            appendInputMethods(out, "operatorInputs", "the operator (gamepad 2)", inputs.gamepad2());
        }
        out.append("}\n");
        return out.toString();
    }

    /**
     * Runs the generated script on the same OpMode and robot, for the same loops, and returns
     * its recording. The snapshot checks are taken from this run, not the live drive, because
     * the script rounds stick values: telemetry that shows a stick value, or a long drive's final
     * position, can differ slightly from the live drive, but never from the test's own inputs.
     *
     * @return the replay, or null if the OpMode or robot can't be made here
     * @throws RuntimeException if the OpMode crashes during the replay
     */
    static RunRecording replay(RunRecording recording, String opModeClass) {
        OpMode opMode;
        try {
            opMode = (OpMode) Class.forName(opModeClass).getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException | ClassCastException | LinkageError e) {
            return null;
        }
        FakeHardwareMap robot = switch (robotConfigMethod(recording.header().robotConfig())) {
            case "teamA" -> RobotConfigs.teamA();
            case "teamB" -> RobotConfigs.teamB();
            case "teamC" -> RobotConfigs.teamC();
            default -> RobotConfigs.driveOnly();
        };
        RecordedInputs.Inputs inputs = RecordedInputs.from(recording);
        OpModeRunner runner = new OpModeRunner(opMode, robot)
                .loopPeriod(recording.header().loopPeriodSeconds())
                .gamepad1(inputs.gamepad1Script())
                .gamepad2(inputs.gamepad2Script());
        RunRecorder recorder = RunRecorder.attachTo(runner);
        runner.init();
        runner.initLoop(count(recording, RunResult.Stage.INIT_LOOP));
        if (count(recording, RunResult.Stage.START) > 0) {
            runner.start();
            runner.loop(count(recording, RunResult.Stage.LOOP));
        }
        runner.stop();
        return recorder.recording();
    }

    /**
     * The generated inputs, run once while writing the test.
     *
     * @param recording the replay's recording, or null if it crashed
     * @param crash     what the OpMode threw, or null
     */
    record Replay(RunRecording recording, RuntimeException crash) {

        /** Runs the replay, or returns null if the OpMode or robot can't be made here. */
        static Replay of(RunRecording recording, String opModeClass) {
            try {
                RunRecording run = replay(recording, opModeClass);
                return run == null ? null : new Replay(run, null);
            } catch (RuntimeException e) {
                return new Replay(null, e);
            }
        }
    }

    /** What a check threw, or null if it passed. */
    private static AssertionError failureOf(Runnable check) {
        try {
            check.run();
            return null;
        } catch (AssertionError e) {
            return e;
        }
    }

    /**
     * Writes one safety check, or, if the recorded drive already fails it, the check commented
     * out with the reason, so the student can see it and decide.
     */
    private static void appendSafetyCheck(StringBuilder out, String check, AssertionError alreadyFails) {
        if (alreadyFails == null) {
            out.append("        ").append(check).append("\n");
            return;
        }
        out.append("        // Turned off because the recorded drive already fails it:\n");
        for (String line : wrap(alreadyFails.getMessage(), 90)) {
            out.append("        //   ").append(line).append("\n");
        }
        out.append("        // ").append(check).append("\n");
    }

    /** Splits text into lines of at most {@code width} characters, at spaces. */
    private static List<String> wrap(String text, int width) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : String.valueOf(text).replace('\n', ' ').split(" ")) {
            if (line.length() > 0 && line.length() + 1 + word.length() > width) {
                lines.add(line.toString());
                line.setLength(0);
            }
            if (line.length() > 0) {
                line.append(' ');
            }
            line.append(word);
        }
        if (line.length() > 0) {
            lines.add(line.toString());
        }
        return lines;
    }

    /** Snapshot checks: where the test's own replay ended, and its last Driver Station screen. */
    private static void appendSnapshots(StringBuilder out, Replay replay, boolean enabled) {
        if (replay == null) {
            return;
        }
        if (replay.crash() != null) {
            out.append("        // (No snapshot checks: replaying the recorded inputs crashed with ")
                    .append(replay.crash().getClass().getSimpleName()).append(".)\n");
            return;
        }
        String p = enabled ? "        " : "        // ";
        Pose end = DriveChecks.finalPose(replay.recording());
        List<String[]> values = finalTelemetryValues(replay.recording());
        if (end == null && values.isEmpty()) {
            return;
        }
        out.append("        // SNAPSHOT CHECKS: how this test's inputs ended when it was generated.\n");
        if (!enabled) {
            out.append("        // Remove the // in front of the ones you want.\n");
        }
        out.append("        // They fail if the robot code now behaves differently, so update or delete them\n");
        out.append("        // when you change the code on purpose.\n");
        if (end != null) {
            out.append(p).append("Pose end = DriveChecks.finalPose(run);\n");
            out.append(p).append("assertEquals(").append(fixed(end.xInches(), 2)).append(", end.xInches(), ")
                    .append(RecordedInputs.number(POSITION_TOLERANCE_INCHES)).append(", \"final x (in)\");\n");
            out.append(p).append("assertEquals(").append(fixed(end.yInches(), 2)).append(", end.yInches(), ")
                    .append(RecordedInputs.number(POSITION_TOLERANCE_INCHES)).append(", \"final y (in)\");\n");
            out.append(p).append("assertEquals(0.0, Pose.inches(0, 0, ").append(fixed(end.headingDegrees(), 1))
                    .append(").headingDifferenceDegreesTo(end), ")
                    .append(RecordedInputs.number(HEADING_TOLERANCE_DEGREES)).append(", \"final heading (deg)\");\n");
        }
        for (String[] value : values) {
            out.append(p).append("assertEquals(").append(javaString(value[1]))
                    .append(", runner.telemetry().lastValue(").append(javaString(value[0])).append("));\n");
        }
    }

    /** Script methods, split into parts of {@link #LINES_PER_METHOD} lines. */
    private static void appendInputMethods(StringBuilder out, String name, String who, List<Step> steps) {
        List<List<Step>> parts = new ArrayList<>();
        for (int i = 0; i < steps.size(); i += LINES_PER_METHOD) {
            parts.add(steps.subList(i, Math.min(steps.size(), i + LINES_PER_METHOD)));
        }
        boolean anyInit = steps.stream().anyMatch(s -> s.period() == Period.INIT && s.kind() != Kind.NOTE);

        out.append("\n    /**\n");
        out.append("     * What ").append(who).append(" did, one line per change. Times are seconds after PLAY");
        out.append(anyInit ? ";\n     * lines after duringInit() are seconds after INIT, before PLAY.\n" : ".\n");
        out.append("     */\n");
        out.append("    private static GamepadScript ").append(name).append("() {\n");
        out.append("        GamepadScript s = new GamepadScript();\n");
        Period[] current = {Period.MATCH};
        appendSteps(out, parts.get(0), current);
        for (int i = 1; i < parts.size(); i++) {
            out.append("        ").append(name).append("Part").append(i + 1).append("(s);\n");
        }
        out.append("        return s;\n");
        out.append("    }\n");

        for (int i = 1; i < parts.size(); i++) {
            out.append("\n    /** More of ").append(name).append("(), split up so long drives still compile. */\n");
            out.append("    private static void ").append(name).append("Part").append(i + 1).append("(GamepadScript s) {\n");
            appendSteps(out, parts.get(i), current);
            out.append("    }\n");
        }
    }

    /** One statement per step, switching between duringInit() and afterStart() as needed. */
    private static void appendSteps(StringBuilder out, List<Step> steps, Period[] current) {
        for (Step step : steps) {
            if (step.kind() == Kind.NOTE) {
                // A comment only: it doesn't need duringInit() / afterStart() around it.
                out.append("        // ").append(step.period() == Period.INIT ? "INIT + " : "")
                        .append(RecordedInputs.number(step.time())).append(" s: ")
                        .append(step.note().replace("\n", " ")).append("\n");
                continue;
            }
            if (step.period() != current[0]) {
                current[0] = step.period();
                out.append(current[0] == Period.INIT ? "        s.duringInit();   // before PLAY: seconds after INIT\n"
                        : "        s.afterStart();   // after PLAY: seconds after PLAY\n");
            }
            String at = "        s.at(" + RecordedInputs.number(step.time()) + ")";
            out.append(switch (step.kind()) {
                case STICK -> at + ".stick(Axis." + step.axis() + ", " + RecordedInputs.number(step.value()) + ");\n";
                case PRESS -> at + ".press(Button." + step.button() + ");\n";
                case RELEASE -> at + ".release(Button." + step.button() + ");\n";
                case HOLD -> at + ".hold(Button." + step.button() + ", "
                        + RecordedInputs.number(step.holdSeconds()) + ");\n";
                case NOTE -> "";   // written above
            });
        }
    }

    // ---- Small helpers ---------------------------------------------------------------------

    /** The RobotConfigs method for a recording's robot configuration name. */
    static String robotConfigMethod(String configName) {
        return switch (configName == null ? "" : configName) {
            case "teamA", "teamB", "teamC", "driveOnly" -> configName;
            default -> throw new IllegalArgumentException(
                    "Can't make a test for robot configuration \"" + configName + "\": only teamA, teamB, teamC"
                            + " and driveOnly have a RobotConfigs method. Edit the generated test by hand instead.");
        };
    }

    /** How many entries of one stage the recording has. */
    private static int count(RunRecording recording, RunResult.Stage stage) {
        int n = 0;
        for (LoopRecord loop : recording.loops()) {
            if (loop.stage() == stage) {
                n++;
            }
        }
        return n;
    }

    /**
     * "caption : value" pairs from the last telemetry screen sent, which is what
     * {@code runner.telemetry().lastValue(...)} reads. Captions shown twice are left out,
     * because lastValue() can only check one of them.
     */
    private static List<String[]> finalTelemetryValues(RunRecording recording) {
        for (int i = recording.loops().size() - 1; i >= 0; i--) {
            LoopRecord loop = recording.loops().get(i);
            if (loop.telemetry().isEmpty()) {
                continue;
            }
            TelemetryScreen screen = loop.telemetry().get(loop.telemetry().size() - 1);
            List<String[]> values = new ArrayList<>();
            List<String> repeated = new ArrayList<>();
            for (String line : screen.lines()) {
                int split = line.indexOf(" : ");
                if (split <= 0) {
                    continue;
                }
                String caption = line.substring(0, split);
                if (values.removeIf(v -> v[0].equals(caption)) || repeated.contains(caption)) {
                    repeated.add(caption);
                    continue;
                }
                values.add(new String[]{caption, line.substring(split + 3)});
            }
            return values;
        }
        return List.of();
    }

    /** A Java string literal, with quotes, backslashes and non-ASCII characters escaped. */
    static String javaString(String text) {
        StringBuilder out = new StringBuilder("\"");
        for (char c : text.toCharArray()) {
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 32 || c > 126) {
                        out.append(String.format(Locale.ROOT, "\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.append('"').toString();
    }

    /** Text safe inside a Javadoc comment. */
    private static String javadoc(String text) {
        return text == null ? "" : text.replace("*/", "* /").replace("\n", " ");
    }

    /** Seconds with two decimals, e.g. "12.00". */
    private static String seconds(double seconds) {
        return String.format(Locale.ROOT, "%.2f", seconds);
    }

    /** A number with a fixed count of decimals, as Java source, e.g. "-3.20". */
    private static String fixed(double value, int decimals) {
        String text = String.format(Locale.ROOT, "%." + decimals + "f", value);
        return text.startsWith("-0.") && Double.parseDouble(text) == 0.0 ? text.substring(1) : text;
    }
}
