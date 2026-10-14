package org.firstinspires.ftc.teamcode.common.hardware;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.HardwareMap;

/**
 * Hardware access for the four required drivetrain motors.
 *
 * <p>All four configured motors are required. Missing hardware causes initialization to fail
 * with the configured name in the error message. This wrapper does not decide how the robot
 * should drive; it only applies motor commands requested by a higher layer.</p>
 */
public class DriveHardware {
    public static final String FRONT_LEFT_MOTOR_NAME = "frontLeft";
    public static final String FRONT_RIGHT_MOTOR_NAME = "frontRight";
    public static final String REAR_LEFT_MOTOR_NAME = "rearLeft";
    public static final String REAR_RIGHT_MOTOR_NAME = "rearRight";

    private DcMotorEx frontLeft;
    private DcMotorEx frontRight;
    private DcMotorEx rearLeft;
    private DcMotorEx rearRight;
    private final DcMotor.Direction frontLeftDirection;
    private final DcMotor.Direction frontRightDirection;
    private final DcMotor.Direction rearLeftDirection;
    private final DcMotor.Direction rearRightDirection;
    private double frontLeftPower;
    private double frontRightPower;
    private double rearLeftPower;
    private double rearRightPower;

    /** Creates the shared baseline drivetrain direction configuration. */
    public DriveHardware() {
        this(DcMotor.Direction.REVERSE, DcMotor.Direction.FORWARD,
                DcMotor.Direction.REVERSE, DcMotor.Direction.FORWARD);
    }

    /** Creates drivetrain hardware with the supplied motor directions. */
    public DriveHardware(DcMotor.Direction frontLeftDirection,
            DcMotor.Direction frontRightDirection, DcMotor.Direction rearLeftDirection,
            DcMotor.Direction rearRightDirection) {
        if (frontLeftDirection == null || frontRightDirection == null
                || rearLeftDirection == null || rearRightDirection == null) {
            throw new IllegalArgumentException("Drive motor directions are required.");
        }

        this.frontLeftDirection = frontLeftDirection;
        this.frontRightDirection = frontRightDirection;
        this.rearLeftDirection = rearLeftDirection;
        this.rearRightDirection = rearRightDirection;
    }

    /** Finds and configures the required drive motors. */
    public void initialize(HardwareMap hardwareMap) {
        if (hardwareMap == null) {
            throw new IllegalArgumentException("Drive hardware needs a hardware map.");
        }

        frontLeft = getRequiredMotor(hardwareMap, FRONT_LEFT_MOTOR_NAME);
        frontRight = getRequiredMotor(hardwareMap, FRONT_RIGHT_MOTOR_NAME);
        rearLeft = getRequiredMotor(hardwareMap, REAR_LEFT_MOTOR_NAME);
        rearRight = getRequiredMotor(hardwareMap, REAR_RIGHT_MOTOR_NAME);

        frontLeft.setDirection(frontLeftDirection);
        frontRight.setDirection(frontRightDirection);
        rearLeft.setDirection(rearLeftDirection);
        rearRight.setDirection(rearRightDirection);
        setBrakeMode();
        setRunWithoutEncoderMode();
        stop();
    }

    /** Sets motor powers after clamping each requested value to the FTC-safe range. */
    public void setMotorPowers(double frontLeftPower, double frontRightPower,
            double rearLeftPower, double rearRightPower) {
        requireInitialized();

        this.frontLeftPower = clampPower(frontLeftPower);
        this.frontRightPower = clampPower(frontRightPower);
        this.rearLeftPower = clampPower(rearLeftPower);
        this.rearRightPower = clampPower(rearRightPower);

        frontLeft.setPower(this.frontLeftPower);
        frontRight.setPower(this.frontRightPower);
        rearLeft.setPower(this.rearLeftPower);
        rearRight.setPower(this.rearRightPower);
    }

    /** Stops all drivetrain motors. */
    public void stop() {
        if (frontLeft == null || frontRight == null || rearLeft == null || rearRight == null) {
            return;
        }

        setMotorPowers(0.0, 0.0, 0.0, 0.0);
    }

    /** Configures all drive motors to hold position when no power is requested. */
    public void setBrakeMode() {
        requireInitialized();
        setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
    }

    /** Configures all drive motors to coast when no power is requested. */
    public void setFloatMode() {
        requireInitialized();
        setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);
    }

    public double getFrontLeftPower() {
        return frontLeftPower;
    }

    public double getFrontRightPower() {
        return frontRightPower;
    }

    public double getRearLeftPower() {
        return rearLeftPower;
    }

    public double getRearRightPower() {
        return rearRightPower;
    }

    private DcMotorEx getRequiredMotor(HardwareMap hardwareMap, String configuredName) {
        try {
            return hardwareMap.get(DcMotorEx.class, configuredName);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "Required drive motor is missing or misconfigured: " + configuredName,
                    exception);
        }
    }

    private void setRunWithoutEncoderMode() {
        frontLeft.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        frontRight.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        rearLeft.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        rearRight.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
    }

    private void setZeroPowerBehavior(DcMotor.ZeroPowerBehavior behavior) {
        frontLeft.setZeroPowerBehavior(behavior);
        frontRight.setZeroPowerBehavior(behavior);
        rearLeft.setZeroPowerBehavior(behavior);
        rearRight.setZeroPowerBehavior(behavior);
    }

    private void requireInitialized() {
        if (frontLeft == null || frontRight == null || rearLeft == null || rearRight == null) {
            throw new IllegalStateException("Initialize drive hardware before using it.");
        }
    }

    private double clampPower(double power) {
        if (Double.isNaN(power)) {
            return 0.0;
        }
        return Math.max(-1.0, Math.min(1.0, power));
    }
}
