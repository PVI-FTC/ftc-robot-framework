package pvi.harness;

import java.util.ArrayList;
import java.util.List;

import org.firstinspires.ftc.teamcode.opmodes.teleop.TeamATeleOp;
import org.junit.jupiter.api.Test;

import pvi.harness.ds.Axis;
import pvi.harness.ds.Button;
import pvi.harness.ds.GamepadScript;
import pvi.harness.ds.GamepadSource;
import pvi.harness.hardware.RobotConfigs;
import pvi.harness.runtime.OpModeRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Drive and record, step D1: live input reaches robot code through {@link GamepadSource}.
 *
 * <p>The keyboard in the viewer, real controllers and (later) Unity all use this one entry
 * point, so these tests check it against the real Team A TeleOp.</p>
 */
class GamepadSourceTest {

    /**
     * WHAT: A live source pushing the left stick forward makes every drive wheel go forward.
     *
     * <p>WHY: This is the path the keyboard and controllers use. If it didn't reach the OpMode,
     * live driving would do nothing.</p>
     */
    @Test
    void liveSourceDrivesTheRealTeleOp() {
        GamepadSource stickForward = (pad, sinceInit, sinceStart) -> Axis.LEFT_STICK_Y.set(pad, -1.0);
        OpModeRunner runner = new OpModeRunner(new TeamATeleOp(), RobotConfigs.teamA()).gamepad1(stickForward);

        runner.init().start();
        runner.loop(5);

        for (String motor : List.of("frontLeft", "frontRight", "rearLeft", "rearRight")) {
            assertTrue(Math.abs(runner.hardwareMap().motor(motor).getPower()) > 0.5,
                    motor + " should be driving, power was " + runner.hardwareMap().motor(motor).getPower());
        }
    }

    /**
     * WHAT: A button the source holds for a few loops counts as one press, as on the robot.
     *
     * <p>HOW: The operator's A is held from 0.1 s to 0.2 s after PLAY. The TeleOp starts the
     * intake on the press.</p>
     *
     * <p>WHY: Live input must go through the SDK's {@code Gamepad.copy()} so edge detection
     * ({@code wasAJustPressed()}) works. Setting the OpMode's gamepad directly would break it.</p>
     */
    @Test
    void heldButtonFromSourceIsOnePress() {
        GamepadSource tapA = (pad, sinceInit, sinceStart) ->
                Button.A.set(pad, sinceStart >= 0.1 && sinceStart < 0.2);
        OpModeRunner runner = new OpModeRunner(new TeamATeleOp(), RobotConfigs.teamA()).gamepad2(tapA);

        runner.init().start();
        runner.loopUntil(0.5);

        assertEquals("Intaking", runner.telemetry().lastValue("Intake State"));
    }

    /**
     * WHAT: The source is called once per OpMode call, with the simulated times, and runs after
     * the script.
     *
     * <p>WHY: Live input is paced by simulated time like everything else, so a recorded drive
     * replays exactly. Running after the script means live input wins if both set the same
     * input.</p>
     */
    @Test
    void sourceRunsEveryCallAfterTheScript() {
        List<Double> sinceStartSeen = new ArrayList<>();
        List<Double> stickSeenBySource = new ArrayList<>();
        GamepadSource watcher = (pad, sinceInit, sinceStart) -> {
            sinceStartSeen.add(sinceStart);
            stickSeenBySource.add(Axis.LEFT_STICK_Y.read(pad));
            Axis.LEFT_STICK_Y.set(pad, 0.0);   // overrides the script's stick
        };
        GamepadScript script = new GamepadScript().at(0.0).stick(Axis.LEFT_STICK_Y, -1.0);
        OpModeRunner runner = new OpModeRunner(new TeamATeleOp(), RobotConfigs.teamA())
                .gamepad1(script).gamepad1(watcher);

        runner.init().start();
        runner.loop(2);

        // init, start, loop, loop: four calls; before PLAY the time is negative.
        assertEquals(4, sinceStartSeen.size());
        assertTrue(sinceStartSeen.get(0) < 0, "INIT is before PLAY");
        assertEquals(0.0, sinceStartSeen.get(1), 1e-9);
        assertEquals(0.02, sinceStartSeen.get(2), 1e-9);
        // The script had already pushed the stick when the source ran...
        assertEquals(-1.0, stickSeenBySource.get(1), 1e-6);
        // ...but the source's value is what the robot got: no driving.
        assertEquals(0.0, runner.hardwareMap().motor("frontLeft").getPower(), 1e-9);
    }
}
