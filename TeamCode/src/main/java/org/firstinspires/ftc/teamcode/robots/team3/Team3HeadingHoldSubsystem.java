package org.firstinspires.ftc.teamcode.robots.team3;

import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.core.robot.Subsystem;

/**
 * Subsystem managing Pinpoint-based heading lock and correction for Team 3.
 *
 * <p>When active, this subsystem maintains a locked target heading while the driver
 * commands translation (forward/strafe). It applies a proportional correction to rotation
 * to counteract carpet drag, wheel slip, and mechanical drift. If the driver commands manual
 * rotation above the deadband threshold, the driver has full steering authority; when the driver
 * releases the stick, the subsystem allows a brief settling window (150ms) for chassis inertia
 * to subside before locking the new target heading.</p>
 */
public class Team3HeadingHoldSubsystem implements Subsystem {
    /** Proportional gain for heading correction (power per radian of angular error). */
    public static final double KP = 1.0;
    /** Maximum rotational power correction to maintain smooth, stable control without whipping. */
    public static final double MAX_CORRECTION_POWER = 0.45;
    /** Small angular deadband to eliminate motor jitter when pointing straight (approx 0.75 degrees). */
    public static final double ANGLE_DEADBAND_RADIANS = Math.toRadians(0.75);
    /** Threshold on manual rotation input before driver steering overrides heading hold. */
    public static final double MANUAL_STEER_DEADBAND = 0.05;
    /** Milliseconds to wait after driver releases rotation stick before locking new heading. */
    public static final double SETTLE_TIME_MS = 150.0;

    private final Team3PinpointHardware pinpointHardware;
    private final ElapsedTime steeringTimer = new ElapsedTime();

    private boolean active;
    private boolean isSteering;
    private double targetHeadingRadians;

    /**
     * Creates a heading-hold subsystem using the supplied Pinpoint hardware wrapper.
     *
     * @param pinpointHardware hardware wrapper for the Pinpoint computer
     */
    public Team3HeadingHoldSubsystem(Team3PinpointHardware pinpointHardware) {
        if (pinpointHardware == null) {
            throw new IllegalArgumentException("Heading hold subsystem needs pinpoint hardware.");
        }
        this.pinpointHardware = pinpointHardware;
    }

    @Override
    public void initialize() {
        active = false;
        isSteering = false;
        targetHeadingRadians = 0.0;
    }

    @Override
    public void update() {
        pinpointHardware.update();
    }

    @Override
    public void stop() {
        active = false;
        isSteering = false;
    }

    @Override
    public String getName() {
        return "HeadingHold";
    }

    /** Enables or disables heading hold control. */
    public void setActive(boolean active) {
        if (active && !this.active && pinpointHardware.isAvailable()) {
            targetHeadingRadians = pinpointHardware.getHeadingRadians();
            isSteering = false;
        }
        this.active = active;
    }

    /** Returns whether heading hold is currently active. */
    public boolean isActive() {
        return active;
    }

    /** Returns the locked target heading in degrees. */
    public double getTargetHeadingDegrees() {
        return Math.toDegrees(targetHeadingRadians);
    }

    /**
     * Calculates the effective rotation command for the drivetrain.
     *
     * <p>If heading hold is inactive, or Pinpoint is unavailable, the driver's raw rotation
     * is returned unchanged. If heading hold is active and the driver is not actively steering,
     * a proportional heading correction is returned.</p>
     *
     * @param requestedRotate raw rotation input from the driver (-1.0 to 1.0)
     * @return corrected rotation power to supply to the mecanum drive
     */
    public double calculateRotateCommand(double requestedRotate) {
        if (!active || !pinpointHardware.isAvailable()) {
            return requestedRotate;
        }

        // Driver is intentionally steering: pass rotation through directly
        if (Math.abs(requestedRotate) > MANUAL_STEER_DEADBAND) {
            isSteering = true;
            steeringTimer.reset();
            return requestedRotate;
        }

        // Driver just released steering stick: allow chassis momentum to settle before locking
        if (isSteering) {
            if (steeringTimer.milliseconds() < SETTLE_TIME_MS) {
                return 0.0;
            }
            // Settled: lock the new resting heading!
            targetHeadingRadians = pinpointHardware.getHeadingRadians();
            isSteering = false;
        }

        // Heading hold actively maintaining locked heading
        double currentHeading = pinpointHardware.getHeadingRadians();
        double error = normalizeAngle(targetHeadingRadians - currentHeading);

        if (Math.abs(error) < ANGLE_DEADBAND_RADIANS) {
            return 0.0;
        }

        // In FTC/Pinpoint coordinate geometry, CCW is positive.
        // In MecanumDriveController, positive rotate turns the robot Clockwise.
        // When robot drifts CCW (currentHeading > targetHeading), error is negative,
        // so we need positive rotate (CW) to return to target.
        // Therefore, correction = -error * KP = (currentHeading - targetHeading) * KP.
        double correction = -error * KP;
        return Math.max(-MAX_CORRECTION_POWER, Math.min(MAX_CORRECTION_POWER, correction));
    }

    /** Normalizes an angle in radians to the range (-pi, +pi]. */
    private static double normalizeAngle(double radians) {
        while (radians > Math.PI) {
            radians -= 2.0 * Math.PI;
        }
        while (radians < -Math.PI) {
            radians += 2.0 * Math.PI;
        }
        return radians;
    }
}
