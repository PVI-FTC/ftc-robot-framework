package pvi.harness.viewer;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JComponent;

/**
 * The field drawing inside the viewer window. Shows one moment of a recording, sized to fit
 * the panel and centered in it.
 *
 * <p>All drawing is done by {@link FieldPainter}; this class only picks the size and position.
 * It must only be used on the Swing thread (the window does that).</p>
 */
public final class FieldPanel extends JComponent {
    /** Space left around the field, in pixels, for the caption and legend. */
    static final int MARGIN_PIXELS = 30;
    /** Smallest field size worth drawing, in pixels. */
    static final int MIN_FIELD_PIXELS = 100;

    /** The recording being shown. */
    private RecordingView view;
    /** Which loop entry is shown. */
    private int index;

    /** A panel showing the first moment of {@code view}. */
    public FieldPanel(RecordingView view) {
        this.view = view;
        setPreferredSize(new Dimension(780, 780));
    }

    /** Shows loop entry {@code index} and redraws. */
    public void show(int index) {
        view.loop(index); // fails clearly if the index is outside the recording
        this.index = index;
        repaint();
    }

    /** The loop entry being shown. */
    public int shownIndex() {
        return index;
    }

    /** The largest field (with margins) that fits a panel of this size. */
    static FieldCoordinates coordinatesFor(int widthPixels, int heightPixels) {
        int fieldPixels = Math.max(MIN_FIELD_PIXELS, Math.min(widthPixels, heightPixels) - 2 * MARGIN_PIXELS);
        return new FieldCoordinates(fieldPixels, MARGIN_PIXELS);
    }

    /** Switches to another recording, showing its first moment. */
    public void setView(RecordingView newView) {
        this.view = newView;
        show(0);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setColor(FieldPainter.BACKGROUND);
            g.fillRect(0, 0, getWidth(), getHeight());
            FieldCoordinates coords = coordinatesFor(getWidth(), getHeight());
            // Center the field square in the panel.
            g.translate((getWidth() - coords.totalPixels()) / 2, (getHeight() - coords.totalPixels()) / 2);
            FieldPainter.paint(g, coords, view.box(), view.loop(index).pose(), view.trail(index), view.caption(index),
                    FieldPainter.RobotParts.of(view.loop(index).motors()));
        } finally {
            g.dispose();
        }
    }
}
