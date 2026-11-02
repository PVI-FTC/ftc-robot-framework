package pvi.harness.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Remembers which OpModeRunners were created during the current test, so the final-position
 * report can be printed for each one when the test finishes.
 *
 * <p>The test extension {@code PositionReportExtension} calls {@link #startTest()} before
 * each test and reads {@link #runnersThisTest()} after it. Tests never need to use this.</p>
 */
public final class RunnerRegistry {
    /** Runners created since the current test began. Tests run one at a time, so one list is enough. */
    private static final List<OpModeRunner> RUNNERS = new ArrayList<>();

    /** Only static methods here; nothing to create. */
    private RunnerRegistry() {
    }

    /** Forgets the runners from the previous test. */
    public static synchronized void startTest() {
        RUNNERS.clear();
    }

    /** Called by every OpModeRunner when it is created. */
    static synchronized void register(OpModeRunner runner) {
        RUNNERS.add(runner);
    }

    /** Runners created during the current test, in the order they were created. */
    public static synchronized List<OpModeRunner> runnersThisTest() {
        return Collections.unmodifiableList(new ArrayList<>(RUNNERS));
    }
}
