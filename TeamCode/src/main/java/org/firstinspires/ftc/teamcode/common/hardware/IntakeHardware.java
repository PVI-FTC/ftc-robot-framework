package org.firstinspires.ftc.teamcode.common.hardware;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.HardwareMap;

/** Hardware access for the optional intake motor. */
public class IntakeHardware {
    public static final String INTAKE_MOTOR_NAME = "intake";

    private DcMotorEx intakeMotor;

    /** Finds the optional intake motor without preventing drivetrain startup when it is absent. */
    public void initialize(HardwareMap hardwareMap) {
        if (hardwareMap == null) {
            throw new IllegalArgumentException("Intake hardware needs a hardware map.");
        }

        try {
            intakeMotor = hardwareMap.get(DcMotorEx.class, INTAKE_MOTOR_NAME);
            intakeMotor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
            intakeMotor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
            stop();
        } catch (IllegalArgumentException exception) {
            intakeMotor = null;
        }
    }

    /** Runs the intake forward at the requested non-negative magnitude. */
    public void forward(double power) {
        setPower(Math.abs(power));
    }

    /** Runs the intake in reverse at the requested non-negative magnitude. */
    public void reverse(double power) {
        setPower(-Math.abs(power));
    }

    /** Stops the intake when it is available. */
    public void stop() {
        if (intakeMotor != null) {
            intakeMotor.setPower(0.0);
        }
    }

    /** Returns whether the optional intake motor was found during initialization. */
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
