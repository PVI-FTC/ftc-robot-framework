package pvi.harness.viewer;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.nio.file.Path;
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
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.text.DefaultCaret;

import pvi.harness.runtime.RunRecording;
import pvi.harness.runtime.RunRecordingJson;

/**
 * The desktop viewer: replays a finished {@link RunRecording} in a window.
 *
 * <ul>
 *   <li>Top: a dropdown to run a ready-made scenario ({@link Scenarios}) or replay a saved
 *       recording ({@link ViewerChoices}).</li>
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
 * <p>It only ever sees the finished, unchangeable recording, never the runner or the fake
 * robot, so it can't change a test's result. All its work happens on the Swing thread.
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

    /** Builds the window for a recording. Call on the Swing thread; see {@link #open}. */
    public ViewerWindow(RunRecording recording, String title) {
        super(WINDOW_TITLE + " - " + title);
        view = new RecordingView(recording);
        playback = new Playback(recording);
        fieldPanel = new FieldPanel(view);
        gamepadPanel = new GamepadPanel(view);
        timeline = new JSlider(0, view.size() - 1, 0);
        timer = new Timer(FRAME_MILLIS, e -> tick());

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        add(buildChooser(title), BorderLayout.NORTH);
        add(fieldPanel, BorderLayout.CENTER);
        add(buildSidePanel(), BorderLayout.EAST);
        add(buildControls(), BorderLayout.SOUTH);
        bindKeys();
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
        choiceBox.setToolTipText("Run a ready-made scenario, or replay a saved recording (build/recordings)");
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

        JPanel bar = new JPanel(new BorderLayout(8, 0));
        bar.add(new JLabel("Run or replay:"), BorderLayout.WEST);
        bar.add(choiceBox, BorderLayout.CENTER);
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
            for (ViewerChoices.Choice choice : ViewerChoices.list(Scenarios.all(), RunRecordingJson.DEFAULT_FOLDER)) {
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
                action.run();
                refresh();
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
        sideText.setText(String.join("\n", SidePanelText.describe(view, index)));

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
