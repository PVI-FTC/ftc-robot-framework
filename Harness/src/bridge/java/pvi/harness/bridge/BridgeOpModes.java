package pvi.harness.bridge;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;

import org.firstinspires.ftc.teamcode.opmodes.autonomous.TeamAAutoOpMode;
import org.firstinspires.ftc.teamcode.opmodes.teleop.RumbleDemoTeleOp;
import org.firstinspires.ftc.teamcode.opmodes.teleop.TeamATeleOp;
import org.firstinspires.ftc.teamcode.opmodes.teleop.TeamBTeleOp;
import org.firstinspires.ftc.teamcode.opmodes.teleop.TeamCTeleOp;

import java.util.List;
import java.util.function.Supplier;

import pvi.harness.hardware.FakeHardwareMap;
import pvi.harness.hardware.RobotConfigs;
import pvi.harness.runtime.MatchPeriod;

/**
 * The OpModes Unity can run over the live connection: the real, unchanged TeamCode classes, each
 * on its fake robot. Unity picks one by its {@code id} in a "load" message.
 *
 * <p>To offer another OpMode, add a line to {@link #all()}.</p>
 */
public final class BridgeOpModes {

    /**
     * One OpMode Unity can run.
     *
     * @param id      what Unity sends in "load", e.g. "teamA-teleop"
     * @param title   what Unity can show in a menu
     * @param period  AUTONOMOUS (30 s) or TELEOP (2:00): the match length, after which the harness stops it
     * @param opMode  makes a fresh OpMode
     * @param robot   makes its fake robot
     */
    public record Entry(String id, String title, MatchPeriod period, Supplier<OpMode> opMode,
                        Supplier<FakeHardwareMap> robot) {
    }

    /** Only static methods here; nothing to create. */
    private BridgeOpModes() {
    }

    /** Every OpMode Unity can run, in menu order. */
    public static List<Entry> all() {
        return List.of(
                new Entry("teamA-teleop", "Team A TeleOp", MatchPeriod.TELEOP, TeamATeleOp::new, RobotConfigs::teamA),
                new Entry("teamB-teleop", "Team B TeleOp", MatchPeriod.TELEOP, TeamBTeleOp::new, RobotConfigs::teamB),
                new Entry("teamC-teleop", "Team C TeleOp", MatchPeriod.TELEOP, TeamCTeleOp::new, RobotConfigs::teamC),
                new Entry("teamA-autonomous", "Team A autonomous", MatchPeriod.AUTONOMOUS, TeamAAutoOpMode::new, RobotConfigs::teamA),
                new Entry("rumble-demo", "Rumble demo TeleOp", MatchPeriod.TELEOP, RumbleDemoTeleOp::new, RobotConfigs::teamA));
    }

    /** The entry with this id, or null if there is none. */
    public static Entry find(String id) {
        for (Entry entry : all()) {
            if (entry.id().equals(id)) {
                return entry;
            }
        }
        return null;
    }
}
