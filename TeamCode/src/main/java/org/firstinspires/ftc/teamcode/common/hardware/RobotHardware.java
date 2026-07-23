package org.firstinspires.ftc.teamcode.common.hardware;

import com.qualcomm.robotcore.hardware.HardwareMap;

/**
 * Composes the shared hardware wrappers for one robot.
 *
 * <p>Drivetrain initialization occurs first because it is required. Intake and vision are
 * optional, so their unavailable states do not prevent drivetrain use.</p>
 */
public class RobotHardware {
    private final DriveHardware driveHardware = new DriveHardware();
    private final IntakeHardware intakeHardware = new IntakeHardware();
    private final VisionHardware visionHardware = new VisionHardware();

    /** Initializes shared hardware in drivetrain, intake, then vision order. */
    public void initialize(HardwareMap hardwareMap) {
        driveHardware.initialize(hardwareMap);
        intakeHardware.initialize(hardwareMap);
        visionHardware.initialize();
    }

    public DriveHardware getDriveHardware() {
        return driveHardware;
    }

    public IntakeHardware getIntakeHardware() {
        return intakeHardware;
    }

    public VisionHardware getVisionHardware() {
        return visionHardware;
    }

    /** Stops every hardware wrapper and leaves motor outputs safe. */
    public void stopAll() {
        driveHardware.stop();
        intakeHardware.stop();
        visionHardware.stop();
    }
}
