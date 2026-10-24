package pvi.harness.physics;

import java.util.ArrayList;
import java.util.List;

/**
 * The harness's own physics: a mecanum robot with momentum, on a field with walls.
 *
 * <p>Each step:</p>
 * <ol>
 *   <li>The wheel pushes give a commanded speed (forward, sideways, turning), using the drive
 *       model's full-power speeds ({@link MecanumKinematics}).</li>
 *   <li>The robot's real speed moves toward it: the motors close about 63% of the gap every
 *       {@link Dynamics#responseTime} seconds, but never faster than the traction limit
 *       ({@link Dynamics#maxAcceleration}); turning shares that limit. With zero power on FLOAT
 *       the robot instead coasts and slows at the measured coasting decelerations.</li>
 *   <li>The robot moves by its average speed over the step, turned to the field with the
 *       heading halfway through the step.</li>
 *   <li>The field walls stop the robot box at ±72 in and cancel the speed into the wall, so a
 *       robot driving into a wall at an angle slides along it.</li>
 * </ol>
 *
 * <p>A drive model without {@link Dynamics} (the round-number test robots) changes speed
 * instantly, which keeps the estimator's math tests exact.</p>
 */
public final class MomentumPhysics implements PhysicsBackend {
    private final DriveModel model;
    private Pose pose;
    private MecanumKinematics.RobotVelocity velocity = MecanumKinematics.RobotVelocity.STOPPED;
    private String wallContact = "";
    private boolean walls = true;

    /** A robot standing still at {@code start}. */
    public MomentumPhysics(DriveModel model, Pose start) {
        if (model == null || start == null) {
            throw new IllegalArgumentException("The physics needs a drive model and a start pose.");
        }
        this.model = model;
        this.pose = start;
    }

    /** Turns the field walls on (the default) or off (only for testing an off-field check). */
    public void setWalls(boolean on) {
        this.walls = on;
    }

    /** True if the field walls stop the robot. */
    public boolean hasWalls() {
        return walls;
    }

    @Override
    public State state() {
        return new State(pose, velocity, wallContact);
    }

    @Override
    public void step(DriveCommand command, double seconds) {
        if (seconds <= 0) {
            return;
        }
        MecanumKinematics.RobotVelocity target =
                MecanumKinematics.toVelocity(MecanumKinematics.toChassisPower(command.pushes()), model);
        // Without dynamics the robot is at the commanded speed for the whole step.
        MecanumKinematics.RobotVelocity before = model.dynamics() == null ? target : velocity;
        MecanumKinematics.RobotVelocity after = model.dynamics() == null
                ? target : approach(before, target, command, seconds, model.dynamics());

        // Move by the average speed over the step, with the heading halfway through it.
        double forward = (before.forwardInchesPerSecond() + after.forwardInchesPerSecond()) / 2.0;
        double left = (before.leftInchesPerSecond() + after.leftInchesPerSecond()) / 2.0;
        double turn = (before.counterClockwiseRadiansPerSecond() + after.counterClockwiseRadiansPerSecond()) / 2.0;
        double headingChange = turn * seconds;
        double midHeading = pose.headingRadians() + headingChange / 2.0;
        double cos = Math.cos(midHeading);
        double sin = Math.sin(midHeading);
        // The robot's "forward" points along (cos, sin) and its "left" along (-sin, cos).
        double dx = (forward * cos - left * sin) * seconds;
        double dy = (forward * sin + left * cos) * seconds;
        Pose moved = new Pose(pose.xInches() + dx, pose.yInches() + dy, pose.headingRadians() + headingChange);

        velocity = after;
        wallContact = "";
        pose = walls ? keepInsideWalls(moved) : moved;
    }

    /**
     * The robot's speed after {@code seconds}, moving from {@code now} toward {@code target}
     * as the motors and tires allow.
     */
    private MecanumKinematics.RobotVelocity approach(MecanumKinematics.RobotVelocity now,
                                                     MecanumKinematics.RobotVelocity target,
                                                     DriveCommand command, double seconds, Dynamics d) {
        // Inches of wheel travel per radian of turning: lets turning share the traction limit.
        double lever = model.forwardInchesPerSecond() / model.turnRadiansPerSecond();
        WheelPushes p = command.pushes();
        // (Compared as numbers: a reversed motor at zero power gives -0.0.)
        boolean noPower = p.frontLeft() == 0 && p.frontRight() == 0 && p.rearLeft() == 0 && p.rearRight() == 0;
        boolean coasting = !command.braking() && noPower;
        if (coasting) {
            // No power on FLOAT: friction slows each motion at the measured coasting rate.
            double forwardDecel = d.forwardCoast().value() * seconds;
            double lateralDecel = d.lateralCoast().value() * seconds;
            return new MecanumKinematics.RobotVelocity(
                    towardZero(now.forwardInchesPerSecond(), forwardDecel),
                    towardZero(now.leftInchesPerSecond(), lateralDecel),
                    towardZero(now.counterClockwiseRadiansPerSecond(), forwardDecel / lever));
        }
        // The motors pull the speed toward the command (a first-order response)...
        double share = 1.0 - Math.exp(-seconds / d.responseTime().value());
        double dForward = (target.forwardInchesPerSecond() - now.forwardInchesPerSecond()) * share;
        double dLeft = (target.leftInchesPerSecond() - now.leftInchesPerSecond()) * share;
        double dTurn = (target.counterClockwiseRadiansPerSecond() - now.counterClockwiseRadiansPerSecond()) * share;
        // ...but the tires can only take so much before they slip.
        double change = Math.sqrt(dForward * dForward + dLeft * dLeft + (dTurn * lever) * (dTurn * lever));
        double limit = d.maxAcceleration().value() * seconds;
        if (change > limit) {
            double scale = limit / change;
            dForward *= scale;
            dLeft *= scale;
            dTurn *= scale;
        }
        return new MecanumKinematics.RobotVelocity(
                settle(now.forwardInchesPerSecond() + dForward, target.forwardInchesPerSecond(), STOP_INCHES_PER_SECOND),
                settle(now.leftInchesPerSecond() + dLeft, target.leftInchesPerSecond(), STOP_INCHES_PER_SECOND),
                settle(now.counterClockwiseRadiansPerSecond() + dTurn, target.counterClockwiseRadiansPerSecond(),
                        STOP_INCHES_PER_SECOND / lever));
    }

    /**
     * Below this speed with no power for that motion, static friction holds the robot still.
     * (The motor response alone would only ever get closer to zero, creeping forever.)
     */
    static final double STOP_INCHES_PER_SECOND = 0.25;

    /** 0 if the robot is told not to move this way and is nearly stopped; otherwise {@code speed}. */
    private static double settle(double speed, double target, double stopSpeed) {
        return target == 0 && Math.abs(speed) < stopSpeed ? 0.0 : speed;
    }

    /** {@code value} moved toward 0 by {@code amount}, without passing 0. */
    private static double towardZero(double value, double amount) {
        return Math.abs(value) <= amount ? 0.0 : value - Math.copySign(amount, value);
    }

    /**
     * The field walls: if any part of the robot box would be past a wall, moves the robot back
     * so the box just touches it, and cancels the speed into that wall. Only the blocked
     * direction stops, so a robot driving into a wall at an angle slides along it. Sets
     * {@link #wallContact}.
     *
     * <p>How far the box reaches from its center depends on the heading: a robot turned 45
     * degrees reaches further than one facing straight ahead. The box size comes from the drive
     * model; Team A's length and width are still placeholders (18 × 18 in, the FTC maximum)
     * until they are measured (Step 10), so where the robot stops against a wall is only as
     * exact as those numbers.</p>
     */
    private Pose keepInsideWalls(Pose p) {
        double cos = Math.abs(Math.cos(p.headingRadians()));
        double sin = Math.abs(Math.sin(p.headingRadians()));
        double halfLength = model.lengthInches() / 2.0;
        double halfWidth = model.widthInches() / 2.0;
        double limitX = Pose.FIELD_HALF_WIDTH_INCHES - (cos * halfLength + sin * halfWidth);
        double limitY = Pose.FIELD_HALF_WIDTH_INCHES - (sin * halfLength + cos * halfWidth);

        double x = p.xInches();
        double y = p.yInches();
        List<String> touching = new ArrayList<>();
        boolean blockX = false;
        boolean blockY = false;
        if (limitX > 0 && x > limitX) {
            x = limitX;
            touching.add("+x");
            blockX = true;
        } else if (limitX > 0 && x < -limitX) {
            x = -limitX;
            touching.add("-x");
            blockX = true;
        }
        if (limitY > 0 && y > limitY) {
            y = limitY;
            touching.add("+y");
            blockY = true;
        } else if (limitY > 0 && y < -limitY) {
            y = -limitY;
            touching.add("-y");
            blockY = true;
        }
        wallContact = String.join("/", touching);
        if (touching.isEmpty()) {
            return p;
        }
        // Cancel the field-frame speed into the wall, then turn the rest back to the robot frame.
        double h = p.headingRadians();
        double c = Math.cos(h);
        double s = Math.sin(h);
        double fieldX = velocity.forwardInchesPerSecond() * c - velocity.leftInchesPerSecond() * s;
        double fieldY = velocity.forwardInchesPerSecond() * s + velocity.leftInchesPerSecond() * c;
        if (blockX && Math.signum(fieldX) == Math.signum(x)) {
            fieldX = 0.0;
        }
        if (blockY && Math.signum(fieldY) == Math.signum(y)) {
            fieldY = 0.0;
        }
        velocity = new MecanumKinematics.RobotVelocity(fieldX * c + fieldY * s, -fieldX * s + fieldY * c,
                velocity.counterClockwiseRadiansPerSecond());
        return new Pose(x, y, h);
    }
}
