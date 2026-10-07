package org.firstinspires.ftc.teamcode.core.util;

import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.util.ElapsedTime;

import java.util.function.BooleanSupplier;

/**
 * Triggers one-shot gamepad rumble events whenever a condition first becomes true.
 *
 * <p>Each registered condition is checked every loop via a rising-edge test: the event fires
 * exactly once when the condition transitions from false to true. It will not re-fire until
 * the condition goes false again and turns true a second time.</p>
 *
 * <h2>Match-timer events (pre-configured)</h2>
 * <ul>
 *   <li><b>90 s elapsed</b> - 2 blips (30 seconds left / Endgame warning)</li>
 *   <li><b>105 s elapsed</b> - 3 blips (15 seconds left / Final countdown)</li>
 *   <li><b>115 s elapsed</b> - 500 ms long pulse (5 seconds left / Match cutoff)</li>
 * </ul>
 *
 * <h2>Typical OpMode usage</h2>
 * <pre>
 *   // In init():
 *   rumble = new RumbleManager(gamepad1);
 *   rumble.addTimerAlert(60.0, 1);   // optional: add your own timer alerts
 *
 *   // In start():
 *   rumble.resetTimer();
 *
 *   // In loop():
 *   rumble.update();
 *   if (driverInput.wasAJustPressed()) { rumble.rumbleNow(200); } // optional: instant rumble
 * </pre>
 *
 * <h2>Adding a custom time-based alert</h2>
 * <pre>
 *   // Rumble 1 blip when 60 seconds have elapsed:
 *   rumble.addTimerAlert(60.0, 1);
 * </pre>
 *
 * <h2>Adding a custom condition event</h2>
 * <pre>
 *   rumble.addEvent(() -> robot.isIntakeHolding(), () -> gamepad1.rumbleBlips(1));
 * </pre>
 */
public class RumbleManager {

    // Maximum number of registered events (match events + user-defined slots).
    // Raise this constant if you add many custom events.
    private static final int MAX_EVENTS = 16;

    private final Gamepad gamepad;
    private final ElapsedTime matchTimer = new ElapsedTime();

    // Parallel arrays allocated once at construction - zero allocation during update().
    private final BooleanSupplier[] conditions    = new BooleanSupplier[MAX_EVENTS];
    private final Runnable[]        actions       = new Runnable[MAX_EVENTS];
    private final boolean[]         previousState = new boolean[MAX_EVENTS];
    private int eventCount = 0;

    /**
     * Creates a RumbleManager tied to one gamepad.
     *
     * @param gamepad the non-null gamepad that will receive rumble commands
     */
    public RumbleManager(Gamepad gamepad) {
        if (gamepad == null) {
            throw new IllegalArgumentException("RumbleManager requires a non-null gamepad.");
        }
        this.gamepad = gamepad;
        registerMatchTimerEvents();
    }

    /**
     * Resets the match timer to zero.
     *
     * <p>Call this from {@code start()} so match-time events are measured from the moment
     * the Driver Station presses PLAY, not from {@code init()}.</p>
     */
    public void resetTimer() {
        matchTimer.reset();
    }

    /**
     * Evaluates all registered conditions and fires any whose rising edge is detected.
     *
     * <p>Call exactly once per TeleOp {@code loop()}, after updating any sensor or
     * mechanism state that your custom conditions depend on.</p>
     */
    public void update() {
        for (int i = 0; i < eventCount; i++) {
            boolean current = conditions[i].getAsBoolean();
            if (current && !previousState[i]) {
                actions[i].run();
            }
            previousState[i] = current;
        }
    }

    /**
     * Adds a custom one-shot rumble event.
     *
     * <p>The {@code action} fires exactly once each time {@code condition} transitions from
     * false to true, then resets when the condition goes false again.</p>
     *
     * <pre>
     *   // Rumble when intake reaches Holding state:
     *   rumble.addEvent(() -> robot.getIntakeStateName().equals("Holding"), () -> gamepad1.rumbleBlips(1));
     *   // Rumble on a button press (use InputManager, not gamepad directly):
     *   rumble.addEvent(() -> driverInput.wasAJustPressed(), () -> rumble.rumbleNow(200));
     * </pre>
     *
     * @param condition returns {@code true} when the event should fire
     * @param action    the rumble command to run (e.g. {@code () -> gamepad1.rumbleBlips(2)})
     * @throws IllegalStateException if the event capacity is exceeded
     */
    public void addEvent(BooleanSupplier condition, Runnable action) {
        if (eventCount >= MAX_EVENTS) {
            throw new IllegalStateException(
                    "RumbleManager: cannot register more than " + MAX_EVENTS + " events. "
                    + "Raise MAX_EVENTS if you need more.");
        }
        conditions[eventCount]    = condition;
        actions[eventCount]       = action;
        previousState[eventCount] = false;
        eventCount++;
    }

    /**
     * Schedules a gamepad rumble to fire once at a specific point during the match.
     *
     * <p><b>When to use this:</b> Call it in {@code init()} when you want the controller
     * to buzz at a time YOUR team chooses. The three built-in alerts (90 s, 105 s, 115 s)
     * are NOT changed — this only adds extra ones on top.</p>
     *
     * <p><b>How it works:</b> As the match timer runs, this checks each loop whether the
     * specified number of seconds has passed. The moment it has, the gamepad buzzes the
     * chosen number of times — exactly once. It will not buzz again.</p>
     *
     * <p><b>How to use it — two steps:</b></p>
     * <ol>
     *   <li>Call this method inside {@code init()}, after creating the RumbleManager.</li>
     *   <li>Make sure you call {@code rumble.update()} every loop as usual — that is what
     *       checks the timer and fires the buzz.</li>
     * </ol>
     *
     * <pre>
     *   // In init() — buzz 1 time at the 60-second mark:
     *   rumble.addTimerAlert(60.0, 1);
     *
     *   // In init() — buzz 4 times at the 80-second mark:
     *   rumble.addTimerAlert(80.0, 4);
     * </pre>
     *
     * @param secondsElapsed the number of seconds into the match when the buzz should fire
     *                       (e.g. 60.0 means 1 minute in, 90.0 means 1.5 minutes in)
     * @param blips          how many short buzzes to send
     *                       (1 = one short buzz, 3 = three short buzzes, etc.)
     */
    public void addTimerAlert(double secondsElapsed, int blips) {
        addTimerAlert(secondsElapsed, blips, 0);
    }

    /**
     * Schedules a gamepad rumble to fire once at a specific point during the match,
     * with control over both the number of blips AND how long the rumble lasts.
     *
     * <p><b>Use blips OR durationMs — not both at the same time:</b></p>
     * <ul>
     *   <li>Set {@code blips} to the number of short buzzes you want, and {@code durationMs}
     *       to {@code 0} for a blip-style alert.</li>
     *   <li>Set {@code blips} to {@code 0} and {@code durationMs} to the length of the
     *       continuous rumble in milliseconds for a long-buzz alert.</li>
     * </ul>
     *
     * <pre>
     *   // 3 short blips at 80 seconds (blip style — set durationMs to 0):
     *   rumble.addTimerAlert(80.0, 3, 0);
     *
     *   // 1 full second of continuous rumble at 85 seconds (duration style — set blips to 0):
     *   rumble.addTimerAlert(85.0, 0, 1000);
     *
     *   // Quick 300 ms buzz at 60 seconds (duration style):
     *   rumble.addTimerAlert(60.0, 0, 300);
     * </pre>
     *
     * @param secondsElapsed the number of seconds into the match when the rumble fires
     *                       (e.g. 60.0 = 1 minute in, 90.0 = 1.5 minutes in)
     * @param blips          how many short buzzes to send; use 0 if you want durationMs instead
     * @param durationMs     how long a continuous rumble lasts in milliseconds; use 0 if you
     *                       want blips instead (1000 milliseconds = 1 second)
     */
    public void addTimerAlert(double secondsElapsed, int blips, int durationMs) {
        addEvent(
            () -> matchTimer.seconds() >= secondsElapsed,
            blips > 0
                ? () -> gamepad.rumbleBlips(blips)
                : () -> gamepad.rumble(durationMs)
        );
    }


    /**
     * Rumbles the gamepad RIGHT NOW for a set number of milliseconds.
     *
     * <p><b>When to use this:</b> Call it anywhere inside {@code loop()} the moment
     * something happens and you want the driver to feel it immediately. Unlike the
     * timer alerts, this does not wait for any condition — it fires the instant you
     * call it.</p>
     *
     * <p><b>Tip — milliseconds guide:</b></p>
     * <ul>
     *   <li>100–200 ms = a quick tap (good for button confirmations)</li>
     *   <li>300–500 ms = a firm buzz (good for state changes like intake holding)</li>
     *   <li>1000 ms = a full one-second rumble (good for major events)</li>
     * </ul>
     *
     * <p><b>How to use it:</b> Wrap the call in whatever condition you want, inside
     * {@code loop()}. No extra setup needed.</p>
     *
     * <pre>
     *   // Quick 200 ms tap when the driver presses button A:
     *   if (driverInput.wasAJustPressed()) {
     *       rumble.rumbleNow(200);
     *   }
     *
     *   // Firm 500 ms buzz the moment the intake enters Holding state:
     *   if (robot.getIntakeStateName().equals("Holding")) {
     *       rumble.rumbleNow(500);
     *   }
     * </pre>
     *
     * @param durationMs how long the rumble lasts, in milliseconds
     *                   (1000 milliseconds = 1 second)
     */
    public void rumbleNow(int durationMs) {
        gamepad.rumble(durationMs);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /** Registers the three standard FTC match-timer rumble events. */
    private void registerMatchTimerEvents() {
        // 90 s elapsed - 2 blips (30 s left, Endgame begins)
        addEvent(
            () -> matchTimer.seconds() >= 90.0,
            () -> gamepad.rumbleBlips(2)
        );

        // 105 s elapsed - 3 blips (15 s left, Final countdown)
        addEvent(
            () -> matchTimer.seconds() >= 105.0,
            () -> gamepad.rumbleBlips(3)
        );

        // 115 s elapsed - 500 ms long pulse (5 s left, Match cutoff imminent)
        addEvent(
            () -> matchTimer.seconds() >= 115.0,
            () -> gamepad.rumble(500)
        );
    }
}
