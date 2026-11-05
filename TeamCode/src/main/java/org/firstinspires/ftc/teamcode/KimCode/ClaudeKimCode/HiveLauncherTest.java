package org.firstinspires.ftc.teamcode.KimCode.ClaudeKimCode;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.teamcode.core.input.InputManager;

/**
 * Tests the launcher and measures the speeds for {@link HiveShotTable}. The robot does not drive.
 *
 * <p>Put the robot a measured distance from the CELL and find the speed that lands shots. Write
 * down the distance and speed, then repeat from other distances.</p>
 */
@TeleOp(name = "Hive Launcher Test", group = "KimCode Testing")
public final class HiveLauncherTest extends OpMode {
    private static final double STARTING_SPEED_TICKS_PER_SECOND = 1200.0;
    private static final double SPEED_STEP_TICKS_PER_SECOND = 50.0;
    private static final int ALL_PRELOADS = 4;

    private KimLauncher launcher;
    private InputManager input;
    private double chosenSpeed = STARTING_SPEED_TICKS_PER_SECOND;
    private boolean wheelOn;

    @Override
    public void init() {
        launcher = new KimLauncher(hardwareMap);
        input = new InputManager(gamepad1);
    }

    @Override
    public void loop() {
        input.update();
        mapControls();
        launcher.update();

        telemetry.addData("Launcher found", launcher.isAvailable());
        telemetry.addData("Controls", "A wheel on, B stop, X 1 shot, Y 4 shots");
        telemetry.addData("Change speed", "Right bumper up, left bumper down");
        telemetry.addData("Chosen speed", "%.0f ticks/s", chosenSpeed);
        telemetry.addData("Actual speed", "%.0f ticks/s", launcher.actualSpeed());
        telemetry.addData("At speed", launcher.isAtSpeed());
        telemetry.addData("Firing", "%s, %d shots left", launcher.stepName(),
                launcher.shotsLeft());
        telemetry.update();
    }

    @Override
    public void stop() {
        if (launcher != null) {
            launcher.stop();
        }
    }

    private void mapControls() {
        if (input.wasRightBumperJustPressed()) {
            changeSpeed(SPEED_STEP_TICKS_PER_SECOND);
        } else if (input.wasLeftBumperJustPressed()) {
            changeSpeed(-SPEED_STEP_TICKS_PER_SECOND);
        }

        if (input.wasAJustPressed()) {
            wheelOn = true;
            launcher.spinUpTo(chosenSpeed);
        }
        if (input.wasBJustPressed()) {
            wheelOn = false;
            launcher.stop();
        }
        // A shot needs the wheel running, and starting another mid-shot would lose count.
        if (wheelOn && launcher.isDoneFiring()) {
            if (input.wasXJustPressed()) {
                launcher.fire(1);
            } else if (input.wasYJustPressed()) {
                launcher.fire(ALL_PRELOADS);
            }
        }
    }

    private void changeSpeed(double changeTicksPerSecond) {
        chosenSpeed = Math.max(0.0, chosenSpeed + changeTicksPerSecond);
        if (wheelOn) {
            launcher.spinUpTo(chosenSpeed);
        }
    }
}
