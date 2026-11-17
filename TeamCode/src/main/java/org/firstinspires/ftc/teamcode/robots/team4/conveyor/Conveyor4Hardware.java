package org.firstinspires.ftc.teamcode.robots.team4.conveyor;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.PwmControl;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.hardware.ServoImplEx;

/** Sole hardware owner for Team 4's conveyor motor and Actuonix L16-R lift. */
public class Conveyor4Hardware {
    public static final String MOTOR_NAME = "conveyor4Motor";
    public static final String LIFT_NAME = "conveyor4Lift";
    // Confirm physical belt direction on the robot before normal use.
    public static final DcMotor.Direction MOTOR_DIRECTION = DcMotor.Direction.FORWARD;
    private DcMotorEx motor;
    private ServoImplEx lift;

    /** Missing conveyor devices are reported individually and do not disable driving. */
    public void initialize(HardwareMap hardwareMap) {
        motor = hardwareMap.tryGet(DcMotorEx.class, MOTOR_NAME);
        if (motor != null) {
            motor.setPower(0.0);
            motor.setDirection(MOTOR_DIRECTION);
            motor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
            motor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        }
        lift = hardwareMap.tryGet(ServoImplEx.class, LIFT_NAME);
        if (lift != null) {
            lift.setPwmDisable();
            lift.setDirection(Servo.Direction.FORWARD);
            lift.setPwmRange(new PwmControl.PwmRange(1000, 2000));
        }
    }

    public boolean isMotorAvailable() { return motor != null; }
    public boolean isLiftAvailable() { return lift != null; }

    public void setBeltPower(double power) {
        if (motor != null) {
            motor.setPower(Double.isFinite(power) ? Math.max(-1.0, Math.min(1.0, power)) : 0.0);
        }
    }

    public void setLiftTarget(double target) {
        if (lift != null && Double.isFinite(target)) {
            lift.setPosition(Math.max(0.0, Math.min(1.0, target)));
            lift.setPwmEnable();
        }
    }

    /** Signal removal is not a guaranteed mechanical stop with external servo power. */
    public void disableLiftPwm() {
        if (lift != null) lift.setPwmDisable();
    }

    public void stop() {
        setBeltPower(0.0);
        disableLiftPwm();
    }
}
