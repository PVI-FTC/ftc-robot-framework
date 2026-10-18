package pvi.harness.physics;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import pvi.harness.hardware.FakeHardwareMap;
import pvi.harness.physics.MecanumKinematics.RobotVelocity;
import pvi.harness.runtime.OpModeRunner;

/**
 * Estimates where the robot would be on the field, by adding up small movements every loop
 * ("dead reckoning").
 *
 * <p>Each update:</p>
 * <ol>
 *   <li>takes the wheel pushes read at the <b>previous</b> update (those powers were driving
 *       the robot for the whole time since then),</li>
 *   <li>turns them into robot speeds (Step 4 math and the drive model),</li>
 *   <li>turns robot-relative speeds into field directions using the heading, multiplies by
 *       the time since the last update, and adds the small step to the position,</li>
 *   <li>reads the wheels again, ready for the next update.</li>
 * </ol>
 *
 * <p><b>Assumptions:</b> speed is proportional to power, changes instantly, and the wheels
 * never slip. Short, quick moves will read a little long on a real robot. See the README.</p>
 *
 * <p>Easiest use: {@code PoseEstimator estimator = PoseEstimator.attachTo(runner, DriveModels.teamA());}
 * then run the OpMode and call {@code estimator.printReport()}.</p>
 */
public final class PoseEstimator {
    /** Speeds smaller than this (in/s) count as "not moving", ignoring tiny rounding leftovers. */
    private static final double MOVING_THRESHOLD_INCHES_PER_SECOND = 1e-6;

    /**
     * One entry in the history of where the robot was.
     *
     * @param sinceInitSeconds seconds since INIT; keeps counting up during INIT, unlike match time
     * @param matchTimeSeconds seconds since PLAY (0 before PLAY)
     * @param pose             where the robot was at that moment
     */
    public record TrackPoint(double sinceInitSeconds, double matchTimeSeconds, Pose pose) {
    }

    /** The robot's speeds and size. */
    private final DriveModel model;
    /** Reads which way each wheel pushes. */
    private final WheelReader wheels;
    /** Where the robot started. */
    private final Pose start;
    /** Where the robot is now (estimated). */
    private Pose pose;
    /** Wheel pushes read at the last update; they act until the next update. */
    private WheelPushes lastPushes = WheelPushes.STOPPED;
    /** Time of the last update in seconds, or NaN before the first update. */
    private double lastTimeSeconds = Double.NaN;
    /** Total length of the path driven, in inches (driving out and back counts both ways). */
    private double distanceInches;
    /** Fastest speed over the ground seen so far, in inches per second. */
    private double maxSpeedInchesPerSecond;
    /** Seconds during which the robot was moving or turning. */
    private double movingSeconds;
    /** Every pose, one per update, oldest first. */
    private final List<TrackPoint> track = new ArrayList<>();

    /**
     * @param model  robot speeds, e.g. {@code DriveModels.teamA()}
     * @param wheels how to read the drive motors, e.g. {@code WheelReader.teamA()}
     * @param start  starting pose, e.g. {@code Pose.ORIGIN} (field center facing +x)
     */
    public PoseEstimator(DriveModel model, WheelReader wheels, Pose start) {
        if (model == null || wheels == null || start == null) {
            throw new IllegalArgumentException("A pose estimator needs a drive model, a wheel reader and a start pose.");
        }
        this.model = model;
        this.wheels = wheels;
        this.start = start;
        this.pose = start;
    }

    // ---- Step 6: plugging into a runner -------------------------------------------------

    /**
     * Creates an estimator for Team A's wheels starting at the field center, and makes it the
     * runner's position estimate (the runner updates it after every OpMode call).
     *
     * <p>Every runner already tracks position automatically with Team A's model from the field
     * center; use this only to choose a different model or start pose.</p>
     */
    public static PoseEstimator attachTo(OpModeRunner runner, DriveModel model) {
        return attachTo(runner, model, WheelReader.teamA(), Pose.ORIGIN);
    }

    /** Like {@link #attachTo(OpModeRunner, DriveModel)}, with a chosen wheel reader and start pose. */
    public static PoseEstimator attachTo(OpModeRunner runner, DriveModel model, WheelReader wheels, Pose start) {
        PoseEstimator estimator = new PoseEstimator(model, wheels, start);
        // Replaces the automatic estimate, so the final-position report uses this one.
        runner.usePositionEstimator(estimator);
        return estimator;
    }

    // ---- Step 5: adding up movement -------------------------------------------------------

    /**
     * Moves the estimate forward to {@code timeSeconds}, then reads the wheels for next time.
     *
     * @param hardwareMap      the fake robot to read motors from
     * @param timeSeconds      a steadily increasing time, e.g. seconds since INIT
     * @param matchTimeSeconds the time to label this point with in the track (seconds after PLAY)
     */
    public void update(FakeHardwareMap hardwareMap, double timeSeconds, double matchTimeSeconds) {
        if (!Double.isNaN(lastTimeSeconds)) {
            double dt = timeSeconds - lastTimeSeconds;
            if (dt < 0) {
                throw new IllegalArgumentException("Time went backwards: from " + lastTimeSeconds
                        + " s to " + timeSeconds + " s.");
            }
            move(lastPushes, dt);
        }
        // These pushes drive the robot from now until the next update.
        lastPushes = wheels.read(hardwareMap);
        lastTimeSeconds = timeSeconds;
        track.add(new TrackPoint(timeSeconds, matchTimeSeconds, pose));
    }

    /** Adds the movement caused by {@code pushes} acting for {@code dt} seconds. */
    private void move(WheelPushes pushes, double dt) {
        RobotVelocity v = MecanumKinematics.toVelocity(MecanumKinematics.toChassisPower(pushes), model);
        double headingChange = v.counterClockwiseRadiansPerSecond() * dt;

        // Use the heading halfway through the step: more accurate when driving while turning.
        double midHeading = pose.headingRadians() + headingChange / 2.0;
        double cos = Math.cos(midHeading);
        double sin = Math.sin(midHeading);

        // Turn robot-relative motion into field directions. The robot's "forward" points
        // along (cos, sin) and its "left" along (-sin, cos).
        double dx = (v.forwardInchesPerSecond() * cos - v.leftInchesPerSecond() * sin) * dt;
        double dy = (v.forwardInchesPerSecond() * sin + v.leftInchesPerSecond() * cos) * dt;

        pose = new Pose(pose.xInches() + dx, pose.yInches() + dy, pose.headingRadians() + headingChange);

        // Statistics for the report.
        distanceInches += Math.hypot(dx, dy);
        double speed = v.speedInchesPerSecond();
        maxSpeedInchesPerSecond = Math.max(maxSpeedInchesPerSecond, speed);
        if (speed > MOVING_THRESHOLD_INCHES_PER_SECOND
                || Math.abs(v.counterClockwiseRadiansPerSecond()) > MOVING_THRESHOLD_INCHES_PER_SECOND) {
            movingSeconds += dt;
        }
    }

    // ---- Results ------------------------------------------------------------------------

    /** Where the robot is now (estimated). */
    public Pose pose() { return pose; }
    /** Where the robot started. */
    public Pose start() { return start; }
    /** The drive model used for the estimate. */
    public DriveModel model() { return model; }
    /** Total path length driven, in inches. */
    public double distanceTraveledInches() { return distanceInches; }
    /** Total path length driven, in feet. */
    public double distanceTraveledFeet() { return DisplayUnit.FEET.fromInches(distanceInches); }
    /** Fastest speed over the ground, in inches per second. */
    public double maxSpeedInchesPerSecond() { return maxSpeedInchesPerSecond; }
    /** Seconds spent moving or turning. */
    public double movingSeconds() { return movingSeconds; }
    /** Every pose, one per update, oldest first. */
    public List<TrackPoint> track() { return Collections.unmodifiableList(track); }

    // ---- Step 8: saving the track -------------------------------------------------------

    /**
     * Saves the track as {@code build/tracks/<name>.csv} (inside {@code Harness/} when tests run
     * through Gradle), for graphing in a spreadsheet. See {@link TrackCsv} for the columns.
     *
     * @param name file name without ".csv": letters, digits, '-', '_' and '.' only
     * @return the full path of the saved file
     */
    public Path saveTrackCsv(String name) {
        return TrackCsv.save(track, TrackCsv.fileInDefaultFolder(name));
    }

    // ---- Step 6: the printed report -------------------------------------------------------

    /** A short text report in feet (see {@link #report(DisplayUnit)}). */
    public String report() {
        return report(DisplayUnit.FEET);
    }

    /**
     * A short text report: start and end pose, how far and how fast, and which drive model
     * values are still placeholders.
     */
    public String report(DisplayUnit unit) {
        String measured = model.isFullyMeasured()
                ? "all values measured"
                : String.join(", ", model.unmeasured()) + " TO MEASURE";
        double straightLine = unit.fromInches(start.distanceInchesTo(pose));
        return String.format(Locale.ROOT,
                "Estimated travel (%s model; %s)%n"
                        + "  start:     %s%n"
                        + "  end:       %s%n"
                        + "  moved:     %.2f %s straight-line, %.2f %s along the path, %.2f s moving%n"
                        + "  top speed: %.2f %s/s",
                model.name(), measured, start.toString(unit), pose.toString(unit),
                straightLine, unit.symbol(), unit.fromInches(distanceInches), unit.symbol(), movingSeconds,
                unit.fromInches(maxSpeedInchesPerSecond), unit.symbol());
    }

    /** Prints the report to the test output. */
    public void printReport() {
        System.out.println(report());
    }
}
