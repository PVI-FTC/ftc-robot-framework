package pvi.harness.physics;

import com.pedropathing.ftc.drivetrains.MecanumConstants;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple.Direction;

import org.firstinspires.ftc.teamcode.robots.teamA.TeamAPedroHarnessAccess;

import pvi.harness.hardware.FakeDcMotorEx;
import pvi.harness.hardware.FakeHardwareMap;

/**
 * Reads the four drive motors and works out which way each wheel would really push the robot.
 *
 * <p>For each wheel:</p>
 * <pre>
 *   push = power × sign(direction the code set) × sign(direction that drives this wheel forward)
 *                × (−1 if miswired) × (0 if the motor is disabled)
 * </pre>
 *
 * <p>"The direction that drives this wheel forward" is a fact about the real robot, found by
 * testing it. When the code uses that same direction, the two signs cancel and the wheel
 * pushes the way the power says. When the code uses the other direction (or the motor is
 * miswired), the wheel pushes backward. That is how the estimator can show what a direction
 * or wiring mistake would do on the field.</p>
 */
public final class WheelReader {
    // Configuration names of the four drive motors.
    private final String frontLeftName;
    private final String frontRightName;
    private final String rearLeftName;
    private final String rearRightName;
    // For each wheel: the direction setting that makes it drive the robot forward on
    // positive power, as verified on the real robot.
    private final Direction frontLeftForward;
    private final Direction frontRightForward;
    private final Direction rearLeftForward;
    private final Direction rearRightForward;

    /**
     * @param frontLeftName       configuration name of the front-left drive motor
     * @param frontLeftForward    the direction setting that makes that wheel drive the robot
     *                            forward on positive power (likewise for the others)
     */
    public WheelReader(String frontLeftName, Direction frontLeftForward,
                       String frontRightName, Direction frontRightForward,
                       String rearLeftName, Direction rearLeftForward,
                       String rearRightName, Direction rearRightForward) {
        this.frontLeftName = frontLeftName;
        this.frontRightName = frontRightName;
        this.rearLeftName = rearLeftName;
        this.rearRightName = rearRightName;
        this.frontLeftForward = frontLeftForward;
        this.frontRightForward = frontRightForward;
        this.rearLeftForward = rearLeftForward;
        this.rearRightForward = rearRightForward;
    }

    /**
     * Team A, using the motor names and directions in TeamAPedroConfiguration.
     * IMPLEMENTATION_STATUS.md records those directions (left REVERSE, right FORWARD) as
     * physically verified in the LP-09 raised-wheel checks.
     */
    public static WheelReader teamA() {
        MecanumConstants pedro = TeamAPedroHarnessAccess.recordedMecanumConstants();
        return new WheelReader(
                pedro.leftFrontMotorName, pedro.leftFrontMotorDirection,
                pedro.rightFrontMotorName, pedro.rightFrontMotorDirection,
                pedro.leftRearMotorName, pedro.leftRearMotorDirection,
                pedro.rightRearMotorName, pedro.rightRearMotorDirection);
    }

    /**
     * Names of drive motors this reader needs that the hardware map doesn't have. Empty means
     * the robot can be tracked. The runner uses this to decide whether to track position.
     */
    public java.util.List<String> missingMotors(FakeHardwareMap hardwareMap) {
        java.util.List<String> missing = new java.util.ArrayList<>();
        for (String name : new String[] {frontLeftName, frontRightName, rearLeftName, rearRightName}) {
            if (hardwareMap.motor(name) == null) {
                missing.add(name);
            }
        }
        return missing;
    }

    /** What each wheel is physically doing right now. */
    public WheelPushes read(FakeHardwareMap hardwareMap) {
        return new WheelPushes(
                push(hardwareMap, frontLeftName, frontLeftForward),
                push(hardwareMap, frontRightName, frontRightForward),
                push(hardwareMap, rearLeftName, rearLeftForward),
                push(hardwareMap, rearRightName, rearRightForward));
    }

    /**
     * True if the drive motors brake at zero power (BRAKE, or not chosen yet, which the SDK
     * treats as BRAKE); false if any of them is on FLOAT, so the robot coasts.
     */
    public boolean isBraking(FakeHardwareMap hardwareMap) {
        for (String name : new String[] {frontLeftName, frontRightName, rearLeftName, rearRightName}) {
            FakeDcMotorEx motor = hardwareMap.motor(name);
            if (motor != null && motor.getZeroPowerBehavior() == DcMotor.ZeroPowerBehavior.FLOAT) {
                return false;
            }
        }
        return true;
    }

    /**
     * Turns the drive motors' encoders with what each wheel is really doing ({@code motion}:
     * a fraction of full speed, forward positive, from the physics). The encoder then counts
     * the wheel's real turning, whatever power the code set (a wheel pushed by a wall or slowed
     * by momentum still counts correctly).
     */
    public void turnEncoders(FakeHardwareMap hardwareMap, WheelPushes motion) {
        turnEncoder(hardwareMap, frontLeftName, frontLeftForward, motion.frontLeft());
        turnEncoder(hardwareMap, frontRightName, frontRightForward, motion.frontRight());
        turnEncoder(hardwareMap, rearLeftName, rearLeftForward, motion.rearLeft());
        turnEncoder(hardwareMap, rearRightName, rearRightForward, motion.rearRight());
    }

    /** One wheel: forward motion means turning the way {@code drivesForward} drives forward. */
    private static void turnEncoder(FakeHardwareMap hardwareMap, String name, Direction drivesForward, double motion) {
        FakeDcMotorEx motor = hardwareMap.motor(name);
        if (motor != null) {
            // The shaft's own turning, in the sense a FORWARD motor reports as positive.
            motor.setPhysicalSpeed(motion * sign(drivesForward));
        }
    }

    /** Works out one wheel's push using the formula in the class description. */
    private static double push(FakeHardwareMap hardwareMap, String name, Direction drivesForward) {
        FakeDcMotorEx motor = hardwareMap.motor(name);
        if (motor == null) {
            throw new IllegalStateException("The position estimator needs drive motor \"" + name
                    + "\", but the hardware map has no such motor. Devices: " + hardwareMap.deviceNames());
        }
        // A disabled motor gets no power from the hub, so it does not push.
        if (!motor.isMotorEnabled()) {
            return 0.0;
        }
        // Matching direction signs cancel (+1); mismatched ones flip the push (-1).
        double push = motor.getPower() * sign(motor.getDirection()) * sign(drivesForward);
        // Swapped wires reverse the motor physically, whatever the code set.
        return motor.isMiswired() ? -push : push;
    }

    /** REVERSE counts as -1, FORWARD as +1. */
    private static double sign(Direction direction) {
        return direction == Direction.REVERSE ? -1.0 : 1.0;
    }
}
