package pvi.harness;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;

import org.firstinspires.ftc.teamcode.opmodes.teleop.TeamATeleOp;
import org.junit.jupiter.api.Test;

import pvi.harness.hardware.FakeDcMotorEx;
import pvi.harness.hardware.FakeHardwareMap;
import pvi.harness.runtime.OpModeRunner;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 1 feasibility check: can the unmodified Team A TeleOp run on a desktop JVM
 * against fake hardware?
 *
 * <p>These were the first tests written. They change gamepad fields directly instead of
 * using a {@code GamepadScript}, which is the quickest way to poke one input.</p>
 */
class HarnessSmokeTest {
    /** Configuration names of the four drive motors, as used in DriveHardware. */
    private static final String[] DRIVE_MOTORS = {"frontLeft", "frontRight", "rearLeft", "rearRight"};

    /**
     * Builds the fake hardware for Team A by hand (later tests use {@code RobotConfigs.teamA()}).
     *
     * @param withIntake false to leave the intake motor out, for missing-hardware tests
     */
    private static FakeHardwareMap teamAHardware(boolean withIntake) {
        FakeHardwareMap hardwareMap = new FakeHardwareMap();
        for (String name : DRIVE_MOTORS) {
            hardwareMap.addMotor(name);
        }
        if (withIntake) {
            hardwareMap.addMotor("intake");
        }
        return hardwareMap;
    }

    /**
     * WHAT: Pressing INIT sets up the drive motors correctly.
     *
     * <p>HOW: Runs only {@code init()}, then reads back what TeamCode did to each fake motor:
     * the init telemetry message, motor directions, brake mode, and starting power.</p>
     *
     * <p>WHY: Wrong motor directions are a classic bug: the robot drives in circles or
     * backwards. This catches that without putting a robot on the floor. Left motors are
     * REVERSE because they are mounted mirror-image to the right ones.</p>
     */
    @Test
    void teleOpInitializesAgainstFakeHardware() {
        // SET UP: real TeamATeleOp, fake Team A hardware.
        OpModeRunner runner = new OpModeRunner(new TeamATeleOp(), teamAHardware(true));

        // RUN: press INIT only.
        runner.init();

        // CHECK: the Driver Station message...
        assertEquals("Team A robot initialized", runner.telemetry().lastValue("Status"));
        // ...the motor directions DriveHardware sets...
        FakeHardwareMap hw = runner.hardwareMap();
        assertEquals(DcMotorSimple.Direction.REVERSE, hw.motor("frontLeft").getDirection());
        assertEquals(DcMotorSimple.Direction.REVERSE, hw.motor("rearLeft").getDirection());
        assertEquals(DcMotorSimple.Direction.FORWARD, hw.motor("frontRight").getDirection());
        assertEquals(DcMotorSimple.Direction.FORWARD, hw.motor("rearRight").getDirection());
        // ...and every drive motor brakes and starts stopped.
        for (String name : DRIVE_MOTORS) {
            assertEquals(DcMotor.ZeroPowerBehavior.BRAKE, hw.motor(name).getZeroPowerBehavior(), name);
            assertEquals(0.0, hw.motor(name).getPower(), name);
        }
    }

    /**
     * WHAT: Pushing the driver's left stick forward makes all four wheels drive forward.
     *
     * <p>HOW: Sets gamepad 1's left stick Y to -1 (FTC sticks read negative when pushed
     * <b>up</b>), runs 3 loops, and checks every drive motor has strong positive power.</p>
     *
     * <p>WHY: Proves the whole TeleOp chain works end to end: gamepad, InputManager, the
     * OpMode's mapping, TeamARobot, the drive state machine, the mecanum math, and
     * DriveHardware. Also checks the drive is in ManualDrive after PLAY.</p>
     */
    @Test
    void leftStickForwardDrivesAllWheelsForward() {
        // SET UP + RUN: INIT and PLAY.
        OpModeRunner runner = new OpModeRunner(new TeamATeleOp(), teamAHardware(true));
        runner.init().start();

        // SCRIPT (quick form): push the stick, then let a few loops run.
        runner.gamepad1().left_stick_y = -1.0f; // pushing the stick up reads as negative Y
        runner.loop(3);

        // CHECK: every wheel drives forward, and the drive reports manual mode.
        for (String name : DRIVE_MOTORS) {
            FakeDcMotorEx motor = runner.hardwareMap().motor(name);
            assertTrue(motor.getPower() > 0.5, "expected forward power on " + motor);
        }
        assertEquals("ManualDrive", runner.telemetry().lastValue("Drive State"));
    }

    /**
     * WHAT: A single tap of the operator's A button starts the intake.
     *
     * <p>HOW: Presses A on gamepad 2 for one loop, releases it, runs two more loops, then
     * checks the intake state and motor power.</p>
     *
     * <p>WHY: The intake lives on gamepad 2 (the operator). It must start on the press and
     * stay running after the button is released, because InputManager reacts to
     * "just pressed", not "held".</p>
     */
    @Test
    void operatorAButtonStartsIntake() {
        // SET UP + RUN: INIT, PLAY, one loop so InputManager has a "before" state.
        OpModeRunner runner = new OpModeRunner(new TeamATeleOp(), teamAHardware(true));
        runner.init().start().loop(1);

        // SCRIPT (quick form): press A for one loop, then let go.
        runner.gamepad2().a = true;
        runner.loop(1);
        runner.gamepad2().a = false;
        runner.loop(2);

        // CHECK: the intake is still running after A was released.
        assertEquals("Intaking", runner.telemetry().lastValue("Intake State"));
        assertTrue(runner.hardwareMap().motor("intake").getPower() > 0, "intake should be running");
    }

    /**
     * WHAT: Pressing STOP leaves every motor at zero power.
     *
     * <p>HOW: Gets the drive and intake both running, presses STOP, then checks all five
     * motors.</p>
     *
     * <p>WHY: Safety. When the Driver Station presses STOP, nothing may keep moving. This is
     * the most important behavior on the robot to never break.</p>
     */
    @Test
    void stopLeavesEveryMotorAtZero() {
        // SET UP + RUN: drive forward with the intake on.
        OpModeRunner runner = new OpModeRunner(new TeamATeleOp(), teamAHardware(true));
        runner.init().start();
        runner.gamepad1().left_stick_y = -1.0f;
        runner.gamepad2().a = true;
        runner.loop(3);

        // RUN: press STOP.
        runner.stop();

        // CHECK: every motor, drive and intake, is at zero.
        for (String name : new String[] {"frontLeft", "frontRight", "rearLeft", "rearRight", "intake"}) {
            assertEquals(0.0, runner.hardwareMap().motor(name).getPower(), name);
        }
    }

    /**
     * WHAT: A robot with no intake motor configured can still drive.
     *
     * <p>HOW: Builds hardware without "intake", checks INIT and PLAY don't crash, then
     * drives forward and checks telemetry says the intake is unavailable.</p>
     *
     * <p>WHY: ARCHITECTURE.md says the intake is optional and must fail safely. If an
     * unplugged intake stopped the drivetrain, a match could be lost to one loose cable.</p>
     */
    @Test
    void missingIntakeDoesNotBreakDriving() {
        // SET UP: no intake motor in the configuration.
        OpModeRunner runner = new OpModeRunner(new TeamATeleOp(), teamAHardware(false));

        // RUN + CHECK: INIT and PLAY must not throw.
        assertDoesNotThrow(() -> runner.init().start());
        runner.gamepad1().left_stick_y = -1.0f;
        runner.loop(3);

        // CHECK: drivers are told, and driving still works.
        assertEquals("false", runner.telemetry().lastValue("Intake Available"));
        assertTrue(runner.hardwareMap().motor("frontLeft").getPower() > 0.5);
    }

    /**
     * WHAT: A missing drive motor stops INIT with an error that names the motor.
     *
     * <p>HOW: Builds hardware without "frontLeft", runs INIT, and expects an
     * IllegalStateException whose message contains "frontLeft".</p>
     *
     * <p>WHY: Drive motors are required. Failing loudly at INIT, with the exact name, tells
     * the team which configuration entry or cable to check, instead of the robot
     * mysteriously driving crooked.</p>
     */
    @Test
    void missingDriveMotorFailsWithClearMessage() {
        // SET UP: only three of the four drive motors.
        FakeHardwareMap hardwareMap = new FakeHardwareMap();
        hardwareMap.addMotor("frontRight");
        hardwareMap.addMotor("rearLeft");
        hardwareMap.addMotor("rearRight");
        OpModeRunner runner = new OpModeRunner(new TeamATeleOp(), hardwareMap);

        // RUN + CHECK: INIT must throw this exact kind of error...
        IllegalStateException error = assertThrows(IllegalStateException.class, runner::init);

        // ...and the message must say which motor is missing.
        assertNotNull(error.getMessage());
        assertTrue(error.getMessage().contains("frontLeft"), error.getMessage());
    }
}
