package pvi.harness.pvi;

import java.util.List;
import java.util.Map;

import org.firstinspires.ftc.teamcode.KimCode.SimpleLeave;
import org.firstinspires.ftc.teamcode.TeleopV1;
import org.firstinspires.ftc.teamcode.opmodes.teleop.TeamATeleOp;
import org.junit.jupiter.api.Test;

import pvi.harness.bridge.BridgeProtocol;
import pvi.harness.bridge.BridgeSession;
import pvi.harness.ds.Axis;
import pvi.harness.ds.GamepadScript;
import pvi.harness.hardware.FakeHardwareMap;
import pvi.harness.hardware.RobotConfigs;
import pvi.harness.physics.ModelValue;
import pvi.harness.physics.Pose;
import pvi.harness.recorded.RecordedTestWriter;
import pvi.harness.runtime.HarnessConfig;
import pvi.harness.runtime.JsonReader;
import pvi.harness.runtime.OpModeCatalog;
import pvi.harness.runtime.OpModeRunner;
import pvi.harness.runtime.RobotProfile;
import pvi.harness.runtime.RunRecorder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PVI extra (D18): KimCode's OpModes run on their own robot, {@code robots/kimcode/drive.properties},
 * and Team A's OpModes stay on Team A's.
 *
 * <p>TeleopV1's left stick uses the normal mapping since team commit 810de43: up drives the robot
 * forward and right strafes it right. These tests check the code as it is, so if the team changes
 * the mapping, they change too.</p>
 */
class KimCodeRobotTest {
    /** A bridge gamepad message: the left stick all the way right. */
    private static final String STICK_RIGHT = "{\"buttons\": [], \"leftStickX\": 1, \"leftStickY\": 0, \"rightStickX\": 0,"
            + " \"rightStickY\": 0, \"leftTrigger\": 0, \"rightTrigger\": 0}";

    private static OpModeCatalog.Entry opMode(String simpleName) {
        return OpModeCatalog.fromConfig(HarnessConfig.load()).stream()
                .filter(e -> e.simpleName().equals(simpleName)).findFirst().orElseThrow();
    }

    private static FakeHardwareMap robotFor(String simpleName) {
        return RobotConfigs.forOpMode(opMode(simpleName), HarnessConfig.load());
    }

    /** TeleopV1 for 1 s with one axis of the left stick pushed; where the robot ends up. */
    private static Pose teleOpWith(Axis axis, double value) {
        GamepadScript driver = new GamepadScript().at(0.0).stick(axis, value);
        OpModeRunner runner = new OpModeRunner(new TeleopV1(), robotFor("TeleopV1")).gamepad1(driver);
        runner.run(0.1, 1.0).assertNoException();
        return runner.positionEstimator().pose();
    }

    /** WHAT: TeleopV1 and SimpleLeave get the KimCode robot, with the devices they ask for. */
    @Test
    void kimCodesOpModesGetTheirOwnRobot() {
        for (String simpleName : List.of("TeleopV1", "SimpleLeave")) {
            FakeHardwareMap robot = robotFor(simpleName);
            assertEquals("kimcode", robot.configName());
            for (String motor : List.of("fl", "fr", "bl", "br")) {
                assertNotNull(robot.motor(motor), motor);
            }
            assertNull(robot.motor("frontLeft"), "not Team A's motors");
            assertEquals("KimCode", robot.settings().robotName());
        }
    }

    /** WHAT: A Team A OpMode still gets Team A's motors, webcam and name. */
    @Test
    void teamAsOpModesKeepTeamAsRobot() {
        FakeHardwareMap robot = robotFor("TeamATeleOp");

        assertEquals("teamA", robot.configName());
        assertNotNull(robot.motor("frontLeft"));
        assertNull(robot.motor("fl"));
        assertEquals("Team A", robot.settings().robotName());
        assertEquals(1, robot.cameraSetups().size());
    }

    /** WHAT: Team A's TeleOp on Team A's robot still moves as its drive model says. */
    @Test
    void teamAsTeleOpStillDrives() {
        GamepadScript driver = new GamepadScript().at(0.0).stick(Axis.LEFT_STICK_Y, -1.0);
        OpModeRunner runner = new OpModeRunner(new TeamATeleOp(), robotFor("TeamATeleOp")).gamepad1(driver);
        runner.run(0.1, 1.0).assertNoException();

        assertEquals("Team A", runner.positionEstimator().model().name());
        assertTrue(runner.positionEstimator().pose().xInches() > 10, runner.positionEstimator().pose().toString());
    }

    /**
     * WHAT: The profile's OpModes are real ones, and none of its numbers claims to be measured:
     * it borrows Team A's, which weren't measured on this robot.
     */
    @Test
    void theProfileListsRealOpModesAndMeasuredNothing() {
        RobotProfile kimcode = RobotProfile.all(HarnessConfig.load()).stream()
                .filter(p -> p.id().equals("kimcode")).findFirst().orElseThrow();
        for (String name : kimcode.forOpModes()) {
            assertTrue(OpModeCatalog.fromConfig(HarnessConfig.load()).stream()
                    .anyMatch(e -> e.name().equals(name) || e.simpleName().equals(name)), name + " is no OpMode");
        }
        HarnessConfig c = kimcode.config();
        for (ModelValue fact : List.of(c.forwardSpeed(), c.strafeSpeed(), c.forwardCoast(), c.lateralCoast(),
                c.lengthInches(), c.widthInches(), c.turnRate(), c.responseTime(), c.maxAcceleration(), c.speedVoltage())) {
            assertFalse(fact.measured(), fact + " isn't measured on this robot");
        }
    }

    /** WHAT: TeleopV1 gets through INIT (Blinker, motors, Limelight) and up on its left stick drives the robot forward. */
    @Test
    void teleopV1DrivesForwardOnTheSticksUpDown() {
        Pose end = teleOpWith(Axis.LEFT_STICK_Y, -1.0);

        assertTrue(end.xInches() > 10, "forward: " + end);
        assertEquals(0.0, end.yInches(), 0.5, "not sideways: " + end);
    }

    /** WHAT: Right on TeleopV1's left stick strafes the robot right. */
    @Test
    void teleopV1StrafesOnTheSticksLeftRight() {
        Pose end = teleOpWith(Axis.LEFT_STICK_X, 1.0);

        assertTrue(end.yInches() < -10, "to the right: " + end);
        assertEquals(0.0, end.xInches(), 0.5, "not forward: " + end);
    }

    /**
     * WHAT: SimpleLeave runs to its end and moves the robot a little. With TeleopV1's directions
     * it strafes, because it disagrees with TeleopV1 on fl and br (see the profile's comment).
     */
    @Test
    void simpleLeaveRunsAndMoves() {
        OpModeRunner runner = new OpModeRunner(new SimpleLeave(), robotFor("SimpleLeave"));
        runner.run(0.1, 2.0).assertNoException();

        Pose end = runner.positionEstimator().pose();
        assertTrue(Math.abs(end.yInches()) > 1, "strafed: " + end);
        assertEquals(0.0, end.xInches(), 0.5, "not forward: " + end);
    }

    /** WHAT: With Unity's physics, the bridge reads this robot's wheels, not Team A's. */
    @Test
    @SuppressWarnings("unchecked")
    void theBridgeReadsThisRobotsWheelsForUnity() {
        BridgeSession session = new BridgeSession();
        String still = "{\"xInches\": 0, \"yInches\": 0, \"headingRadians\": 0, \"forwardInchesPerSecond\": 0,"
                + " \"leftInchesPerSecond\": 0, \"counterClockwiseRadiansPerSecond\": 0, \"wallContact\": \"\"}";
        session.handle("{\"type\": \"hello\", \"version\": " + BridgeProtocol.VERSION + "}");
        session.handle("{\"type\": \"load\", \"opMode\": \"TeleopV1\", \"physics\": \"unity\"}");
        session.handle("{\"type\": \"init\"}");
        session.handle("{\"type\": \"play\", \"physicsState\": " + still + "}");

        // A few steps: the OpMode's own thread reacts to the stick within a loop or so.
        Map<String, Object> frame = null;
        for (int i = 0; i < 3; i++) {
            frame = (Map<String, Object>) JsonReader.parse(session.handle("{\"type\": \"step\", \"gamepad1\": "
                    + STICK_RIGHT + ", \"physicsState\": " + still + "}"));
        }

        Map<String, Object> command = (Map<String, Object>) frame.get("driveCommand");
        assertNotNull(command, String.valueOf(frame));
        assertTrue((Double) command.get("frontLeft") > 0.5, command.toString());
        assertTrue((Double) command.get("rearRight") > 0.5, command.toString());
    }

    /**
     * WHAT: Through the bridge, SimpleLeave (a LinearOpMode that returns) ends the run by itself long
     * before the match does, in STOP, and a further step is refused.
     */
    @Test
    @SuppressWarnings("unchecked")
    void anAutonomousThatReturnsEndsTheBridgeRunEarly() {
        BridgeSession session = new BridgeSession();
        session.handle("{\"type\": \"hello\", \"version\": " + BridgeProtocol.VERSION + "}");
        session.handle("{\"type\": \"load\", \"opMode\": \"SimpleLeave\"}");
        session.handle("{\"type\": \"init\"}");
        session.handle("{\"type\": \"play\"}");

        Map<String, Object> frame = null;
        int steps = 0;
        while (steps < 2000 && (frame == null || frame.get("finished") != Boolean.TRUE)) {
            frame = (Map<String, Object>) JsonReader.parse(session.handle("{\"type\": \"step\"}"));
            steps++;
        }

        assertEquals(Boolean.TRUE, frame.get("finished"), String.valueOf(frame));
        assertEquals("STOP", ((Map<String, Object>) frame.get("loop")).get("stage"));
        assertTrue(steps > 5 && steps < 100, "its 0.25 s drive, not the whole match: " + steps + " steps");
        assertEquals("error", ((Map<String, Object>) JsonReader.parse(session.handle("{\"type\": \"step\"}"))).get("type"));
    }

    /** WHAT: A drive on this robot can be saved as a test, which makes the same robot. */
    @Test
    void aSavedTestMakesTheSameRobot() {
        OpModeRunner runner = new OpModeRunner(new TeleopV1(), robotFor("TeleopV1"));
        RunRecorder recorder = RunRecorder.attachTo(runner);
        runner.run(0.1, 0.2).assertNoException();

        String source = RecordedTestWriter.source(recorder.recording(), new RecordedTestWriter.Options(
                "KimDrive", TeleopV1.class.getName(), "test", "2026-11-03", false));

        assertTrue(source.contains("RobotConfigs.profile(\"kimcode\")"), source);
    }
}
