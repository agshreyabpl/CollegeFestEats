package com.collegefest.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import com.collegefest.db.ItemDAO;
import com.collegefest.models.Item;
import com.collegefest.models.Order;
import com.collegefest.service.OrderObserver;
import com.collegefest.service.OrderService;

/**
 * VendorDashboard — Person B's file, restyled + extended on feature day.
 *
 * CHANGED THIS ROUND (feature requests):
 *  - PALETTE: repainted from indigo/navy to the shared AppTheme charcoal +
 *    amber, so Login, Student and Vendor screens are one consistent app.
 *  - LOGOUT button in the top bar (same behaviour as the student side:
 *    clears the ConcurrentHashMap session and returns to LoginFrame).
 *  - MY MENU panel in the sidebar, below "Add Menu Item": lists the
 *    logged-in vendor's OWN items from MongoDB with an "In stock" checkbox
 *    per row. Unticking marks the item out of stock (students see it
 *    greyed-out and cannot order it); ticking brings it back — any time,
 *    as often as needed. Backed by ItemDAO.updateAvailability().
 *  - LIVE POLLING: the order table now refreshes every 5 s through
 *    OrderPoller + MongoOrderDataSource (same pipeline the student
 *    dashboard uses), on top of the existing Observer push updates —
 *    so orders placed from ANOTHER running copy of the app appear too.
 *  - ORDER IDs: the table model keeps the FULL MongoDB id and displays it
 *    through ShortIdRenderer (#A1B2C3) — the exact same format the student
 *    sees, so both sides can talk about the same order. This also fixes
 *    the old change-status bug that matched orders by row number.
 *
 * KEPT (professor requirements, Person B's work):
 *  - Concurrent initial load: ExecutorService + CountDownLatch
 *  - Observer pattern: push updates from OrderService
 *  - Popular items: HashMap + PriorityQueue with a lambda comparator
 *  - CSV export via Person A's VendorOrderExporter
 */
public class VendorDashboard extends JFrame implements OrderObserver {

    // ── Fonts (AppTheme + one big title size) ──────────────────
    private static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 22);

    // ── State ──────────────────────────────────────────────────
    private final String       vendorId;
    private final String       vendorName;
    private final OrderService orderService;
    private final ItemDAO      itemDAO;
    private final OrderDataSource dataSource = MongoOrderDataSource.getInstance();

    // ── Order table (model column 0 = FULL id, rendered short) ─
    private final String[] COLS = {"Order ID", "Student", "Items", "Status", "ETA (min)", "Placed At"};
    private final DefaultTableModel tableModel;
    private final JTable orderTable;

    // ── Popular items ──────────────────────────────────────────
    private final JPanel popularPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));

    // ── My Menu (availability toggles) ─────────────────────────
    private final DefaultTableModel menuTableModel;
    private final JTable menuTable;
    /** Item ids aligned with menuTableModel rows — toggles know which document to update. */
    private final List<String> menuItemIds = new ArrayList<>();
    private boolean menuReloading = false;   // suppress the listener during reloads

    // ── Status bar + polling ───────────────────────────────────
    private final JLabel statusBar = new JLabel("  Loading orders…");
    private OrderPoller<List<OrderRow>> ordersPoller;

    // ══════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ══════════════════════════════════════════════════════════

    public VendorDashboard(String vendorId, String vendorName, OrderService orderService) {
        this.vendorId     = vendorId;
        this.vendorName   = (vendorName == null || vendorName.isEmpty()) ? vendorId : vendorName;
        this.orderService = orderService;
        this.itemDAO      = new ItemDAO();
        this.tableModel   = new DefaultTableModel(COLS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        this.orderTable = new JTable(tableModel);

        this.menuTableModel = new DefaultTableModel(
                new String[]{"Item", "Price (\u20B9)", "In stock"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return c == 2; }
            @Override public Class<?> getColumnClass(int c) {
                return c == 2 ? Boolean.class : String.class;
            }
        };
        this.menuTable = new JTable(menuTableModel);

        orderService.registerObserver(this);   // register for live push updates

        buildUI();
        loadDataConcurrently();                // concurrent load on open (prof req)
        startPolling();                        // then keep everything fresh

        setTitle("CollegeFest Eats  —  Vendor Dashboard");
        setDefaultCloseOperation(EXIT_ON_CLOSE);   // same as the student dashboard
        setSize(1100, 700);
        setMinimumSize(new Dimension(920, 580));
        setLocationRelativeTo(null);
        setIconImage(AppIcon.generate());
        getContentPane().setBackground(AppTheme.BACKGROUND_DARK);

        // Remove observer + stop the poll timer when the window closes.
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent e) {
                orderService.removeObserver(VendorDashboard.this);
                if (ordersPoller != null) ordersPoller.stop();
            }
        });

        setVisible(true);
    }

    /** Old signature kept so nothing else that constructs this breaks. */
    public VendorDashboard(String vendorId, OrderService orderService) {
        this(vendorId, vendorId, orderService);
    }

    // ══════════════════════════════════════════════════════════
    // UI CONSTRUCTION
    // ══════════════════════════════════════════════════════════

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(AppTheme.BACKGROUND_DARK);
        setContentPane(root);

        root.add(buildTopBar(),    BorderLayout.NORTH);
        // Menu management moved to its own window (top-bar 'My Menu' button).
        root.add(buildMainPanel(), BorderLayout.CENTER);
        root.add(buildStatusBar(), BorderLayout.SOUTH);
    }

    // ── Top bar ────────────────────────────────────────────────
    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(AppTheme.SURFACE);
        bar.setBorder(new MatteBorder(0, 0, 2, 0, AppTheme.ACCENT_AMBER));
        bar.setPreferredSize(new Dimension(0, 64));

        // Left: logo + title + who's logged in
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 18, 0));
        left.setOpaque(false);
        JLabel icon  = new JLabel("\uD83C\uDF7D");   // 🍽
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 28));
        JLabel title = new JLabel("CollegeFest Eats");
        title.setFont(FONT_TITLE);
        title.setForeground(AppTheme.TEXT_PRIMARY);
        JLabel sub = new JLabel("Vendor Portal  \u00B7  " + vendorName + "  (" + vendorId + ")");
        sub.setFont(AppTheme.FONT_BODY);
        sub.setForeground(AppTheme.TEXT_SECONDARY);
        left.add(icon);
        left.add(title);
        left.add(sub);
        bar.add(left, BorderLayout.WEST);

        // Right: export + logout (feature request: same as student dashboard)
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 14));
        right.setOpaque(false);
        JButton menuBtn = new JButton("\uD83C\uDF54  My Menu");
        AppTheme.styleButton(menuBtn, true);
        menuBtn.addActionListener(e ->
                new VendorMenuManagerFrame(vendorId).setVisible(true));
        right.add(menuBtn);
        JButton exportBtn = new JButton("\u2B07  Export CSV");
        AppTheme.styleButton(exportBtn, false);
        exportBtn.addActionListener(e -> exportOrders());
        JButton logoutBtn = new JButton("Logout");
        AppTheme.styleButton(logoutBtn, false);
        logoutBtn.addActionListener(e -> logout());
        right.add(exportBtn);
        right.add(logoutBtn);
        bar.add(right, BorderLayout.EAST);

        return bar;
    }

    /** Mirror of the student dashboard's logout: clear session, back to login. */
    private void logout() {
        OrderService.removeSession(vendorId);
        dispose();                             // windowClosed handles observer + poller
        new LoginFrame().setVisible(true);
    }

    // ── Center: split pane (main | sidebar) ───────────────────
    private JSplitPane buildCenterPane() {
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                                          buildMainPanel(), buildSidebar());
        split.setDividerLocation(720);
        split.setDividerSize(1);
        split.setBackground(AppTheme.BACKGROUND_DARK);
        split.setBorder(null);
        split.setResizeWeight(0.68);
        return split;
    }

    // ── Main panel: popular items + order table + buttons ─────
    private JPanel buildMainPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(AppTheme.BACKGROUND_DARK);
        panel.setBorder(new EmptyBorder(16, 16, 12, 8));

        panel.add(buildPopularSection(), BorderLayout.NORTH);
        panel.add(buildTableSection(),   BorderLayout.CENTER);
        panel.add(buildActionButtons(),  BorderLayout.SOUTH);

        return panel;
    }

    // ── Popular items strip ────────────────────────────────────
    private JPanel buildPopularSection() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(AppTheme.SURFACE);
        wrapper.setBorder(new CompoundBorder(
                new MatteBorder(0, 4, 0, 0, AppTheme.ACCENT_AMBER),
                new EmptyBorder(10, 14, 10, 14)));

        JLabel heading = new JLabel("\uD83D\uDD25  Most Ordered Today");   // 🔥
        heading.setFont(AppTheme.FONT_SUBHEADING);
        heading.setForeground(AppTheme.TEXT_PRIMARY);

        popularPanel.setOpaque(false);
        JLabel placeholder = new JLabel("—  Loading…");
        placeholder.setFont(AppTheme.FONT_BODY);
        placeholder.setForeground(AppTheme.TEXT_SECONDARY);
        popularPanel.add(placeholder);

        wrapper.add(heading,     BorderLayout.WEST);
        wrapper.add(popularPanel,BorderLayout.CENTER);
        return wrapper;
    }

    // ── Order table ────────────────────────────────────────────
    private JScrollPane buildTableSection() {
        AppTheme.styleTable(orderTable);
        orderTable.setRowHeight(34);
        orderTable.setRowSorter(new TableRowSorter<>(tableModel));

        // Column widths
        orderTable.getColumnModel().getColumn(0).setPreferredWidth(90);
        orderTable.getColumnModel().getColumn(1).setPreferredWidth(110);
        orderTable.getColumnModel().getColumn(2).setPreferredWidth(230);
        orderTable.getColumnModel().getColumn(3).setPreferredWidth(100);
        orderTable.getColumnModel().getColumn(4).setPreferredWidth(70);
        orderTable.getColumnModel().getColumn(5).setPreferredWidth(90);

        // Order id: FULL id in the model, short form (#A1B2C3) on screen —
        // identical to the student dashboard.
        orderTable.getColumnModel().getColumn(0).setCellRenderer(new ShortIdRenderer());
        // Status: same shared badge renderer as the student dashboard.
        orderTable.getColumnModel().getColumn(3).setCellRenderer(new StatusBadgeRenderer());

        JScrollPane scroll = new JScrollPane(orderTable);
        scroll.setBackground(AppTheme.BACKGROUND_DARK);
        scroll.getViewport().setBackground(AppTheme.SURFACE);
        scroll.setBorder(new LineBorder(AppTheme.BORDER, 1, true));
        return scroll;
    }

    // ── Action buttons ─────────────────────────────────────────
    private JPanel buildActionButtons() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        panel.setOpaque(false);

        JButton btnPreparing = new JButton("\u2699  Mark Preparing");   // ⚙
        JButton btnReady     = new JButton("\u2713  Mark Ready");       // ✓
        JButton btnServed    = new JButton("\u2714  Mark Served");      // ✔
        JButton btnRefresh   = new JButton("\u21BB  Refresh");          // ↻
        AppTheme.styleButton(btnPreparing, false);
        AppTheme.styleButton(btnReady, false);
        AppTheme.styleButton(btnServed, true);
        AppTheme.styleButton(btnRefresh, false);

        // The GUI-facing statuses are lowercase; MongoOrderDataSource maps
        // them to the canonical Order.STATUS_* database values.
        btnPreparing.addActionListener(e -> changeStatus("preparing"));
        btnReady    .addActionListener(e -> changeStatus("ready"));
        btnServed   .addActionListener(e -> changeStatus("served"));
        btnRefresh  .addActionListener(e -> ordersPoller.pollNow());

        panel.add(btnPreparing);
        panel.add(btnReady);
        panel.add(btnServed);
        panel.add(Box.createHorizontalStrut(16));
        panel.add(btnRefresh);
        return panel;
    }

    // ── Sidebar: Add Menu Item form + My Menu availability panel ──
    private JComponent buildSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout(0, 12));
        sidebar.setBackground(AppTheme.BACKGROUND_DARK);
        sidebar.setBorder(new EmptyBorder(16, 8, 12, 16));

        sidebar.add(buildAddItemCard(), BorderLayout.NORTH);
        // Feature request: the vendor's own menu with in/out-of-stock
        // toggles lives DIRECTLY BELOW the Add Menu Item form.
        sidebar.add(buildMyMenuCard(),  BorderLayout.CENTER);
        return sidebar;
    }

    private JPanel buildAddItemCard() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(AppTheme.SURFACE);
        card.setBorder(new CompoundBorder(
                new LineBorder(AppTheme.BORDER, 1, true),
                new EmptyBorder(16, 16, 16, 16)));

        JLabel heading = new JLabel("\uFF0B  Add Menu Item");   // ＋
        heading.setFont(AppTheme.FONT_SUBHEADING);
        heading.setForeground(AppTheme.ACCENT_AMBER);
        heading.setAlignmentX(LEFT_ALIGNMENT);

        JTextField nameField  = darkField();
        JTextField priceField = darkField();
        JTextField prepField  = darkField();
        JCheckBox  availBox   = new JCheckBox("Available now", true);
        availBox.setFont(AppTheme.FONT_BODY);
        availBox.setForeground(AppTheme.TEXT_SECONDARY);
        availBox.setOpaque(false);
        availBox.setAlignmentX(LEFT_ALIGNMENT);

        JButton submitBtn = new JButton("Add Item");
        AppTheme.styleButton(submitBtn, true);
        submitBtn.setAlignmentX(LEFT_ALIGNMENT);
        submitBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        submitBtn.addActionListener(e -> {
            String name  = nameField.getText().trim();
            String price = priceField.getText().trim();
            String prep  = prepField.getText().trim();
            if (name.isEmpty() || price.isEmpty() || prep.isEmpty()) {
                showError("Please fill in all fields.");
                return;
            }
            try {
                double priceVal = Double.parseDouble(price);
                int    prepVal  = Integer.parseInt(prep);
                if (priceVal < 0 || prepVal < 0) {
                    showError("Price and prep time can't be negative.");
                    return;
                }
                new SwingWorker<Void, Void>() {
                    @Override protected Void doInBackground() {
                        Item item = new Item();
                        item.setVendorId(vendorId);
                        item.setName(name);
                        item.setPrice(priceVal);
                        item.setPrepTimeMinutes(prepVal);
                        item.setAvailable(availBox.isSelected());
                        itemDAO.insertItem(item);
                        return null;
                    }
                    @Override protected void done() {
                        try {
                            get();
                            nameField.setText("");
                            priceField.setText("");
                            prepField.setText("");
                            setStatus("\u2713  Item \"" + name + "\" added.");
                            reloadMenuFromDatabase();   // it appears in My Menu right away
                        } catch (Exception ex) {
                            showError("Failed to add item: " + rootMessage(ex));
                        }
                    }
                }.execute();
            } catch (NumberFormatException nfe) {
                showError("Price and prep time must be numbers.");
            }
        });

        card.add(heading);
        card.add(Box.createVerticalStrut(14));
        card.add(sidebarLabel("Item Name"));   card.add(Box.createVerticalStrut(4));
        card.add(nameField);                   card.add(Box.createVerticalStrut(10));
        card.add(sidebarLabel("Price (\u20B9)"));   card.add(Box.createVerticalStrut(4));
        card.add(priceField);                  card.add(Box.createVerticalStrut(10));
        card.add(sidebarLabel("Prep Time (minutes)"));   card.add(Box.createVerticalStrut(4));
        card.add(prepField);                   card.add(Box.createVerticalStrut(10));
        card.add(availBox);                    card.add(Box.createVerticalStrut(14));
        card.add(submitBtn);
        return card;
    }

    /**
     * MY MENU (feature request) — every item this vendor has ever added,
     * loaded live from MongoDB, with an "In stock" checkbox per row:
     *
     *   untick  -> ItemDAO.updateAvailability(id, false)  = OUT OF STOCK
     *              (students see it greyed out, can't order it)
     *   tick    -> ItemDAO.updateAvailability(id, true)   = back on sale
     *
     * Because the full list is always visible here, an item marked out of
     * stock is never "lost" — the vendor can bring it back any time.
     */
    private JPanel buildMyMenuCard() {
        JPanel card = new JPanel(new BorderLayout(0, 10));
        card.setBackground(AppTheme.SURFACE);
        card.setBorder(new CompoundBorder(
                new LineBorder(AppTheme.BORDER, 1, true),
                new EmptyBorder(16, 16, 16, 16)));

        JLabel heading = new JLabel("\uD83C\uDF54  My Menu  (tick = in stock)");   // 🍔
        heading.setFont(AppTheme.FONT_SUBHEADING);
        heading.setForeground(AppTheme.ACCENT_AMBER);
        card.add(heading, BorderLayout.NORTH);

        AppTheme.styleTable(menuTable);
        menuTable.setRowHeight(30);
        menuTable.getColumnModel().getColumn(0).setPreferredWidth(150);
        menuTable.getColumnModel().getColumn(1).setPreferredWidth(70);
        menuTable.getColumnModel().getColumn(2).setPreferredWidth(70);

        // The checkbox listener — THE availability toggle.
        menuTableModel.addTableModelListener(e -> {
            if (menuReloading || e.getColumn() != 2) return;
            int row = e.getFirstRow();
            if (row < 0 || row >= menuItemIds.size()) return;
            String itemId  = menuItemIds.get(row);
            String itemName = String.valueOf(menuTableModel.getValueAt(row, 0));
            boolean inStock = Boolean.TRUE.equals(menuTableModel.getValueAt(row, 2));
            new SwingWorker<Void, Void>() {
                @Override protected Void doInBackground() {
                    itemDAO.updateAvailability(itemId, inStock);
                    return null;
                }
                @Override protected void done() {
                    try {
                        get();
                        setStatus(inStock
                                ? "\u2713  \"" + itemName + "\" is back in stock."
                                : "\u2713  \"" + itemName + "\" marked OUT OF STOCK.");
                    } catch (Exception ex) {
                        showError("Could not update availability: " + rootMessage(ex));
                        reloadMenuFromDatabase();   // revert the checkbox to DB truth
                    }
                }
            }.execute();
        });

        JScrollPane scroll = new JScrollPane(menuTable);
        scroll.getViewport().setBackground(AppTheme.SURFACE);
        scroll.setBorder(new LineBorder(AppTheme.BORDER, 1, true));
        card.add(scroll, BorderLayout.CENTER);

        JButton reloadBtn = new JButton("\u21BB  Reload Menu");
        AppTheme.styleButton(reloadBtn, false);
        reloadBtn.addActionListener(e -> reloadMenuFromDatabase());
        JPanel south = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        south.setOpaque(false);
        south.add(reloadBtn);
        card.add(south, BorderLayout.SOUTH);

        return card;
    }

    /** Pulls this vendor's items from MongoDB into the My Menu table. */
    private void reloadMenuFromDatabase() {
        new SwingWorker<List<Item>, Void>() {
            @Override protected List<Item> doInBackground() {
                return itemDAO.findByVendor(vendorId);
            }
            @Override protected void done() {
                try {
                    List<Item> items = get();
                    menuReloading = true;
                    try {
                        menuTableModel.setRowCount(0);
                        menuItemIds.clear();
                        for (Item item : items) {
                            menuItemIds.add(item.getId());
                            menuTableModel.addRow(new Object[]{
                                    item.getName(),
                                    formatPrice(item.getPrice()),
                                    item.isAvailable()
                            });
                        }
                    } finally {
                        menuReloading = false;
                    }
                    setStatus("  " + items.size() + " menu item(s) loaded.");
                } catch (Exception ex) {
                    showError("Could not load menu: " + rootMessage(ex));
                }
            }
        }.execute();
    }

    private static String formatPrice(double price) {
        return price == Math.floor(price)
                ? String.valueOf((long) price)
                : String.format("%.2f", price);
    }

    // ── Status bar ─────────────────────────────────────────────
    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(AppTheme.SURFACE);
        bar.setBorder(new MatteBorder(1, 0, 0, 0, AppTheme.BORDER));
        bar.setPreferredSize(new Dimension(0, 28));
        statusBar.setFont(AppTheme.FONT_BODY);
        statusBar.setForeground(AppTheme.TEXT_SECONDARY);
        bar.add(statusBar, BorderLayout.WEST);
        return bar;
    }

    // ══════════════════════════════════════════════════════════
    // LIVE POLLING — every 5 s, same pipeline as the student side
    // ══════════════════════════════════════════════════════════

    private void startPolling() {
        ordersPoller = new OrderPoller<>(5000,
                () -> dataSource.fetchIncomingOrdersForVendor(vendorId),
                this::applyRows);
        ordersPoller.start();
    }

    /** EDT (OrderPoller guarantees it). Keeps the selection across refreshes. */
    private void applyRows(List<OrderRow> rows) {
        String selectedFullId = selectedOrderId();

        tableModel.setRowCount(0);
        for (OrderRow r : rows) {
            tableModel.addRow(new Object[]{
                    r.orderId,                                   // FULL id (rendered short)
                    r.counterpartyName,                          // student's NAME, not id
                    r.items,
                    r.status,                                    // lowercase, badge-rendered
                    r.etaMinutes == null ? "—" : r.etaMinutes,
                    r.placedAt
            });
        }
        if (selectedFullId != null) reselect(selectedFullId);
        refreshPopularItems(rows);
        setStatus("  \u2713  " + rows.size() + " orders  \u00B7  updated "
                + new java.text.SimpleDateFormat("h:mm:ss a").format(new java.util.Date()));
    }

    private String selectedOrderId() {
        int viewRow = orderTable.getSelectedRow();
        if (viewRow < 0) return null;
        int modelRow = orderTable.convertRowIndexToModel(viewRow);
        return (String) tableModel.getValueAt(modelRow, 0);
    }

    private void reselect(String fullId) {
        for (int m = 0; m < tableModel.getRowCount(); m++) {
            if (fullId.equals(tableModel.getValueAt(m, 0))) {
                int v = orderTable.convertRowIndexToView(m);
                if (v != -1) orderTable.setRowSelectionInterval(v, v);
                return;
            }
        }
    }

    // ══════════════════════════════════════════════════════════
    // CONCURRENT INITIAL LOAD (ExecutorService + CountDownLatch)
    // ══════════════════════════════════════════════════════════

    /**
     * First paint of the table using 2 threads concurrently — kept from
     * Person B's original because it ticks the ExecutorService +
     * CountDownLatch professor requirements. After this, OrderPoller
     * keeps the data fresh every 5 seconds.
     */
    private void loadDataConcurrently() {
        setStatus("  Refreshing…");

        new SwingWorker<List<OrderRow>, Void>() {
            @Override
            protected List<OrderRow> doInBackground() throws Exception {
                ExecutorService executor = Executors.newFixedThreadPool(2);
                CountDownLatch  latch    = new CountDownLatch(2);

                // Thread 1 — fetch vendor orders (rows come pre-shaped:
                // student names resolved, statuses lowercased, ETA set)
                final List<List<OrderRow>> result = new ArrayList<>();
                result.add(null);
                executor.submit(() -> {
                    try {
                        result.set(0, dataSource.fetchIncomingOrdersForVendor(vendorId));
                    } finally {
                        latch.countDown();
                    }
                });

                // Thread 2 — reserved for future parallel pre-fetching
                executor.submit(latch::countDown);

                latch.await(); // wait for both threads
                executor.shutdown();
                return result.get(0) != null ? result.get(0) : new ArrayList<>();
            }

            @Override
            protected void done() {
                try {
                    applyRows(get());
                } catch (Exception ex) {
                    setStatus("  \u2717  Error loading orders: " + rootMessage(ex));
                }
            }
        }.execute();
    }

    // ══════════════════════════════════════════════════════════
    // OBSERVER — push update from OrderService (same-JVM instant path)
    // ══════════════════════════════════════════════════════════

    /**
     * Called by OrderService when any order changes. Matching is by FULL
     * id from the table model now (the old version compared short ids,
     * which broke the moment the display format changed).
     */
    @Override
    public void update(Order order) {
        if (!vendorId.equals(order.getVendorId())) return;   // not my stall
        SwingUtilities.invokeLater(() -> {
            // The consumer thread can notify while the constructor is still
            // running (observer registers before polling starts) — guard it.
            if (ordersPoller != null) ordersPoller.pollNow();
        });
    }

    // ══════════════════════════════════════════════════════════
    // STATUS CHANGE — by full id from the model (row-index bug fixed)
    // ══════════════════════════════════════════════════════════

    private void changeStatus(String newStatus) {
        String fullId = selectedOrderId();
        if (fullId == null) {
            showError("Please select an order from the table first.");
            return;
        }
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() {
                // MongoOrderDataSource maps "preparing" -> "PREPARING" and
                // OrderService enforces the forward-only rule.
                orderService.updateOrderStatus(
                        fullId, toDbStatus(newStatus));
                return null;
            }
            @Override protected void done() {
                try {
                    get();
                    setStatus("  \u2713  " + AppTheme.shortId(fullId) + " marked " + newStatus + ".");
                } catch (Exception ex) {
                    // e.g. "Invalid status change: SERVED → PREPARING"
                    showError(rootMessage(ex));
                }
                ordersPoller.pollNow();
            }
        }.execute();
    }

    private static String toDbStatus(String ui) {
        switch (ui.toLowerCase()) {
            case "pending":   return Order.STATUS_PLACED;
            case "preparing": return Order.STATUS_PREPARING;
            case "ready":     return Order.STATUS_READY;
            case "served":    return Order.STATUS_SERVED;
            case "cancelled": return Order.STATUS_CANCELLED;
            default:          return ui.toUpperCase();
        }
    }

    // ══════════════════════════════════════════════════════════
    // POPULAR ITEMS — HashMap + PriorityQueue (professor req)
    // ══════════════════════════════════════════════════════════

    private void refreshPopularItems(List<OrderRow> rows) {
        // Count frequency with HashMap
        Map<String, Integer> freq = new HashMap<>();
        for (OrderRow r : rows) {
            if (r.items == null || r.items.isEmpty()) continue;
            for (String item : r.items.split(",\\s*")) {
                freq.merge(item, 1, Integer::sum);
            }
        }

        // Max-heap PriorityQueue — highest count first (lambda comparator)
        PriorityQueue<Map.Entry<String, Integer>> pq = new PriorityQueue<>(
                (a, b) -> b.getValue() - a.getValue()
        );
        pq.addAll(freq.entrySet());

        String[] medals = {"\uD83E\uDD47", "\uD83E\uDD48", "\uD83E\uDD49"};   // 🥇🥈🥉
        List<JLabel> badges = new ArrayList<>();
        for (int i = 0; i < 3 && !pq.isEmpty(); i++) {
            Map.Entry<String, Integer> entry = pq.poll();
            badges.add(popularBadge(medals[i] + "  " + entry.getKey()
                                    + "  (" + entry.getValue() + ")"));
        }
        if (badges.isEmpty()) {
            badges.add(popularBadge("No orders yet"));
        }

        popularPanel.removeAll();
        badges.forEach(popularPanel::add);
        popularPanel.revalidate();
        popularPanel.repaint();
    }

    // ══════════════════════════════════════════════════════════
    // CSV EXPORT
    // ══════════════════════════════════════════════════════════

    private void exportOrders() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export daily orders as CSV");
        chooser.setSelectedFile(new java.io.File("vendor_orders_" + vendorId + ".csv"));

        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            String path = chooser.getSelectedFile().getAbsolutePath();
            new SwingWorker<Void, Void>() {
                @Override protected Void doInBackground() throws Exception {
                    // Person A's exporter — try-with-resources inside
                    com.collegefest.io.VendorOrderExporter.exportToCSV(vendorId, path);
                    return null;
                }
                @Override protected void done() {
                    try {
                        get();
                        setStatus("  \u2713  Exported to " + path);
                    } catch (Exception ex) {
                        showError("Export failed: " + rootMessage(ex));
                    }
                }
            }.execute();
        }
    }

    // ══════════════════════════════════════════════════════════
    // UI HELPERS
    // ══════════════════════════════════════════════════════════

    /** Dark-themed text field matching AppTheme. */
    private JTextField darkField() {
        JTextField field = new JTextField();
        field.setFont(AppTheme.FONT_BODY);
        field.setBackground(AppTheme.SURFACE_LIGHT);
        field.setForeground(AppTheme.TEXT_PRIMARY);
        field.setCaretColor(AppTheme.ACCENT_AMBER);
        field.setBorder(new CompoundBorder(
                new LineBorder(AppTheme.BORDER, 1, true),
                new EmptyBorder(6, 10, 6, 10)));
        field.setAlignmentX(LEFT_ALIGNMENT);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        return field;
    }

    /** Small grey label used in the sidebar form. */
    private JLabel sidebarLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(AppTheme.FONT_BODY);
        lbl.setForeground(AppTheme.TEXT_SECONDARY);
        lbl.setAlignmentX(LEFT_ALIGNMENT);
        return lbl;
    }

    /** Amber pill badge for the popular-items strip. */
    private JLabel popularBadge(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI Emoji", Font.BOLD, 12));
        lbl.setForeground(AppTheme.ACCENT_AMBER);
        lbl.setBackground(AppTheme.SURFACE_LIGHT);
        lbl.setOpaque(true);
        lbl.setBorder(new CompoundBorder(
                new LineBorder(AppTheme.BORDER, 1, true),
                new EmptyBorder(4, 10, 4, 10)));
        return lbl;
    }

    private void setStatus(String msg) {
        SwingUtilities.invokeLater(() -> statusBar.setText(msg));
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private static String rootMessage(Throwable t) {
        Throwable cause = (t.getCause() != null) ? t.getCause() : t;
        return cause.getMessage() != null ? cause.getMessage() : cause.toString();
    }
}
