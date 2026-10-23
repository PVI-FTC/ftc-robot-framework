package pvi.harness.viewer;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.text.DefaultCaret;

import pvi.harness.runtime.MatchPeriod;
import pvi.harness.runtime.RunRecording;
import pvi.harness.runtime.RunRecordingJson;

/**
 * The desktop viewer: replays a finished {@link RunRecording} in a window.
 *
 * <ul>
 *   <li>Top: a dropdown to drive a TeleOp live from the keyboard ({@link LiveDrive}), run a
 *       ready-made scenario ({@link Scenarios}) or replay a saved recording
 *       ({@link ViewerChoices}).</li>
 *   <li>Left: the field, robot, heading arrow and trail ({@link FieldPanel}).</li>
 *   <li>Right: the side panel ({@link SidePanelText}): time, stage, pose, motors, the Driver
 *       Station screen and rumbles; under it, pictures of both gamepads with the held buttons
 *       lit ({@link GamepadPanel}).</li>
 *   <li>Bottom: play/pause, step buttons, speed, and a timeline slider.</li>
 * </ul>
 *
 * <p>Keyboard: Space = play/pause, Left/Right = one loop back/forward, Shift+Left/Right = one
 * second, Home/End = start/end, [ and ] (or - and +) = slower/faster.</p>
 *
 * <p>While driving live, the keys in {@link KeyboardGamepads} go to the robot instead (Enter =
 * PLAY, Esc = STOP and save); the playback controls are off until the drive is saved, then it
 * replays like any recording.</p>
 *
 * <p>It only ever sees unchangeable recordings (snapshots, while driving), never the runner or
 * the fake robot, so it can't change a test's result. All its work happens on the Swing thread;
 * a live drive runs the robot code on its own thread ({@link LiveDrive}).
 * Opening it on a computer with no display (or during tests, which are headless) throws
 * {@link java.awt.HeadlessException} instead of showing anything.</p>
 */
public final class ViewerWindow extends JFrame {
    /** How often the window checks the clock while playing, in milliseconds (about 60 per second). */
    private static final int FRAME_MILLIS = 16;
    /** Start of the window title; the shown recording's name follows it. */
    private static final String WINDOW_TITLE = "Harness viewer";

    /** The recording shown; replaced when the dropdown picks another. */
    private RecordingView view;
    /** Play/pause/speed state for {@link #view}; replaced with it. */
    private Playback playback;
    private final FieldPanel fieldPanel;
    private final GamepadPanel gamepadPanel;
    private final JTextArea sideText = new JTextArea();
    private final JSlider timeline;
    private final JButton playButton = new JButton("Play");
    private final JComboBox<String> speedBox = new JComboBox<>();
    private final JLabel timeLabel = new JLabel();
    /** Ticks while playing; runs on the Swing thread. */
    private final Timer timer;
    /** Real time (System.nanoTime) of the last tick, to know how much time passed. */
    private long lastTickNanos;
    /** True while the window itself moves the slider, so that move isn't treated as a seek. */
    private boolean updatingControls;
    /** The dropdown: scenarios to run and recordings to replay. */
    private final JComboBox<ViewerChoices.Choice> choiceBox = new JComboBox<>();
    /** One line under the dropdown describing what is shown. */
    private final JLabel choiceDescription = new JLabel(" ");
    /** True while the window itself fills or selects the dropdown, so that isn't treated as a pick. */
    private boolean updatingChoices;

    /** The live keyboard drive in progress, or null when replaying. */
    private LiveDrive liveDrive;
    /** Which TeleOp {@link #liveDrive} is driving. */
    private LiveDrive.Option liveOption;
    /** Keys held down right now; the live drive's gamepads read them. */
    private final HeldKeys heldKeys = new HeldKeys();
    /** While driving: picks up the newest snapshot about 30 times a second. */
    private final Timer liveTimer;
    /** While driving: sends the keys the robot uses to {@link #heldKeys} instead of the window. */
    private final KeyEventDispatcher driveKeys = this::dispatchDriveKey;
    /** Controls that only make sense when replaying; turned off while driving. */
    private final List<JComponent> replayControls = new ArrayList<>();
    /**
     * Record: on = the next drive is saved, off = practice (nothing saved). It is read when Enter
     * (PLAY) is pressed and locked until the drive ends.
     */
    private final JToggleButton recordButton = new JToggleButton();

    /** Builds the window for a recording. Call on the Swing thread; see {@link #open}. */
    public ViewerWindow(RunRecording recording, String title) {
        super(WINDOW_TITLE + " - " + title);
        view = new RecordingView(recording);
        playback = new Playback(recording);
        fieldPanel = new FieldPanel(view);
        gamepadPanel = new GamepadPanel(view);
        timeline = new JSlider(0, view.size() - 1, 0);
        timer = new Timer(FRAME_MILLIS, e -> tick());
        liveTimer = new Timer(2 * FRAME_MILLIS, e -> liveTick());

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        add(buildChooser(title), BorderLayout.NORTH);
        add(fieldPanel, BorderLayout.CENTER);
        add(buildSidePanel(), BorderLayout.EAST);
        add(buildControls(), BorderLayout.SOUTH);
        bindKeys();
        watchWindow();
        refresh();
        pack();
        setLocationByPlatform(true);
    }

    /**
     * Opens a viewer window for {@code recording} on the Swing thread and returns at once.
     * The window is given only the immutable recording.
     */
    public static void open(RunRecording recording, String title) {
        SwingUtilities.invokeLater(() -> new ViewerWindow(recording, title).setVisible(true));
    }

    /** Saves a scenario's recording as {@code build/recordings/viewer-<id>.json} and returns the file. */
    static Path saveScenarioRun(Scenarios.Scenario scenario, RunRecording recording) {
        return RunRecordingJson.save(recording, RunRecordingJson.fileInDefaultFolder("viewer-" + scenario.id()));
    }

    // ---- Building the window -------------------------------------------------------------------

    /** The bar at the top: the dropdown and a line describing what is shown. */
    private JComponent buildChooser(String shownName) {
        choiceBox.setToolTipText("Drive a TeleOp from the keyboard, run a ready-made scenario, or replay a saved"
                + " recording (build/recordings)");
        choiceBox.setFocusable(false);
        choiceBox.setMaximumRowCount(20);
        fillChoices("Replay: " + shownName);
        choiceDescription.setText("Showing " + shownName);
        // Rescan build/recordings every time the list opens, so new test runs show up.
        choiceBox.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                ViewerChoices.Choice selected = (ViewerChoices.Choice) choiceBox.getSelectedItem();
                fillChoices(selected == null ? "" : selected.label());
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
            }
        });
        choiceBox.addActionListener(e -> {
            if (!updatingChoices && choiceBox.getSelectedItem() != null) {
                choose((ViewerChoices.Choice) choiceBox.getSelectedItem());
            }
        });

        recordButton.setFocusable(false);   // keep the keyboard on the robot
        recordButton.setToolTipText("Turn on before pressing Enter (PLAY) to save the drive."
                + " Off = practice: the drive is shown but not saved.");
        recordButton.addActionListener(e -> showRecordState());
        showRecordState();

        JPanel bar = new JPanel(new BorderLayout(8, 0));
        bar.add(new JLabel("Drive, run or replay:"), BorderLayout.WEST);
        bar.add(choiceBox, BorderLayout.CENTER);
        bar.add(recordButton, BorderLayout.EAST);
        bar.add(choiceDescription, BorderLayout.SOUTH);
        bar.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        return bar;
    }

    /** Refills the dropdown and selects the entry with {@code selectedLabel}, if there is one. */
    private void fillChoices(String selectedLabel) {
        updatingChoices = true;
        try {
            choiceBox.removeAllItems();
            ViewerChoices.Choice toSelect = null;
            for (ViewerChoices.Choice choice : ViewerChoices.list(LiveDrive.options(), Scenarios.all(), RunRecordingJson.DEFAULT_FOLDER)) {
                choiceBox.addItem(choice);
                if (choice.label().equals(selectedLabel)) {
                    toSelect = choice;
                }
            }
            choiceBox.setSelectedItem(toSelect);
        } finally {
            updatingChoices = false;
        }
    }

    /** Runs the picked scenario, or loads the picked recording, then shows it. */
    private void choose(ViewerChoices.Choice choice) {
        if (choice.isDrive()) {
            startDrive(choice.drive());
            return;
        }
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            if (choice.isScenario()) {
                // Simulated time: even a whole match runs in about a second.
                RunRecording recording = choice.scenario().perform();
                Path saved = saveScenarioRun(choice.scenario(), recording);
                display(recording, saved.getFileName().toString());
                choiceDescription.setText("Ran " + choice.scenario().title() + ": " + choice.scenario().description());
            } else {
                display(RunRecordingJson.load(choice.file()), choice.file().getFileName().toString());
            }
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Could not show " + choice.label(),
                    JOptionPane.ERROR_MESSAGE);
        } finally {
            setCursor(Cursor.getDefaultCursor());
        }
    }

    /** Replaces the shown recording, starting paused at its beginning (the speed is kept). */
    private void display(RunRecording recording, String name) {
        int speedIndex = playback.speedIndex();
        playback.pause();
        view = new RecordingView(recording);
        playback = new Playback(recording);
        playback.setSpeedIndex(speedIndex);
        fieldPanel.setView(view);
        gamepadPanel.setView(view);
        updatingControls = true;
        try {
            timeline.setMaximum(view.size() - 1);
        } finally {
            updatingControls = false;
        }
        setTitle(WINDOW_TITLE + " - " + name);
        choiceDescription.setText("Showing " + name);
        fillChoices(ViewerChoices.REPLAY_PREFIX + name);
        refresh();
    }

    private JComponent buildSidePanel() {
        JPanel side = new JPanel(new BorderLayout());
        side.add(buildSideText(), BorderLayout.CENTER);
        side.add(gamepadPanel, BorderLayout.SOUTH);
        return side;
    }

    private JComponent buildSideText() {
        sideText.setEditable(false);
        sideText.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        sideText.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        sideText.setFocusable(false);
        // Don't jump to the bottom every time the text changes.
        ((DefaultCaret) sideText.getCaret()).setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
        JScrollPane scroll = new JScrollPane(sideText);
        scroll.setPreferredSize(new Dimension(420, 640));
        return scroll;
    }

    private JComponent buildControls() {
        JButton toStart = button("|<", "Start (Home)", () -> playback.seek(0));
        JButton back = button("<", "One loop back (Left; Shift+Left = 1 s)", () -> playback.step(-1));
        JButton forward = button(">", "One loop forward (Right; Shift+Right = 1 s)", () -> playback.step(1));
        JButton toEnd = button(">|", "End (End)", () -> playback.seek(view.size() - 1));
        playButton.setToolTipText("Play / pause (Space)");
        playButton.setFocusable(false);
        playButton.addActionListener(e -> {
            playback.togglePlay();
            refresh();
        });

        for (double speed : Playback.SPEEDS) {
            speedBox.addItem(String.format(Locale.ROOT, "%sx", trimNumber(speed)));
        }
        speedBox.setToolTipText("Playback speed ([ slower, ] faster)");
        speedBox.setFocusable(false);
        speedBox.addActionListener(e -> {
            if (!updatingControls) {
                playback.setSpeedIndex(speedBox.getSelectedIndex());
                refresh();
            }
        });

        timeline.setFocusable(false);
        timeline.addChangeListener(e -> {
            if (!updatingControls) {
                playback.pause();
                playback.seek(timeline.getValue());
                refresh();
            }
        });

        replayControls.addAll(List.of(toStart, back, playButton, forward, toEnd, speedBox, timeline));

        JPanel buttons = new JPanel();
        buttons.add(toStart);
        buttons.add(back);
        buttons.add(playButton);
        buttons.add(forward);
        buttons.add(toEnd);
        buttons.add(new JLabel("  Speed"));
        buttons.add(speedBox);
        buttons.add(timeLabel);

        JPanel controls = new JPanel(new BorderLayout());
        controls.add(buttons, BorderLayout.WEST);
        controls.add(timeline, BorderLayout.CENTER);
        controls.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 8));
        return controls;
    }

    /** A small button that runs {@code action} then refreshes the window. */
    private JButton button(String text, String tip, Runnable action) {
        JButton button = new JButton(text);
        button.setToolTipText(tip);
        button.setFocusable(false);
        button.addActionListener(e -> {
            action.run();
            refresh();
        });
        return button;
    }

    /** Keyboard shortcuts, active anywhere in the window. Lambdas, so they use the current playback. */
    private void bindKeys() {
        key(KeyEvent.VK_SPACE, 0, "play", () -> playback.togglePlay());
        key(KeyEvent.VK_LEFT, 0, "back", () -> playback.step(-1));
        key(KeyEvent.VK_RIGHT, 0, "forward", () -> playback.step(1));
        key(KeyEvent.VK_LEFT, InputEvent.SHIFT_DOWN_MASK, "backSecond", () -> playback.jumpSeconds(-1.0));
        key(KeyEvent.VK_RIGHT, InputEvent.SHIFT_DOWN_MASK, "forwardSecond", () -> playback.jumpSeconds(1.0));
        key(KeyEvent.VK_HOME, 0, "start", () -> playback.seek(0));
        key(KeyEvent.VK_END, 0, "end", () -> playback.seek(view.size() - 1));
        key(KeyEvent.VK_OPEN_BRACKET, 0, "slower", () -> playback.slower());
        key(KeyEvent.VK_MINUS, 0, "slower2", () -> playback.slower());
        key(KeyEvent.VK_CLOSE_BRACKET, 0, "faster", () -> playback.faster());
        key(KeyEvent.VK_EQUALS, 0, "faster2", () -> playback.faster());
    }

    private void key(int keyCode, int modifiers, String name, Runnable action) {
        InputMap keys = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        keys.put(KeyStroke.getKeyStroke(keyCode, modifiers), name);
        getRootPane().getActionMap().put(name, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (liveDrive != null) {
                    return;   // playback keys are off while driving
                }
                action.run();
                refresh();
            }
        });
    }

    // ---- Live keyboard driving ------------------------------------------------------------------

    /** Starts driving {@code option} live: INIT now, Enter = PLAY, Esc = STOP and save. */
    private void startDrive(LiveDrive.Option option) {
        playback.pause();
        heldKeys.releaseAll();
        liveOption = option;
        liveDrive = new LiveDrive(option, heldKeys, MatchPeriod.TELEOP.seconds(), true);
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(driveKeys);
        choiceBox.setEnabled(false);
        replayControls.forEach(control -> control.setEnabled(false));
        setTitle(WINDOW_TITLE + " - driving " + option.title());
        choiceDescription.setText("Driving " + option.title() + ". INIT: press Enter for PLAY, Esc to stop.");
        liveDrive.start();
        liveTimer.start();
    }

    /** Shows the newest snapshot, and saves the drive once it has finished. */
    private void liveTick() {
        if (liveDrive == null) {
            liveTimer.stop();
            return;
        }
        RunRecording snapshot = liveDrive.latest();
        if (snapshot != null && !snapshot.loops().isEmpty()) {
            showSnapshot(snapshot);
        }
        if (liveDrive.phase() == LiveDrive.Phase.DRIVING) {
            choiceDescription.setText("Driving " + liveOption.title() + ". Esc to stop and save.");
        }
        if (liveDrive.phase() == LiveDrive.Phase.FINISHED) {
            finishDrive();
        }
    }

    /** Shows the last moment of a live snapshot. */
    private void showSnapshot(RunRecording snapshot) {
        view = new RecordingView(snapshot);
        playback = new Playback(snapshot);
        playback.seek(view.size() - 1);
        fieldPanel.setView(view);
        gamepadPanel.setView(view);
        updatingControls = true;
        try {
            timeline.setMaximum(view.size() - 1);
        } finally {
            updatingControls = false;
        }
        refresh();
    }

    /** The drive has finished: save it, then replay it like any recording. */
    private void finishDrive() {
        liveTimer.stop();
        KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(driveKeys);
        heldKeys.releaseAll();
        RunRecording recording = liveDrive.latest();
        RuntimeException error = liveDrive.error();
        boolean keep = liveDrive.keepRecording();
        LiveDrive.Option option = liveOption;
        liveDrive = null;
        liveOption = null;
        choiceBox.setEnabled(true);
        replayControls.forEach(control -> control.setEnabled(true));
        recordButton.setEnabled(true);

        if (recording != null && !recording.loops().isEmpty()) {
            if (keep) {
                Path saved = saveDrive(option, recording);
                display(recording, saved.getFileName().toString());
                choiceDescription.setText("Drove " + option.title() + "; saved as " + saved.getFileName()
                        + ". Press Space to replay it.");
            } else {
                display(recording, "practice drive (not saved)");
                choiceDescription.setText("Practice drive with " + option.title() + " was not saved. Turn on"
                        + " Record before pressing Enter to save one. Press Space to replay it now.");
            }
        }
        if (error != null) {
            JOptionPane.showMessageDialog(this, "The robot code crashed during the drive: " + error,
                    "Drive ended early", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Saves a live drive as {@code build/recordings/drive-<id>-<date>-<time>.json} and returns the file. */
    static Path saveDrive(LiveDrive.Option option, RunRecording recording) {
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss", Locale.ROOT));
        return RunRecordingJson.save(recording, RunRecordingJson.fileInDefaultFolder("drive-" + option.id() + "-" + stamp));
    }

    /** Lines at the top of the side panel while driving: what to press, and the layout. */
    private List<String> driveStatus() {
        List<String> lines = new ArrayList<>();
        if (liveDrive.phase() == LiveDrive.Phase.INIT) {
            lines.add(">> INIT: press Enter for PLAY <<");
            lines.add(recordButton.isSelected()
                    ? "Record ON: this drive will be saved"
                    : "Record OFF: practice, not saved (click Record before Enter to save)");
        } else {
            lines.add(liveDrive.keepRecording() ? "Recording: this drive will be saved" : "Practice: not saved");
            RunRecording.LoopRecord last = view.loop(view.size() - 1);
            double left = Math.max(0.0, liveDrive.matchSeconds() - last.matchTimeSeconds());
            lines.add(String.format(Locale.ROOT, ">> DRIVING: %d:%02d left, Esc to stop <<",
                    (int) left / 60, (int) left % 60));
        }
        lines.addAll(KeyboardGamepads.help());
        return lines;
    }

    /**
     * While driving, the robot's keys go to {@link #heldKeys} and are not passed on, so they
     * don't move the dropdown or playback. Enter presses PLAY; Esc presses STOP.
     */
    private boolean dispatchDriveKey(KeyEvent event) {
        if (liveDrive == null) {
            return false;
        }
        int code = event.getKeyCode();
        if (event.getID() == KeyEvent.KEY_PRESSED) {
            if (code == KeyboardGamepads.PLAY_KEY) {
                // The Record button's state at PLAY decides whether this drive is saved.
                liveDrive.requestPlay(recordButton.isSelected());
                recordButton.setEnabled(false);
            } else if (code == KeyboardGamepads.STOP_KEY) {
                liveDrive.requestStop();
            } else {
                heldKeys.press(code);
            }
        } else if (event.getID() == KeyEvent.KEY_RELEASED) {
            heldKeys.release(code);
        } else {
            return true;   // typed characters: nothing to type into while driving
        }
        return KeyboardGamepads.isUsed(code);
    }

    /** Shows the Record button's state in its text. */
    private void showRecordState() {
        recordButton.setText(recordButton.isSelected() ? "\u25CF Record: ON" : "\u25CB Record: OFF");
        if (liveDrive != null) {
            refresh();
        }
    }

    /** Lets go of every key when the window loses focus; stops a drive when the window closes. */
    private void watchWindow() {
        addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowLostFocus(WindowEvent e) {
                heldKeys.releaseAll();
            }
        });
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                if (liveDrive != null) {
                    liveDrive.requestStop();
                    liveTimer.stop();
                    KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(driveKeys);
                }
            }
        });
    }

    // ---- Playing --------------------------------------------------------------------------------

    /** One timer tick: move the playhead by the real time since the last tick. */
    private void tick() {
        long now = System.nanoTime();
        double realSeconds = (now - lastTickNanos) / 1e9;
        lastTickNanos = now;
        if (playback.advance(realSeconds) || !playback.isPlaying()) {
            refresh();
        }
    }

    /** Brings every part of the window up to date with the playback state. */
    private void refresh() {
        int index = playback.index();
        fieldPanel.show(index);
        gamepadPanel.show(index);
        List<String> side = new ArrayList<>();
        if (liveDrive != null) {
            side.addAll(driveStatus());
            side.add("");
        }
        side.addAll(SidePanelText.describe(view, index));
        sideText.setText(String.join("\n", side));

        updatingControls = true;
        try {
            timeline.setValue(index);
            speedBox.setSelectedIndex(playback.speedIndex());
        } finally {
            updatingControls = false;
        }
        playButton.setText(playback.isPlaying() ? "Pause" : "Play");
        RunRecording.LoopRecord loop = view.loop(index);
        RunRecording.LoopRecord last = view.loop(view.size() - 1);
        timeLabel.setText(String.format(Locale.ROOT, "  t = %.2f / %.2f s", loop.matchTimeSeconds(),
                last.matchTimeSeconds()));

        // Run the timer only while playing, so an idle window uses no CPU.
        if (playback.isPlaying() && !timer.isRunning()) {
            lastTickNanos = System.nanoTime();
            timer.start();
        } else if (!playback.isPlaying() && timer.isRunning()) {
            timer.stop();
        }
    }

    /** 0.25 -> "0.25", 1.0 -> "1". */
    private static String trimNumber(double value) {
        return value == Math.rint(value) ? Integer.toString((int) value) : Double.toString(value);
    }
}
