package pvi.harness.physics;

import com.qualcomm.robotcore.hardware.DcMotorSimple.Direction;

import org.firstinspires.ftc.teamcode.opmodes.teleop.TeamATeleOp;
import org.junit.jupiter.api.Test;

import pvi.harness.hardware.FakeHardwareMap;
import pvi.harness.hardware.RobotConfigs;
import pvi.harness.runtime.OpModeRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Position estimator Step 3: reading which way each wheel really pushes the robot.
 */
class WheelReaderTest {
    /** Allowed rounding error for decimal math. */
    private static final double TOLERANCE = 1e-9;

    /** Team A drive motors, set by hand the way DriveHardware sets them (left REVERSE). */
    private static FakeHardwareMap teamADriveSetByHand(double power) {
        FakeHardwareMap hw = RobotConfigs.driveOnly();
        hw.motor("frontLeft").setDirection(Direction.REVERSE);
        hw.motor("rearLeft").setDirection(Direction.REVERSE);
        hw.motor("frontRight").setDirection(Direction.FORWARD);
        hw.motor("rearRight").setDirection(Direction.FORWARD);
        for (String name : RobotConfigs.driveMotorNames()) {
            hw.motor(name).setPower(power);
        }
        return hw;
    }

    /** Runs TeamATeleOp for a few loops with the given driver stick input. */
    private static OpModeRunner teleOpWithStick(FakeHardwareMap hw, float leftX, float leftY) {
        OpModeRunner runner = new OpModeRunner(new TeamATeleOp(), hw);
        runner.init().start();
        runner.gamepad1().left_stick_x = leftX;
        runner.gamepad1().left_stick_y = leftY;
        runner.loop(3);
        return runner;
    }

    /**
     * WHAT: With the verified directions, positive power pushes every wheel forward.
     *
     * <p>HOW: Sets the motors by hand exactly as DriveHardware does (left REVERSE, right
     * FORWARD), all at +0.5, and reads the pushes.</p>
     *
     * <p>WHY: This is the robot as the team verified it in LP-09. The reader must agree with
     * that real-world result, or every later estimate is wrong.</p>
     */
    @Test
    void verifiedDirectionsMakeEveryWheelPushForward() {
        WheelPushes pushes = WheelReader.teamA().read(teamADriveSetByHand(0.5));

        assertEquals(new WheelPushes(0.5, 0.5, 0.5, 0.5), pushes);
    }

    /**
     * WHAT: Team A's real TeleOp, stick pushed forward, makes every wheel push forward.
     *
     * <p>HOW: Runs the unmodified TeamATeleOp with the left stick up and reads the pushes.</p>
     *
     * <p>WHY: Earlier tests only checked the <i>power</i> numbers the code sends. This checks
     * what those numbers would make the wheels <i>do</i>, including the direction settings.</p>
     */
    @Test
    void teleOpStickForwardPushesEveryWheelForward() {
        OpModeRunner runner = teleOpWithStick(RobotConfigs.teamA(), 0f, -1f);

        WheelPushes pushes = WheelReader.teamA().read(runner.hardwareMap());

        assertTrue(pushes.allForward(), pushes.toString());
        assertEquals(pushes.frontLeft(), pushes.rearRight(), TOLERANCE, "all wheels equal: " + pushes);
    }

    /**
     * WHAT: Stick to the right gives the mecanum strafe pattern: + − − +.
     *
     * <p>HOW: Runs TeamATeleOp with the left stick fully right. Front-left and rear-right must
     * push forward, front-right and rear-left backward, all equally hard.</p>
     *
     * <p>WHY: That diagonal pattern is what makes a mecanum robot slide sideways. If any sign
     * were wrong, the robot would turn or drift instead of strafing.</p>
     */
    @Test
    void teleOpStickRightGivesTheStrafePattern() {
        OpModeRunner runner = teleOpWithStick(RobotConfigs.teamA(), 1f, 0f);

        WheelPushes pushes = WheelReader.teamA().read(runner.hardwareMap());

        assertEquals(new WheelPushes(1.0, -1.0, -1.0, 1.0), pushes);
    }

    /**
     * WHAT: Forgetting to reverse the left motors makes the left wheels push backward.
     *
     * <p>HOW: Sets all four motors to FORWARD direction (as if someone deleted the REVERSE
     * lines), +0.5 power. The left wheels must push backward, the right ones forward.</p>
     *
     * <p>WHY: On the field that robot would spin in place instead of driving. The code's
     * power numbers look fine, so only a model of the wheels can show the problem.</p>
     */
    @Test
    void forgottenReverseMakesLeftWheelsPushBackward() {
        FakeHardwareMap hw = teamADriveSetByHand(0.5);
        hw.motor("frontLeft").setDirection(Direction.FORWARD);
        hw.motor("rearLeft").setDirection(Direction.FORWARD);

        WheelPushes pushes = WheelReader.teamA().read(hw);

        assertEquals(new WheelPushes(-0.5, 0.5, -0.5, 0.5), pushes);
    }

    /**
     * WHAT: A miswired motor pushes the wrong way, even though the code can't tell.
     *
     * <p>HOW: Swaps frontLeft's wires, runs TeamATeleOp with the stick forward. The code still
     * reads back a positive power for frontLeft, but that wheel pushes backward.</p>
     *
     * <p>WHY: Swapped motor wires are a real build mistake. This shows the estimator can
     * reveal it, which a test of the code alone never could.</p>
     */
    @Test
    void miswiredMotorPushesTheWrongWayWithoutTheCodeKnowing() {
        FakeHardwareMap hw = RobotConfigs.teamA().miswire("frontLeft");
        OpModeRunner runner = teleOpWithStick(hw, 0f, -1f);

        WheelPushes pushes = WheelReader.teamA().read(runner.hardwareMap());

        assertTrue(hw.motor("frontLeft").getPower() > 0, "the code still sees positive power");
        assertTrue(pushes.frontLeft() < 0, "but the wheel pushes backward: " + pushes);
        assertTrue(pushes.frontRight() > 0 && pushes.rearLeft() > 0 && pushes.rearRight() > 0, pushes.toString());
    }

    /**
     * WHAT: A disabled motor doesn't push, whatever power is set.
     *
     * <p>WHY: setMotorDisable() cuts power on the real hub, so the model must match.</p>
     */
    @Test
    void disabledMotorDoesNotPush() {
        FakeHardwareMap hw = teamADriveSetByHand(0.5);
        hw.motor("rearRight").setMotorDisable();

        WheelPushes pushes = WheelReader.teamA().read(hw);

        assertEquals(0.0, pushes.rearRight(), TOLERANCE);
        assertEquals(0.5, pushes.frontLeft(), TOLERANCE);
    }

    /**
     * WHAT: A missing drive motor gives an error naming it.
     *
     * <p>WHY: The estimator needs all four wheels. A clear message points at the cause.</p>
     */
    @Test
    void missingDriveMotorGivesAClearError() {
        FakeHardwareMap hw = RobotConfigs.driveOnly().without("rearLeft");

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> WheelReader.teamA().read(hw));

        assertTrue(error.getMessage().contains("rearLeft"), error.getMessage());
    }

    /**
     * WHAT: The baseline drive code and the Pedro configuration agree on motor directions.
     *
     * <p>HOW: Runs TeamATeleOp's INIT (which uses DriveHardware) and compares each motor's
     * direction with the one in TeamAPedroConfiguration.</p>
     *
     * <p>WHY: Team A records the motor directions in two places. If someone changes one and
     * forgets the other, the robot drives differently in TeleOp and Pedro autonomous. This
     * catches the mismatch, and confirms the reader uses the directions the robot really has.</p>
     */
    @Test
    void baselineDriveCodeAndPedroConfigAgreeOnDirections() {
        OpModeRunner runner = new OpModeRunner(new TeamATeleOp(), RobotConfigs.teamA());
        runner.init();
        runner.hardwareMap().motor("frontLeft").setPower(0.3);
        runner.hardwareMap().motor("frontRight").setPower(0.3);
        runner.hardwareMap().motor("rearLeft").setPower(0.3);
        runner.hardwareMap().motor("rearRight").setPower(0.3);

        // If DriveHardware's directions matched Pedro's, every wheel pushes forward.
        WheelPushes pushes = WheelReader.teamA().read(runner.hardwareMap());

        assertEquals(new WheelPushes(0.3, 0.3, 0.3, 0.3), pushes);
    }
}
