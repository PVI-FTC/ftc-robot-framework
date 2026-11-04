package pvi.harness.pvi;

import java.util.List;

import com.pedropathing.algorithm.ForesightConfig;
import com.pedropathing.revhub.drivetrains.MecanumConfig;

import org.firstinspires.ftc.teamcode.common.hardware.DriveHardware;
import org.firstinspires.ftc.teamcode.common.hardware.IntakeHardware;
import org.firstinspires.ftc.teamcode.robots.teamA.TeamAPedroHarnessAccess;
import org.junit.jupiter.api.Test;

import pvi.harness.runtime.HarnessConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PVI extra (built only with {@code pvi.extras=true}): the robot in {@code harness.properties}
 * still matches Team A's real code.
 *
 * <p>The harness core reads its robot facts only from {@code harness.properties}, so it works in
 * any repo. For PVI those facts are copies of Team A's Pedro configuration and hardware names.
 * When someone re-tunes Pedro or renames a device, these tests fail and say which line of
 * {@code harness.properties} to update. (Pedro 3: the values are in Team A's ForesightConfig,
 * MecanumConfig and PinpointConfig.)</p>
 */
class TeamAConfigMatchesPedroTest {
    private static final HarnessConfig CONFIG = HarnessConfig.load();
    private static final String FIX = " Update harness.properties to match TeamAPedroConfiguration.";

    /**
     * WHAT: The forward and strafe speeds equal Pedro's maxAchievableForwardVelocity and
     * maxAchievableStrafeVelocity exactly.
     *
     * <p>WHY: The harness reads the copy, so a re-tune that isn't copied over must show up here.</p>
     */
    @Test
    void speedsMatchPedrosVelocityTuners() {
        ForesightConfig pedro = TeamAPedroHarnessAccess.recordedForesightConfig();

        assertEquals(pedro.maxAchievableForwardVelocity.get(), CONFIG.forwardSpeed().value(), 0.0,
                "drive.forwardSpeedInchesPerSecond." + FIX);
        assertEquals(pedro.maxAchievableStrafeVelocity.get(), CONFIG.strafeSpeed().value(), 0.0,
                "drive.strafeSpeedInchesPerSecond." + FIX);
        assertTrue(CONFIG.forwardSpeed().measured() && CONFIG.strafeSpeed().measured(),
                "the tuned speeds are measurements");
    }

    /**
     * WHAT: The coasting decelerations equal Pedro's natural forward and strafe decelerations.
     *
     * <p>WHY: Momentum and coasting use them, so the Pedro routes depend on them.</p>
     */
    @Test
    void coastingMatchesPedrosNaturalDeceleration() {
        ForesightConfig pedro = TeamAPedroHarnessAccess.recordedForesightConfig();

        assertEquals(Math.abs(pedro.naturalForwardDeceleration.get()), CONFIG.forwardCoast().value(), 0.0,
                "drive.forwardCoastInchesPerSecondSquared." + FIX);
        assertEquals(Math.abs(pedro.naturalStrafeDeceleration.get()), CONFIG.lateralCoast().value(), 0.0,
                "drive.lateralCoastInchesPerSecondSquared." + FIX);
    }

    /**
     * WHAT: The drive motors' names and forward directions equal the ones in Pedro's mecanum
     * config, and the Pinpoint's name equals Pedro's Pinpoint name.
     *
     * <p>WHY: A wrong direction would make the simulated robot drive the wrong way.</p>
     */
    @Test
    void motorsAndPinpointMatchPedro() {
        MecanumConfig pedro = TeamAPedroHarnessAccess.recordedMecanumConfig();

        assertEquals(new HarnessConfig.DriveMotorNames(pedro.frontLeftName.get(), pedro.frontRightName.get(),
                pedro.backLeftName.get(), pedro.backRightName.get()), CONFIG.driveMotors(),
                "drive.frontLeft and the others." + FIX);
        assertEquals(new HarnessConfig.DriveMotorDirections(pedro.frontLeftDirection.get(),
                        pedro.frontRightDirection.get(), pedro.backLeftDirection.get(), pedro.backRightDirection.get()),
                CONFIG.driveDirections(), "drive.frontLeft.direction and the others." + FIX);
        assertEquals(TeamAPedroHarnessAccess.recordedPinpointConfig().name.get(), CONFIG.pinpointName(),
                "pinpoint." + FIX);
    }

    /**
     * WHAT: The fake robot's device names equal the ones TeamCode's hardware wrappers look up.
     *
     * <p>WHY: TeamARobot asks for these names; a missing one crashes every Team A test.</p>
     */
    @Test
    void deviceNamesMatchTeamCodesHardwareWrappers() {
        assertEquals(new HarnessConfig.DriveMotorNames(DriveHardware.FRONT_LEFT_MOTOR_NAME,
                DriveHardware.FRONT_RIGHT_MOTOR_NAME, DriveHardware.REAR_LEFT_MOTOR_NAME,
                DriveHardware.REAR_RIGHT_MOTOR_NAME), CONFIG.driveMotors(), "drive names vs DriveHardware");
        assertEquals(List.of(IntakeHardware.INTAKE_MOTOR_NAME), CONFIG.otherMotors(),
                "hardware.otherMotors vs IntakeHardware");
    }
}
