package pvi.harness.viewer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.Locale;

import pvi.harness.physics.Pose;
import pvi.harness.runtime.RunRecording;

/**
 * Draws the field from above: the 12 × 12 ft field with its grid, the path the robot has
 * driven so far (the trail), the robot as a box, and an arrow showing which way it faces.
 *
 * <p>It only draws what it is given (plain data such as {@link Pose}), so the same code draws
 * into a window (Step 13) or into an image file. Every inch-to-pixel conversion goes through
 * {@link FieldCoordinates}.</p>
 */
public final class FieldPainter {
    // Colors. Package-private so tests can check which thing a pixel belongs to.
    // The look follows Theme: a dark field with hairline grid, a cyan path, a wireframe robot.
    static final Color BACKGROUND = Theme.BACKGROUND;
    static final Color FIELD = new Color(0x0A, 0x0D, 0x11);
    static final Color FOOT_LINE = new Color(0x12, 0x17, 0x1D);
    static final Color TILE_LINE = new Color(0x1D, 0x24, 0x2C);
    static final Color BORDER = new Color(0x3A, 0x44, 0x4F);
    static final Color TRAIL = Theme.ACCENT;
    static final Color ROBOT_FILL = new Color(0x0E, 0x1F, 0x2E);
    static final Color ROBOT_OUTLINE = new Color(0xA9, 0xBC, 0xCC);
    static final Color ROBOT_FRONT = Theme.ACCENT;
    static final Color HEADING_ARROW = Theme.TEXT;
    static final Color TEXT = Theme.TEXT;
    /** Quieter text: axis labels and the legend. */
    static final Color TEXT_MUTED = Theme.MUTED;

    /** FTC field tiles are 2 ft (24 in) square: 6 × 6 tiles. */
    static final double TILE_INCHES = 24.0;
    /** Lighter lines every foot, between the tile lines. */
    static final double FOOT_INCHES = 12.0;
    /** A wheel, seen from above. */
    static final Color WHEEL = new Color(0x1A, 0x2A, 0x38);
    /** Mecanum roller lines on a wheel. */
    static final Color WHEEL_ROLLERS = new Color(0x3D, 0x52, 0x63);
    /** A wheel or the intake turning backwards (wheel) or ejecting (intake). */
    static final Color REVERSE = Theme.AMBER;

    /** Wheel size on the drawing, in inches (a typical 104 mm mecanum wheel, seen from above). */
    static final double WHEEL_LENGTH_INCHES = 4.0;
    static final double WHEEL_WIDTH_INCHES = 1.6;

    /** How far the heading arrow reaches past the front of the robot, in inches. */
    static final double ARROW_PAST_FRONT_INCHES = 8.0;

    /**
     * The robot's footprint for drawing, from the drive model.
     *
     * @param lengthInches front to back
     * @param widthInches  side to side
     * @param measured     true only if both were measured on the real robot
     * @param modelName    e.g. "Team A", or "unknown robot"
     */
    public record RobotBox(double lengthInches, double widthInches, boolean measured, String modelName) {

        /** Size used when a recording has no drive model: the FTC maximum starting size. */
        public static final RobotBox UNKNOWN = new RobotBox(18.0, 18.0, false, "unknown robot");

        /** The box from a recording's drive model, or {@link #UNKNOWN} if it has none. */
        public static RobotBox of(RunRecording.DriveModelValues model) {
            if (model == null) {
                return UNKNOWN;
            }
            return new RobotBox(model.length().value(), model.width().value(),
                    model.length().measured() && model.width().measured(), model.name());
        }
    }

    /**
     * What the robot's moving parts are doing, for drawing them on the robot: the power each
     * drive motor and the intake was set to (-1 to 1). A missing intake is drawn as none.
     */
    public record RobotParts(double frontLeft, double frontRight, double rearLeft, double rearRight,
                             boolean hasIntake, double intake) {

        /**
         * Reads the parts from a loop's motors by their configuration names ("frontLeft",
         * "frontRight", "rearLeft", "rearRight", "intake"). Returns null if the four drive
         * motors aren't all there (then no wheels are drawn).
         */
        public static RobotParts of(List<RunRecording.MotorState> motors) {
            Double fl = null, fr = null, rl = null, rr = null, in = null;
            for (RunRecording.MotorState m : motors) {
                switch (m.name()) {
                    case "frontLeft" -> fl = m.power();
                    case "frontRight" -> fr = m.power();
                    case "rearLeft" -> rl = m.power();
                    case "rearRight" -> rr = m.power();
                    case "intake" -> in = m.power();
                    default -> { }
                }
            }
            if (fl == null || fr == null || rl == null || rr == null) {
                return null;
            }
            return new RobotParts(fl, fr, rl, rr, in != null, in == null ? 0 : in);
        }
    }

    /** Only static methods here; nothing to create. */
    private FieldPainter() {
    }

    /** Like {@link #paint(Graphics2D, FieldCoordinates, RobotBox, Pose, List, String)}, also drawing the wheels and intake. */
    public static void paint(Graphics2D g, FieldCoordinates coords, RobotBox box, Pose pose,
                             List<Pose> trail, String caption, RobotParts parts) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        paintField(g, coords);
        paintTrail(g, coords, trail);
        if (pose != null) {
            paintRobot(g, coords, box, pose, parts);
        }
        paintText(g, coords, box, pose, caption);
    }

    /**
     * Draws everything, in order from bottom to top: background, field and grid, trail, robot,
     * heading arrow, then the caption (top) and legend (bottom) in the margins.
     *
     * @param g       where to draw (a window or an image)
     * @param coords  inch-to-pixel conversion
     * @param box     the robot's size
     * @param pose    where the robot is, or null to draw no robot (position not tracked)
     * @param trail   where the robot has been, oldest first (may be empty)
     * @param caption text for the top margin, e.g. the OpMode and time, or null for none
     */
    public static void paint(Graphics2D g, FieldCoordinates coords, RobotBox box, Pose pose,
                             List<Pose> trail, String caption) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        paintField(g, coords);
        paintTrail(g, coords, trail);
        if (pose != null) {
            paintRobot(g, coords, box, pose, null);
        }
        paintText(g, coords, box, pose, caption);
    }

    /** The legend line shown under the field: what the robot box is, and if its size is a guess. */
    public static String legendText(RobotBox box) {
        String size = String.format(Locale.ROOT, "Robot %.0f x %.0f in (%s)", box.lengthInches(), box.widthInches(),
                box.modelName());
        String flag = box.measured() ? "measured size" : "PLACEHOLDER size, not measured";
        return size + ", " + flag + ". Cyan = path, white = facing.";
    }

    // ---- The parts --------------------------------------------------------------------------

    /** Background, field square, foot and tile lines, border. */
    private static void paintField(Graphics2D g, FieldCoordinates c) {
        int total = c.totalPixels();
        g.setColor(BACKGROUND);
        g.fill(new Rectangle2D.Double(0, 0, total, total));

        double half = Pose.FIELD_HALF_WIDTH_INCHES;
        g.setColor(FIELD);
        g.fill(new Rectangle2D.Double(c.screenX(-half), c.screenY(half), c.fieldPixels(), c.fieldPixels()));

        // Faint lines every foot, then slightly brighter ones every tile (2 ft) on top.
        drawGrid(g, c, FOOT_INCHES, FOOT_LINE, 1f);
        drawGrid(g, c, TILE_INCHES, TILE_LINE, 1f);

        // A small cross at the field center (0, 0).
        double arm = 3 * c.pixelsPerInch();
        g.setColor(BORDER);
        g.setStroke(new BasicStroke(1f));
        g.draw(new Line2D.Double(c.screenX(0) - arm, c.screenY(0), c.screenX(0) + arm, c.screenY(0)));
        g.draw(new Line2D.Double(c.screenX(0), c.screenY(0) - arm, c.screenX(0), c.screenY(0) + arm));

        // Hairline border with brighter corner ticks.
        Theme.drawFrame(g, c.screenX(-half), c.screenY(half), c.fieldPixels(), c.fieldPixels());
    }

    /** Lines across the field every {@code spacingInches}, both ways. */
    private static void drawGrid(Graphics2D g, FieldCoordinates c, double spacingInches, Color color, float widthPixels) {
        double half = Pose.FIELD_HALF_WIDTH_INCHES;
        g.setColor(color);
        g.setStroke(new BasicStroke(widthPixels));
        for (double at = -half + spacingInches; at < half - 1e-9; at += spacingInches) {
            g.draw(new Line2D.Double(c.screenX(at), c.screenY(-half), c.screenX(at), c.screenY(half)));
            g.draw(new Line2D.Double(c.screenX(-half), c.screenY(at), c.screenX(half), c.screenY(at)));
        }
    }

    /** A line through every pose the robot has been at. */
    private static void paintTrail(Graphics2D g, FieldCoordinates c, List<Pose> trail) {
        if (trail == null || trail.size() < 2) {
            return;
        }
        Path2D.Double path = new Path2D.Double();
        path.moveTo(c.screenX(trail.get(0).xInches()), c.screenY(trail.get(0).yInches()));
        for (Pose p : trail.subList(1, trail.size())) {
            path.lineTo(c.screenX(p.xInches()), c.screenY(p.yInches()));
        }
        g.setColor(TRAIL);
        g.setStroke(new BasicStroke((float) Math.max(2.0, 0.3 * c.pixelsPerInch()),
                BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(path);
    }

    /**
     * The robot box (turned to its heading), its wheels and intake if {@code parts} is given, a
     * light front edge, and the heading arrow.
     */
    private static void paintRobot(Graphics2D g, FieldCoordinates c, RobotBox box, Pose pose, RobotParts parts) {
        double halfLength = box.lengthInches() / 2.0;
        double halfWidth = box.widthInches() / 2.0;
        // Corners in order front-left, front-right, back-right, back-left (forward, left), in inches.
        double[][] corners = {{halfLength, halfWidth}, {halfLength, -halfWidth},
                {-halfLength, -halfWidth}, {-halfLength, halfWidth}};

        Path2D.Double outline = new Path2D.Double();
        for (int i = 0; i < corners.length; i++) {
            double[] screen = toScreen(c, pose, corners[i][0], corners[i][1]);
            if (i == 0) {
                outline.moveTo(screen[0], screen[1]);
            } else {
                outline.lineTo(screen[0], screen[1]);
            }
        }
        outline.closePath();
        g.setColor(ROBOT_FILL);
        g.fill(outline);
        g.setColor(ROBOT_OUTLINE);
        g.setStroke(new BasicStroke(1.5f));
        g.draw(outline);

        if (parts != null) {
            paintParts(g, c, box, pose, parts);
        }

        // Front edge, so the robot's front is visible even without the arrow.
        double[] frontLeft = toScreen(c, pose, halfLength, halfWidth);
        double[] frontRight = toScreen(c, pose, halfLength, -halfWidth);
        g.setColor(ROBOT_FRONT);
        g.setStroke(new BasicStroke((float) Math.max(2.5, 0.6 * c.pixelsPerInch()), BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_MITER));
        g.draw(new Line2D.Double(frontLeft[0], frontLeft[1], frontRight[0], frontRight[1]));

        // Heading arrow: from the center to a little past the front, with a triangle head.
        double tipForward = halfLength + ARROW_PAST_FRONT_INCHES;
        double headLength = 4.0;
        double headHalfWidth = 2.5;
        double[] center = toScreen(c, pose, 0, 0);
        double[] shaftEnd = toScreen(c, pose, tipForward - headLength, 0);
        double[] tip = toScreen(c, pose, tipForward, 0);
        double[] headLeft = toScreen(c, pose, tipForward - headLength, headHalfWidth);
        double[] headRight = toScreen(c, pose, tipForward - headLength, -headHalfWidth);
        g.setColor(HEADING_ARROW);
        g.setStroke(new BasicStroke((float) Math.max(2.0, 0.45 * c.pixelsPerInch()), BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(center[0], center[1], shaftEnd[0], shaftEnd[1]));
        Path2D.Double head = new Path2D.Double();
        head.moveTo(tip[0], tip[1]);
        head.lineTo(headLeft[0], headLeft[1]);
        head.lineTo(headRight[0], headRight[1]);
        head.closePath();
        g.fill(head);
    }

    /**
     * Four mecanum wheels in the corners and the intake across the front. Each wheel fills from
     * its middle toward the front (cyan) or back (amber) as far as its power; the rollers are
     * drawn as diagonal lines, in the X pattern a mecanum drive has seen from above. The intake
     * lights cyan while taking in, amber while ejecting.
     */
    private static void paintParts(Graphics2D g, FieldCoordinates c, RobotBox box, Pose pose, RobotParts parts) {
        double halfLength = box.lengthInches() / 2.0;
        double halfWidth = box.widthInches() / 2.0;
        double wheelForward = halfLength - WHEEL_LENGTH_INCHES / 2.0 - 1.0;
        double wheelLeft = halfWidth - WHEEL_WIDTH_INCHES / 2.0 - 0.6;
        // forward, left, power, roller slant (+1 = "/" from above, -1 = "\").
        double[][] wheels = {
                {wheelForward, wheelLeft, parts.frontLeft(), -1},
                {wheelForward, -wheelLeft, parts.frontRight(), +1},
                {-wheelForward, wheelLeft, parts.rearLeft(), +1},
                {-wheelForward, -wheelLeft, parts.rearRight(), -1}};
        for (double[] w : wheels) {
            paintWheel(g, c, pose, w[0], w[1], w[2], w[3]);
        }
        if (parts.hasIntake()) {
            paintIntake(g, c, pose, halfLength, halfWidth, parts.intake());
        }
    }

    /** One wheel centered at (forward, left) inches on the robot. */
    private static void paintWheel(Graphics2D g, FieldCoordinates c, Pose pose, double forward, double left,
                                   double power, double slant) {
        double hl = WHEEL_LENGTH_INCHES / 2.0;
        double hw = WHEEL_WIDTH_INCHES / 2.0;
        g.setColor(WHEEL);
        g.fill(robotRect(c, pose, forward - hl, forward + hl, left - hw, left + hw));

        // Power: fill from the middle toward the front (forward) or the back (backward).
        double clamped = Math.max(-1.0, Math.min(1.0, power));
        if (Math.abs(clamped) > 0.005) {
            double end = forward + clamped * hl;
            g.setColor(clamped > 0 ? Theme.ACCENT : REVERSE);
            g.fill(robotRect(c, pose, Math.min(forward, end), Math.max(forward, end), left - hw, left + hw));
        }

        // Rollers: three short diagonal lines.
        g.setColor(WHEEL_ROLLERS);
        g.setStroke(new BasicStroke(1f));
        for (double at = -hl * 0.6; at <= hl * 0.6 + 1e-9; at += hl * 0.6) {
            double[] a = toScreen(c, pose, forward + at - slant * hw * 0.8, left - hw * 0.8);
            double[] b = toScreen(c, pose, forward + at + slant * hw * 0.8, left + hw * 0.8);
            g.draw(new Line2D.Double(a[0], a[1], b[0], b[1]));
        }
        g.setColor(ROBOT_OUTLINE);
        g.draw(robotRect(c, pose, forward - hl, forward + hl, left - hw, left + hw));
    }

    /** The intake: a bar just inside the front edge, with chevrons showing which way it runs. */
    private static void paintIntake(Graphics2D g, FieldCoordinates c, Pose pose, double halfLength,
                                    double halfWidth, double power) {
        double front = halfLength - 0.8;
        double back = front - 2.2;
        double side = halfWidth * 0.55;
        boolean running = Math.abs(power) > 0.005;
        g.setColor(running ? (power > 0 ? Theme.ACCENT : REVERSE) : WHEEL);
        g.fill(robotRect(c, pose, back, front, -side, side));
        g.setColor(ROBOT_OUTLINE);
        g.setStroke(new BasicStroke(1f));
        g.draw(robotRect(c, pose, back, front, -side, side));
        if (running) {
            // Chevrons point in (toward the robot) while intaking, out while ejecting.
            double tipShift = power > 0 ? -0.6 : 0.6;
            double middle = (front + back) / 2.0;
            g.setColor(Theme.BACKGROUND);
            g.setStroke(new BasicStroke(1.4f));
            for (double at = -side * 0.6; at <= side * 0.6 + 1e-9; at += side * 0.6) {
                double[] a = toScreen(c, pose, middle - tipShift, at - 0.7);
                double[] tip = toScreen(c, pose, middle + tipShift, at);
                double[] b = toScreen(c, pose, middle - tipShift, at + 0.7);
                Path2D.Double chevron = new Path2D.Double();
                chevron.moveTo(a[0], a[1]);
                chevron.lineTo(tip[0], tip[1]);
                chevron.lineTo(b[0], b[1]);
                g.draw(chevron);
            }
        }
    }

    /** A rectangle on the robot, from forward/left limits in inches, turned with the robot. */
    private static Path2D.Double robotRect(FieldCoordinates c, Pose pose, double back, double front,
                                           double right, double left) {
        double[][] corners = {{front, left}, {front, right}, {back, right}, {back, left}};
        Path2D.Double path = new Path2D.Double();
        for (int i = 0; i < corners.length; i++) {
            double[] p = toScreen(c, pose, corners[i][0], corners[i][1]);
            if (i == 0) {
                path.moveTo(p[0], p[1]);
            } else {
                path.lineTo(p[0], p[1]);
            }
        }
        path.closePath();
        return path;
    }

    /** Caption in the top margin, legend in the bottom margin, axis labels at the edges. */
    private static void paintText(Graphics2D g, FieldCoordinates c, RobotBox box, Pose pose, String caption) {
        double half = Pose.FIELD_HALF_WIDTH_INCHES;
        float fontSize = (float) Math.max(9, Math.min(12, c.marginPixels() * 0.42));
        float left = (float) c.screenX(-half);
        float topBaseline = (float) (c.screenY(half) - c.marginPixels() * 0.35);
        float bottomBaseline = (float) (c.screenY(-half) + c.marginPixels() * 0.65);

        String top = caption == null ? "" : caption;
        if (pose != null) {
            top = (top.isEmpty() ? "" : top + "   ") + pose.toString();
        }
        float width = (float) c.fieldPixels();
        Theme.drawLabel(g, top, left, topBaseline, fitted(g, top, fontSize, width * 0.85f), TEXT);
        String legend = legendText(box);
        Theme.drawLabel(g, legend, left, bottomBaseline, fitted(g, legend, fontSize, width), box.measured() ? TEXT_MUTED : Theme.AMBER);

        // Which way is +x and +y: at the right edge, beside the center line and above the top-right corner.
        Theme.drawLabel(g, "+x", (float) (c.screenX(half) + 6), (float) c.screenY(0) + fontSize / 3, fontSize, TEXT_MUTED);
        Theme.drawLabel(g, "+y", (float) (c.screenX(half) - 2.2f * fontSize), topBaseline, fontSize, TEXT_MUTED);
    }

    /** The largest label size up to {@code size} (and at least 7) at which {@code text} fits in {@code maxWidth}. */
    private static float fitted(Graphics2D g, String text, float size, float maxWidth) {
        float fit = size;
        while (fit > 7f && g.getFontMetrics(Theme.labelFont(fit)).stringWidth(Theme.label(text)) > maxWidth) {
            fit -= 0.5f;
        }
        return fit;
    }

    /** Screen pixels for a point given in inches forward/left of the robot center. */
    private static double[] toScreen(FieldCoordinates c, Pose pose, double forwardInches, double leftInches) {
        double cos = Math.cos(pose.headingRadians());
        double sin = Math.sin(pose.headingRadians());
        // Same robot-to-field turn as the position estimator uses.
        double x = pose.xInches() + forwardInches * cos - leftInches * sin;
        double y = pose.yInches() + forwardInches * sin + leftInches * cos;
        return new double[] {c.screenX(x), c.screenY(y)};
    }
}
