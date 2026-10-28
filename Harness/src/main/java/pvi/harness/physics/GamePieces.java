package pvi.harness.physics;

import java.util.ArrayList;
import java.util.List;

/**
 * The game pieces on the field and how they move: the robot box pushes loose balls out of its
 * way, they roll and slow down, and they bounce off the walls and off each other.
 *
 * <p>Only pieces in the {@link GamePiece#FIELD} state move; balls in a FLOWER, a CELL or an
 * ALLIANCE AREA stay where they are for now. The robot is never slowed by a ball (it is far
 * heavier), and balls don't roll into the HIVE frame or FLOWERs yet (only the walls stop them).</p>
 *
 * <p>Every step works through the balls in the same order with no random numbers, so the same
 * run always gives exactly the same pushes, and a replay shows them again exactly.</p>
 *
 * <p>The position estimator owns one of these and calls {@link #step} each time the robot moves.</p>
 */
public final class GamePieces {
    /** Robot and balls never move further than this (inches) in one sub-step, so nothing passes through anything. */
    static final double MAX_MOVE_PER_SUBSTEP_INCHES = 0.5;
    /** Most sub-steps in one step (a teleport across the field would otherwise take forever). */
    static final int MAX_SUBSTEPS = 200;

    private final List<GamePiece> initial;
    private final PieceModel model;
    /** What the balls can bump into besides the walls (the HIVE frame, the FLOWERs). */
    private final List<PieceObstacle> obstacles;
    // Current state, one entry per piece in the initial order.
    private final String[] ids;
    private final String[] kinds;
    private final String[] states;
    private final double[] x;
    private final double[] y;
    private final double[] z;
    private final double[] vx;
    private final double[] vy;
    private final double[] radius;
    private final double[] mass;

    /** The pieces at their starting spots, standing still, with nothing but the walls to hit. */
    public GamePieces(List<GamePiece> start, PieceModel model) {
        this(start, model, List.of());
    }

    /**
     * The pieces at their starting spots, standing still. Loose balls also bounce off
     * {@code obstacles} (for BIOBUZZ, {@link BioBuzzObstacles#all()}) whenever the walls are on.
     */
    public GamePieces(List<GamePiece> start, PieceModel model, List<PieceObstacle> obstacles) {
        if (start == null || model == null || obstacles == null) {
            throw new IllegalArgumentException("Game pieces need a starting list, a piece model and an obstacle list.");
        }
        this.initial = List.copyOf(start);
        this.model = model;
        this.obstacles = List.copyOf(obstacles);
        int n = initial.size();
        ids = new String[n];
        kinds = new String[n];
        states = new String[n];
        x = new double[n];
        y = new double[n];
        z = new double[n];
        vx = new double[n];
        vy = new double[n];
        radius = new double[n];
        mass = new double[n];
        for (int i = 0; i < n; i++) {
            GamePiece p = initial.get(i);
            for (int j = 0; j < i; j++) {
                if (ids[j].equals(p.id())) {
                    throw new IllegalArgumentException("Two game pieces are both called \"" + p.id() + "\".");
                }
            }
            ids[i] = p.id();
            kinds[i] = p.kind();
            states[i] = p.state();
            x[i] = p.xInches();
            y[i] = p.yInches();
            z[i] = p.zInches();
            radius[i] = p.radiusInches();
            mass[i] = model.massOf(p.kind());
        }
    }

    /** No pieces at all. */
    public static GamePieces none() {
        return new GamePieces(List.of(), PieceModel.bioBuzz());
    }

    /** The pieces as they were at the start, in their fixed order. */
    public List<GamePiece> initial() {
        return initial;
    }

    /** Every piece now, in the same order as {@link #initial()}. */
    public List<GamePiece> snapshot() {
        List<GamePiece> now = new ArrayList<>(ids.length);
        for (int i = 0; i < ids.length; i++) {
            now.add(new GamePiece(ids[i], kinds[i], states[i], x[i], y[i], z[i]));
        }
        return now;
    }

    /** How the pieces move. */
    public PieceModel model() {
        return model;
    }

    /** A loose ball's speed over the tiles now, in in/s (0 for any other piece). */
    public double speedInchesPerSecond(String id) {
        for (int i = 0; i < ids.length; i++) {
            if (ids[i].equals(id)) {
                return Math.hypot(vx[i], vy[i]);
            }
        }
        throw new IllegalArgumentException("There is no game piece called \"" + id + "\".");
    }

    /**
     * Moves the loose balls on by {@code seconds}, while the robot box moves from {@code robotBefore}
     * to {@code robotAfter}. The step is cut into small sub-steps so a fast robot can't jump over
     * a ball.
     *
     * @param robotBefore  the robot at the start of the step
     * @param robotAfter   the robot at the end of the step
     * @param lengthInches the robot box, front to back
     * @param widthInches  the robot box, side to side
     * @param seconds      how long the step is
     * @param walls        true if the field walls stop the balls
     */
    public void step(Pose robotBefore, Pose robotAfter, double lengthInches, double widthInches, double seconds,
                     boolean walls) {
        if (seconds <= 0 || ids.length == 0) {
            return;
        }
        double turn = Math.IEEEremainder(robotAfter.headingRadians() - robotBefore.headingRadians(), 2 * Math.PI);
        double reach = Math.hypot(lengthInches, widthInches) / 2.0;
        double robotMove = Math.hypot(robotAfter.xInches() - robotBefore.xInches(),
                robotAfter.yInches() - robotBefore.yInches()) + Math.abs(turn) * reach;
        double ballMove = 0;
        for (int i = 0; i < ids.length; i++) {
            ballMove = Math.max(ballMove, Math.hypot(vx[i], vy[i]) * seconds);
        }
        int substeps = (int) Math.min(MAX_SUBSTEPS,
                Math.max(1, Math.ceil(Math.max(robotMove, ballMove) / MAX_MOVE_PER_SUBSTEP_INCHES)));
        double h = seconds / substeps;
        Pose last = robotBefore;
        for (int k = 1; k <= substeps; k++) {
            double share = (double) k / substeps;
            Pose robot = new Pose(
                    robotBefore.xInches() + (robotAfter.xInches() - robotBefore.xInches()) * share,
                    robotBefore.yInches() + (robotAfter.yInches() - robotBefore.yInches()) * share,
                    robotBefore.headingRadians() + turn * share);
            subStep(last, robot, lengthInches / 2.0, widthInches / 2.0, h, walls);
            last = robot;
        }
    }

    /** One small step: roll, get pushed by the robot, bounce off the walls, then off each other. */
    private void subStep(Pose robotBefore, Pose robot, double halfLength, double halfWidth, double h, boolean walls) {
        double slowBy = model.rollingDeceleration().value() * h;
        for (int i = 0; i < ids.length; i++) {
            if (!GamePiece.FIELD.equals(states[i])) {
                continue;
            }
            // Roll on, slowing down.
            x[i] += vx[i] * h;
            y[i] += vy[i] * h;
            double speed = Math.hypot(vx[i], vy[i]);
            double slower = speed - slowBy;
            if (slower < PieceModel.STOP_INCHES_PER_SECOND) {
                vx[i] = 0;
                vy[i] = 0;
            } else {
                vx[i] *= slower / speed;
                vy[i] *= slower / speed;
            }
            pushOutOfRobot(i, robotBefore, robot, halfLength, halfWidth, h);
            if (walls) {
                bounceOffWalls(i);
                for (PieceObstacle obstacle : obstacles) {
                    bounceOff(i, obstacle);
                }
            }
        }
        for (int i = 0; i < ids.length; i++) {
            for (int j = i + 1; j < ids.length; j++) {
                if (GamePiece.FIELD.equals(states[i]) && GamePiece.FIELD.equals(states[j])) {
                    collide(i, j);
                }
            }
        }
    }

    /**
     * If ball {@code i} overlaps the robot box, moves it out to just touch the box and gives it at
     * least the speed of the bit of robot that hit it (plus a little bounce).
     */
    private void pushOutOfRobot(int i, Pose robotBefore, Pose robot, double halfLength, double halfWidth, double h) {
        double cos = Math.cos(robot.headingRadians());
        double sin = Math.sin(robot.headingRadians());
        double dx = x[i] - robot.xInches();
        double dy = y[i] - robot.yInches();
        // The ball center in the robot's own frame: forward and left of the robot center.
        double forward = dx * cos + dy * sin;
        double left = -dx * sin + dy * cos;
        double r = radius[i];
        double nearForward = clamp(forward, -halfLength, halfLength);
        double nearLeft = clamp(left, -halfWidth, halfWidth);
        double normalForward;
        double normalLeft;
        boolean inside = forward == nearForward && left == nearLeft;
        if (inside) {
            // The center is inside the box: leave through the nearest side.
            double[] gaps = {halfLength - forward, forward + halfLength, halfWidth - left, left + halfWidth};
            int side = 0;
            for (int s = 1; s < 4; s++) {
                if (gaps[s] < gaps[side]) {
                    side = s;
                }
            }
            normalForward = side == 0 ? 1 : side == 1 ? -1 : 0;
            normalLeft = side == 2 ? 1 : side == 3 ? -1 : 0;
            nearForward = side == 0 ? halfLength : side == 1 ? -halfLength : forward;
            nearLeft = side == 2 ? halfWidth : side == 3 ? -halfWidth : left;
        } else {
            double gapForward = forward - nearForward;
            double gapLeft = left - nearLeft;
            double gap = Math.hypot(gapForward, gapLeft);
            if (gap >= r) {
                return; // not touching
            }
            normalForward = gapForward / gap;
            normalLeft = gapLeft / gap;
        }
        // Back to the field frame: the touching point on the box and the push direction.
        double nx = normalForward * cos - normalLeft * sin;
        double ny = normalForward * sin + normalLeft * cos;
        double touchX = robot.xInches() + nearForward * cos - nearLeft * sin;
        double touchY = robot.yInches() + nearForward * sin + nearLeft * cos;
        x[i] = touchX + nx * r;
        y[i] = touchY + ny * r;

        // How fast that point of the robot is moving: where it was a sub-step ago.
        double beforeCos = Math.cos(robotBefore.headingRadians());
        double beforeSin = Math.sin(robotBefore.headingRadians());
        double wasX = robotBefore.xInches() + nearForward * beforeCos - nearLeft * beforeSin;
        double wasY = robotBefore.yInches() + nearForward * beforeSin + nearLeft * beforeCos;
        double robotSpeedIn = ((touchX - wasX) * nx + (touchY - wasY) * ny) / h;
        double ballSpeedIn = vx[i] * nx + vy[i] * ny;
        if (ballSpeedIn < robotSpeedIn) {
            double newSpeed = robotSpeedIn + model.robotBounce().value() * (robotSpeedIn - ballSpeedIn);
            vx[i] += (newSpeed - ballSpeedIn) * nx;
            vy[i] += (newSpeed - ballSpeedIn) * ny;
        }
    }

    /** Keeps ball {@code i} inside the field walls, bouncing it back off any wall it reaches. */
    private void bounceOffWalls(int i) {
        double limit = Pose.FIELD_HALF_WIDTH_INCHES - radius[i];
        double bounce = model.wallBounce().value();
        if (x[i] > limit || x[i] < -limit) {
            x[i] = clamp(x[i], -limit, limit);
            if (Math.signum(vx[i]) == Math.signum(x[i])) {
                vx[i] = -vx[i] * bounce;
            }
        }
        if (y[i] > limit || y[i] < -limit) {
            y[i] = clamp(y[i], -limit, limit);
            if (Math.signum(vy[i]) == Math.signum(y[i])) {
                vy[i] = -vy[i] * bounce;
            }
        }
    }

    /**
     * If ball {@code i} overlaps {@code obstacle} (and reaches down to it), moves it out to just
     * touch the obstacle and bounces it back the way the walls do: the speed into the obstacle
     * reverses, shrunk by the wall bounce; the speed along it is kept.
     */
    private void bounceOff(int i, PieceObstacle obstacle) {
        double r = radius[i];
        if (!obstacle.reachedBy(r)) {
            return; // the ball rolls underneath
        }
        double nx;
        double ny;
        if (obstacle.cylinder()) {
            double dx = x[i] - obstacle.minXInches();
            double dy = y[i] - obstacle.minYInches();
            double distance = Math.hypot(dx, dy);
            double touching = obstacle.maxXInches() + r;
            if (distance >= touching) {
                return;
            }
            nx = distance > 0 ? dx / distance : 1.0;
            ny = distance > 0 ? dy / distance : 0.0;
            x[i] = obstacle.minXInches() + nx * touching;
            y[i] = obstacle.minYInches() + ny * touching;
        } else {
            double nearX = clamp(x[i], obstacle.minXInches(), obstacle.maxXInches());
            double nearY = clamp(y[i], obstacle.minYInches(), obstacle.maxYInches());
            if (nearX == x[i] && nearY == y[i]) {
                // The center is inside the box: leave through the nearest side.
                double[] gaps = {obstacle.maxXInches() - x[i], x[i] - obstacle.minXInches(),
                        obstacle.maxYInches() - y[i], y[i] - obstacle.minYInches()};
                int side = 0;
                for (int s = 1; s < 4; s++) {
                    if (gaps[s] < gaps[side]) {
                        side = s;
                    }
                }
                nx = side == 0 ? 1 : side == 1 ? -1 : 0;
                ny = side == 2 ? 1 : side == 3 ? -1 : 0;
                nearX = side == 0 ? obstacle.maxXInches() : side == 1 ? obstacle.minXInches() : x[i];
                nearY = side == 2 ? obstacle.maxYInches() : side == 3 ? obstacle.minYInches() : y[i];
            } else {
                double gapX = x[i] - nearX;
                double gapY = y[i] - nearY;
                double gap = Math.hypot(gapX, gapY);
                if (gap >= r) {
                    return; // not touching
                }
                nx = gapX / gap;
                ny = gapY / gap;
            }
            x[i] = nearX + nx * r;
            y[i] = nearY + ny * r;
        }
        double into = vx[i] * nx + vy[i] * ny;
        if (into < 0) {
            double change = -(1 + model.wallBounce().value()) * into;
            vx[i] += change * nx;
            vy[i] += change * ny;
        }
    }

    /** Two loose balls that overlap are moved apart and bounce off each other (the lighter one moves more). */
    private void collide(int i, int j) {
        double dx = x[j] - x[i];
        double dy = y[j] - y[i];
        double distance = Math.hypot(dx, dy);
        double touching = radius[i] + radius[j];
        if (distance >= touching) {
            return;
        }
        double nx = distance > 0 ? dx / distance : 1.0;
        double ny = distance > 0 ? dy / distance : 0.0;
        double inverseI = 1.0 / mass[i];
        double inverseJ = 1.0 / mass[j];
        double overlap = touching - distance;
        double shareI = inverseI / (inverseI + inverseJ);
        x[i] -= nx * overlap * shareI;
        y[i] -= ny * overlap * shareI;
        x[j] += nx * overlap * (1 - shareI);
        y[j] += ny * overlap * (1 - shareI);
        double closing = (vx[j] - vx[i]) * nx + (vy[j] - vy[i]) * ny;
        if (closing < 0) {
            double impulse = -(1 + model.pieceBounce().value()) * closing / (inverseI + inverseJ);
            vx[i] -= impulse * inverseI * nx;
            vy[i] -= impulse * inverseI * ny;
            vx[j] += impulse * inverseJ * nx;
            vy[j] += impulse * inverseJ * ny;
        }
    }

    private static double clamp(double value, double low, double high) {
        return Math.max(low, Math.min(high, value));
    }
}
