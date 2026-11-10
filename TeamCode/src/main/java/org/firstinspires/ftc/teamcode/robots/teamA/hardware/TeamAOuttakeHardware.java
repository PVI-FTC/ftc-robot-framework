package org.firstinspires.ftc.teamcode.robots.teamA.hardware;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;

/** Hardware access for one optional Team A flywheel outtake motor. */
public class TeamAOuttakeHardware {
    private static final double FULL_SPEED_RPM = 6000.0;
    private static final double IDLE_SPEED_PERCENT = 0.40;
    private static final double LAUNCH_SPEED_PERCENT = 0.80;
    private static final double DEGREES_PER_REVOLUTION = 360.0;
    private static final double SECONDS_PER_MINUTE = 60.0;

    private final String motorName;
    private DcMotorEx outtakeMotor;

    public TeamAOuttakeHardware(String motorName) {
        if (motorName == null || motorName.isEmpty()) {
            throw new IllegalArgumentException("Outtake hardware needs a motor name.");
        }
        this.motorName = motorName;
    }

    /** Finds and configures the motor without preventing drivetrain startup when it is absent. */
    public void initialize(HardwareMap hardwareMap) {
        if (hardwareMap == null) {
            throw new IllegalArgumentException("Outtake hardware needs a hardware map.");
        }

        try {
            outtakeMotor = hardwareMap.get(DcMotorEx.class, motorName);
            outtakeMotor.setDirection(DcMotor.Direction.FORWARD);
            outtakeMotor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);
            outtakeMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            stop();
        } catch (IllegalArgumentException exception) {
            outtakeMotor = null;
        }
    }

    /** Holds the flywheel at 40 percent of its 6,000 RPM rated speed. */
    public void runAtIdleVelocity() {
        setVelocityPercent(IDLE_SPEED_PERCENT);
    }

    /** Holds the flywheel at 80 percent of its 6,000 RPM rated speed. */
    public void runAtLaunchVelocity() {
        setVelocityPercent(LAUNCH_SPEED_PERCENT);
    }

    /** Stops the flywheel when it is available. */
    public void stop() {
        if (outtakeMotor != null) {
            outtakeMotor.setVelocity(0.0, AngleUnit.DEGREES);
        }
    }

    public boolean isAvailable() {
        return outtakeMotor != null;
    }

    private void setVelocityPercent(double speedPercent) {
        if (outtakeMotor != null) {
            double degreesPerSecond = FULL_SPEED_RPM * speedPercent
                    * DEGREES_PER_REVOLUTION / SECONDS_PER_MINUTE;
            outtakeMotor.setVelocity(degreesPerSecond, AngleUnit.DEGREES);
        }
    }
}
