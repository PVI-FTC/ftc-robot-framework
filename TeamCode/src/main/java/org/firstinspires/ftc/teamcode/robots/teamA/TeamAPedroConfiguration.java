package org.firstinspires.ftc.teamcode.robots.teamA;

import com.pedropathing.control.PIDFCoefficients;
import com.pedropathing.control.PredictiveBrakingCoefficients;
import com.pedropathing.follower.FollowerConstants;
import com.pedropathing.geometry.Pose;
import com.pedropathing.ftc.drivetrains.MecanumConstants;
import com.pedropathing.ftc.localization.constants.PinpointConstants;
import com.pedropathing.paths.PathConstraints;
import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;

/** Team A-only Pedro configuration. Real hardware values must be supplied before follower creation. */
public final class TeamAPedroConfiguration {
    public static final double APPLICATION_MAX_POWER = 0.20;

    private final String missingConfigurationReason;
    private final FollowerConstants followerConstants;
    private final MecanumConstants mecanumConstants;
    private final PinpointConstants pinpointConstants;
    private final PathConstraints pathConstraints;
    private final Pose startingPose;
    private final boolean restrictedManualDriveReady;
    private final boolean pathFollowingReady;

    private TeamAPedroConfiguration(String missingConfigurationReason, FollowerConstants followerConstants,
            MecanumConstants mecanumConstants, PinpointConstants pinpointConstants,
            PathConstraints pathConstraints, Pose startingPose, boolean restrictedManualDriveReady,
            boolean pathFollowingReady) {
        this.missingConfigurationReason = missingConfigurationReason;
        this.followerConstants = followerConstants;
        this.mecanumConstants = mecanumConstants;
        this.pinpointConstants = pinpointConstants;
        this.pathConstraints = pathConstraints;
        this.startingPose = startingPose;
        this.restrictedManualDriveReady = restrictedManualDriveReady;
        this.pathFollowingReady = pathFollowingReady;
    }

    /** Returns the safe default until Team A records inspected physical configuration values. */
    public static TeamAPedroConfiguration unconfigured() {
        return new TeamAPedroConfiguration("Team A Pedro hardware configuration has not been recorded.",
                null, null, null, null, null, false, false);
    }

    /** Creates a configuration from supplied facts while keeping both powered permissions closed. */
    public static TeamAPedroConfiguration configured(FollowerConstants followerConstants,
            MecanumConstants mecanumConstants, PinpointConstants pinpointConstants) {
        if (followerConstants == null || mecanumConstants == null || pinpointConstants == null) {
            throw new IllegalArgumentException("Pedro configuration needs follower, mecanum, and Pinpoint constants.");
        }
        return new TeamAPedroConfiguration(null, followerConstants, mecanumConstants, pinpointConstants,
                PathConstraints.defaultConstraints.copy(), null, false, false);
    }

    /** Holds Team A facts and the path permission accepted through LP-10. */
    public static TeamAPedroConfiguration recordedTeamAConfiguration() {
        return createRecordedTeamAConfiguration(true, true);
    }

    /** Opens only the supervised LP-09 restricted-manual test permission. */
    public static TeamAPedroConfiguration restrictedManualTestConfiguration() {
        return createRecordedTeamAConfiguration(true, false);
    }

    private static TeamAPedroConfiguration createRecordedTeamAConfiguration(
            boolean restrictedManualDriveReady, boolean pathFollowingReady) {
        FollowerConstants follower = new FollowerConstants()
                .mass(4.85)
                .headingPIDFCoefficients(new PIDFCoefficients(2.2, 0.2, 0.189, 0.02))
                .forwardZeroPowerAcceleration(-42.4832745291992)
                .lateralZeroPowerAcceleration(-50.60694780564594)
                .predictiveBrakingCoefficients(new PredictiveBrakingCoefficients(
                        0.15, 0.051792761842529726, 0.002367856854157051))
                .centripetalScaling(0.0);
        MecanumConstants mecanum = new MecanumConstants()
                .xVelocity(62.61374213751846)
                .yVelocity(49.757958599901585)
                .maxPower(APPLICATION_MAX_POWER)
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
                // LP-09 forward test: 24 in forward reported -23.8 in before this reversal.
                .forwardEncoderDirection(GoBildaPinpointDriver.EncoderDirection.REVERSED)
                // LP-09 left test: 24 in left reported -24.2 in before this reversal.
                .strafeEncoderDirection(GoBildaPinpointDriver.EncoderDirection.REVERSED);
        PathConstraints constraints = new PathConstraints(0.97, 100.0);
        return new TeamAPedroConfiguration(null, follower, mecanum, pinpoint, constraints,
                new Pose(0.0, 0.0, 0.0), restrictedManualDriveReady, pathFollowingReady);
    }

    public boolean isConfigured() { return missingConfigurationReason == null; }
    public boolean isSafeInitializationReady() { return isConfigured(); }
    public boolean isRestrictedManualDriveReady() { return restrictedManualDriveReady; }
    public boolean isPathFollowingReady() { return pathFollowingReady; }
    public String getMissingConfigurationReason() { return missingConfigurationReason; }
    FollowerConstants getFollowerConstants() { requireConfigured(); return followerConstants; }
    MecanumConstants getMecanumConstants() { requireConfigured(); return mecanumConstants; }
    PinpointConstants getPinpointConstants() { requireConfigured(); return pinpointConstants; }
    PathConstraints getPathConstraints() { requireConfigured(); return pathConstraints; }
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
