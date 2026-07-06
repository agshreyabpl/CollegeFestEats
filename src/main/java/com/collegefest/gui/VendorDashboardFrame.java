package com.collegefest.gui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/**
 * DAY 1 SKELETON — layout and wireframe only.
 *
 * What's real right now:
 *   - Full layout: header, incoming-orders table, status-update buttons, menu panel.
 *   - Dummy rows so the screen is reviewable without a DB call.
 *
 * What's a stub (Person B/C wire this up in Steps 4 & 5):
 *   - Status buttons print to console instead of calling OrderDAO.updateStatus().
 *   - "Add Menu Item" opens a placeholder dialog, doesn't call ItemDAO.insertItem().
 *   - No Observer notification to StudentDashboard yet — that's Step 5.
 *
 * 
 * based on the selected order's current status (pending -> preparing -> ready -> served).
 */
public class VendorDashboardFrame extends JFrame {

	private static final long serialVersionUID = 1L;
	
	private final String vendorId;
    private final String vendorName;

    private DefaultTableModel tableModel;
    private JTable ordersTable;
    private JLabel queueCountLabel;

    public VendorDashboardFrame(String vendorId, String vendorName) {
        this.vendorId = vendorId;
        this.vendorName = vendorName;

        setTitle("CollegeFest — Vendor Dashboard");
        setSize(960, 620);
        setMinimumSize(new Dimension(800, 480));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(AppTheme.BACKGROUND_DARK);
        root.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        setContentPane(root);

        root.add(buildHeaderPanel(), BorderLayout.NORTH);
        root.add(buildOrdersPanel(), BorderLayout.CENTER);
        root.add(buildActionPanel(), BorderLayout.SOUTH);

        loadDummyOrders();
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

        JButton logoutButton = new JButton("Logout");
        AppTheme.styleButton(logoutButton, false);
        logoutButton.addActionListener(e -> dispose()); // TODO(Step 5): clear session

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.setOpaque(false);
        right.add(queueCountLabel);
        right.add(logoutButton);

        panel.add(left, BorderLayout.WEST);
        panel.add(right, BorderLayout.EAST);
        return panel;
    }

    // ---------------------------------------------------------------
    // Center: incoming orders table
    // ---------------------------------------------------------------
    private JPanel buildOrdersPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setOpaque(false);

        JLabel sectionTitle = new JLabel("Incoming Orders");
        sectionTitle.setFont(AppTheme.FONT_SUBHEADING);
        sectionTitle.setForeground(AppTheme.TEXT_PRIMARY);
        panel.add(sectionTitle, BorderLayout.NORTH);

        String[] columns = {"Order ID", "Student", "Items", "Status", "Placed At"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        ordersTable = new JTable(tableModel);
        AppTheme.styleTable(ordersTable);

        JScrollPane scrollPane = new JScrollPane(ordersTable);
        scrollPane.getViewport().setBackground(AppTheme.SURFACE);
        scrollPane.setBorder(BorderFactory.createLineBorder(AppTheme.BORDER));

        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    // ---------------------------------------------------------------
    // Footer: status-update buttons + menu management
    // ---------------------------------------------------------------
    private JPanel buildActionPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));

        JPanel statusButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        statusButtons.setOpaque(false);

        JButton markPreparing = new JButton("Mark Preparing");
        JButton markReady = new JButton("Mark Ready");
        JButton markServed = new JButton("Mark Served");
        AppTheme.styleButton(markPreparing, false);
        AppTheme.styleButton(markReady, false);
        AppTheme.styleButton(markServed, true);

        markPreparing.addActionListener(e -> stubStatusUpdate("preparing"));
        markReady.addActionListener(e -> stubStatusUpdate("ready"));
        markServed.addActionListener(e -> stubStatusUpdate("served"));

        statusButtons.add(markPreparing);
        statusButtons.add(markReady);
        statusButtons.add(markServed);

        JButton addMenuItem = new JButton("+ Add Menu Item");
        AppTheme.styleButton(addMenuItem, false);
        addMenuItem.addActionListener(e ->
            JOptionPane.showMessageDialog(this,
                "TODO (Step 6): open form and call ItemDAO.insertItem()",
                "Add Menu Item — stub", JOptionPane.INFORMATION_MESSAGE));

        JPanel menuPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        menuPanel.setOpaque(false);
        menuPanel.add(addMenuItem);

        panel.add(statusButtons, BorderLayout.WEST);
        panel.add(menuPanel, BorderLayout.EAST);
        return panel;
    }

    private void stubStatusUpdate(String newStatus) {
        int row = ordersTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select an order first.", "No selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        // TODO(Step 4/5): call OrderDAO.updateStatus(orderId, newStatus)
        // then notify observers so StudentDashboard picks it up on next poll.
        tableModel.setValueAt(newStatus, row, 3);
    }

    // ---------------------------------------------------------------
    // Dummy data — replace with OrderDAO.findByVendor(vendorId) in Step 4
    // ---------------------------------------------------------------
    private void loadDummyOrders() {
        tableModel.addRow(new Object[]{"ORD-1001", "Shreya Agrawal", "Veg Puff, Cold Coffee", "preparing", "12:41 PM"});
        tableModel.addRow(new Object[]{"ORD-1002", "Aman Verma", "Samosa Chaat", "pending", "12:52 PM"});
        tableModel.addRow(new Object[]{"ORD-1003", "Priya Nair", "Cold Coffee", "ready", "12:35 PM"});
        queueCountLabel.setText("Queue: 3");
    }

    // Standalone entry point for reviewing the wireframe without running the whole app.
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            VendorDashboardFrame frame = new VendorDashboardFrame("VEN001", "Rajesh Food Stall");
            frame.setVisible(true);
        });
    }
}
