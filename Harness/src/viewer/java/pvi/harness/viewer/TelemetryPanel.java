package pvi.harness.viewer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.JComponent;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;

import pvi.harness.ds.RumbleRecord;
import pvi.harness.physics.Pose;
import pvi.harness.runtime.RunRecording;

/**
 * The viewer's right-hand column: the moment being shown, as "mission control" cards. Big
 * position readouts, a gauge per motor and servo, the Driver Station screen as it looked then,
 * and events (rumbles, controller connects/disconnects). While driving live, a status card and
 * the key layout come first.
 *
 * <p>Drawn by hand in the {@link Theme} style. The same facts as plain text are in
 * {@link SidePanelText}, which is also given to screen readers. Swing thread only.</p>
 */
public final class TelemetryPanel extends JComponent implements Scrollable {
    /** Width of the column, in pixels. */
    static final int WIDTH = 400;
    private static final int PAD = 14;
    private static final int CARD_GAP = 10;

    private RecordingView view;
    private int index;
    /** Lines for the live-drive cards, or null when not driving. */
    private List<String> liveStatus;
    private List<String> liveHelp;
    /** Height the last paint needed; the preferred height follows it. */
    private int contentHeight = 900;

    public TelemetryPanel(RecordingView view) {
        this.view = view;
        setOpaque(true);
    }

    /** Shows loop entry {@code index} of the current recording. */
    public void show(int index) {
        view.loop(index); // fails clearly if the index is outside the recording
        this.index = index;
        getAccessibleContext().setAccessibleDescription(String.join("\n", SidePanelText.describe(view, index)));
        repaint();
    }

    /** Switches to another recording, showing its first moment. */
    public void setView(RecordingView newView) {
        this.view = newView;
        show(0);
    }

    /**
     * While driving: the status lines (the first is the headline) and the key layout. Pass
     * nulls when the drive is over.
     */
    public void setLive(List<String> status, List<String> help) {
        this.liveStatus = status;
        this.liveHelp = help;
        repaint();
    }

    /** Screen readers get the same facts as plain text ({@link SidePanelText}). */
    @Override
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleJComponent() {
                @Override
                public AccessibleRole getAccessibleRole() {
                    return AccessibleRole.PANEL;
                }
            };
        }
        return accessibleContext;
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(WIDTH, contentHeight);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            Theme.smooth(g);
            g.setColor(Theme.BACKGROUND);
            g.fillRect(0, 0, getWidth(), getHeight());
            int needed = paintCards(g);
            if (needed != contentHeight) {
                contentHeight = needed;
                revalidate();
            }
        } finally {
            g.dispose();
        }
    }

    // ---- Cards -------------------------------------------------------------------------------------

    /** Paints every card from the top; returns the height used. */
    private int paintCards(Graphics2D g) {
        RunRecording recording = view.recording();
        RunRecording.Header header = recording.header();
        RunRecording.LoopRecord loop = view.loop(index);
        int width = getWidth() - 2 * PAD;
        int y = PAD;

        if (liveStatus != null && !liveStatus.isEmpty()) {
            y = liveCard(g, y, width) + CARD_GAP;
        }

        // Vehicle: what is running.
        y = card(g, "Vehicle", y, width, 44, (top) -> {
            readout(g, PAD + 12, top, "OpMode", header.opMode(), Theme.TEXT);
            readout(g, PAD + 12 + width * 2 / 5, top, "Period",
                    header.period() == null ? "-" : header.period().name(), Theme.TEXT);
            readout(g, PAD + 12 + width * 7 / 10, top, "Robot", header.robotConfig(), Theme.TEXT);
        }) + CARD_GAP;

        // Position: big numbers.
        Pose pose = loop.pose();
        y = card(g, "Position", y, width, 58, (top) -> {
            if (pose == null) {
                text(g, "NOT TRACKED", PAD + 12, top + 32, Theme.text(16f), Theme.MUTED);
                return;
            }
            bigReadout(g, PAD + 12, top, "X", String.format(Locale.ROOT, "%.2f", pose.xInches() / 12.0), "ft");
            bigReadout(g, PAD + 12 + width / 3, top, "Y", String.format(Locale.ROOT, "%.2f", pose.yInches() / 12.0), "ft");
            bigReadout(g, PAD + 12 + width * 2 / 3, top, "Heading",
                    String.format(Locale.ROOT, "%.1f", pose.headingDegrees()), "deg");
        }) + CARD_GAP;

        if (loop.autoStep() != null) {
            y = card(g, "Auto step", y, width, 28, (top) ->
                    text(g, ellipsize(g, loop.autoStep(), Theme.text(15f), width - 24), PAD + 12, top + 20,
                            Theme.text(15f), Theme.ACCENT)) + CARD_GAP;
        }

        // Motors and servos: a gauge each.
        int rows = loop.motors().size() + loop.servos().size();
        y = card(g, "Motors" + (loop.servos().isEmpty() ? "" : " / servos"), y, width, Math.max(1, rows) * 22, (top) -> {
            int rowY = top;
            for (RunRecording.MotorState motor : loop.motors()) {
                gaugeRow(g, rowY, width, motor.name(), motor.power(), true,
                        String.format(Locale.ROOT, "%+.2f", motor.power()));
                rowY += 22;
            }
            for (RunRecording.ServoState servo : loop.servos()) {
                gaugeRow(g, rowY, width, servo.name(), servo.isSet() ? servo.position() : 0, false,
                        servo.isSet() ? String.format(Locale.ROOT, "%.2f", servo.position()) : "unset");
                rowY += 22;
            }
            if (rows == 0) {
                text(g, "NO MOTORS", PAD + 12, top + 15, Theme.text(12f), Theme.MUTED);
            }
        }) + CARD_GAP;

        // Driver Station screen as it looked at this moment.
        List<String> screen = driverStationLines(index);
        y = card(g, "Driver Station" + screenTime(index), y, width, Math.max(1, screen.size()) * 17 + 2, (top) -> {
            int lineY = top + 13;
            Font mono = Theme.mono(12f);
            if (screen.isEmpty()) {
                text(g, "nothing sent yet", PAD + 12, lineY, mono, Theme.MUTED);
            }
            for (String line : screen) {
                text(g, ellipsize(g, line, mono, width - 24), PAD + 12, lineY, mono, Theme.TEXT);
                lineY += 17;
            }
        }) + CARD_GAP;

        // Events: rumbles and marked moments so far.
        List<String> events = eventLines(recording, loop);
        y = card(g, "Events", y, width, events.size() * 17 + 2, (top) -> {
            int lineY = top + 13;
            for (String line : events) {
                boolean warning = line.contains("disconnected");
                text(g, ellipsize(g, line, Theme.text(12f), width - 24), PAD + 12, lineY, Theme.text(12f),
                        warning ? Theme.AMBER : Theme.MUTED);
                lineY += 17;
            }
        });
        return y + PAD;
    }

    /** The live-drive cards: status (headline in color) and the key layout. */
    private int liveCard(Graphics2D g, int y, int width) {
        String headline = liveStatus.get(0);
        Color headlineColor = headline.startsWith(">> INIT") ? Theme.AMBER : Theme.GREEN;
        List<String> rest = liveStatus.subList(1, liveStatus.size());
        y = card(g, "Live", y, width, 24 + rest.size() * 17, (top) -> {
            text(g, headline.replace(">>", "").replace("<<", "").trim(), PAD + 12, top + 17, Theme.bold(15f), headlineColor);
            int lineY = top + 40;
            for (String line : rest) {
                Color color = line.startsWith("Record ON") || line.startsWith("Recording") ? Theme.RED
                        : line.startsWith("Controllers are off") || line.contains("failed") ? Theme.AMBER : Theme.MUTED;
                text(g, ellipsize(g, line, Theme.text(12f), width - 24), PAD + 12, lineY, Theme.text(12f), color);
                lineY += 17;
            }
        });
        if (liveHelp == null || liveHelp.isEmpty()) {
            return y;
        }
        y += CARD_GAP;
        return card(g, "Controls", y, width, liveHelp.size() * 15, (top) -> {
            int lineY = top + 11;
            Font mono = Theme.mono(11f);
            for (String line : liveHelp) {
                boolean heading = !line.startsWith(" ");
                text(g, ellipsize(g, line, mono, width - 24), PAD + 12, lineY, mono, heading ? Theme.TEXT : Theme.MUTED);
                lineY += 15;
            }
        });
    }

    /** Something that draws a card's contents, given the y where the contents start. */
    private interface Contents {
        void paint(int top);
    }

    /** A card: dark box, hairline frame, spaced title. Returns the y just below it. */
    private int card(Graphics2D g, String title, int y, int width, int contentHeight, Contents contents) {
        int height = 30 + contentHeight + 10;
        g.setColor(Theme.PANEL);
        g.fill(new Rectangle2D.Double(PAD, y, width, height));
        g.setColor(Theme.LINE);
        g.setStroke(new BasicStroke(1f));
        g.draw(new Rectangle2D.Double(PAD + 0.5, y + 0.5, width - 1, height - 1));
        // Accent tick at the top-left, then the title.
        g.setColor(Theme.ACCENT);
        g.fill(new Rectangle2D.Double(PAD, y, 2, 14));
        Theme.drawLabel(g, title, PAD + 12, y + 18, 10f, Theme.MUTED);
        g.setColor(Theme.LINE);
        g.draw(new Line2D.Double(PAD + 12, y + 25.5, PAD + width - 12, y + 25.5));
        contents.paint(y + 30);
        return y + height;
    }

    /** A small label over a value. */
    private void readout(Graphics2D g, int x, int top, String label, String value, Color color) {
        Theme.drawLabel(g, label, x, top + 10, 9f, Theme.FAINT);
        text(g, value, x, top + 31, Theme.text(15f), color);
    }

    /** A small label over a big number and its unit. */
    private void bigReadout(Graphics2D g, int x, int top, String label, String value, String unit) {
        Theme.drawLabel(g, label, x, top + 10, 9f, Theme.FAINT);
        Font big = Theme.number(30f);
        text(g, value, x, top + 44, big, Theme.TEXT);
        int valueWidth = g.getFontMetrics(big).stringWidth(value);
        Theme.drawLabel(g, unit, x + valueWidth + 4, top + 44, 9f, Theme.MUTED);
    }

    /**
     * One gauge row: name, a thin track with the value filled in, and the number. Centered
     * gauges ({@code centered}) go from -1 to 1 with 0 in the middle; others from 0 to 1.
     */
    private void gaugeRow(Graphics2D g, int top, int width, String name, double value, boolean centered, String number) {
        int x = PAD + 12;
        // "frontLeft" reads as "FRONT LEFT".
        Theme.drawLabel(g, name.replaceAll("([a-z0-9])([A-Z])", "$1 $2"), x, top + 14, 10f, Theme.MUTED);
        int trackX = PAD + 12 + width * 2 / 5;
        int trackWidth = width - (trackX - PAD) - 70;
        int trackY = top + 9;
        g.setColor(Theme.RAISED);
        g.fill(new Rectangle2D.Double(trackX, trackY, trackWidth, 4));
        double clamped = Math.max(centered ? -1 : 0, Math.min(1, value));
        g.setColor(clamped == 0 ? Theme.FAINT : Theme.ACCENT);
        if (centered) {
            double mid = trackX + trackWidth / 2.0;
            double end = mid + clamped * trackWidth / 2.0;
            g.fill(new Rectangle2D.Double(Math.min(mid, end), trackY, Math.max(1, Math.abs(end - mid)), 4));
            g.setColor(Theme.MUTED);
            g.fill(new Rectangle2D.Double(mid - 0.5, trackY - 3, 1, 10));
        } else {
            g.fill(new Rectangle2D.Double(trackX, trackY, Math.max(1, clamped * trackWidth), 4));
        }
        Font mono = Theme.mono(12f);
        int numberWidth = g.getFontMetrics(mono).stringWidth(number);
        text(g, number, PAD + width - 12 - numberWidth, top + 14, mono, clamped == 0 ? Theme.MUTED : Theme.TEXT);
    }

    private static void text(Graphics2D g, String text, float x, float y, Font font, Color color) {
        g.setFont(font);
        g.setColor(color);
        g.drawString(text, x, y);
    }

    /** {@code text} shortened with "..." to fit {@code maxWidth} pixels in {@code font}. */
    private static String ellipsize(Graphics2D g, String text, Font font, int maxWidth) {
        FontMetrics metrics = g.getFontMetrics(font);
        if (metrics.stringWidth(text) <= maxWidth) {
            return text;
        }
        String shorter = text;
        while (!shorter.isEmpty() && metrics.stringWidth(shorter + "...") > maxWidth) {
            shorter = shorter.substring(0, shorter.length() - 1);
        }
        return shorter + "...";
    }

    // ---- Data for the cards ------------------------------------------------------------------------

    /** The last Driver Station screen sent at or before loop {@code at}. */
    private List<String> driverStationLines(int at) {
        for (int i = at; i >= 0; i--) {
            List<RunRecording.TelemetryScreen> screens = view.loop(i).telemetry();
            if (!screens.isEmpty()) {
                RunRecording.TelemetryScreen screen = screens.get(screens.size() - 1);
                List<String> lines = new ArrayList<>(screen.lines());
                for (String log : screen.log()) {
                    lines.add("log: " + log);
                }
                return lines;
            }
        }
        return List.of();
    }

    /** " · SENT 1.24 S" for the card title, or "" if nothing was sent yet. */
    private String screenTime(int at) {
        for (int i = at; i >= 0; i--) {
            if (!view.loop(i).telemetry().isEmpty()) {
                return String.format(Locale.ROOT, "  ·  sent %.2f s", view.loop(i).matchTimeSeconds());
            }
        }
        return "";
    }

    /** Rumble counts and every marked event up to this moment. */
    private static List<String> eventLines(RunRecording recording, RunRecording.LoopRecord loop) {
        List<String> lines = new ArrayList<>();
        lines.add("Gamepad 1 rumbles: " + rumblesSoFar(recording.gamepad1Rumbles(), loop)
                + "    Gamepad 2 rumbles: " + rumblesSoFar(recording.gamepad2Rumbles(), loop));
        for (RunRecording.Event event : recording.events()) {
            if (event.sinceInitSeconds() <= loop.sinceInitSeconds() + 1e-9) {
                lines.add(String.format(Locale.ROOT, "T+%6.2f  %s", event.matchTimeSeconds(), event.text()));
            }
        }
        return lines;
    }

    private static int rumblesSoFar(List<RumbleRecord> rumbles, RunRecording.LoopRecord loop) {
        int count = 0;
        for (RumbleRecord rumble : rumbles) {
            if (rumble.matchTimeSeconds() <= loop.matchTimeSeconds() + 1e-9) {
                count++;
            }
        }
        return count;
    }

    // ---- Scrolling ---------------------------------------------------------------------------------

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return new Dimension(WIDTH, 600);
    }

    @Override
    public int getScrollableUnitIncrement(java.awt.Rectangle visible, int orientation, int direction) {
        return 16;
    }

    @Override
    public int getScrollableBlockIncrement(java.awt.Rectangle visible, int orientation, int direction) {
        return orientation == SwingConstants.VERTICAL ? visible.height - 32 : visible.width;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }
}
