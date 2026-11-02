package pvi.harness.physics;

import org.firstinspires.ftc.teamcode.common.hardware.DriveHardware;
import org.firstinspires.ftc.teamcode.common.subsystems.drive.MecanumDriveController;
import org.junit.jupiter.api.Test;

import pvi.harness.hardware.RobotConfigs;
import pvi.harness.hardware.FakeHardwareMap;
import pvi.harness.physics.MecanumKinematics.ChassisPower;
import pvi.harness.physics.MecanumKinematics.RobotVelocity;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Position estimator Step 4: four wheel pushes into whole-robot motion.
 */
class MecanumKinematicsTest {
    /** Allowed rounding error for decimal math. */
    private static final double TOLERANCE = 1e-9;

    /** Asserts every part of two ChassisPowers matches. */
    private static void assertChassis(ChassisPower expected, ChassisPower actual) {
        assertEquals(expected.forward(), actual.forward(), TOLERANCE, "forward: " + actual);
        assertEquals(expected.strafeRight(), actual.strafeRight(), TOLERANCE, "strafe: " + actual);
        assertEquals(expected.turnClockwise(), actual.turnClockwise(), TOLERANCE, "turn: " + actual);
    }

    /**
     * WHAT: Un-mixing recovers exactly what TeamCode's own MecanumDriveController was asked to do.
     *
     * <p>HOW: For many combinations of forward, strafe and turn (kept small enough that the
     * controller doesn't scale them down), runs the <b>real</b> MecanumDriveController on fake
     * motors, reads the wheel pushes with Step 3's WheelReader, and un-mixes them. The result
     * must equal the original request.</p>
     *
     * <p>WHY: This proves the estimator uses the same conventions as your team's drive code
     * (which way is "right", which way is "clockwise"), using the code itself rather than a
     * copy of its formulas.</p>
     */
    @Test
    void unmixingRecoversWhatTeamCodesControllerWasAsked() {
        FakeHardwareMap hw = RobotConfigs.driveOnly();
        DriveHardware driveHardware = new DriveHardware();
        driveHardware.initialize(hw);
        MecanumDriveController controller = new MecanumDriveController(driveHardware);
        WheelReader wheels = WheelReader.teamA();

        double[] values = {-0.3, -0.1, 0.0, 0.2, 0.3};
        for (double forward : values) {
            for (double strafe : values) {
                for (double turn : values) {
                    controller.updateManualDrive(forward, strafe, turn);

                    ChassisPower recovered = MecanumKinematics.toChassisPower(wheels.read(hw));

                    assertChassis(new ChassisPower(forward, strafe, turn), recovered);
                }
            }
        }
    }

    /**
     * WHAT: The three pure wheel patterns un-mix into pure motions.
     *
     * <p>HOW: All wheels forward = forward only. (+,−,−,+) = strafe right only.
     * (+,−,+,−) = clockwise turn only.</p>
     *
     * <p>WHY: These are the textbook mecanum patterns. Each must produce exactly one kind of
     * motion with nothing leaking into the other two.</p>
     */
    @Test
    void pureWheelPatternsGivePureMotions() {
        assertChassis(new ChassisPower(1, 0, 0),
                MecanumKinematics.toChassisPower(new WheelPushes(1, 1, 1, 1)));
        assertChassis(new ChassisPower(0, 1, 0),
                MecanumKinematics.toChassisPower(new WheelPushes(1, -1, -1, 1)));
        assertChassis(new ChassisPower(0, 0, 1),
                MecanumKinematics.toChassisPower(new WheelPushes(1, -1, 1, -1)));
    }

    /**
     * WHAT: Mixing then un-mixing gives back the original (for the harness's own mixer).
     *
     * <p>WHY: Tests in Step 5 use toWheelPushes to set up motions; it must agree with
     * toChassisPower.</p>
     */
    @Test
    void mixingAndUnmixingAreOpposites() {
        ChassisPower original = new ChassisPower(0.25, -0.4, 0.15);

        assertChassis(original, MecanumKinematics.toChassisPower(MecanumKinematics.toWheelPushes(original)));
    }

    /**
     * WHAT: Power fractions become real speeds, in the standard left/counter-clockwise convention.
     *
     * <p>HOW: With a test robot (4 ft/s forward, 3 ft/s strafe, 90 °/s turn): full forward is
     * 48 in/s; full strafe right is -36 in/s "left"; full clockwise turn is -π/2 rad/s.</p>
     *
     * <p>WHY: TeamCode uses "right" and "clockwise" as positive; positions on the field use
     * "left" and "counter-clockwise" as positive. This is the one place the sign flips.</p>
     */
    @Test
    void convertsPowersToSpeedsWithStandardSigns() {
        DriveModel model = DriveModel.forTesting("test", 4, 3, 90);

        RobotVelocity forward = MecanumKinematics.toVelocity(new ChassisPower(1, 0, 0), model);
        RobotVelocity right = MecanumKinematics.toVelocity(new ChassisPower(0, 1, 0), model);
        RobotVelocity clockwise = MecanumKinematics.toVelocity(new ChassisPower(0, 0, 1), model);

        assertEquals(48.0, forward.forwardInchesPerSecond(), TOLERANCE);
        assertEquals(-36.0, right.leftInchesPerSecond(), TOLERANCE);
        assertEquals(-Math.PI / 2, clockwise.counterClockwiseRadiansPerSecond(), TOLERANCE);
        assertEquals(0.0, clockwise.speedInchesPerSecond(), TOLERANCE);
    }
}
