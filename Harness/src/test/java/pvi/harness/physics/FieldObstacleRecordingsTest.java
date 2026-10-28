package pvi.harness.physics;

import java.util.List;

import org.firstinspires.ftc.teamcode.opmodes.teleop.TeamATeleOp;
import org.junit.jupiter.api.Test;

import pvi.harness.ds.Axis;
import pvi.harness.ds.GamepadScript;
import pvi.harness.hardware.RobotConfigs;
import pvi.harness.runtime.OpModeRunner;
import pvi.harness.runtime.RunRecorder;
import pvi.harness.runtime.RunRecording;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Two recordings of loose balls meeting the BIOBUZZ field elements, saved in
 * {@code build/recordings} for the Unity replay (open them with {@code -obstacles} to see the
 * shapes): {@code c15-flower-bracket-bounce.json} (balls roll under a FLOWER head into its wall
 * brackets) and {@code c15-foot-bounce.json} (balls run into the HIVE frame's foot and lip).
 *
 * <p>Each is Team A's TeleOp, started where its pre-loads (just behind the robot) face the target,
 * driving backward for a moment to launch them. The robot stops well short of the field elements.</p>
 */
class FieldObstacleRecordingsTest {
    private static final double POLLEN = GamePiece.radiusInches(GamePiece.POLLEN);

    /** Team A facing -y at (x, y), backing up (toward +y) at full speed for {@code pushSeconds}, then 4 s of rolling. */
    private static RunRecording launchPreloads(double x, double y, double pushSeconds, String name) {
        GamepadScript driver = new GamepadScript()
                .at(0.0).stick(Axis.LEFT_STICK_Y, 1.0)   // full backward: toward +y
                .at(pushSeconds).stick(Axis.LEFT_STICK_Y, 0.0);
        OpModeRunner runner = new OpModeRunner(new TeamATeleOp(), RobotConfigs.teamA()).gamepad1(driver);
        PoseEstimator.attachTo(runner, DriveModels.teamA(), WheelReader.teamA(), Pose.inches(x, y, -90));
        RunRecorder recorder = RunRecorder.attachTo(runner);
        runner.run(0.2, 4.0);
        recorder.save(name);
        return recorder.recording();
    }

    /** Every place a pre-load was during the run. */
    private static List<GamePiece> preloadPositions(RunRecording recording) {
        return recording.loops().stream().flatMap(loop -> loop.pieces().stream())
                .filter(p -> p.id().startsWith("pollen-preload")).toList();
    }

    /**
     * How far a ball's edge is from an obstacle box (x0..x1, y0..y1) seen from above, in inches:
     * 0 when touching, negative when overlapping. Measured from the nearest point of the box, so a
     * ball touching a corner counts the same as one touching a side.
     */
    private static double gap(GamePiece p, double x0, double x1, double y0, double y1) {
        double dx = Math.max(x0 - p.xInches(), Math.max(0, p.xInches() - x1));
        double dy = Math.max(y0 - p.yInches(), Math.max(0, p.yInches() - y1));
        return Math.hypot(dx, dy) - POLLEN;
    }

    /** The closest any pre-load came to the box (see {@link #gap}). */
    private static double closest(List<GamePiece> seen, double x0, double x1, double y0, double y1) {
        return seen.stream().mapToDouble(p -> gap(p, x0, x1, y0, y1)).min().orElse(Double.POSITIVE_INFINITY);
    }

    /**
     * WHAT: Pre-loads launched toward the blue-wall FLOWER roll under its head (the head starts 3.75 in
     * up) and reach its wall brackets, touching them but never going into them.
     *
     * <p>WHY: A recording for checking the FLOWER brackets and heads in the Unity replay, made again
     * on every full test run.</p>
     */
    @Test
    void preloadsRollUnderAFlowerHeadAndBounceOffItsBrackets() {
        List<GamePiece> seen = preloadPositions(launchPreloads(-21.12, 34, 0.3, "c15-flower-bracket-bounce"));

        double bracket1 = closest(seen, -25.29, -24.01, 69.22, 70.5);
        double bracket2 = closest(seen, -22.78, -21.5, 69.22, 70.5);
        assertTrue(Math.min(bracket1, bracket2) < 0.1, "a pre-load reached the brackets: closest " + Math.min(bracket1, bracket2));
        assertTrue(bracket1 > -0.25 && bracket2 > -0.25, "and none went into them: " + bracket1 + ", " + bracket2);
        assertTrue(seen.stream().anyMatch(p -> Math.hypot(p.xInches() + 23.39, p.yInches() - 68.04) < 3.13),
                "a pre-load rolled under the FLOWER head (within 3.13 in of its center)");
    }

    /**
     * WHAT: Pre-loads launched at the blue HIVE frame from underneath reach its far foot and the lip,
     * touching but never going into them, while the robot stays short of the frame.
     */
    @Test
    void preloadsStopAtTheHiveFrameFoot() {
        RunRecording recording = launchPreloads(-18.1, -6, 0.25, "c15-foot-bounce");
        List<GamePiece> seen = preloadPositions(recording);

        double robotBackEdge = recording.loops().stream().mapToDouble(l -> l.pose().yInches()).max().orElseThrow() + 9;
        assertTrue(robotBackEdge < 22.79 - 2 * POLLEN, "the robot stays short of the foot: back edge at y " + robotBackEdge);
        double foot = closest(seen, -18.86, -17.33, 22.79, 24.05);
        double lip = closest(seen, -18.87, 18.87, 24.16, 24.36);
        assertTrue(foot < 0.1, "a pre-load reached the foot: closest " + foot);
        assertTrue(foot > -0.25 && lip > -0.25, "and none went into the foot or the lip: " + foot + ", " + lip);
    }
}
