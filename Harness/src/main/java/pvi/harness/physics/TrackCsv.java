package pvi.harness.physics;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import pvi.harness.physics.PoseEstimator.TrackPoint;

/**
 * Turns a robot's track (from {@link PoseEstimator#track()}) into a CSV file that a spreadsheet
 * or graphing tool can open, so you can plot where the robot went.
 *
 * <p>One row per update (one per OpMode call), oldest first, under this header:</p>
 * <pre>
 *   version,since_init_s,match_time_s,x_in,y_in,heading_deg
 * </pre>
 * <ul>
 *   <li>{@code version}: the format version, {@value #VERSION}. It goes up whenever the columns
 *       change, so a reader can tell which layout a file uses.</li>
 *   <li>{@code since_init_s}: seconds since INIT. Plot against this to see the whole run.</li>
 *   <li>{@code match_time_s}: seconds since PLAY, 0 for every row before PLAY.</li>
 *   <li>{@code x_in}, {@code y_in}: robot center in inches, FTC field frame (origin at the
 *       field center, edges at ±72).</li>
 *   <li>{@code heading_deg}: degrees counter-clockwise from +x, between -180 and +180.</li>
 * </ul>
 *
 * <p>Numbers always use a dot for decimals and lines end in "\n", whatever the PC's language,
 * so the same run always gives the same file. The format is also described in the README.</p>
 */
public final class TrackCsv {
    /** Format version written in every row. Raise it (and update the README) when columns change. */
    public static final int VERSION = 1;
    /** The first line of every file. */
    public static final String HEADER = "version,since_init_s,match_time_s,x_in,y_in,heading_deg";
    /** Where {@link PoseEstimator#saveTrackCsv(String)} puts files, relative to the folder tests run in. */
    public static final Path DEFAULT_FOLDER = Path.of("build", "tracks");

    /** Only static methods here; nothing to create. */
    private TrackCsv() {
    }

    /** The whole file as text: the header, then one line per track point. */
    public static String format(List<TrackPoint> track) {
        if (track == null) {
            throw new IllegalArgumentException("Cannot write a CSV for a null track.");
        }
        StringBuilder csv = new StringBuilder(HEADER).append('\n');
        for (TrackPoint point : track) {
            Pose pose = point.pose();
            csv.append(String.format(Locale.ROOT, "%d,%.3f,%.3f,%.3f,%.3f,%.2f",
                    VERSION,
                    point.sinceInitSeconds(),
                    point.matchTimeSeconds(),
                    Pose.tidy(pose.xInches(), 3),
                    Pose.tidy(pose.yInches(), 3),
                    Pose.tidy(pose.headingDegrees(), 2)))
                    .append('\n');
        }
        return csv.toString();
    }

    /**
     * Writes the track to {@code file}, creating its folder if needed, and replacing any older
     * file with the same name.
     *
     * @return the full path of the saved file, to print or open
     */
    public static Path save(List<TrackPoint> track, Path file) {
        String text = format(track);
        Path fullPath = file.toAbsolutePath();
        try {
            if (fullPath.getParent() != null) {
                Files.createDirectories(fullPath.getParent());
            }
            Files.writeString(fullPath, text, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not save the track CSV to " + fullPath, e);
        }
        return fullPath;
    }

    /**
     * {@code <name>.csv} inside {@link #DEFAULT_FOLDER}. Only simple names are allowed, so a
     * test can't write outside the build folder by accident.
     */
    public static Path fileInDefaultFolder(String name) {
        if (name == null || !name.matches("[A-Za-z0-9_.-]+") || name.startsWith(".")) {
            throw new IllegalArgumentException("Track file name \"" + name + "\" is not allowed. Use letters,"
                    + " digits, '-', '_' and '.' only (not starting with '.'), without \".csv\","
                    + " e.g. \"teamAAutonomous\".");
        }
        return DEFAULT_FOLDER.resolve(name + ".csv");
    }
}
