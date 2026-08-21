package org.firstinspires.ftc.teamcode.common.subsystems.drive;

import org.firstinspires.ftc.teamcode.core.fsm.State;

/**
 * Driver translation with heading held by the selected drive controller.
 *
 * <p>Controllers with localization capture a target in {@link #enter()} and correct rotation on
 * each update. Simple controllers safely retain their ordinary manual-drive fallback.</p>
 */
public class HeadingHoldState implements State {
    private final DriveSubsystem driveSubsystem;

    public HeadingHoldState(DriveSubsystem driveSubsystem) {
        this.driveSubsystem = driveSubsystem;
    }

    @Override
    public void enter() {
        driveSubsystem.beginHeadingHold();
    }

    @Override
    public void update() {
        driveSubsystem.applyHeadingHoldDrive();
    }

    @Override
    public void exit() {
        // The next active state controls the drive outputs.
    }

    @Override
    public String getName() {
        return "HeadingHold";
    }
}
