package org.firstinspires.ftc.teamcode.robots.team3;

/**
 * Pure mathematical controller providing closed-loop 3-DOF pose stabilization (X, Y, Heading).
 *
 * <p>Key architectural design:
 * <ul>
 *   <li><b>Direction Holding Always On:</b> In both {@link Team3DriveAssistLevel#HEADING} and
 *       {@link Team3DriveAssistLevel#FULL} modes, direction holding is continuously active.
 *       It maintains the locked target heading during driving, strafing, and pushes.
 *       When the driver uses the rotation stick, manual steering takes immediate priority;
 *       upon release, it settles for 150ms before locking the new heading.</li>
 *   <li><b>Toggleable X/Y Position Hold (Y Button):</b> When engaged (FULL mode), the robot also
 *       locks physical 2D field coordinates (X, Y). If shoved, bumped, or dragged 4+ feet,
 *       it aggressively returns 100% of the way to its exact locked pose. When toggled off
 *       (HEADING mode), translation is 100% manual while direction holding remains on.</li>
 *   <li><b>Anti-Rubber-Banding Deceleration Settling:</b> On stick release in FULL mode, a brief
 *       deceleration phase lets chassis inertia come to a stop before capturing the anchor,
 *       completely eliminating overshoot and rubber-banding.</li>
 *   <li><b>True Physical Velocity Damping:</b> Damping is computed directly against the robot's
 *       measured physical velocity (derived from Pinpoint odometry and low-pass filtered),
 *       providing critical damping with zero derivative kick when setpoints change.</li>
 *   <li><b>Continuous Static Friction Ramp:</b> Static friction feedforward ramps linearly across
 *       a boundary zone instead of stepping discontinuously, eliminating limit-cycle chattering.</li>
 *   <li><b>Zero-Crossing Integral Reset:</b> Integral accumulation is restricted to a small
 *       precision zone and resets immediately if error crosses zero, preventing integral windup
 *       overrun.</li>
 * </ul>
 * </p>
 */
public class Team3PoseLock {
    /** Operating mode of the translational controller. */
    public enum TranslationState {
        MANUAL_DRIVING,
        DECELERATING,
        LOCKED
    }

    // --- Control Gains ---
    /** Translational proportional gain (power per inch of error). */
    public static final double KP_TRANSLATIONAL = 0.12;
    /** Translational derivative / damping gain (power per in/s physical velocity). */
    public static final double KD_TRANSLATIONAL = 0.025;
    /** Translational integral gain (power per inch-second of accumulated error). */
    public static final double KI_TRANSLATIONAL = 0.03;
    /** Minimum power kick to overcome wheel scrubbing / static friction on carpet. */
    public static final double KS_TRANSLATIONAL = 0.06;
    /** Maximum translational correction power clamp. */
    public static final double MAX_TRANSLATIONAL_CORRECTION = 0.65;
    /** Maximum power contribution from translational integral term. */
    public static final double MAX_INTEGRAL_TRANSLATIONAL_POWER = 0.10;

    /** Heading proportional gain (power per radian of angular error). */
    public static final double KP_HEADING = 1.2;
    /** Maximum heading correction power clamp. */
    public static final double MAX_HEADING_CORRECTION = 0.45;
    /** Small angular deadband to eliminate motor jitter when pointing straight (~0.5 deg). */
    public static final double HEADING_DEADBAND_RADIANS = Math.toRadians(0.50);
    /** Threshold on manual rotation input before driver steering overrides heading hold. */
    public static final double MANUAL_STEER_DEADBAND = 0.05;
    /** Seconds to wait after driver releases rotation stick before locking new heading. */
    public static final double STEER_SETTLE_SECONDS = 0.150;

    // --- Thresholds & Zones ---
    /** Deadband below which manual translational stick inputs are treated as zero. */
    public static final double STICK_DEADBAND = 0.05;
    /** Deadband below which translational error is treated as zero (no motor hum). */
    public static final double POSITION_DEADBAND_INCHES = 0.10;
    /** Boundary zone across which translational friction feedforward ramps linearly. */
    public static final double POSITION_RAMP_INCHES = 0.50;
    /** Maximum error distance within which translational integral is allowed to accumulate. */
    public static final double INTEGRAL_ZONE_INCHES = 2.0;

    /** Speed threshold in inches/second below which translation is considered stopped. */
    public static final double STOPPED_SPEED_THRESHOLD_IPS = 1.5;
    /** Maximum time in seconds to wait for coasting deceleration before forcing locked anchor. */
    public static final double MAX_DECEL_TIME_SECONDS = 0.18;

    // --- Operating States ---
    private Team3DriveAssistLevel assistLevel = Team3DriveAssistLevel.HEADING;
    private TranslationState translationState = TranslationState.MANUAL_DRIVING;
    private boolean isSteering;
    private double steeringSettleTimerSeconds;

    // --- Locked Anchors ---
    private double anchorX;
    private double anchorY;
    private double anchorHeading;
    private boolean anchorInitialized;

    // --- History for Velocity & Derivation ---
    private double lastRobotX;
    private double lastRobotY;
    private double lastRobotHeading;
    private boolean hasLastPose;

    private double filteredVx;
    private double filteredVy;
    private double filteredVHeading;
    private double robotSpeedIps;

    // --- Timers & Integrals ---
    private double decelTransTimerSeconds;

    private double integralFwd;
    private double integralLeft;

    private double lastErrorRobotFwd;
    private double lastErrorRobotLeft;

    // --- Telemetry Cache ---
    private double telemetryErrorInches;
    private double telemetryHeadingErrorDegrees;

    /**
     * Resets the virtual anchor to match the current robot physical pose and enters LOCKED state.
     *
     * @param robotX current physical X in inches
     * @param robotY current physical Y in inches
     * @param robotHeading current physical heading in radians
     */
    public void resetAnchor(double robotX, double robotY, double robotHeading) {
        anchorX = robotX;
        anchorY = robotY;
        anchorHeading = normalizeAngle(robotHeading);
        anchorInitialized = true;

        lastRobotX = robotX;
        lastRobotY = robotY;
        lastRobotHeading = robotHeading;
        hasLastPose = true;

        filteredVx = 0.0;
        filteredVy = 0.0;
        filteredVHeading = 0.0;
        robotSpeedIps = 0.0;

        translationState = TranslationState.LOCKED;
        isSteering = false;
        steeringSettleTimerSeconds = STEER_SETTLE_SECONDS;
        decelTransTimerSeconds = 0.0;

        integralFwd = 0.0;
        integralLeft = 0.0;

        lastErrorRobotFwd = 0.0;
        lastErrorRobotLeft = 0.0;

        telemetryErrorInches = 0.0;
        telemetryHeadingErrorDegrees = 0.0;
    }

    /** Sets the current drive assist operating level. */
    public void setAssistLevel(Team3DriveAssistLevel level) {
        if (level != null) {
            this.assistLevel = level;
        }
    }

    /** Returns the current drive assist operating level. */
    public Team3DriveAssistLevel getAssistLevel() {
        return assistLevel;
    }

    /** Snaps the anchor heading to a target angle and locks direction holding. */
    public void snapToHeading(double targetHeadingRadians, double currentX, double currentY) {
        anchorHeading = normalizeAngle(targetHeadingRadians);
        if (!anchorInitialized) {
            anchorX = currentX;
            anchorY = currentY;
            anchorInitialized = true;
        }
        isSteering = false;
        steeringSettleTimerSeconds = STEER_SETTLE_SECONDS;
    }

    /** Returns the locked target X in inches for telemetry. */
    public double getTargetXInches() {
        return anchorX;
    }

    /** Returns the locked target Y in inches for telemetry. */
    public double getTargetYInches() {
        return anchorY;
    }

    /** Returns the locked target heading in degrees for telemetry. */
    public double getTargetHeadingDegrees() {
        return Math.toDegrees(anchorHeading);
    }

    /** Returns current translational error in inches for telemetry. */
    public double getErrorInches() {
        return telemetryErrorInches;
    }

    /** Returns current heading error in degrees for telemetry. */
    public double getHeadingErrorDegrees() {
        return telemetryHeadingErrorDegrees;
    }

    /** Returns current physical speed in inches per second for telemetry. */
    public double getRobotSpeedIps() {
        return robotSpeedIps;
    }

    /** Returns active translation state name for telemetry. */
    public String getTranslationStateName() {
        return translationState.name();
    }

    /**
     * Calculates stabilized drivetrain commands (forward, strafe, rotate).
     *
     * @param forwardStick driver's raw forward stick command (-1.0 to 1.0)
     * @param strafeStick driver's raw strafe stick command (-1.0 to 1.0, right positive)
     * @param rotateStick driver's raw rotate stick command (-1.0 to 1.0, clockwise positive)
     * @param robotX current physical robot X in inches from Pinpoint odometry
     * @param robotY current physical robot Y in inches from Pinpoint odometry
     * @param robotHeading current physical robot heading in radians from Pinpoint IMU
     * @param dtSeconds time elapsed since last loop in seconds
     * @return 3-element array: [effectiveForward, effectiveStrafe, effectiveRotate]
     */
    public double[] update(double forwardStick, double strafeStick, double rotateStick,
                           double robotX, double robotY, double robotHeading, double dtSeconds) {
        double[] output = new double[]{forwardStick, strafeStick, rotateStick};

        if (assistLevel == Team3DriveAssistLevel.OFF) {
            resetAnchor(robotX, robotY, robotHeading);
            return output;
        }

        if (!anchorInitialized || !hasLastPose) {
            resetAnchor(robotX, robotY, robotHeading);
            return output;
        }

        // Clamp dt to prevent numerical spikes on loop pauses
        double dt = Math.max(0.001, Math.min(0.100, dtSeconds));

        // 1. Calculate physical robot velocity from Pinpoint odometry
        double rawVx = (robotX - lastRobotX) / dt;
        double rawVy = (robotY - lastRobotY) / dt;
        double rawVHeading = normalizeAngle(robotHeading - lastRobotHeading) / dt;

        lastRobotX = robotX;
        lastRobotY = robotY;
        lastRobotHeading = robotHeading;

        // Low-pass filter velocity (~15 Hz cutoff) to eliminate single-tick quantization jitter
        filteredVx = 0.65 * filteredVx + 0.35 * rawVx;
        filteredVy = 0.65 * filteredVy + 0.35 * rawVy;
        filteredVHeading = 0.65 * filteredVHeading + 0.35 * rawVHeading;
        robotSpeedIps = Math.hypot(filteredVx, filteredVy);

        // Convert field velocity into robot-frame velocity
        double vRobotFwd = filteredVx * Math.cos(robotHeading) + filteredVy * Math.sin(robotHeading);
        double vRobotLeft = -filteredVx * Math.sin(robotHeading) + filteredVy * Math.cos(robotHeading);

        // 2. Identify manual driver intent
        boolean isDriverTranslating = (Math.abs(forwardStick) > STICK_DEADBAND) || (Math.abs(strafeStick) > STICK_DEADBAND);
        boolean isDriverRotating = (Math.abs(rotateStick) > MANUAL_STEER_DEADBAND);

        // =========================================================================
        // 3. TRANSLATION CONTROL (X, Y)
        // =========================================================================
        if (assistLevel == Team3DriveAssistLevel.FULL) {
            // FULL Mode: Holds physical X, Y coordinates as well as heading
            if (isDriverTranslating) {
                // Driver is manually translating: pass stick commands directly
                translationState = TranslationState.MANUAL_DRIVING;
                decelTransTimerSeconds = 0.0;
                integralFwd = 0.0;
                integralLeft = 0.0;
                output[0] = forwardStick;
                output[1] = strafeStick;
            } else {
                // Driver is NOT touching translational sticks
                if (translationState == TranslationState.MANUAL_DRIVING) {
                    // Just released sticks: enter deceleration settling phase
                    translationState = TranslationState.DECELERATING;
                    decelTransTimerSeconds = 0.0;
                    integralFwd = 0.0;
                    integralLeft = 0.0;
                }

                if (translationState == TranslationState.DECELERATING) {
                    decelTransTimerSeconds += dt;
                    // Passive / gentle active motor braking during deceleration
                    double brakeFwd = -0.015 * vRobotFwd;
                    double brakeLeft = -0.015 * vRobotLeft;
                    output[0] = Math.max(-0.20, Math.min(0.20, brakeFwd));
                    output[1] = -Math.max(-0.20, Math.min(0.20, brakeLeft));

                    // Once chassis slows to rest or timeout expires, lock the anchor at the final stopped spot
                    if (robotSpeedIps < STOPPED_SPEED_THRESHOLD_IPS || decelTransTimerSeconds >= MAX_DECEL_TIME_SECONDS) {
                        translationState = TranslationState.LOCKED;
                        anchorX = robotX;
                        anchorY = robotY;
                        integralFwd = 0.0;
                        integralLeft = 0.0;
                        lastErrorRobotFwd = 0.0;
                        lastErrorRobotLeft = 0.0;
                        output[0] = 0.0;
                        output[1] = 0.0;
                    }
                }

                if (translationState == TranslationState.LOCKED) {
                    // In LOCKED state: virtual anchor NEVER moves regardless of how far pushed or dragged!
                    double dx = anchorX - robotX;
                    double dy = anchorY - robotY;
                    telemetryErrorInches = Math.hypot(dx, dy);

                    // Transform field position error into robot frame
                    double errRobotFwd = dx * Math.cos(robotHeading) + dy * Math.sin(robotHeading);
                    double errRobotLeft = -dx * Math.sin(robotHeading) + dy * Math.cos(robotHeading);

                    // Zero-crossing reset: if error crossed zero, clear integral to prevent overshoot
                    if (errRobotFwd * lastErrorRobotFwd < 0.0) {
                        integralFwd = 0.0;
                    }
                    if (errRobotLeft * lastErrorRobotLeft < 0.0) {
                        integralLeft = 0.0;
                    }
                    lastErrorRobotFwd = errRobotFwd;
                    lastErrorRobotLeft = errRobotLeft;

                    // Zone-based integral: only integrate within precision zone to avoid windup
                    if (Math.abs(errRobotFwd) < INTEGRAL_ZONE_INCHES && Math.abs(errRobotFwd) > POSITION_DEADBAND_INCHES) {
                        integralFwd += errRobotFwd * dt;
                        double maxI = MAX_INTEGRAL_TRANSLATIONAL_POWER / KI_TRANSLATIONAL;
                        integralFwd = Math.max(-maxI, Math.min(maxI, integralFwd));
                    } else {
                        integralFwd = 0.0;
                    }

                    if (Math.abs(errRobotLeft) < INTEGRAL_ZONE_INCHES && Math.abs(errRobotLeft) > POSITION_DEADBAND_INCHES) {
                        integralLeft += errRobotLeft * dt;
                        double maxI = MAX_INTEGRAL_TRANSLATIONAL_POWER / KI_TRANSLATIONAL;
                        integralLeft = Math.max(-maxI, Math.min(maxI, integralLeft));
                    } else {
                        integralLeft = 0.0;
                    }

                    // Compute smooth friction compensation (continuous ramp avoids limit-cycle chatter)
                    double sFwd = computeFrictionCompensation(errRobotFwd, POSITION_DEADBAND_INCHES, POSITION_RAMP_INCHES, KS_TRANSLATIONAL);
                    double sLeft = computeFrictionCompensation(errRobotLeft, POSITION_DEADBAND_INCHES, POSITION_RAMP_INCHES, KS_TRANSLATIONAL);

                    // Closed-loop PID with true velocity damping
                    double corrFwd = (KP_TRANSLATIONAL * errRobotFwd)
                            - (KD_TRANSLATIONAL * vRobotFwd)
                            + (KI_TRANSLATIONAL * integralFwd)
                            + sFwd;
                    corrFwd = Math.max(-MAX_TRANSLATIONAL_CORRECTION, Math.min(MAX_TRANSLATIONAL_CORRECTION, corrFwd));

                    double corrLeft = (KP_TRANSLATIONAL * errRobotLeft)
                            - (KD_TRANSLATIONAL * vRobotLeft)
                            + (KI_TRANSLATIONAL * integralLeft)
                            + sLeft;
                    corrLeft = Math.max(-MAX_TRANSLATIONAL_CORRECTION, Math.min(MAX_TRANSLATIONAL_CORRECTION, corrLeft));

                    output[0] = corrFwd;
                    output[1] = -corrLeft; // Left is negative strafe command in standard mecanum
                }
            }
        } else {
            // HEADING Mode: translation is 100% normal manual stick driving
            translationState = TranslationState.MANUAL_DRIVING;
            anchorX = robotX;
            anchorY = robotY;
            telemetryErrorInches = 0.0;
            output[0] = forwardStick;
            output[1] = strafeStick;
        }

        // =========================================================================
        // 4. ROTATION CONTROL (HEADING HOLD) - ALWAYS ACTIVE IN HEADING & FULL MODES
        // =========================================================================
        if (isDriverRotating) {
            // Driver is actively steering: pass rotation stick through directly
            isSteering = true;
            steeringSettleTimerSeconds = 0.0;
            output[2] = rotateStick;
        } else {
            // Driver is NOT touching rotation stick
            if (isSteering) {
                steeringSettleTimerSeconds += dt;
                if (steeringSettleTimerSeconds < STEER_SETTLE_SECONDS) {
                    output[2] = 0.0; // Allow chassis rotational inertia to settle smoothly
                } else {
                    // Settle complete: lock the new resting heading!
                    anchorHeading = normalizeAngle(robotHeading);
                    isSteering = false;
                    output[2] = 0.0;
                }
            } else {
                // Heading hold is actively maintaining the locked target heading
                double dh = normalizeAngle(anchorHeading - robotHeading);
                telemetryHeadingErrorDegrees = Math.toDegrees(dh);

                if (Math.abs(dh) < HEADING_DEADBAND_RADIANS) {
                    output[2] = 0.0;
                } else {
                    // In FTC/Pinpoint coordinate geometry: CCW is positive.
                    // In MecanumDriveController: positive rotate turns Clockwise.
                    // When robot drifts CCW (dh < 0), we need positive rotate (CW) to return to target.
                    // Therefore, correction = -dh * KP_HEADING.
                    double correction = -dh * KP_HEADING;
                    output[2] = Math.max(-MAX_HEADING_CORRECTION, Math.min(MAX_HEADING_CORRECTION, correction));
                }
            }
        }

        return output;
    }

    /**
     * Computes continuous static friction feedforward ramping linearly between deadband and rampZone.
     * Eliminates limit-cycle chattering caused by discontinuous sign functions.
     */
    private double computeFrictionCompensation(double error, double deadband, double rampZone, double ks) {
        double absErr = Math.abs(error);
        if (absErr <= deadband) {
            return 0.0;
        }
        double ramp = Math.min(1.0, (absErr - deadband) / Math.max(0.001, (rampZone - deadband)));
        return Math.copySign(ks * ramp, error);
    }

    /** Normalizes an angle in radians to the range (-pi, +pi]. */
    public static double normalizeAngle(double radians) {
        while (radians > Math.PI) {
            radians -= 2.0 * Math.PI;
        }
        while (radians < -Math.PI) {
            radians += 2.0 * Math.PI;
        }
        return radians;
    }
}
