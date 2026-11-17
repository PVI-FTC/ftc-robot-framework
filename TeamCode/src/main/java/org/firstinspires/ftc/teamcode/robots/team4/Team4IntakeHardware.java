package org.firstinspires.ftc.teamcode.robots.team4;

import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.teamcode.common.hardware.IntakeHardware;

/** CR-servo implementation of the shared intake hardware API for Team 4. */
public class Team4IntakeHardware extends IntakeHardware {
    public static final String SERVO_NAME = "intake4Servo";
    public static final double MAX_POWER = 0.50;
    // Reverse this after testing if positive power ejects instead of intaking.
    public static final DcMotorSimple.Direction DIRECTION = DcMotorSimple.Direction.FORWARD;
    private CRServo servo;

    @Override
    public void initialize(HardwareMap hardwareMap) {
        if (hardwareMap == null) throw new IllegalArgumentException("Intake needs a hardware map.");
        servo = hardwareMap.tryGet(CRServo.class, SERVO_NAME);
        if (servo != null) {
            servo.setPower(0.0);
            servo.setDirection(DIRECTION);
        }
    }

    @Override
    public void forward(double power) { setPower(Math.abs(power)); }

    @Override
    public void reverse(double power) { setPower(-Math.abs(power)); }

    @Override
    public void stop() { setPower(0.0); }

    @Override
    public boolean isAvailable() { return servo != null; }

    private void setPower(double power) {
        if (servo != null) {
            servo.setPower(Double.isFinite(power)
                    ? Math.max(-1.0, Math.min(1.0, power)) * MAX_POWER : 0.0);
        }
    }
}
