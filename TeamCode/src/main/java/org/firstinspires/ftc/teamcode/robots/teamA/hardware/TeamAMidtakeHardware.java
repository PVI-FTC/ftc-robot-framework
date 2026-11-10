package org.firstinspires.ftc.teamcode.robots.teamA.hardware;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.HardwareMap;

/** Hardware access for Team A's optional pollen midtake motor. */
public class TeamAMidtakeHardware {
    public static final String MOTOR_NAME = "midtakeMotor";

    private DcMotor midtakeMotor;

    /** Finds and configures the motor without preventing drivetrain startup when it is absent. */
    public void initialize(HardwareMap hardwareMap) {
        if (hardwareMap == null) {
            throw new IllegalArgumentException("Midtake hardware needs a hardware map.");
        }

        try {
            midtakeMotor = hardwareMap.get(DcMotor.class, MOTOR_NAME);
            midtakeMotor.setDirection(DcMotor.Direction.FORWARD);
            midtakeMotor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
            midtakeMotor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
            stop();
        } catch (IllegalArgumentException exception) {
            midtakeMotor = null;
        }
    }

    /** Runs the midtake forward at full power when it is available. */
    public void run() {
        if (midtakeMotor != null) {
            midtakeMotor.setPower(1.0);
        }
    }

    /** Stops the midtake when it is available. */
    public void stop() {
        if (midtakeMotor != null) {
            midtakeMotor.setPower(0.0);
        }
    }

    public boolean isAvailable() {
        return midtakeMotor != null;
    }
}
