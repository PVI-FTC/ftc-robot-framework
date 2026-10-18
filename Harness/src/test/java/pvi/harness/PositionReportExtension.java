package pvi.harness;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import pvi.harness.runtime.OpModeRunner;
import pvi.harness.runtime.RunnerRegistry;

/**
 * Prints the robot's final estimated position after every test that ran an OpMode.
 *
 * <p>JUnit loads this automatically for every test (see
 * {@code src/test/resources/META-INF/services/org.junit.jupiter.api.extension.Extension} and
 * {@code junit-platform.properties}), so tests don't need any extra code. The text appears in
 * the test's output: under the test name in the terminal, and in Android Studio when you
 * click the test in the Run panel.</p>
 */
public class PositionReportExtension implements BeforeEachCallback, AfterEachCallback {

    /** Before each test: forget the previous test's runners. */
    @Override
    public void beforeEach(ExtensionContext context) {
        RunnerRegistry.startTest();
    }

    /** After each test: print the final position for every runner the test created. */
    @Override
    public void afterEach(ExtensionContext context) {
        for (OpModeRunner runner : RunnerRegistry.runnersThisTest()) {
            System.out.println();
            System.out.println(runner.positionReport());
        }
    }
}
