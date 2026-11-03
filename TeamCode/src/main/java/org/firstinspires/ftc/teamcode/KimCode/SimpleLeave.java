package org.firstinspires.ftc.teamcode.KimCode;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotor;

@Autonomous(name = "SimpleLeave", group = "KimCode")
public final class SimpleLeave extends LinearOpMode {
    @Override
    public void runOpMode() throws InterruptedException {
        DcMotor fr = hardwareMap.get(DcMotor.class, "fr");
        DcMotor fl = hardwareMap.get(DcMotor.class, "fl");
        DcMotor br = hardwareMap.get(DcMotor.class, "br");
        DcMotor bl = hardwareMap.get(DcMotor.class, "bl");

        fl.setDirection(DcMotor.Direction.FORWARD);
        fr.setDirection(DcMotor.Direction.FORWARD);
        br.setDirection(DcMotor.Direction.REVERSE);
        bl.setDirection(DcMotor.Direction.REVERSE);

        waitForStart();
        if (isStopRequested()) return;

        fr.setPower(0.3);
        fl.setPower(0.3);
        br.setPower(0.3);
        bl.setPower(0.3);
        sleep(250);
        fr.setPower(0);
        fl.setPower(0);
        br.setPower(0);
        bl.setPower(0);
    }
}
