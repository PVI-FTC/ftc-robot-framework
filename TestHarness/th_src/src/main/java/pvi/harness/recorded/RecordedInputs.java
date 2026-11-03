package pvi.harness.recorded;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import pvi.harness.ds.Axis;
import pvi.harness.ds.Button;
import pvi.harness.ds.GamepadScript;
import pvi.harness.runtime.RunRecording;
import pvi.harness.runtime.RunRecording.GamepadState;
import pvi.harness.runtime.RunRecording.LoopRecord;
import pvi.harness.runtime.RunResult;

/**
 * Turns the gamepad inputs saved in a {@link RunRecording} back into {@link GamepadScript} steps
 * (step D3 of "Drive and record a test").
 *
 * <p>A recording stores both gamepads on every loop. This keeps only the <b>moments something
 * changed</b>, so a 2-minute drive becomes a readable list such as:</p>
 * <pre>
 *   at 0.00: stick LEFT_STICK_Y = -1.0
 *   at 1.24: hold A for 0.12 s
 *   at 2.10: stick LEFT_STICK_Y = 0.0
 * </pre>
 *
 * <p>Times come straight from the recording, so they are already whole loops (20 ms). Inputs
 * made before PLAY are measured from INIT, the same as {@link GamepadScript#duringInit()}.</p>
 *
 * <p>To keep the script short, sticks and triggers are rounded to two decimals, values closer
 * to 0 than {@link #DEAD_ZONE} count as 0, and a new value is only written once it has moved at
 * least {@link #STICK_STEP} from the last one written (going back to exactly 0, or to the end of
 * its range, is always written). So a replay matches the recording to within
 * {@link #STICK_STEP}; buttons match exactly.</p>
 */
public final class RecordedInputs {
    /** Stick and trigger values closer to 0 than this are written as 0 (same as the controller dead zone). */
    public static final double DEAD_ZONE = 0.05;
    /** A stick or trigger must move at least this far before a new value is written. */
    public static final double STICK_STEP = 0.05;

    /** Which clock a step's time is measured on, like GamepadScript's duringInit() / afterStart(). */
    public enum Period {
        /** Before PLAY: seconds after INIT. */
        INIT,
        /** After PLAY: seconds after PLAY. */
        MATCH
    }

    /** What a step does. */
    public enum Kind {
        /** Moves a stick or trigger to {@code value}. */
        STICK,
        /** Presses a button and keeps holding it (it is released later, after PLAY, or never). */
        PRESS,
        /** Releases a button pressed in an earlier period. */
        RELEASE,
        /** Presses a button and releases it {@code holdSeconds} later. */
        HOLD,
        /** Not an input: a moment marked in the recording, such as a controller disconnect. */
        NOTE
    }

    /**
     * One line of the script.
     *
     * @param period      INIT (seconds after INIT) or MATCH (seconds after PLAY)
     * @param time        when it happens, in seconds, on the period's clock
     * @param kind        what it does
     * @param axis        the stick or trigger, for STICK; otherwise null
     * @param button      the button, for PRESS / RELEASE / HOLD; otherwise null
     * @param value       the new stick or trigger value, for STICK
     * @param holdSeconds how long the button is held, for HOLD
     * @param note        the marked moment's text, for NOTE; otherwise null
     */
    public record Step(Period period, double time, Kind kind, Axis axis, Button button,
                       double value, double holdSeconds, String note) {

        /** Adds this step to a script. NOTE steps add nothing. */
        void applyTo(GamepadScript script) {
            switch (kind) {
                case STICK -> script.at(time).stick(axis, value);
                case PRESS -> script.at(time).press(button);
                case RELEASE -> script.at(time).release(button);
                case HOLD -> script.at(time).hold(button, holdSeconds);
                case NOTE -> { }
            }
        }

        /** Plain text, e.g. "at 1.24: hold A for 0.12 s". */
        @Override
        public String toString() {
            String when = (period == Period.INIT ? "INIT + " : "at ") + number(time);
            return switch (kind) {
                case STICK -> when + ": stick " + axis + " = " + number(value);
                case PRESS -> when + ": press " + button;
                case RELEASE -> when + ": release " + button;
                case HOLD -> when + ": hold " + button + " for " + number(holdSeconds) + " s";
                case NOTE -> when + ": (" + note + ")";
            };
        }
    }

    /** The steps for both gamepads, each list in time order (INIT steps first). */
    public record Inputs(List<Step> gamepad1, List<Step> gamepad2) {

        public Inputs {
            gamepad1 = List.copyOf(gamepad1);
            gamepad2 = List.copyOf(gamepad2);
        }

        /** Gamepad 1 (driver) as a ready-to-use script. */
        public GamepadScript gamepad1Script() {
            return toScript(gamepad1);
        }

        /** Gamepad 2 (operator) as a ready-to-use script. */
        public GamepadScript gamepad2Script() {
            return toScript(gamepad2);
        }

        /** True if the gamepad has any real input (notes don't count). */
        public static boolean hasInputs(List<Step> steps) {
            return steps.stream().anyMatch(step -> step.kind() != Kind.NOTE);
        }
    }

    private RecordedInputs() {
    }

    /**
     * Reads both gamepads out of a recording.
     *
     * <p>Marked moments (such as "Gamepad 1 controller disconnected") are kept as NOTE steps on
     * the gamepad their text names, or on gamepad 1 if it names neither, so they can be shown
     * as comments.</p>
     */
    public static Inputs from(RunRecording recording) {
        List<Step> pad1 = stepsFor(recording.loops(), true);
        List<Step> pad2 = stepsFor(recording.loops(), false);
        for (RunRecording.Event event : recording.events()) {
            boolean beforePlay = isBeforePlay(event, recording.loops());
            Step note = new Step(beforePlay ? Period.INIT : Period.MATCH,
                    beforePlay ? event.sinceInitSeconds() : event.matchTimeSeconds(),
                    Kind.NOTE, null, null, 0, 0, event.text());
            List<Step> target = event.text().toLowerCase(Locale.ROOT).contains("gamepad 2") ? pad2 : pad1;
            target.add(note);
        }
        sortInPlace(pad1);
        sortInPlace(pad2);
        return new Inputs(pad1, pad2);
    }

    /** Builds a script from steps, in order. */
    public static GamepadScript toScript(List<Step> steps) {
        GamepadScript script = new GamepadScript();
        Period current = Period.MATCH;
        for (Step step : steps) {
            if (step.period() != current) {
                current = step.period();
                if (current == Period.INIT) {
                    script.duringInit();
                } else {
                    script.afterStart();
                }
            }
            step.applyTo(script);
        }
        return script;
    }

    /**
     * Rounds a stick or trigger value the way scripts write it: two decimals, and 0 inside the
     * dead zone.
     */
    public static double tidy(double value) {
        double rounded = Math.round(value * 100.0) / 100.0;
        if (Math.abs(rounded) < DEAD_ZONE) {
            return 0.0;
        }
        return rounded == 0.0 ? 0.0 : rounded;   // never write -0.0
    }

    /** Short number text: "1.24", "-1.0", "0.5" (at most 6 decimals, at least one). */
    public static String number(double value) {
        if (value == 0.0) {
            return "0.0";
        }
        String text = String.format(Locale.ROOT, "%.6f", value);
        text = text.replaceAll("0+$", "");
        return text.endsWith(".") ? text + "0" : text;
    }

    // ---- Internals ------------------------------------------------------------------------

    /** The changes on one gamepad, loop by loop. */
    private static List<Step> stepsFor(List<LoopRecord> loops, boolean firstGamepad) {
        List<Step> steps = new ArrayList<>();
        // Last value written for each axis, and the step that pressed each held button.
        Map<Axis, Double> written = new EnumMap<>(Axis.class);
        for (Axis axis : Axis.values()) {
            written.put(axis, 0.0);
        }
        Map<Button, Integer> pressStep = new EnumMap<>(Button.class);

        for (LoopRecord loop : loops) {
            GamepadState state = firstGamepad ? loop.gamepad1() : loop.gamepad2();
            if (state == null) {
                state = GamepadState.RELEASED;
            }
            boolean beforePlay = loop.stage() == RunResult.Stage.INIT || loop.stage() == RunResult.Stage.INIT_LOOP;
            Period period = beforePlay ? Period.INIT : Period.MATCH;
            double time = beforePlay ? loop.sinceInitSeconds() : loop.matchTimeSeconds();

            // Buttons: a press starts a step; the release turns it into a HOLD when both
            // happened in the same period, or adds a RELEASE when they didn't.
            for (Button button : Button.values()) {
                boolean held = state.buttons().contains(button);
                Integer pressedAt = pressStep.get(button);
                if (held && pressedAt == null) {
                    steps.add(new Step(period, time, Kind.PRESS, null, button, 0, 0, null));
                    pressStep.put(button, steps.size() - 1);
                } else if (!held && pressedAt != null) {
                    Step press = steps.get(pressedAt);
                    if (press.period() == period) {
                        steps.set(pressedAt, new Step(period, press.time(), Kind.HOLD, null, button,
                                0, roundTime(time - press.time()), null));
                    } else {
                        steps.add(new Step(period, time, Kind.RELEASE, null, button, 0, 0, null));
                    }
                    pressStep.remove(button);
                }
            }

            // Sticks and triggers: only when they moved enough (see the class comment).
            for (Axis axis : Axis.values()) {
                double value = tidy(read(state, axis));
                double last = written.get(axis);
                boolean endOfRange = value == 0.0 || Math.abs(value) == 1.0;
                if ((endOfRange && value != last) || Math.abs(value - last) >= STICK_STEP - 1e-9) {
                    steps.add(new Step(period, time, Kind.STICK, axis, null, value, 0, null));
                    written.put(axis, value);
                }
            }
        }
        return steps;
    }

    /** One stick or trigger from a saved gamepad state. */
    private static double read(GamepadState state, Axis axis) {
        return switch (axis) {
            case LEFT_STICK_X -> state.leftStickX();
            case LEFT_STICK_Y -> state.leftStickY();
            case RIGHT_STICK_X -> state.rightStickX();
            case RIGHT_STICK_Y -> state.rightStickY();
            case LEFT_TRIGGER -> state.leftTrigger();
            case RIGHT_TRIGGER -> state.rightTrigger();
        };
    }

    /** True if the event happened before the first START entry (or there is none). */
    private static boolean isBeforePlay(RunRecording.Event event, List<LoopRecord> loops) {
        for (LoopRecord loop : loops) {
            if (loop.stage() == RunResult.Stage.START) {
                return event.sinceInitSeconds() < loop.sinceInitSeconds() - 1e-9;
            }
        }
        return true;
    }

    /** INIT steps first, then MATCH; by time within each; same-time steps keep their order. */
    private static void sortInPlace(List<Step> steps) {
        steps.sort((a, b) -> {
            if (a.period() != b.period()) {
                return a.period() == Period.INIT ? -1 : 1;
            }
            return Double.compare(a.time(), b.time());
        });
    }

    /** Rounds a time difference to the microsecond, so 1.36 - 1.24 shows as 0.12. */
    private static double roundTime(double seconds) {
        return Math.round(seconds * 1_000_000.0) / 1_000_000.0;
    }
}
