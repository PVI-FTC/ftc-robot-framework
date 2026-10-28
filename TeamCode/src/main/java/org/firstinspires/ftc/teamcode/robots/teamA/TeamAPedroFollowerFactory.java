package org.firstinspires.ftc.teamcode.robots.teamA;

import com.pedropathing.algorithm.Foresight;
import com.pedropathing.follower.Follower;
import com.pedropathing.revhub.drivetrains.Mecanum;
import com.pedropathing.revhub.localizers.PinpointLocalizer;
import com.qualcomm.robotcore.hardware.HardwareMap;

/** Creates Team A's Pedro 3 follower after the required physical configuration is available. */
public final class TeamAPedroFollowerFactory {
    public Follower create(HardwareMap hardwareMap, TeamAPedroConfiguration configuration) {
        if (hardwareMap == null) throw new IllegalArgumentException("Pedro follower needs a hardware map.");
        if (configuration == null) throw new IllegalArgumentException("Pedro follower needs configuration.");
        configuration.requireConfigured();
        Follower follower = new Follower(
                new PinpointLocalizer(hardwareMap, configuration.getPinpointConfig()),
                new Mecanum(hardwareMap, configuration.getMecanumConfig()),
                new Foresight(configuration.getForesightConfig()));
        if (configuration.getStartingPose() != null) {
            follower.setPose(configuration.getStartingPose());
        }
        follower.stop();
        return follower;
    }
}
