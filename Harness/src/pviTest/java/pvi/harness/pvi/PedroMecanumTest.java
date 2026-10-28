package pvi.harness.pvi;

import com.pedropathing.drivetrain.DrivePowers;
import com.pedropathing.revhub.drivetrains.Mecanum;

import org.firstinspires.ftc.teamcode.robots.teamA.TeamAPedroHarnessAccess;
import org.junit.jupiter.api.Test;

import pvi.harness.hardware.FakeHardwareMap;
import pvi.harness.hardware.RobotConfigs;
import pvi.harness.physics.WheelPushes;
import pvi.harness.physics.WheelReader;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PVI extra (built only with {@code pvi.extras=true}): Pedro 3's own mecanum drivetrain, built
 * from Team A's Pedro configuration, drives the harness's fake motors the way the harness expects.
 *
 * <p>Pedro 2 had a battery-compensation check here ({@code PedroBatteryTest}). Pedro 3 doesn't
 * read the battery at all (its mecanum drivetrain has no voltage compensation), so that check was
 * removed; the harness's own battery checks are in {@code pvi.harness.physics.BatteryTest}.</p>
 */
class PedroMecanumTest {

    /**
     * WHAT: Asking Pedro 3's mecanum for "full forward" makes every wheel push the robot forward,
     * as the harness reads the fake motors (motor names and directions from harness.properties).
     *
     * <p>WHY: Pedro sets each motor's direction itself. If Team A's Pedro directions and the
     * harness's ever disagree, the simulated robot would drive the wrong way on every route.</p>
     */
    @Test
    void pedroForwardMakesEveryWheelPushForward() {
        FakeHardwareMap robot = RobotConfigs.teamA();
        Mecanum drive = new Mecanum(robot, TeamAPedroHarnessAccess.recordedMecanumConfig());

        drive.drive(new DrivePowers(1.0, 0.0, 0.0), false);
        WheelPushes pushes = WheelReader.teamA().read(robot);

        double[] wheels = {pushes.frontLeft(), pushes.frontRight(), pushes.rearLeft(), pushes.rearRight()};
        for (int i = 0; i < wheels.length; i++) {
            assertTrue(wheels[i] > 0.5, "wheel " + i + " should push forward hard, but pushes " + wheels[i]);
        }
    }
}
