package org.firstinspires.ftc.teamcode.robots.teamA;

import com.qualcomm.hardware.rev.RevColorSensorV3;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.HardwareMap;

/** Hardware access for Team A's midtake, pollen, and nectar mechanisms. */
final class TeamAMechanismHardware {
    static final String MIDTAKE_MOTOR_NAME = "midtakeMotor";
    static final String POLLEN_MOTOR_NAME = "pollenMotor";
    static final String NECTAR_MOTOR_NAME = "nectarMotor";
    static final String NECTAR_SERVO_NAME = "nectarServo";
    static final String INTAKE_SENSOR_NAME = "intakeSensor";
    static final String POLLEN_SENSOR_NAME = "pollenSensor";

    private DcMotorEx midtakeMotor;
    private DcMotorEx pollenMotor;
    private DcMotorEx nectarMotor;
    private CRServo nectarServo;
    private RevColorSensorV3 intakeSensor;
    private RevColorSensorV3 pollenSensor;

    /** Finds and configures Team A's required mechanism hardware. */
    void initialize(HardwareMap hardwareMap) {
        if (hardwareMap == null) {
            throw new IllegalArgumentException("Team A mechanism hardware needs a hardware map.");
        }

        midtakeMotor = hardwareMap.get(DcMotorEx.class, MIDTAKE_MOTOR_NAME);
        configureEncoderMotor(midtakeMotor);
        midtakeMotor.setPower(0.0);
        pollenMotor = hardwareMap.get(DcMotorEx.class, POLLEN_MOTOR_NAME);
        configureEncoderMotor(pollenMotor);
        setPollenVelocity(0.0);
        nectarMotor = hardwareMap.get(DcMotorEx.class, NECTAR_MOTOR_NAME);
        configureEncoderMotor(nectarMotor);
        setNectarVelocity(0.0);
        nectarServo = hardwareMap.get(CRServo.class, NECTAR_SERVO_NAME);
        nectarServo.setDirection(CRServo.Direction.FORWARD);
        nectarServo.setPower(0.0);
        intakeSensor = hardwareMap.get(RevColorSensorV3.class, INTAKE_SENSOR_NAME);
        pollenSensor = hardwareMap.get(RevColorSensorV3.class, POLLEN_SENSOR_NAME);
    }

    /** Leaves every Team A mechanism output at zero power. */
    void stop() {
        setMotorPowerToZero(midtakeMotor);
        stopOuttakes();
        if (nectarServo != null) {
            nectarServo.setPower(0.0);
        }
    }

    void setPollenVelocity(double velocityTicksPerSecond) {
        setVelocity(pollenMotor, velocityTicksPerSecond);
    }

    void setNectarVelocity(double velocityTicksPerSecond) {
        setVelocity(nectarMotor, velocityTicksPerSecond);
    }

    void stopOuttakes() {
        setPollenVelocity(0.0);
        setNectarVelocity(0.0);
    }

    private void configureEncoderMotor(DcMotorEx motor) {
        motor.setDirection(DcMotor.Direction.FORWARD);
        motor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
    }

    private void setVelocity(DcMotorEx motor, double velocityTicksPerSecond) {
        if (motor == null) {
            return;
        }

        double safeVelocity = Double.isNaN(velocityTicksPerSecond)
                ? 0.0 : Math.max(0.0, velocityTicksPerSecond);
        motor.setVelocity(safeVelocity);
    }

    private void setMotorPowerToZero(DcMotor motor) {
        if (motor != null) {
            motor.setPower(0.0);
        }
    }
}
