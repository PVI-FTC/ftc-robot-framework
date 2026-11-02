package pvi.harness.physics;

import pvi.harness.runtime.HarnessConfig;

/**
 * Drive models for the real robots, with the source of every number.
 *
 * <p>The numbers live in {@code harness.properties}. When the team measures a placeholder, change
 * it there from {@code placeholder} to {@code measured} and write down where it came from.</p>
 */
public final class DriveModels {
    /** Only static methods here; nothing to create. */
    private DriveModels() {
    }

    /** The robot in a settings file: speeds, turn rate, size, momentum and battery, each with its source. */
    public static DriveModel fromConfig(HarnessConfig config) {
        return new DriveModel(config.robotName(), config.forwardSpeed(), config.strafeSpeed(),
                config.turnRate(), config.lengthInches(), config.widthInches(), Dynamics.fromConfig(config))
                .withSpeedVoltage(config.speedVoltage());
    }

    /**
     * The robot in {@code harness.properties}. For PVI that's Team A's mecanum robot:
     *
     * <ul>
     *   <li><b>Forward and strafe speed:</b> Pedro's xVelocity / yVelocity, copied from
     *       {@code TeamAPedroConfiguration}. The PVI extras check that they still match, so a
     *       re-tune that isn't copied over makes a test fail. IMPLEMENTATION_STATUS.md records
     *       these as the latest completed full-power tuner results.</li>
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
        return fromConfig(HarnessConfig.load());
    }
}
