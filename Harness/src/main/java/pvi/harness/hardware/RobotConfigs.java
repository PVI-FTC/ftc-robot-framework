package pvi.harness.hardware;

import org.firstinspires.ftc.teamcode.common.hardware.DriveHardware;
import org.firstinspires.ftc.teamcode.common.hardware.IntakeHardware;
import org.firstinspires.ftc.teamcode.robots.teamA.TeamAPedroHarnessAccess;

/**
 * Ready-made fake robot configurations that match what TeamCode looks up.
 *
 * <p>Device names come from the constants in TeamCode's hardware wrappers, so if a team
 * renames a device there, these configurations follow automatically.</p>
 */
public final class RobotConfigs {
    /** Only static methods here; nothing to create. */
    private RobotConfigs() {
    }

    /** Four mecanum drive motors and nothing else. */
    public static FakeHardwareMap driveOnly() {
        FakeHardwareMap hardwareMap = new FakeHardwareMap();
        hardwareMap.addMotor(DriveHardware.FRONT_LEFT_MOTOR_NAME);
        hardwareMap.addMotor(DriveHardware.FRONT_RIGHT_MOTOR_NAME);
        hardwareMap.addMotor(DriveHardware.REAR_LEFT_MOTOR_NAME);
        hardwareMap.addMotor(DriveHardware.REAR_RIGHT_MOTOR_NAME);
        return hardwareMap.named("driveOnly");
    }

    /**
     * The shared baseline robot: drive plus intake (used by TeamARobot), plus the goBILDA
     * Pinpoint that Team A's Pedro configuration uses, under the name that configuration gives.
     */
    public static FakeHardwareMap teamA() {
        FakeHardwareMap hardwareMap = driveOnly();
        hardwareMap.addMotor(IntakeHardware.INTAKE_MOTOR_NAME);
        hardwareMap.addPinpoint(TeamAPedroHarnessAccess.recordedPinpointConstants().hardwareMapName);
        return hardwareMap.named("teamA");
    }

    /** TeamBRobot currently uses the same shared RobotHardware as Team A. */
    public static FakeHardwareMap teamB() {
        return teamA().named("teamB");
    }

    /** TeamCRobot currently uses the same shared RobotHardware as Team A. */
    public static FakeHardwareMap teamC() {
        return teamA().named("teamC");
    }

    /** Names of the four drive motors, for loops in tests. */
    public static String[] driveMotorNames() {
        return new String[] {
                DriveHardware.FRONT_LEFT_MOTOR_NAME,
                DriveHardware.FRONT_RIGHT_MOTOR_NAME,
                DriveHardware.REAR_LEFT_MOTOR_NAME,
                DriveHardware.REAR_RIGHT_MOTOR_NAME,
        };
    }
}
