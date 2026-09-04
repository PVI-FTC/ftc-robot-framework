package org.firstinspires.ftc.teamcode.opmodes.teleop;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.teamcode.core.input.InputManager;
import org.firstinspires.ftc.teamcode.core.util.RumbleManager;
import org.firstinspires.ftc.teamcode.robots.teamA.TeamARobot;

/**
 * Template TeleOp demonstrating RumbleManager integration.
 *
 * <p>
 * This OpMode shows the three required steps to add gamepad rumble to any
 * TeleOp, plus
 * a clearly marked section where teammates can add custom one-liner conditions.
 * </p>
 *
 * <p>
 * Copy this pattern into any TeleOp; the three match-timer alerts are
 * registered
 * automatically by the RumbleManager constructor.
 * </p>
 */
@TeleOp(name = "Rumble Demo (Template)", group = "Testing")
public class RumbleDemoTeleOp extends OpMode {

    private TeamARobot robot;
    private InputManager driverInput;
    private InputManager operatorInput;

    // --- Step 1: Declare a RumbleManager field ---
    // One manager per gamepad that should rumble. Here we rumble gamepad1 (driver).
    private RumbleManager rumble;

    @Override
    public void init() {
        robot = new TeamARobot();
        robot.initialize(hardwareMap);
        driverInput = new InputManager(gamepad1);
        operatorInput = new InputManager(gamepad2);

        // --- Step 2: Construct RumbleManager in init() ---
        // Pass the gamepad that should receive rumble feedback.
        // The three match-timer events (90 s, 105 s, 115 s) are registered
        // automatically.
        rumble = new RumbleManager(gamepad1);

        // --- Add custom events here (fires once per false->true transition) ---
        // IMPORTANT: Always use InputManager to read buttons, not gamepad directly.
        // Use robot state methods (like getIntakeStateName()) for mechanism conditions.
        // rumble.addEvent(() -> operatorInput.wasAJustPressed(), () ->
        // rumble.rumbleNow(150));
        // rumble.addEvent(() -> robot.getIntakeStateName().equals("Holding"), () ->
        // gamepad1.rumbleBlips(1));
        // rumble.addEvent(() -> robot.getVisionStateName().equals("Tracking"), () ->
        // gamepad1.rumbleBlips(2));

        telemetry.addData("Status", "Rumble demo initialized");
        telemetry.update();
    }

    @Override
    public void start() {
        driverInput.update();
        operatorInput.update();
        robot.enableManualDrive();

        // --- Step 3: Reset the timer in start() ---
        // This ensures the 90 s / 105 s / 115 s match-timer events count from
        // the moment the Driver Station presses PLAY, not from init().
        rumble.resetTimer();

        // Rumble for 1 second when the match starts so the driver knows PLAY was pressed.
        // 1000 = 1000 milliseconds = 1 second.
        rumble.rumbleNow(1000);
    }

    @Override
    public void loop() {
        driverInput.update();
        operatorInput.update();

        // Drive mapping (same as TeamATeleOp)
        robot.drive(
                -driverInput.getLeftStickY(),
                driverInput.getLeftStickX(),
                driverInput.getRightStickX());

        if (operatorInput.wasAJustPressed()) {
            robot.startIntake();
        } else if (operatorInput.wasBJustPressed()) {
            robot.stopIntake();
        } else if (operatorInput.wasXJustPressed()) {
            robot.ejectIntake();
        } else if (operatorInput.wasYJustPressed()) {
            robot.holdIntake();
        }

        robot.update();

        // --- Step 4: Call rumble.update() once per loop ---
        // This checks all registered conditions and fires any rising-edge events.
        // No allocation occurs here; the arrays were set up in init().
        rumble.update();

        publishTelemetry();
    }

    @Override
    public void stop() {
        if (robot != null) {
            robot.stop();
        }
    }

    private void publishTelemetry() {
        telemetry.addData("Drive State", robot.getDriveStateName());
        telemetry.addData("Intake State", robot.getIntakeStateName());
        telemetry.addData("Vision State", robot.getVisionStateName());
        telemetry.update();
    }
}
