package pvi.harness.viewer;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;

/**
 * The "Replays" window: every saved recording in {@code build/recordings}, newest first, with
 * what made it (Drive, Scenario or Test), when it was saved and its size. Type to filter; Open
 * (or double-click, or Enter) replays the selected one; "Open file..." picks a recording from
 * anywhere on the computer. ALL / DRIVES / SCENARIOS / TESTS show one kind only.
 *
 * <p>Use {@link #choose}; it waits until the window is closed. Swing thread only.</p>
 */
public final class ReplayPicker extends JDialog {
    private static final DateTimeFormatter SAVED_FORMAT =
            DateTimeFormatter.ofPattern("MMM d  HH:mm:ss", Locale.ROOT).withZone(ZoneId.systemDefault());

    private final List<ReplayLibrary.Entry> all;
    private final List<ReplayLibrary.Entry> shown = new ArrayList<>();
    private final JTextField search = new JTextField();
    private final Model model = new Model();
    private final JTable table = new JTable(model);
    private final JLabel count = new JLabel();
    /** Which kind to show, or null for all. */
    private ReplayLibrary.Kind kindFilter;
    private Path chosen;

    private ReplayPicker(Frame owner, Path folder) {
        super(owner, "Replays", true);
        this.all = ReplayLibrary.list(folder);
        getContentPane().setBackground(Theme.BACKGROUND);
        setLayout(new BorderLayout());
        add(buildTop(), BorderLayout.NORTH);
        add(buildTable(), BorderLayout.CENTER);
        add(buildButtons(), BorderLayout.SOUTH);
        bindKeys();
        refilter();
        setSize(new Dimension(860, 540));
        setLocationRelativeTo(owner);
    }

    /**
     * Shows the window over {@code owner} and waits. Returns the recording to replay, or null
     * if the user cancelled.
     */
    public static Path choose(Frame owner, Path folder) {
        ReplayPicker picker = new ReplayPicker(owner, folder);
        picker.setVisible(true);   // waits here until the window closes (it is modal)
        return picker.chosen;
    }

    // ---- Building ----------------------------------------------------------------------------------

    private JComponent buildTop() {
        JLabel title = new JLabel(Theme.label("Replays"));
        title.setFont(Theme.labelFont(14f));
        title.setForeground(Theme.TEXT);
        count.setFont(Theme.labelFont(10f));
        count.setForeground(Theme.MUTED);

        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        heading.add(title, BorderLayout.WEST);
        heading.add(count, BorderLayout.EAST);

        search.setFont(Theme.text(13f));
        search.setForeground(Theme.TEXT);
        search.setBackground(Theme.PANEL);
        search.setCaretColor(Theme.TEXT);
        search.setBorder(BorderFactory.createCompoundBorder(Theme.hairline(), BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        search.setToolTipText("Type to filter by name or type (Drive, Scenario, Test)");
        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                refilter();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                refilter();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                refilter();
            }
        });
        JLabel searchLabel = new JLabel(Theme.label("Filter"));
        searchLabel.setFont(Theme.labelFont(10f));
        searchLabel.setForeground(Theme.MUTED);

        JPanel kinds = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        kinds.setOpaque(false);
        javax.swing.ButtonGroup group = new javax.swing.ButtonGroup();
        addKindButton(kinds, group, "All", null, true);
        addKindButton(kinds, group, "Drives", ReplayLibrary.Kind.DRIVE, false);
        addKindButton(kinds, group, "Scenarios", ReplayLibrary.Kind.SCENARIO, false);
        addKindButton(kinds, group, "Tests", ReplayLibrary.Kind.TEST, false);

        JPanel searchRow = new JPanel(new BorderLayout(10, 0));
        searchRow.setOpaque(false);
        searchRow.add(searchLabel, BorderLayout.WEST);
        searchRow.add(search, BorderLayout.CENTER);
        searchRow.add(kinds, BorderLayout.EAST);

        JPanel top = new JPanel(new BorderLayout(0, 10));
        top.setBackground(Theme.BACKGROUND);
        top.setBorder(BorderFactory.createEmptyBorder(16, 18, 10, 18));
        top.add(heading, BorderLayout.NORTH);
        top.add(searchRow, BorderLayout.SOUTH);
        return top;
    }

    /** One of the ALL / DRIVES / SCENARIOS / TESTS buttons; the selected one is cyan. */
    private void addKindButton(JPanel row, javax.swing.ButtonGroup group, String text, ReplayLibrary.Kind kind,
                               boolean selected) {
        javax.swing.JToggleButton button = Theme.flat(new javax.swing.JToggleButton(Theme.label(text)));
        button.setFont(Theme.labelFont(10f));
        button.setFocusable(false);
        button.setSelected(selected);
        button.setForeground(selected ? Theme.ACCENT : Theme.MUTED);
        button.addItemListener(e -> button.setForeground(button.isSelected() ? Theme.ACCENT : Theme.MUTED));
        button.addActionListener(e -> {
            kindFilter = kind;
            refilter();
        });
        group.add(button);
        row.add(button);
    }

    private JComponent buildTable() {
        table.setFont(Theme.text(13f));
        table.setForeground(Theme.TEXT);
        table.setBackground(Theme.PANEL);
        table.setSelectionBackground(Theme.RAISED);
        table.setSelectionForeground(Theme.TEXT);
        table.setGridColor(Theme.LINE);
        table.setShowVerticalLines(false);
        table.setRowHeight(28);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(0).setPreferredWidth(380);
        table.getColumnModel().getColumn(1).setPreferredWidth(90);
        table.getColumnModel().getColumn(2).setPreferredWidth(150);
        table.getColumnModel().getColumn(3).setPreferredWidth(80);
        table.setDefaultRenderer(Object.class, new Cell());

        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setDefaultRenderer((t, value, selected, focused, row, column) -> {
            JLabel label = new JLabel(Theme.label(String.valueOf(value)));
            label.setFont(Theme.labelFont(10f));
            label.setForeground(Theme.MUTED);
            label.setOpaque(true);
            label.setBackground(Theme.BACKGROUND);
            label.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.LINE), BorderFactory.createEmptyBorder(6, 8, 6, 8)));
            return label;
        });

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && table.rowAtPoint(e.getPoint()) >= 0) {
                    openSelected();
                }
            }
        });

        JScrollPane scroll = Theme.flat(new JScrollPane(table));
        scroll.getViewport().setBackground(Theme.PANEL);
        scroll.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(0, 18, 0, 18), Theme.hairline()));
        JPanel holder = new JPanel(new BorderLayout());
        holder.setBackground(Theme.BACKGROUND);
        holder.add(scroll, BorderLayout.CENTER);
        return holder;
    }

    private JComponent buildButtons() {
        JButton openFile = Theme.flat(new JButton(Theme.label("Open file...")));
        openFile.setToolTipText("Pick a recording (.json) from anywhere on this computer");
        openFile.addActionListener(e -> openFromAnywhere());
        JButton cancel = Theme.flat(new JButton(Theme.label("Cancel")));
        cancel.addActionListener(e -> dispose());
        JButton open = Theme.flat(new JButton(Theme.label("Open")));
        open.setForeground(Theme.ACCENT);
        open.addActionListener(e -> openSelected());

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        left.setOpaque(false);
        left.add(openFile);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        right.add(cancel);
        right.add(open);

        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Theme.BACKGROUND);
        bar.setBorder(BorderFactory.createEmptyBorder(12, 18, 16, 18));
        bar.add(left, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    /** Enter opens, Esc cancels, Up/Down move through the list even while typing in the filter. */
    private void bindKeys() {
        JComponent root = getRootPane();
        bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "open", this::openSelected);
        bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel", this::dispose);
        bind(search, KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "down", () -> move(1));
        bind(search, KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "up", () -> move(-1));
        bind(table, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "openRow", this::openSelected);
    }

    private static void bind(JComponent component, KeyStroke key, String name, Runnable action) {
        component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(key, name);
        component.getInputMap(JComponent.WHEN_FOCUSED).put(key, name);
        component.getActionMap().put(name, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        });
    }

    // ---- Behaviour ---------------------------------------------------------------------------------

    /** Shows only the entries matching the filter, keeping the first one selected. */
    private void refilter() {
        shown.clear();
        for (ReplayLibrary.Entry entry : all) {
            if ((kindFilter == null || entry.kind() == kindFilter) && entry.matches(search.getText())) {
                shown.add(entry);
            }
        }
        model.fireTableDataChanged();
        if (!shown.isEmpty()) {
            table.setRowSelectionInterval(0, 0);
        }
        count.setText(Theme.label(shown.size() == all.size()
                ? all.size() + " saved" : shown.size() + " of " + all.size()));
    }

    private void move(int by) {
        if (shown.isEmpty()) {
            return;
        }
        int row = Math.max(0, Math.min(shown.size() - 1, table.getSelectedRow() + by));
        table.setRowSelectionInterval(row, row);
        table.scrollRectToVisible(table.getCellRect(row, 0, true));
    }

    private void openSelected() {
        int row = table.getSelectedRow();
        if (row >= 0 && row < shown.size()) {
            chosen = shown.get(row).file();
            dispose();
        }
    }

    /** Windows' own file window, for a recording outside build/recordings. */
    private void openFromAnywhere() {
        FileDialog dialog = new FileDialog(this, "Open a recording", FileDialog.LOAD);
        dialog.setFile("*.json");
        dialog.setVisible(true);
        if (dialog.getFile() != null) {
            chosen = Path.of(dialog.getDirectory(), dialog.getFile());
            dispose();
        }
    }

    // ---- Table -------------------------------------------------------------------------------------

    private final class Model extends AbstractTableModel {
        private final String[] columns = {"Name", "Type", "Saved", "Size"};

        @Override
        public int getRowCount() {
            return shown.size();
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Override
        public String getColumnName(int column) {
            return columns[column];
        }

        @Override
        public Object getValueAt(int row, int column) {
            ReplayLibrary.Entry entry = shown.get(row);
            return switch (column) {
                case 0 -> entry.name();
                case 1 -> entry.kind();
                case 2 -> SAVED_FORMAT.format(entry.saved());
                default -> entry.sizeText();
            };
        }
    }

    /** Dark cells; the type in its own color (drives cyan). */
    private static final class Cell extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focused,
                                                       int row, int column) {
            super.getTableCellRendererComponent(table, value, selected, false, row, column);
            setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
            setBackground(selected ? Theme.RAISED : Theme.PANEL);
            if (value instanceof ReplayLibrary.Kind kind) {
                setText(Theme.label(kind.title()));
                setFont(Theme.labelFont(10f));
                setForeground(kind == ReplayLibrary.Kind.DRIVE ? Theme.ACCENT
                        : kind == ReplayLibrary.Kind.SCENARIO ? Theme.TEXT : Theme.MUTED);
            } else {
                setFont(column == 0 ? Theme.text(13f) : Theme.mono(12f));
                setForeground(column == 0 ? Theme.TEXT : Theme.MUTED);
            }
            if (selected && column == 0) {
                setForeground(Theme.ACCENT);
            }
            return this;
        }
    }
}
