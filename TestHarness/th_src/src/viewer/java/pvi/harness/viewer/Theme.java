package pvi.harness.viewer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.font.TextAttribute;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.ListCellRenderer;
import javax.swing.DefaultListCellRenderer;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.plaf.basic.BasicSliderUI;

/**
 * The viewer's look: a dark "mission control" style inspired by SpaceX's launch displays.
 * Near-black panels, hairline borders, white and grey text, uppercase labels with wide letter
 * spacing, large light numbers, and only a few accent colors (cyan for data, amber for warnings,
 * red for recording).
 *
 * <p>Every color and font the window uses comes from here, so the whole look can be changed in
 * one place. Drawing code reads the constants; Swing parts are styled with the helper methods.</p>
 */
public final class Theme {

    // ---- Colors --------------------------------------------------------------------------------

    /** Window background: almost black. */
    public static final Color BACKGROUND = new Color(0x05, 0x07, 0x0A);
    /** Card and bar background, a step lighter than the window. */
    public static final Color PANEL = new Color(0x0B, 0x0E, 0x12);
    /** A step lighter again: hovered buttons, gauge tracks. */
    public static final Color RAISED = new Color(0x14, 0x19, 0x1F);
    /** Hairline borders and dividers. */
    public static final Color LINE = new Color(0x22, 0x29, 0x31);
    /** Main text and numbers. */
    public static final Color TEXT = new Color(0xE8, 0xEC, 0xEF);
    /** Labels and secondary text. */
    public static final Color MUTED = new Color(0x7A, 0x85, 0x8F);
    /** Even quieter text: units, hints. */
    public static final Color FAINT = new Color(0x4A, 0x53, 0x5C);
    /** Data accent: live values, the trail, the timeline. */
    public static final Color ACCENT = new Color(0x4F, 0xC3, 0xF7);
    /** Warnings: placeholders, controller disconnects. */
    public static final Color AMBER = new Color(0xFF, 0xB3, 0x00);
    /** Recording. */
    public static final Color RED = new Color(0xFF, 0x3B, 0x30);
    /** Good / running. */
    public static final Color GREEN = new Color(0x34, 0xC7, 0x59);

    // ---- Fonts ---------------------------------------------------------------------------------

    /**
     * A DIN-style face like SpaceX's displays: Bahnschrift ships with Windows 10 and 11; then
     * D-DIN if installed; then Segoe UI; then Java's default sans serif.
     */
    private static final String FAMILY = firstInstalled("Bahnschrift", "D-DIN", "DIN", "Segoe UI", Font.SANS_SERIF);
    /** Fixed-width face for the Driver Station screen, so columns line up. */
    private static final String MONO_FAMILY = firstInstalled("Cascadia Mono", "Consolas", Font.MONOSPACED);

    /** Letter spacing for uppercase labels (0.12 = 12% of the font size). */
    private static final float LABEL_TRACKING = 0.12f;

    /** Only constants and helpers here; nothing to create. */
    private Theme() {
    }

    /** Normal text at {@code size} points. */
    public static Font text(float size) {
        return new Font(FAMILY, Font.PLAIN, 1).deriveFont(size);
    }

    /** Bold text at {@code size} points. */
    public static Font bold(float size) {
        return new Font(FAMILY, Font.BOLD, 1).deriveFont(size);
    }

    /** An uppercase label font: small, widely spaced. Use with {@link #label(String)}. */
    public static Font labelFont(float size) {
        return text(size).deriveFont(Map.of(TextAttribute.TRACKING, LABEL_TRACKING));
    }

    /** Large numbers (the mission clock, position readouts): light where the face has it. */
    public static Font number(float size) {
        Font light = new Font(FAMILY + " Light", Font.PLAIN, 1);
        Font base = light.getFamily().equals(Font.DIALOG) ? text(1) : light;
        return base.deriveFont(size);
    }

    /** Fixed-width text, for the Driver Station screen. */
    public static Font mono(float size) {
        return new Font(MONO_FAMILY, Font.PLAIN, 1).deriveFont(size);
    }

    /** Label text in the house style: uppercase. */
    public static String label(String text) {
        return text.toUpperCase(Locale.ROOT);
    }

    /** Turns on smooth shapes and text. */
    public static void smooth(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }

    /** A one-pixel line border. */
    public static Border hairline() {
        return BorderFactory.createLineBorder(LINE);
    }

    // ---- Styling Swing parts -------------------------------------------------------------------

    /**
     * A flat button: dark, a hairline border, uppercase spaced label, lighter while the mouse is
     * over it. Works for toggle buttons too.
     */
    public static <B extends AbstractButton> B flat(B button) {
        button.setFont(labelFont(11f));
        button.setForeground(TEXT);
        button.setBackground(PANEL);
        button.setContentAreaFilled(false);
        button.setOpaque(true);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(hairline(), BorderFactory.createEmptyBorder(6, 12, 6, 12)));
        button.getModel().addChangeListener(e -> {
            boolean hot = button.isEnabled() && (button.getModel().isRollover() || button.getModel().isPressed());
            button.setBackground(hot ? RAISED : PANEL);
        });
        button.setRolloverEnabled(true);
        return button;
    }

    /** A dark dropdown with a thin arrow and a dark list. */
    public static <T> JComboBox<T> flat(JComboBox<T> box) {
        // Swing paints a disabled dropdown light grey; keep it dark, with faint text.
        javax.swing.UIManager.put("ComboBox.disabledBackground", PANEL);
        javax.swing.UIManager.put("ComboBox.disabledForeground", FAINT);
        box.setUI(new BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                JButton arrow = new BasicArrowButton(BasicArrowButton.SOUTH, PANEL, PANEL, MUTED, PANEL);
                arrow.setBorder(BorderFactory.createEmptyBorder());
                return arrow;
            }

            @Override
            public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
                g.setColor(PANEL);
                g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
            }
        });
        box.setFont(text(13f));
        box.setForeground(TEXT);
        box.setBackground(PANEL);
        box.setBorder(hairline());
        ListCellRenderer<Object> plain = new DefaultListCellRenderer();
        box.setRenderer((list, value, index, selected, focused) -> {
            Component cell = plain.getListCellRendererComponent(list, value, index, selected, false);
            cell.setFont(text(13f));
            cell.setForeground(selected && index >= 0 ? BACKGROUND : TEXT);
            cell.setBackground(selected && index >= 0 ? ACCENT : PANEL);
            if (cell instanceof JComponent c) {
                c.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
            }
            list.setBackground(PANEL);
            return cell;
        });
        return box;
    }

    /**
     * A thin timeline slider: a hairline track, the played part in the accent color, and a
     * small vertical playhead.
     */
    public static JSlider flat(JSlider slider) {
        slider.setOpaque(false);
        slider.setUI(new BasicSliderUI(slider) {
            @Override
            public void paintTrack(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics;
                smooth(g);
                int y = trackRect.y + trackRect.height / 2;
                g.setColor(LINE);
                g.fillRect(trackRect.x, y - 1, trackRect.width, 2);
                g.setColor(slider.isEnabled() ? ACCENT : FAINT);
                int played = thumbRect.x + thumbRect.width / 2;
                g.fillRect(trackRect.x, y - 1, Math.max(0, played - trackRect.x), 2);
            }

            @Override
            public void paintThumb(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics;
                g.setColor(slider.isEnabled() ? TEXT : FAINT);
                int x = thumbRect.x + thumbRect.width / 2;
                g.fillRect(x - 1, thumbRect.y + 2, 3, thumbRect.height - 4);
            }

            @Override
            protected Dimension getThumbSize() {
                return new Dimension(9, 18);
            }

            @Override
            public void paintFocus(Graphics g) {
                // no dotted focus box
            }
        });
        return slider;
    }

    /** A scroll pane with no border and a slim dark scroll bar. */
    public static JScrollPane flat(JScrollPane scroll) {
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(BACKGROUND);
        scroll.setBackground(BACKGROUND);
        for (JScrollBar bar : List.of(scroll.getVerticalScrollBar(), scroll.getHorizontalScrollBar())) {
            bar.setPreferredSize(new Dimension(8, 8));
            bar.setUI(new BasicScrollBarUI() {
                @Override
                protected void configureScrollBarColors() {
                    thumbColor = LINE;
                    trackColor = BACKGROUND;
                }

                @Override
                protected JButton createDecreaseButton(int orientation) {
                    return noButton();
                }

                @Override
                protected JButton createIncreaseButton(int orientation) {
                    return noButton();
                }

                @Override
                protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
                    g.setColor(thumbColor);
                    g.fillRect(r.x + 2, r.y, r.width - 4, r.height);
                }
            });
        }
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    /**
     * A small round dot icon (the Record button's light): filled with {@code fill}, or just an
     * outline in {@code outline} when {@code fill} is null.
     */
    public static final class Dot implements javax.swing.Icon {
        private static final int SIZE = 10;
        private final Color fill;
        private final Color outline;

        public Dot(Color fill, Color outline) {
            this.fill = fill;
            this.outline = outline;
        }

        @Override
        public void paintIcon(Component c, Graphics graphics, int x, int y) {
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                smooth(g);
                java.awt.geom.Ellipse2D.Double dot = new java.awt.geom.Ellipse2D.Double(x + 0.5, y + 0.5, SIZE - 1, SIZE - 1);
                if (fill != null) {
                    g.setColor(fill);
                    g.fill(dot);
                } else {
                    g.setColor(outline);
                    g.setStroke(new BasicStroke(1.2f));
                    g.draw(dot);
                }
            } finally {
                g.dispose();
            }
        }

        @Override
        public int getIconWidth() {
            return SIZE;
        }

        @Override
        public int getIconHeight() {
            return SIZE;
        }
    }

    /** An invisible, zero-size button: scroll bars here have no arrow buttons. */
    private static JButton noButton() {
        JButton none = new JButton();
        none.setPreferredSize(new Dimension(0, 0));
        none.setMinimumSize(new Dimension(0, 0));
        none.setMaximumSize(new Dimension(0, 0));
        return none;
    }

    /** Draws an uppercase, spaced label at (x, baseline y) in {@code color}. */
    public static void drawLabel(Graphics2D g, String text, float x, float y, float size, Color color) {
        g.setFont(labelFont(size));
        g.setColor(color);
        g.drawString(label(text), x, y);
    }

    /** A hairline frame around a rectangle, with small brighter corner ticks. */
    public static void drawFrame(Graphics2D g, double x, double y, double w, double h) {
        g.setStroke(new BasicStroke(1f));
        g.setColor(LINE);
        g.draw(new java.awt.geom.Rectangle2D.Double(x, y, w, h));
        g.setColor(MUTED);
        double t = 6;
        g.draw(new java.awt.geom.Line2D.Double(x, y, x + t, y));
        g.draw(new java.awt.geom.Line2D.Double(x, y, x, y + t));
        g.draw(new java.awt.geom.Line2D.Double(x + w, y, x + w - t, y));
        g.draw(new java.awt.geom.Line2D.Double(x + w, y, x + w, y + t));
        g.draw(new java.awt.geom.Line2D.Double(x, y + h, x + t, y + h));
        g.draw(new java.awt.geom.Line2D.Double(x, y + h, x, y + h - t));
        g.draw(new java.awt.geom.Line2D.Double(x + w, y + h, x + w - t, y + h));
        g.draw(new java.awt.geom.Line2D.Double(x + w, y + h, x + w, y + h - t));
    }

    /** The first font family in {@code names} that is installed (the last one is always used as a fallback). */
    private static String firstInstalled(String... names) {
        if (GraphicsEnvironment.isHeadless()) {
            return names[names.length - 1];
        }
        List<String> installed = Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        for (String name : names) {
            if (installed.contains(name)) {
                return name;
            }
        }
        return names[names.length - 1];
    }
}
