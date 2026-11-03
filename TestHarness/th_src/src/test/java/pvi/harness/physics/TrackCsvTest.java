package pvi.harness.physics;

import com.qualcomm.robotcore.hardware.DcMotorSimple.Direction;

import org.firstinspires.ftc.teamcode.opmodes.autonomous.TeamAAutoOpMode;
import org.firstinspires.ftc.teamcode.opmodes.teleop.TeamATeleOp;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import pvi.harness.ds.Axis;
import pvi.harness.ds.GamepadScript;
import pvi.harness.hardware.FakeHardwareMap;
import pvi.harness.hardware.RobotConfigs;
import pvi.harness.physics.PoseEstimator.TrackPoint;
import pvi.harness.runtime.OpModeRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Position estimator Step 8: saving the robot's track as a CSV file for graphing.
 *
 * <p>The format (columns, units, version) is described in {@link TrackCsv} and the README.</p>
 */
class TrackCsvTest {

    /**
     * WHAT: The CSV starts with the header, then has one row per track point.
     *
     * <p>HOW: A round-number test robot (4 ft/s = 48 in/s) driven full forward by hand for
     * 0.1 s, updated every 20 ms: 6 track points. The last row must be at 0.1 s and 4.8 in.</p>
     *
     * <p>WHY: Graphing tools read the header to name the columns. A missing or extra row would
     * shift the whole graph.</p>
     */
    @Test
    void csvHasTheHeaderThenOneRowPerTrackPoint() {
        // SET UP: drive motors with the verified directions, all at full forward power.
        FakeHardwareMap hw = RobotConfigs.driveOnly();
        hw.motor("frontLeft").setDirection(Direction.REVERSE);
        hw.motor("rearLeft").setDirection(Direction.REVERSE);
        for (String motor : new String[] {"frontLeft", "frontRight", "rearLeft", "rearRight"}) {
            hw.motor(motor).setPower(1.0);
        }
        PoseEstimator estimator = new PoseEstimator(DriveModel.forTesting("Test bot", 4, 3, 90),
                WheelReader.teamA(), Pose.ORIGIN);

        // RUN: one update at 0 s, then five more, 20 ms apart.
        for (int i = 0; i <= 5; i++) {
            estimator.update(hw, i * 0.020, i * 0.020);
        }
        String[] lines = TrackCsv.format(estimator.track()).split("\n");

        // CHECK
        assertEquals(7, lines.length, "header + 6 rows");
        assertEquals("version,since_init_s,match_time_s,x_in,y_in,heading_deg", lines[0]);
        assertEquals("1,0.000,0.000,0.000,0.000,0.00", lines[1]);
        assertEquals("1,0.100,0.100,4.800,0.000,0.00", lines[6]);
    }

    /**
     * WHAT: Numbers always use a dot for decimals, and tiny negatives print as 0, not -0.
     *
     * <p>HOW: Switches the PC's language to German (which normally writes "0,5") while
     * formatting a point at x = -0.0001 in, y = 1.5 in, heading 30°.</p>
     *
     * <p>WHY: A comma decimal would split one number into two columns. The same run must give
     * exactly the same file on every computer.</p>
     */
    @Test
    void numbersUseADotAndNeverShowMinusZero() {
        Locale before = Locale.getDefault();
        try {
            Locale.setDefault(Locale.GERMANY);
            TrackPoint point = new TrackPoint(0.5, 0.0, new Pose(-0.0001, 1.5, Math.toRadians(30)));

            String[] lines = TrackCsv.format(List.of(point)).split("\n");

            assertEquals("1,0.500,0.000,0.000,1.500,30.00", lines[1]);
        } finally {
            Locale.setDefault(before);
        }
    }

    /**
     * WHAT: Rows before PLAY count up in since_init_s, with match_time_s held at 0.
     *
     * <p>HOW: Runs Team A's autonomous with 1 s of INIT and 2 s of match. The first row is at
     * INIT (0 s, 0 s). Every row before 1 s since INIT has match time 0. The last row is about
     * 3 s since INIT and 2 s of match time.</p>
     *
     * <p>WHY: Without since_init_s, all the INIT rows would pile up at time 0 on a graph.</p>
     */
    @Test
    void rowsBeforePlayCountUpSinceInitWithMatchTimeZero() {
        // SET UP + RUN
        OpModeRunner runner = new OpModeRunner(new TeamAAutoOpMode(), RobotConfigs.teamA());
        runner.run(1.0, 2.0).assertNoException();
        String[] lines = TrackCsv.format(runner.positionEstimator().track()).split("\n");

        // CHECK
        assertTrue(lines[1].startsWith("1,0.000,0.000,"), lines[1]);
        for (int i = 1; i < lines.length; i++) {
            String[] columns = lines[i].split(",");
            double sinceInit = Double.parseDouble(columns[1]);
            double matchTime = Double.parseDouble(columns[2]);
            if (sinceInit < 1.0 - 1e-9) {
                assertEquals(0.0, matchTime, 1e-9, "before PLAY, row " + i + ": " + lines[i]);
            }
        }
        String[] last = lines[lines.length - 1].split(",");
        assertEquals(3.0, Double.parseDouble(last[1]), 0.021, "last row since INIT");
        assertEquals(2.0, Double.parseDouble(last[2]), 0.021, "last row match time");
    }

    /**
     * WHAT: saveTrackCsv writes the file to build/tracks, and it holds the real run.
     *
     * <p>HOW: Team A's TeleOp with the driver holding full forward for 1 s. The saved file must
     * equal {@link TrackCsv#format}, and its last row must show about 62.6 in forward
     * (Team A's forward speed for 1 s, minus up to one loop).</p>
     *
     * <p>WHY: This is how a student gets a file to open in a spreadsheet. Run the test and look
     * in Harness/build/tracks/.</p>
     */
    @Test
    void saveTrackCsvWritesTheRunToBuildTracks() throws IOException {
        // SCRIPT
        GamepadScript driver = new GamepadScript()
                .at(0.0).stick(Axis.LEFT_STICK_Y, -1.0)
                .at(1.0).releaseAll();

        // SET UP + RUN
        OpModeRunner runner = new OpModeRunner(new TeamATeleOp(), RobotConfigs.teamA()).gamepad1(driver);
        runner.run(1.0, 2.0).assertNoException();
        PoseEstimator estimator = runner.positionEstimator();
        Path saved = estimator.saveTrackCsv("TrackCsvTest-teleOpForward");
        System.out.println("Track saved to " + saved);

        // CHECK
        assertTrue(saved.endsWith(Path.of("build", "tracks", "TrackCsvTest-teleOpForward.csv")), saved.toString());
        String text = Files.readString(saved, StandardCharsets.UTF_8);
        assertEquals(TrackCsv.format(estimator.track()), text);
        String[] lines = text.split("\n");
        double lastX = Double.parseDouble(lines[lines.length - 1].split(",")[3]);
        double forwardInchesPerSecond = DriveModels.teamA().forwardInchesPerSecond();
        assertEquals(forwardInchesPerSecond, lastX, forwardInchesPerSecond * 0.021, "about 1 s of full forward");
    }

    /**
     * WHAT: File names with folders or other odd characters are refused, with a clear message.
     *
     * <p>WHY: A name like "../x" would write outside the build folder, where it could clutter
     * or overwrite real files.</p>
     */
    @Test
    void refusesFileNamesThatCouldLeaveTheBuildFolder() {
        for (String badName : new String[] {"../escape", "a/b", "a\\b", "", ".hidden"}) {
            IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                    () -> TrackCsv.fileInDefaultFolder(badName), "should refuse \"" + badName + "\"");
            assertTrue(error.getMessage().contains("not allowed"), error.getMessage());
        }
    }
}
