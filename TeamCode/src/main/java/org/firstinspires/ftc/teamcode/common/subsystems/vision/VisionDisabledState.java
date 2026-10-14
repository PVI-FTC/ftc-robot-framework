package org.firstinspires.ftc.teamcode.common.subsystems.vision;

import org.firstinspires.ftc.teamcode.core.fsm.State;

/** Safe vision state with no active search or tracking. */
public class VisionDisabledState implements State {
    private final VisionSubsystem visionSubsystem;

    public VisionDisabledState(VisionSubsystem visionSubsystem) {
        this.visionSubsystem = visionSubsystem;
    }

    @Override
    public void enter() {
        // Keep the optional source initialized so a later enable request can use it.
        visionSubsystem.clearVisionObservations();
    }

    @Override
    public void update() {
        // No vision work occurs while disabled.
    }

    @Override
    public void exit() {
        // Searching begins on the next active update.
    }

    @Override
    public String getName() {
        return "VisionDisabled";
    }
}
