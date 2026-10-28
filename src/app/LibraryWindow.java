package app;

import dao.BookDAO;
import dao.CategoryDAO;
import dao.Database;
import dao.LoanDAO;
import dao.MemberDAO;
import model.Book;
import model.Category;
import model.Loan;
import model.Member;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.DefaultListCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.SpinnerDateModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.prefs.Preferences;

/** Main librarian workspace for books, members, and circulation. */
public class LibraryWindow extends JFrame {
    private static Color INK = new Color(35, 45, 43);
    private static Color MUTED = new Color(111, 125, 120);
    private static Color PAPER = new Color(247, 248, 244);
    private static Color SURFACE = Color.WHITE;
    private static Color GREEN = new Color(31, 105, 82);
    private static Color PRIMARY_BUTTON_SURFACE = new Color(175, 216, 190);
    private static Color ACCENT_TEXT = Color.WHITE;
    private static Color LINE = new Color(222, 228, 222);
    private static Color SELECTED = new Color(222, 238, 230);
    private static Color TABLE_HEADER = new Color(242, 245, 240);
    private static Color TAB_TEXT = new Color(35, 45, 43);
    private static Color TAB_ACTIVE = new Color(222, 238, 230);
    private static Color TAB_IDLE = Color.WHITE;
    private static Color BUTTON_SURFACE = Color.WHITE;
    private static Color BUTTON_TEXT = new Color(35, 45, 43);
    private static final Font BODY = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font DISPLAY = new Font("Georgia", Font.BOLD, 26);
    private static final Preferences SETTINGS = Preferences.userNodeForPackage(LibraryWindow.class);

    private final BookDAO bookDAO = new BookDAO();
    private final MemberDAO memberDAO = new MemberDAO();
    private final CategoryDAO categoryDAO = new CategoryDAO();
    private final LoanDAO loanDAO = new LoanDAO();
    private final DefaultTableModel booksModel = model("ID", "Title", "Author", "Category", "Status");
    private final DefaultTableModel membersModel = model("ID", "Member", "Email");
    private final DefaultTableModel loansModel = model("Loan", "Member", "Book", "Checked out", "Due date", "Status");
    private final JTable booksTable = table(booksModel);
    private final JTable membersTable = table(membersModel);
    private final JTable loansTable = table(loansModel);
    private final JTextField searchField = new JTextField(24);
    private final JComboBox<String> availabilityFilter = new JComboBox<>(
            new String[]{"All books", "Available", "Checked out"});
    private final JComboBox<String> categoryFilter = new JComboBox<>(new String[]{"All categories"});
    private final JComboBox<String> authorFilter = new JComboBox<>(new String[]{"All authors"});
    private boolean updatingBookFilters;
    private JTabbedPane tabs;
    private Theme theme = loadTheme();
    private final JLabel booksCount = new JLabel("0");
    private final JLabel membersCount = new JLabel("0");
    private final JLabel activeCount = new JLabel("0");
    private final JLabel overdueCount = new JLabel("0");

    public LibraryWindow() {
        super("Fieldnote Library | Lending Desk");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(900, 620));
        setSize(1180, 780);
        setLocationRelativeTo(null);
        getContentPane().setBackground(PAPER);
        applyLookAndFeel();
        configureThemePalette();
        styleTable(booksTable);
        styleTable(membersTable);
        styleTable(loansTable);
        searchField.addActionListener(e -> refreshBooks(searchField.getText().trim()));
        availabilityFilter.addActionListener(e -> refreshBooks(searchField.getText().trim()));
        categoryFilter.addActionListener(e -> {
            if (!updatingBookFilters) refreshBooks(searchField.getText().trim());
        });
        authorFilter.addActionListener(e -> {
            if (!updatingBookFilters) refreshBooks(searchField.getText().trim());
        });
        styleBookFilters();
        buildWindow();
        refreshAll();
    }

    private void buildWindow() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(PAPER);
        root.add(buildHeader(), BorderLayout.NORTH);

        tabs = new JTabbedPane();
        tabs.setFont(BODY.deriveFont(Font.BOLD));
        tabs.setForeground(INK);
        tabs.setBackground(PAPER);
        tabs.setOpaque(true);
        tabs.addTab("Overview", buildOverview());
        tabs.addTab("Books", buildBooks());
        tabs.addTab("Members", buildMembers());
        tabs.addTab("Circulation", buildCirculation());
        tabs.setBorder(BorderFactory.createEmptyBorder(8, 24, 20, 24));
        styleTabs();
        root.add(tabs, BorderLayout.CENTER);
        setContentPane(root);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(SURFACE);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, LINE),
                BorderFactory.createEmptyBorder(18, 28, 18, 28)));

        JPanel brand = new JPanel(new BorderLayout(14, 0));
        brand.setOpaque(false);
        JLabel mark = new JLabel("F", SwingConstants.CENTER);
        mark.setPreferredSize(new Dimension(42, 42));
        mark.setOpaque(true);
        mark.setBackground(GREEN);
        mark.setForeground(ACCENT_TEXT);
        mark.setFont(new Font("Georgia", Font.BOLD, 22));
        JLabel title = new JLabel("FIELDNOTE  /  LIBRARY DESK");
        title.setFont(BODY.deriveFont(Font.BOLD, 14));
        title.setForeground(INK);
        JLabel subtitle = new JLabel("Lending, membership, and collection records");
        subtitle.setFont(BODY.deriveFont(12f));
        subtitle.setForeground(MUTED);
        JPanel titles = new JPanel(new GridBagLayout());
        titles.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.anchor = GridBagConstraints.WEST;
        c.gridy = 0;
        titles.add(title, c);
        c.gridy = 1;
        c.insets = new Insets(4, 0, 0, 0);
        titles.add(subtitle, c);
        brand.add(mark, BorderLayout.WEST);
        brand.add(titles, BorderLayout.CENTER);

        JButton reset = quietButton("Reset sample data");
        reset.addActionListener(e -> resetDatabase());
        JButton settings = settingsButton();
        settings.addActionListener(e -> showSettings());
        JPanel headerActions = new JPanel();
        headerActions.setOpaque(false);
        headerActions.add(settings);
        headerActions.add(reset);
        header.add(brand, BorderLayout.WEST);
        header.add(headerActions, BorderLayout.EAST);
        return header;
    }

    private JPanel buildOverview() {
        JPanel panel = pagePanel();
        JLabel heading = heading("A clear view of the library");
        JLabel intro = label("Today’s collection and lending activity, at a glance.", MUTED);
        panel.add(heading);
        panel.add(intro);

        JPanel stats = new JPanel(new GridBagLayout());
        stats.setOpaque(false);
        addStat(stats, 0, "BOOKS IN CATALOG", booksCount, "Titles in the collection");
        addStat(stats, 1, "REGISTERED MEMBERS", membersCount, "Active library accounts");
        addStat(stats, 2, "CURRENT LOANS", activeCount, "Books away from the shelves");
        addStat(stats, 3, "OVERDUE", overdueCount, "Needs a follow-up");
        panel.add(stats);

        JPanel lower = new JPanel(new BorderLayout(0, 12));
        lower.setOpaque(false);
        lower.setBorder(BorderFactory.createEmptyBorder(24, 0, 0, 0));
        lower.add(sectionTitle("Due back soon"), BorderLayout.NORTH);
        JTable dueTable = table(new DefaultTableModel(new Object[]{"Member", "Book", "Due date"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        });
        if (theme == Theme.DARK) {
            dueTable.getTableHeader().setForeground(Color.BLACK);
            dueTable.getTableHeader().setBackground(TAB_IDLE);
        } else if (theme == Theme.PINK) {
            dueTable.getTableHeader().setForeground(new Color(74, 38, 52));
            dueTable.getTableHeader().setBackground(TABLE_HEADER);
        } else if (theme != Theme.LIGHT) {
            dueTable.getTableHeader().setForeground(INK);
            dueTable.getTableHeader().setBackground(TABLE_HEADER);
        }
        JScrollPane dueScroll = new JScrollPane(dueTable);
        styleScroll(dueScroll);
        lower.add(dueScroll, BorderLayout.CENTER);
        panel.add(lower);
        refreshOverviewDue(dueTable);
        return panel;
    }

    private void addStat(JPanel parent, int column, String caption, JLabel value, String detail) {
        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LINE), BorderFactory.createEmptyBorder(16, 18, 16, 18)));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridy = 0;
        JLabel cap = label(caption, MUTED);
        cap.setFont(BODY.deriveFont(Font.BOLD, 10));
        card.add(cap, c);
        value.setFont(DISPLAY.deriveFont(30f));
        value.setForeground(INK);
        c.gridy++;
        c.insets = new Insets(7, 0, 0, 0);
        card.add(value, c);
        c.gridy++;
        JLabel desc = label(detail, MUTED);
        desc.setFont(BODY.deriveFont(11f));
        card.add(desc, c);

        GridBagConstraints outer = new GridBagConstraints();
        outer.gridx = column;
        outer.gridy = 0;
        outer.weightx = 1;
        outer.fill = GridBagConstraints.HORIZONTAL;
        outer.insets = new Insets(18, column == 0 ? 0 : 8, 0, column == 3 ? 0 : 8);
        parent.add(card, outer);
    }

    private JPanel buildBooks() {
        JPanel panel = pagePanel();
        JPanel top = new JPanel(new BorderLayout(10, 0));
        top.setOpaque(false);
        JPanel titles = new JPanel(new GridBagLayout());
        titles.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.anchor = GridBagConstraints.WEST;
        titles.add(heading("Books"), c);
        c.gridy = 1;
        c.insets = new Insets(4, 0, 0, 0);
        titles.add(label("Browse and maintain the library catalog.", MUTED), c);
        JPanel tools = new JPanel();
        tools.setOpaque(false);
        searchField.setPreferredSize(new Dimension(230, 34));
        searchField.setFont(BODY);
        searchField.setToolTipText("Search by title or author");
        tools.add(searchField);
        JButton search = quietButton("Search");
        search.addActionListener(e -> refreshBooks(searchField.getText().trim()));
        tools.add(search);
        top.add(titles, BorderLayout.WEST);
        top.add(tools, BorderLayout.EAST);
        panel.add(top);

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        filters.setOpaque(false);
        filters.setBorder(BorderFactory.createEmptyBorder(14, 0, 0, 0));
        filters.add(label("Filter by", MUTED));
        filters.add(categoryFilter);
        filters.add(authorFilter);
        filters.add(availabilityFilter);
        panel.add(filters);

        panel.add(tablePanel(booksTable));
        JPanel actions = new JPanel();
        actions.setOpaque(false);
        JButton add = primaryButton("Add book");
        add.addActionListener(e -> showBookDialog(null));
        JButton findOnline = quietButton("Find series online");
        findOnline.addActionListener(e -> findSeriesOnline());
        JButton edit = quietButton("Edit selected");
        edit.addActionListener(e -> editSelectedBook());
        JButton delete = quietButton("Delete selected");
        delete.addActionListener(e -> deleteSelectedBook());
        JButton checkout = quietButton("Check out");
        checkout.addActionListener(e -> checkoutSelectedBook());
        actions.add(add);
        actions.add(findOnline);
        actions.add(edit);
        actions.add(delete);
        actions.add(checkout);
        panel.add(actions);
        return panel;
    }

    private JPanel buildMembers() {
        JPanel panel = pagePanel();
        panel.add(heading("Members"));
        panel.add(label("Keep contact details current and review borrowing history.", MUTED));
        panel.add(tablePanel(membersTable));
        JPanel actions = new JPanel();
        actions.setOpaque(false);
        JButton add = primaryButton("Add member");
        add.addActionListener(e -> showMemberDialog(null));
        JButton edit = quietButton("Edit selected");
        edit.addActionListener(e -> editSelectedMember());
        JButton delete = quietButton("Delete selected");
        delete.addActionListener(e -> deleteSelectedMember());
        JButton history = quietButton("Borrowing history");
        history.addActionListener(e -> showSelectedHistory());
        actions.add(add);
        actions.add(edit);
        actions.add(delete);
        actions.add(history);
        panel.add(actions);
        return panel;
    }

    private JPanel buildCirculation() {
        JPanel panel = pagePanel();
        panel.add(heading("Circulation"));
        panel.add(label("Track checked-out books, overdue items, and returns.", MUTED));
        panel.add(tablePanel(loansTable));
        JPanel actions = new JPanel();
        actions.setOpaque(false);
        JButton checkout = primaryButton("Check out a book");
        checkout.addActionListener(e -> showCheckoutDialog());
        JButton returned = quietButton("Mark returned");
        returned.addActionListener(e -> returnSelectedLoan());
        JButton dueDate = quietButton("Adjust due date");
        dueDate.addActionListener(e -> adjustSelectedDueDate());
        JButton fees = quietButton("Overdue fees");
        fees.addActionListener(e -> showOverdueFees());
        JButton counts = quietButton("Loan totals");
        counts.addActionListener(e -> showLoanCounts());
        actions.add(checkout);
        actions.add(returned);
        actions.add(dueDate);
        actions.add(fees);
        actions.add(counts);
        panel.add(actions);
        return panel;
    }

    private void refreshAll() {
        refreshBooks(searchField.getText().trim());
        refreshMembers();
        refreshLoans();
    }

    private void findSeriesOnline() {
        String seriesTitle = JOptionPane.showInputDialog(this,
                "Enter the series title. The catalog will save this title only, not individual volumes.",
                "Find a series online", JOptionPane.PLAIN_MESSAGE);
        if (seriesTitle == null || seriesTitle.trim().isEmpty()) return;
        seriesTitle = seriesTitle.trim();
        final String titleToAdd = seriesTitle;

        JDialog progress = new JDialog(this, "Searching Open Library", true);
        JLabel status = label("Searching for the creator and category...", MUTED);
        status.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));
        progress.add(status);
        progress.pack();
        progress.setLocationRelativeTo(this);

        SwingWorker<OpenLibrarySeriesSearch.SearchResult, Void> worker =
                new SwingWorker<OpenLibrarySeriesSearch.SearchResult, Void>() {
            @Override
            protected OpenLibrarySeriesSearch.SearchResult doInBackground() throws Exception {
                return OpenLibrarySeriesSearch.search(titleToAdd);
            }

            @Override
            protected void done() {
                progress.dispose();
                try {
                    OpenLibrarySeriesSearch.SearchResult result = get();
                    if (result == null) {
                        showWarning("No matching series was found. Check the title and try again.");
                        return;
                    }
                    List<Category> categories = categoryDAO.findAll();
                    JComboBox<CategoryChoice> category = new JComboBox<>();
                    category.addItem(new CategoryChoice(null, "Uncategorized"));
                    Category suggestion = suggestCategory(result.getSubjects(), categories);
                    int selectedIndex = 0;
                    for (Category item : categories) {
                        category.addItem(new CategoryChoice(item.getCategoryId(), item.getName()));
                        if (suggestion != null && item.getCategoryId() == suggestion.getCategoryId()) {
                            selectedIndex = category.getItemCount() - 1;
                        }
                    }
                    category.setSelectedIndex(selectedIndex);
                    category.setFont(BODY);
                    category.setBackground(SURFACE);
                    category.setForeground(INK);
                    JLabel titleValue = label(titleToAdd, INK);
                    JLabel authorValue = label(result.getAuthor(), INK);
                    JPanel form = formPanel();
                    addFormRow(form, 0, "Series title", titleValue);
                    addFormRow(form, 1, "Author", authorValue);
                    addFormRow(form, 2, "Category", category);
                    JLabel note = label("Volume details are not imported.", MUTED);
                    note.setBorder(BorderFactory.createEmptyBorder(10, 5, 0, 5));
                    int choice = JOptionPane.showConfirmDialog(LibraryWindow.this,
                            new Object[]{form, note}, "Review series", JOptionPane.OK_CANCEL_OPTION,
                            JOptionPane.PLAIN_MESSAGE);
                    if (choice == JOptionPane.YES_OPTION) {
                        perform(() -> {
                            Integer categoryId = ((CategoryChoice) category.getSelectedItem()).id;
                            bookDAO.insert(titleToAdd, result.getAuthor(), categoryId);
                            refreshAll();
                        });
                    }
                } catch (Exception e) {
                    Throwable cause = e.getCause() == null ? e : e.getCause();
                    showError("Could not search Open Library: " + cause.getMessage());
                }
            }
        };
        worker.execute();
        progress.setVisible(true);
    }

    private Category suggestCategory(List<String> subjects, List<Category> categories) {
        Category bestMatch = null;
        int bestScore = 0;
        for (String subject : subjects) {
            String normalized = subject.toLowerCase(java.util.Locale.ROOT).replace('-', ' ').trim();
            for (Category category : categories) {
                if (normalized.equals(category.getName().toLowerCase(java.util.Locale.ROOT))) {
                    int score = category.getName().equalsIgnoreCase("Fiction") ? 1 : 4;
                    if (score > bestScore) {
                        bestMatch = category;
                        bestScore = score;
                    }
                }
            }

            String suggestedName = null;
            int suggestedScore = 0;
            if (normalized.contains("manga") || normalized.contains("japanese comics")) {
                suggestedName = "Manga";
                suggestedScore = 6;
            } else if (normalized.contains("romance") || normalized.contains("love stories")) {
                suggestedName = "Romance";
                suggestedScore = 5;
            } else if (normalized.contains("science fiction") || normalized.contains("sci fi")
                    || normalized.contains("space opera") || normalized.contains("cyberpunk")) {
                suggestedName = "Science Fiction";
                suggestedScore = 5;
            } else if (normalized.contains("mystery") || normalized.contains("detective")
                    || normalized.contains("crime fiction") || normalized.contains("thriller")) {
                suggestedName = "Mystery";
                suggestedScore = 4;
            } else if (normalized.contains("history") || normalized.contains("historical")) {
                suggestedName = "History";
                suggestedScore = 4;
            } else if (normalized.equals("science") || normalized.startsWith("science ")) {
                suggestedName = "Science";
                suggestedScore = 4;
            } else if (normalized.contains("fantasy") || normalized.contains("fairy tales")) {
                suggestedName = "Fantasy";
                suggestedScore = 4;
            } else if (normalized.contains("horror") || normalized.contains("ghost stories")) {
                suggestedName = "Horror";
                suggestedScore = 4;
            } else if (normalized.contains("biography") || normalized.contains("autobiography")
                    || normalized.contains("memoir")) {
                suggestedName = "Biography";
                suggestedScore = 4;
            } else if (normalized.contains("poetry") || normalized.contains("poems")) {
                suggestedName = "Poetry";
                suggestedScore = 4;
            } else if (normalized.contains("children") || normalized.contains("juvenile")) {
                suggestedName = "Children's";
                suggestedScore = 4;
            } else if (normalized.equals("nonfiction") || normalized.contains("non fiction")) {
                suggestedName = "Nonfiction";
                suggestedScore = 3;
            } else if (normalized.contains("fiction") || normalized.contains("fantasy")
                    || normalized.contains("comic")
                    || normalized.contains("graphic novel") || normalized.contains("adventure")) {
                suggestedName = "Fiction";
                suggestedScore = 1;
            }
            if (suggestedName != null && suggestedScore > bestScore) {
                for (Category category : categories) {
                    if (category.getName().equalsIgnoreCase(suggestedName)) {
                        bestMatch = category;
                        bestScore = suggestedScore;
                        break;
                    }
                }
            }
        }
        return bestMatch;
    }

    private void showSettings() {
        JComboBox<Theme> appearance = new JComboBox<>(Theme.values());
        appearance.setSelectedItem(theme);
        appearance.setFont(BODY);
        styleThemeSelector(appearance);
        JPanel settings = new JPanel();
        settings.setLayout(new javax.swing.BoxLayout(settings, javax.swing.BoxLayout.Y_AXIS));
        settings.setBackground(SURFACE);
        settings.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        JLabel heading = new JLabel("Appearance");
        heading.setFont(BODY.deriveFont(Font.BOLD, 14));
        heading.setForeground(INK);
        JLabel description = new JLabel("Choose the color theme for the library desk.");
        description.setFont(BODY);
        description.setForeground(MUTED);
        JPanel row = new JPanel(new BorderLayout(18, 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(14, 0, 0, 0));
        JLabel label = new JLabel("Theme");
        label.setFont(BODY.deriveFont(Font.BOLD));
        label.setForeground(INK);
        row.add(label, BorderLayout.WEST);
        row.add(appearance, BorderLayout.EAST);
        settings.add(heading);
        settings.add(description);
        settings.add(row);

        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");
        styleSettingsButton(okButton);
        styleSettingsButton(cancelButton);
        boolean[] accepted = {false};
        JDialog dialog = new JDialog(this, "Settings", true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.getContentPane().setBackground(SURFACE);
        dialog.setLayout(new BorderLayout(0, 14));
        dialog.add(settings, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setBackground(SURFACE);
        actions.add(cancelButton);
        actions.add(okButton);
        actions.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));
        dialog.add(actions, BorderLayout.SOUTH);
        okButton.addActionListener(e -> {
            accepted[0] = true;
            dialog.dispose();
        });
        cancelButton.addActionListener(e -> dialog.dispose());
        dialog.getRootPane().setDefaultButton(okButton);
        dialog.pack();
        dialog.setMinimumSize(new Dimension(390, 190));
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
        if (accepted[0] && appearance.getSelectedItem() != theme) {
            theme = (Theme) appearance.getSelectedItem();
            SETTINGS.put("appearance", theme.name());
            applyTheme();
        }
    }

    private static Theme loadTheme() {
        try {
            return Theme.valueOf(SETTINGS.get("appearance", Theme.LIGHT.name()));
        } catch (IllegalArgumentException e) {
            return Theme.LIGHT;
        }
    }

    private void styleThemeSelector(JComboBox<Theme> appearance) {
        Color selectorBackground = Color.WHITE;
        Color selectorForeground = Color.BLACK;
        appearance.setBackground(selectorBackground);
        appearance.setForeground(selectorForeground);
        appearance.setOpaque(true);
        appearance.setRenderer(new DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(
                    JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel cell = (JLabel) super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);
                cell.setFont(BODY);
                cell.setForeground(selectorForeground);
                cell.setBackground(isSelected ? new Color(222, 238, 230) : selectorBackground);
                cell.setOpaque(true);
                return cell;
            }
        });
    }

    private void applyTheme() {
        int selectedTab = tabs.getSelectedIndex();
        configureThemePalette();
        getContentPane().setBackground(PAPER);
        styleTable(booksTable);
        styleTable(membersTable);
        styleTable(loansTable);
        searchField.setBackground(SURFACE);
        searchField.setForeground(INK);
        styleBookFilters();
        buildWindow();
        tabs.setSelectedIndex(Math.min(selectedTab, tabs.getTabCount() - 1));
        styleTabs();
        refreshAll();
        revalidate();
        repaint();
    }

    private void configureThemePalette() {
        if (theme == Theme.DARK) {
            INK = new Color(231, 238, 233);
            MUTED = new Color(169, 184, 174);
            PAPER = new Color(25, 34, 30);
            SURFACE = new Color(36, 48, 42);
            GREEN = new Color(91, 161, 125);
            PRIMARY_BUTTON_SURFACE = new Color(220, 229, 221);
            ACCENT_TEXT = new Color(16, 30, 23);
            LINE = new Color(63, 79, 69);
            SELECTED = new Color(54, 82, 65);
            TABLE_HEADER = new Color(44, 58, 50);
            TAB_TEXT = new Color(23, 32, 27);
            TAB_ACTIVE = new Color(220, 229, 221);
            TAB_IDLE = new Color(194, 207, 197);
            BUTTON_SURFACE = new Color(220, 229, 221);
            BUTTON_TEXT = Color.BLACK;
        } else if (theme == Theme.PINK) {
            INK = new Color(74, 38, 52);
            MUTED = new Color(133, 86, 104);
            PAPER = new Color(255, 241, 246);
            SURFACE = new Color(255, 250, 252);
            GREEN = new Color(181, 91, 125);
            PRIMARY_BUTTON_SURFACE = new Color(246, 202, 218);
            ACCENT_TEXT = Color.BLACK;
            LINE = new Color(232, 190, 207);
            SELECTED = new Color(249, 218, 229);
            TABLE_HEADER = new Color(250, 224, 234);
            TAB_TEXT = new Color(74, 38, 52);
            TAB_ACTIVE = new Color(246, 202, 218);
            TAB_IDLE = new Color(255, 235, 242);
            BUTTON_SURFACE = new Color(255, 235, 242);
            BUTTON_TEXT = new Color(74, 38, 52);
        } else if (theme == Theme.BLUE) {
            INK = new Color(33, 51, 76);
            MUTED = new Color(91, 116, 148);
            PAPER = new Color(239, 246, 255);
            SURFACE = new Color(250, 252, 255);
            GREEN = new Color(76, 126, 184);
            PRIMARY_BUTTON_SURFACE = new Color(203, 222, 247);
            ACCENT_TEXT = Color.BLACK;
            LINE = new Color(190, 211, 239);
            SELECTED = new Color(216, 231, 250);
            TABLE_HEADER = new Color(226, 238, 253);
            TAB_TEXT = INK;
            TAB_ACTIVE = new Color(203, 222, 247);
            TAB_IDLE = new Color(233, 242, 254);
            BUTTON_SURFACE = new Color(233, 242, 254);
            BUTTON_TEXT = INK;
        } else if (theme == Theme.GREEN) {
            INK = new Color(38, 61, 48);
            MUTED = new Color(94, 126, 102);
            PAPER = new Color(241, 248, 239);
            SURFACE = new Color(251, 253, 249);
            GREEN = new Color(91, 145, 99);
            PRIMARY_BUTTON_SURFACE = new Color(207, 229, 201);
            ACCENT_TEXT = Color.BLACK;
            LINE = new Color(194, 218, 188);
            SELECTED = new Color(222, 238, 216);
            TABLE_HEADER = new Color(231, 243, 225);
            TAB_TEXT = INK;
            TAB_ACTIVE = new Color(207, 229, 201);
            TAB_IDLE = new Color(237, 247, 232);
            BUTTON_SURFACE = new Color(237, 247, 232);
            BUTTON_TEXT = INK;
        } else if (theme == Theme.YELLOW) {
            INK = new Color(74, 60, 27);
            MUTED = new Color(137, 112, 59);
            PAPER = new Color(255, 250, 230);
            SURFACE = new Color(255, 253, 244);
            GREEN = new Color(177, 139, 49);
            PRIMARY_BUTTON_SURFACE = new Color(246, 226, 164);
            ACCENT_TEXT = Color.BLACK;
            LINE = new Color(231, 211, 151);
            SELECTED = new Color(250, 237, 195);
            TABLE_HEADER = new Color(251, 241, 207);
            TAB_TEXT = INK;
            TAB_ACTIVE = new Color(246, 226, 164);
            TAB_IDLE = new Color(255, 246, 216);
            BUTTON_SURFACE = new Color(255, 246, 216);
            BUTTON_TEXT = INK;
        } else if (theme == Theme.RED) {
            INK = new Color(76, 38, 39);
            MUTED = new Color(139, 82, 81);
            PAPER = new Color(255, 242, 239);
            SURFACE = new Color(255, 250, 248);
            GREEN = new Color(177, 77, 72);
            PRIMARY_BUTTON_SURFACE = new Color(245, 202, 194);
            ACCENT_TEXT = Color.BLACK;
            LINE = new Color(233, 188, 181);
            SELECTED = new Color(250, 220, 214);
            TABLE_HEADER = new Color(251, 226, 220);
            TAB_TEXT = INK;
            TAB_ACTIVE = new Color(245, 202, 194);
            TAB_IDLE = new Color(255, 235, 230);
            BUTTON_SURFACE = new Color(255, 235, 230);
            BUTTON_TEXT = INK;
        } else if (theme == Theme.ORANGE) {
            INK = new Color(79, 51, 29);
            MUTED = new Color(143, 99, 60);
            PAPER = new Color(255, 246, 234);
            SURFACE = new Color(255, 251, 245);
            GREEN = new Color(190, 116, 53);
            PRIMARY_BUTTON_SURFACE = new Color(250, 216, 177);
            ACCENT_TEXT = Color.BLACK;
            LINE = new Color(235, 199, 157);
            SELECTED = new Color(252, 229, 199);
            TABLE_HEADER = new Color(253, 234, 210);
            TAB_TEXT = INK;
            TAB_ACTIVE = new Color(250, 216, 177);
            TAB_IDLE = new Color(255, 239, 218);
            BUTTON_SURFACE = new Color(255, 239, 218);
            BUTTON_TEXT = INK;
        } else {
            INK = new Color(35, 45, 43);
            MUTED = new Color(111, 125, 120);
            PAPER = new Color(247, 248, 244);
            SURFACE = Color.WHITE;
            GREEN = new Color(31, 105, 82);
            PRIMARY_BUTTON_SURFACE = new Color(175, 216, 190);
            ACCENT_TEXT = Color.WHITE;
            LINE = new Color(222, 228, 222);
            SELECTED = new Color(222, 238, 230);
            TABLE_HEADER = new Color(242, 245, 240);
            TAB_TEXT = INK;
            TAB_ACTIVE = SELECTED;
            TAB_IDLE = SURFACE;
            BUTTON_SURFACE = SURFACE;
            BUTTON_TEXT = INK;
        }
        UIManager.put("Panel.background", SURFACE);
        UIManager.put("Button.background", SURFACE);
        UIManager.put("Button.foreground", INK);
        UIManager.put("TabbedPane.background", SURFACE);
        UIManager.put("TabbedPane.foreground", INK);
        UIManager.put("TabbedPane.selected", SELECTED);
        UIManager.put("TabbedPane.contentAreaColor", PAPER);
        UIManager.put("TabbedPane.unselectedBackground", SURFACE);
        UIManager.put("OptionPane.background", SURFACE);
        UIManager.put("OptionPane.messageForeground", INK);
    }

    private void refreshBooks(String keyword) {
        perform(() -> {
            List<Book> catalog = bookDAO.findAll();
            updateBookFilterOptions(catalog);
            booksModel.setRowCount(0);
            String selectedCategory = (String) categoryFilter.getSelectedItem();
            String selectedAuthor = (String) authorFilter.getSelectedItem();
            for (Book book : catalog) {
                if (!keyword.isEmpty()
                        && !book.getTitle().toLowerCase(java.util.Locale.ROOT)
                        .contains(keyword.toLowerCase(java.util.Locale.ROOT))
                        && !book.getAuthor().toLowerCase(java.util.Locale.ROOT)
                        .contains(keyword.toLowerCase(java.util.Locale.ROOT))) continue;
                if (selectedCategory != null && !"All categories".equals(selectedCategory)) {
                    String bookCategory = book.getCategoryName() == null ? "Uncategorized" : book.getCategoryName();
                    if (!selectedCategory.equals(bookCategory)) continue;
                }
                if (selectedAuthor != null && !"All authors".equals(selectedAuthor)
                        && !selectedAuthor.equals(book.getAuthor())) continue;
                String status = (String) availabilityFilter.getSelectedItem();
                if ("Available".equals(status) && !book.isAvailable()) continue;
                if ("Checked out".equals(status) && book.isAvailable()) continue;
                booksModel.addRow(new Object[]{book.getBookId(), book.getTitle(), book.getAuthor(),
                        book.getCategoryName() == null ? "Uncategorized" : book.getCategoryName(),
                        book.isAvailable() ? "Available" : "Checked out"});
            }
            booksCount.setText(String.valueOf(catalog.size()));
        });
    }

    private void updateBookFilterOptions(List<Book> catalog) {
        String selectedCategory = (String) categoryFilter.getSelectedItem();
        String selectedAuthor = (String) authorFilter.getSelectedItem();
        TreeSet<String> categories = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        TreeSet<String> authors = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (Book book : catalog) {
            categories.add(book.getCategoryName() == null ? "Uncategorized" : book.getCategoryName());
            authors.add(book.getAuthor());
        }

        updatingBookFilters = true;
        categoryFilter.removeAllItems();
        categoryFilter.addItem("All categories");
        for (String category : categories) categoryFilter.addItem(category);
        if (selectedCategory != null && categories.contains(selectedCategory)) {
            categoryFilter.setSelectedItem(selectedCategory);
        } else {
            categoryFilter.setSelectedItem("All categories");
        }

        authorFilter.removeAllItems();
        authorFilter.addItem("All authors");
        for (String author : authors) authorFilter.addItem(author);
        if (selectedAuthor != null && authors.contains(selectedAuthor)) {
            authorFilter.setSelectedItem(selectedAuthor);
        } else {
            authorFilter.setSelectedItem("All authors");
        }
        updatingBookFilters = false;
    }

    private void styleBookFilters() {
        for (JComboBox<String> filter : java.util.Arrays.asList(
            categoryFilter, authorFilter, availabilityFilter)) {
            filter.setFont(BODY);
            filter.setBackground(SURFACE);
            filter.setForeground(INK);
        }
        categoryFilter.setToolTipText("Filter by category");
        authorFilter.setToolTipText("Filter by author");
        availabilityFilter.setToolTipText("Filter by checkout status");
    }

    private void refreshMembers() {
        perform(() -> {
            List<Member> members = memberDAO.findAll();
            membersModel.setRowCount(0);
            for (Member member : members) {
                membersModel.addRow(new Object[]{member.getMemberId(), member.getName(), member.getEmail()});
            }
            membersCount.setText(String.valueOf(members.size()));
        });
    }

    private void refreshLoans() {
        perform(() -> {
            List<Loan> loans = loanDAO.findActive();
            loansModel.setRowCount(0);
            int overdue = 0;
            LocalDate today = LocalDate.now();
            for (Loan loan : loans) {
                boolean isOverdue = loan.getDueDate().isBefore(today);
                if (isOverdue) overdue++;
                loansModel.addRow(new Object[]{loan.getLoanId(), loan.getMemberName(), loan.getBookTitle(),
                        loan.getCheckoutDate(), loan.getDueDate(), isOverdue ? "Overdue" : "On loan"});
            }
            activeCount.setText(String.valueOf(loans.size()));
            overdueCount.setText(String.valueOf(overdue));
        });
    }

    private void refreshOverviewDue(JTable dueTable) {
        perform(() -> {
            DefaultTableModel dueModel = (DefaultTableModel) dueTable.getModel();
            for (Loan loan : loanDAO.findActive()) {
                dueModel.addRow(new Object[]{loan.getMemberName(), loan.getBookTitle(), loan.getDueDate()});
            }
        });
    }

    private void showBookDialog(Book existing) {
        perform(() -> {
            JTextField title = new JTextField(existing == null ? "" : existing.getTitle(), 24);
            JTextField author = new JTextField(existing == null ? "" : existing.getAuthor(), 24);
            List<Category> categories = categoryDAO.findAll();
            JComboBox<CategoryChoice> category = new JComboBox<>();
            category.addItem(new CategoryChoice(null, "Uncategorized"));
            int selected = 0;
            for (Category item : categories) {
                category.addItem(new CategoryChoice(item.getCategoryId(), item.getName()));
                if (existing != null && item.getCategoryId() == (existing.getCategoryId() == null ? -1 : existing.getCategoryId())) {
                    selected = category.getItemCount() - 1;
                }
            }
            category.setSelectedIndex(selected);
            JPanel form = formPanel();
            addFormRow(form, 0, "Title", title);
            addFormRow(form, 1, "Author", author);
            addFormRow(form, 2, "Category", category);
            int result = JOptionPane.showConfirmDialog(this, form,
                    existing == null ? "Add a book" : "Edit book", JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE);
            if (result != JOptionPane.OK_OPTION) return;
            String bookTitle = title.getText().trim();
            String bookAuthor = author.getText().trim();
            if (bookTitle.isEmpty() || bookAuthor.isEmpty()) {
                showWarning("Title and author are required.");
                return;
            }
            Integer categoryId = ((CategoryChoice) category.getSelectedItem()).id;
            if (existing == null) {
                bookDAO.insert(bookTitle, bookAuthor, categoryId);
            } else {
                existing.setTitle(bookTitle);
                existing.setAuthor(bookAuthor);
                existing.setCategoryId(categoryId);
                bookDAO.update(existing);
            }
            refreshAll();
        });
    }

    private void showMemberDialog(Member existing) {
        JTextField name = new JTextField(existing == null ? "" : existing.getName(), 24);
        JTextField email = new JTextField(existing == null ? "" : existing.getEmail(), 24);
        JPanel form = formPanel();
        addFormRow(form, 0, "Full name", name);
        addFormRow(form, 1, "Email address", email);
        int result = JOptionPane.showConfirmDialog(this, form,
                existing == null ? "Add a member" : "Edit member", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;
        String memberName = name.getText().trim();
        String memberEmail = email.getText().trim();
        if (memberName.isEmpty() || !validEmail(memberEmail)) {
            showWarning("Enter a name and a valid email address.");
            return;
        }
        perform(() -> {
            if (existing == null) {
                memberDAO.insert(memberName, memberEmail);
            } else {
                existing.setName(memberName);
                existing.setEmail(memberEmail);
                memberDAO.update(existing);
            }
            refreshAll();
        });
    }

    private void editSelectedBook() {
        int row = booksTable.getSelectedRow();
        if (row < 0) { showWarning("Select a book first."); return; }
        int id = (int) booksModel.getValueAt(booksTable.convertRowIndexToModel(row), 0);
        perform(() -> showBookDialog(bookDAO.findById(id)));
    }

    private void deleteSelectedBook() {
        int id = selectedId(booksTable, booksModel, "book");
        if (id < 0) return;
        if (confirm("Delete this book? Books with loan history cannot be deleted.")) {
            perform(() -> { bookDAO.delete(id); refreshAll(); });
        }
    }

    private void editSelectedMember() {
        int row = membersTable.getSelectedRow();
        if (row < 0) { showWarning("Select a member first."); return; }
        int id = (int) membersModel.getValueAt(membersTable.convertRowIndexToModel(row), 0);
        perform(() -> showMemberDialog(memberDAO.findById(id)));
    }

    private void deleteSelectedMember() {
        int id = selectedId(membersTable, membersModel, "member");
        if (id < 0) return;
        if (confirm("Delete this member? Members with loan history cannot be deleted.")) {
            perform(() -> { memberDAO.delete(id); refreshAll(); });
        }
    }

    private void checkoutSelectedBook() {
        int row = booksTable.getSelectedRow();
        if (row < 0) { showWarning("Select a book first."); return; }
        int id = (int) booksModel.getValueAt(booksTable.convertRowIndexToModel(row), 0);
        perform(() -> showCheckoutDialog(id));
    }

    private void showCheckoutDialog() { showCheckoutDialog(null); }

    private void showCheckoutDialog(Integer selectedBookId) {
        perform(() -> {
            List<Member> members = memberDAO.findAll();
            List<Book> books = bookDAO.findAll();
            JComboBox<MemberChoice> member = new JComboBox<>();
            for (Member item : members) member.addItem(new MemberChoice(item));
            JComboBox<BookChoice> book = new JComboBox<>();
            for (Book item : books) {
                if (item.isAvailable()) {
                    book.addItem(new BookChoice(item));
                    if (selectedBookId != null && selectedBookId == item.getBookId()) {
                        book.setSelectedIndex(book.getItemCount() - 1);
                    }
                }
            }
            if (members.isEmpty() || book.getItemCount() == 0) {
                showWarning(members.isEmpty() ? "Add a member before checking out books."
                        : "There are no available books to check out.");
                return;
            }
            JSpinner dueDate = dateSpinner(LocalDate.now().plusDays(14));
            JPanel form = formPanel();
            addFormRow(form, 0, "Member", member);
            addFormRow(form, 1, "Available book", book);
            addFormRow(form, 2, "Due date", dueDate);
            int result = JOptionPane.showConfirmDialog(this, form, "Check out a book",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (result == JOptionPane.OK_OPTION) {
                dueDate.commitEdit();
                LocalDate chosenDueDate = spinnerDate(dueDate);
                if (chosenDueDate.isBefore(LocalDate.now())) {
                    showWarning("The due date cannot be before today.");
                    return;
                }
                loanDAO.checkout(((MemberChoice) member.getSelectedItem()).member.getMemberId(),
                        ((BookChoice) book.getSelectedItem()).book.getBookId(), chosenDueDate);
                refreshAll();
            }
        });
    }

    private void adjustSelectedDueDate() {
        int selectedRow = loansTable.getSelectedRow();
        if (selectedRow < 0) {
            showWarning("Select an active loan first.");
            return;
        }
        int row = loansTable.convertRowIndexToModel(selectedRow);
        int loanId = (int) loansModel.getValueAt(row, 0);
        String bookTitle = String.valueOf(loansModel.getValueAt(row, 2));
        LocalDate checkoutDate = (LocalDate) loansModel.getValueAt(row, 3);
        LocalDate currentDueDate = (LocalDate) loansModel.getValueAt(row, 4);
        JSpinner dueDate = dateSpinner(currentDueDate);
        JPanel form = formPanel();
        addFormRow(form, 0, "Book", label(bookTitle, INK));
        addFormRow(form, 1, "Checked out", label(checkoutDate.toString(), INK));
        addFormRow(form, 2, "New due date", dueDate);
        int result = JOptionPane.showConfirmDialog(this, form, "Adjust due date",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        perform(() -> {
            dueDate.commitEdit();
            LocalDate chosenDueDate = spinnerDate(dueDate);
            if (chosenDueDate.isBefore(checkoutDate)) {
                showWarning("The due date cannot be before the checkout date.");
                return;
            }
            if (!loanDAO.updateDueDate(loanId, chosenDueDate)) {
                showWarning("That loan is no longer active.");
                return;
            }
            refreshLoans();
        });
    }

    private JSpinner dateSpinner(LocalDate initialDate) {
        Date value = Date.from(initialDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        JSpinner spinner = new JSpinner(new SpinnerDateModel(value, null, null, Calendar.DAY_OF_MONTH));
        JSpinner.DateEditor editor = new JSpinner.DateEditor(spinner, "yyyy-MM-dd");
        editor.getTextField().setBackground(SURFACE);
        editor.getTextField().setForeground(INK);
        spinner.setEditor(editor);
        spinner.setBackground(SURFACE);
        spinner.setForeground(INK);
        spinner.setPreferredSize(new Dimension(130, 32));
        spinner.setFont(BODY);
        return spinner;
    }

    private LocalDate spinnerDate(JSpinner spinner) {
        return ((Date) spinner.getValue()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private void returnSelectedLoan() {
        int row = loansTable.getSelectedRow();
        if (row < 0) { showWarning("Select an active loan first."); return; }
        int loanId = (int) loansModel.getValueAt(loansTable.convertRowIndexToModel(row), 0);
        perform(() -> {
            loanDAO.returnBook(loanId);
            refreshAll();
        });
    }

    private void showSelectedHistory() {
        int id = selectedId(membersTable, membersModel, "member");
        if (id < 0) return;
        perform(() -> {
            List<Loan> history = loanDAO.findByMember(id);
            String[] columns = {"Book", "Checked out", "Due", "Returned"};
            DefaultTableModel historyModel = new DefaultTableModel(columns, 0) {
                @Override public boolean isCellEditable(int row, int column) { return false; }
            };
            for (Loan loan : history) {
                historyModel.addRow(new Object[]{loan.getBookTitle(), loan.getCheckoutDate(), loan.getDueDate(),
                        loan.getReturnDate() == null ? "Not returned" : loan.getReturnDate()});
            }
            JTable historyTable = table(historyModel);
            JScrollPane scroll = new JScrollPane(historyTable);
            scroll.setPreferredSize(new Dimension(610, 300));
            JOptionPane.showMessageDialog(this, scroll, "Member borrowing history", JOptionPane.PLAIN_MESSAGE);
        });
    }

    private void showOverdueFees() {
        perform(() -> {
            LocalDate today = LocalDate.now();
            List<Loan> overdue = loanDAO.findOverdue(today);
            List<LateFeeCalculator.Fee> fees = LateFeeCalculator.calculate(overdue, today);
            String[] columns = {"Member", "Days late", "Fee owed"};
            DefaultTableModel feeModel = new DefaultTableModel(columns, 0) {
                @Override public boolean isCellEditable(int row, int column) { return false; }
            };
            for (LateFeeCalculator.Fee fee : fees) {
                feeModel.addRow(new Object[]{fee.getMemberName(), fee.getTotalDaysLate(),
                        String.format("$%.2f", fee.getAmount())});
            }
            JTable feeTable = table(feeModel);
            JScrollPane scroll = new JScrollPane(feeTable);
            scroll.setPreferredSize(new Dimension(480, 280));
            JOptionPane.showMessageDialog(this, scroll,
                    "Overdue fees  |  $" + String.format("%.2f", LateFeeCalculator.DAILY_RATE) + " per day",
                    JOptionPane.PLAIN_MESSAGE);
        });
    }

    private void showLoanCounts() {
        String input = JOptionPane.showInputDialog(this, "Minimum number of loans", "1");
        if (input == null) return;
        int minimum;
        try {
            minimum = Integer.parseInt(input.trim());
            if (minimum < 1) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showWarning("Enter a whole number greater than zero.");
            return;
        }
        perform(() -> {
            Map<String, Integer> counts = loanDAO.loanCountsPerMember(minimum);
            String[] columns = {"Member", "Total loans"};
            DefaultTableModel countModel = new DefaultTableModel(columns, 0) {
                @Override public boolean isCellEditable(int row, int column) { return false; }
            };
            for (Map.Entry<String, Integer> entry : counts.entrySet()) {
                countModel.addRow(new Object[]{entry.getKey(), entry.getValue()});
            }
            JTable countTable = table(countModel);
            JScrollPane scroll = new JScrollPane(countTable);
            scroll.setPreferredSize(new Dimension(420, 280));
            JOptionPane.showMessageDialog(this, scroll, "Loan totals by member", JOptionPane.PLAIN_MESSAGE);
        });
    }

    private void resetDatabase() {
        if (confirm("This will erase changes and reload the sample data. Continue?")) {
            perform(() -> { Database.rebuild(); refreshAll(); });
        }
    }

    private void perform(DatabaseTask task) {
        try {
            task.run();
        } catch (IllegalStateException e) {
            showWarning(e.getMessage());
        } catch (SQLException e) {
            showError(friendlyMessage(e));
        } catch (Exception e) {
            showError(e.getMessage() == null ? e.toString() : e.getMessage());
        }
    }

    private int selectedId(JTable table, DefaultTableModel model, String recordName) {
        int row = table.getSelectedRow();
        if (row < 0) {
            showWarning("Select a " + recordName + " first.");
            return -1;
        }
        return (int) model.getValueAt(table.convertRowIndexToModel(row), 0);
    }

    private boolean confirm(String message) {
        return JOptionPane.showConfirmDialog(this, message, "Please confirm",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
    }

    private void showWarning(String message) {
        JOptionPane.showMessageDialog(this, message, "Library desk", JOptionPane.WARNING_MESSAGE);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Something went wrong", JOptionPane.ERROR_MESSAGE);
    }

    private String friendlyMessage(SQLException e) {
        String message = String.valueOf(e.getMessage());
        if (message.contains("FOREIGN KEY")) return "This record has borrowing history and cannot be deleted.";
        if (message.contains("UNIQUE")) return "That email address is already registered.";
        if (message.contains("CHECK")) return "The requested change violates a date rule.";
        return message;
    }

    private JPanel pagePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new javax.swing.BoxLayout(panel, javax.swing.BoxLayout.Y_AXIS));
        panel.setBackground(PAPER);
        panel.setBorder(BorderFactory.createEmptyBorder(24, 4, 8, 4));
        return panel;
    }

    private JPanel tablePanel(JTable table) {
        JScrollPane scroll = new JScrollPane(table);
        styleScroll(scroll);
        scroll.setPreferredSize(new Dimension(850, 440));
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(18, 0, 14, 0));
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private void styleScroll(JScrollPane scroll) {
        scroll.setBorder(BorderFactory.createLineBorder(LINE));
        scroll.getViewport().setBackground(SURFACE);
    }

    private JLabel heading(String text) {
        JLabel label = new JLabel(text);
        label.setFont(DISPLAY);
        label.setForeground(INK);
        label.setAlignmentX(LEFT_ALIGNMENT);
        return label;
    }

    private JLabel sectionTitle(String text) {
        JLabel label = new JLabel(text);
        label.setFont(BODY.deriveFont(Font.BOLD, 15));
        label.setForeground(INK);
        return label;
    }

    private JLabel label(String text, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(BODY);
        label.setForeground(color);
        label.setAlignmentX(LEFT_ALIGNMENT);
        return label;
    }

    private JButton primaryButton(String text) {
        JButton button = new JButton(text);
        button.setFont(BODY.deriveFont(Font.BOLD));
        button.setBackground(PRIMARY_BUTTON_SURFACE);
        button.setForeground(Color.BLACK);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(true);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        return button;
    }

    private void styleSettingsButton(JButton button) {
        button.setFont(BODY.deriveFont(Font.BOLD));
        button.setBackground(BUTTON_SURFACE);
        button.setForeground(Color.BLACK);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(LINE),
                BorderFactory.createEmptyBorder(8, 16, 8, 16)));
    }

    private JButton quietButton(String text) {
        JButton button = new JButton(text);
        button.setFont(BODY.deriveFont(Font.BOLD));
        button.setForeground(BUTTON_TEXT);
        button.setBackground(BUTTON_SURFACE);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(true);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(LINE),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        return button;
    }

    private JButton settingsButton() {
        JButton button = quietButton("Settings");
        if (theme == Theme.DARK) {
            button.setBackground(TAB_ACTIVE);
            button.setForeground(TAB_TEXT);
        } else if (theme == Theme.PINK) {
            button.setBackground(BUTTON_SURFACE);
            button.setForeground(BUTTON_TEXT);
        }
        return button;
    }

    private JPanel formPanel() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(SURFACE);
        form.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        return form;
    }

    private void addFormRow(JPanel form, int row, String caption, java.awt.Component field) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = row;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(7, 5, 7, 16);
        JLabel label = label(caption, MUTED);
        label.setFont(BODY.deriveFont(Font.BOLD, 12));
        form.add(label, c);
        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(7, 0, 7, 5);
        field.setFont(BODY);
        form.add(field, c);
    }

    private static DefaultTableModel model(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
    }

    private static JTable table(DefaultTableModel model) {
        JTable table = new JTable(model);
        styleTable(table);
        return table;
    }

    private static void styleTable(JTable table) {
        table.setFont(BODY);
        table.setForeground(INK);
        table.setBackground(SURFACE);
        table.setRowHeight(34);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setSelectionBackground(SELECTED);
        table.setSelectionForeground(INK);
        table.setShowVerticalLines(false);
        table.setGridColor(LINE);
        table.getTableHeader().setFont(BODY.deriveFont(Font.BOLD, 11));
        table.getTableHeader().setForeground(MUTED);
        table.getTableHeader().setBackground(TABLE_HEADER);
        table.getTableHeader().setPreferredSize(new Dimension(0, 36));
    }

    private void styleTabs() {
        if (tabs == null) return;
        tabs.setBackground(PAPER);
        tabs.setForeground(TAB_TEXT);
        for (int index = 0; index < tabs.getTabCount(); index++) {
            tabs.setBackgroundAt(index, index == tabs.getSelectedIndex() ? TAB_ACTIVE : TAB_IDLE);
            tabs.setForegroundAt(index, TAB_TEXT);
        }
    }

    private void applyLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            UIManager.put("OptionPane.messageFont", BODY);
            UIManager.put("OptionPane.buttonFont", BODY);
        } catch (Exception ignored) {
            // Use Swing's default look and feel if the system theme cannot be loaded.
        }
    }

    private boolean validEmail(String email) {
        int at = email.indexOf('@');
        return at > 0 && email.indexOf('.', at) > at + 1 && !email.contains(" ");
    }

    private static class CategoryChoice {
        private final Integer id;
        private final String name;
        private CategoryChoice(Integer id, String name) { this.id = id; this.name = name; }
        @Override public String toString() { return name; }
    }

    private static class MemberChoice {
        private final Member member;
        private MemberChoice(Member member) { this.member = member; }
        @Override public String toString() { return member.getName() + "  ·  " + member.getEmail(); }
    }

    private static class BookChoice {
        private final Book book;
        private BookChoice(Book book) { this.book = book; }
        @Override public String toString() { return book.getTitle() + "  ·  " + book.getAuthor(); }
    }

    private enum Theme {
        LIGHT("Light"), DARK("Dark"), PINK("Pink"), BLUE("Blue"), GREEN("Green"),
        YELLOW("Yellow"), RED("Red"), ORANGE("Orange");

        private final String label;
        Theme(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }

    @FunctionalInterface
    private interface DatabaseTask {
        void run() throws Exception;
    }
}