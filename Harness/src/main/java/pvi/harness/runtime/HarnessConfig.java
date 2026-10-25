package pvi.harness.runtime;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import pvi.harness.physics.Dynamics;
import pvi.harness.physics.ModelValue;

/**
 * The harness settings from {@code harness.properties}: the one file a team changes to use the
 * harness with their own robot (RP1).
 *
 * <p>It says where the robot code is, what the drive motors and Pinpoint are called, and the
 * robot facts the simulation needs (size, turn rate, ...). Each fact is written as
 * {@code number | measured or placeholder | where it came from}, so the harness can never show a
 * guess as a measurement. Every key is listed in README.md.</p>
 *
 * <p>Reading checks everything at once and fails early: a missing key, a key the harness doesn't
 * know (usually a typo), or a value that isn't a number gives one plain message listing every
 * problem. Nothing reads this yet except its tests and {@code build.gradle}; RP2 connects the
 * runner and physics to it.</p>
 */
public final class HarnessConfig {
    /** The file name, in the Harness folder. */
    public static final String FILE_NAME = "harness.properties";
    /** The layout of the file this harness reads. Goes up whenever keys change (README.md). */
    public static final int VERSION = 1;

    /** The four mecanum drive motors' names in the robot configuration. */
    public record DriveMotorNames(String frontLeft, String frontRight, String rearLeft, String rearRight) {
    }

    /** Every key the file may contain, so a misspelled key is caught instead of ignored. */
    private static final Set<String> KNOWN_KEYS = Set.of(
            "config.version", "teamCode.sourceDir", "host.dependenciesFile", "host.skipLibraries",
            "drive.frontLeft", "drive.frontRight", "drive.rearLeft", "drive.rearRight", "pinpoint",
            "robot.lengthInches", "robot.widthInches", "drive.turnRateDegreesPerSecond",
            "drive.responseTimeSeconds", "drive.maxAccelerationInchesPerSecondSquared", "drive.speedVoltage",
            "field", "viewer.open");

    private final String teamCodeSourceDir;
    private final String hostDependenciesFile;
    private final List<String> skipLibraries;
    private final DriveMotorNames driveMotors;
    private final String pinpointName;
    private final ModelValue lengthInches;
    private final ModelValue widthInches;
    private final ModelValue turnRate;
    private final ModelValue responseTime;
    private final ModelValue maxAcceleration;
    private final ModelValue speedVoltage;
    private final String field;
    private final boolean viewerOpen;

    /** Reads {@code harness.properties} from the current folder (the Harness folder in Gradle runs). */
    public static HarnessConfig load() {
        return load(Path.of(FILE_NAME));
    }

    /** Reads a settings file. Fails with a plain message if it is missing or has problems. */
    public static HarnessConfig load(Path file) {
        if (!Files.isRegularFile(file)) {
            throw new IllegalStateException("No harness settings file at " + file.toAbsolutePath()
                    + ". Copy harness.properties into the Harness folder (see README.md).");
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return read(reader, file.toString());
        } catch (IOException e) {
            throw new IllegalStateException("Could not read " + file.toAbsolutePath(), e);
        }
    }

    /** Reads settings written as text, for tests. {@code name} is used in error messages. */
    public static HarnessConfig fromText(String text, String name) {
        try {
            return read(new StringReader(text), name);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read " + name, e);   // a StringReader never fails
        }
    }

    private static HarnessConfig read(Reader reader, String name) throws IOException {
        Properties properties = new Properties();
        properties.load(reader);
        List<String> problems = new ArrayList<>();
        HarnessConfig config = new HarnessConfig(properties, problems);
        for (String key : new TreeSet<>(properties.stringPropertyNames())) {
            if (!KNOWN_KEYS.contains(key)) {
                problems.add(key + ": not a harness setting (check the spelling against README.md)");
            }
        }
        if (!problems.isEmpty()) {
            throw new IllegalArgumentException("Problems in " + name + ":\n  " + String.join("\n  ", problems));
        }
        return config;
    }

    /** Reads every value, adding a line to {@code problems} for each one that is wrong. */
    private HarnessConfig(Properties properties, List<String> problems) {
        Values values = new Values(properties, problems);
        String version = values.text("config.version");
        if (version != null && !version.equals(Integer.toString(VERSION))) {
            problems.add("config.version: this harness reads version " + VERSION + ", the file says " + version);
        }
        teamCodeSourceDir = values.text("teamCode.sourceDir");
        hostDependenciesFile = values.text("host.dependenciesFile");
        String skip = properties.getProperty("host.skipLibraries", "").trim();   // may be empty
        skipLibraries = skip.isEmpty() ? List.of() : List.of(skip.split("\\s*,\\s*"));
        driveMotors = new DriveMotorNames(values.text("drive.frontLeft"), values.text("drive.frontRight"),
                values.text("drive.rearLeft"), values.text("drive.rearRight"));
        pinpointName = values.text("pinpoint");
        lengthInches = values.fact("robot.lengthInches", ModelValue.INCHES);
        widthInches = values.fact("robot.widthInches", ModelValue.INCHES);
        turnRate = values.fact("drive.turnRateDegreesPerSecond", ModelValue.DEGREES_PER_SECOND);
        responseTime = values.fact("drive.responseTimeSeconds", Dynamics.SECONDS);
        maxAcceleration = values.fact("drive.maxAccelerationInchesPerSecondSquared",
                Dynamics.INCHES_PER_SECOND_SQUARED);
        speedVoltage = values.fact("drive.speedVoltage", ModelValue.VOLTS);
        field = values.text("field");
        viewerOpen = values.yesNo("viewer.open");
    }

    /** Where the robot code is, relative to the Harness folder. */
    public String teamCodeSourceDir() {
        return teamCodeSourceDir;
    }

    /** The robot build's library list, relative to the Harness folder. */
    public String hostDependenciesFile() {
        return hostDependenciesFile;
    }

    /** {@code group:name} of the host libraries the harness leaves out (Android only). */
    public List<String> skipLibraries() {
        return skipLibraries;
    }

    /** The drive motors' names in the robot configuration. */
    public DriveMotorNames driveMotors() {
        return driveMotors;
    }

    /** The Pinpoint's name in the robot configuration. */
    public String pinpointName() {
        return pinpointName;
    }

    /** Robot length, front bumper to back bumper. */
    public ModelValue lengthInches() {
        return lengthInches;
    }

    /** Robot width, side bumper to side bumper. */
    public ModelValue widthInches() {
        return widthInches;
    }

    /** Fastest spin at full power. */
    public ModelValue turnRate() {
        return turnRate;
    }

    /** The drive motors' response time (see {@link Dynamics}). */
    public ModelValue responseTime() {
        return responseTime;
    }

    /** The traction limit (see {@link Dynamics}). */
    public ModelValue maxAcceleration() {
        return maxAcceleration;
    }

    /** Battery voltage the drive speeds were measured at. */
    public ModelValue speedVoltage() {
        return speedVoltage;
    }

    /** The season field: a file in {@code fields/} without ".json". */
    public String field() {
        return field;
    }

    /** Step 15 viewer switch (not used yet). Tests never open a window whatever it says. */
    public boolean viewerOpen() {
        return viewerOpen;
    }

    /** Small helper that reads one kind of value and writes down what was wrong. */
    private record Values(Properties properties, List<String> problems) {

        /** A value that must be there and not blank. */
        String text(String key) {
            String value = properties.getProperty(key);
            if (value == null || value.isBlank()) {
                problems.add(key + ": missing (see README.md for an example)");
                return null;
            }
            return value.trim();
        }

        /** {@code true} or {@code false}. */
        boolean yesNo(String key) {
            String value = text(key);
            if (value != null && !value.equals("true") && !value.equals("false")) {
                problems.add(key + ": expected true or false, got \"" + value + "\"");
            }
            return "true".equals(value);
        }

        /** {@code number | measured or placeholder | source}, as a {@link ModelValue}. */
        ModelValue fact(String key, String unit) {
            String value = text(key);
            if (value == null) {
                return null;
            }
            String[] parts = value.split("\\|", 3);
            if (parts.length != 3 || parts[2].isBlank()) {
                problems.add(key + ": expected \"number | measured or placeholder | where it came from\", got \""
                        + value + "\"");
                return null;
            }
            double number;
            try {
                number = Double.parseDouble(parts[0].trim());
            } catch (NumberFormatException e) {
                problems.add(key + ": \"" + parts[0].trim() + "\" is not a number");
                return null;
            }
            if (!(number > 0) || Double.isInfinite(number)) {
                problems.add(key + ": must be a number above zero, got " + parts[0].trim());
                return null;
            }
            String kind = parts[1].trim();
            String source = parts[2].trim();
            if (kind.equals("measured")) {
                return ModelValue.measured(number, unit, source);
            }
            if (kind.equals("placeholder")) {
                return ModelValue.placeholder(number, unit, source);
            }
            problems.add(key + ": the middle part must be measured or placeholder, got \"" + kind + "\"");
            return null;
        }
    }
}
