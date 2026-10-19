package pvi.harness.viewer;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JComponent;

/**
 * The bottom-right corner of the viewer window: pictures of the driver's and operator's
 * gamepads at the shown moment, with held buttons lit (drawn by {@link GamepadIndicator}).
 * Must only be used on the Swing thread (the window does that).
 */
public final class GamepadPanel extends JComponent {
    /** Space around the two controllers, in pixels. */
    static final int INSET_PIXELS = 8;

    /** The recording being shown. */
    private RecordingView view;
    /** Which loop entry is shown. */
    private int index;

    /** A panel showing the gamepads at the first moment of {@code view}. */
    public GamepadPanel(RecordingView view) {
        this.view = view;
        int width = (int) Math.ceil(2 * GamepadIndicator.CONTROLLER_WIDTH * GamepadIndicator.SCALE) + 2 * INSET_PIXELS;
        int height = (int) Math.ceil(GamepadIndicator.CONTROLLER_HEIGHT * GamepadIndicator.SCALE) + 2 * INSET_PIXELS;
        setPreferredSize(new Dimension(width, height));
    }

    /** Shows the gamepads at loop entry {@code index} and redraws. */
    public void show(int index) {
        view.loop(index); // fails clearly if the index is outside the recording
        this.index = index;
        repaint();
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
            GamepadIndicator.paint(g, getWidth() - INSET_PIXELS, getHeight() - INSET_PIXELS, view.loop(index));
        } finally {
            g.dispose();
        }
    }
}
