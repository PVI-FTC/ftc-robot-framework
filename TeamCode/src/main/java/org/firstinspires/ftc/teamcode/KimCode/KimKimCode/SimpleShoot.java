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
import com.qualcomm.robotcore.hardware.Blinker;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.util.ElapsedTime;
import static com.pedropathing.ivy.Scheduler.schedule;
import static com.pedropathing.ivy.groups.Groups.sequential;
@Autonomous
public class SimpleShoot extends OpMode {
    private Follower follower;
    private final PoseFactory p = PoseFactory.degrees();
    private DcMotor fr;
    private DcMotor fl;
    private DcMotor br;
    private DcMotor bl;
    private DcMotorEx outtake;
    private DcMotorEx intake;
    private DcMotorEx transfer;
    private final ElapsedTime timer = new ElapsedTime();

    //Poses---------------------------------
    private final Pose Start = p.of(56, 9.14, 90);
    private final Pose OneStart = Start;
    private final Pose OneControl = p.of(68.6437, 34.5539, 0);
    private final Pose OneEnd = p.of(41.1941, 20.9864, 180);
    private final Pose TwoStart = OneEnd;
    private final Pose TwoEnd = p.of(27.8803, 9.14, 180);
    private final Pose ThreeStart = TwoEnd;
    private final Pose ThreeEnd = p.of(9.1765, 9.012, 180);
    private final Pose FourStart = ThreeEnd;
    private final Pose FourControl = p.of(41.3854, 53.5739, 0);
    private final Pose FourEnd = p.of(56, 9.14, 90);
    private final Pose FiveStart = FourEnd;
    private final Pose FiveControl = p.of(16.4887, 42.3618, 0);
    private final Pose FiveEnd = p.of(16.329, 95.7103, 90);
    //--------------------------------------

    //Paths---------------------------------
    public Path PathOne() {
        return Paths.curve(OneStart, OneControl, OneEnd).linear(OneStart, OneEnd);
    }
    public Path PathTwo() {
        return Paths.line(TwoStart, TwoEnd).constant(TwoEnd);
    }
    public Path PathThree() {
        return Paths.line(ThreeStart, ThreeEnd).constant(ThreeEnd);
    }
    public Path PathFour() {
        return Paths.curve(FourStart, FourControl, FourEnd).linear(FourStart, FourEnd);
    }
    public Path PathFive() {
        return Paths.curve(FiveStart, FiveControl, FiveEnd).constant(FiveEnd);
    }
    //----------------------------------------

    //COMMANDS-------------------------------------
    Command Shoot = Command.build()
            .setStart(() -> {
                timer.reset();
                outtake.setPower(1);
            })
            .setExecute(() -> {
                if (timer.seconds() >= 3.0) {
                    intake.setPower(1);
                    transfer.setPower(1);
                }
            })
            .setDone(() -> timer.seconds() >= 5.0
            )
            .setEnd(endCondition -> {
                intake.setPower(0);
                transfer.setPower(0);
                outtake.setPower(0);
            });
    //--------------------------------------------


    //FINAL ROUTINE --------------------
    private Command autoRoutine() {
        return sequential(
                Shoot,
                follow(follower, PathOne()),
                follow(follower, PathTwo()),
                follow(follower, PathThree()),
                follow(follower, PathFour()),
                Shoot,
                follow(follower, PathFive())
        );
    }
    //--------------------------------

    @Override
    public void init() {
        follower = Constants.create(hardwareMap);
        follower.setPose(Start);
        Scheduler.reset();
        follower.update();

        //hardware mapping-------------------------------------------------------
        fr = hardwareMap.get(DcMotor .class, "fr");
        fl = hardwareMap.get(DcMotor.class, "fl");
        br = hardwareMap.get(DcMotor.class, "br");
        bl = hardwareMap.get(DcMotor.class, "bl");

        fr.setDirection(DcMotorSimple.Direction.FORWARD);
        bl.setDirection(DcMotorSimple.Direction.REVERSE);
        fl.setDirection(DcMotorSimple.Direction.REVERSE);
        br.setDirection(DcMotorSimple.Direction.FORWARD);

        //outtake.setDirection(DcMotorSimple.Direction.FORWARD);
        //intake.setDirection(DcMotorSimple.Direction.REVERSE);
        //transfer.setDirection(DcMotorSimple.Direction.REVERSE);

        outtake = hardwareMap.get(DcMotorEx.class, "outtake");
        intake = hardwareMap.get(DcMotorEx.class, "intake");
        transfer = hardwareMap.get(DcMotorEx.class, "transfer");
        //----------------------------------------------------------------------
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
    @Override
    public void stop() {
        OpModeStorage.autonomousEndPose = follower.pose(); //saves your position in that file
    }
}
