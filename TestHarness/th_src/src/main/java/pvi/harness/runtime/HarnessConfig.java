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

import pvi.harness.hardware.CameraSetup;
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
    public static final int VERSION = 6;

    /** The file in a robot's folder that holds its cameras: the only place they are set. */
    public static final String CAMERA_FILE_NAME = "camera.properties";

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
            "robot.name", "robot.id", "drive.frontLeft", "drive.frontRight", "drive.rearLeft", "drive.rearRight", "pinpoint",
            "hardware.otherMotors", "hardware.servos", "devices.imu", "devices.crServos", "devices.distanceSensors",
            "devices.touchSensors", "devices.colorSensors", "devices.analogInputs", "devices.otos",
            "devices.limelights", "devices.blinkers", "devices.deadWheels",
            "drive.frontLeft.direction", "drive.frontRight.direction", "drive.rearLeft.direction",
            "drive.rearRight.direction",
            "drive.forwardSpeedInchesPerSecond", "drive.strafeSpeedInchesPerSecond",
            "drive.forwardCoastInchesPerSecondSquared", "drive.lateralCoastInchesPerSecondSquared",
            "robot.lengthInches", "robot.widthInches", "drive.turnRateDegreesPerSecond",
            "drive.responseTimeSeconds", "drive.maxAccelerationInchesPerSecondSquared", "drive.speedVoltage",
            "pvi.extras", "field", "field.robotObstacles", "viewer.open", "viewer.defaultTeleOp",
            "smokeTest.skipOpModes", "smokeTest.notDrivingTeleOps");

    /** The keys as read, so a robot profile can start from them. */
    private final Properties properties;
    private final String teamCodeSourceDir;
    private final List<String> opModePackages;
    private final String hostDependenciesFile;
    private final List<String> skipLibraries;
    private final String robotName;
    private final String robotId;
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
    private final List<CameraSetup> cameras;
    private final List<String> blinkers;
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
                    + ". Copy harness.properties into the TestHarness/th_src folder (see docs/README.md).");
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            HarnessConfig config = read(reader, file.toString(), RobotProfile.FOLDER);
            // A misspelled robot.id would otherwise just mean "no cameras", with no message.
            if (!Files.isDirectory(RobotProfile.FOLDER.resolve(config.robotId()))) {
                throw new IllegalArgumentException("Problems in " + file + ":\n  robot.id: there's no folder "
                        + RobotProfile.FOLDER.resolve(config.robotId()) + ". Make it, or correct the name.");
            }
            return config;
        } catch (IOException e) {
            throw new IllegalStateException("Could not read " + file.toAbsolutePath(), e);
        }
    }

    /** Reads settings written as text, for tests. {@code name} is used in error messages. */
    public static HarnessConfig fromText(String text, String name) {
        return fromText(text, name, RobotProfile.FOLDER);
    }

    /** The same, with the robots' folders (and so their cameras) in {@code robotsFolder}. */
    public static HarnessConfig fromText(String text, String name, Path robotsFolder) {
        try {
            return read(new StringReader(text), name, robotsFolder);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read " + name, e);   // a StringReader never fails
        }
    }

    private static HarnessConfig read(Reader reader, String name, Path robotsFolder) throws IOException {
        Properties properties = new Properties();
        properties.load(reader);
        return check(properties, name, robotsFolder, new ArrayList<>());
    }

    /** Makes the settings from {@code properties}, failing with every problem at once. */
    private static HarnessConfig check(Properties properties, String name, Path robotsFolder, List<String> problems) {
        HarnessConfig config = new HarnessConfig(properties, robotsFolder, problems);
        for (String key : new TreeSet<>(properties.stringPropertyNames())) {
            if (!KNOWN_KEYS.contains(key) && !isOldCameraKey(key)) {
                problems.add(key + ": not a harness setting (check the spelling against docs/README.md)");
            }
        }
        if (!problems.isEmpty()) {
            throw new IllegalArgumentException("Problems in " + name + ":\n  " + String.join("\n  ", problems));
        }
        return config;
    }

    /**
     * These settings with a robot profile's keys applied ({@link RobotProfile}). A profile may only
     * change the robot: {@code robot.*}, {@code drive.*}, {@code pinpoint}, {@code hardware.*},
     * {@code devices.*} and {@code webcam.*}. The rest (where the code is, the field, the viewer)
     * is one setting for the whole harness. The profile's cameras are its own
     * ({@code <robotsFolder>/<id>/camera.properties}), never this robot's.
     *
     * @param profile      the profile's keys
     * @param name         used in error messages
     * @param id           the profile's folder name
     * @param robotsFolder where the robots' folders are
     */
    HarnessConfig forProfile(Properties profile, String name, String id, Path robotsFolder) {
        Properties merged = new Properties();
        merged.putAll(properties);
        merged.putAll(profile);
        merged.setProperty("robot.id", id);
        List<String> problems = new ArrayList<>();
        for (String key : new TreeSet<>(profile.stringPropertyNames())) {
            if (KNOWN_KEYS.contains(key) && !isRobotKey(key)) {
                problems.add(key + ": a robot profile can't change this; it belongs in " + FILE_NAME);
            }
            if (key.equals("robot.id")) {
                problems.add(key + ": a robot profile's id is its folder's name, here " + id);
            }
        }
        return check(merged, name, robotsFolder, problems);
    }

    /** True for a key that describes the robot, which a profile may change. */
    private static boolean isRobotKey(String key) {
        return key.equals("pinpoint") || key.startsWith("robot.") || key.startsWith("drive.")
                || key.startsWith("hardware.") || key.startsWith("devices.");
    }

    /** True for a camera key in a settings file: cameras moved to each robot's own file. */
    private static boolean isOldCameraKey(String key) {
        return key.equals("devices.webcams") || key.startsWith("webcam.");
    }

    /** Reads every value, adding a line to {@code problems} for each one that is wrong. */
    private HarnessConfig(Properties properties, Path robotsFolder, List<String> problems) {
        this.properties = properties;
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
        robotId = values.text("robot.id");
        if (robotId != null && !robotId.matches("[A-Za-z0-9._-]+")) {
            problems.add("robot.id: must be a folder's name in robots/, got \"" + robotId + "\"");
        }
        if (properties.stringPropertyNames().stream().anyMatch(HarnessConfig::isOldCameraKey)) {
            problems.add("devices.webcams, webcam.*: cameras don't go in this file any more. Move these lines to "
                    + robotsFolder.resolve(String.valueOf(robotId)).resolve(CAMERA_FILE_NAME));
        }
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
        cameras = readCameras(robotId, robotsFolder, problems);
        blinkers = values.list("devices.blinkers");
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

    /** This robot's folder in {@code robots/}, e.g. "team-a". */
    public String robotId() {
        return robotId;
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

    /** Names of its USB webcams ({@code devices.webcams} in its camera file). May be empty. */
    public List<String> webcams() {
        return cameras.stream().map(CameraSetup::name).toList();
    }

    /**
     * Each webcam's mount and view ({@code webcam.<name>.<fact>}), in the order of
     * {@code devices.webcams}, from this robot's own {@code robots/<id>/camera.properties}. Empty
     * if it has no such file.
     */
    public List<CameraSetup> cameras() {
        return cameras;
    }

    /** Names of its hub status lights ({@code devices.blinkers}). May be empty. */
    public List<String> blinkers() {
        return blinkers;
    }

    /**
     * The webcams in {@code <robotsFolder>/<robotId>/camera.properties}: none if there's no such
     * file. Each thing wrong in the file is added to {@code problems}, naming the file.
     */
    private static List<CameraSetup> readCameras(String robotId, Path robotsFolder, List<String> problems) {
        if (robotId == null) {
            return List.of();
        }
        Path file = robotsFolder.resolve(robotId).resolve(CAMERA_FILE_NAME);
        if (!Files.isRegularFile(file)) {
            return List.of();
        }
        Properties keys = new Properties();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            keys.load(reader);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read " + file.toAbsolutePath(), e);
        }
        List<String> fileProblems = new ArrayList<>();
        Values values = new Values(keys, fileProblems);
        List<String> webcams = values.list("devices.webcams");
        List<CameraSetup> setups = values.cameras(webcams);
        for (String key : new TreeSet<>(keys.stringPropertyNames())) {
            if (!key.equals("devices.webcams") && !isWebcamKey(key, webcams)) {
                fileProblems.add(key + ": not a camera setting (check the spelling, and that the webcam is in"
                        + " devices.webcams)");
            }
        }
        fileProblems.forEach(problem -> problems.add(file + ": " + problem));
        return setups;
    }

    /** True for a {@code webcam.<name>.<fact>} key of a webcam in {@code webcams}. */
    private static boolean isWebcamKey(String key, List<String> webcams) {
        for (String name : webcams) {
            for (String fact : CameraSetup.FACT_NAMES) {
                if (key.equals("webcam." + name + "." + fact)) {
                    return true;
                }
            }
            if (key.equals("webcam." + name + ".aprilTags")) {
                return true;
            }
        }
        return false;
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
                problems.add(key + ": missing (see docs/README.md for an example)");
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

        /** Each webcam's {@code webcam.<name>.<fact>} keys, as camera setups. */
        List<CameraSetup> cameras(List<String> names) {
            List<CameraSetup> setups = new ArrayList<>();
            for (String name : names) {
                List<ModelValue> facts = new ArrayList<>();
                for (String fact : CameraSetup.FACT_NAMES) {
                    facts.add(number("webcam." + name + "." + fact, CameraSetup.unitOf(fact)));
                }
                String aprilTags = text("webcam." + name + ".aprilTags");
                if (facts.contains(null) || aprilTags == null) {
                    continue;   // the missing or wrong key is already in problems
                }
                try {
                    setups.add(new CameraSetup(name, facts.get(0), facts.get(1), facts.get(2), facts.get(3),
                            facts.get(4), facts.get(5), facts.get(6), facts.get(7), facts.get(8), facts.get(9),
                            facts.get(10), facts.get(11), aprilTags));
                } catch (IllegalArgumentException e) {
                    problems.add("webcam." + name + ": " + e.getMessage());
                }
            }
            return List.copyOf(setups);
        }

        /** {@code number | measured or placeholder | source}, as a {@link ModelValue}; the number must be above zero. */
        ModelValue fact(String key, String unit) {
            ModelValue fact = number(key, unit);
            if (fact != null && !(fact.value() > 0)) {
                problems.add(key + ": must be a number above zero, got " + fact.value());
                return null;
            }
            return fact;
        }

        /** {@code number | measured or placeholder | source}, as a {@link ModelValue}; any number, zero or below too. */
        ModelValue number(String key, String unit) {
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
            if (Double.isNaN(number) || Double.isInfinite(number)) {
                problems.add(key + ": must be a real number, got " + parts[0].trim());
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
