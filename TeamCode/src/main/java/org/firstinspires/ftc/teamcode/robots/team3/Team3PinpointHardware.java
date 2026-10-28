package org.firstinspires.ftc.teamcode.robots.team3;

import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.robotcore.external.navigation.Pose2D;

/**
 * Hardware wrapper for Team 3's goBILDA Pinpoint Odometry Computer.
 *
 * <p>Wraps device initialization, configuration, updates, and heading queries.
 * Pinpoint is optional so if it is not plugged in or missing from the hardware map,
 * the drivetrain continues to operate safely without errors.</p>
 */
public class Team3PinpointHardware {
    public static final String PINPOINT_HARDWARE_MAP_NAME = "pinpoint";

    /** Pod offsets measured on Team 3's robot chassis. */
    public static final double FORWARD_POD_Y_INCHES = -8.0;
    public static final double STRAFE_POD_X_INCHES = -5.75;

    private GoBildaPinpointDriver pinpoint;
    private double xInches;
    private double yInches;
    private double headingRadians;
    private double headingDegrees;

    /**
     * Initializes the Pinpoint device if present in the hardware map.
     *
     * @param hardwareMap the FTC hardware map supplied by the OpMode
     */
    public void initialize(HardwareMap hardwareMap) {
        if (hardwareMap == null) {
            throw new IllegalArgumentException("Team 3 pinpoint hardware needs a hardware map.");
        }

        try {
            pinpoint = hardwareMap.get(GoBildaPinpointDriver.class, PINPOINT_HARDWARE_MAP_NAME);
        } catch (Exception exception) {
            pinpoint = null;
        }

        if (pinpoint != null) {
            try {
                pinpoint.setOffsets(FORWARD_POD_Y_INCHES, STRAFE_POD_X_INCHES, DistanceUnit.INCH);
                pinpoint.setEncoderResolution(GoBildaPinpointDriver.GoBildaOdometryPods.goBILDA_SWINGARM_POD);
                pinpoint.setEncoderDirections(GoBildaPinpointDriver.EncoderDirection.REVERSED,
                        GoBildaPinpointDriver.EncoderDirection.FORWARD);
                pinpoint.resetPosAndIMU();
                update();
            } catch (Exception exception) {
                // Safe fallback if device communication encounters an issue
            }
        }

        xInches = 0.0;
        yInches = 0.0;
        headingRadians = 0.0;
        headingDegrees = 0.0;
    }

    /**
     * Reads the latest data from the Pinpoint computer.
     * Call once per loop from the robot/subsystem update.
     */
    public void update() {
        if (pinpoint == null) {
            return;
        }

        try {
            pinpoint.update();
            Pose2D pose = pinpoint.getPosition();
            if (pose != null) {
                xInches = pose.getX(DistanceUnit.INCH);
                yInches = pose.getY(DistanceUnit.INCH);
                headingRadians = pose.getHeading(AngleUnit.RADIANS);
                headingDegrees = Math.toDegrees(headingRadians);
            }
        } catch (Exception exception) {
            // Protect control loop from transient I2C bus errors
        }
    }

    /** Returns whether the Pinpoint computer was found and initialized. */
    public boolean isAvailable() {
        return pinpoint != null;
    }

    /** Returns current X position in inches. */
    public double getXInches() {
        return xInches;
    }

    /** Returns current Y position in inches. */
    public double getYInches() {
        return yInches;
    }

    /** Returns current heading in radians (-pi to +pi). */
    public double getHeadingRadians() {
        return headingRadians;
    }

    /** Returns current heading in degrees (-180 to +180). */
    public double getHeadingDegrees() {
        return headingDegrees;
    }

    /** Zeroes the heading and position without recalibrating the IMU. */
    public void resetHeading() {
        if (pinpoint != null) {
            try {
                pinpoint.setPosition(new Pose2D(DistanceUnit.INCH, 0, 0, AngleUnit.DEGREES, 0));
                xInches = 0.0;
                yInches = 0.0;
                headingRadians = 0.0;
                headingDegrees = 0.0;
            } catch (Exception exception) {
                // Ignore transient errors
            }
        }
    }
}
