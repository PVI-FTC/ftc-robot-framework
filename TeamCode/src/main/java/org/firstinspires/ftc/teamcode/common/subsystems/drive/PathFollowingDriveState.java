package org.firstinspires.ftc.teamcode.common.subsystems.drive;

import org.firstinspires.ftc.teamcode.core.fsm.State;

/** Drive state that gives an optional controller one non-blocking path-following update per loop. */
public class PathFollowingDriveState implements State {
    private final DriveSubsystem driveSubsystem;

    public PathFollowingDriveState(DriveSubsystem driveSubsystem) {
        this.driveSubsystem = driveSubsystem;
    }

    @Override
    public void enter() {
        // The controller begins only after a higher layer has requested a path.
    }

    @Override
    public void update() {
        driveSubsystem.updatePathFollowing();
    }

    @Override
    public void exit() {
        // The next active state controls the drive outputs.
    }

    @Override
    public String getName() {
        return "PathFollowing";
    }
}