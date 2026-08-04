package org.firstinspires.ftc.teamcode.robots.teamA;

import com.pedropathing.follower.FollowerConstants;
import com.pedropathing.ftc.drivetrains.MecanumConstants;
import com.pedropathing.ftc.localization.constants.PinpointConstants;

/** Team A-only Pedro configuration. Real hardware values must be supplied before follower creation. */
public final class TeamAPedroConfiguration {
    private final String missingConfigurationReason;
    private final FollowerConstants followerConstants;
    private final MecanumConstants mecanumConstants;
    private final PinpointConstants pinpointConstants;

    private TeamAPedroConfiguration(String missingConfigurationReason, FollowerConstants followerConstants,
            MecanumConstants mecanumConstants, PinpointConstants pinpointConstants) {
        this.missingConfigurationReason = missingConfigurationReason;
        this.followerConstants = followerConstants;
        this.mecanumConstants = mecanumConstants;
        this.pinpointConstants = pinpointConstants;
    }

    /** Returns the safe default until Team A records inspected physical configuration values. */
    public static TeamAPedroConfiguration unconfigured() {
        return new TeamAPedroConfiguration("Team A Pedro hardware configuration has not been recorded.",
                null, null, null);
    }

    /** Creates a configuration from real Team A hardware and tuning facts. */
    public static TeamAPedroConfiguration configured(FollowerConstants followerConstants,
            MecanumConstants mecanumConstants, PinpointConstants pinpointConstants) {
        if (followerConstants == null || mecanumConstants == null || pinpointConstants == null) {
            throw new IllegalArgumentException("Pedro configuration needs follower, mecanum, and Pinpoint constants.");
        }
        return new TeamAPedroConfiguration(null, followerConstants, mecanumConstants, pinpointConstants);
    }

    public boolean isConfigured() { return missingConfigurationReason == null; }
    public String getMissingConfigurationReason() { return missingConfigurationReason; }
    FollowerConstants getFollowerConstants() { requireConfigured(); return followerConstants; }
    MecanumConstants getMecanumConstants() { requireConfigured(); return mecanumConstants; }
    PinpointConstants getPinpointConstants() { requireConfigured(); return pinpointConstants; }
    void requireConfigured() { if (!isConfigured()) throw new IllegalStateException(missingConfigurationReason); }
}