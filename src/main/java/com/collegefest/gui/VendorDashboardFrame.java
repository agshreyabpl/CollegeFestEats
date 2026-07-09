package main.java.com.collegefest.gui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.List;

/**
 * DAY 6 — live refresh via SwingWorker/Timer polling (Step 4), backed by
 * SimulatedOrderDataSource (in-memory stand-in for OrderDAO). Shares the
 * same simulated backend as StudentDashboardFrame — mark an order "ready"
 * here and it shows up on the student side on its next poll.
 *
 * New this round:
 *   - Incoming Orders table polls every 5 seconds via OrderPoller.
 *   - Status buttons call dataSource.updateOrderStatus(...) and trigger an
 *     immediate poll instead of editing the table directly.
 *   - Poller stopped on dispose() so no background Timer survives the
 *     window closing.
 *
 * My Menu tab is unaffected by today's changes — it's still in-memory only,
 * tracked separately since it's ItemDAO's concern (Step 6), not OrderDAO's.
 *
 * TODO(Step 4 merge): swap SimulatedOrderDataSource.getInstance() for a
 * real OrderDAO-backed OrderDataSource once that class exists on this
 * branch — see OrderDataSource.java for the intended swap point.
 * TODO(Step 4): row selection should enable/disable the three status
 * buttons based on the selected order's current status.
 */
public class VendorDashboardFrame extends JFrame {

    private final String vendorId;
    private final String vendorName;
    private final OrderDataSource dataSource = SimulatedOrderDataSource.getInstance();

    private DefaultTableModel ordersTableModel;
    private JTable ordersTable;
    private JLabel queueCountLabel;
    private JLabel lastUpdatedLabel;

    private DefaultTableModel menuTableModel;
    private JTable menuTable;

    private OrderPoller<List<OrderRow>> ordersPoller;
    private JButton markPreparingButton;
    private JButton markReadyButton;
    private JButton markServedButton;

    public VendorDashboardFrame(String vendorId, String vendorName) {
        this.vendorId = vendorId;
        this.vendorName = vendorName;

        setTitle("CollegeFest — Vendor Dashboard");
        setSize(980, 640);
        setMinimumSize(new Dimension(800, 480));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setIconImage(AppIcon.generate());

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(AppTheme.BACKGROUND_DARK);
        root.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        setContentPane(root);

        root.add(buildHeaderPanel(), BorderLayout.NORTH);
        root.add(buildTabs(), BorderLayout.CENTER);

        loadDummyMenu();
        startPolling();
    }

    @Override
    public void dispose() {
        if (ordersPoller != null) ordersPoller.stop();
        super.dispose();
    }

    // ---------------------------------------------------------------
    // Live polling
    // ---------------------------------------------------------------
    private void startPolling() {
        // NOTE: using vendorName as the lookup key for now, same as
        // PlaceOrderDialog does — see SimulatedOrderDataSource's TODO
        // about swapping this for a real vendorId once vendor lookup
        // via UserDAO exists.
        ordersPoller = new OrderPoller<>(5000,
                () -> dataSource.fetchIncomingOrdersForVendor(vendorName),
                this::applyOrdersResults);
        ordersPoller.setOnPollStart(() -> {
            lastUpdatedLabel.setText("Refreshing…");
            setStatusButtonsEnabled(false);
        });
        ordersPoller.setOnPollComplete(() -> setStatusButtonsEnabled(true));
        ordersPoller.start();
    }

    private void setStatusButtonsEnabled(boolean enabled) {
        if (markPreparingButton != null) markPreparingButton.setEnabled(enabled);
        if (markReadyButton != null) markReadyButton.setEnabled(enabled);
        if (markServedButton != null) markServedButton.setEnabled(enabled);
    }

    private void applyOrdersResults(List<OrderRow> rows) {
        // Remember which order was selected (by ID, not row index) so a
        // status-button click doesn't lose the selection when pollNow()
        // rebuilds the table right after.
        String selectedOrderId = getSelectedOrderId();

        ordersTableModel.setRowCount(0);
        for (OrderRow row : rows) {
            ordersTableModel.addRow(new Object[]{
                    row.orderId, row.counterpartyName, row.items, row.status, row.placedAt
            });
        }
        queueCountLabel.setText("Queue: " + rows.size());
        lastUpdatedLabel.setText("Last updated: " + new java.text.SimpleDateFormat("h:mm:ss a").format(new java.util.Date()));

        reselectOrder(selectedOrderId);
    }

    private String getSelectedOrderId() {
        int viewRow = ordersTable.getSelectedRow();
        if (viewRow == -1) return null;
        int modelRow = ordersTable.convertRowIndexToModel(viewRow);
        return (String) ordersTableModel.getValueAt(modelRow, 0);
    }

    private void reselectOrder(String orderId) {
        if (orderId == null) return;
        for (int modelRow = 0; modelRow < ordersTableModel.getRowCount(); modelRow++) {
            if (orderId.equals(ordersTableModel.getValueAt(modelRow, 0))) {
                int viewRow = ordersTable.convertRowIndexToView(modelRow);
                if (viewRow != -1) {
                    ordersTable.setRowSelectionInterval(viewRow, viewRow);
                }
                return;
            }
        }
        // Order no longer present (shouldn't normally happen — status changes
        // don't remove rows) — selection just stays cleared, which is fine.
    }

    // ---------------------------------------------------------------
    // Header
    // ---------------------------------------------------------------
    private JPanel buildHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);

        JLabel welcome = AppTheme.heading(vendorName);
        JLabel subId = new JLabel("Vendor ID: " + vendorId);
        subId.setFont(AppTheme.FONT_BODY);
        subId.setForeground(AppTheme.TEXT_SECONDARY);
        subId.setBorder(BorderFactory.createEmptyBorder(4, 2, 0, 0));

        left.add(welcome);
        left.add(subId);

        queueCountLabel = new JLabel("Queue: —");
        queueCountLabel.setFont(AppTheme.FONT_SUBHEADING);
        queueCountLabel.setForeground(AppTheme.ACCENT_AMBER);

        lastUpdatedLabel = new JLabel("Last updated: —");
        lastUpdatedLabel.setFont(AppTheme.FONT_BODY);
        lastUpdatedLabel.setForeground(AppTheme.TEXT_SECONDARY);

        JButton logoutButton = new JButton("Logout");
        AppTheme.styleButton(logoutButton, false);
        logoutButton.addActionListener(e -> dispose()); // TODO(Step 5): clear session

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.setOpaque(false);
        right.add(lastUpdatedLabel);
        right.add(queueCountLabel);
        right.add(logoutButton);

        panel.add(left, BorderLayout.WEST);
        panel.add(right, BorderLayout.EAST);
        return panel;
    }

    // ---------------------------------------------------------------
    // Tabs: Incoming Orders / My Menu
    // ---------------------------------------------------------------
    private JTabbedPane buildTabs() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(AppTheme.FONT_SUBHEADING);
        tabs.setForeground(AppTheme.TEXT_PRIMARY);
        tabs.setBackground(AppTheme.SURFACE_LIGHT);
        tabs.setOpaque(true);

        tabs.addTab("Incoming Orders", buildOrdersTab());
        tabs.addTab("My Menu", buildMenuTab());
        return tabs;
    }

    // ---------------------------------------------------------------
    // Tab 1: incoming orders table + status buttons
    // ---------------------------------------------------------------
    private JPanel buildOrdersTab() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 0));
        wrapper.setBackground(AppTheme.BACKGROUND_DARK);
        wrapper.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));

        wrapper.add(buildOrdersPanel(), BorderLayout.CENTER);
        wrapper.add(buildStatusActionPanel(), BorderLayout.SOUTH);
        return wrapper;
    }

    private JPanel buildOrdersPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setOpaque(false);

        JLabel sectionTitle = new JLabel("Incoming Orders  (live, refreshes every 5s — click a header to sort)");
        sectionTitle.setFont(AppTheme.FONT_SUBHEADING);
        sectionTitle.setForeground(AppTheme.TEXT_PRIMARY);
        panel.add(sectionTitle, BorderLayout.NORTH);

        String[] columns = {"Order ID", "Student", "Items", "Status", "Placed At"};
        ordersTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        ordersTable = new JTable(ordersTableModel);
        AppTheme.styleTable(ordersTable);
        ordersTable.getColumnModel().getColumn(3).setCellRenderer(new StatusBadgeRenderer());
        ordersTable.setRowSorter(new TableRowSorter<>(ordersTableModel));

        JScrollPane scrollPane = new JScrollPane(ordersTable);
        scrollPane.getViewport().setBackground(AppTheme.SURFACE);
        scrollPane.setBorder(BorderFactory.createLineBorder(AppTheme.BORDER));

        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildStatusActionPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 16));
        panel.setOpaque(false);

        JButton markPreparing = new JButton("Mark Preparing");
        JButton markReady = new JButton("Mark Ready");
        JButton markServed = new JButton("Mark Served");
        AppTheme.styleButton(markPreparing, false);
        AppTheme.styleButton(markReady, false);
        AppTheme.styleButton(markServed, true);

        markPreparing.addActionListener(e -> updateSelectedOrderStatus("preparing"));
        markReady.addActionListener(e -> updateSelectedOrderStatus("ready"));
        markServed.addActionListener(e -> updateSelectedOrderStatus("served"));

        markPreparingButton = markPreparing;
        markReadyButton = markReady;
        markServedButton = markServed;

        panel.add(markPreparing);
        panel.add(markReady);
        panel.add(markServed);
        return panel;
    }

    private void updateSelectedOrderStatus(String newStatus) {
        int viewRow = ordersTable.getSelectedRow();
        if (viewRow == -1) {
            JOptionPane.showMessageDialog(this, "Select an order first.", "No selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        // Convert view row to model row since the table is sortable.
        int modelRow = ordersTable.convertRowIndexToModel(viewRow);
        String orderId = (String) ordersTableModel.getValueAt(modelRow, 0);

        dataSource.updateOrderStatus(orderId, newStatus);
        ordersPoller.pollNow();
    }

    // ---------------------------------------------------------------
    // Tab 2: menu management (unchanged from Day 3 — still in-memory,
    // this is ItemDAO's concern for Step 6, not today's OrderDAO work)
    // ---------------------------------------------------------------
    private JPanel buildMenuTab() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 8));
        wrapper.setBackground(AppTheme.BACKGROUND_DARK);
        wrapper.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));

        JLabel sectionTitle = new JLabel("My Menu");
        sectionTitle.setFont(AppTheme.FONT_SUBHEADING);
        sectionTitle.setForeground(AppTheme.TEXT_PRIMARY);
        wrapper.add(sectionTitle, BorderLayout.NORTH);

        String[] columns = {"Item", "Price (\u20B9)", "Prep Time (min)", "Available"};
        menuTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return col == 3;
            }

            @Override
            public Class<?> getColumnClass(int col) {
                return col == 3 ? Boolean.class : String.class;
            }
        };
        menuTable = new JTable(menuTableModel);
        AppTheme.styleTable(menuTable);
        // TODO(Step 6): on checkbox toggle, call ItemDAO.updateAvailability(itemId, available)

        JScrollPane scrollPane = new JScrollPane(menuTable);
        scrollPane.getViewport().setBackground(AppTheme.SURFACE);
        scrollPane.setBorder(BorderFactory.createLineBorder(AppTheme.BORDER));
        wrapper.add(scrollPane, BorderLayout.CENTER);

        wrapper.add(buildMenuActionPanel(), BorderLayout.SOUTH);
        return wrapper;
    }

    private JPanel buildMenuActionPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 16));
        panel.setOpaque(false);

        JButton addMenuItem = new JButton("+ Add Menu Item");
        AppTheme.styleButton(addMenuItem, true);
        addMenuItem.addActionListener(e -> {
            AddMenuItemDialog dialog = new AddMenuItemDialog(this, result -> {
                // TODO(Step 6): call ItemDAO.insertItem(...) here instead of
                // just adding a row directly to the in-memory table.
                menuTableModel.addRow(new Object[]{
                        result.name, result.price, result.prepTimeMinutes, Boolean.TRUE
                });
            });
            dialog.setVisible(true);
        });

        JButton removeItem = new JButton("Remove Selected");
        AppTheme.styleButton(removeItem, false);
        removeItem.addActionListener(e -> {
            int row = menuTable.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Select a menu item first.", "No selection", JOptionPane.WARNING_MESSAGE);
                return;
            }
            // TODO(Step 6): call ItemDAO to delete/deactivate instead of just removing the row.
            menuTableModel.removeRow(row);
        });

        panel.add(addMenuItem);
        panel.add(removeItem);
        return panel;
    }

    private void loadDummyMenu() {
        menuTableModel.addRow(new Object[]{"Veg Puff", 30.0, 4, Boolean.TRUE});
        menuTableModel.addRow(new Object[]{"Samosa Chaat", 45.0, 6, Boolean.TRUE});
        menuTableModel.addRow(new Object[]{"Cold Coffee", 40.0, 3, Boolean.TRUE});
        menuTableModel.addRow(new Object[]{"Masala Dosa", 60.0, 9, Boolean.FALSE});
    }

    // Standalone entry point for reviewing live polling without running the whole app.
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            AppTheme.applyGlobalDefaults();
            VendorDashboardFrame frame = new VendorDashboardFrame("VEN001", "Rajesh Food Stall");
            frame.setVisible(true);
        });
    }
}
