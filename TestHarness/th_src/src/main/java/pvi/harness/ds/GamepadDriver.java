package pvi.harness.ds;

import com.qualcomm.robotcore.hardware.Gamepad;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Plays a {@link GamepadScript}, and any live {@link GamepadSource}, into the gamepad an OpMode
 * reads. Every way of giving a gamepad input goes through here.
 *
 * <p>The script changes a separate "physical" gamepad. Each loop that state is handed to
 * the OpMode's gamepad with the SDK's own {@link Gamepad#copy(Gamepad)}, the same path the
 * Robot Controller uses when Driver Station data arrives. That keeps the SDK's
 * {@code aWasPressed()}-style edge detection working exactly as on the robot.</p>
 */
public class GamepadDriver {
    /** The gamepad "in the driver's hands": the script (or the test) changes this one. */
    private final Gamepad physical = new Gamepad();
    /** The gamepad the OpMode reads (its gamepad1 or gamepad2 field). */
    private final Gamepad opModeGamepad;
    /** Every rumble the OpMode sent to this gamepad, in order. */
    private final List<RumbleRecord> rumbles = new ArrayList<>();
    /** Scripted actions before PLAY, sorted by time. Empty when no script is set. */
    private List<GamepadScript.Action> initActions = List.of();
    /** Scripted actions after PLAY, sorted by time. */
    private List<GamepadScript.Action> matchActions = List.of();
    /** Index of the next INIT action still to apply. */
    private int nextInit;
    /** Index of the next MATCH action still to apply. */
    private int nextMatch;
    /** Live input (keyboard, controller, later Unity), or null. Applied after the script each loop. */
    private GamepadSource source;

    /** @param opModeGamepad the OpMode's gamepad that this driver feeds */
    public GamepadDriver(Gamepad opModeGamepad) {
        this.opModeGamepad = opModeGamepad;
    }

    /** Loads a script and starts it from the beginning. */
    public void setScript(GamepadScript script) {
        initActions = script.actionsFor(GamepadScript.Period.INIT);
        matchActions = script.actionsFor(GamepadScript.Period.MATCH);
        nextInit = 0;
        nextMatch = 0;
    }

    /**
     * Lets a live source (keyboard, controller, later Unity) drive this gamepad. Each loop it
     * runs after the script's actions, so it wins if both change the same input. Pass null to
     * remove it.
     */
    public void setSource(GamepadSource source) {
        this.source = source;
    }

    /** The gamepad the test changes directly, for quick tests without a script. */
    public Gamepad physical() {
        return physical;
    }

    /**
     * Applies every scripted action that is due, then delivers the new state to the OpMode.
     *
     * @param sinceInit  seconds since INIT
     * @param sinceStart seconds since PLAY, or a negative number before PLAY
     */
    public void deliver(double sinceInit, double sinceStart) {
        // Apply every INIT action whose time has come. (The tiny 1e-9 allowance stops
        // rounding from delaying an action scheduled exactly on a loop boundary.)
        while (nextInit < initActions.size() && initActions.get(nextInit).time <= sinceInit + 1e-9) {
            initActions.get(nextInit++).apply.accept(physical);
        }
        // MATCH actions only start counting once PLAY has been pressed.
        if (sinceStart >= 0) {
            while (nextMatch < matchActions.size() && matchActions.get(nextMatch).time <= sinceStart + 1e-9) {
                matchActions.get(nextMatch++).apply.accept(physical);
            }
        }
        // Live input last, so it reflects what the driver is doing right now.
        if (source != null) {
            source.update(physical, sinceInit, sinceStart);
        }
        // Hand the new state to the OpMode through the SDK's own copy(), which also runs the
        // SDK's button edge detection (aWasPressed() and friends), like Driver Station data.
        opModeGamepad.copy(physical);
    }

    /**
     * Records rumble effects the OpMode queued since the last call.
     *
     * <p>The SDK puts each rumble command in the gamepad's rumbleQueue as a list of steps
     * (motor strengths and a duration). This adds up the duration and counts the separate
     * buzzes: each change from "off" to "on" is one pulse.</p>
     *
     * @param matchTime seconds after PLAY, stamped on each record
     */
    public void collectRumbles(double matchTime) {
        Gamepad.RumbleEffect effect;
        while (opModeGamepad.rumbleQueue != null && (effect = opModeGamepad.rumbleQueue.poll()) != null) {
            int total = 0;
            int pulses = 0;
            boolean wasOn = false;
            for (Gamepad.RumbleEffect.Step step : effect.steps) {
                total += step.duration;
                boolean on = step.large > 0 || step.small > 0;   // either rumble motor running
                if (on && !wasOn) {
                    pulses++;
                }
                wasOn = on;
            }
            rumbles.add(new RumbleRecord(matchTime, total, pulses));
        }
    }

    /** Every rumble recorded so far, oldest first. */
    public List<RumbleRecord> rumbles() {
        return Collections.unmodifiableList(rumbles);
    }
}
