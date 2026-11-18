package org.firstinspires.ftc.teamcode.opmodes.teleop;

import com.qualcomm.robotcore.hardware.Gamepad;

/** TeamA-only edge detection for the MQ2 D-pad controls. */
final class TeamADpadInputManager {
    private final Gamepad gamepad;
    private boolean previousUp;
    private boolean up;
    private boolean previousDown;
    private boolean down;

    TeamADpadInputManager(Gamepad gamepad) {
        if (gamepad == null) {
            throw new IllegalArgumentException("Team A D-pad input needs a gamepad.");
        }
        this.gamepad = gamepad;
        up = gamepad.dpad_up;
        previousUp = up;
        down = gamepad.dpad_down;
        previousDown = down;
    }

    void update() {
        previousUp = up;
        up = gamepad.dpad_up;
        previousDown = down;
        down = gamepad.dpad_down;
    }

    boolean wasUpJustPressed() {
        return up && !previousUp;
    }

    boolean wasDownJustPressed() {
        return down && !previousDown;
    }
}
