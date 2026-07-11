package com.collegefest.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.File;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import com.collegefest.db.OrderDAO;
import com.collegefest.io.StudentOrderExporter;
import com.collegefest.service.OrderService;

/**
 * Student Dashboard — LIVE on MongoDB.
 *
 * CHANGED THIS ROUND (feature requests):
 *  - LAYOUT now mirrors the vendor dashboard: same top bar (logo + title +
 *    "Student Portal · name (id)" + Logout), same bottom status bar, same
 *    table styling — one consistent app in the shared charcoal + amber
 *    AppTheme palette.
 *  - The first tab is renamed "Home" (was "Active Orders").
 *  - ORDER IDs: the table model keeps the FULL MongoDB id and displays it
 *    through ShortIdRenderer (#A1B2C3) — the exact same format the vendor
 *    sees, so a student can read their order id to the stall and both are
 *    looking at the same thing. The ETA banner uses the same short form.
 *
 * KEPT from the integration pass:
 *  - OrderPoller (Timer + SwingWorker) polls both tabs every 5 seconds.
 *  - "+ Place New Order" -> PlaceOrderDialog -> dataSource.placeOrder()
 *    on a SwingWorker; rush-aware ETA from EtaEngine via OrderService.
 *  - "Cancel Order" (enabled only while the selected row is "pending").
 *  - "Export History (CSV)" via Person A's StudentOrderExporter.
 */
public class StudentDashboardFrame extends JFrame {

    private static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 22);

    private final String studentId;
    private final String studentName;

    private final MongoOrderDataSource dataSource = MongoOrderDataSource.getInstance();

    private DefaultTableModel activeTableModel;
    private JTable activeTable;
    private JLabel activeOrdersLabel;
    private JLabel nextEtaLabel;
    private JButton cancelButton;

    private DefaultTableModel historyTableModel;
    private JTable historyTable;

    private final JLabel statusBar = new JLabel("  Loading your orders…");

    /** Live polling (OrderPoller: Timer + SwingWorker inside). */
    private JTabbedPane tabs;
    private final JPanel vendorListPanel = new JPanel();
    private final JPanel popularBarPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));

    private OrderPoller<List<OrderRow>> activePoller;
    private OrderPoller<List<OrderRow>> historyPoller;

    public StudentDashboardFrame(String studentId, String studentName) {
        this.studentId = studentId;
        this.studentName = studentName;

        setTitle("CollegeFest Eats — Student Dashboard");
        setSize(1100, 700);
        setMinimumSize(new Dimension(920, 580));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setIconImage(AppIcon.generate());

        // Same skeleton as the vendor dashboard: top bar / content / status bar.
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(AppTheme.BACKGROUND_DARK);
        setContentPane(root);

        root.add(buildTopBar(), BorderLayout.NORTH);
        root.add(buildContent(), BorderLayout.CENTER);
        root.add(buildStatusBar(), BorderLayout.SOUTH);

        startPolling();
    }

    // ---------------------------------------------------------------
    // Top bar — mirror of the vendor dashboard's
    // ---------------------------------------------------------------
    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(AppTheme.SURFACE);
        bar.setBorder(new MatteBorder(0, 0, 2, 0, AppTheme.ACCENT_AMBER));
        bar.setPreferredSize(new Dimension(0, 64));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 18, 0));
        left.setOpaque(false);
        JLabel icon = new JLabel("\uD83C\uDF7D");   // 🍽 — same logo as the vendor side
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 28));
        JLabel title = new JLabel("CollegeFest Eats");
        title.setFont(FONT_TITLE);
        title.setForeground(AppTheme.TEXT_PRIMARY);
        JLabel sub = new JLabel("Student Portal  \u00B7  " + studentName + "  (" + studentId + ")");
        sub.setFont(AppTheme.FONT_BODY);
        sub.setForeground(AppTheme.TEXT_SECONDARY);
        left.add(icon);
        left.add(title);
        left.add(sub);
        bar.add(left, BorderLayout.WEST);

        activeOrdersLabel = new JLabel("Active orders: —");
        activeOrdersLabel.setFont(AppTheme.FONT_SUBHEADING);
        activeOrdersLabel.setForeground(AppTheme.ACCENT_AMBER);

        JButton logoutButton = new JButton("Logout");
        AppTheme.styleButton(logoutButton, false);
        logoutButton.addActionListener(e -> {
            OrderService.removeSession(studentId);   // clear the session map
            dispose();
            new LoginFrame().setVisible(true);
        });

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 14));
        right.setOpaque(false);
        right.add(activeOrdersLabel);
        right.add(logoutButton);
        bar.add(right, BorderLayout.EAST);

        return bar;
    }

    // ---------------------------------------------------------------
    // Content: tabs — "Home" (renamed from "Active Orders") + history
    // ---------------------------------------------------------------
    private JPanel buildContent() {
        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(AppTheme.BACKGROUND_DARK);
        content.setBorder(new EmptyBorder(16, 16, 12, 16));

        tabs = new JTabbedPane();
        tabs.setFont(AppTheme.FONT_SUBHEADING);
        tabs.setOpaque(true);

        tabs.addTab("Vendors", buildVendorsTab());        // the new home screen
        tabs.addTab("Active Orders", buildHomeTab());
        tabs.addTab("Order History", buildHistoryTab());
        AppTheme.styleTabs(tabs);   // readable colours on the dark theme
        content.add(tabs, BorderLayout.CENTER);
        return content;
    }

    // ---------------------------------------------------------------
    // Status bar — mirror of the vendor dashboard's
    // ---------------------------------------------------------------
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

    private void setStatus(String msg) {
        SwingUtilities.invokeLater(() -> statusBar.setText(msg));
    }

    // ---------------------------------------------------------------
    // LIVE DATA - polls MongoDB every 5 s, applies results on the EDT
    // ---------------------------------------------------------------
    private void startPolling() {
        activePoller = new OrderPoller<>(5000,
                () -> dataSource.fetchActiveOrdersForStudent(studentId),
                this::applyActiveRows);
        historyPoller = new OrderPoller<>(5000,
                () -> dataSource.fetchHistoryForStudent(studentId),
                this::applyHistoryRows);
        activePoller.start();
        historyPoller.start();

        // Stop the timers when the window goes away.
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosed(java.awt.event.WindowEvent e) {
                activePoller.stop();
                historyPoller.stop();
            }
        });
    }

    /** Runs on the EDT (OrderPoller guarantees it). Preserves selection. */
    private void applyActiveRows(List<OrderRow> rows) {
        String selectedId = selectedOrderId();
        activeTableModel.setRowCount(0);
        for (OrderRow r : rows) {
            activeTableModel.addRow(new Object[]{
                    r.orderId,                                  // FULL id (rendered short)
                    r.counterpartyName, r.items, r.status,
                    r.etaMinutes == null ? "—" : r.etaMinutes, r.placedAt});
        }
        if (selectedId != null) reselect(selectedId);
        activeOrdersLabel.setText("Active orders: " + rows.size());
        refreshNextEtaBanner();
        updateCancelEnabled();
        setStatus("  \u2713  updated "
                + new java.text.SimpleDateFormat("h:mm:ss a").format(new java.util.Date()));
    }

    private void applyHistoryRows(List<OrderRow> rows) {
        historyTableModel.setRowCount(0);
        for (OrderRow r : rows) {
            historyTableModel.addRow(new Object[]{
                    r.orderId, r.counterpartyName, r.items, r.status, r.placedAt});
        }
    }

    private String selectedOrderId() {
        int viewRow = activeTable.getSelectedRow();
        if (viewRow < 0) return null;
        int modelRow = activeTable.convertRowIndexToModel(viewRow);
        return (String) activeTableModel.getValueAt(modelRow, 0);
    }

    private void reselect(String orderId) {
        for (int m = 0; m < activeTableModel.getRowCount(); m++) {
            if (orderId.equals(activeTableModel.getValueAt(m, 0))) {
                int v = activeTable.convertRowIndexToView(m);
                if (v != -1) activeTable.setRowSelectionInterval(v, v);
                return;
            }
        }
    }

    // ---------------------------------------------------------------
    // Tab 1: HOME — ETA banner + active orders table + actions
    // ---------------------------------------------------------------
    /**
     * THE NEW HOME SCREEN (feature request): a "Popular Stalls" bar on top
     * (vendors ranked by how many orders they have received - HashMap +
     * PriorityQueue inside the adapter), then every vendor as a themed row
     * with a "View Menu" button that opens that stall's own VendorMenuFrame
     * (per-item live ETAs, +/- steppers, total, place order).
     */
    private JPanel buildVendorsTab() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 12));
        wrapper.setBackground(AppTheme.BACKGROUND_DARK);
        wrapper.setBorder(new EmptyBorder(16, 0, 0, 0));

        JPanel popular = new JPanel(new BorderLayout());
        popular.setBackground(AppTheme.SURFACE);
        popular.setBorder(new CompoundBorder(
                new MatteBorder(0, 4, 0, 0, AppTheme.ACCENT_AMBER),
                new EmptyBorder(6, 12, 6, 12)));
        JLabel popTitle = new JLabel("\uD83D\uDD25 Popular Stalls:");
        popTitle.setFont(AppTheme.FONT_SUBHEADING);
        popTitle.setForeground(AppTheme.ACCENT_AMBER);
        popular.add(popTitle, BorderLayout.WEST);
        popularBarPanel.setOpaque(false);
        popular.add(popularBarPanel, BorderLayout.CENTER);
        wrapper.add(popular, BorderLayout.NORTH);

        vendorListPanel.setLayout(new BoxLayout(vendorListPanel, BoxLayout.Y_AXIS));
        vendorListPanel.setBackground(AppTheme.BACKGROUND_DARK);
        JScrollPane scroll = new JScrollPane(vendorListPanel,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createLineBorder(AppTheme.BORDER));
        scroll.getViewport().setBackground(AppTheme.BACKGROUND_DARK);
        wrapper.add(scroll, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 12));
        actions.setOpaque(false);
        JButton refreshBtn = new JButton("\u21BB  Refresh stalls");
        AppTheme.styleButton(refreshBtn, false);
        refreshBtn.addActionListener(e -> loadVendorsAndPopular());
        actions.add(refreshBtn);
        wrapper.add(actions, BorderLayout.SOUTH);

        loadVendorsAndPopular();
        return wrapper;
    }

    /** Vendors + popularity in ONE background trip, applied on the EDT. */
    private void loadVendorsAndPopular() {
        new SwingWorker<Object[], Void>() {
            @Override protected Object[] doInBackground() {
                return new Object[]{
                        dataSource.freshVendorNames(),
                        dataSource.popularStallNames(3)};
            }
            @Override protected void done() {
                try {
                    Object[] r = get();
                    String[] vendors = (String[]) r[0];
                    @SuppressWarnings("unchecked")
                    List<String> popular = (List<String>) r[1];

                    popularBarPanel.removeAll();
                    if (popular.isEmpty()) {
                        JLabel none = new JLabel("no orders yet - be the first!");
                        none.setFont(AppTheme.FONT_BODY);
                        none.setForeground(AppTheme.TEXT_SECONDARY);
                        popularBarPanel.add(none);
                    }
                    String[] medals = {"\uD83E\uDD47 ", "\uD83E\uDD48 ", "\uD83E\uDD49 "};
                    for (int i = 0; i < popular.size(); i++) {
                        JLabel badge = new JLabel(medals[i] + popular.get(i));
                        badge.setFont(AppTheme.FONT_SUBHEADING);
                        badge.setForeground(AppTheme.TEXT_PRIMARY);
                        badge.setOpaque(true);
                        badge.setBackground(AppTheme.SURFACE_LIGHT);
                        badge.setBorder(new CompoundBorder(
                                BorderFactory.createLineBorder(AppTheme.BORDER),
                                new EmptyBorder(4, 10, 4, 10)));
                        popularBarPanel.add(badge);
                    }

                    vendorListPanel.removeAll();
                    vendorListPanel.add(Box.createVerticalStrut(10));
                    if (vendors.length == 0) {
                        JLabel none = new JLabel("  No stalls yet - vendors can sign up from the login page.");
                        none.setFont(AppTheme.FONT_BODY);
                        none.setForeground(AppTheme.TEXT_SECONDARY);
                        vendorListPanel.add(none);
                    }
                    for (String v : vendors) {
                        vendorListPanel.add(vendorRow(v));
                        vendorListPanel.add(Box.createVerticalStrut(8));
                    }
                    popularBarPanel.revalidate(); popularBarPanel.repaint();
                    vendorListPanel.revalidate(); vendorListPanel.repaint();
                } catch (Exception ex) {
                    setStatus("Could not load stalls - check internet.");
                }
            }
        }.execute();
    }

    /** One stall row: name + View Menu button -> VendorMenuFrame. */
    private JPanel vendorRow(String vendorName) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setBackground(AppTheme.SURFACE);
        row.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER),
                new EmptyBorder(10, 14, 10, 14)));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));

        JLabel name = new JLabel("\uD83C\uDF7D  " + vendorName);
        name.setFont(AppTheme.FONT_SUBHEADING);
        name.setForeground(AppTheme.TEXT_PRIMARY);
        row.add(name, BorderLayout.CENTER);

        JButton view = new JButton("View Menu  \u2192");
        AppTheme.styleButton(view, true);
        view.addActionListener(e ->
                new VendorMenuFrame(studentId, studentName, vendorName, () -> {
                    tabs.setSelectedIndex(1);      // jump to Active Orders
                    activePoller.pollNow();        // show the new order + ETA
                    loadVendorsAndPopular();       // popularity just changed
                }).setVisible(true));
        row.add(view, BorderLayout.EAST);
        return row;
    }

    private JPanel buildHomeTab() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 12));
        wrapper.setBackground(AppTheme.BACKGROUND_DARK);
        wrapper.setBorder(new EmptyBorder(16, 0, 0, 0));

        wrapper.add(buildNextEtaBanner(), BorderLayout.NORTH);
        wrapper.add(buildActiveOrdersPanel(), BorderLayout.CENTER);
        wrapper.add(buildActiveActionPanel(), BorderLayout.SOUTH);
        return wrapper;
    }

    private JPanel buildNextEtaBanner() {
        JPanel banner = new JPanel(new BorderLayout());
        banner.setBackground(AppTheme.SURFACE);
        banner.setBorder(new CompoundBorder(
                new MatteBorder(0, 4, 0, 0, AppTheme.ACCENT_AMBER),
                new EmptyBorder(12, 16, 12, 16)));

        nextEtaLabel = new JLabel("Next up: —");
        nextEtaLabel.setFont(AppTheme.FONT_SUBHEADING);
        nextEtaLabel.setForeground(AppTheme.TEXT_PRIMARY);

        banner.add(nextEtaLabel, BorderLayout.WEST);
        return banner;
    }

    private JPanel buildActiveOrdersPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setOpaque(false);

        JLabel sectionTitle = new JLabel("Your Active Orders");
        sectionTitle.setFont(AppTheme.FONT_SUBHEADING);
        sectionTitle.setForeground(AppTheme.TEXT_PRIMARY);
        panel.add(sectionTitle, BorderLayout.NORTH);

        String[] columns = {"Order ID", "Vendor", "Items", "Status", "ETA (min)", "Placed At"};
        activeTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        activeTable = new JTable(activeTableModel);
        AppTheme.styleTable(activeTable);
        activeTable.setRowHeight(34);   // same row height as the vendor table
        // Order id: FULL id in the model, short form (#A1B2C3) on screen —
        // identical to what the vendor dashboard shows.
        activeTable.getColumnModel().getColumn(0).setCellRenderer(new ShortIdRenderer());
        activeTable.getColumnModel().getColumn(3).setCellRenderer(new StatusBadgeRenderer());
        activeTable.setRowSorter(new TableRowSorter<>(activeTableModel));
        // Cancel is only allowed while the selected order is still "pending".
        activeTable.getSelectionModel().addListSelectionListener(
                e -> updateCancelEnabled());

        JScrollPane scrollPane = new JScrollPane(activeTable);
        scrollPane.getViewport().setBackground(AppTheme.SURFACE);
        scrollPane.setBorder(new LineBorder(AppTheme.BORDER, 1, true));

        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    // ---------------------------------------------------------------
    // Action row: place order + cancel
    // ---------------------------------------------------------------
    private JPanel buildActiveActionPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 16));
        panel.setOpaque(false);

        // Ordering now starts from the Vendors tab (click a stall).
        JLabel hint = new JLabel("To order: open the Vendors tab and pick a stall.");
        hint.setFont(AppTheme.FONT_BODY);
        hint.setForeground(AppTheme.TEXT_SECONDARY);

        cancelButton = new JButton("Cancel Order");
        AppTheme.styleButton(cancelButton, false);
        cancelButton.setEnabled(false);
        cancelButton.addActionListener(e -> cancelSelectedOrder());

        panel.add(cancelButton);
        panel.add(hint);
        return panel;
    }

    /** Real order placement - DB work on a SwingWorker, dialog on the EDT. */
    private void submitOrder(String vendorName, List<String> itemNames) {
        new SwingWorker<String, Void>() {
            @Override protected String doInBackground() {
                // MongoOrderDataSource waits for the consumer thread to
                // persist, so we get a real order id (and the ETA is
                // already computed by EtaEngine inside placeOrder).
                return dataSource.placeOrder(studentId, studentName,
                        vendorName, itemNames);
            }
            @Override protected void done() {
                try {
                    String orderId = get();
                    activePoller.pollNow();   // show it immediately
                    JOptionPane.showMessageDialog(StudentDashboardFrame.this,
                            "Order " + AppTheme.shortId(orderId) + " placed!\n"
                                    + "Watch the Home tab for your live ETA.",
                            "Order placed", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    Throwable c = ex.getCause() != null ? ex.getCause() : ex;
                    JOptionPane.showMessageDialog(StudentDashboardFrame.this,
                            "Could not place order: " + c.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void updateCancelEnabled() {
        int viewRow = activeTable.getSelectedRow();
        if (viewRow < 0) { cancelButton.setEnabled(false); return; }
        int modelRow = activeTable.convertRowIndexToModel(viewRow);
        String status = String.valueOf(activeTableModel.getValueAt(modelRow, 3));
        cancelButton.setEnabled("pending".equalsIgnoreCase(status));
    }

    private void cancelSelectedOrder() {
        String orderId = selectedOrderId();
        if (orderId == null) return;
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() {
                OrderService.getInstance().cancelOrder(orderId);
                return null;
            }
            @Override protected void done() {
                try {
                    get();
                    activePoller.pollNow();
                    historyPoller.pollNow();
                } catch (Exception ex) {
                    // e.g. vendor started cooking between polls
                    Throwable c = ex.getCause() != null ? ex.getCause() : ex;
                    JOptionPane.showMessageDialog(StudentDashboardFrame.this,
                            c.getMessage(), "Cannot cancel",
                            JOptionPane.WARNING_MESSAGE);
                    activePoller.pollNow();
                }
            }
        }.execute();
    }

    // ---------------------------------------------------------------
    // Tab 2: order history + REAL CSV export
    // ---------------------------------------------------------------
    private JPanel buildHistoryTab() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 8));
        wrapper.setBackground(AppTheme.BACKGROUND_DARK);
        wrapper.setBorder(new EmptyBorder(16, 0, 0, 0));

        JLabel sectionTitle = new JLabel("Order History");
        sectionTitle.setFont(AppTheme.FONT_SUBHEADING);
        sectionTitle.setForeground(AppTheme.TEXT_PRIMARY);
        wrapper.add(sectionTitle, BorderLayout.NORTH);

        String[] columns = {"Order ID", "Vendor", "Items", "Status", "Placed At"};
        historyTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        historyTable = new JTable(historyTableModel);
        AppTheme.styleTable(historyTable);
        historyTable.setRowHeight(34);
        historyTable.getColumnModel().getColumn(0).setCellRenderer(new ShortIdRenderer());
        historyTable.getColumnModel().getColumn(3).setCellRenderer(new StatusBadgeRenderer());
        historyTable.setRowSorter(new TableRowSorter<>(historyTableModel));

        JScrollPane scrollPane = new JScrollPane(historyTable);
        scrollPane.getViewport().setBackground(AppTheme.SURFACE);
        scrollPane.setBorder(new LineBorder(AppTheme.BORDER, 1, true));
        wrapper.add(scrollPane, BorderLayout.CENTER);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 16));
        actionPanel.setOpaque(false);
        JButton exportButton = new JButton("Export History (CSV)");
        AppTheme.styleButton(exportButton, false);
        exportButton.addActionListener(e -> exportHistory());
        actionPanel.add(exportButton);
        wrapper.add(actionPanel, BorderLayout.SOUTH);

        return wrapper;
    }

    /** Person A's exporter: BufferedWriter + try-with-resources inside. */
    private void exportHistory() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("my_orders.csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        String path = chooser.getSelectedFile().getAbsolutePath();

        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                // auth's exporter is static and takes the order list itself
                StudentOrderExporter.exportToCSV(
                        new OrderDAO().findByStudent(studentId), path);
                return null;
            }
            @Override protected void done() {
                try {
                    get();
                    JOptionPane.showMessageDialog(StudentDashboardFrame.this,
                            "Order history exported to:\n" + path,
                            "Export complete", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    Throwable c = ex.getCause() != null ? ex.getCause() : ex;
                    JOptionPane.showMessageDialog(StudentDashboardFrame.this,
                            "Export failed: " + c.getMessage(),
                            "Export error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    // ---------------------------------------------------------------
    // "Next up" ETA banner - fed by live data, ids in the shared short form.
    // ---------------------------------------------------------------
    private void refreshNextEtaBanner() {
        int bestRow = -1;
        int bestEta = Integer.MAX_VALUE;

        for (int row = 0; row < activeTableModel.getRowCount(); row++) {
            Object etaValue = activeTableModel.getValueAt(row, 4);
            if (etaValue instanceof Integer) {
                int eta = (Integer) etaValue;
                if (eta < bestEta) {
                    bestEta = eta;
                    bestRow = row;
                }
            }
        }

        if (bestRow == -1) {
            nextEtaLabel.setText("Next up: no active orders");
        } else {
            String orderId = (String) activeTableModel.getValueAt(bestRow, 0);
            String vendor = (String) activeTableModel.getValueAt(bestRow, 1);
            nextEtaLabel.setText("Next up: " + AppTheme.shortId(orderId) + " from "
                    + vendor + " in " + bestEta + " min");
        }
    }
}
