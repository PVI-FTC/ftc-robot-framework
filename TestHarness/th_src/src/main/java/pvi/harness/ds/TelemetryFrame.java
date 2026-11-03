package pvi.harness.ds;

import java.util.List;
import java.util.Map;

/**
 * One screen of Driver Station telemetry, as sent by a single {@code telemetry.update()}.
 *
 * @param matchTimeSeconds seconds after PLAY (0 before PLAY)
 * @param lines            the text lines a driver would read, in order
 * @param values           caption to value, for easy test lookups
 * @param log              the telemetry log lines shown under the data
 */
public record TelemetryFrame(double matchTimeSeconds, List<String> lines,
                             Map<String, String> values, List<String> log) {

    /** The value shown for a caption, or null if this frame does not have it. */
    public String value(String caption) {
        return values.get(caption);
    }

    /** True when this frame has the caption. */
    public boolean hasCaption(String caption) {
        return values.containsKey(caption);
    }

    /** True when any line contains the text, e.g. a Pedro menu entry. */
    public boolean containsText(String text) {
        for (String line : lines) {
            if (line.contains(text)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return String.join("\n", lines);
    }
}
