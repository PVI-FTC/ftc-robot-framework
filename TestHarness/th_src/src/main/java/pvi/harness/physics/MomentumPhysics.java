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
 *   <li>Field elements the robot can hit ({@link #setObstacles}, for BIOBUZZ the HIVE frame's
 *       feet and lips and the FLOWERs) stop the robot box the same way: it is pushed back out,
 *       loses its speed into the element and slides along it.</li>
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
    /** Field elements that block the robot box (none unless {@link #setObstacles} is called). */
    private List<PieceObstacle> obstacles = List.of();
    private String obstacleContact = "";

    /**
     * The robot box moves at most this far (inches, at any corner) between two obstacle checks,
     * so it can't jump through a thin part such as the HIVE frame's 0.2 in lip.
     */
    static final double MAX_MOVE_PER_OBSTACLE_CHECK_INCHES = 0.1;

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

    /**
     * The field elements that block the robot box, for example {@link BioBuzzObstacles#all()}.
     * The robot is a box from the tiles up, so every obstacle counts whatever its height (a
     * FLOWER's raised head too: no robot fits under 3.75 in). Empty: only the walls block it.
     */
    public void setObstacles(List<PieceObstacle> obstacles) {
        this.obstacles = List.copyOf(obstacles);
    }

    /** The field elements that block the robot box (empty if none). */
    public List<PieceObstacle> obstacles() {
        return obstacles;
    }

    @Override
    public State state() {
        return new State(pose, velocity, wallContact, obstacleContact);
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
        obstacleContact = "";
        Pose clear = obstacles.isEmpty() ? moved : avoidObstacles(pose, moved);
        pose = walls ? keepInsideWalls(clear) : clear;
    }

    /**
     * Moves the robot box from {@code from} toward {@code to} in small steps; whenever it would
     * overlap an obstacle, pushes it back out the shortest way, takes away the speed (and the rest
     * of this step's motion) into the obstacle and carries on along it, as at a wall. Returns
     * {@code to} unchanged when nothing is hit. Sets {@link #obstacleContact}.
     */
    private Pose avoidObstacles(Pose from, Pose to) {
        double halfLength = model.lengthInches() / 2.0;
        double halfWidth = model.widthInches() / 2.0;
        double reach = Math.hypot(halfLength, halfWidth);
        double dx = to.xInches() - from.xInches();
        double dy = to.yInches() - from.yInches();
        double dh = to.headingRadians() - from.headingRadians();
        double travel = Math.hypot(dx, dy) + Math.abs(dh) * reach;
        // Only obstacles the box could reach during this step matter.
        List<PieceObstacle> near = new ArrayList<>();
        for (PieceObstacle o : obstacles) {
            if (distanceToCenter(o, from) <= reach + travel + obstacleRadius(o)) {
                near.add(o);
            }
        }
        if (near.isEmpty()) {
            return to;
        }
        int steps = (int) Math.max(1, Math.ceil(travel / MAX_MOVE_PER_OBSTACLE_CHECK_INCHES));
        double stepX = dx / steps;
        double stepY = dy / steps;
        double stepH = dh / steps;
        double x = from.xInches();
        double y = from.yInches();
        double h = from.headingRadians();
        List<String> touched = new ArrayList<>();
        for (int k = 0; k < steps; k++) {
            x += stepX;
            y += stepY;
            h += stepH;
            for (PieceObstacle o : near) {
                double[] push = penetration(o, x, y, h, halfLength, halfWidth);
                if (push == null) {
                    continue;
                }
                double nx = push[0];
                double ny = push[1];
                x += nx * push[2];
                y += ny * push[2];
                // No more motion into the obstacle this step...
                double into = stepX * nx + stepY * ny;
                if (into < 0) {
                    stepX -= into * nx;
                    stepY -= into * ny;
                }
                // ...and no speed into it afterwards.
                cancelSpeedInto(nx, ny, h);
                String name = displayName(o);
                if (!touched.contains(name)) {
                    touched.add(name);
                }
            }
        }
        if (touched.isEmpty()) {
            return to;
        }
        obstacleContact = String.join("/", touched);
        return new Pose(x, y, h);
    }

    /**
     * How the robot box at (x, y, heading h) overlaps {@code o}: {normalX, normalY, depth}, where
     * the unit normal points from the obstacle toward the robot and depth is how far to move the
     * robot along it; or null if they don't overlap. Boxes use the separating-axis test (the
     * field's x and y axes and the robot's own two); cylinders use the box's nearest point.
     */
    private static double[] penetration(PieceObstacle o, double x, double y, double h,
                                        double halfLength, double halfWidth) {
        double c = Math.cos(h);
        double s = Math.sin(h);
        if (o.cylinder()) {
            double cx = o.minXInches();
            double cy = o.minYInches();
            double radius = o.maxXInches();
            // The circle center in the robot's frame, and the box's nearest point to it.
            double forward = (cx - x) * c + (cy - y) * s;
            double left = -(cx - x) * s + (cy - y) * c;
            double nearForward = Math.max(-halfLength, Math.min(halfLength, forward));
            double nearLeft = Math.max(-halfWidth, Math.min(halfWidth, left));
            if (nearForward != forward || nearLeft != left) {
                double gapF = forward - nearForward;
                double gapL = left - nearLeft;
                double gap = Math.hypot(gapF, gapL);
                if (gap >= radius) {
                    return null;
                }
                // From the circle toward the box's nearest point, in the field frame.
                double nF = -gapF / gap;
                double nL = -gapL / gap;
                return new double[] {nF * c - nL * s, nF * s + nL * c, radius - gap};
            }
            // The circle's center is inside the box: leave along the robot's nearest side.
            double[] gaps = {halfLength - forward, forward + halfLength, halfWidth - left, left + halfWidth};
            int side = 0;
            for (int i = 1; i < 4; i++) {
                if (gaps[i] < gaps[side]) {
                    side = i;
                }
            }
            double nF = side == 0 ? -1 : side == 1 ? 1 : 0;
            double nL = side == 2 ? -1 : side == 3 ? 1 : 0;
            return new double[] {nF * c - nL * s, nF * s + nL * c, gaps[side] + radius};
        }
        double boxX = (o.minXInches() + o.maxXInches()) / 2.0;
        double boxY = (o.minYInches() + o.maxYInches()) / 2.0;
        double boxHalfX = (o.maxXInches() - o.minXInches()) / 2.0;
        double boxHalfY = (o.maxYInches() - o.minYInches()) / 2.0;
        double[][] axes = {{1, 0}, {0, 1}, {c, s}, {-s, c}};
        double best = Double.POSITIVE_INFINITY;
        double[] result = null;
        for (double[] a : axes) {
            double robotRadius = halfLength * Math.abs(c * a[0] + s * a[1]) + halfWidth * Math.abs(-s * a[0] + c * a[1]);
            double boxRadius = boxHalfX * Math.abs(a[0]) + boxHalfY * Math.abs(a[1]);
            double d = (x - boxX) * a[0] + (y - boxY) * a[1];
            double overlap = robotRadius + boxRadius - Math.abs(d);
            if (overlap <= 0) {
                return null; // a gap along this axis: no overlap
            }
            if (overlap < best) {
                best = overlap;
                double sign = d >= 0 ? 1 : -1;
                result = new double[] {a[0] * sign, a[1] * sign, overlap};
            }
        }
        return result;
    }

    /** Takes away the robot's speed toward an obstacle whose surface normal is (nx, ny). */
    private void cancelSpeedInto(double nx, double ny, double h) {
        double c = Math.cos(h);
        double s = Math.sin(h);
        double fieldX = velocity.forwardInchesPerSecond() * c - velocity.leftInchesPerSecond() * s;
        double fieldY = velocity.forwardInchesPerSecond() * s + velocity.leftInchesPerSecond() * c;
        double into = fieldX * nx + fieldY * ny;
        if (into < 0) {
            fieldX -= into * nx;
            fieldY -= into * ny;
            velocity = new MecanumKinematics.RobotVelocity(fieldX * c + fieldY * s, -fieldX * s + fieldY * c,
                    velocity.counterClockwiseRadiansPerSecond());
        }
    }

    /** Distance from the robot center to the obstacle's center. */
    private static double distanceToCenter(PieceObstacle o, Pose p) {
        double cx = o.cylinder() ? o.minXInches() : (o.minXInches() + o.maxXInches()) / 2.0;
        double cy = o.cylinder() ? o.minYInches() : (o.minYInches() + o.maxYInches()) / 2.0;
        return Math.hypot(p.xInches() - cx, p.yInches() - cy);
    }

    /** How far the obstacle reaches from its center. */
    private static double obstacleRadius(PieceObstacle o) {
        return o.cylinder() ? o.maxXInches()
                : Math.hypot(o.maxXInches() - o.minXInches(), o.maxYInches() - o.minYInches()) / 2.0;
    }

    /**
     * What a person calls the element an obstacle belongs to: "HIVE frame" for the {@code frame-}
     * parts, "far FLOWER" (and "blue-wall", "red-wall", "audience") for a FLOWER's parts.
     */
    static String displayName(PieceObstacle o) {
        String id = o.id();
        if (id.startsWith("frame-")) {
            return "HIVE frame";
        }
        if (id.startsWith("flower-")) {
            String rest = id.substring("flower-".length());
            int part = Math.max(rest.lastIndexOf("-bracket"), rest.lastIndexOf("-head"));
            return (part > 0 ? rest.substring(0, part) : rest) + " FLOWER";
        }
        return id;
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
