package pvi.harness;

import org.junit.jupiter.api.Test;

import pvi.harness.ds.Axis;
import pvi.harness.ds.GamepadScript;
import pvi.harness.hardware.FakeHardwareMap;
import pvi.harness.hardware.RobotConfigs;
import pvi.harness.physics.Pose;
import pvi.harness.runtime.HarnessConfig;
import pvi.harness.runtime.OpModeCatalog;
import pvi.harness.runtime.OpModeRunner;
import pvi.harness.runtime.RunResult;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * STARTER TEMPLATE: copy this file to write your first test of your own robot
 * (see {@code BRING_YOUR_OWN_ROBOT.md}).
 *
 * <p>It works in any team's repository: it finds your TeleOp by the name it has on the Driver
 * Station, so there is no TeamCode class to import. Change {@link #MY_TELEOP} to your TeleOp's
 * name, run it, then add your own checks where it says TODO.</p>
 *
 * <p>Every test has the same four steps: <b>script</b> (what the drivers do), <b>set up</b>
 * (which OpMode, on which robot), <b>run</b>, <b>check</b>.</p>
 */
class MyFirstRobotTest {
    /** CHANGE THIS: your TeleOp's name, exactly as in its @TeleOp(name = "...") line. */
    private static final String MY_TELEOP = "Team A Mecanum Drive";

    @Test
    void pushingTheLeftStickForwardDrivesTheRobotForward() {
        // 1. SCRIPT: one second after PLAY, the driver pushes the left stick all the way forward
        //    (on a gamepad, forward is -1), and lets go at 2 seconds.
        GamepadScript driver = new GamepadScript()
                .at(1.0).stick(Axis.LEFT_STICK_Y, -1.0)
                .at(2.0).stick(Axis.LEFT_STICK_Y, 0.0);

        // 2. SET UP: your TeleOp, on the robot described in harness.properties.
        HarnessConfig config = HarnessConfig.load();
        OpModeCatalog.Entry teleOp = OpModeCatalog.fromConfig(config).stream()
                .filter(entry -> entry.name().equals(MY_TELEOP))
                .findFirst()
                .orElse(null);
        assertNotNull(teleOp, "No OpMode named \"" + MY_TELEOP + "\". Change MY_TELEOP at the top of this file "
                + "to your TeleOp's name, as on the Driver Station.");
        FakeHardwareMap robot = RobotConfigs.forOpMode(teleOp, config);
        OpModeRunner runner = new OpModeRunner(teleOp.newOpMode(), robot).gamepad1(driver);
        assertNotNull(runner.positionEstimator(), "The harness can't track this robot: " + runner.positionReport());
        Pose start = runner.positionEstimator().pose();

        // 3. RUN: INIT, 1 s of waiting, PLAY, 3 s of driving time, STOP.
        RunResult result = runner.run(1.0, 3.0);

        // 4. CHECK: it didn't crash, and the robot ended up ahead of where it started.
        result.assertNoException();
        Pose end = runner.positionEstimator().pose();
        double forwardInches = (end.xInches() - start.xInches()) * Math.cos(start.headingRadians())
                + (end.yInches() - start.yInches()) * Math.sin(start.headingRadians());
        assertTrue(forwardInches > 6.0, "The robot should drive forward, but moved " + forwardInches
                + " in. If it drove backwards, a motor direction may be wrong. " + runner.positionReport());

        // TODO: add your own checks here, for example:
        //   assertEquals("Ready", runner.telemetry().lastValue("Status"));
        //   assertEquals(0.0, runner.hardwareMap().motor("intake").getPower(), 1e-9);
    }
}
