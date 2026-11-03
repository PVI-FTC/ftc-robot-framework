package pvi.harness.runtime;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;

/**
 * A second robot for some of the OpModes (manager decision D18). Most OpModes run on the robot in
 * {@code harness.properties}; an OpMode written for a different robot is listed in a profile, the
 * file {@code robots/<id>/drive.properties}.
 *
 * <p>A profile has the same keys as {@code harness.properties}, but only the robot's (see
 * {@link HarnessConfig#forProfile}) and only the ones that differ, plus {@code forOpModes}: the
 * OpModes that use it, by Driver Station name or class name. The harness finds profiles by
 * looking in the folder, so adding a robot needs no code. The file is apart from
 * {@code robot.properties}, which only says how a robot looks in Unity.</p>
 *
 * @param id         the folder's name, e.g. "kimcode"; recordings show it as the robot's name
 * @param forOpModes the OpModes that run on this robot
 * @param config     {@code harness.properties} with this profile's keys applied
 */
public record RobotProfile(String id, List<String> forOpModes, HarnessConfig config) {
    /** Where the profiles' folders are, next to {@code harness.properties}. */
    public static final Path FOLDER = Path.of("robots");
    /** The file in a robot's folder that makes it a profile. */
    public static final String FILE_NAME = "drive.properties";
    /** The key listing the OpModes that use a profile. */
    private static final String OPMODES_KEY = "forOpModes";

    /** Every profile in {@link #FOLDER}, on top of {@code base}. */
    public static List<RobotProfile> all(HarnessConfig base) {
        return all(base, FOLDER);
    }

    /**
     * Every profile in {@code folder}, by folder name. Fails with one plain message listing every
     * problem: a wrong key or value, no {@code forOpModes}, or an OpMode that two profiles claim.
     */
    public static List<RobotProfile> all(HarnessConfig base, Path folder) {
        if (!Files.isDirectory(folder)) {
            return List.of();
        }
        List<RobotProfile> profiles = new ArrayList<>();
        List<String> problems = new ArrayList<>();
        Map<String, String> claimedBy = new LinkedHashMap<>();
        try (Stream<Path> robots = Files.list(folder)) {
            for (Path robot : robots.sorted().toList()) {
                Path file = robot.resolve(FILE_NAME);
                if (!Files.isRegularFile(file)) {
                    continue;   // a robot that only has a look (robot.properties)
                }
                RobotProfile profile = read(base, robot.getFileName().toString(), file, problems);
                if (profile == null) {
                    continue;
                }
                profiles.add(profile);
                for (String opMode : profile.forOpModes()) {
                    String other = claimedBy.putIfAbsent(opMode, profile.id());
                    if (other != null) {
                        problems.add("OpMode \"" + opMode + "\" is in " + OPMODES_KEY + " of both " + other + " and "
                                + profile.id() + ". An OpMode runs on one robot.");
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not read the robot profiles in " + folder.toAbsolutePath(), e);
        }
        if (!problems.isEmpty()) {
            throw new IllegalArgumentException(String.join("\n", problems));
        }
        return List.copyOf(profiles);
    }

    /** One profile file, or null with the reason added to {@code problems}. */
    private static RobotProfile read(HarnessConfig base, String id, Path file, List<String> problems) throws IOException {
        Properties keys = new Properties();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            keys.load(reader);
        }
        String opModes = (String) keys.remove(OPMODES_KEY);
        if (opModes == null || opModes.isBlank()) {
            problems.add("Problems in " + file + ":\n  " + OPMODES_KEY
                    + ": missing (the OpModes that use this robot, by Driver Station name or class name)");
            return null;
        }
        try {
            return new RobotProfile(id, List.of(opModes.trim().split("\\s*,\\s*")), base.forProfile(keys, file.toString()));
        } catch (IllegalArgumentException e) {
            problems.add(e.getMessage());
            return null;
        }
    }

    /**
     * The profile an OpMode runs on, or null for the robot in {@code harness.properties}. Fails if
     * two profiles claim the same OpMode, one by its Driver Station name and one by its class name.
     */
    public static RobotProfile forOpMode(List<RobotProfile> profiles, String driverStationName, String simpleName) {
        RobotProfile found = null;
        for (RobotProfile profile : profiles) {
            if (profile.forOpModes().contains(driverStationName) || profile.forOpModes().contains(simpleName)) {
                if (found != null) {
                    throw new IllegalArgumentException("OpMode \"" + driverStationName + "\" (" + simpleName + ") is in "
                            + OPMODES_KEY + " of both " + found.id() + " and " + profile.id()
                            + ". An OpMode runs on one robot.");
                }
                found = profile;
            }
        }
        return found;
    }
}
