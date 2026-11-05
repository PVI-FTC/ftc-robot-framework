package org.firstinspires.ftc.teamcode.KimCode.ClaudeKimCode;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.util.ElapsedTime;

/**
 * Controls the launcher wheel and the motor that feeds balls into it.
 *
 * <p>Call {@link #update()} once per loop. Firing happens in small timed steps, so it never makes
 * the loop wait. If either motor is missing from the robot configuration, this class does nothing
 * instead of failing, so the robot can still finish its autonomous and park.</p>
 *
 * <p>The feed may end up being the intake or a second motor. To switch, change FEED_MOTOR_NAME,
 * FEED_DIRECTION, and FEED_POWER; nothing else needs to change.</p>
 */
final class KimLauncher {
    private enum FireStep { IDLE, SPINNING_UP, FEEDING, GAP }

    /** From TeleopV1's commented-out code. Check these against the robot configuration. */
    private static final String LAUNCHER_MOTOR_NAME = "pollen";
    private static final String FEED_MOTOR_NAME = "intake";
    /** TeleopV1 ran both of these motors with negative power. */
    private static final DcMotorSimple.Direction LAUNCHER_DIRECTION =
            DcMotorSimple.Direction.REVERSE;
    private static final DcMotorSimple.Direction FEED_DIRECTION = DcMotorSimple.Direction.REVERSE;

    /** PLACEHOLDER values below; tune them with HiveLauncherTest. */
    private static final double FEED_POWER = 0.8;
    private static final double FEED_SECONDS = 0.35;
    private static final double GAP_SECONDS = 0.60;
    private static final double SPEED_TOLERANCE_TICKS_PER_SECOND = 40.0;
    /** A shot still goes after this long, so a slow wheel can't use up all of AUTO. */
    private static final double SPIN_UP_TIME_LIMIT_SECONDS = 2.0;

    private final DcMotorEx launcherMotor;
    private final DcMotorEx feedMotor;
    private final ElapsedTime stepTimer = new ElapsedTime();
    private FireStep step = FireStep.IDLE;
    private double targetSpeed;
    private int shotsLeft;

    KimLauncher(HardwareMap hardwareMap) {
        launcherMotor = hardwareMap.tryGet(DcMotorEx.class, LAUNCHER_MOTOR_NAME);
        feedMotor = hardwareMap.tryGet(DcMotorEx.class, FEED_MOTOR_NAME);
        if (isAvailable()) {
            launcherMotor.setDirection(LAUNCHER_DIRECTION);
            launcherMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            launcherMotor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);
            feedMotor.setDirection(FEED_DIRECTION);
        }
    }

    boolean isAvailable() {
        return launcherMotor != null && feedMotor != null;
    }

    /** Starts the wheel or changes its speed. It keeps running until {@link #stop()}. */
    void spinUpTo(double ticksPerSecond) {
        targetSpeed = ticksPerSecond;
        if (isAvailable()) {
            launcherMotor.setVelocity(ticksPerSecond);
        }
    }

    /** Starts firing, with the wheel already set by {@link #spinUpTo(double)}. */
    void fire(int shots) {
        if (isAvailable() && shots > 0) {
            shotsLeft = shots;
            enter(FireStep.SPINNING_UP);
        }
    }

    /** Moves firing along by one small step. Call once per loop. */
    void update() {
        switch (step) {
            case SPINNING_UP:
                if (isAtSpeed() || stepTimer.seconds() >= SPIN_UP_TIME_LIMIT_SECONDS) {
                    startFeeding();
                }
                break;
            case FEEDING:
                if (stepTimer.seconds() >= FEED_SECONDS) {
                    finishShot();
                }
                break;
            case GAP:
                // The wheel slows down after a shot, so give it time to recover.
                if (stepTimer.seconds() >= GAP_SECONDS && (isAtSpeed()
                        || stepTimer.seconds() >= GAP_SECONDS + SPIN_UP_TIME_LIMIT_SECONDS)) {
                    startFeeding();
                }
                break;
            case IDLE:
            default:
                break;
        }
    }

    /** True when no shots are left. Also true if the launcher is missing. */
    boolean isDoneFiring() {
        return step == FireStep.IDLE;
    }

    boolean isAtSpeed() {
        return isAvailable() && Math.abs(actualSpeed() - targetSpeed)
                <= SPEED_TOLERANCE_TICKS_PER_SECOND;
    }

    double targetSpeed() {
        return targetSpeed;
    }

    double actualSpeed() {
        return isAvailable() ? launcherMotor.getVelocity() : 0.0;
    }

    int shotsLeft() {
        return shotsLeft;
    }

    String stepName() {
        return step.name();
    }

    /** Stops both motors and forgets any shots that were left. */
    void stop() {
        shotsLeft = 0;
        targetSpeed = 0.0;
        step = FireStep.IDLE;
        if (isAvailable()) {
            feedMotor.setPower(0.0);
            launcherMotor.setPower(0.0);
        }
    }

    private void startFeeding() {
        feedMotor.setPower(FEED_POWER);
        enter(FireStep.FEEDING);
    }

    private void finishShot() {
        feedMotor.setPower(0.0);
        shotsLeft--;
        enter(shotsLeft > 0 ? FireStep.GAP : FireStep.IDLE);
    }

    private void enter(FireStep newStep) {
        step = newStep;
        stepTimer.reset();
    }
}
