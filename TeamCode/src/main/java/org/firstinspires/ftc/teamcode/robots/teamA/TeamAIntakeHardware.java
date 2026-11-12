package org.firstinspires.ftc.teamcode.robots.teamA;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.teamcode.common.hardware.IntakeHardware;

/** Team A's intake wrapper using the requested basic {@link DcMotor} interface. */
final class TeamAIntakeHardware extends IntakeHardware {
    private static final String INTAKE_MOTOR_NAME = "intakeMotor";

    private DcMotor intakeMotor;

    TeamAIntakeHardware() {
        super(INTAKE_MOTOR_NAME, DcMotor.Direction.FORWARD);
    }

    @Override
    public void initialize(HardwareMap hardwareMap) {
        if (hardwareMap == null) {
            throw new IllegalArgumentException("Team A intake hardware needs a hardware map.");
        }

        try {
            intakeMotor = hardwareMap.get(DcMotor.class, INTAKE_MOTOR_NAME);
            intakeMotor.setDirection(DcMotor.Direction.FORWARD);
            intakeMotor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
            intakeMotor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
            stop();
        } catch (IllegalArgumentException exception) {
            intakeMotor = null;
        }
    }

    @Override
    public void forward(double power) {
        setPower(Math.abs(power));
    }

    @Override
    public void reverse(double power) {
        setPower(-Math.abs(power));
    }

    @Override
    public void stop() {
        if (intakeMotor != null) {
            intakeMotor.setPower(0.0);
        }
    }

    @Override
    public boolean isAvailable() {
        return intakeMotor != null;
    }

    private void setPower(double power) {
        if (intakeMotor != null) {
            intakeMotor.setPower(clampPower(power));
        }
    }

    private double clampPower(double power) {
        if (Double.isNaN(power)) {
            return 0.0;
        }
        return Math.max(-1.0, Math.min(1.0, power));
    }
}
