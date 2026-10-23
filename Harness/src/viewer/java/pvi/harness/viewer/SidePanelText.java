package pvi.harness.viewer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import pvi.harness.ds.RumbleRecord;
import pvi.harness.runtime.RunRecording;

/**
 * The text of the viewer's side panel for one moment of a recording: what was running, the
 * time and stage, where the robot was, motor powers, servo positions, the Driver Station screen
 * and rumbles so far.
 *
 * <p>Plain text, worked out without any window, so tests can check exactly what the panel
 * shows. The window just puts these lines in a text box.</p>
 */
public final class SidePanelText {
    /** Only static methods here; nothing to create. */
    private SidePanelText() {
    }

    /** Every line of the panel for loop {@code index}, top to bottom. */
    public static List<String> describe(RecordingView view, int index) {
        RunRecording recording = view.recording();
        RunRecording.Header header = recording.header();
        RunRecording.LoopRecord loop = view.loop(index);
        List<String> lines = new ArrayList<>();

        lines.add(header.opMode() + "   " + (header.period() == null ? "period unknown" : header.period())
                + "   robot: " + header.robotConfig());
        lines.add(String.format(Locale.ROOT, "Stage: %-10s Loop %d of %d", loop.stage(), index + 1, view.size()));
        lines.add(String.format(Locale.ROOT, "Match time: %.2f s   (since INIT %.2f s)",
                loop.matchTimeSeconds(), loop.sinceInitSeconds()));
        lines.add("Robot: " + (loop.pose() == null ? "position not tracked" : loop.pose().toString()));
        lines.add("Auto step: " + (loop.autoStep() == null ? "-" : loop.autoStep()));

        lines.add("");
        lines.add("Motors (power)");
        for (RunRecording.MotorState motor : loop.motors()) {
            lines.add(String.format(Locale.ROOT, "  %-12s %6.2f", motor.name(), motor.power()));
        }
        if (!loop.servos().isEmpty()) {
            lines.add("Servos (position)");
            for (RunRecording.ServoState servo : loop.servos()) {
                lines.add(servo.isSet()
                        ? String.format(Locale.ROOT, "  %-12s %6.2f", servo.name(), servo.position())
                        : String.format(Locale.ROOT, "  %-12s %6s", servo.name(), "not set"));
            }
        }

        lines.add("");
        addDriverStation(lines, view, index);

        lines.add("");
        addRumbles(lines, "Gamepad 1", recording.gamepad1Rumbles(), loop);
        addRumbles(lines, "Gamepad 2", recording.gamepad2Rumbles(), loop);
        addEvents(lines, recording.events(), loop);
        return lines;
    }

    /** Marked moments (e.g. a controller disconnecting) up to this moment, if there were any. */
    private static void addEvents(List<String> lines, List<RunRecording.Event> events, RunRecording.LoopRecord loop) {
        List<RunRecording.Event> soFar = events.stream()
                .filter(event -> event.sinceInitSeconds() <= loop.sinceInitSeconds() + 1e-9)
                .toList();
        if (soFar.isEmpty()) {
            return;
        }
        lines.add("");
        lines.add("Events so far");
        for (RunRecording.Event event : soFar) {
            lines.add(String.format(Locale.ROOT, "  %.2f s  %s", event.matchTimeSeconds(), event.text()));
        }
    }

    /**
     * The Driver Station screen as it looked at this moment: the last screen sent at or before
     * this loop (the Driver Station keeps showing a screen until a new one arrives).
     */
    private static void addDriverStation(List<String> lines, RecordingView view, int index) {
        for (int i = index; i >= 0; i--) {
            List<RunRecording.TelemetryScreen> screens = view.loop(i).telemetry();
            if (!screens.isEmpty()) {
                RunRecording.TelemetryScreen screen = screens.get(screens.size() - 1);
                lines.add(String.format(Locale.ROOT, "Driver Station (sent at %.2f s)", view.loop(i).matchTimeSeconds()));
                for (String line : screen.lines()) {
                    lines.add("  " + line);
                }
                for (String line : screen.log()) {
                    lines.add("  log: " + line);
                }
                return;
            }
        }
        lines.add("Driver Station: nothing sent yet");
    }

    /** How many rumbles a gamepad has had up to this moment, and the latest one. */
    private static void addRumbles(List<String> lines, String gamepad, List<RumbleRecord> rumbles,
                                   RunRecording.LoopRecord loop) {
        RumbleRecord latest = null;
        int count = 0;
        for (RumbleRecord rumble : rumbles) {
            // Only rumbles sent up to this moment (ones sent before PLAY are stamped 0 s).
            if (rumble.matchTimeSeconds() <= loop.matchTimeSeconds() + 1e-9) {
                latest = rumble;
                count++;
            }
        }
        if (latest == null) {
            lines.add(gamepad + " rumbles: none yet");
        } else {
            lines.add(String.format(Locale.ROOT, "%s rumbles: %d (latest %d ms, %d pulse%s at %.2f s)",
                    gamepad, count, latest.totalDurationMs(), latest.pulses(), latest.pulses() == 1 ? "" : "s",
                    latest.matchTimeSeconds()));
        }
    }
}
