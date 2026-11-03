package pvi.harness.viewer;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
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
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import pvi.harness.recorded.RecordedTestWriter;
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
    /** The right-hand column: telemetry cards for the shown moment. */
    private final TelemetryPanel telemetryPanel;
    /** The bar across the top: the mark, the mission clock, the Record button. */
    private final MissionHeader header;
    /** Name of what is shown (file name, or "practice drive"), for the header. */
    private String shownName;
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
    /** Opens the replay picker (saved recordings). */
    private final JButton replaysButton = new JButton();
    /** Turns the run on screen into a new test file (like gradlew makeTest). */
    private final JButton saveTestButton = new JButton();
    /** What the dropdown shows when nothing in it is picked (while replaying a recording). */
    private String choicePlaceholder = "";
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
    /** The live drive's notice count last shown, so each new controller notice is shown once. */
    private int noticesShown;

    /** Builds the window for a recording. Call on the Swing thread; see {@link #open}. */
    public ViewerWindow(RunRecording recording, String title) {
        super(WINDOW_TITLE + " - " + title);
        view = new RecordingView(recording);
        playback = new Playback(recording);
        fieldPanel = new FieldPanel(view);
        gamepadPanel = new GamepadPanel(view);
        telemetryPanel = new TelemetryPanel(view);
        shownName = title;
        timeline = new JSlider(0, view.size() - 1, 0);
        timer = new Timer(FRAME_MILLIS, e -> tick());
        liveTimer = new Timer(2 * FRAME_MILLIS, e -> liveTick());

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        getContentPane().setBackground(Theme.BACKGROUND);
        setLayout(new BorderLayout());
        JComponent chooser = buildChooser(title);
        header = new MissionHeader(view, recordButton);
        JPanel top = new JPanel(new BorderLayout());
        top.add(header, BorderLayout.NORTH);
        top.add(chooser, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);
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

    /**
     * Opens a viewer window that goes straight into a live drive of {@code option} (INIT, waiting
     * for Enter). {@code background} is shown for the moment before the drive's first snapshot.
     */
    public static void openDriving(RunRecording background, String title, LiveDrive.Option option) {
        SwingUtilities.invokeLater(() -> {
            ViewerWindow window = new ViewerWindow(background, title);
            window.setVisible(true);
            window.startDrive(option);
        });
    }

    /** Saves a scenario's recording as {@code build/recordings/viewer-<id>.json} and returns the file. */
    static Path saveScenarioRun(Scenarios.Scenario scenario, RunRecording recording) {
        return RunRecordingJson.save(recording, RunRecordingJson.fileInDefaultFolder("viewer-" + scenario.id()));
    }

    // ---- Building the window -------------------------------------------------------------------

    /** The bar at the top: the dropdown and a line describing what is shown. */
    private JComponent buildChooser(String shownName) {
        choiceBox.setToolTipText("Drive a TeleOp live, or run a ready-made scenario. Saved recordings: Replays button");
        Theme.flat(choiceBox);
        // While replaying, nothing in the dropdown is picked: show what is being replayed instead.
        javax.swing.ListCellRenderer<? super ViewerChoices.Choice> themed = choiceBox.getRenderer();
        choiceBox.setRenderer((list, value, index, selected, focused) -> {
            Component cell = themed.getListCellRendererComponent(list, value, index, selected, focused);
            if (value == null && index < 0 && cell instanceof JLabel label) {
                label.setText(choicePlaceholder);
                label.setForeground(Theme.MUTED);
            }
            return cell;
        });
        choicePlaceholder = "Replaying " + shownName;
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

        Theme.flat(recordButton);
        recordButton.setFont(Theme.labelFont(12f));
        recordButton.setFocusable(false);   // keep the keyboard on the robot
        recordButton.setToolTipText("Turn on before pressing Enter (PLAY) to save the drive."
                + " Off = practice: the drive is shown but not saved.");
        recordButton.addActionListener(e -> showRecordState());
        showRecordState();

        Theme.flat(replaysButton);
        replaysButton.setText(Theme.label("Replays..."));
        replaysButton.setToolTipText("Pick a saved recording to replay (Ctrl+O)");
        replaysButton.setFocusable(false);
        replaysButton.addActionListener(e -> openReplayPicker());
        replayControls.add(replaysButton);

        Theme.flat(saveTestButton);
        saveTestButton.setText(Theme.label("Save as test"));
        saveTestButton.setToolTipText("Turn the run on screen into a new test in src\\test\\java\\pvi\\harness\\recorded"
                + " (like gradlew makeTest)");
        saveTestButton.setFocusable(false);
        saveTestButton.addActionListener(e -> saveShownRunAsTest());
        replayControls.add(saveTestButton);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setBackground(Theme.BACKGROUND);
        buttons.add(saveTestButton);
        buttons.add(replaysButton);

        JLabel caption = new JLabel(Theme.label("Drive / run"));
        caption.setFont(Theme.labelFont(10f));
        caption.setForeground(Theme.MUTED);
        choiceDescription.setFont(Theme.text(12f));
        choiceDescription.setForeground(Theme.MUTED);

        JPanel bar = new JPanel(new BorderLayout(12, 6));
        bar.setBackground(Theme.BACKGROUND);
        bar.add(caption, BorderLayout.WEST);
        bar.add(choiceBox, BorderLayout.CENTER);
        bar.add(buttons, BorderLayout.EAST);
        bar.add(choiceDescription, BorderLayout.SOUTH);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.LINE),
                BorderFactory.createEmptyBorder(10, 20, 8, 20)));
        return bar;
    }

    /** Refills the dropdown and selects the entry with {@code selectedLabel}, if there is one. */
    private void fillChoices(String selectedLabel) {
        updatingChoices = true;
        try {
            choiceBox.removeAllItems();
            ViewerChoices.Choice toSelect = null;
            for (ViewerChoices.Choice choice : ViewerChoices.runnable(LiveDrive.options(), Scenarios.all())) {
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
                fillChoices(choice.label());
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
        telemetryPanel.setView(view);
        shownName = name;
        updatingControls = true;
        try {
            timeline.setMaximum(view.size() - 1);
        } finally {
            updatingControls = false;
        }
        setTitle(WINDOW_TITLE + " - " + name);
        choicePlaceholder = "Replaying " + name;
        choiceDescription.setText("Showing " + name);
        choiceDescription.setForeground(Theme.MUTED);
        fillChoices(ViewerChoices.REPLAY_PREFIX + name);
        refresh();
    }

    /** The right-hand column: telemetry cards (scrolling) over the two gamepads. */
    private JComponent buildSidePanel() {
        JScrollPane scroll = Theme.flat(new JScrollPane(telemetryPanel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER));
        scroll.setPreferredSize(new Dimension(TelemetryPanel.WIDTH + 8, 600));
        JPanel side = new JPanel(new BorderLayout());
        side.setBackground(Theme.BACKGROUND);
        side.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, Theme.LINE));
        side.add(scroll, BorderLayout.CENTER);
        side.add(gamepadPanel, BorderLayout.SOUTH);
        return side;
    }

    private JComponent buildControls() {
        JButton toStart = button("|<", "Start (Home)", () -> playback.seek(0));
        JButton back = button("<", "One loop back (Left; Shift+Left = 1 s)", () -> playback.step(-1));
        JButton forward = button(">", "One loop forward (Right; Shift+Right = 1 s)", () -> playback.step(1));
        JButton toEnd = button(">|", "End (End)", () -> playback.seek(view.size() - 1));
        Theme.flat(playButton);
        playButton.setPreferredSize(new Dimension(92, playButton.getPreferredSize().height));
        playButton.setToolTipText("Play / pause (Space)");
        playButton.setFocusable(false);
        playButton.addActionListener(e -> {
            playback.togglePlay();
            refresh();
        });

        for (double speed : Playback.SPEEDS) {
            speedBox.addItem(String.format(Locale.ROOT, "%sx", trimNumber(speed)));
        }
        Theme.flat(speedBox);
        speedBox.setToolTipText("Playback speed ([ slower, ] faster)");
        speedBox.setFocusable(false);
        speedBox.addActionListener(e -> {
            if (!updatingControls) {
                playback.setSpeedIndex(speedBox.getSelectedIndex());
                refresh();
            }
        });

        Theme.flat(timeline);
        timeline.setFocusable(false);
        timeline.addChangeListener(e -> {
            if (!updatingControls) {
                playback.pause();
                playback.seek(timeline.getValue());
                refresh();
            }
        });

        replayControls.addAll(List.of(toStart, back, playButton, forward, toEnd, speedBox, timeline));

        JLabel speedLabel = new JLabel(Theme.label("Speed"));
        speedLabel.setFont(Theme.labelFont(10f));
        speedLabel.setForeground(Theme.MUTED);
        timeLabel.setFont(Theme.mono(13f));
        timeLabel.setForeground(Theme.TEXT);

        JPanel buttons = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 6, 0));
        buttons.setOpaque(false);
        buttons.add(toStart);
        buttons.add(back);
        buttons.add(playButton);
        buttons.add(forward);
        buttons.add(toEnd);
        buttons.add(javax.swing.Box.createHorizontalStrut(14));
        buttons.add(speedLabel);
        buttons.add(speedBox);
        buttons.add(javax.swing.Box.createHorizontalStrut(14));
        buttons.add(timeLabel);
        buttons.add(javax.swing.Box.createHorizontalStrut(10));

        JPanel controls = new JPanel(new BorderLayout());
        controls.setBackground(Theme.PANEL);
        controls.add(buttons, BorderLayout.WEST);
        controls.add(timeline, BorderLayout.CENTER);
        controls.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.LINE),
                BorderFactory.createEmptyBorder(10, 14, 10, 20)));
        return controls;
    }

    /** A small button that runs {@code action} then refreshes the window. */
    private JButton button(String text, String tip, Runnable action) {
        JButton button = Theme.flat(new JButton(text));
        button.setFont(Theme.text(13f));
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
        key(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK, "replays", () -> SwingUtilities.invokeLater(this::openReplayPicker));
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

    /** Opens the replay picker and replays the recording picked there (not while driving). */
    private void openReplayPicker() {
        if (liveDrive != null) {
            return;
        }
        Path file = ReplayPicker.choose(this, RunRecordingJson.DEFAULT_FOLDER);
        if (file == null) {
            return;
        }
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            display(RunRecordingJson.load(file), file.getFileName().toString());
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Could not open " + file.getFileName(),
                    JOptionPane.ERROR_MESSAGE);
        } finally {
            setCursor(Cursor.getDefaultCursor());
        }
    }

    // ---- Save as test ---------------------------------------------------------------------------

    /**
     * Asks for a class name, then writes the run on screen as a new test (see {@link SaveAsTest}).
     * Not while driving. Saving runs the OpMode once more, so it happens off the Swing thread
     * and the controls are off until it finishes.
     */
    private void saveShownRunAsTest() {
        if (liveDrive != null) {
            return;
        }
        RunRecording recording = view.recording();
        String className = askClassName(SaveAsTest.suggestedClassName(recording));
        if (className == null) {
            return;   // cancelled
        }
        String source = shownName;
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        choiceBox.setEnabled(false);
        replayControls.forEach(control -> control.setEnabled(false));
        choiceDescription.setText("Saving " + className + ".java (runs the OpMode once more)...");
        new SwingWorker<Path, Void>() {
            @Override
            protected Path doInBackground() {
                return SaveAsTest.save(recording, className, source,
                        RecordedTestWriter.DEFAULT_FOLDER, RecordedTestWriter.TEAMCODE_SOURCES);
            }

            @Override
            protected void done() {
                setCursor(Cursor.getDefaultCursor());
                choiceBox.setEnabled(true);
                replayControls.forEach(control -> control.setEnabled(true));
                try {
                    showSavedTest(get());
                } catch (Exception e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    choiceDescription.setText("Could not save the test.");
                    JOptionPane.showMessageDialog(ViewerWindow.this, cause.getMessage(), "Could not save the test",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    /** The "Save as test" dialog. Returns the chosen class name, or null if cancelled. */
    private String askClassName(String suggested) {
        JTextField nameField = new JTextField(suggested, 28);
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.add(new JLabel("Test class name:"), BorderLayout.NORTH);
        panel.add(nameField, BorderLayout.CENTER);
        panel.add(new JLabel("<html>Saved in src\\test\\java\\pvi\\harness\\recorded.<br>"
                + "Never replaces a file: a second one becomes ...2.</html>"), BorderLayout.SOUTH);
        while (true) {
            int answer = JOptionPane.showConfirmDialog(this, panel, "Save as test",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (answer != JOptionPane.OK_OPTION) {
                return null;
            }
            String problem = SaveAsTest.problemWithClassName(nameField.getText());
            if (problem == null) {
                return nameField.getText().trim();
            }
            JOptionPane.showMessageDialog(this, problem, "Save as test", JOptionPane.WARNING_MESSAGE);
        }
    }

    /** Says where the new test went and how to run it, with a button to copy the command. */
    private void showSavedTest(Path file) {
        String command = SaveAsTest.runCommand(file);
        choiceDescription.setText("Saved a new test: " + file.getFileName());
        String[] buttons = {"Copy command", "OK"};
        int answer = JOptionPane.showOptionDialog(this,
                "Saved " + file.getFileName() + " in " + file.toAbsolutePath().getParent()
                        + "\n\nRun it from Harness\\ with:\n" + command,
                "Saved as test", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, buttons, buttons[1]);
        if (answer == 0) {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(command), null);
        }
    }

    // ---- Live keyboard driving ------------------------------------------------------------------

    /** Starts driving {@code option} live: INIT now, Enter = PLAY, Esc = STOP and save. */
    private void startDrive(LiveDrive.Option option) {
        playback.pause();
        heldKeys.releaseAll();
        liveOption = option;
        liveDrive = new LiveDrive(option, heldKeys, new JamepadControllers(), MatchPeriod.TELEOP.seconds(), true);
        noticesShown = 0;
        fillChoices(ViewerChoices.DRIVE_PREFIX + option.title());
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(driveKeys);
        choiceBox.setEnabled(false);
        replayControls.forEach(control -> control.setEnabled(false));
        setTitle(WINDOW_TITLE + " - driving " + option.title());
        shownName = "driving " + option.title();
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
            choiceDescription.setText("Driving " + liveOption.title() + ". Esc to stop.");
            choiceDescription.setForeground(Theme.MUTED);
        }
        if (liveDrive.noticeCount() != noticesShown) {
            // A controller connected or disconnected: say so where the driver is looking.
            noticesShown = liveDrive.noticeCount();
            String notice = liveDrive.notice();
            choiceDescription.setText(notice);
            if (notice.contains("disconnected")) {
                choiceDescription.setForeground(Theme.AMBER);
                Toolkit.getDefaultToolkit().beep();
            }
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
        telemetryPanel.setView(view);
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
                        + ". Press Space to replay it, or Save as test.");
            } else {
                display(recording, "practice drive (not saved)");
                choiceDescription.setText("Practice drive with " + option.title() + " was not saved. Turn on"
                        + " Record before pressing Enter to save one. Press Space to replay it now, or Save as test.");
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

    /** Status lines for the telemetry panel while driving: what to press, recording, inputs. */
    private List<String> driveStatus() {
        List<String> lines = new ArrayList<>();
        // The first line is the headline (the telemetry panel shows it large).
        if (liveDrive.phase() == LiveDrive.Phase.INIT) {
            lines.add(">> INIT: press Enter for PLAY <<");
            lines.add(recordButton.isSelected()
                    ? "Record ON: this drive will be saved"
                    : "Record OFF: practice, not saved (click Record before Enter to save)");
        } else {
            RunRecording.LoopRecord last = view.loop(view.size() - 1);
            double left = Math.max(0.0, liveDrive.matchSeconds() - last.matchTimeSeconds());
            lines.add(String.format(Locale.ROOT, ">> DRIVING: %d:%02d left, Esc to stop <<",
                    (int) left / 60, (int) left % 60));
            lines.add(liveDrive.keepRecording() ? "Recording: this drive will be saved" : "Practice: not saved");
        }
        List<String> inputs = liveDrive.inputStatus();
        lines.add("Gamepad 1: " + inputs.get(0) + "   Gamepad 2: " + inputs.get(1));
        if (liveDrive.controllerProblem() != null) {
            lines.add(liveDrive.controllerProblem());
        }
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
        recordButton.setText(Theme.label(recordButton.isSelected() ? "Rec on" : "Rec off"));
        recordButton.setIcon(new Theme.Dot(recordButton.isSelected() ? Theme.RED : null, Theme.MUTED));
        recordButton.setIconTextGap(8);
        recordButton.setForeground(recordButton.isSelected() ? Theme.RED : Theme.MUTED);
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
        telemetryPanel.show(index);
        if (liveDrive != null) {
            telemetryPanel.setLive(driveStatus(), KeyboardGamepads.help());
        } else {
            telemetryPanel.setLive(null, null);
        }
        boolean recordingLive = liveDrive != null && (liveDrive.phase() == LiveDrive.Phase.INIT
                ? recordButton.isSelected() : liveDrive.keepRecording());
        header.show(view, index, shownName, liveDrive != null, recordingLive);

        updatingControls = true;
        try {
            timeline.setValue(index);
            speedBox.setSelectedIndex(playback.speedIndex());
        } finally {
            updatingControls = false;
        }
        playButton.setText(Theme.label(playback.isPlaying() ? "Pause" : "Play"));
        RunRecording.LoopRecord loop = view.loop(index);
        RunRecording.LoopRecord last = view.loop(view.size() - 1);
        timeLabel.setText("T+ " + MissionHeader.clock(loop.matchTimeSeconds()) + "  /  "
                + MissionHeader.clock(last.matchTimeSeconds()));

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
