package org.firstinspires.ftc.teamcode.robots.team3;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.Servo;

/**
 * Hardware access for Team 3's outtake motor, midtake motor, and intake-deploy servo.
 *
 * <p>All three devices are on the Expansion Hub. Each one is optional so a missing device does
 * not prevent the drivetrain from running; commands to a missing device are ignored.</p>
 */
public class Team3MechanismHardware {
    /** Expansion Hub motor port 1. */
    public static final String OUTTAKE_MOTOR_NAME = "outtake";
    /** Expansion Hub motor port 2. */
    public static final String MIDTAKE_MOTOR_NAME = "midtake";
    /** Expansion Hub servo port 0. */
    public static final String INTAKE_DEPLOY_SERVO_NAME = "intake-deploy";

    private DcMotorEx outtakeMotor;
    private DcMotorEx midtakeMotor;
    private Servo intakeDeployServo;

    /** Finds each optional device without throwing when one is missing from the configuration. */
    public void initialize(HardwareMap hardwareMap) {
        if (hardwareMap == null) {
            throw new IllegalArgumentException("Team 3 mechanism hardware needs a hardware map.");
        }

        outtakeMotor = findMotor(hardwareMap, OUTTAKE_MOTOR_NAME);
        midtakeMotor = findMotor(hardwareMap, MIDTAKE_MOTOR_NAME);

        try {
            intakeDeployServo = hardwareMap.get(Servo.class, INTAKE_DEPLOY_SERVO_NAME);
        } catch (IllegalArgumentException exception) {
            intakeDeployServo = null;
        }

        stop();
    }

    /** Sets the outtake motor power. Positive is the motor's default forward direction. */
    public void setOuttakePower(double power) {
        if (outtakeMotor != null) {
            outtakeMotor.setPower(clampPower(power));
        }
    }

    /** Sets the midtake motor power. Positive is the motor's default forward direction. */
    public void setMidtakePower(double power) {
        if (midtakeMotor != null) {
            midtakeMotor.setPower(clampPower(power));
        }
    }

    /** Moves the intake-deploy servo to a position from 0.0 to 1.0. */
    public void setIntakeDeployPosition(double position) {
        if (intakeDeployServo != null) {
            intakeDeployServo.setPosition(Math.max(0.0, Math.min(1.0, position)));
        }
    }

    /** Stops both motors. The servo holds its last position. */
    public void stop() {
        setOuttakePower(0.0);
        setMidtakePower(0.0);
    }

    public boolean isOuttakeAvailable() {
        return outtakeMotor != null;
    }

    public boolean isMidtakeAvailable() {
        return midtakeMotor != null;
    }

    public boolean isIntakeDeployAvailable() {
        return intakeDeployServo != null;
    }

    private DcMotorEx findMotor(HardwareMap hardwareMap, String name) {
        try {
            DcMotorEx motor = hardwareMap.get(DcMotorEx.class, name);
            motor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
            motor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
            return motor;
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private double clampPower(double power) {
        if (Double.isNaN(power)) {
            return 0.0;
        }
        return Math.max(-1.0, Math.min(1.0, power));
    }
}
