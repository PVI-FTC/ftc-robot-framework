package pvi.harness.hardware;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.HardwareMap;

import pvi.harness.runtime.HarnessConfig;

/**
 * Test helper for sample OpModes and test runs: the drive motors named in
 * {@code harness.properties}, each set to the direction that makes positive power drive its wheel
 * forward. So the samples drive and spin any team's robot, not only Team A's.
 */
public final class ConfigDrive {
    private final DcMotor[] left;
    private final DcMotor[] right;

    public ConfigDrive(HardwareMap hardwareMap) {
        HarnessConfig config = HarnessConfig.load();
        HarnessConfig.DriveMotorNames names = config.driveMotors();
        HarnessConfig.DriveMotorDirections directions = config.driveDirections();
        left = new DcMotor[] {hardwareMap.get(DcMotor.class, names.frontLeft()), hardwareMap.get(DcMotor.class, names.rearLeft())};
        right = new DcMotor[] {hardwareMap.get(DcMotor.class, names.frontRight()), hardwareMap.get(DcMotor.class, names.rearRight())};
        left[0].setDirection(directions.frontLeft());
        left[1].setDirection(directions.rearLeft());
        right[0].setDirection(directions.frontRight());
        right[1].setDirection(directions.rearRight());
    }

    /** Powers the left and right sides (positive = forward). Opposite signs spin the robot. */
    public void power(double leftPower, double rightPower) {
        for (DcMotor motor : left) {
            motor.setPower(leftPower);
        }
        for (DcMotor motor : right) {
            motor.setPower(rightPower);
        }
    }
}
