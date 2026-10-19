package pvi.harness.viewer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
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
    static final Color BACKGROUND = new Color(0x2B, 0x2B, 0x2B);
    static final Color FIELD = new Color(0x9A, 0x9A, 0x9A);
    static final Color FOOT_LINE = new Color(0x8A, 0x8A, 0x8A);
    static final Color TILE_LINE = new Color(0x5A, 0x5A, 0x5A);
    static final Color BORDER = new Color(0x1A, 0x1A, 0x1A);
    static final Color TRAIL = new Color(0x1B, 0x9E, 0x4B);
    static final Color ROBOT_FILL = new Color(0x2E, 0x6F, 0xD9);
    static final Color ROBOT_OUTLINE = new Color(0x10, 0x2A, 0x5C);
    static final Color ROBOT_FRONT = new Color(0xF2, 0xF2, 0xF2);
    static final Color HEADING_ARROW = new Color(0xFF, 0xA5, 0x00);
    static final Color TEXT = new Color(0xE8, 0xE8, 0xE8);

    /** FTC field tiles are 2 ft (24 in) square: 6 × 6 tiles. */
    static final double TILE_INCHES = 24.0;
    /** Lighter lines every foot, between the tile lines. */
    static final double FOOT_INCHES = 12.0;
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

    /** Only static methods here; nothing to create. */
    private FieldPainter() {
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
            paintRobot(g, coords, box, pose);
        }
        paintText(g, coords, box, pose, caption);
    }

    /** The legend line shown under the field: what the robot box is, and if its size is a guess. */
    public static String legendText(RobotBox box) {
        String size = String.format(Locale.ROOT, "Robot %.0f x %.0f in (%s)", box.lengthInches(), box.widthInches(),
                box.modelName());
        String flag = box.measured() ? "measured size" : "PLACEHOLDER size, not measured";
        return size + ", " + flag + ". Green = path so far. Orange = facing.";
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

        // Thin lines every foot, then darker ones every tile (2 ft) on top.
        drawGrid(g, c, FOOT_INCHES, FOOT_LINE, 1f);
        drawGrid(g, c, TILE_INCHES, TILE_LINE, 2f);

        g.setColor(BORDER);
        g.setStroke(new BasicStroke(3f));
        g.draw(new Rectangle2D.Double(c.screenX(-half), c.screenY(half), c.fieldPixels(), c.fieldPixels()));
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
        g.setStroke(new BasicStroke((float) Math.max(2.0, 0.6 * c.pixelsPerInch()),
                BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(path);
    }

    /** The robot box (turned to its heading), a light front edge, and the heading arrow. */
    private static void paintRobot(Graphics2D g, FieldCoordinates c, RobotBox box, Pose pose) {
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
        g.setStroke(new BasicStroke(2f));
        g.draw(outline);

        // Front edge, so the robot's front is visible even without the arrow.
        double[] frontLeft = toScreen(c, pose, halfLength, halfWidth);
        double[] frontRight = toScreen(c, pose, halfLength, -halfWidth);
        g.setColor(ROBOT_FRONT);
        g.setStroke(new BasicStroke((float) Math.max(3.0, 0.8 * c.pixelsPerInch()), BasicStroke.CAP_BUTT,
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
        g.setStroke(new BasicStroke((float) Math.max(2.0, 0.6 * c.pixelsPerInch()), BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(center[0], center[1], shaftEnd[0], shaftEnd[1]));
        Path2D.Double head = new Path2D.Double();
        head.moveTo(tip[0], tip[1]);
        head.lineTo(headLeft[0], headLeft[1]);
        head.lineTo(headRight[0], headRight[1]);
        head.closePath();
        g.fill(head);
    }

    /** Caption in the top margin, legend in the bottom margin, axis labels at the edges. */
    private static void paintText(Graphics2D g, FieldCoordinates c, RobotBox box, Pose pose, String caption) {
        double half = Pose.FIELD_HALF_WIDTH_INCHES;
        int fontSize = (int) Math.max(10, Math.min(14, c.marginPixels() * 0.5));
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, fontSize));
        g.setColor(TEXT);
        float left = (float) c.screenX(-half);
        float topBaseline = (float) (c.screenY(half) - c.marginPixels() * 0.3);
        float bottomBaseline = (float) (c.screenY(-half) + c.marginPixels() * 0.65);

        String top = caption == null ? "" : caption;
        if (pose != null) {
            top = (top.isEmpty() ? "" : top + "   ") + pose.toString();
        }
        g.drawString(top, left, topBaseline);
        g.drawString(legendText(box), left, bottomBaseline);

        // Which way is +x and +y, in the right and top margins.
        g.drawString("+x", (float) (c.screenX(half) + 4), (float) c.screenY(0));
        g.drawString("+y", (float) (c.screenX(half) - 2 * fontSize), topBaseline);
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
