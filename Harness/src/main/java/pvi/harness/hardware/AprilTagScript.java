package pvi.harness.hardware;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A timeline of which AprilTags the fake camera sees, in the style of
 * {@link pvi.harness.ds.GamepadScript}.
 *
 * <pre>{@code
 * AprilTagScript tags = new AprilTagScript()
 *         .at(1.5).see(21)                          // tag 21 comes into view 1.5 s after PLAY...
 *         .at(3.0).lose(21)                         // ...and leaves it at 3 s
 *         .at(4.0).see(SeenAprilTag.at(22, 2, 30, 5)); // tag 22, 2 in right, 30 in ahead, 5 in up
 * }</pre>
 *
 * <p>Times in {@link #at(double)} are seconds after PLAY. Use {@link #duringInit()} first for
 * tags seen before PLAY; those times are seconds after INIT.</p>
 *
 * <p>A tag stays in view until the script loses it, the way a tag on a wall stays in view of a
 * robot that isn't moving. {@link #see(int)} puts a tag squarely 24 inches straight ahead
 * ({@link SeenAprilTag#straightAhead(int)}); give a {@link SeenAprilTag} to say exactly where.
 * Seeing a tag that is already in view moves it to the new spot.</p>
 */
public class AprilTagScript {

    /** Which clock an entry's time is measured on: since INIT, or since PLAY. */
    private enum Period { INIT, MATCH }

    /**
     * One scheduled change, e.g. "tag 21 comes into view 1.5 s after PLAY".
     *
     * @param seen  the tag and where it is, or null when the entry removes tags
     * @param lostId the tag to remove; -1 with seen == null means "remove every tag"
     */
    private record Entry(Period period, double seconds, int order, SeenAprilTag seen, int lostId) {
    }

    /** Every scheduled change, in the order it was written. */
    private final List<Entry> entries = new ArrayList<>();
    /** The period new entries go into; MATCH unless duringInit() was called. */
    private Period cursorPeriod = Period.MATCH;
    /** The time new entries go at; moved by at() and after(). */
    private double cursorSeconds;

    /** Following entries happen before PLAY; times are seconds after INIT. */
    public AprilTagScript duringInit() {
        cursorPeriod = Period.INIT;
        cursorSeconds = 0.0;
        return this;
    }

    /** Following entries happen after PLAY; times are seconds after PLAY. This is the default. */
    public AprilTagScript afterStart() {
        cursorPeriod = Period.MATCH;
        cursorSeconds = 0.0;
        return this;
    }

    /** Moves the script cursor to this many seconds after PLAY (or INIT, after duringInit()). */
    public AprilTagScript at(double seconds) {
        if (!(seconds >= 0)) {
            throw new IllegalArgumentException("Script times cannot be negative: " + seconds);
        }
        cursorSeconds = seconds;
        return this;
    }

    /** Moves the script cursor this many seconds after the previous entry. */
    public AprilTagScript after(double seconds) {
        if (!(seconds >= 0)) {
            throw new IllegalArgumentException("after(...) needs a time of 0 or more, but was " + seconds);
        }
        cursorSeconds += seconds;
        return this;
    }

    /** The tag comes into view, squarely 24 inches straight ahead of the camera. */
    public AprilTagScript see(int id) {
        return see(SeenAprilTag.straightAhead(id));
    }

    /** The tag comes into view (or moves) to exactly this spot in front of the camera. */
    public AprilTagScript see(SeenAprilTag tag) {
        if (tag == null) {
            throw new IllegalArgumentException("see(...) needs a tag, not null.");
        }
        entries.add(new Entry(cursorPeriod, cursorSeconds, entries.size(), tag, -1));
        return this;
    }

    /** The tag leaves the camera's view. Losing a tag that isn't in view does nothing. */
    public AprilTagScript lose(int id) {
        if (id < 0) {
            throw new IllegalArgumentException("An AprilTag id cannot be negative: " + id);
        }
        entries.add(new Entry(cursorPeriod, cursorSeconds, entries.size(), null, id));
        return this;
    }

    /** Every tag leaves the camera's view. */
    public AprilTagScript loseAll() {
        entries.add(new Entry(cursorPeriod, cursorSeconds, entries.size(), null, -1));
        return this;
    }

    /**
     * The tags in view at a moment, in the order they came into view.
     *
     * @param sinceInitSeconds  seconds since INIT
     * @param sinceStartSeconds seconds since PLAY, or a negative number before PLAY
     */
    public List<SeenAprilTag> tagsInView(double sinceInitSeconds, double sinceStartSeconds) {
        // Replay every entry that is due, in time order. Scripts are short, so starting over
        // each time keeps this simple and always gives the same answer for the same moment.
        List<Entry> due = new ArrayList<>();
        for (Entry entry : entries) {
            boolean isDue = entry.period() == Period.INIT
                    ? entry.seconds() <= sinceInitSeconds + 1e-9
                    : sinceStartSeconds >= 0 && entry.seconds() <= sinceStartSeconds + 1e-9;
            if (isDue) {
                due.add(entry);
            }
        }
        // INIT entries come before every PLAY entry; then by time; then in the order written.
        due.sort(Comparator.comparing(Entry::period)
                .thenComparingDouble(Entry::seconds)
                .thenComparingInt(Entry::order));

        Map<Integer, SeenAprilTag> inView = new LinkedHashMap<>();
        for (Entry entry : due) {
            if (entry.seen() != null) {
                inView.put(entry.seen().id(), entry.seen());
            } else if (entry.lostId() >= 0) {
                inView.remove(entry.lostId());
            } else {
                inView.clear();
            }
        }
        return List.copyOf(inView.values());
    }
}
