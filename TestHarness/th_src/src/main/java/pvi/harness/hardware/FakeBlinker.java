package pvi.harness.hardware;

import com.qualcomm.robotcore.hardware.Blinker;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * A hub's status light, which robot code finds with
 * {@code hardwareMap.get(Blinker.class, "Control Hub")}. It only remembers the pattern it was
 * told to show, so a test can read it back; nothing draws it.
 *
 * <p>Add one with {@code hardwareMap.addBlinker("Control Hub")}, or list it in
 * {@code devices.blinkers}.</p>
 */
public class FakeBlinker extends FakeDevice implements Blinker {
    /** The most steps a real hub's light takes in one pattern. */
    private static final int MAX_PATTERN_STEPS = 16;

    private List<Step> pattern = List.of();
    private final Deque<List<Step>> pushedPatterns = new ArrayDeque<>();

    public FakeBlinker(String name) {
        super(name);
    }

    @Override public void setPattern(Collection<Step> steps) {
        pattern = steps == null ? List.of() : new ArrayList<>(steps);
    }

    @Override public Collection<Step> getPattern() {
        return new ArrayList<>(pattern);
    }

    @Override public void pushPattern(Collection<Step> steps) {
        pushedPatterns.push(pattern);
        setPattern(steps);
    }

    @Override public boolean patternStackNotEmpty() {
        return !pushedPatterns.isEmpty();
    }

    @Override public boolean popPattern() {
        if (pushedPatterns.isEmpty()) {
            pattern = List.of();
            return false;
        }
        pattern = pushedPatterns.pop();
        return true;
    }

    /** One step that stays lit: the SDK's helper for a single color. */
    @Override public void setConstant(int color) {
        setPattern(List.of(new Step(color, 1, TimeUnit.SECONDS)));
    }

    @Override public void stopBlinking() {
        setPattern(List.of());
    }

    @Override public int getBlinkerPatternMaxLength() {
        return MAX_PATTERN_STEPS;
    }
}
