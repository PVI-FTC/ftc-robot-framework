package org.firstinspires.ftc.teamcode.robots.teamA;

import com.pedropathing.ftc.drivetrains.MecanumConstants;

/**
 * HARNESS FILE, not robot code. It lives in Harness/, but uses TeamCode's package name.
 *
 * <p>{@link TeamAPedroConfiguration} only shares its Pedro constants with classes in this
 * package. Placing this small read-only bridge in the same package lets the harness read
 * Team A's tuned values without changing TeamCode. It is never deployed to the robot.</p>
 *
 * <p>Because the harness reads the values instead of copying them, re-tuning the robot
 * automatically updates the harness.</p>
 */
public final class TeamAPedroHarnessAccess {
    /** Only static methods here; nothing to create. */
    private TeamAPedroHarnessAccess() {
    }

    /** The mecanum constants (speeds, motor names, motor directions) in Team A's recorded configuration. */
    public static MecanumConstants recordedMecanumConstants() {
        return TeamAPedroConfiguration.recordedTeamAConfiguration().getMecanumConstants();
    }
}
