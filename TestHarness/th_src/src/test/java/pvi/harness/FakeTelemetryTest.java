package pvi.harness;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.junit.jupiter.api.Test;

import java.util.List;

import pvi.harness.ds.FakeTelemetry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Phase 2: the telemetry fake follows the SDK's rules.
 *
 * <p>These test the <b>harness's</b> fake telemetry directly, with no OpMode. Robot tests
 * rely on reading telemetry, so the fake must behave like the real Driver Station
 * telemetry.</p>
 */
class FakeTelemetryTest {

    /**
     * WHAT: update() sends everything added so far as one screen, then clears it.
     *
     * <p>HOW: Adds a plain value and a formatted value, calls update(), and checks the sent
     * frame: values by caption, the exact screen lines, and that nothing is left pending.</p>
     *
     * <p>WHY: This is the SDK's default "auto-clear" behavior. If the fake kept old lines,
     * tests could read stale values and pass when they shouldn't.</p>
     */
    @Test
    void updateSendsAFrameAndAutoClears() {
        // SET UP: two items, one using a format string like String.format.
        FakeTelemetry telemetry = new FakeTelemetry();
        telemetry.addData("Speed", 0.5);
        telemetry.addData("Mode", "%s-%d", "Auto", 2);

        // RUN
        telemetry.update();

        // CHECK: values, the lines as a driver would read them, and nothing left over.
        assertEquals("0.5", telemetry.lastValue("Speed"));
        assertEquals("Auto-2", telemetry.lastValue("Mode"));
        assertEquals(List.of("Speed : 0.5", "Mode : Auto-2"), telemetry.lastFrame().lines());
        assertFalse(telemetry.isDirty());
        assertEquals(0, telemetry.pendingFrame().lines().size());
    }

    /**
     * WHAT: "Retained" items stay across updates, and function values are read at send time.
     *
     * <p>HOW: Adds a retained item whose value comes from a function, plus a normal item.
     * Changes the number between two updates. The retained item should show 5 then 7; the
     * normal item should be gone after the first update.</p>
     *
     * <p>WHY: OpModes use {@code setRetained(true)} for always-on lines and functions for
     * live values. The fake has to match the SDK or those OpModes would look wrong in tests.</p>
     */
    @Test
    void retainedItemsSurviveAndFuncValuesAreReadAtUpdate() {
        // SET UP: a live counter (retained) and a one-time item.
        FakeTelemetry telemetry = new FakeTelemetry();
        int[] counter = {0};
        telemetry.addData("Counter", () -> counter[0]).setRetained(true);
        telemetry.addData("Temporary", "x");

        // RUN: two updates with different counter values.
        counter[0] = 5;
        telemetry.update();
        counter[0] = 7;
        telemetry.update();

        // CHECK: the counter was read at each update; the temporary item was cleared.
        assertEquals("5", telemetry.frames().get(0).value("Counter"));
        assertEquals("7", telemetry.lastValue("Counter"));
        assertNull(telemetry.lastValue("Temporary"));
    }

    /**
     * WHAT: Several items can share one line.
     *
     * <p>HOW: Adds a line captioned "Pose " with x and y items, and checks it prints as one
     * line with the SDK's separators, while each value can still be looked up on its own.</p>
     *
     * <p>WHY: Compact multi-value lines (like a pose) are common; tests must be able to read
     * them.</p>
     */
    @Test
    void linesGroupSeveralItems() {
        // SET UP: one line holding two items.
        FakeTelemetry telemetry = new FakeTelemetry();
        Telemetry.Line pose = telemetry.addLine("Pose ");
        pose.addData("x", 1).addData("y", 2);

        // RUN
        telemetry.update();

        // CHECK: one line on screen, and each value readable by caption.
        assertEquals(List.of("Pose x : 1 | y : 2"), telemetry.lastFrame().lines());
        assertEquals("2", telemetry.lastValue("y"));
    }

    /**
     * WHAT: The telemetry log keeps only the newest entries, up to its capacity.
     *
     * <p>HOW: Sets capacity to 2, adds three entries, and checks only the last two remain.</p>
     *
     * <p>WHY: The log is the scrolling message area under telemetry. Matching the SDK's
     * capacity rule means tests see the same messages the drivers would.</p>
     */
    @Test
    void logKeepsTheNewestEntriesUpToCapacity() {
        // SET UP: room for 2, add 3.
        FakeTelemetry telemetry = new FakeTelemetry();
        telemetry.log().setCapacity(2);
        telemetry.log().add("one");
        telemetry.log().add("two");
        telemetry.log().add("three %d", 3);

        // RUN
        telemetry.update();

        // CHECK: the oldest ("one") was dropped.
        assertEquals(List.of("two", "three 3"), telemetry.lastFrame().log());
    }
}
