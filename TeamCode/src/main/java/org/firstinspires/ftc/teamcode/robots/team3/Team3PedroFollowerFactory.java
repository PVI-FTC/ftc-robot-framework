package org.firstinspires.ftc.teamcode.robots.team3;

import com.pedropathing.follower.Follower;
import com.pedropathing.ftc.FollowerBuilder;
import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver;
import com.qualcomm.robotcore.hardware.HardwareMap;

/**
 * Creates Team 3's follower after the required physical configuration is available.
 *
 * <p>Each call explicitly resets the goBILDA Pinpoint hardware position before building the
 * follower. This is necessary because Pedro's {@code setStartingPose()} applies a mathematical
 * offset but does not physically command the Pinpoint to clear its accumulated position counter.
 * Without this reset, the Pinpoint retains position data across OpMode runs and power cycles,
 * causing X/Y to start at non-zero values.</p>
 */
public final class Team3PedroFollowerFactory {

    public Follower create(HardwareMap hardwareMap, Team3PedroConfiguration configuration) {
        if (hardwareMap == null) throw new IllegalArgumentException("Pedro follower needs a hardware map.");
        if (configuration == null) throw new IllegalArgumentException("Pedro follower needs configuration.");
        configuration.requireConfigured();
        resetPinpoint(hardwareMap, Team3PedroConfiguration.PINPOINT_HARDWARE_MAP_NAME);
        Follower follower = new FollowerBuilder(configuration.getFollowerConstants(), hardwareMap)
                .pinpointLocalizer(configuration.getPinpointConstants())
                .mecanumDrivetrain(configuration.getMecanumConstants())
                .pathConstraints(configuration.getPathConstraints())
                .build();
        if (configuration.getStartingPose() != null) {
            follower.setStartingPose(configuration.getStartingPose());
        }
        follower.breakFollowing();
        return follower;
    }

    public Follower create(HardwareMap hardwareMap, Team3PedroConfiguration configuration, double maxPower) {
        if (hardwareMap == null) throw new IllegalArgumentException("Pedro follower needs a hardware map.");
        if (configuration == null) throw new IllegalArgumentException("Pedro follower needs configuration.");
        configuration.requireConfigured();
        configuration.getMecanumConstants().maxPower(maxPower);
        resetPinpoint(hardwareMap, Team3PedroConfiguration.PINPOINT_HARDWARE_MAP_NAME);
        Follower follower = new FollowerBuilder(configuration.getFollowerConstants(), hardwareMap)
                .pinpointLocalizer(configuration.getPinpointConstants())
                .mecanumDrivetrain(configuration.getMecanumConstants())
                .pathConstraints(configuration.getPathConstraints())
                .build();
        if (configuration.getStartingPose() != null) {
            follower.setStartingPose(configuration.getStartingPose());
        }
        follower.breakFollowing();
        return follower;
    }

    /**
     * Resets the Pinpoint's accumulated position counter to zero.
     *
     * <p>If the Pinpoint is absent or not yet initialized, this call is silently skipped
     * so that other initialization errors are still reported clearly.</p>
     */
    private void resetPinpoint(HardwareMap hardwareMap, String pinpointName) {
        try {
            GoBildaPinpointDriver pinpoint =
                    hardwareMap.get(GoBildaPinpointDriver.class, pinpointName);
            pinpoint.resetPosAndIMU();
        } catch (Exception e) {
            // Pinpoint not found or not ready; follower initialization will surface the real error.
        }
    }
}
