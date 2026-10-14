package org.firstinspires.ftc.teamcode.robots.team3;

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

/** Team 3-only Pedro configuration. Real hardware values must be supplied before follower creation. */
public final class Team3PedroConfiguration {
    public static final double APPLICATION_MAX_POWER = 0.20;
    /** Hardware map name for the goBILDA Pinpoint odometry computer on Team 3's robot. */
    public static final String PINPOINT_HARDWARE_MAP_NAME = "pinpoint";

    private final String missingConfigurationReason;
    private final FollowerConstants followerConstants;
    private final MecanumConstants mecanumConstants;
    private final PinpointConstants pinpointConstants;
    private final PathConstraints pathConstraints;
    private final Pose startingPose;
    private final boolean restrictedManualDriveReady;
    private final boolean pathFollowingReady;

    private Team3PedroConfiguration(String missingConfigurationReason, FollowerConstants followerConstants,
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

    /** Returns the safe default until Team 3 records inspected physical configuration values. */
    public static Team3PedroConfiguration unconfigured() {
        return new Team3PedroConfiguration("Team 3 Pedro hardware configuration has not been recorded.",
                null, null, null, null, null, false, false);
    }

    /** Creates a configuration from supplied facts while keeping both powered permissions closed. */
    public static Team3PedroConfiguration configured(FollowerConstants followerConstants,
            MecanumConstants mecanumConstants, PinpointConstants pinpointConstants) {
        if (followerConstants == null || mecanumConstants == null || pinpointConstants == null) {
            throw new IllegalArgumentException("Pedro configuration needs follower, mecanum, and Pinpoint constants.");
        }
        return new Team3PedroConfiguration(null, followerConstants, mecanumConstants, pinpointConstants,
                PathConstraints.defaultConstraints.copy(), null, false, false);
    }

    /** Holds Team 3 facts and path permission. */
    public static Team3PedroConfiguration recordedTeam3Configuration() {
        return createRecordedTeam3Configuration(true, true);
    }

    /** Opens only the supervised restricted-manual test permission. */
    public static Team3PedroConfiguration restrictedManualTestConfiguration() {
        return createRecordedTeam3Configuration(true, false);
    }

    private static Team3PedroConfiguration createRecordedTeam3Configuration(
            boolean restrictedManualDriveReady, boolean pathFollowingReady) {
        FollowerConstants follower = new FollowerConstants()
                .mass(8.62) // 19 lbs nominal (17-21 lb range: 7.71 - 9.53 kg)
                .headingPIDFCoefficients(new PIDFCoefficients(2.2, 0.2, 0.189, 0.02))
                .forwardZeroPowerAcceleration(-42.4832745291992)
                .lateralZeroPowerAcceleration(-50.60694780564594)
                .predictiveBrakingCoefficients(new PredictiveBrakingCoefficients(
                        0.15, 0.051792761842529726, 0.002367856854157051))
                .centripetalScaling(0.0);
        MecanumConstants mecanum = new MecanumConstants()
                .xVelocity(57.666) // 5-run average: 56.67, 57.76, 58.01, 58.28, 57.61
                .yVelocity(58.178) // 5-run average: 56.12, 55.9, 59.67, 59.2, 60
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
                .forwardPodY(-8.0)
                .strafePodX(-5.75) // measured: 5.75 inches behind center of rotation
                .distanceUnit(DistanceUnit.INCH)
                .hardwareMapName(PINPOINT_HARDWARE_MAP_NAME)
                .encoderResolution(GoBildaPinpointDriver.GoBildaOdometryPods.goBILDA_SWINGARM_POD)
                .forwardEncoderDirection(GoBildaPinpointDriver.EncoderDirection.REVERSED)
                .strafeEncoderDirection(GoBildaPinpointDriver.EncoderDirection.FORWARD);
        PathConstraints constraints = new PathConstraints(0.97, 100.0);
        return new Team3PedroConfiguration(null, follower, mecanum, pinpoint, constraints,
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
            throw new IllegalStateException("Restricted manual drive is locked until safety checks begin.");
        }
    }
    void requirePathFollowingReady() {
        if (!pathFollowingReady) {
            throw new IllegalStateException("Path following is locked until localization and tuning are verified.");
        }
    }
}
