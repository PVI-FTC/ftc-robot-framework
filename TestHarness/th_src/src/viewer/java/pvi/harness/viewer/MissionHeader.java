package pvi.harness.viewer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;

import pvi.harness.runtime.RunRecording;
import pvi.harness.runtime.RunResult;

/**
 * The bar across the top of the viewer: the "FTC HARNESS" mark and what is running on the left,
 * a small mission clock (T+ match time) with the stage in the middle, and room on the right for
 * the Record button. Swing thread only.
 */
public final class MissionHeader extends JPanel {
    /** Height of the bar, in pixels. */
    static final int HEIGHT = 58;

    private RecordingView view;
    private int index;
    /** "LIVE" while driving, "REPLAY" otherwise. */
    private boolean live;
    /** True while a live drive will be saved (shows a red dot). */
    private boolean recording;
    private String source = "";

    /** @param right what to show at the right end (the Record button) */
    public MissionHeader(RecordingView view, JComponent right) {
        super(new BorderLayout());
        this.view = view;
        setOpaque(true);
        setBackground(Theme.PANEL);
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.LINE));
        setPreferredSize(new Dimension(1200, HEIGHT));
        JPanel holder = new JPanel(new java.awt.GridBagLayout());
        holder.setOpaque(false);
        holder.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 16));
        holder.add(right);
        add(holder, BorderLayout.EAST);
    }

    /** Shows loop entry {@code index}. */
    public void show(RecordingView newView, int newIndex, String newSource, boolean isLive, boolean isRecording) {
        this.view = newView;
        this.index = newIndex;
        this.source = newSource == null ? "" : newSource;
        this.live = isLive;
        this.recording = isRecording;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            Theme.smooth(g);
            RunRecording.Header header = view.recording().header();
            RunRecording.LoopRecord loop = view.loop(index);

            // Left: the mark and what is shown.
            g.setColor(Theme.TEXT);
            g.fill(new java.awt.geom.Rectangle2D.Double(20, 14, 3, 30));
            Theme.drawLabel(g, "FTC Harness", 34, 27, 14f, Theme.TEXT);
            String subtitle = header.opMode() + "  ·  " + header.robotConfig()
                    + (source.isEmpty() ? "" : "  ·  " + source);
            Theme.drawLabel(g, subtitle, 34, 43, 9.5f, Theme.MUTED);

            // Middle: the mission clock, kept quiet so it doesn't pull the eye from the field.
            String clock = "T+ " + clock(loop.matchTimeSeconds());
            Font clockFont = Theme.number(20f);
            g.setFont(clockFont);
            int clockWidth = g.getFontMetrics().stringWidth(clock);
            int centerX = getWidth() / 2;
            g.setColor(Theme.MUTED);
            g.drawString(clock, centerX - clockWidth / 2, 28);

            // Under it: mode and stage, with a colored dot.
            String mode = live ? "Live" : "Replay";
            String stageText = mode + "  ·  " + (header.period() == null ? "" : header.period() + "  ·  ")
                    + loop.stage() + String.format(Locale.ROOT, "  ·  loop %d / %d", index + 1, view.size());
            g.setFont(Theme.labelFont(9.5f));
            int stageWidth = g.getFontMetrics().stringWidth(Theme.label(stageText));
            int stageX = centerX - stageWidth / 2 + 7;
            g.setColor(stageColor(loop.stage()));
            g.fill(new Ellipse2D.Double(stageX - 13, 38, 6, 6));
            Theme.drawLabel(g, stageText, stageX, 44, 9f, Theme.FAINT);

            // A red recording dot next to the clock while a drive is being recorded.
            if (live && recording) {
                g.setColor(Theme.RED);
                g.fill(new Ellipse2D.Double(centerX + clockWidth / 2.0 + 10, 16, 8, 8));
            }

            // Thin ticks either side of the clock, like a launch display.
            g.setColor(Theme.LINE);
            g.draw(new Line2D.Double(centerX - clockWidth / 2.0 - 46, 21, centerX - clockWidth / 2.0 - 16, 21));
            g.draw(new Line2D.Double(centerX + clockWidth / 2.0 + 26, 21, centerX + clockWidth / 2.0 + 56, 21));
        } finally {
            g.dispose();
        }
    }

    /** 83.456 s -> "01:23.45". */
    static String clock(double seconds) {
        double s = Math.max(0, seconds);
        int minutes = (int) (s / 60);
        double rest = s - minutes * 60;
        return String.format(Locale.ROOT, "%02d:%05.2f", minutes, rest);
    }

    /** Amber before PLAY, green while running, grey once stopped. */
    private static Color stageColor(RunResult.Stage stage) {
        return switch (stage) {
            case INIT, INIT_LOOP -> Theme.AMBER;
            case START, LOOP -> Theme.GREEN;
            case STOP -> Theme.MUTED;
        };
    }
}
