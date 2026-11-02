package pvi.harness.viewer;

import org.firstinspires.ftc.teamcode.opmodes.teleop.TeamATeleOp;
import org.junit.jupiter.api.Test;

import pvi.harness.ds.Axis;
import pvi.harness.ds.GamepadScript;
import pvi.harness.hardware.RobotConfigs;
import pvi.harness.physics.DriveModels;
import pvi.harness.runtime.OpModeRunner;
import pvi.harness.runtime.RunRecorder;
import pvi.harness.runtime.RunRecording;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * W2: the viewer shows the robot's momentum. Speed, speeding up / slowing down, and wall hits
 * are worked out from a recording (no new fields needed in the file).
 */
class RecordingMotionTest {

    /** Full forward from the field center for 2 s: about 1 s to reach the +x wall at 63 in. */
    private static RecordingView driveIntoTheWall() {
        GamepadScript driver = new GamepadScript()
                .at(0.0).stick(Axis.LEFT_STICK_Y, -1.0)
                .at(2.0).stick(Axis.LEFT_STICK_Y, 0.0);
        OpModeRunner runner = new OpModeRunner(new TeamATeleOp(), RobotConfigs.teamA()).gamepad1(driver);
        RunRecorder recorder = RunRecorder.attachTo(runner);
        runner.run(0.2, 3.0);
        return new RecordingView(recorder.recording());
    }

    private static int loopAt(RecordingView view, double matchSeconds) {
        for (int i = 0; i < view.size(); i++) {
            RunRecording.LoopRecord loop = view.loop(i);
            if (loop.stage() == pvi.harness.runtime.RunResult.Stage.LOOP && loop.matchTimeSeconds() >= matchSeconds - 1e-9) {
                return i;
            }
        }
        throw new AssertionError("no loop at " + matchSeconds + " s");
    }

    /**
     * WHAT: Just after the stick goes forward the robot is speeding up (positive speed change);
     * a little later it is near its full forward speed.
     *
     * <p>WHY: This is the momentum the viewer's Motion card shows.</p>
     */
    @Test
    void speedRisesWhileTheRobotSpeedsUp() {
        RecordingView view = driveIntoTheWall();
        RecordingView.Motion early = view.motion(loopAt(view, 0.1));
        RecordingView.Motion later = view.motion(loopAt(view, 0.6));

        assertTrue(early.speedChangePerSecond() > 50, "speeding up hard at first: " + early);
        assertTrue(early.speedInchesPerSecond() < later.speedInchesPerSecond(), "faster later: " + early + " vs " + later);
        assertEquals(DriveModels.teamA().forwardInchesPerSecond(), later.speedInchesPerSecond(), 6.0,
                "near full speed after 0.6 s: " + later);
        assertEquals(0.0, later.turnDegreesPerSecond(), 1e-6, "driving straight");
    }

    /**
     * WHAT: Hitting the +x wall stops the robot and shows as a recent wall hit for a moment,
     * then no longer.
     *
     * <p>WHY: The viewer flashes that wall red when the robot hits it.</p>
     */
    @Test
    void aWallHitIsSeenForAMoment() {
        RecordingView view = driveIntoTheWall();
        int hitLoop = -1;
        for (int i = 0; i < view.size(); i++) {
            if (view.wallHitWithin(i, 0.02) != null) {
                hitLoop = i;
                break;
            }
        }
        assertTrue(hitLoop > 0, "the drive should hit a wall");
        assertEquals("+x", view.wallHitWithin(hitLoop, 0.5));
        assertTrue(view.motion(hitLoop).speedChangePerSecond() < -100, "stopped by the wall: " + view.motion(hitLoop));
        assertNull(view.wallHitWithin(Math.min(view.size() - 1, hitLoop + 50), 0.5), "the flash is over 1 s later");
        assertNull(view.wallHitWithin(loopAt(view, 0.2), 0.5), "no hit before reaching the wall");
    }

    /**
     * WHAT: The side panel describes the motion in words: speed, speeding up or slowing down,
     * and turning.
     *
     * <p>WHY: The Motion card shows these; the same text goes to screen readers.</p>
     */
    @Test
    void sidePanelDescribesTheMotion() {
        RecordingView view = driveIntoTheWall();
        String early = String.join("\n", SidePanelText.describe(view, loopAt(view, 0.1)));
        String steady = String.join("\n", SidePanelText.describe(view, loopAt(view, 2.8)));

        assertTrue(early.contains("Motion: ") && early.contains("speeding up"), early);
        assertTrue(steady.contains("Motion: 0.0 in/s, steady"), "stopped at the wall by then: " + steady);
    }
}
