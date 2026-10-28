package pvi.harness;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import pvi.harness.ds.Axis;
import pvi.harness.ds.GamepadScript;
import pvi.harness.hardware.FakeHardwareMap;
import pvi.harness.hardware.RobotConfigs;
import pvi.harness.runtime.HarnessConfig;
import pvi.harness.runtime.MatchPeriod;
import pvi.harness.runtime.OpModeCatalog;
import pvi.harness.runtime.OpModeRunner;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * The first test to run on a new robot (RP10): every OpMode the harness finds in TeamCode gets
 * one quick run on the robot described in {@code harness.properties}.
 *
 * <p>Each run is INIT, half a second of init loops, PLAY, 2 seconds of loops and STOP, with no
 * driver input. It checks only that nothing crashes. If something does, the test fails with a
 * plain line saying what to fix, for example:</p>
 * <pre>
 * missing device "armMotor" (DcMotorEx): add it to harness.properties
 * </pre>
 *
 * <p>An OpMode the settings robot can't run (one written for another robot) can be listed in
 * {@code smokeTest.skipOpModes} in {@code harness.properties}. It then shows as SKIPPED in the
 * report, with that reason, so it is never left out silently.</p>
 *
 * <p>Every TeleOp also gets one more run on a robot with the drive motors from the settings:
 * the left stick is pushed forward for a second after PLAY, and at least one drive motor must get
 * power, or the test says "pushing the left stick forward didn't power any drive motor". That is
 * the one thing every TeleOp is expected to do; it checks nothing else about the OpMode. A
 * TeleOp that isn't meant to drive (a camera or tuning tool, say) can be listed in
 * {@code smokeTest.notDrivingTeleOps}; it then skips only this check, not the crash check.</p>
 *
 * <p>The OpModes come from {@link OpModeCatalog} (every {@code @TeleOp} and {@code @Autonomous}
 * in {@code teamCode.opModePackages}), so a new OpMode is tested without editing this file. Both
 * iterative OpModes and LinearOpModes run. See {@code BRING_YOUR_OWN_ROBOT.md}.</p>
 */
class EveryOpModeSmokeTest {
    /** Seconds of init loops before PLAY. */
    private static final double INIT_SECONDS = 0.5;
    /** Seconds of loops after PLAY. */
    private static final double RUN_SECONDS = 2.0;
    /** Seconds after PLAY when the stick check pushes the left stick forward, and lets go. */
    private static final double STICK_FROM_SECONDS = 0.2;
    private static final double STICK_UNTIL_SECONDS = 1.2;
    /** The SDK's message when hardwareMap.get(...) finds no device: name and type. */
    private static final Pattern MISSING_DEVICE =
            Pattern.compile("Unable to find a hardware device with name \"([^\"]*)\" and type (\\w+)");

    @TestFactory
    Stream<DynamicTest> everyOpModeRunsWithoutCrashing() {
        HarnessConfig config = HarnessConfig.load();
        List<OpModeCatalog.Entry> opModes = OpModeCatalog.fromConfig(config);
        return opModes.stream().map(entry -> DynamicTest.dynamicTest(
                entry.name() + " (" + entry.period() + ")", () -> runOnce(entry, config)));
    }

    @TestFactory
    Stream<DynamicTest> everyTeleOpDrivesOnTheLeftStick() {
        HarnessConfig config = HarnessConfig.load();
        return OpModeCatalog.fromConfig(config).stream()
                .filter(entry -> entry.period() == MatchPeriod.TELEOP)
                .map(entry -> DynamicTest.dynamicTest(entry.name() + " drives on the left stick",
                        () -> drivesOnTheLeftStick(entry, config)));
    }

    /**
     * Like {@link #runOnce}, with the left stick pushed forward for a second after PLAY; fails if
     * no drive motor got any power meanwhile.
     */
    private static void drivesOnTheLeftStick(OpModeCatalog.Entry entry, HarnessConfig config) {
        if (config.smokeTestSkipOpModes().contains(entry.name())) {
            Assumptions.abort("skipped: listed in smokeTest.skipOpModes in harness.properties");
        }
        if (config.smokeTestNotDrivingTeleOps().contains(entry.name())) {
            Assumptions.abort("skipped: listed as not driving (smokeTest.notDrivingTeleOps in harness.properties)");
        }
        FakeHardwareMap robot = RobotConfigs.forOpMode(entry, config);
        HarnessConfig.DriveMotorNames names = config.driveMotors();
        List<String> drive = List.of(names.frontLeft(), names.frontRight(), names.rearLeft(), names.rearRight());
        if (drive.stream().anyMatch(name -> robot.motor(name) == null)) {
            Assumptions.abort("skipped: the robot in harness.properties has no drive motors " + drive);
        }
        GamepadScript driver = new GamepadScript()
                .at(STICK_FROM_SECONDS).stick(Axis.LEFT_STICK_Y, -1.0)   // forward is -1 on a gamepad
                .at(STICK_UNTIL_SECONDS).stick(Axis.LEFT_STICK_Y, 0.0);
        OpModeRunner runner = new OpModeRunner(entry.newOpMode(), robot).gamepad1(driver);
        // The most drive power seen while the stick was pushed (plus one loop for the code to react).
        double[] most = {0.0};
        runner.everyLoop(r -> {
            double sincePlay = r.clock().hasStarted() ? r.clock().sinceStart() : -1.0;
            if (sincePlay >= STICK_FROM_SECONDS && sincePlay <= STICK_UNTIL_SECONDS + r.loopPeriodSeconds()) {
                for (String name : drive) {
                    most[0] = Math.max(most[0], Math.abs(r.hardwareMap().motor(name).getPower()));
                }
            }
        });
        try {
            runner.init();
            while (runner.clock().sinceInit() < INIT_SECONDS - 1e-9) {
                runner.initLoop(1);
            }
            runner.start();
            runner.loopFor(RUN_SECONDS);
            runner.stop();
        } catch (RuntimeException | LinkageError | StackOverflowError problem) {
            // everyOpModeRunsWithoutCrashing reports the crash itself.
            Assumptions.abort("skipped: it crashed (" + explain(problem) + ")");
        }
        if (most[0] == 0.0) {
            fail(entry.name() + " (" + entry.simpleName() + "): pushing the left stick forward for "
                    + (STICK_UNTIL_SECONDS - STICK_FROM_SECONDS) + " s after PLAY didn't power any drive motor "
                    + drive + ". Check the drive code, the gamepad mapping, or the drive.* names in harness.properties.");
        }
    }

    /** INIT, a few init loops, PLAY, 2 s, STOP; fails with a plain line if anything crashes. */
    private static void runOnce(OpModeCatalog.Entry entry, HarnessConfig config) {
        if (config.smokeTestSkipOpModes().contains(entry.name())) {
            Assumptions.abort("skipped: listed in smokeTest.skipOpModes in harness.properties");
        }
        FakeHardwareMap robot = RobotConfigs.forOpMode(entry, config);
        OpModeRunner runner = new OpModeRunner(entry.newOpMode(), robot);
        String stage = "INIT";
        try {
            runner.init();
            stage = "init_loop";
            while (runner.clock().sinceInit() < INIT_SECONDS - 1e-9) {
                runner.initLoop(1);
            }
            stage = "PLAY (start)";
            runner.start();
            stage = "loop";
            runner.loopFor(RUN_SECONDS);
            stage = "STOP";
            runner.stop();
        } catch (RuntimeException | LinkageError | StackOverflowError problem) {
            fail(entry.name() + " (" + entry.simpleName() + ") crashed during " + stage + ": "
                    + explain(problem), problem);
        }
    }

    /** One plain line for the most common problems; the full error is attached below it. */
    static String explain(Throwable problem) {
        for (Throwable cause = problem; cause != null; cause = cause.getCause()) {
            Matcher missing = MISSING_DEVICE.matcher(String.valueOf(cause.getMessage()));
            if (missing.find()) {
                return "missing device \"" + missing.group(1) + "\" (" + missing.group(2)
                        + "): add it to harness.properties (see BRING_YOUR_OWN_ROBOT.md)";
            }
        }
        return problem.getClass().getSimpleName() + ": " + problem.getMessage();
    }
}
