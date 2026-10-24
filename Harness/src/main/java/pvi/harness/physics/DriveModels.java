package pvi.harness.physics;

import com.pedropathing.ftc.drivetrains.MecanumConstants;

import org.firstinspires.ftc.teamcode.robots.teamA.TeamAPedroHarnessAccess;

/**
 * Drive models for the real robots, with the source of every number.
 *
 * <p>Values decided in position-estimator Step 0. When the team measures a placeholder, change
 * it from {@code placeholder(...)} to {@code measured(...)} and write down where it came from.</p>
 */
public final class DriveModels {
    /** Only static methods here; nothing to create. */
    private DriveModels() {
    }

    /**
     * Team A's mecanum robot.
     *
     * <ul>
     *   <li><b>Forward and strafe speed:</b> read live from {@code TeamAPedroConfiguration}
     *       (Pedro's xVelocity / yVelocity), so re-tuning updates the harness automatically.
     *       IMPLEMENTATION_STATUS.md records these as the latest completed full-power tuner
     *       results, superseding earlier values.</li>
     *   <li><b>Turn rate:</b> not recorded anywhere. 180 °/s placeholder.</li>
     *   <li><b>Size:</b> physical dimensions are unverified (PEDRO_ROBOT_SETUP_REFERENCE.md).
     *       18 × 18 in is the FTC maximum starting size.</li>
     *   <li><b>Momentum:</b> {@link Dynamics#teamA()}: coasting from Pedro's zero-power
     *       acceleration tuners; response time and traction limit are placeholders.</li>
     *   <li><b>Battery:</b> the speeds were measured at some battery voltage that wasn't
     *       written down; 12.5 V (a charged battery) is the placeholder, so a lower battery
     *       slows the robot in proportion.</li>
     * </ul>
     */
    public static DriveModel teamA() {
        MecanumConstants pedro = TeamAPedroHarnessAccess.recordedMecanumConstants();
        return new DriveModel("Team A",
                ModelValue.measured(pedro.xVelocity, ModelValue.INCHES_PER_SECOND,
                        "Pedro xVelocity in TeamAPedroConfiguration (latest Forward Velocity Tuner)"),
                ModelValue.measured(pedro.yVelocity, ModelValue.INCHES_PER_SECOND,
                        "Pedro yVelocity in TeamAPedroConfiguration (latest Lateral Velocity Tuner)"),
                ModelValue.placeholder(180.0, ModelValue.DEGREES_PER_SECOND,
                        "not recorded; spin at full power for 1 s and measure the degrees turned"),
                ModelValue.placeholder(18.0, ModelValue.INCHES,
                        "unverified; FTC maximum starting size. Measure bumper to bumper"),
                ModelValue.placeholder(18.0, ModelValue.INCHES,
                        "unverified; FTC maximum starting size. Measure bumper to bumper"),
                Dynamics.teamA())
                .withSpeedVoltage(ModelValue.placeholder(12.5, ModelValue.VOLTS,
                        "not recorded; battery voltage when Pedro's velocity tuners ran. Write it down next"
                                + " time the tuners run"));
    }
}
