package org.firstinspires.ftc.teamcode.robots.team3;

import com.pedropathing.follower.Follower;
import com.pedropathing.ftc.FollowerBuilder;
import com.qualcomm.robotcore.hardware.HardwareMap;

/** Creates Team 3's follower after the required physical configuration is available. */
public final class Team3PedroFollowerFactory {
    public Follower create(HardwareMap hardwareMap, Team3PedroConfiguration configuration) {
        if (hardwareMap == null) throw new IllegalArgumentException("Pedro follower needs a hardware map.");
        if (configuration == null) throw new IllegalArgumentException("Pedro follower needs configuration.");
        configuration.requireConfigured();
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
}
