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

import com.qualcomm.robotcore.hardware.DcMotorSimple.Direction;

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
 * problem. The runner, physics and fake robot read their robot facts from here (RP2), so none of
 * them needs the team's Pedro configuration.</p>
 */
public final class HarnessConfig {
    /** The file name, in the Harness folder. */
    public static final String FILE_NAME = "harness.properties";
    /** The layout of the file this harness reads. Goes up whenever keys change (README.md). */
    public static final int VERSION = 3;

    /**
     * One dead-wheel odometry pod from {@code devices.deadWheels}.
     *
     * @param port         the motor port its encoder is plugged into
     * @param parallel     true for a pod along the robot (measures forward), false for one across it
     * @param offsetInches parallel: how far left of the center it is; perpendicular: how far forward
     */
    public record DeadWheel(String port, boolean parallel, double offsetInches) {
    }

    /** The four mecanum drive motors' names in the robot configuration. */
    public record DriveMotorNames(String frontLeft, String frontRight, String rearLeft, String rearRight) {
    }

    /**
     * For each drive motor, the direction setting that makes that wheel drive the robot forward on
     * positive power (see {@code WheelReader}).
     */
    public record DriveMotorDirections(Direction frontLeft, Direction frontRight, Direction rearLeft,
                                       Direction rearRight) {
    }

    /** Every key the file may contain, so a misspelled key is caught instead of ignored. */
    private static final Set<String> KNOWN_KEYS = Set.of(
            "config.version", "teamCode.sourceDir", "teamCode.opModePackages", "host.dependenciesFile",
            "host.skipLibraries",
            "robot.name", "drive.frontLeft", "drive.frontRight", "drive.rearLeft", "drive.rearRight", "pinpoint",
            "hardware.otherMotors", "hardware.servos", "devices.imu", "devices.crServos", "devices.distanceSensors",
            "devices.touchSensors", "devices.colorSensors", "devices.analogInputs", "devices.otos",
            "devices.limelights", "devices.webcams", "devices.deadWheels",
            "drive.frontLeft.direction", "drive.frontRight.direction", "drive.rearLeft.direction",
            "drive.rearRight.direction",
            "drive.forwardSpeedInchesPerSecond", "drive.strafeSpeedInchesPerSecond",
            "drive.forwardCoastInchesPerSecondSquared", "drive.lateralCoastInchesPerSecondSquared",
            "robot.lengthInches", "robot.widthInches", "drive.turnRateDegreesPerSecond",
            "drive.responseTimeSeconds", "drive.maxAccelerationInchesPerSecondSquared", "drive.speedVoltage",
            "pvi.extras", "field", "field.robotObstacles", "viewer.open", "viewer.defaultTeleOp",
            "smokeTest.skipOpModes", "smokeTest.notDrivingTeleOps");

    private final String teamCodeSourceDir;
    private final List<String> opModePackages;
    private final String hostDependenciesFile;
    private final List<String> skipLibraries;
    private final String robotName;
    private final DriveMotorNames driveMotors;
    private final DriveMotorDirections driveDirections;
    private final String pinpointName;
    private final List<String> otherMotors;
    private final List<String> servos;
    private final List<String> imus;
    private final List<String> crServos;
    private final List<String> distanceSensors;
    private final List<String> touchSensors;
    private final List<String> colorSensors;
    private final List<String> analogInputs;
    private final List<String> otos;
    private final List<String> limelights;
    private final List<String> webcams;
    private final List<DeadWheel> deadWheels;
    private final ModelValue forwardSpeed;
    private final ModelValue strafeSpeed;
    private final ModelValue forwardCoast;
    private final ModelValue lateralCoast;
    private final boolean pviExtras;
    private final ModelValue lengthInches;
    private final ModelValue widthInches;
    private final ModelValue turnRate;
    private final ModelValue responseTime;
    private final ModelValue maxAcceleration;
    private final ModelValue speedVoltage;
    private final String field;
    private final boolean viewerOpen;
    private final boolean robotObstacles;
    private final List<String> smokeTestSkipOpModes;
    private final List<String> smokeTestNotDrivingTeleOps;
    private final String viewerDefaultTeleOp;

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
        opModePackages = values.list("teamCode.opModePackages");
        if (opModePackages.isEmpty() && properties.getProperty("teamCode.opModePackages") != null) {
            problems.add("teamCode.opModePackages: needs at least one package, e.g. org.firstinspires.ftc.teamcode");
        }
        hostDependenciesFile = values.text("host.dependenciesFile");
        skipLibraries = values.list("host.skipLibraries");
        robotName = values.text("robot.name");
        driveMotors = new DriveMotorNames(values.text("drive.frontLeft"), values.text("drive.frontRight"),
                values.text("drive.rearLeft"), values.text("drive.rearRight"));
        driveDirections = new DriveMotorDirections(values.direction("drive.frontLeft.direction"),
                values.direction("drive.frontRight.direction"), values.direction("drive.rearLeft.direction"),
                values.direction("drive.rearRight.direction"));
        pinpointName = values.text("pinpoint");
        otherMotors = values.list("hardware.otherMotors");
        servos = values.list("hardware.servos");
        imus = values.list("devices.imu");
        crServos = values.list("devices.crServos");
        distanceSensors = values.list("devices.distanceSensors");
        touchSensors = values.list("devices.touchSensors");
        colorSensors = values.list("devices.colorSensors");
        analogInputs = values.list("devices.analogInputs");
        otos = values.list("devices.otos");
        limelights = values.list("devices.limelights");
        webcams = values.list("devices.webcams");
        deadWheels = values.deadWheels("devices.deadWheels");
        forwardSpeed = values.fact("drive.forwardSpeedInchesPerSecond", ModelValue.INCHES_PER_SECOND);
        strafeSpeed = values.fact("drive.strafeSpeedInchesPerSecond", ModelValue.INCHES_PER_SECOND);
        forwardCoast = values.fact("drive.forwardCoastInchesPerSecondSquared", Dynamics.INCHES_PER_SECOND_SQUARED);
        lateralCoast = values.fact("drive.lateralCoastInchesPerSecondSquared", Dynamics.INCHES_PER_SECOND_SQUARED);
        pviExtras = values.yesNo("pvi.extras");
        lengthInches = values.fact("robot.lengthInches", ModelValue.INCHES);
        widthInches = values.fact("robot.widthInches", ModelValue.INCHES);
        turnRate = values.fact("drive.turnRateDegreesPerSecond", ModelValue.DEGREES_PER_SECOND);
        responseTime = values.fact("drive.responseTimeSeconds", Dynamics.SECONDS);
        maxAcceleration = values.fact("drive.maxAccelerationInchesPerSecondSquared",
                Dynamics.INCHES_PER_SECOND_SQUARED);
        speedVoltage = values.fact("drive.speedVoltage", ModelValue.VOLTS);
        field = values.text("field");
        viewerOpen = values.yesNo("viewer.open");
        robotObstacles = values.yesNo("field.robotObstacles");
        smokeTestSkipOpModes = values.list("smokeTest.skipOpModes");
        smokeTestNotDrivingTeleOps = values.list("smokeTest.notDrivingTeleOps");
        viewerDefaultTeleOp = values.mayBeEmpty("viewer.defaultTeleOp");
    }

    /** Where the robot code is, relative to the Harness folder. */
    public String teamCodeSourceDir() {
        return teamCodeSourceDir;
    }

    /** Java packages searched for {@code @TeleOp} / {@code @Autonomous} OpModes (at least one). */
    public List<String> opModePackages() {
        return opModePackages;
    }

    /** The robot build's library list, relative to the Harness folder. */
    public String hostDependenciesFile() {
        return hostDependenciesFile;
    }

    /** {@code group:name} of the host libraries the harness leaves out (Android only). */
    public List<String> skipLibraries() {
        return skipLibraries;
    }

    /** The robot's name in reports, recordings and the viewer, e.g. "Team A". */
    public String robotName() {
        return robotName;
    }

    /** The drive motors' names in the robot configuration. */
    public DriveMotorNames driveMotors() {
        return driveMotors;
    }

    /** The direction setting that makes each drive wheel push the robot forward. */
    public DriveMotorDirections driveDirections() {
        return driveDirections;
    }

    /** The Pinpoint's name in the robot configuration. */
    public String pinpointName() {
        return pinpointName;
    }

    /** Names of the fake robot's other motors (not the drive), e.g. "intake". May be empty. */
    public List<String> otherMotors() {
        return otherMotors;
    }

    /** Names of the fake robot's servos ({@code hardware.servos}). May be empty. */
    public List<String> servos() {
        return servos;
    }

    /** Names of the fake robot's IMUs ({@code devices.imu}). May be empty. */
    public List<String> imus() {
        return imus;
    }

    /** Names of its continuous-rotation servos ({@code devices.crServos}). May be empty. */
    public List<String> crServos() {
        return crServos;
    }

    /** Names of its distance sensors ({@code devices.distanceSensors}). May be empty. */
    public List<String> distanceSensors() {
        return distanceSensors;
    }

    /** Names of its touch sensors ({@code devices.touchSensors}). May be empty. */
    public List<String> touchSensors() {
        return touchSensors;
    }

    /** Names of its color sensors ({@code devices.colorSensors}). May be empty. */
    public List<String> colorSensors() {
        return colorSensors;
    }

    /** Names of its analog inputs ({@code devices.analogInputs}). May be empty. */
    public List<String> analogInputs() {
        return analogInputs;
    }

    /** Names of its SparkFun OTOS sensors ({@code devices.otos}). May be empty. */
    public List<String> otos() {
        return otos;
    }

    /** Names of its Limelight cameras ({@code devices.limelights}). May be empty. */
    public List<String> limelights() {
        return limelights;
    }

    /** Names of its USB webcams ({@code devices.webcams}). May be empty. */
    public List<String> webcams() {
        return webcams;
    }

    /** Its dead-wheel odometry pods ({@code devices.deadWheels}). May be empty. */
    public List<DeadWheel> deadWheels() {
        return deadWheels;
    }

    /** Full-power forward speed. */
    public ModelValue forwardSpeed() {
        return forwardSpeed;
    }

    /** Full-power sideways speed. */
    public ModelValue strafeSpeed() {
        return strafeSpeed;
    }

    /** How fast the robot slows when coasting forward with zero power on FLOAT (see {@link Dynamics}). */
    public ModelValue forwardCoast() {
        return forwardCoast;
    }

    /** The same for sideways coasting. */
    public ModelValue lateralCoast() {
        return lateralCoast;
    }

    /** True to build PVI's own extras (src/pvi/java and src/pviTest/java). */
    public boolean pviExtras() {
        return pviExtras;
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

    /**
     * True if the field elements (BIOBUZZ: the HIVE frame's feet and lips, the FLOWERs) block the
     * simulated robot, as the walls do.
     */
    public boolean robotObstacles() {
        return robotObstacles;
    }

    /** OpModes (Driver Station names) the run-every-OpMode smoke test leaves out. May be empty. */
    public List<String> smokeTestSkipOpModes() {
        return smokeTestSkipOpModes;
    }

    /**
     * TeleOps (Driver Station names) that aren't meant to drive, such as camera or tuning tools:
     * the smoke test's left-stick drive check skips them (they still get the crash check). May be
     * empty.
     */
    public List<String> smokeTestNotDrivingTeleOps() {
        return smokeTestNotDrivingTeleOps;
    }

    /** The season field: a file in {@code fields/} without ".json". */
    public String field() {
        return field;
    }

    /** Step 15: open the viewer after "gradlew test" finishes. The tests themselves never open a window. */
    public boolean viewerOpen() {
        return viewerOpen;
    }

    /**
     * The TeleOp the viewer drives when it opens: a Driver Station name (e.g. "Team A Mecanum
     * Drive") or a class's simple name (e.g. "TeamATeleOp"). Empty means the first TeleOp.
     */
    public String viewerDefaultTeleOp() {
        return viewerDefaultTeleOp;
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

        /** Text that may be empty, but the key must be there (so a forgotten line is caught). */
        String mayBeEmpty(String key) {
            String value = properties.getProperty(key);
            if (value == null) {
                problems.add(key + ": missing (it may be empty, as in \"" + key + "=\")");
                return "";
            }
            return value.trim();
        }

        /** A comma-separated list, spaces around commas removed. The key must be there; it may be empty. */
        List<String> list(String key) {
            String value = properties.getProperty(key);
            if (value == null) {
                problems.add(key + ": missing (it may be empty, as in \"" + key + "=\")");
                return List.of();
            }
            return value.isBlank() ? List.of() : List.of(value.trim().split("\\s*,\\s*"));
        }

        /** {@code name:parallel:inches} or {@code name:perpendicular:inches} items, comma-separated. */
        List<DeadWheel> deadWheels(String key) {
            List<DeadWheel> pods = new ArrayList<>();
            for (String item : list(key)) {
                String[] parts = item.split("\\s*:\\s*");
                if (parts.length != 3 || !(parts[1].equals("parallel") || parts[1].equals("perpendicular"))) {
                    problems.add(key + ": expected name:parallel:inches or name:perpendicular:inches, got \"" + item + "\"");
                    continue;
                }
                try {
                    pods.add(new DeadWheel(parts[0], parts[1].equals("parallel"), Double.parseDouble(parts[2])));
                } catch (NumberFormatException e) {
                    problems.add(key + ": \"" + parts[2] + "\" in \"" + item + "\" is not a number");
                }
            }
            return List.copyOf(pods);
        }

        /** {@code FORWARD} or {@code REVERSE}. */
        Direction direction(String key) {
            String value = text(key);
            if (value == null) {
                return null;
            }
            if (value.equals("FORWARD")) {
                return Direction.FORWARD;
            }
            if (value.equals("REVERSE")) {
                return Direction.REVERSE;
            }
            problems.add(key + ": expected FORWARD or REVERSE, got \"" + value + "\"");
            return null;
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
