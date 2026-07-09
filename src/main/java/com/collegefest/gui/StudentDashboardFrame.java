package main.java.com.collegefest.gui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;

/**
 * DAY 4 — full layout pass, mirrors the tab structure VendorDashboardFrame
 * got on Day 3 so both dashboards feel like one consistent app.
 *
 * New this round:
 *   - Split into a JTabbedPane: "Active Orders" and "Order History".
 *   - "Next up" ETA banner at the top of Active Orders — highlights the
 *     rush-aware ETA engine, which is the project's headline feature.
 *   - Orders table sortable (carried over from Day 3).
 *
 * Still stubbed (Person B/C wire this up in Steps 2, 3, 5, 8):
 *   - ETA values and the "Next up" banner are hardcoded, not from EtaEngine.
 *   - "Place New Order" adds a local row instead of calling OrderDAO.insertOrder().
 *   - "Export CSV" on both tabs just shows a message — real export is Step 8.
 *   - No SwingWorker polling yet — tables are static until Step 3.
 *   - No Observer wiring yet — vendor status changes won't reach this screen
 *     until Step 5.
 */
public class StudentDashboardFrame extends JFrame {

    private final String studentId;
    private final String studentName;

    private DefaultTableModel activeTableModel;
    private JTable activeTable;
    private JLabel activeOrdersLabel;
    private JLabel nextEtaLabel;

    private DefaultTableModel historyTableModel;
    private JTable historyTable;

    public StudentDashboardFrame(String studentId, String studentName) {
        this.studentId = studentId;
        this.studentName = studentName;

        setTitle("CollegeFest — Student Dashboard");
        setSize(940, 640);
        setMinimumSize(new Dimension(760, 480));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setIconImage(AppIcon.generate());

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(AppTheme.BACKGROUND_DARK);
        root.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        setContentPane(root);

        root.add(buildHeaderPanel(), BorderLayout.NORTH);
        root.add(buildTabs(), BorderLayout.CENTER);

        loadDummyActiveOrders();
        loadDummyHistory();
        refreshNextEtaBanner();
    }

    // ---------------------------------------------------------------
    // Header: welcome text + student id + logout
    // ---------------------------------------------------------------
    private JPanel buildHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);

        JLabel welcome = AppTheme.heading("Welcome, " + studentName);
        JLabel subId = new JLabel("Student ID: " + studentId);
        subId.setFont(AppTheme.FONT_BODY);
        subId.setForeground(AppTheme.TEXT_SECONDARY);
        subId.setBorder(BorderFactory.createEmptyBorder(4, 2, 0, 0));

        left.add(welcome);
        left.add(subId);

        activeOrdersLabel = new JLabel("Active orders: —");
        activeOrdersLabel.setFont(AppTheme.FONT_SUBHEADING);
        activeOrdersLabel.setForeground(AppTheme.ACCENT_AMBER);

        JButton logoutButton = new JButton("Logout");
        AppTheme.styleButton(logoutButton, false);
        logoutButton.addActionListener(e -> {
            // TODO(Step 5): clear session, return to LoginFrame
            dispose();
        });

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.setOpaque(false);
        right.add(activeOrdersLabel);
        right.add(logoutButton);

        panel.add(left, BorderLayout.WEST);
        panel.add(right, BorderLayout.EAST);
        return panel;
    }

    // ---------------------------------------------------------------
    // Tabs: Active Orders / Order History
    // ---------------------------------------------------------------
    private JTabbedPane buildTabs() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(AppTheme.FONT_SUBHEADING);
        tabs.setForeground(AppTheme.TEXT_PRIMARY);
        tabs.setBackground(AppTheme.SURFACE_LIGHT);
        tabs.setOpaque(true);

        tabs.addTab("Active Orders", buildActiveTab());
        tabs.addTab("Order History", buildHistoryTab());
        return tabs;
    }

    // ---------------------------------------------------------------
    // Tab 1: active orders + ETA banner + place order
    // ---------------------------------------------------------------
    private JPanel buildActiveTab() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 12));
        wrapper.setBackground(AppTheme.BACKGROUND_DARK);
        wrapper.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));

        wrapper.add(buildNextEtaBanner(), BorderLayout.NORTH);
        wrapper.add(buildActiveOrdersPanel(), BorderLayout.CENTER);
        wrapper.add(buildActiveActionPanel(), BorderLayout.SOUTH);
        return wrapper;
    }

    private JPanel buildNextEtaBanner() {
        JPanel banner = new JPanel(new BorderLayout());
        banner.setBackground(AppTheme.SURFACE);
        banner.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, AppTheme.ACCENT_AMBER),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)));

        nextEtaLabel = new JLabel("Next up: —");
        nextEtaLabel.setFont(AppTheme.FONT_SUBHEADING);
        nextEtaLabel.setForeground(AppTheme.TEXT_PRIMARY);

        banner.add(nextEtaLabel, BorderLayout.WEST);
        return banner;
    }

    private JPanel buildActiveOrdersPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setOpaque(false);

        JLabel sectionTitle = new JLabel("Active Orders  (click a column header to sort)");
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
        activeTable.getColumnModel().getColumn(3).setCellRenderer(new StatusBadgeRenderer());
        activeTable.setRowSorter(new TableRowSorter<>(activeTableModel));

        JScrollPane scrollPane = new JScrollPane(activeTable);
        scrollPane.getViewport().setBackground(AppTheme.SURFACE);
        scrollPane.setBorder(BorderFactory.createLineBorder(AppTheme.BORDER));

        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildActiveActionPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 16));
        panel.setOpaque(false);

        JButton placeOrderButton = new JButton("+ Place New Order");
        AppTheme.styleButton(placeOrderButton, true);
        placeOrderButton.addActionListener(e -> {
            PlaceOrderDialog dialog = new PlaceOrderDialog(this, result -> {
                // TODO(Step 3): call OrderDAO.insertOrder(...) and
                // EtaEngine.calculateEta(...) here instead of adding a
                // placeholder row directly to the table.
                activeTableModel.addRow(new Object[]{
                        "ORD-" + (1000 + activeTableModel.getRowCount() + historyTableModelSize() + 1),
                        result.vendorName,
                        String.join(", ", result.itemNames),
                        "pending",
                        "—",
                        "just now"
                });
                activeOrdersLabel.setText("Active orders: " + activeTableModel.getRowCount());
                refreshNextEtaBanner();
            });
            dialog.setVisible(true);
        });

        panel.add(placeOrderButton);
        return panel;
    }

    // ---------------------------------------------------------------
    // Tab 2: order history
    // ---------------------------------------------------------------
    private JPanel buildHistoryTab() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 8));
        wrapper.setBackground(AppTheme.BACKGROUND_DARK);
        wrapper.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));

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
        historyTable.getColumnModel().getColumn(3).setCellRenderer(new StatusBadgeRenderer());
        historyTable.setRowSorter(new TableRowSorter<>(historyTableModel));

        JScrollPane scrollPane = new JScrollPane(historyTable);
        scrollPane.getViewport().setBackground(AppTheme.SURFACE);
        scrollPane.setBorder(BorderFactory.createLineBorder(AppTheme.BORDER));
        wrapper.add(scrollPane, BorderLayout.CENTER);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 16));
        actionPanel.setOpaque(false);
        JButton exportButton = new JButton("Export History (CSV)");
        AppTheme.styleButton(exportButton, false);
        exportButton.addActionListener(e ->
            JOptionPane.showMessageDialog(this,
                "TODO (Step 8): export historyTableModel rows to CSV via try-with-resources",
                "Export CSV — stub", JOptionPane.INFORMATION_MESSAGE));
        actionPanel.add(exportButton);
        wrapper.add(actionPanel, BorderLayout.SOUTH);

        return wrapper;
    }

    // ---------------------------------------------------------------
    // "Next up" ETA banner logic — currently reads the dummy ETA column.
    // TODO(Step 2/3): once EtaEngine + live polling exist, recompute this
    // every time activeTableModel refreshes, using real ETA values.
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
            nextEtaLabel.setText("Next up: " + orderId + " from " + vendor + " in " + bestEta + " min");
        }
    }

    private int historyTableModelSize() {
        return historyTableModel == null ? 0 : historyTableModel.getRowCount();
    }

    // ---------------------------------------------------------------
    // Dummy data so the screen is reviewable before OrderDAO is wired in.
    // Replace with live polling in Step 3.
    // ---------------------------------------------------------------
    private void loadDummyActiveOrders() {
        activeTableModel.addRow(new Object[]{"ORD-1001", "Rajesh Food Stall", "Veg Puff, Cold Coffee", "preparing", 6, "12:41 PM"});
        activeTableModel.addRow(new Object[]{"ORD-1002", "Rajesh Food Stall", "Samosa Chaat", "pending", 11, "12:52 PM"});
        activeOrdersLabel.setText("Active orders: 2");
    }

    private void loadDummyHistory() {
        historyTableModel.addRow(new Object[]{"ORD-0987", "Campus Cafe", "Masala Dosa", "served", "11:20 AM"});
        historyTableModel.addRow(new Object[]{"ORD-0954", "Chaat Corner", "Pani Puri", "served", "10:05 AM"});
    }

    // Standalone entry point for reviewing the layout without running the whole app.
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            StudentDashboardFrame frame = new StudentDashboardFrame("STU001", "Shreya Agrawal");
            frame.setVisible(true);
        });
    }
}
