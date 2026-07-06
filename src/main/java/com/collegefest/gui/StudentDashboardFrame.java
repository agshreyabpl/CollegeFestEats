package com.collegefest.gui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/**
 * DAY 1 SKELETON — layout and wireframe only.
 *
 * What's real right now:
 *   - Full layout: header, orders table, action bar.
 *   - Dummy rows in the table so the screen is reviewable without a DB call.
 *
 * What's a stub (Person C wires this up properly in Step 3):
 *   - "Place New Order" opens a placeholder dialog, doesn't insert into Mongo.
 *   - "Export CSV" just shows a message (real logic comes in Step 8).
 *   - No SwingWorker polling yet — table is static until Step 3.
 *
 * TODO(Step 3): replace loadDummyOrders() with a SwingWorker that calls
 * OrderDAO.findByStudent(studentId) every 5s and repopulates tableModel.
 */
public class StudentDashboardFrame extends JFrame {

	private static final long serialVersionUID = 1L;
	
	private final String studentId;
    private final String studentName;

    private DefaultTableModel tableModel;
    private JTable ordersTable;
    private JLabel activeOrdersLabel;

    public StudentDashboardFrame(String studentId, String studentName) {
        this.studentId = studentId;
        this.studentName = studentName;

        setTitle("CollegeFest — Student Dashboard");
        setSize(920, 600);
        setMinimumSize(new Dimension(760, 460));
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
    // Center: orders table
    // ---------------------------------------------------------------
    private JPanel buildOrdersPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setOpaque(false);

        JLabel sectionTitle = new JLabel("Your Orders");
        sectionTitle.setFont(AppTheme.FONT_SUBHEADING);
        sectionTitle.setForeground(AppTheme.TEXT_PRIMARY);
        panel.add(sectionTitle, BorderLayout.NORTH);

        String[] columns = {"Order ID", "Vendor", "Items", "Status", "ETA (min)", "Placed At"};
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
    // Footer: place order / export
    // ---------------------------------------------------------------
    private JPanel buildActionPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 16));
        panel.setOpaque(false);

        JButton placeOrderButton = new JButton("+ Place New Order");
        AppTheme.styleButton(placeOrderButton, true);
        placeOrderButton.addActionListener(e ->
            JOptionPane.showMessageDialog(this,
                "TODO (Step 3): open vendor/item picker and call OrderDAO.insertOrder()",
                "Place Order — stub", JOptionPane.INFORMATION_MESSAGE));

        JButton exportButton = new JButton("Export History (CSV)");
        AppTheme.styleButton(exportButton, false);
        exportButton.addActionListener(e ->
            JOptionPane.showMessageDialog(this,
                "TODO (Step 8): export tableModel rows to CSV via try-with-resources",
                "Export CSV — stub", JOptionPane.INFORMATION_MESSAGE));

        panel.add(placeOrderButton);
        panel.add(exportButton);
        return panel;
    }

    // ---------------------------------------------------------------
    // Dummy data so the screen is reviewable before OrderDAO is wired in.
    // Replace with live polling in Step 3.
    // ---------------------------------------------------------------
    private void loadDummyOrders() {
        tableModel.addRow(new Object[]{"ORD-1001", "Rajesh Food Stall", "Veg Puff, Cold Coffee", "preparing", 6, "12:41 PM"});
        tableModel.addRow(new Object[]{"ORD-1002", "Rajesh Food Stall", "Samosa Chaat", "pending", 11, "12:52 PM"});
        activeOrdersLabel.setText("Active orders: 2");
    }

    // Standalone entry point for reviewing the wireframe without running the whole app.
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            StudentDashboardFrame frame = new StudentDashboardFrame("STU001", "Shreya Agrawal");
            frame.setVisible(true);
        });
    }
}

