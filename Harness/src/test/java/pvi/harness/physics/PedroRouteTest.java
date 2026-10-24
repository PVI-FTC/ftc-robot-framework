package pvi.harness.physics;

import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.firstinspires.ftc.teamcode.opmodes.autonomous.TeamAPedroPilotAutoOpMode;
import org.firstinspires.ftc.teamcode.robots.teamA.TeamAPedroPathRoute;
import org.junit.jupiter.api.Test;

import pvi.harness.ds.Button;
import pvi.harness.ds.GamepadScript;
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
 * <p>Each test picks a route in Team A's Pedro path menu ({@code TeamAPedroPilotAutoOpMode}),
 * places the robot at the route's start on the field, runs a 30 s autonomous, and checks what
 * the OpMode itself reports: "Sequence Complete" and the observed end pose against the route's
 * expected end. Start and end poses come from TeamCode's own {@code TeamAPedroPathRoute}, so
 * the test follows the team's changes to a route.</p>
 *
 * <p><b>Tolerance: 1 in and 2°.</b> The simulation gives the same result every run, and today
 * every route ends within 0.25 in and 1.5° (LP-11 0.24 in, DECODE 0.10 in, Curving Test 0.24 in
 * and 1.5°). The margin is for small, intended physics changes, such as replacing placeholder
 * values after calibration (Step 10), not for randomness. Real problems are much bigger: wrong
 * timing, a wrong start or a frame mistake left routes 10-17 in off or unfinished. 1 in is also
 * about what a well-tuned real robot achieves, so a pass means Pedro's tuning behaves sensibly
 * on this robot model.</p>
 */
class PedroRouteTest {
    private static final double POSITION_TOLERANCE_INCHES = 1.0;
    private static final double HEADING_TOLERANCE_DEGREES = 2.0;

    /**
     * The routes in the order TeamAPedroPilotAutoOpMode's menu lists them (DPAD_DOWN moves down
     * one). If the team reorders the menu, the "Path" check in {@link #checkRoute} fails and says so.
     */
    private static final TeamAPedroPathRoute[] MENU = {
            TeamAPedroPathRoute.LP11_PILOT, TeamAPedroPathRoute.DECODE_ROUTE, TeamAPedroPathRoute.CURVING_TEST,
    };

    /**
     * WHAT: LP-11 Pilot, a straight 24 in drive, finishes on its end point.
     *
     * <p>WHY: The simplest route. While sensors lagged a loop behind the robot, Pedro stopped
     * 6 in short and pushed back and forth for the rest of the match.</p>
     */
    @Test
    void lp11PilotEndsOnTarget() {
        checkRoute(TeamAPedroPathRoute.LP11_PILOT);
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
        checkRoute(TeamAPedroPathRoute.DECODE_ROUTE);
    }

    /**
     * WHAT: The Curving Test, Bezier curves with the heading following the path, finishes on
     * its end point.
     *
     * <p>WHY: Curves exercise Pedro's turning and centripetal correction, which lines don't.</p>
     */
    @Test
    void curvingTestEndsOnTarget() {
        checkRoute(TeamAPedroPathRoute.CURVING_TEST);
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

    /** Runs a route with Team A's model and checks it finished within tolerance. */
    private static void checkRoute(TeamAPedroPathRoute route) {
        String name = route.getDisplayName();
        RunResult result = runRoute(route, DriveModels.teamA());
        result.assertNoException();
        assertEquals(name, result.telemetry().lastValue("Path"));
        assertEquals("true", result.telemetry().lastValue("Sequence Complete"), name + " should finish within 30 s");
        double[] expected = poseOrNull(result.telemetry().lastValue("Expected End"));
        double[] end = poseOrNull(result.telemetry().lastValue("Observed End"));
        assertNotNull(end, name + ": no observed end pose");
        double positionError = Math.hypot(end[0] - expected[0], end[1] - expected[1]);
        // TeamCode's own "End Heading Error" doesn't wrap (it can say -358.5 for 1.5), so wrap here.
        double headingError = Math.IEEEremainder(end[2] - expected[2], 360.0);
        System.out.printf(Locale.ROOT, "%s ended %.3f in and %.2f deg from its target%n", name, positionError, headingError);
        assertTrue(positionError <= POSITION_TOLERANCE_INCHES, name + " ended " + positionError + " in off");
        assertTrue(Math.abs(headingError) <= HEADING_TOLERANCE_DEGREES, name + " ended " + headingError + " deg off");
    }

    /** Picks the route in the menu during INIT, places the robot at its start, and runs a 30 s autonomous. */
    static RunResult runRoute(TeamAPedroPathRoute route, DriveModel model) {
        return runRoute(route, model, RobotConfigs.teamA());
    }

    /** Like {@link #runRoute(TeamAPedroPathRoute, DriveModel)}, on a chosen fake robot (for example a miswired one). */
    static RunResult runRoute(TeamAPedroPathRoute route, DriveModel model, pvi.harness.hardware.FakeHardwareMap robot) {
        int menuIndex = Arrays.asList(MENU).indexOf(route);
        if (menuIndex < 0) {
            throw new IllegalArgumentException(route + " is not in MENU; add it in the menu's order.");
        }
        GamepadScript driver = new GamepadScript().duringInit().at(0.2);
        if (menuIndex > 0) {
            driver.tap(Button.DPAD_DOWN, menuIndex);
        }
        driver.tap(Button.RIGHT_BUMPER);
        OpModeRunner runner = new OpModeRunner(new TeamAPedroPilotAutoOpMode(), robot).gamepad1(driver);
        PoseEstimator.attachTo(runner, model, WheelReader.teamA(), startOf(route, model));
        return runner.run(1.0, 30.0);
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

    /** "(48.061, 81.073, 135.031 deg)" as {x, y, degrees}, or null if not a pose (e.g. "unavailable"). */
    private static double[] poseOrNull(String text) {
        Matcher m = Pattern.compile("\\(([-\\d.]+), ([-\\d.]+), ([-\\d.]+) deg\\)").matcher(String.valueOf(text));
        if (!m.find()) {
            return null;
        }
        return new double[] {Double.parseDouble(m.group(1)), Double.parseDouble(m.group(2)), Double.parseDouble(m.group(3))};
    }
}
