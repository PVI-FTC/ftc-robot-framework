package pvi.harness.viewer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

import pvi.harness.ds.Button;
import pvi.harness.runtime.RunRecording;

/**
 * Small pictures of the driver's and operator's gamepads, with every button that is held at
 * the shown moment lit up. A, B, X and Y light in their gamepad colors; other buttons light
 * orange. The stick knobs move the way the sticks are pushed, and the triggers fill up as
 * they are pressed. The viewer shows it in the bottom-right corner of the window, under the
 * side panel ({@link GamepadPanel}).
 *
 * <p>Each controller is drawn in its own "design" coordinates, {@link #CONTROLLER_WIDTH} by
 * {@link #CONTROLLER_HEIGHT} pixels, then scaled by {@link #SCALE}.</p>
 */
public final class GamepadIndicator {
    /** Width of one controller drawing, in design pixels (before scaling). */
    static final double CONTROLLER_WIDTH = 200;
    /** Height of one controller drawing (with its label), in design pixels. */
    static final double CONTROLLER_HEIGHT = 130;
    /** Size of the drawing in the window compared with the design size (1 = as designed). */
    static final double SCALE = 1.0;
    /** How far a stick knob moves at full tilt, in design pixels. */
    static final double STICK_TRAVEL = 7;
    /** Sticks and triggers closer to rest than this count as untouched. */
    static final double AT_REST = 0.005;

    // Colors. Package-private so tests can check which thing a pixel shows.
    static final Color BODY = new Color(0x3A, 0x3A, 0x3A);
    static final Color BODY_EDGE = new Color(0x15, 0x15, 0x15);
    static final Color UNPRESSED = new Color(0x5C, 0x5C, 0x5C);
    static final Color LIT = new Color(0xFF, 0x8C, 0x1A);
    static final Color A_GREEN = new Color(0x3C, 0xD0, 0x4B);
    static final Color B_RED = new Color(0xE8, 0x30, 0x30);
    static final Color X_BLUE = new Color(0x2E, 0x86, 0xFF);
    static final Color Y_YELLOW = new Color(0xFF, 0xD0, 0x00);
    static final Color STICK_BASE = new Color(0x22, 0x22, 0x22);
    static final Color BOX = new Color(0, 0, 0, 170);

    /** Only static methods here; nothing to create. */
    private GamepadIndicator() {
    }

    /**
     * Draws both controllers side by side (Driver, then Operator) on a dark box whose
     * bottom-right corner is at ({@code right}, {@code bottom}) pixels.
     */
    public static void paint(Graphics2D g, int right, int bottom, RunRecording.LoopRecord loop) {
        double width = 2 * CONTROLLER_WIDTH * SCALE;
        double height = CONTROLLER_HEIGHT * SCALE;
        double left = right - width;
        double top = bottom - height;
        Graphics2D box = (Graphics2D) g.create();
        try {
            box.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            box.setColor(BOX);
            box.fill(new RoundRectangle2D.Double(left, top, width, height, 12, 12));
            box.translate(left, top);
            box.scale(SCALE, SCALE);
            paintController(box, 0, 0, loop.gamepad1(), "Driver");
            paintController(box, CONTROLLER_WIDTH, 0, loop.gamepad2(), "Operator");
        } finally {
            box.dispose();
        }
    }

    /**
     * Draws one controller with its top-left corner at ({@code left}, {@code top}), in the
     * graphics' current units (design pixels).
     */
    public static void paintController(Graphics2D g, double left, double top, RunRecording.GamepadState pad,
                                       String label) {
        Graphics2D c = (Graphics2D) g.create();
        try {
            c.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            c.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            c.translate(left, top);

            c.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
            c.setColor(FieldPainter.TEXT);
            c.drawString(label, 8, 14);

            // Triggers (fill up as pressed) and bumpers, above the body.
            paintTrigger(c, new Rectangle2D.Double(30, 18, 36, 10), pad.leftTrigger());
            paintTrigger(c, new Rectangle2D.Double(134, 18, 36, 10), pad.rightTrigger());
            fill(c, new RoundRectangle2D.Double(26, 31, 44, 10, 8, 8), held(pad, Button.LEFT_BUMPER) ? LIT : UNPRESSED);
            fill(c, new RoundRectangle2D.Double(130, 31, 44, 10, 8, 8), held(pad, Button.RIGHT_BUMPER) ? LIT : UNPRESSED);

            // Body with two grips, joined into one shape so the outline has no seams.
            Area body = new Area(new RoundRectangle2D.Double(14, 42, 172, 60, 40, 40));
            body.add(new Area(new Ellipse2D.Double(16, 66, 58, 56)));
            body.add(new Area(new Ellipse2D.Double(126, 66, 58, 56)));
            fill(c, body, BODY);
            c.setColor(BODY_EDGE);
            c.setStroke(new BasicStroke(1.5f));
            c.draw(body);

            // Left stick (upper left) and right stick (lower right), Xbox / Logitech layout.
            paintStick(c, center(Button.LEFT_STICK_BUTTON), pad.leftStickX(), pad.leftStickY(),
                    held(pad, Button.LEFT_STICK_BUTTON));
            paintStick(c, center(Button.RIGHT_STICK_BUTTON), pad.rightStickX(), pad.rightStickY(),
                    held(pad, Button.RIGHT_STICK_BUTTON));

            // D-pad.
            for (Button arrow : new Button[] {Button.DPAD_UP, Button.DPAD_DOWN, Button.DPAD_LEFT, Button.DPAD_RIGHT}) {
                Point2D p = center(arrow);
                fill(c, new Rectangle2D.Double(p.getX() - 4.5, p.getY() - 4.5, 9, 9), held(pad, arrow) ? LIT : UNPRESSED);
            }

            // Face buttons: bright when held, a dark version of their color when not.
            paintFaceButton(c, Button.Y, Y_YELLOW, held(pad, Button.Y));
            paintFaceButton(c, Button.A, A_GREEN, held(pad, Button.A));
            paintFaceButton(c, Button.X, X_BLUE, held(pad, Button.X));
            paintFaceButton(c, Button.B, B_RED, held(pad, Button.B));

            // Back, Guide, Start and the touchpad in the middle.
            Point2D back = center(Button.BACK);
            fill(c, new RoundRectangle2D.Double(back.getX() - 5, back.getY() - 3, 10, 6, 4, 4), held(pad, Button.BACK) ? LIT : UNPRESSED);
            Point2D start = center(Button.START);
            fill(c, new RoundRectangle2D.Double(start.getX() - 5, start.getY() - 3, 10, 6, 4, 4), held(pad, Button.START) ? LIT : UNPRESSED);
            Point2D guide = center(Button.GUIDE);
            fill(c, new Ellipse2D.Double(guide.getX() - 5, guide.getY() - 5, 10, 10), held(pad, Button.GUIDE) ? LIT : UNPRESSED);
            Point2D touch = center(Button.TOUCHPAD);
            fill(c, new RoundRectangle2D.Double(touch.getX() - 9, touch.getY() - 4, 18, 8, 4, 4), held(pad, Button.TOUCHPAD) ? LIT : UNPRESSED);
        } finally {
            c.dispose();
        }
    }

    /**
     * Where each button is drawn, in design pixels from the controller's top-left corner.
     * For the stick buttons this is the center of the stick's base.
     */
    static Point2D center(Button button) {
        return switch (button) {
            case LEFT_STICK_BUTTON -> new Point2D.Double(55, 66);
            case RIGHT_STICK_BUTTON -> new Point2D.Double(125, 90);
            case DPAD_UP -> new Point2D.Double(75, 81);
            case DPAD_DOWN -> new Point2D.Double(75, 99);
            case DPAD_LEFT -> new Point2D.Double(66, 90);
            case DPAD_RIGHT -> new Point2D.Double(84, 90);
            case Y -> new Point2D.Double(145, 54);
            case A -> new Point2D.Double(145, 78);
            case X -> new Point2D.Double(133, 66);
            case B -> new Point2D.Double(157, 66);
            case BACK -> new Point2D.Double(87, 58);
            case GUIDE -> new Point2D.Double(100, 54);
            case START -> new Point2D.Double(113, 58);
            case TOUCHPAD -> new Point2D.Double(100, 70);
            case LEFT_BUMPER -> new Point2D.Double(48, 36);
            case RIGHT_BUMPER -> new Point2D.Double(152, 36);
        };
    }

    /** Where a stick's knob is drawn: its base center, moved by the stick (up is negative y). */
    static Point2D knobCenter(Button stickButton, double x, double y) {
        Point2D base = center(stickButton);
        return new Point2D.Double(base.getX() + x * STICK_TRAVEL, base.getY() + y * STICK_TRAVEL);
    }

    // ---- Parts ------------------------------------------------------------------------------

    private static void paintStick(Graphics2D c, Point2D base, double x, double y, boolean clicked) {
        fill(c, new Ellipse2D.Double(base.getX() - 13, base.getY() - 13, 26, 26), STICK_BASE);
        boolean moved = Math.abs(x) > AT_REST || Math.abs(y) > AT_REST;
        double knobX = base.getX() + x * STICK_TRAVEL;
        double knobY = base.getY() + y * STICK_TRAVEL;
        fill(c, new Ellipse2D.Double(knobX - 8, knobY - 8, 16, 16), moved || clicked ? LIT : UNPRESSED);
        if (clicked) {
            // A stick pressed in (LS / RS) gets a white ring.
            c.setColor(Color.WHITE);
            c.setStroke(new BasicStroke(2f));
            c.draw(new Ellipse2D.Double(knobX - 8, knobY - 8, 16, 16));
        }
    }

    private static void paintTrigger(Graphics2D c, Rectangle2D outline, double pressed) {
        fill(c, outline, UNPRESSED);
        if (pressed > AT_REST) {
            double fillWidth = outline.getWidth() * Math.min(1.0, pressed);
            fill(c, new Rectangle2D.Double(outline.getX(), outline.getY(), fillWidth, outline.getHeight()), LIT);
        }
    }

    private static void paintFaceButton(Graphics2D c, Button button, Color color, boolean pressed) {
        Point2D p = center(button);
        fill(c, new Ellipse2D.Double(p.getX() - 6, p.getY() - 6, 12, 12), pressed ? color : dim(color));
    }

    /** A button's color when it is not pressed: much darker, so lit buttons stand out. */
    static Color dim(Color color) {
        return new Color(color.getRed() / 3, color.getGreen() / 3, color.getBlue() / 3);
    }

    private static boolean held(RunRecording.GamepadState pad, Button button) {
        return pad.buttons().contains(button);
    }

    private static void fill(Graphics2D c, Shape shape, Color color) {
        c.setColor(color);
        c.fill(shape);
    }
}
