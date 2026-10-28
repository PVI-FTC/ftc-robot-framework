package pvi.harness.physics;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import pvi.harness.hardware.FakeHardwareMap;
import pvi.harness.runtime.HarnessConfig;
import pvi.harness.physics.MecanumKinematics.RobotVelocity;
import pvi.harness.runtime.OpModeRunner;

/**
 * The simulated robot's position: where the robot would be on the field, worked out every loop
 * from what the wheels were told to do.
 *
 * <p>The motion itself comes from a {@link PhysicsBackend}: by default the harness's own
 * {@link MomentumPhysics} (momentum, coasting, field walls), later possibly Unity. This class
 * reads the drive motors, hands them to the backend one step at a time, and keeps the track,
 * wall hits and statistics for reports and recordings. It also turns the drive-motor encoders
 * with the wheels' real motion, so robot code reading encoders sees what the wheels did.</p>
 *
 * <p>Each loop the runner first calls {@link #advanceTo} (the robot moves, with the wheel
 * powers set during the last loop, up to "now"), then lets sensors read the result, runs the
 * OpMode, and finally calls {@link #update} (records the moment and reads the new wheel
 * powers).</p>
 *
 * <p>It also moves the game pieces ({@link GamePieces}): the robot pushes loose balls out of
 * its way, but is never slowed by them.</p>
 *
 * <p><b>Assumptions:</b> the wheels never slip except at the traction limit, and the robot
 * moves on a flat field with nothing but the walls in its way. See the README.</p>
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

    /**
     * A moment the robot ran into a field wall (the start of each contact; staying against the
     * wall doesn't add more).
     *
     * @param sinceInitSeconds seconds since INIT
     * @param matchTimeSeconds seconds since PLAY (0 before PLAY)
     * @param wall             which wall: "+x", "-x", "+y" or "-y" (two, e.g. "+x/-y", in a corner)
     * @param pose             where the robot was, pressed against the wall
     */
    public record WallHit(double sinceInitSeconds, double matchTimeSeconds, String wall, Pose pose) {
    }

    /**
     * One time the robot ran into a field element (W11), like {@link WallHit}.
     *
     * @param sinceInitSeconds seconds since INIT
     * @param matchTimeSeconds seconds since PLAY (0 before PLAY)
     * @param obstacle         which element: "HIVE frame", "far FLOWER", ... ("/" between two)
     * @param pose             where the robot was, pressed against it
     */
    public record ObstacleHit(double sinceInitSeconds, double matchTimeSeconds, String obstacle, Pose pose) {
    }

    /** The robot's speeds, size and momentum. */
    private final DriveModel model;
    /** Reads which way each wheel pushes, and turns the drive encoders. */
    private final WheelReader wheels;
    /** Where the robot started. */
    private final Pose start;
    /** What moves the robot. */
    private final PhysicsBackend physics;
    /** What the wheels were told at the last update; it acts until the next one. */
    private PhysicsBackend.DriveCommand lastCommand = PhysicsBackend.DriveCommand.STOPPED;
    /** Time the robot has been moved up to, in seconds, or NaN before the first update. */
    private double lastTimeSeconds = Double.NaN;
    /** Total length of the path driven, in inches (driving out and back counts both ways). */
    private double distanceInches;
    /** Fastest speed over the ground seen so far, in inches per second. */
    private double maxSpeedInchesPerSecond;
    /** Seconds during which the robot was moving or turning. */
    private double movingSeconds;
    /** Every pose, one per update, oldest first. */
    private final List<TrackPoint> track = new ArrayList<>();
    /** Which walls the robot was pressed against after the last move ("" if none). */
    private String wallContact = "";
    /** Every time the robot ran into a wall, oldest first. */
    private final List<WallHit> wallHits = new ArrayList<>();
    /** Which field elements the robot was pressed against after the last move ("" if none). */
    private String obstacleContact = "";
    /** Every time the robot ran into a field element, oldest first. */
    private final List<ObstacleHit> obstacleHits = new ArrayList<>();
    /** The game pieces the robot can push (BIOBUZZ's by default). */
    private GamePieces pieces;

    /**
     * An estimate using the harness's own physics ({@link MomentumPhysics}). With
     * {@code field.robotObstacles=true} in {@code harness.properties} (the default), the BIOBUZZ
     * field elements ({@link BioBuzzObstacles}) block the robot as the walls do.
     *
     * @param model  robot speeds, size and momentum, e.g. {@code DriveModels.teamA()}
     * @param wheels how to read the drive motors, e.g. {@code WheelReader.teamA()}
     * @param start  starting pose, e.g. {@code Pose.ORIGIN} (field center facing +x)
     */
    public PoseEstimator(DriveModel model, WheelReader wheels, Pose start) {
        this(model, wheels, start, harnessPhysics(model, start));
    }

    /** The harness's own physics, with the field elements in it if the settings say so. */
    private static MomentumPhysics harnessPhysics(DriveModel model, Pose start) {
        if (model == null || start == null) {
            return null;
        }
        MomentumPhysics physics = new MomentumPhysics(model, start);
        if (HarnessConfig.load().robotObstacles()) {
            physics.setObstacles(BioBuzzObstacles.all());
        }
        return physics;
    }

    /** An estimate whose motion comes from {@code physics} (for example Unity), already at {@code start}. */
    public PoseEstimator(DriveModel model, WheelReader wheels, Pose start, PhysicsBackend physics) {
        if (model == null || wheels == null || start == null || physics == null) {
            throw new IllegalArgumentException("A pose estimator needs a drive model, a wheel reader, a start pose"
                    + " and a physics backend.");
        }
        this.model = model;
        this.wheels = wheels;
        this.start = start;
        this.physics = physics;
        this.pieces = new GamePieces(BioBuzzPieces.start(start, model.lengthInches()), PieceModel.bioBuzz(),
                BioBuzzObstacles.all());
    }

    /**
     * Uses these game pieces instead of the BIOBUZZ ones (for example {@link GamePieces#none()}).
     * Call it before the run starts. Returns this estimator.
     */
    public PoseEstimator withPieces(GamePieces newPieces) {
        if (newPieces == null) {
            throw new IllegalArgumentException("Use GamePieces.none() for a field without game pieces.");
        }
        this.pieces = newPieces;
        return this;
    }

    /** The game pieces on the field: where they started and where they are now. */
    public GamePieces pieces() {
        return pieces;
    }

    /**
     * Lets the robot drive through the field walls (they stop it by default). Only useful for
     * testing a check that the robot stays on the field. Returns this estimator.
     */
    public PoseEstimator withoutWalls() {
        if (!(physics instanceof MomentumPhysics momentum)) {
            throw new IllegalStateException("Only the harness's own physics can turn its walls off.");
        }
        momentum.setWalls(false);
        return this;
    }

    /**
     * Lets the robot drive through the HIVE frame and the FLOWERs (only the walls stop it), as
     * with {@code field.robotObstacles=false}. Returns this estimator.
     */
    public PoseEstimator withoutRobotObstacles() {
        if (physics instanceof MomentumPhysics momentum) {
            momentum.setObstacles(List.of());
        }
        return this;
    }

    /** True if field elements (the HIVE frame, the FLOWERs) block the robot. */
    public boolean hasRobotObstacles() {
        return physics instanceof MomentumPhysics momentum && !momentum.obstacles().isEmpty();
    }

    /** True if the field walls stop the robot (the default). */
    public boolean hasWalls() {
        return !(physics instanceof MomentumPhysics momentum) || momentum.hasWalls();
    }

    /** What moves the robot. */
    public PhysicsBackend physics() {
        return physics;
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
     * Moves the robot forward to {@code timeSeconds} with the wheel powers read at the last
     * {@link #update}, and turns the drive encoders with the wheels' real motion. Calling it
     * again for the same time does nothing.
     *
     * @param hardwareMap      the fake robot (its drive encoders are turned), or null
     * @param timeSeconds      a steadily increasing time, e.g. seconds since INIT
     * @param matchTimeSeconds the time to label a wall hit with (seconds after PLAY)
     */
    public void advanceTo(FakeHardwareMap hardwareMap, double timeSeconds, double matchTimeSeconds) {
        if (Double.isNaN(lastTimeSeconds)) {
            return;   // nothing to move until the wheels have been read once
        }
        double dt = timeSeconds - lastTimeSeconds;
        if (dt < 0) {
            throw new IllegalArgumentException("Time went backwards: from " + lastTimeSeconds
                    + " s to " + timeSeconds + " s.");
        }
        if (dt == 0) {
            return;
        }
        Pose before = physics.state().pose();
        physics.step(lastCommand, dt);
        PhysicsBackend.State now = physics.state();
        lastTimeSeconds = timeSeconds;
        // The robot pushes any loose game pieces in its way.
        pieces.step(before, now.pose(), model.lengthInches(), model.widthInches(), dt, hasWalls());

        // A new wall contact (not just staying against the same wall) is a hit.
        String contactBefore = wallContact;
        wallContact = now.wallContact();
        if (!wallContact.isEmpty() && !wallContact.equals(contactBefore)) {
            wallHits.add(new WallHit(timeSeconds, matchTimeSeconds, wallContact, now.pose()));
        }
        // The same for field elements.
        String obstacleBefore = obstacleContact;
        obstacleContact = now.obstacleContact();
        if (!obstacleContact.isEmpty() && !obstacleContact.equals(obstacleBefore)) {
            obstacleHits.add(new ObstacleHit(timeSeconds, matchTimeSeconds, obstacleContact, now.pose()));
        }
        // Statistics for the report.
        distanceInches += Math.hypot(now.pose().xInches() - before.xInches(), now.pose().yInches() - before.yInches());
        RobotVelocity v = now.velocity();
        double speed = v.speedInchesPerSecond();
        maxSpeedInchesPerSecond = Math.max(maxSpeedInchesPerSecond, speed);
        if (speed > MOVING_THRESHOLD_INCHES_PER_SECOND
                || Math.abs(v.counterClockwiseRadiansPerSecond()) > MOVING_THRESHOLD_INCHES_PER_SECOND) {
            movingSeconds += dt;
        }
        // The drive encoders turn with what the wheels really did.
        if (hardwareMap != null) {
            wheels.turnEncoders(hardwareMap, wheelMotion(v));
        }
    }

    /**
     * Moves the robot forward to {@code timeSeconds} (see {@link #advanceTo}), records the
     * moment in the track, then reads the wheels: those powers drive the robot until the next
     * update.
     *
     * @param hardwareMap      the fake robot to read motors from
     * @param timeSeconds      a steadily increasing time, e.g. seconds since INIT
     * @param matchTimeSeconds the time to label this point with in the track (seconds after PLAY)
     */
    public void update(FakeHardwareMap hardwareMap, double timeSeconds, double matchTimeSeconds) {
        advanceTo(hardwareMap, timeSeconds, matchTimeSeconds);
        // A motor's speed is about proportional to its battery voltage, so a tired battery
        // pushes the wheels less hard than the same power on a fresh one.
        double scale = model.voltageScale(hardwareMap.batteryVoltage().getVoltage());
        WheelPushes pushes = wheels.read(hardwareMap);
        if (scale != 1.0) {
            pushes = new WheelPushes(pushes.frontLeft() * scale, pushes.frontRight() * scale,
                    pushes.rearLeft() * scale, pushes.rearRight() * scale);
        }
        lastCommand = new PhysicsBackend.DriveCommand(pushes, wheels.isBraking(hardwareMap));
        lastTimeSeconds = timeSeconds;
        track.add(new TrackPoint(timeSeconds, matchTimeSeconds, pose()));
    }

    /**
     * How fast each wheel is really turning (a fraction of full speed, forward positive), from
     * the robot's speed: the mecanum mixing in reverse.
     */
    private WheelPushes wheelMotion(RobotVelocity v) {
        return MecanumKinematics.toWheelPushes(new MecanumKinematics.ChassisPower(
                v.forwardInchesPerSecond() / model.forwardInchesPerSecond(),
                -v.leftInchesPerSecond() / model.strafeInchesPerSecond(),
                -v.counterClockwiseRadiansPerSecond() / model.turnRadiansPerSecond()));
    }

    // ---- Results ------------------------------------------------------------------------

    /** Where the robot is now (estimated). */
    public Pose pose() { return physics.state().pose(); }
    /** How the robot is moving now, in its own frame (forward, left, counter-clockwise). */
    public RobotVelocity velocity() { return physics.state().velocity(); }
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
    /** Every time the robot ran into a field wall, oldest first (empty without walls). */
    public List<WallHit> wallHits() { return Collections.unmodifiableList(wallHits); }

    /** Every time the robot ran into a field element (the HIVE frame, a FLOWER), oldest first. */
    public List<ObstacleHit> obstacleHits() { return Collections.unmodifiableList(obstacleHits); }

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
        Pose pose = pose();
        double straightLine = unit.fromInches(start.distanceInchesTo(pose));
        return String.format(Locale.ROOT,
                "Estimated travel (%s model; %s)%n"
                        + "  start:     %s%n"
                        + "  end:       %s%n"
                        + "  moved:     %.2f %s straight-line, %.2f %s along the path, %.2f s moving%n"
                        + "  top speed: %.2f %s/s%n"
                        + "  walls:     %s",
                model.name(), measured, start.toString(unit), pose.toString(unit),
                straightLine, unit.symbol(), unit.fromInches(distanceInches), unit.symbol(), movingSeconds,
                unit.fromInches(maxSpeedInchesPerSecond), unit.symbol(), wallSummary());
    }

    /** "none hit", "hit 2 times (first: -x wall at 3.24 s)", or "off (the robot can leave the field)". */
    private String wallSummary() {
        if (!hasWalls()) {
            return "off (the robot can leave the field)";
        }
        if (wallHits.isEmpty()) {
            return "none hit" + obstacleSummary();
        }
        WallHit first = wallHits.get(0);
        return String.format(Locale.ROOT, "hit %d time%s (first: %s wall at %.2f s)", wallHits.size(),
                wallHits.size() == 1 ? "" : "s", first.wall(), first.matchTimeSeconds()) + obstacleSummary();
    }

    /** "; field elements hit 1 time (first: HIVE frame at 2.10 s)", or "" if none was hit. */
    private String obstacleSummary() {
        if (obstacleHits.isEmpty()) {
            return "";
        }
        ObstacleHit first = obstacleHits.get(0);
        return String.format(Locale.ROOT, "; field elements hit %d time%s (first: %s at %.2f s)", obstacleHits.size(),
                obstacleHits.size() == 1 ? "" : "s", first.obstacle(), first.matchTimeSeconds());
    }

    /** Prints the report to the test output. */
    public void printReport() {
        System.out.println(report());
    }
}
