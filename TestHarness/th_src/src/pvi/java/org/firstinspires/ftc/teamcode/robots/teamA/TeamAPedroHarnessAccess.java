package org.firstinspires.ftc.teamcode.robots.teamA;

import com.pedropathing.algorithm.ForesightConfig;
import com.pedropathing.revhub.drivetrains.MecanumConfig;
import com.pedropathing.revhub.localizers.PinpointConfig;

/**
 * HARNESS FILE, not robot code. It lives in Harness/, but uses TeamCode's package name.
 *
 * <p>{@link TeamAPedroConfiguration} only shares its Pedro constants with classes in this
 * package. Placing this small read-only bridge in the same package lets the harness read
 * Team A's tuned values without changing TeamCode. It is never deployed to the robot.</p>
 *
 * <p>A PVI extra: it is built only with {@code pvi.extras=true} in {@code harness.properties},
 * and the harness core never uses it (the core reads its robot facts from
 * {@code harness.properties}). PVI's extra tests use it to check that those facts still match
 * Team A's Pedro configuration, so a re-tune that isn't copied over makes a test fail.</p>
 *
 * <p>Pedro 3 (2026-10-27): the values live in three config objects, each read with
 * {@code .get()} on its fields (for example {@code foresight().maxAchievableForwardVelocity.get()}).</p>
 */
public final class TeamAPedroHarnessAccess {
    /** Only static methods here; nothing to create. */
    private TeamAPedroHarnessAccess() {
    }

    /** Pedro's path-following settings: top speeds, natural decelerations, controllers. */
    public static ForesightConfig recordedForesightConfig() {
        return TeamAPedroConfiguration.recordedTeamAConfiguration().getForesightConfig();
    }

    /** The mecanum drive: motor names and the direction that drives each wheel forward. */
    public static MecanumConfig recordedMecanumConfig() {
        return TeamAPedroConfiguration.recordedTeamAConfiguration().getMecanumConfig();
    }

    /** The Pinpoint: configuration name, pod offsets, pod type and directions. */
    public static PinpointConfig recordedPinpointConfig() {
        return TeamAPedroConfiguration.recordedTeamAConfiguration().getPinpointConfig();
    }
}
