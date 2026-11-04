package pvi.harness.ds;

import com.qualcomm.robotcore.hardware.Gamepad;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/**
 * A timeline of what a driver does with one gamepad.
 *
 * <pre>{@code
 * GamepadScript driver = new GamepadScript()
 *         .at(0.0).stick(Axis.LEFT_STICK_Y, -1.0)   // push forward from PLAY...
 *         .at(2.0).stick(Axis.LEFT_STICK_Y, 0.0)    // ...and let go after 2 seconds
 *         .at(2.5).tap(Button.A);                   // tap A
 * }</pre>
 *
 * <p>Times in {@link #at(double)} are seconds after PLAY. Use {@link #duringInit()} first to
 * script inputs made before PLAY (for example choosing from a Pedro menu); those times are
 * seconds after INIT.</p>
 *
 * <p>Sticks and held buttons keep their value until the script changes them, just like a
 * real gamepad.</p>
 */
public class GamepadScript {
    /** Long enough for several 20 ms loops to see the press, like a quick human tap. */
    public static final double DEFAULT_TAP_SECONDS = 0.10;

    /** Which clock an action's time is measured on: since INIT, or since PLAY. */
    enum Period { INIT, MATCH }

    /** One scheduled change to the gamepad, e.g. "press A at 1.5 s after PLAY". */
    static final class Action {
        /** INIT (before PLAY) or MATCH (after PLAY). */
        final Period period;
        /** Seconds after INIT or PLAY (see period). */
        final double time;
        /** Position in the script, so actions at the same time keep the order they were written. */
        final int order;
        /** Human-readable text, e.g. "press A", for debugging. */
        final String description;
        /** What to do to the gamepad, e.g. set {@code pad.a = true}. */
        final Consumer<Gamepad> apply;

        Action(Period period, double time, int order, String description, Consumer<Gamepad> apply) {
            this.period = period;
            this.time = time;
            this.order = order;
            this.description = description;
            this.apply = apply;
        }
    }

    /** Every scheduled action, in the order it was written. */
    private final List<Action> actions = new ArrayList<>();
    /** The period new actions go into; MATCH unless duringInit() was called. */
    private Period cursorPeriod = Period.MATCH;
    /** The time new actions go at; moved by at(), after(), tap(). */
    private double cursorTime;

    /** Following entries happen before PLAY; times are seconds after INIT. */
    public GamepadScript duringInit() {
        cursorPeriod = Period.INIT;
        cursorTime = 0.0;
        return this;
    }

    /** Following entries happen after PLAY; times are seconds after PLAY. This is the default. */
    public GamepadScript afterStart() {
        cursorPeriod = Period.MATCH;
        cursorTime = 0.0;
        return this;
    }

    /** Moves the script cursor to an absolute time in the current period. */
    public GamepadScript at(double seconds) {
        if (seconds < 0) {
            throw new IllegalArgumentException("Script times cannot be negative: " + seconds);
        }
        cursorTime = seconds;
        return this;
    }

    /** Moves the script cursor forward by some seconds. */
    public GamepadScript after(double seconds) {
        return at(cursorTime + seconds);
    }

    /** Sets a stick or trigger, which stays there until changed. */
    public GamepadScript stick(Axis axis, double value) {
        return add(axis + "=" + value, pad -> axis.set(pad, value));
    }

    /** Presses a button and keeps holding it. */
    public GamepadScript press(Button button) {
        return add("press " + button, pad -> button.set(pad, true));
    }

    /** Releases a held button. */
    public GamepadScript release(Button button) {
        return add("release " + button, pad -> button.set(pad, false));
    }

    /** Presses a button for {@code seconds}, then releases it. The cursor does not move. */
    public GamepadScript hold(Button button, double seconds) {
        double start = cursorTime;
        press(button);
        at(start + seconds).release(button);
        return at(start);
    }

    /** A quick press and release. The cursor moves to just after the release. */
    public GamepadScript tap(Button button) {
        hold(button, DEFAULT_TAP_SECONDS);
        return after(DEFAULT_TAP_SECONDS * 2);
    }

    /** Several quick taps in a row, for example moving down a menu. */
    public GamepadScript tap(Button button, int times) {
        for (int i = 0; i < times; i++) {
            tap(button);
        }
        return this;
    }

    /** Lets go of every stick, trigger and button. */
    public GamepadScript releaseAll() {
        return add("release all", pad -> {
            for (Button b : Button.values()) {
                b.set(pad, false);
            }
            for (Axis a : Axis.values()) {
                a.set(pad, 0.0);
            }
        });
    }

    /** Schedules one action at the cursor and returns the script, so calls can be chained. */
    private GamepadScript add(String description, Consumer<Gamepad> apply) {
        actions.add(new Action(cursorPeriod, cursorTime, actions.size(), description, apply));
        return this;
    }

    /** Actions for one period, in time order (ties keep the order they were written). */
    List<Action> actionsFor(Period period) {
        List<Action> result = new ArrayList<>();
        for (Action action : actions) {
            if (action.period == period) {
                result.add(action);
            }
        }
        result.sort(Comparator.comparingDouble((Action a) -> a.time).thenComparingInt(a -> a.order));
        return result;
    }
}
