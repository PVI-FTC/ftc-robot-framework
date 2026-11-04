package org.firstinspires.ftc.teamcode.harness;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.teamcode.opmodes.teleop.TeamATeleOp;

/**
 * Drives exactly like "Team A Mecanum Drive", and records the drive for the harness.
 *
 * <p>ROBOT CODE, not harness code. Copy it into TeamCode (see
 * {@code Harness/robot-recorder/README.md}). It shows up on the Driver Station as
 * "Record: Team A Mecanum Drive". Pick it instead of the normal TeleOp when you want the drive
 * saved.</p>
 *
 * <p>It runs the real {@link TeamATeleOp} inside it, unchanged: every call is passed straight
 * through, and the recorder only reads the gamepads afterwards. To record a different TeleOp,
 * copy this file and change the three marked lines.</p>
 */
@TeleOp(name = "Record: Team A Mecanum Drive", group = "Harness")   // (1) the Driver Station name
public class RecordTeamATeleOp extends OpMode {
    private final OpMode teleOp = new TeamATeleOp();                // (2) the TeleOp to run
    private final HarnessDriveRecorder recorder =
            new HarnessDriveRecorder("TeamATeleOp", "teamA");        // (3) its class name, the harness robot

    @Override
    public void init() {
        // Hand the real TeleOp the same hardware, telemetry and gamepads the SDK gave us.
        teleOp.hardwareMap = hardwareMap;
        teleOp.telemetry = telemetry;
        teleOp.gamepad1 = gamepad1;
        teleOp.gamepad2 = gamepad2;
        teleOp.resetRuntime();
        teleOp.init();
        recorder.init(gamepad1, gamepad2);
    }

    @Override
    public void init_loop() {
        teleOp.init_loop();
        recorder.initLoop(gamepad1, gamepad2);
    }

    @Override
    public void start() {
        teleOp.resetRuntime();
        teleOp.start();
        recorder.start(gamepad1, gamepad2);
    }

    @Override
    public void loop() {
        teleOp.loop();
        recorder.loop(gamepad1, gamepad2);
    }

    @Override
    public void stop() {
        teleOp.stop();
        String where = recorder.stop(gamepad1, gamepad2);
        telemetry.addData("Harness recording", where);
        telemetry.update();
    }
}
