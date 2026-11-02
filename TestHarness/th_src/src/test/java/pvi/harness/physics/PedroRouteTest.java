package pvi.harness.physics;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.firstinspires.ftc.teamcode.opmodes.autonomous.TeamAPedroPilotAutoOpMode;
import org.firstinspires.ftc.teamcode.robots.teamA.TeamAPedroPathRoute;
import org.junit.jupiter.api.Test;

import pvi.harness.ds.Button;
import pvi.harness.ds.GamepadScript;
import pvi.harness.ds.TelemetryFrame;
import pvi.harness.hardware.RobotConfigs;
import pvi.harness.runtime.OpModeRunner;
import pvi.harness.runtime.RunResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 5 (Step 17): Pedro follows Team A's real routes on the simulated robot, with momentum,
 * field walls, and the fake Pinpoint reading the robot's motion.
 *
 * <p>Each test picks a route with the D-pad during INIT in {@code TeamAPedroPilotAutoOpMode}
 * (Pedro 3), places the robot at the route's start on the field, runs a 30 s autonomous, and
 * checks what the OpMode itself reports: "Selected Route", "Sequence Complete" and the observed
 * end pose. Start and end poses come from TeamCode's own {@code TeamAPedroPathRoute}, so the
 * test follows the team's changes to a route.</p>
 *
 * <p><b>Field elements (manager decision D12, 2026-10-27).</b> Since the simulated robot is
 * blocked by the BIOBUZZ HIVE frame and FLOWERs ({@code field.robotObstacles=true}), the DECODE,
 * Curving Test and Illegal Path routes can't finish: they are DECODE-season paths that cross the
 * middle of the field, where the BIOBUZZ HIVE now stands. Their tests drive with the robot
 * obstacles off (walls still on), so they keep checking Pedro's following;
 * {@link #decodeSeasonRoutesHitTheBiobuzzHive} pins that they do hit the HIVE. LP-11 runs with
 * everything on.</p>
 *
 * <p>Each route test also prints how long the route took, from PLAY until the OpMode first
 * shows "Sequence Complete", for comparing with the real robot (CALIBRATION.md). The time is not
 * checked: it will change when the robot's speeds are measured.</p>
 *
 * <p><b>Tolerance: 1 in and 2 deg.</b> The simulation gives the same result every run, and on
 * Pedro 3 every route ends within 0.2 in and 0.05 deg (LP-11 0.155 in, DECODE 0.192 in, Curving
 * Test 0.157 in; on Pedro 2 they were 0.24, 0.10 and 0.24 in). The margin is for small, intended
 * physics changes, such as replacing placeholder values after calibration (Step 10), not for
 * randomness. Real problems are much bigger: wrong
 * timing, a wrong start or a frame mistake left routes 10-17 in off or unfinished. 1 in is also
 * about what a well-tuned real robot achieves, so a pass means Pedro's tuning behaves sensibly
 * on this robot model.</p>
 */
class PedroRouteTest {
    private static final double POSITION_TOLERANCE_INCHES = 1.0;
    private static final double HEADING_TOLERANCE_DEGREES = 2.0;

    /**
     * The routes in the order TeamAPedroPilotAutoOpMode offers them: it starts on the first and
     * DPAD_DOWN moves down one. Pedro 3's OpMode uses the enum's own order, so this does too. If
     * that ever changes, the "Selected Route" check in {@link #checkRoute} fails and says so.
     */
    private static final TeamAPedroPathRoute[] MENU = TeamAPedroPathRoute.values();

    /**
     * WHAT: LP-11 Pilot, a straight 24 in drive, finishes on its end point.
     *
     * <p>WHY: The simplest route. While sensors lagged a loop behind the robot, Pedro stopped
     * 6 in short and pushed back and forth for the rest of the match.</p>
     */
    @Test
    void lp11PilotEndsOnTarget() {
        checkRoute(TeamAPedroPathRoute.LP11_PILOT, true);
    }

    /**
     * WHAT: The DECODE Route, an 11-segment tour of the field with heading changes, finishes on
     * its end point.
     *
     * <p>WHY: It drives close to the walls and turns on most segments. Started in the wrong
     * place, the robot hit a wall that wasn't really there and Pedro gave up 17 in from the end.</p>
     */
    @Test
    void decodeRouteEndsOnTarget() {
        checkRoute(TeamAPedroPathRoute.DECODE_ROUTE, false);   // DECODE-season path: crosses the BIOBUZZ HIVE (D12)
    }

    /**
     * WHAT: The Curving Test, Bezier curves with the heading following the path, finishes on
     * its end point.
     *
     * <p>WHY: Curves exercise Pedro's turning and centripetal correction, which lines don't.</p>
     */
    @Test
    void curvingTestEndsOnTarget() {
        checkRoute(TeamAPedroPathRoute.CURVING_TEST, false);   // DECODE-season path: crosses the BIOBUZZ HIVE (D12)
    }

    /**
     * WHAT: The "Illegal Path", the fourth route in the chooser, is driven like the others and
     * finishes on its end point.
     *
     * <p>WHY: Its name sounds like a route the robot code should refuse, but TeamCode builds and
     * follows it as a normal Pedro path (three segments, two wide curves). This checks it keeps
     * doing exactly that, and that its curves stay inside the field walls. ("Illegal" presumably
     * means it breaks a game rule, which the harness doesn't model.)</p>
     */
    @Test
    void illegalPathIsFollowedLikeAnyOtherRoute() {
        checkRoute(TeamAPedroPathRoute.ILLEGAL_PATH, false);   // DECODE-season path: crosses the BIOBUZZ HIVE (D12)
    }

    /**
     * WHAT: With the BIOBUZZ field's HIVE frame and FLOWERs blocking the robot, the three
     * DECODE-season routes run into the HIVE.
     *
     * <p>WHY: Pins the reason their tests above drive without robot obstacles (D12). If the team
     * redraws these routes for BIOBUZZ, this fails: then turn the obstacles back on above and
     * delete this test.</p>
     */
    @Test
    void decodeSeasonRoutesHitTheBiobuzzHive() {
        for (TeamAPedroPathRoute route : new TeamAPedroPathRoute[] {
                TeamAPedroPathRoute.DECODE_ROUTE, TeamAPedroPathRoute.CURVING_TEST, TeamAPedroPathRoute.ILLEGAL_PATH}) {
            OpModeRunner runner = routeRunner(route, DriveModels.teamA(), RobotConfigs.teamA(), true);
            runner.run(1.0, 30.0);
            List<PoseEstimator.ObstacleHit> hits = runner.positionEstimator().obstacleHits();
            assertTrue(!hits.isEmpty() && hits.get(0).obstacle().equals("HIVE frame"),
                    route.getDisplayName() + " no longer runs into the HIVE frame (hits: " + hits + "). If the"
                            + " route was redrawn for BIOBUZZ, switch the robot obstacles back on in its test above"
                            + " and take it out of this list.");
        }
    }

    /**
     * WHAT: The route check can fail: with the front-left motor's wires swapped, the robot
     * misses the DECODE end (or never finishes).
     *
     * <p>WHY: Proves the check catches a broken robot. (Pedro's feedback is strong: a robot
     * twice as fast as its tuning still lands on target, so a wrong speed alone isn't enough.)</p>
     */
    @Test
    void aMiswiredRobotMissesTheRoute() {
        TeamAPedroPathRoute route = TeamAPedroPathRoute.DECODE_ROUTE;
        RunResult result = runRoute(route, DriveModels.teamA(), RobotConfigs.teamA().miswire("frontLeft"));
        double[] end = poseOrNull(result.telemetry().lastValue("Observed End"));
        boolean missed = end == null
                || Math.hypot(end[0] - route.getEndXInches(), end[1] - route.getEndYInches()) > POSITION_TOLERANCE_INCHES
                || Math.abs(Math.IEEEremainder(end[2] - Math.toDegrees(route.getEndHeadingRadians()), 360.0))
                        > HEADING_TOLERANCE_DEGREES;
        assertTrue(missed, "a miswired robot should not land within tolerance: "
                + result.telemetry().lastValue("Observed End"));
    }

    // ---- Helpers ----------------------------------------------------------------------------

    /**
     * Runs a route with Team A's model and checks it finished within tolerance.
     *
     * @param robotObstacles false to let the robot through the HIVE frame and FLOWERs (walls stay)
     */
    private static void checkRoute(TeamAPedroPathRoute route, boolean robotObstacles) {
        String name = route.getDisplayName();
        RunResult result = routeRunner(route, DriveModels.teamA(), RobotConfigs.teamA(), robotObstacles).run(1.0, 30.0);
        result.assertNoException();
        assertEquals(name, result.telemetry().lastValue("Selected Route"));
        assertEquals("true", result.telemetry().lastValue("Sequence Complete"), name + " should finish within 30 s");
        // Pedro 3's OpMode no longer shows the expected end, so take it from the route itself.
        double[] expected = {route.getEndXInches(), route.getEndYInches(), Math.toDegrees(route.getEndHeadingRadians())};
        double[] end = poseOrNull(result.telemetry().lastValue("Observed End"));
        assertNotNull(end, name + ": no observed end pose");
        double positionError = Math.hypot(end[0] - expected[0], end[1] - expected[1]);
        // Wrap the heading difference, so 359.5 deg against 0 deg counts as 0.5 deg off.
        double headingError = Math.IEEEremainder(end[2] - expected[2], 360.0);
        // Timing is printed, not checked: it will change once the robot's speeds are measured
        // (CALIBRATION.md). Compare it with the real robot's time for the same route.
        Double seconds = secondsToComplete(result);
        assertNotNull(seconds, name + ": no Driver Station screen showed Sequence Complete = true");
        System.out.printf(Locale.ROOT, "%s finished %.2f s after PLAY, ended %.3f in and %.2f deg from its target%n",
                name, seconds, positionError, headingError);
        assertTrue(positionError <= POSITION_TOLERANCE_INCHES, name + " ended " + positionError + " in off");
        assertTrue(Math.abs(headingError) <= HEADING_TOLERANCE_DEGREES, name + " ended " + headingError + " deg off");
    }

    /** Picks the route with the D-pad during INIT, places the robot at its start, and runs a 30 s autonomous. */
    static RunResult runRoute(TeamAPedroPathRoute route, DriveModel model) {
        return runRoute(route, model, RobotConfigs.teamA());
    }

    /** Like {@link #runRoute(TeamAPedroPathRoute, DriveModel)}, on a chosen fake robot (for example a miswired one). */
    static RunResult runRoute(TeamAPedroPathRoute route, DriveModel model, pvi.harness.hardware.FakeHardwareMap robot) {
        return routeRunner(route, model, robot, true).run(1.0, 30.0);
    }

    /**
     * A runner ready to drive a route (picked during INIT, robot at the route's start), with the
     * robot blocked by the field's elements or not.
     */
    static OpModeRunner routeRunner(TeamAPedroPathRoute route, DriveModel model,
                                    pvi.harness.hardware.FakeHardwareMap robot, boolean robotObstacles) {
        int menuIndex = Arrays.asList(MENU).indexOf(route);
        if (menuIndex < 0) {
            throw new IllegalArgumentException(route + " is not in MENU; add it in the menu's order.");
        }
        GamepadScript driver = new GamepadScript().duringInit().at(0.2);
        if (menuIndex > 0) {
            driver.tap(Button.DPAD_DOWN, menuIndex);
        }
        OpModeRunner runner = new OpModeRunner(new TeamAPedroPilotAutoOpMode(), robot).gamepad1(driver);
        PoseEstimator estimator = PoseEstimator.attachTo(runner, model, WheelReader.teamA(), startOf(route, model));
        if (!robotObstacles) {
            estimator.withoutRobotObstacles();
        }
        return runner;
    }

    /**
     * Where the robot starts on the field for a route, so the walls are where Pedro thinks
     * they are. LP-11 starts at Pedro's (0, 0), a field corner; it is a relative test drive, so
     * it starts at the field center instead.
     */
    static Pose startOf(TeamAPedroPathRoute route, DriveModel model) {
        Pose start = route == TeamAPedroPathRoute.LP11_PILOT ? Pose.ORIGIN
                : Pose.fromPedro(route.getStartXInches(), route.getStartYInches(),
                        Math.toDegrees(route.getStartHeadingRadians()));
        // Pedro's start is 8 in from the wall; the placeholder 18 in robot would poke 1 in through
        // it, so nudge it inside (the real Team A robot must be at most 16 in long).
        double limit = Pose.FIELD_HALF_WIDTH_INCHES - Math.max(model.lengthInches(), model.widthInches()) / 2.0;
        return new Pose(Math.max(-limit, Math.min(limit, start.xInches())),
                Math.max(-limit, Math.min(limit, start.yInches())), start.headingRadians());
    }

    /**
     * Seconds from PLAY until the OpMode first showed "Sequence Complete" as true, or null if it
     * never did. Read from the Driver Station screens the run sent.
     */
    static Double secondsToComplete(RunResult result) {
        for (TelemetryFrame frame : result.telemetry().frames()) {
            if ("true".equals(frame.value("Sequence Complete"))) {
                return frame.matchTimeSeconds();
            }
        }
        return null;
    }

    /** "(48.061, 81.073, 135.031 deg)" as {x, y, degrees}, or null if not a pose (e.g. "unavailable"). */
    private static double[] poseOrNull(String text) {
        Matcher m = Pattern.compile("\\(([-\\d.]+), ([-\\d.]+), ([-\\d.]+) deg\\)").matcher(String.valueOf(text));
        if (!m.find()) {
            return null;
        }
        return new double[] {Double.parseDouble(m.group(1)), Double.parseDouble(m.group(2)), Double.parseDouble(m.group(3))};
    }
}
