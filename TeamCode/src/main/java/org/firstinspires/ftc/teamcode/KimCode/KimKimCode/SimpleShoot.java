package org.firstinspires.ftc.teamcode.KimCode.KimKimCode;

import com.pedropathing.api.Paths;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.pedropathing.follower.Follower;
import org.firstinspires.ftc.teamcode.pedro.Constants;
import com.pedropathing.api.PoseFactory;
import com.pedropathing.math.Pose;
import static com.pedropathing.api.Paths.*;
import com.pedropathing.paths.Path;
import com.pedropathing.ivy.Scheduler;
import static com.pedropathing.ivy.Scheduler.schedule;
import static com.pedropathing.ivy.pedro.PedroCommands.follow;
import com.pedropathing.ivy.Command;
import com.pedropathing.ivy.Scheduler;
import static com.pedropathing.ivy.Scheduler.schedule;
import static com.pedropathing.ivy.groups.Groups.sequential;
@Autonomous
public class SimpleShoot extends OpMode {
    private Follower follower;
    private final PoseFactory p = PoseFactory.degrees();

    //Poses---------------------------------
    private final Pose start = p.of(24, 24, 0);
    private final Pose control = p.of(24, 24, 0);
    private final Pose end = p.of(48, 48, 90);
    //--------------------------------------

    //Paths---------------------------------
    public Path curvePath() {
        return Paths.curve(start, control, end);
    }
    //----------------------------------------

    //FINAL ROUTINE --------------------
    private Command autoRoutine() {
        return sequential(
                follow(follower, curvePath()),
                // Add mechanism commands here.
                //follow(follower, park())
        );
    }
    //--------------------------------

    @Override
    public void init() {
        follower = Constants.create(hardwareMap);
        follower.setPose(start);
        Scheduler.reset();
        follower.update();

    }
    @Override
    public void start() {
        schedule(autoRoutine());
    }
    @Override
    public void loop() {
        follower.update();
        Scheduler.execute();

        telemetry.addData("X", follower.pose().x());
        telemetry.addData("Y", follower.pose().y());
        telemetry.addData("Heading", Math.toDegrees(follower.pose().heading()));
        telemetry.addData("Follower Mode", follower.mode());
        telemetry.update();
    }
}
