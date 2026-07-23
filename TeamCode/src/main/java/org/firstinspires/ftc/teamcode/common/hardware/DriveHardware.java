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
    private double frontLeftPower;
    private double frontRightPower;
    private double rearLeftPower;
    private double rearRightPower;

    /** Finds and configures the required drive motors. */
    public void initialize(HardwareMap hardwareMap) {
        if (hardwareMap == null) {
            throw new IllegalArgumentException("Drive hardware needs a hardware map.");
        }

        frontLeft = getRequiredMotor(hardwareMap, FRONT_LEFT_MOTOR_NAME);
        frontRight = getRequiredMotor(hardwareMap, FRONT_RIGHT_MOTOR_NAME);
        rearLeft = getRequiredMotor(hardwareMap, REAR_LEFT_MOTOR_NAME);
        rearRight = getRequiredMotor(hardwareMap, REAR_RIGHT_MOTOR_NAME);

        frontLeft.setDirection(DcMotor.Direction.REVERSE);
        rearLeft.setDirection(DcMotor.Direction.REVERSE);
        frontRight.setDirection(DcMotor.Direction.FORWARD);
        rearRight.setDirection(DcMotor.Direction.FORWARD);
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
