package com.collegefest.gui;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.collegefest.models.Item;

/**
 * STUDENT-FACING MENU WINDOW - opens when a student clicks a stall on the
 * Vendors tab of their dashboard.
 *
 * Layout (per the feature request):
 *  - themed top bar with the stall's name
 *  - one row per menu item:  name  ₹price  ·  "ETA ~X min"  ·  [-] 0 [+]
 *    The ETA is LIVE - EtaEngine computes it against the stall's current
 *    queue, so a busy stall honestly shows longer times per item.
 *  - out-of-stock items are still LISTED (greyed, "out of stock" tag) but
 *    have no steppers, so they cannot be added - exactly the requested rule.
 *  - bottom-LEFT: running Total ₹ · bottom-RIGHT: Place order button
 *    (enabled once at least one quantity is above zero).
 *
 * Threading: the menu+ETA load and the order submission each run on a
 * SwingWorker; the window shows "Loading menu..." meanwhile and is never
 * frozen. On success it closes itself and tells the dashboard (onPlaced
 * callback) to jump to the Active Orders tab and poll immediately.
 */
public class VendorMenuFrame extends JFrame {

    private final MongoOrderDataSource dataSource = MongoOrderDataSource.getInstance();
    private final String studentId;
    private final String studentName;
    private final String vendorName;
    private final Runnable onPlaced;

    private final JPanel menuPanel = new JPanel();
    private final JLabel totalLabel = new JLabel("Total:  \u20B90");
    private final JButton placeButton = new JButton("Place order");
    private final JLabel statusLabel = new JLabel(" ");

    /** item name -> {price, chosen quantity, quantity label} */
    private final Map<String, Double> prices = new LinkedHashMap<>();
    private final Map<String, Integer> quantities = new LinkedHashMap<>();
    private final Map<String, JLabel> qtyLabels = new LinkedHashMap<>();

    public VendorMenuFrame(String studentId, String studentName,
                           String vendorName, Runnable onPlaced) {
        super(vendorName + " - Menu");
        this.studentId = studentId;
        this.studentName = studentName;
        this.vendorName = vendorName;
        this.onPlaced = onPlaced;

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setSize(560, 620);
        setLocationRelativeTo(null);
        setIconImage(AppIcon.generate());

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(AppTheme.BACKGROUND_DARK);
        setContentPane(root);

        // ---- top bar, same style as the dashboards ----
        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(AppTheme.SURFACE);
        top.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));
        JLabel title = new JLabel("\uD83C\uDF7D  " + vendorName);
        title.setFont(AppTheme.FONT_HEADING);
        title.setForeground(AppTheme.TEXT_PRIMARY);
        JLabel sub = new JLabel("Pick quantities with + / -   \u00B7   ETA updates with the live queue");
        sub.setFont(AppTheme.FONT_BODY);
        sub.setForeground(AppTheme.TEXT_SECONDARY);
        JPanel titleBox = new JPanel(new GridLayout(0, 1));
        titleBox.setOpaque(false);
        titleBox.add(title);
        titleBox.add(sub);
        top.add(titleBox, BorderLayout.WEST);
        root.add(top, BorderLayout.NORTH);

        // ---- menu rows ----
        menuPanel.setLayout(new BoxLayout(menuPanel, BoxLayout.Y_AXIS));
        menuPanel.setBackground(AppTheme.BACKGROUND_DARK);
        menuPanel.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));
        JScrollPane scroll = new JScrollPane(menuPanel,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(AppTheme.BACKGROUND_DARK);
        root.add(scroll, BorderLayout.CENTER);

        // ---- bottom bar: Total (left)  ·  Place order (right) ----
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBackground(AppTheme.SURFACE);
        bottom.setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 18));
        totalLabel.setFont(AppTheme.FONT_HEADING);
        totalLabel.setForeground(AppTheme.ACCENT_AMBER);
        bottom.add(totalLabel, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        statusLabel.setFont(AppTheme.FONT_BODY);
        statusLabel.setForeground(AppTheme.TEXT_SECONDARY);
        AppTheme.styleButton(placeButton, true);
        placeButton.setEnabled(false);
        placeButton.addActionListener(e -> placeOrder());
        right.add(statusLabel);
        right.add(placeButton);
        bottom.add(right, BorderLayout.EAST);
        root.add(bottom, BorderLayout.SOUTH);

        loadMenu();
    }

    // ---------------------------------------------------------------
    private void loadMenu() {
        JLabel loading = new JLabel("Loading menu...");
        loading.setFont(AppTheme.FONT_BODY);
        loading.setForeground(AppTheme.TEXT_SECONDARY);
        menuPanel.add(loading);

        new SwingWorker<List<Object[]>, Void>() {
            @Override protected List<Object[]> doInBackground() {
                // {Item, Integer liveEta} per row - all DB work off the EDT.
                List<Object[]> rows = new ArrayList<>();
                for (Item item : dataSource.menuForVendorName(vendorName)) {
                    int eta = dataSource.etaFor(vendorName, item.getPrepTimeMinutes());
                    rows.add(new Object[]{item, eta});
                }
                return rows;
            }
            @Override protected void done() {
                menuPanel.removeAll();
                try {
                    List<Object[]> rows = get();
                    if (rows.isEmpty()) {
                        JLabel empty = new JLabel(
                                vendorName + " has not added any menu items yet.");
                        empty.setFont(AppTheme.FONT_BODY);
                        empty.setForeground(AppTheme.TEXT_SECONDARY);
                        menuPanel.add(empty);
                    }
                    for (Object[] row : rows) {
                        menuPanel.add(menuRow((Item) row[0], (Integer) row[1]));
                        menuPanel.add(Box.createVerticalStrut(8));
                    }
                } catch (Exception ex) {
                    JLabel err = new JLabel("Menu load failed - check internet.");
                    err.setForeground(AppTheme.TEXT_SECONDARY);
                    menuPanel.add(err);
                }
                menuPanel.revalidate();
                menuPanel.repaint();
            }
        }.execute();
    }

    /** One item row. Out-of-stock rows are listed but cannot be added. */
    private JPanel menuRow(Item item, int liveEta) {
        boolean inStock = item.isAvailable();

        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setBackground(AppTheme.SURFACE);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 54));

        String text = item.getName() + "   \u20B9" + (int) item.getPrice()
                + "   \u00B7   ETA ~" + liveEta + " min"
                + (inStock ? "" : "   \u00B7   OUT OF STOCK");
        JLabel name = new JLabel(text);
        name.setFont(AppTheme.FONT_BODY);
        name.setForeground(inStock ? AppTheme.TEXT_PRIMARY : AppTheme.TEXT_SECONDARY);
        row.add(name, BorderLayout.CENTER);

        if (inStock) {
            prices.put(item.getName(), item.getPrice());
            quantities.put(item.getName(), 0);

            JPanel stepper = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
            stepper.setOpaque(false);
            JButton minus = stepButton("-");
            JLabel qty = new JLabel("0");
            qty.setFont(AppTheme.FONT_SUBHEADING);
            qty.setForeground(AppTheme.TEXT_PRIMARY);
            qty.setPreferredSize(new Dimension(26, 22));
            qty.setHorizontalAlignment(SwingConstants.CENTER);
            JButton plus = stepButton("+");
            qtyLabels.put(item.getName(), qty);
            minus.addActionListener(e -> bump(item.getName(), -1));
            plus.addActionListener(e -> bump(item.getName(), +1));
            stepper.add(minus); stepper.add(qty); stepper.add(plus);
            row.add(stepper, BorderLayout.EAST);
        }
        return row;
    }

    private void bump(String itemName, int delta) {
        int next = Math.max(0, quantities.getOrDefault(itemName, 0) + delta);
        quantities.put(itemName, next);
        qtyLabels.get(itemName).setText(String.valueOf(next));
        refreshTotal();
    }

    /** Total = sum of price x quantity, shown bottom-left, always live. */
    private void refreshTotal() {
        double total = 0;
        int count = 0;
        for (Map.Entry<String, Integer> e : quantities.entrySet()) {
            total += prices.getOrDefault(e.getKey(), 0.0) * e.getValue();
            count += e.getValue();
        }
        totalLabel.setText("Total:  \u20B9" + (int) total);
        placeButton.setEnabled(count > 0);
        statusLabel.setText(count > 0 ? count + " item(s)" : " ");
    }

    private void placeOrder() {
        List<String> selected = new ArrayList<>();
        for (Map.Entry<String, Integer> e : quantities.entrySet()) {
            for (int i = 0; i < e.getValue(); i++) selected.add(e.getKey());
        }
        if (selected.isEmpty()) return;

        placeButton.setEnabled(false);
        placeButton.setText("Placing...");

        new SwingWorker<String, Void>() {
            @Override protected String doInBackground() {
                return dataSource.placeOrder(studentId, studentName,
                        vendorName, selected);
            }
            @Override protected void done() {
                placeButton.setText("Place order");
                try {
                    get();
                    JOptionPane.showMessageDialog(VendorMenuFrame.this,
                            "Order placed at " + vendorName + "!\n"
                                    + "Watch Active Orders for your live ETA.",
                            "Order placed", JOptionPane.INFORMATION_MESSAGE);
                    dispose();
                    if (onPlaced != null) onPlaced.run();
                } catch (Exception ex) {
                    placeButton.setEnabled(true);
                    Throwable c = ex.getCause() != null ? ex.getCause() : ex;
                    JOptionPane.showMessageDialog(VendorMenuFrame.this,
                            "Could not place order: " + c.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private JButton stepButton(String symbol) {
        JButton b = new JButton(symbol);
        b.setFont(new Font("Segoe UI", Font.BOLD, 14));
        b.setBackground(AppTheme.SURFACE_LIGHT);
        b.setForeground(AppTheme.TEXT_PRIMARY);
        b.setFocusPainted(false);
        b.setMargin(new Insets(0, 8, 0, 8));
        return b;
    }
}
