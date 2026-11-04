package pvi.harness.viewer;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

import org.junit.jupiter.api.Test;

import pvi.harness.runtime.RunRecording;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The mission-control look: the telemetry cards and the header clock. Drawn off-screen, so no
 * window is needed.
 */
class TelemetryPanelTest {

    /**
     * WHAT: The clock shows minutes, seconds and hundredths, with leading zeros.
     *
     * <p>WHY: The header's T+ clock and the time label use it; a 2:00 match must read 02:00.00.</p>
     */
    @Test
    void clockFormatsMatchTime() {
        assertEquals("00:00.00", MissionHeader.clock(0));
        assertEquals("01:23.46", MissionHeader.clock(83.456));
        assertEquals("02:00.00", MissionHeader.clock(120));
        assertEquals("00:00.00", MissionHeader.clock(-1), "never negative");
    }

    /**
     * WHAT: The telemetry panel draws a real run, and a motor that is driving shows its gauge in
     * the accent color, both while replaying and with the live-drive cards on top.
     *
     * <p>WHY: The cards are drawn by hand; this catches a crash or an empty panel.</p>
     */
    @Test
    void drawsARunWithLiveCards() {
        RunRecording recording = PviScenarios.teleOpDemoDrive().perform();
        RecordingView view = new RecordingView(recording);
        int driving = 0;
        for (int i = 0; i < view.size(); i++) {
            if (Math.abs(view.loop(i).motors().get(0).power()) > 0.3) {
                driving = i;
                break;
            }
        }
        TelemetryPanel panel = new TelemetryPanel(view);
        panel.show(driving);
        assertTrue(hasAccent(paint(panel)), "a driving motor's gauge should be filled in the accent color");

        panel.setLive(List.of(">> DRIVING: 1:52 left, Esc to stop <<", "Practice: not saved"), KeyboardGamepads.help());
        assertTrue(hasAccent(paint(panel)));
        assertTrue(panel.getPreferredSize().height > 600, "the live cards make the column taller");
    }

    /** Paints the panel into an image at its preferred size (twice, so the height settles). */
    private static BufferedImage paint(TelemetryPanel panel) {
        BufferedImage image = null;
        for (int pass = 0; pass < 2; pass++) {
            panel.setSize(TelemetryPanel.WIDTH, panel.getPreferredSize().height);
            image = new BufferedImage(panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            panel.paint(g);
            g.dispose();
        }
        return image;
    }

    private static boolean hasAccent(BufferedImage image) {
        int accent = Theme.ACCENT.getRGB();
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (image.getRGB(x, y) == accent) {
                    return true;
                }
            }
        }
        return false;
    }
}
