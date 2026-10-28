package org.firstinspires.ftc.teamcode.robots.team3;

/**
 * Operating levels for Team 3's real-time closed-loop drive assistance.
 */
public enum Team3DriveAssistLevel {
    /** Raw manual mecanum drive; no closed-loop odometry correction. */
    OFF("OFF"),
    /** Corrects rotation only to maintain locked target heading. */
    HEADING("HEADING"),
    /** Resists all uncommanded translation and rotation (full 3-DOF pose lock). */
    FULL("FULL");

    private final String displayName;

    Team3DriveAssistLevel(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** Returns the next assist level in the cycle: OFF -> HEADING -> FULL -> OFF. */
    public Team3DriveAssistLevel next() {
        switch (this) {
            case OFF:
                return HEADING;
            case HEADING:
                return FULL;
            case FULL:
            default:
                return OFF;
        }
    }

    /** Toggles between HEADING (direction holding only) and FULL (direction + XY hold). */
    public Team3DriveAssistLevel togglePoseLock() {
        return (this == FULL) ? HEADING : FULL;
    }
}
