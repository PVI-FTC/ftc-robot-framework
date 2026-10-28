package org.firstinspires.ftc.teamcode.robots.teamA;

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

/** Team A-only Pedro 3 configuration. Powered permissions remain explicit. */
public final class TeamAPedroConfiguration {
    public static final double APPLICATION_MAX_POWER = 0.20;
    private final String missingConfigurationReason;
    private final ForesightConfig foresightConfig;
    private final MecanumConfig mecanumConfig;
    private final PinpointConfig pinpointConfig;
    private final Pose startingPose;
    private final boolean restrictedManualDriveReady;
    private final boolean pathFollowingReady;

    private TeamAPedroConfiguration(String reason, ForesightConfig foresightConfig,
            MecanumConfig mecanumConfig, PinpointConfig pinpointConfig, Pose startingPose,
            boolean manualReady, boolean pathReady) {
        this.missingConfigurationReason = reason;
        this.foresightConfig = foresightConfig;
        this.mecanumConfig = mecanumConfig;
        this.pinpointConfig = pinpointConfig;
        this.startingPose = startingPose;
        this.restrictedManualDriveReady = manualReady;
        this.pathFollowingReady = pathReady;
    }

    public static TeamAPedroConfiguration unconfigured() {
        return new TeamAPedroConfiguration("Team A Pedro hardware configuration has not been recorded.",
                null, null, null, null, false, false);
    }

    public static TeamAPedroConfiguration configured(ForesightConfig foresightConfig,
            MecanumConfig mecanumConfig, PinpointConfig pinpointConfig) {
        if (foresightConfig == null || mecanumConfig == null || pinpointConfig == null) {
            throw new IllegalArgumentException("Pedro 3 configuration needs Foresight, mecanum, and Pinpoint configs.");
        }
        return new TeamAPedroConfiguration(null, foresightConfig, mecanumConfig,
                pinpointConfig, null, false, false);
    }

    public static TeamAPedroConfiguration recordedTeamAConfiguration() {
        return createRecordedTeamAConfiguration(true, true);
    }

    public static TeamAPedroConfiguration restrictedManualTestConfiguration() {
        return createRecordedTeamAConfiguration(true, false);
    }

    private static TeamAPedroConfiguration createRecordedTeamAConfiguration(
            boolean manualReady, boolean pathReady) {
        ForesightConfig foresight = new ForesightConfig(c -> {
            c.forwardTranslational.set(Controller.proportional(0.3));
            c.strafeTranslational.set(Controller.proportional(0.3));
            c.headingFeedback.set(Controller.proportional(2.2));
            c.headingStaticFF.set(Controller.proportionalFeedforward(0.0));
            c.brake.set(Controller.proportionalFeedforward(0.008731598255925491));
            c.coast.set(Controller.proportionalFeedforward(0.010978350889324107));
            c.linearBrakeCoefficients.set(Matrix.diag(0.10605894992901523, 0.08719146175596092));
            c.quadraticBrakeCoefficients.set(Matrix.diag(0.0014663966976606565, 0.0013837064502458813));
            c.headingBrakeCoefficients.set(Vector2D.cartesian(0.05642143125655298, 0.0063829525363003695));
            c.maxAchievableForwardVelocity.set(62.61374213751846);
            c.maxAchievableStrafeVelocity.set(49.757958599901585);
            c.naturalForwardDeceleration.set(42.4832745291992);
            c.naturalStrafeDeceleration.set(50.60694780564594);
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
            c.name.set("pinpoint");
            c.xPodOffset.set(-10.0);
            c.yPodOffset.set(-6.25);
            c.offsetUnits.set(DistanceUnit.INCH);
            c.globalDistanceUnit.set(DistanceUnit.INCH);
            c.encoderResolutionUnit.set(DistanceUnit.INCH);
            c.podType.set(GoBildaPinpointDriver.GoBildaOdometryPods.goBILDA_4_BAR_POD);
            c.xPodDirection.set(GoBildaPinpointDriver.EncoderDirection.REVERSED);
            c.yPodDirection.set(GoBildaPinpointDriver.EncoderDirection.REVERSED);
        });
        return new TeamAPedroConfiguration(null, foresight, mecanum, pinpoint,
                PoseFactory.radians().of(0.0, 0.0, 0.0), manualReady, pathReady);
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
