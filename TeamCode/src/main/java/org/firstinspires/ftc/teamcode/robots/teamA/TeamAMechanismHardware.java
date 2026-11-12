package org.firstinspires.ftc.teamcode.robots.teamA;

import android.graphics.Color;

import com.qualcomm.hardware.rev.RevColorSensorV3;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.NormalizedRGBA;
import com.qualcomm.robotcore.hardware.SwitchableLight;

import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;

/** Hardware access for Team A's midtake, pollen, and nectar mechanisms. */
final class TeamAMechanismHardware {
    static final float COLOR_SENSOR_GAIN = 2.0f;
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
        setMidtakeVelocity(0.0);
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
        configureColorSensor(intakeSensor);
        configureColorSensor(pollenSensor);
    }

    /** Leaves every Team A mechanism output at zero power. */
    void stop() {
        setMidtakeVelocity(0.0);
        stopOuttakes();
        if (nectarServo != null) {
            nectarServo.setPower(0.0);
        }
    }

    void setPollenVelocity(double velocityTicksPerSecond) {
        setVelocity(pollenMotor, velocityTicksPerSecond);
    }

    void setMidtakeVelocity(double velocityTicksPerSecond) {
        setVelocity(midtakeMotor, velocityTicksPerSecond);
    }

    void setNectarVelocity(double velocityTicksPerSecond) {
        setVelocity(nectarMotor, velocityTicksPerSecond);
    }

    void stopOuttakes() {
        setPollenVelocity(0.0);
        setNectarVelocity(0.0);
    }

    void setNectarServoPower(double power) {
        if (nectarServo == null) {
            return;
        }
        double safePower = Double.isNaN(power) ? 0.0 : Math.max(-1.0, Math.min(1.0, power));
        nectarServo.setPower(safePower);
    }

    ColorReading readIntakeSensor() {
        return readColor(intakeSensor);
    }

    ColorReading readPollenSensor() {
        return readColor(pollenSensor);
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

    private void configureColorSensor(RevColorSensorV3 sensor) {
        sensor.setGain(COLOR_SENSOR_GAIN);
        if (sensor instanceof SwitchableLight) {
            ((SwitchableLight) sensor).enableLight(true);
        }
    }

    private ColorReading readColor(RevColorSensorV3 sensor) {
        if (sensor == null) {
            return ColorReading.unavailable();
        }

        NormalizedRGBA colors = sensor.getNormalizedColors();
        float[] hsv = new float[3];
        Color.colorToHSV(colors.toColor(), hsv);
        return new ColorReading(true, hsv[0], hsv[1], hsv[2],
                sensor.getDistance(DistanceUnit.CM));
    }

    /** One neutral HSV and proximity sample from a Team A color sensor. */
    static final class ColorReading {
        private final boolean available;
        private final float hueDegrees;
        private final float saturation;
        private final float value;
        private final double distanceCm;

        private ColorReading(
                boolean available,
                float hueDegrees,
                float saturation,
                float value,
                double distanceCm) {
            this.available = available;
            this.hueDegrees = hueDegrees;
            this.saturation = saturation;
            this.value = value;
            this.distanceCm = distanceCm;
        }

        static ColorReading unavailable() {
            return new ColorReading(false, Float.NaN, Float.NaN, Float.NaN, Double.NaN);
        }

        boolean isAvailable() {
            return available;
        }

        float getHueDegrees() {
            return hueDegrees;
        }

        float getSaturation() {
            return saturation;
        }

        float getValue() {
            return value;
        }

        double getDistanceCm() {
            return distanceCm;
        }
    }
}
