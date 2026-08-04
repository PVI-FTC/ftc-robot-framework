package org.firstinspires.ftc.teamcode.robots.teamA;

import com.pedropathing.follower.FollowerConstants;
import com.pedropathing.geometry.Pose;
import com.pedropathing.ftc.drivetrains.MecanumConstants;
import com.pedropathing.ftc.localization.constants.PinpointConstants;
import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;

/** Team A-only Pedro configuration. Real hardware values must be supplied before follower creation. */
public final class TeamAPedroConfiguration {
    private final String missingConfigurationReason;
    private final FollowerConstants followerConstants;
    private final MecanumConstants mecanumConstants;
    private final PinpointConstants pinpointConstants;
    private final Pose startingPose;
    private final boolean restrictedManualDriveReady;
    private final boolean pathFollowingReady;

    private TeamAPedroConfiguration(String missingConfigurationReason, FollowerConstants followerConstants,
            MecanumConstants mecanumConstants, PinpointConstants pinpointConstants, Pose startingPose,
            boolean restrictedManualDriveReady, boolean pathFollowingReady) {
        this.missingConfigurationReason = missingConfigurationReason;
        this.followerConstants = followerConstants;
        this.mecanumConstants = mecanumConstants;
        this.pinpointConstants = pinpointConstants;
        this.startingPose = startingPose;
        this.restrictedManualDriveReady = restrictedManualDriveReady;
        this.pathFollowingReady = pathFollowingReady;
    }

    /** Returns the safe default until Team A records inspected physical configuration values. */
    public static TeamAPedroConfiguration unconfigured() {
        return new TeamAPedroConfiguration("Team A Pedro hardware configuration has not been recorded.",
                null, null, null, null, false, false);
    }

    /** Creates a configuration from supplied facts while keeping both powered permissions closed. */
    public static TeamAPedroConfiguration configured(FollowerConstants followerConstants,
            MecanumConstants mecanumConstants, PinpointConstants pinpointConstants) {
        if (followerConstants == null || mecanumConstants == null || pinpointConstants == null) {
            throw new IllegalArgumentException("Pedro configuration needs follower, mecanum, and Pinpoint constants.");
        }
        return new TeamAPedroConfiguration(null, followerConstants, mecanumConstants, pinpointConstants,
                null, false, false);
    }

    /**
     * Holds Team A facts recorded during LP-08. Encoder and motor directions still require the
     * supervised LP-09 checks, so powered permissions remain closed.
     */
    public static TeamAPedroConfiguration recordedTeamAConfiguration() {
        FollowerConstants follower = new FollowerConstants().mass(7.0);
        MecanumConstants mecanum = new MecanumConstants()
                .maxPower(0.20)
                .leftFrontMotorName("frontLeft")
                .leftRearMotorName("rearLeft")
                .rightFrontMotorName("frontRight")
                .rightRearMotorName("rearRight")
                .leftFrontMotorDirection(DcMotorSimple.Direction.REVERSE)
                .leftRearMotorDirection(DcMotorSimple.Direction.REVERSE)
                .rightFrontMotorDirection(DcMotorSimple.Direction.FORWARD)
                .rightRearMotorDirection(DcMotorSimple.Direction.FORWARD);
        PinpointConstants pinpoint = new PinpointConstants()
                .forwardPodY(-6.25)
                .strafePodX(-10.0)
                .distanceUnit(DistanceUnit.INCH)
                .hardwareMapName("pinpoint")
                .encoderResolution(GoBildaPinpointDriver.GoBildaOdometryPods.goBILDA_4_BAR_POD)
                // Provisional test setup only; LP-09 must verify both coordinate signs.
                .forwardEncoderDirection(GoBildaPinpointDriver.EncoderDirection.FORWARD)
                .strafeEncoderDirection(GoBildaPinpointDriver.EncoderDirection.FORWARD);
        return new TeamAPedroConfiguration(null, follower, mecanum, pinpoint,
                new Pose(0.0, 0.0, 0.0), false, false);
    }

    public boolean isConfigured() { return missingConfigurationReason == null; }
    public boolean isSafeInitializationReady() { return isConfigured(); }
    public boolean isRestrictedManualDriveReady() { return restrictedManualDriveReady; }
    public boolean isPathFollowingReady() { return pathFollowingReady; }
    public String getMissingConfigurationReason() { return missingConfigurationReason; }
    FollowerConstants getFollowerConstants() { requireConfigured(); return followerConstants; }
    MecanumConstants getMecanumConstants() { requireConfigured(); return mecanumConstants; }
    PinpointConstants getPinpointConstants() { requireConfigured(); return pinpointConstants; }
    Pose getStartingPose() { requireConfigured(); return startingPose; }
    void requireConfigured() { if (!isConfigured()) throw new IllegalStateException(missingConfigurationReason); }
    void requireRestrictedManualDriveReady() {
        if (!restrictedManualDriveReady) {
            throw new IllegalStateException("Restricted manual drive is locked until LP-09 safety checks begin.");
        }
    }
    void requirePathFollowingReady() {
        if (!pathFollowingReady) {
            throw new IllegalStateException("Path following is locked until localization and tuning are verified.");
        }
    }
}
