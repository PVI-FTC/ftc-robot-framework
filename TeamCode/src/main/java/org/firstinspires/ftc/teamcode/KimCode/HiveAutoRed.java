package org.firstinspires.ftc.teamcode.KimCode;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;

/** The Hive Auto for the Red alliance. All of the behavior is in {@link HiveAuto}. */
@Autonomous(name = "Hive Auto Red", group = "KimCode")
public final class HiveAutoRed extends HiveAuto {
    @Override
    protected HiveField.Alliance alliance() {
        return HiveField.Alliance.RED;
    }
}
