package pvi.harness.pvi;

import java.nio.file.Path;

import org.firstinspires.ftc.teamcode.opmodes.teleop.TeamATeleOp;
import org.junit.jupiter.api.Test;

import pvi.harness.ds.Axis;
import pvi.harness.ds.GamepadScript;
import pvi.harness.hardware.RobotConfigs;
import pvi.harness.physics.GamePiece;
import pvi.harness.physics.PieceModel;
import pvi.harness.runtime.OpModeRunner;
import pvi.harness.runtime.RunRecorder;
import pvi.harness.runtime.RunRecording;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PVI extra (W20): a ball the robot pushes keeps the speed it was given when the robot stops
 * pushing, rolls on, and slows to a stop by the rolling deceleration (a placeholder). The user
 * saw a POLLEN stop dead instead (in Unity's physics, where the robot's motion didn't reach the
 * balls; this test is the harness physics, which Live mode with Unity physics must match).
 *
 * <p>Team A's TeleOp drives full backward for half a second from the field center into its
 * pre-loads, then lets go of the stick. The recording,
 * {@code build/recordings/pushed-ball-rolls-on.json}, shows it in the viewer and Unity.</p>
 */
class PushedBallRollsOnTest {
    private static final String BALL = "pollen-preload-2";
    /** When the driver lets go of the stick (seconds after PLAY). */
    private static final double LET_GO_SECONDS = 0.5;

    private static RunRecording pushThenStop() {
        GamepadScript driver = new GamepadScript()
                .at(0.0).stick(Axis.LEFT_STICK_Y, 1.0)
                .at(LET_GO_SECONDS).stick(Axis.LEFT_STICK_Y, 0.0);
        OpModeRunner runner = new OpModeRunner(new TeamATeleOp(), RobotConfigs.teamA()).gamepad1(driver);
        RunRecorder recorder = RunRecorder.attachTo(runner);
        runner.run(0.2, 6.0).assertNoException();
        Path saved = recorder.save("pushed-ball-rolls-on");
        System.out.println("Recording to watch: " + saved);
        return recorder.recording();
    }

    /** The ball's x at each loop: the header's position updated by each loop's changes. */
    private static double[] ballX(RunRecording recording) {
        double x = recording.header().pieces().stream().filter(p -> p.id().equals(BALL)).findFirst()
                .orElseThrow().xInches();
        double[] xs = new double[recording.loops().size()];
        for (int i = 0; i < xs.length; i++) {
            for (GamePiece p : recording.loops().get(i).pieces()) {
                if (p.id().equals(BALL)) {
                    x = p.xInches();
                }
            }
            xs[i] = x;
        }
        return xs;
    }

    /**
     * WHAT: After the robot has stopped, the pushed POLLEN keeps rolling (here to the far wall,
     * which bounces it back a little), ends well clear of the robot, and then stops for good.
     */
    @Test
    void aPushedBallRollsOnAfterTheRobotStops() {
        RunRecording recording = pushThenStop();
        double[] xs = ballX(recording);

        // The first loop, after letting go, where the robot is no longer moving.
        int robotStopped = -1;
        for (int i = 1; i < xs.length; i++) {
            RunRecording.LoopRecord loop = recording.loops().get(i);
            RunRecording.LoopRecord before = recording.loops().get(i - 1);
            if (loop.matchTimeSeconds() > LET_GO_SECONDS
                    && Math.abs(loop.pose().xInches() - before.pose().xInches()) < 1e-6) {
                robotStopped = i;
                break;
            }
        }
        assertTrue(robotStopped > 0, "the robot stops after the driver lets go");
        double robotBackEdge = recording.loops().get(robotStopped).pose().xInches() - 9;

        // How far it rolled from then on (it may reach the far wall and bounce back a little).
        double rolledAfterRobotStopped = 0;
        for (int i = robotStopped + 1; i < xs.length; i++) {
            rolledAfterRobotStopped += Math.abs(xs[i] - xs[i - 1]);
        }
        assertTrue(rolledAfterRobotStopped > 6, "the ball rolled on " + rolledAfterRobotStopped + " in after the robot stopped");
        assertTrue(xs[xs.length - 1] < robotBackEdge - 6, "it ends well clear of the robot: " + xs[xs.length - 1]
                + " vs back edge " + robotBackEdge);
        assertEquals(xs[xs.length - 2], xs[xs.length - 1], 1e-9, "and has stopped by the end");
        assertTrue(PieceModel.bioBuzz().rollingDeceleration().value() > 0, "rolling slows it down");
    }
}
