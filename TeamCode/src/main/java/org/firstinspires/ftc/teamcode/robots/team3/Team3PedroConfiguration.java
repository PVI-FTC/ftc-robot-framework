package org.firstinspires.ftc.teamcode.robots.team3;

import com.pedropathing.algorithm.ForesightConfig;
import com.pedropathing.api.PoseFactory;
import com.pedropathing.controllers.Controller;
import com.pedropathing.math.Matrix;
import com.pedropathing.math.Pose;
import com.pedropathing.math.Vector2D;
import com.pedropathing.revhub.drivetrains.MecanumConfig;
import com.pedropathing.revhub.localizers.PinpointConfig;
import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;

/** Team 3-only Pedro 3 configuration. Real hardware values must be supplied before follower creation. */
public final class Team3PedroConfiguration {
    public static final double APPLICATION_MAX_POWER = 0.20;
    /** Hardware map name for the goBILDA Pinpoint odometry computer on Team 3's robot. */
    public static final String PINPOINT_HARDWARE_MAP_NAME = "pinpoint";

    private final String missingConfigurationReason;
    private final ForesightConfig foresightConfig;
    private final MecanumConfig mecanumConfig;
    private final PinpointConfig pinpointConfig;
    private final Pose startingPose;
    private final boolean restrictedManualDriveReady;
    private final boolean pathFollowingReady;

    private Team3PedroConfiguration(String missingConfigurationReason, ForesightConfig foresightConfig,
            MecanumConfig mecanumConfig, PinpointConfig pinpointConfig, Pose startingPose,
            boolean restrictedManualDriveReady, boolean pathFollowingReady) {
        this.missingConfigurationReason = missingConfigurationReason;
        this.foresightConfig = foresightConfig;
        this.mecanumConfig = mecanumConfig;
        this.pinpointConfig = pinpointConfig;
        this.startingPose = startingPose;
        this.restrictedManualDriveReady = restrictedManualDriveReady;
        this.pathFollowingReady = pathFollowingReady;
    }

    /** Returns the safe default until Team 3 records inspected physical configuration values. */
    public static Team3PedroConfiguration unconfigured() {
        return new Team3PedroConfiguration("Team 3 Pedro hardware configuration has not been recorded.",
                null, null, null, null, false, false);
    }

    /** Creates a configuration from supplied facts while keeping both powered permissions closed. */
    public static Team3PedroConfiguration configured(ForesightConfig foresightConfig,
            MecanumConfig mecanumConfig, PinpointConfig pinpointConfig) {
        if (foresightConfig == null || mecanumConfig == null || pinpointConfig == null) {
            throw new IllegalArgumentException("Pedro 3 configuration needs Foresight, mecanum, and Pinpoint configs.");
        }
        return new Team3PedroConfiguration(null, foresightConfig, mecanumConfig, pinpointConfig, null, false, false);
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
        ForesightConfig foresight = new ForesightConfig(c -> {
            Controller primaryTranslationalForward = Controller.proportional(0.6340519057428275);
            Controller secondaryTranslationalForward = Controller.proportional(0.23426523699302473);
            Controller primaryTranslationalLateral = Controller.proportional(0.506689633462168);
            Controller secondaryTranslationalLateral = Controller.proportional(0.1872082805679137);

            c.forwardTranslational.set(Controller.piecewise(secondaryTranslationalForward).put(2.5, primaryTranslationalForward));
            c.strafeTranslational.set(Controller.piecewise(secondaryTranslationalLateral).put(2.5, primaryTranslationalLateral));

            c.coast.set(Controller.proportionalFeedforward(0.014646926517752679));
            c.brake.set(Controller.proportionalFeedforward(0.012449887540089776));

            c.headingFeedback.set(Controller.proportional(5.598759885671741));
            c.headingBrakeCoefficients.set(Vector2D.cartesian(0.04652137561314064, 0.013741321592943428));

            c.linearBrakeCoefficients.set(Matrix.diag(0.06848421743295197, 0.05854183147252824));
            c.quadraticBrakeCoefficients.set(Matrix.diag(0.0020064501092362367, 0.0024716957768805316));

            c.maxAchievableForwardVelocity.set(70.01563114118758);
            c.maxAchievableStrafeVelocity.set(52.014594764628455);
            c.naturalForwardDeceleration.set(32.54832913388586);
            c.naturalStrafeDeceleration.set(62.89693210699509);
            c.maxPathSpeed.set(0.97);
            c.maxVelocityConstraint.set(100.0);
            c.velocityConstraint.set(1.0);
            c.translationalConstraint.set(1.0);
            c.headingConstraint.set(1.0);
            c.timeoutConstraint.set(100.0);
        });
        MecanumConfig mecanum = new MecanumConfig(c -> {
            c.frontLeftName.set("frontLeft");
            c.backLeftName.set("rearLeft");
            c.frontRightName.set("frontRight");
            c.backRightName.set("rearRight");
            c.frontLeftDirection.set(DcMotorSimple.Direction.REVERSE);
            c.backLeftDirection.set(DcMotorSimple.Direction.REVERSE);
            c.frontRightDirection.set(DcMotorSimple.Direction.FORWARD);
            c.backRightDirection.set(DcMotorSimple.Direction.FORWARD);
            c.manualBrakeMode.set(true);
        });
        PinpointConfig pinpoint = new PinpointConfig(c -> {
            c.name.set(PINPOINT_HARDWARE_MAP_NAME);
            c.xPodOffset.set(-8.0);
            c.yPodOffset.set(-5.75); // measured: 5.75 inches behind center of rotation
            c.offsetUnits.set(DistanceUnit.INCH);
            c.globalDistanceUnit.set(DistanceUnit.INCH);
            c.encoderResolutionUnit.set(DistanceUnit.INCH);
            c.podType.set(GoBildaPinpointDriver.GoBildaOdometryPods.goBILDA_SWINGARM_POD);
            c.xPodDirection.set(GoBildaPinpointDriver.EncoderDirection.REVERSED);
            c.yPodDirection.set(GoBildaPinpointDriver.EncoderDirection.FORWARD);
        });
        return new Team3PedroConfiguration(null, foresight, mecanum, pinpoint,
                PoseFactory.radians().of(0.0, 0.0, 0.0), restrictedManualDriveReady, pathFollowingReady);
    }

    public boolean isConfigured() { return missingConfigurationReason == null; }
    public boolean isSafeInitializationReady() { return isConfigured(); }
    public boolean isRestrictedManualDriveReady() { return restrictedManualDriveReady; }
    public boolean isPathFollowingReady() { return pathFollowingReady; }
    public String getMissingConfigurationReason() { return missingConfigurationReason; }
    ForesightConfig getForesightConfig() { requireConfigured(); return foresightConfig; }
    MecanumConfig getMecanumConfig() { requireConfigured(); return mecanumConfig; }
    PinpointConfig getPinpointConfig() { requireConfigured(); return pinpointConfig; }
    Pose getStartingPose() { requireConfigured(); return startingPose; }
    void requireConfigured() {
        if (!isConfigured()) throw new IllegalStateException(missingConfigurationReason);
    }
    void requireRestrictedManualDriveReady() {
        if (!restrictedManualDriveReady) throw new IllegalStateException(
                "Restricted manual drive is locked until safety checks begin.");
    }
    void requirePathFollowingReady() {
        if (!pathFollowingReady) throw new IllegalStateException(
                "Path following is locked until localization and tuning are verified.");
    }
}
