package pvi.harness.hardware;

import pvi.harness.runtime.HarnessConfig;
import pvi.harness.runtime.OpModeCatalog;

/**
 * Ready-made fake robot configurations that match what TeamCode looks up.
 *
 * <p>Device names come from {@code harness.properties} (the drive motors, the other motors and
 * the Pinpoint), so a team that renames a device changes it there. For PVI they are the names in
 * TeamCode's {@code DriveHardware} and {@code IntakeHardware}; a PVI extra test checks they still
 * match.</p>
 */
public final class RobotConfigs {
    /** Only static methods here; nothing to create. */
    private RobotConfigs() {
    }

    /** Four mecanum drive motors and nothing else. */
    public static FakeHardwareMap driveOnly() {
        return driveOnly(HarnessConfig.load());
    }

    /** Four mecanum drive motors and nothing else, named as in {@code config}. */
    public static FakeHardwareMap driveOnly(HarnessConfig config) {
        FakeHardwareMap hardwareMap = new FakeHardwareMap();
        for (String name : driveMotorNames(config)) {
            hardwareMap.addMotor(name);
        }
        return hardwareMap.named("driveOnly");
    }

    /**
     * The robot in {@code harness.properties}: the drive, the other motors (PVI: the intake, used
     * by TeamARobot) and a goBILDA Pinpoint, under the names the settings give.
     */
    public static FakeHardwareMap teamA() {
        return robot(HarnessConfig.load());
    }

    /** The robot described by {@code config} (drive, other motors, the {@code devices.*} lists, Pinpoint). */
    public static FakeHardwareMap robot(HarnessConfig config) {
        FakeHardwareMap hardwareMap = driveOnly(config);
        for (String name : config.otherMotors()) {
            hardwareMap.addMotor(name);
        }
        config.servos().forEach(hardwareMap::addServo);
        config.imus().forEach(hardwareMap::addImu);
        config.crServos().forEach(hardwareMap::addCRServo);
        config.distanceSensors().forEach(hardwareMap::addDistanceSensor);
        config.touchSensors().forEach(hardwareMap::addTouchSensor);
        config.colorSensors().forEach(hardwareMap::addColorSensor);
        config.analogInputs().forEach(hardwareMap::addAnalogInput);
        config.otos().forEach(hardwareMap::addOtos);
        config.limelights().forEach(hardwareMap::addLimelight);
        config.webcams().forEach(hardwareMap::addWebcam);
        for (HarnessConfig.DeadWheel pod : config.deadWheels()) {
            hardwareMap.addDeadWheel(pod.parallel()
                    ? FakeDeadWheel.parallel(pod.port(), pod.offsetInches())
                    : FakeDeadWheel.perpendicular(pod.port(), pod.offsetInches()));
        }
        hardwareMap.addPinpoint(config.pinpointName());
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

    /**
     * The fake robot an OpMode found by {@link OpModeCatalog} runs on (manager decision D7): the
     * robot in the settings file, except that with {@code pvi.extras=true} an OpMode in the
     * "Team B" or "Team C" Driver Station group gets {@link #teamB()} or {@link #teamC()}, so
     * PVI's recordings keep saying which team's robot it was.
     */
    public static FakeHardwareMap forOpMode(OpModeCatalog.Entry opMode, HarnessConfig config) {
        if (config.pviExtras()) {
            if ("Team B".equals(opMode.group())) {
                return robot(config).named("teamB");
            }
            if ("Team C".equals(opMode.group())) {
                return robot(config).named("teamC");
            }
        }
        return robot(config);
    }

    /** Names of the four drive motors, for loops in tests. */
    public static String[] driveMotorNames() {
        return driveMotorNames(HarnessConfig.load());
    }

    private static String[] driveMotorNames(HarnessConfig config) {
        HarnessConfig.DriveMotorNames names = config.driveMotors();
        return new String[] {names.frontLeft(), names.frontRight(), names.rearLeft(), names.rearRight()};
    }
}
