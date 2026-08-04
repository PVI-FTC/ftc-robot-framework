package org.firstinspires.ftc.teamcode.robots.teamA;

import com.pedropathing.follower.Follower;
import com.pedropathing.ftc.FollowerBuilder;
import com.qualcomm.robotcore.hardware.HardwareMap;

/** Creates Team A's follower after the required physical configuration is available. */
public final class TeamAPedroFollowerFactory {
    public Follower create(HardwareMap hardwareMap, TeamAPedroConfiguration configuration) {
        if (hardwareMap == null) throw new IllegalArgumentException("Pedro follower needs a hardware map.");
        if (configuration == null) throw new IllegalArgumentException("Pedro follower needs configuration.");
        configuration.requireConfigured();
        Follower follower = new FollowerBuilder(configuration.getFollowerConstants(), hardwareMap)
                .pinpointLocalizer(configuration.getPinpointConstants())
                .mecanumDrivetrain(configuration.getMecanumConstants())
                .build();
        if (configuration.getStartingPose() != null) {
            follower.setStartingPose(configuration.getStartingPose());
        }
        follower.breakFollowing();
        return follower;
    }
}
