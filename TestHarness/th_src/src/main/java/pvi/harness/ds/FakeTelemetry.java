package pvi.harness.ds;

import org.firstinspires.ftc.robotcore.external.Func;
import org.firstinspires.ftc.robotcore.external.Telemetry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

/**
 * Captures what an OpMode would show on the Driver Station.
 *
 * <p>Follows the SDK's rules: {@code addData} items collect until {@code update()} sends
 * them as one frame; with auto-clear on (the default) non-retained items are then
 * removed; {@code Func} values are evaluated when the frame is sent. Every frame is kept,
 * so tests can look back through the whole run.</p>
 *
 * <p>How it is organised inside: telemetry is a list of <b>lines</b>; each line has an
 * optional caption and a list of <b>items</b> ("caption : value"). A plain
 * {@code telemetry.addData("Speed", 0.5)} makes a new line holding one item.</p>
 */
public class FakeTelemetry implements Telemetry {
    /** Lines added since the last clear, in order. This is the "pending" screen. */
    private final List<LineImpl> lines = new ArrayList<>();
    /** Every frame sent by update(), oldest first. */
    private final List<TelemetryFrame> frames = new ArrayList<>();
    /** The scrolling message log shown under the data. */
    private final LogImpl log = new LogImpl();
    /** Where frame timestamps come from; the runner points this at its simulated clock. */
    private DoubleSupplier matchTime = () -> 0.0;
    /** True when something changed since the last update(); the runner sends dirty telemetry for the OpMode. */
    private boolean dirty;
    /** SDK setting: clear non-retained items after each update(). On by default, as in the SDK. */
    private boolean autoClear = true;
    /** SDK setting: how often the real app sends to the Driver Station. Stored only; the fake sends every update. */
    private int msTransmissionInterval = 250;
    /** Text between items on the same line (SDK default " | "). */
    private String itemSeparator = " | ";
    /** Text between a caption and its value (SDK default " : "). */
    private String captionValueSeparator = " : ";

    /** Lets the runner stamp each frame with simulated match time. */
    public void setMatchTimeSource(DoubleSupplier matchTime) {
        this.matchTime = matchTime;
    }

    // ---- Test helpers -------------------------------------------------------

    /** Every frame sent so far, oldest first. */
    public List<TelemetryFrame> frames() {
        return Collections.unmodifiableList(frames);
    }

    /** The frame most recently sent with update(), or an empty frame if none was sent. */
    public TelemetryFrame lastFrame() {
        return frames.isEmpty()
                ? new TelemetryFrame(0.0, List.of(), Map.of(), List.of())
                : frames.get(frames.size() - 1);
    }

    /** A value from the last sent frame, or null when that caption was not sent. */
    public String lastValue(String caption) {
        return lastFrame().value(caption);
    }

    /** What would be sent if update() were called now. */
    public TelemetryFrame pendingFrame() {
        return compose();
    }

    /** How many frames have been sent. */
    public int getUpdateCount() {
        return frames.size();
    }

    /** True when something was added or changed since the last update(). */
    public boolean isDirty() {
        return dirty;
    }

    // ---- Telemetry: adding data ---------------------------------------------
    // Each top-level addData starts a new line with one item, as the SDK does.

    /** {@code addData("Mode", "%s-%d", "Auto", 2)}: a formatted value, like String.format. */
    @Override
    public Item addData(String caption, String format, Object... args) {
        return newLine("").addData(caption, format, args);
    }

    /** {@code addData("Speed", 0.5)}: any value, shown with String.valueOf. */
    @Override
    public Item addData(String caption, Object value) {
        return newLine("").addData(caption, value);
    }

    /** {@code addData("Pose", () -> robot.getPose())}: a value read fresh each time a frame is sent. */
    @Override
    public <T> Item addData(String caption, Func<T> valueProducer) {
        return newLine("").addData(caption, valueProducer);
    }

    /** A formatted value read fresh each time a frame is sent. */
    @Override
    public <T> Item addData(String caption, String format, Func<T> valueProducer) {
        return newLine("").addData(caption, format, valueProducer);
    }

    /** An empty line, to which items can be added. */
    @Override
    public Line addLine() {
        return newLine("");
    }

    /** A line starting with text, e.g. a heading; items can be added after it. */
    @Override
    public Line addLine(String lineCaption) {
        return newLine(lineCaption);
    }

    /** Removes one item from whichever line holds it. */
    @Override
    public boolean removeItem(Item item) {
        for (LineImpl line : lines) {
            if (line.items.remove(item)) {
                dirty = true;
                return true;
            }
        }
        return false;
    }

    /** Removes a whole line. */
    @Override
    public boolean removeLine(Line line) {
        boolean removed = lines.remove(line);
        dirty |= removed;
        return removed;
    }

    // ---- Telemetry: sending and clearing -----------------------------------

    /** Sends the current screen as one frame (saved in the history), then auto-clears if enabled. */
    @Override
    public boolean update() {
        frames.add(compose());
        dirty = false;
        if (autoClear) {
            clear();
            dirty = false;   // clearing after a send is not a new change
        }
        return true;
    }

    /** Removes every item that isn't retained, and any line left empty. The log is kept. */
    @Override
    public void clear() {
        Iterator<LineImpl> lineIterator = lines.iterator();
        while (lineIterator.hasNext()) {
            LineImpl line = lineIterator.next();
            line.items.removeIf(item -> !item.retained);
            if (line.items.isEmpty()) {
                lineIterator.remove();
            }
        }
        dirty = true;
    }

    /** Removes everything, retained items and the log included. */
    @Override
    public void clearAll() {
        lines.clear();
        log.clear();
        dirty = true;
    }

    // ---- Telemetry: settings and features the harness doesn't simulate ------

    /** Actions run by the real app before each send; not simulated. Returns a token anyway. */
    @Override public Object addAction(Runnable action) { return action; }
    /** Actions are not simulated, so there is nothing to remove. */
    @Override public boolean removeAction(Object token) { return false; }
    /** Text-to-speech on the Driver Station; silent in the harness. */
    @Override public void speak(String text) { }
    /** Text-to-speech in a chosen language; silent in the harness. */
    @Override public void speak(String text, String languageCode, String countryCode) { }
    /** Whether update() clears non-retained items. */
    @Override public boolean isAutoClear() { return autoClear; }
    /** Turns auto-clear on or off, as in the SDK. */
    @Override public void setAutoClear(boolean autoClear) { this.autoClear = autoClear; }
    /** The stored transmission interval. */
    @Override public int getMsTransmissionInterval() { return msTransmissionInterval; }
    /** Stores the transmission interval; the fake still records every update(). */
    @Override public void setMsTransmissionInterval(int ms) { msTransmissionInterval = ms; }
    /** Text between items on one line. */
    @Override public String getItemSeparator() { return itemSeparator; }
    /** Changes the text between items on one line. */
    @Override public void setItemSeparator(String separator) { itemSeparator = separator; }
    /** Text between a caption and its value. */
    @Override public String getCaptionValueSeparator() { return captionValueSeparator; }
    /** Changes the text between a caption and its value. */
    @Override public void setCaptionValueSeparator(String separator) { captionValueSeparator = separator; }
    /** Classic or HTML display; the fake always records plain text. */
    @Override public void setDisplayFormat(DisplayFormat displayFormat) { }
    /** The telemetry log (scrolling messages). */
    @Override public Log log() { return log; }

    // ---- Internals -----------------------------------------------------------

    /** Adds a new empty line with an optional caption and marks telemetry as changed. */
    private LineImpl newLine(String caption) {
        LineImpl line = new LineImpl(caption);
        lines.add(line);
        dirty = true;
        return line;
    }

    /**
     * Builds a frame from the current lines: the text a driver would read, plus a
     * caption-to-value map for easy test lookups. Func values are evaluated here.
     */
    private TelemetryFrame compose() {
        List<String> text = new ArrayList<>();
        Map<String, String> values = new LinkedHashMap<>();
        for (LineImpl line : lines) {
            StringBuilder builder = new StringBuilder(line.caption);
            for (int i = 0; i < line.items.size(); i++) {
                ItemImpl item = line.items.get(i);
                String value = item.render();
                values.put(item.caption, value);
                if (i > 0) {
                    builder.append(itemSeparator);
                }
                builder.append(item.caption).append(captionValueSeparator).append(value);
            }
            // A captioned line with no items (e.g. a menu entry) is still findable by its text.
            if (line.items.isEmpty() && !line.caption.isEmpty()) {
                values.putIfAbsent(line.caption, "");
            }
            text.add(builder.toString());
        }
        return new TelemetryFrame(matchTime.getAsDouble(), List.copyOf(text),
                Collections.unmodifiableMap(values), List.copyOf(log.entries));
    }

    /** String.format, in one place so every formatted value uses the same rules. */
    private static String format(String format, Object... args) {
        return String.format(format, args);
    }

    /** One telemetry line: an optional caption followed by items. */
    private final class LineImpl implements Line {
        /** Text at the start of the line (often empty). */
        private final String caption;
        /** The "caption : value" items on this line, in order. */
        private final List<ItemImpl> items = new ArrayList<>();

        LineImpl(String caption) {
            this.caption = caption;
        }

        /** Adds an item whose value comes from {@code value} and marks telemetry as changed. */
        private ItemImpl add(String caption, Supplier<String> value) {
            ItemImpl item = new ItemImpl(this, caption, value);
            items.add(item);
            dirty = true;
            return item;
        }

        /** Formatted value, fixed now. */
        @Override
        public Item addData(String caption, String format, Object... args) {
            String value = format(format, args);
            return add(caption, () -> value);
        }

        /** Plain value, fixed now. */
        @Override
        public Item addData(String caption, Object value) {
            String text = String.valueOf(value);
            return add(caption, () -> text);
        }

        /** Value read each time a frame is sent. */
        @Override
        public <T> Item addData(String caption, Func<T> valueProducer) {
            return add(caption, () -> String.valueOf(valueProducer.value()));
        }

        /** Formatted value read each time a frame is sent. */
        @Override
        public <T> Item addData(String caption, String format, Func<T> valueProducer) {
            return add(caption, () -> format(format, valueProducer.value()));
        }
    }

    /** One "caption : value" entry. The value is a Supplier so Func values stay live. */
    private final class ItemImpl implements Item {
        /** The line this item belongs to (items can add more items to the same line). */
        private final LineImpl line;
        /** The label shown before the value. */
        private String caption;
        /** Produces the value text when a frame is composed. */
        private Supplier<String> value;
        /** True when the item should survive clear() (SDK "retained" items). */
        private boolean retained;

        ItemImpl(LineImpl line, String caption, Supplier<String> value) {
            this.line = line;
            this.caption = caption;
            this.value = value;
        }

        /** The value text right now. */
        String render() {
            return value.get();
        }

        /** The item's label. */
        @Override public String getCaption() { return caption; }

        /** Changes the label. */
        @Override
        public Item setCaption(String caption) {
            this.caption = caption;
            dirty = true;
            return this;
        }

        /** Replaces the value with a formatted one, fixed now. */
        @Override
        public Item setValue(String format, Object... args) {
            String text = format(format, args);
            value = () -> text;
            dirty = true;
            return this;
        }

        /** Replaces the value, fixed now. */
        @Override
        public Item setValue(Object value) {
            String text = String.valueOf(value);
            this.value = () -> text;
            dirty = true;
            return this;
        }

        /** Replaces the value with one read each time a frame is sent. */
        @Override
        public <T> Item setValue(Func<T> valueProducer) {
            value = () -> String.valueOf(valueProducer.value());
            dirty = true;
            return this;
        }

        /** Replaces the value with a formatted one read each time a frame is sent. */
        @Override
        public <T> Item setValue(String format, Func<T> valueProducer) {
            value = () -> format(format, valueProducer.value());
            dirty = true;
            return this;
        }

        /** Keeps (true) or stops keeping (false) this item across clear(). Null counts as false. */
        @Override
        public Item setRetained(Boolean retained) {
            this.retained = retained != null && retained;
            return this;
        }

        /** Whether this item survives clear(). */
        @Override public boolean isRetained() { return retained; }

        /** Adds another item on the same line (formatted value). */
        @Override public Item addData(String caption, String format, Object... args) { return line.addData(caption, format, args); }
        /** Adds another item on the same line. */
        @Override public Item addData(String caption, Object value) { return line.addData(caption, value); }
        /** Adds another live item on the same line. */
        @Override public <T> Item addData(String caption, Func<T> valueProducer) { return line.addData(caption, valueProducer); }
        /** Adds another live formatted item on the same line. */
        @Override public <T> Item addData(String caption, String format, Func<T> valueProducer) { return line.addData(caption, format, valueProducer); }
    }

    /** The telemetry log: a short list of messages that keeps only the newest ones. */
    private final class LogImpl implements Log {
        /** Messages, oldest first. */
        private final List<String> entries = new ArrayList<>();
        /** Most messages kept (SDK default 9). */
        private int capacity = 9;
        /** Display order setting; stored only (frames always list oldest first). */
        private DisplayOrder displayOrder = DisplayOrder.OLDEST_FIRST;

        /** How many messages are kept. */
        @Override public int getCapacity() { return capacity; }

        /** Changes how many messages are kept, dropping the oldest if needed. */
        @Override
        public void setCapacity(int capacity) {
            this.capacity = capacity;
            trim();
        }

        /** The stored display order. */
        @Override public DisplayOrder getDisplayOrder() { return displayOrder; }
        /** Stores the display order. */
        @Override public void setDisplayOrder(DisplayOrder displayOrder) { this.displayOrder = displayOrder; }

        /** Adds a message, dropping the oldest if over capacity. */
        @Override
        public void add(String entry) {
            entries.add(entry);
            trim();
            dirty = true;
        }

        /** Adds a formatted message. */
        @Override
        public void add(String format, Object... args) {
            add(format(format, args));
        }

        /** Removes every message. */
        @Override
        public void clear() {
            entries.clear();
            dirty = true;
        }

        /** Drops the oldest messages until within capacity. */
        private void trim() {
            while (entries.size() > capacity) {
                entries.remove(0);
            }
        }
    }
}
