package org.firstinspires.ftc.teamcode.KimCode.ClaudeKimCode;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;

/** The Hive Auto for the Blue alliance. All of the behavior is in {@link HiveAuto}. */
@Autonomous(name = "Hive Auto Blue", group = "KimCode")
public final class HiveAutoBlue extends HiveAuto {
    @Override
    protected HiveField.Alliance alliance() {
        return HiveField.Alliance.BLUE;
    }
}
