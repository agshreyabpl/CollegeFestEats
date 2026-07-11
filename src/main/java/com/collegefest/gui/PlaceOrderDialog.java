package com.collegefest.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;

import com.collegefest.models.Item;

/**
 * "Place New Order" dialog - integration rewrite of Person C's Day-2 stub.
 *
 * What her TODOs asked for, now done:
 *  - The vendor dropdown is loaded LIVE from MongoDB (every vendor account,
 *    including stalls created minutes ago through "Create account").
 *  - Selecting a vendor loads THAT VENDOR'S OWN MENU from the items
 *    collection - switching vendors switches the list (the old bug where
 *    every vendor showed the same hardcoded items is gone).
 *  - Each menu row has a [-] count [+] quantity stepper starting at 0.
 *    "Place order" is enabled once at least one quantity is above zero.
 *
 * Threading: every MongoDB call (vendor list, each menu load) runs in a
 * SwingWorker; the EDT only ever touches ready-made results. While a menu
 * loads, a "Loading menu..." label shows so the dialog never feels frozen.
 *
 * The result still goes back through Consumer<PlaceOrderResult> exactly
 * like before, so StudentDashboardFrame needed no changes. Quantities are
 * encoded by repeating the item name (2x Samosa -> ["Samosa","Samosa"]),
 * which is what OrderService's average-prep-time and the vendor's
 * popularity ranking already expect.
 */
public class PlaceOrderDialog extends JDialog {

    private final MongoOrderDataSource dataSource = MongoOrderDataSource.getInstance();
    private final Consumer<PlaceOrderResult> onSubmit;

    private final JComboBox<String> vendorDropdown = new JComboBox<>();
    private final JPanel menuPanel = new JPanel();
    private final JLabel statusLabel = new JLabel(" ");
    private final JButton submitButton = new JButton("Place order");

    /** item name -> its quantity label (so +/- buttons can update it). */
    private final Map<String, JLabel> qtyLabels = new LinkedHashMap<>();
    /** item name -> chosen quantity (the actual cart). */
    private final Map<String, Integer> quantities = new LinkedHashMap<>();
    /** item name -> price, so the running total can be computed on every +/-. */
    private final Map<String, Double> prices = new LinkedHashMap<>();
    /** Big amber running total ("Total: ₹408") next to the buttons. */
    private final JLabel totalLabel = new JLabel("Total: ₹0");

    public PlaceOrderDialog(JFrame owner, Consumer<PlaceOrderResult> onSubmit) {
        super(owner, "Place New Order", true);
        this.onSubmit = onSubmit;

        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(AppTheme.SURFACE);
        root.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        root.add(sectionLabel("Vendor"));
        styleCombo(vendorDropdown);
        vendorDropdown.addActionListener(e -> loadMenuForSelectedVendor());
        root.add(vendorDropdown);
        root.add(Box.createVerticalStrut(14));

        root.add(sectionLabel("Menu  (use + / - to choose quantities)"));
        menuPanel.setLayout(new BoxLayout(menuPanel, BoxLayout.Y_AXIS));
        menuPanel.setBackground(AppTheme.SURFACE);
        JScrollPane scroll = new JScrollPane(menuPanel,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setPreferredSize(new Dimension(420, 240));
        scroll.setBorder(BorderFactory.createLineBorder(AppTheme.BORDER));
        scroll.getViewport().setBackground(AppTheme.SURFACE);
        scroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(scroll);
        root.add(Box.createVerticalStrut(8));

        statusLabel.setFont(AppTheme.FONT_BODY);
        statusLabel.setForeground(AppTheme.TEXT_SECONDARY);
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(statusLabel);
        root.add(Box.createVerticalStrut(10));

        // Bottom row: running total on the left, Cancel / Place order on the right.
        JPanel bottomRow = new JPanel(new BorderLayout());
        bottomRow.setOpaque(false);
        bottomRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        totalLabel.setFont(AppTheme.FONT_SUBHEADING);
        totalLabel.setForeground(AppTheme.ACCENT_AMBER);
        bottomRow.add(totalLabel, BorderLayout.WEST);

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonRow.setOpaque(false);
        JButton cancelButton = new JButton("Cancel");
        AppTheme.styleButton(cancelButton, false);
        AppTheme.styleButton(submitButton, true);
        submitButton.setEnabled(false);   // nothing chosen yet
        cancelButton.addActionListener(e -> dispose());
        submitButton.addActionListener(e -> submit());
        buttonRow.add(cancelButton);
        buttonRow.add(submitButton);
        bottomRow.add(buttonRow, BorderLayout.EAST);
        root.add(bottomRow);

        setContentPane(root);
        pack();
        setLocationRelativeTo(owner);

        loadVendors();   // kicks everything off (background thread)
    }

    // ---------------------------------------------------------------
    // Vendors - live from MongoDB, so brand-new stalls appear too
    // ---------------------------------------------------------------
    private void loadVendors() {
        statusLabel.setText("Loading vendors...");
        new SwingWorker<String[], Void>() {
            @Override protected String[] doInBackground() {
                return dataSource.freshVendorNames();
            }
            @Override protected void done() {
                try {
                    vendorDropdown.removeAllItems();
                    for (String name : get()) vendorDropdown.addItem(name);
                    statusLabel.setText(" ");
                    if (vendorDropdown.getItemCount() > 0) {
                        vendorDropdown.setSelectedIndex(0); // triggers menu load
                    } else {
                        statusLabel.setText("No vendors yet - create a vendor account first.");
                    }
                } catch (Exception ex) {
                    statusLabel.setText("Could not load vendors - check internet.");
                }
            }
        }.execute();
    }

    // ---------------------------------------------------------------
    // Menu - reloaded EVERY time the vendor selection changes
    // ---------------------------------------------------------------
    private void loadMenuForSelectedVendor() {
        String vendorName = (String) vendorDropdown.getSelectedItem();
        if (vendorName == null) return;

        menuPanel.removeAll();
        qtyLabels.clear();
        quantities.clear();
        prices.clear();
        updateSubmitState();
        JLabel loading = new JLabel("Loading menu for " + vendorName + "...");
        loading.setFont(AppTheme.FONT_BODY);
        loading.setForeground(AppTheme.TEXT_SECONDARY);
        menuPanel.add(loading);
        menuPanel.revalidate();
        menuPanel.repaint();

        new SwingWorker<List<Item>, Void>() {
            @Override protected List<Item> doInBackground() {
                return dataSource.menuForVendorName(vendorName);
            }
            @Override protected void done() {
                // Ignore stale results if the user already switched vendor.
                if (!vendorName.equals(vendorDropdown.getSelectedItem())) return;
                menuPanel.removeAll();
                try {
                    List<Item> items = get();
                    if (items.isEmpty()) {
                        JLabel empty = new JLabel(
                                vendorName + " has not added any menu items yet.");
                        empty.setFont(AppTheme.FONT_BODY);
                        empty.setForeground(AppTheme.TEXT_SECONDARY);
                        menuPanel.add(empty);
                    }
                    for (Item item : items) {
                        menuPanel.add(menuRow(item));
                        menuPanel.add(Box.createVerticalStrut(6));
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

    /**
     * One row: "Samosa   ₹20  (2 min)      [-]  0  [+]"
     *
     * OUT-OF-STOCK items still appear (feature request: students should be
     * able to SEE them) but greyed out with an "OUT OF STOCK" tag and the
     * +/- stepper disabled, so they can never be added to an order.
     */
    private JPanel menuRow(Item item) {
        boolean inStock = item.isAvailable();

        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));

        JLabel name = new JLabel(item.getName() + "   ₹" + (int) item.getPrice()
                + "   (" + item.getPrepTimeMinutes() + " min)");
        name.setFont(AppTheme.FONT_BODY);
        name.setForeground(inStock ? AppTheme.TEXT_PRIMARY : AppTheme.TEXT_SECONDARY);
        row.add(name, BorderLayout.CENTER);

        if (!inStock) {
            // Visible but unorderable — just a red tag where the stepper would be.
            JLabel tag = new JLabel("OUT OF STOCK");
            tag.setFont(AppTheme.FONT_TABLE_HEAD);
            tag.setForeground(AppTheme.STATUS_CANCELLED);
            tag.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(AppTheme.BORDER),
                    BorderFactory.createEmptyBorder(2, 8, 2, 8)));
            JPanel tagWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
            tagWrap.setOpaque(false);
            tagWrap.add(tag);
            row.add(tagWrap, BorderLayout.EAST);
            return row;   // no stepper, no quantities entry -> can't be ordered
        }

        // The +/- stepper. Quantity starts at 0 for every item.
        quantities.put(item.getName(), 0);
        prices.put(item.getName(), item.getPrice());
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

        stepper.add(minus);
        stepper.add(qty);
        stepper.add(plus);
        row.add(stepper, BorderLayout.EAST);
        return row;
    }

    private void bump(String itemName, int delta) {
        int next = Math.max(0, quantities.getOrDefault(itemName, 0) + delta);
        quantities.put(itemName, next);
        qtyLabels.get(itemName).setText(String.valueOf(next));
        updateSubmitState();
    }

    private void updateSubmitState() {
        int count = quantities.values().stream().mapToInt(Integer::intValue).sum();
        // Total amount = Σ price × quantity across the whole cart.
        double amount = quantities.entrySet().stream()
                .mapToDouble(e -> prices.getOrDefault(e.getKey(), 0.0) * e.getValue())
                .sum();
        submitButton.setEnabled(count > 0);
        statusLabel.setText(count > 0 ? count + " item(s) selected" : " ");
        totalLabel.setText("Total: ₹" + formatAmount(amount));
    }

    /** ₹199 for whole amounts, ₹199.50 when there are paise. */
    private static String formatAmount(double amount) {
        if (amount == Math.floor(amount)) {
            return String.valueOf((long) amount);
        }
        return String.format("%.2f", amount);
    }

    private void submit() {
        String vendorName = (String) vendorDropdown.getSelectedItem();
        List<String> selected = new ArrayList<>();
        for (Map.Entry<String, Integer> e : quantities.entrySet()) {
            for (int i = 0; i < e.getValue(); i++) selected.add(e.getKey());
        }
        if (vendorName == null || selected.isEmpty()) return;   // belt & braces
        onSubmit.accept(new PlaceOrderResult(vendorName, selected));
        dispose();
    }

    // ---- styling helpers (Person C's originals, kept) ----

    private JLabel sectionLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(AppTheme.FONT_SUBHEADING);
        label.setForeground(AppTheme.TEXT_SECONDARY);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));
        return label;
    }

    private void styleCombo(JComboBox<String> combo) {
        combo.setBackground(AppTheme.SURFACE);
        combo.setForeground(AppTheme.TEXT_PRIMARY);
        combo.setFont(AppTheme.FONT_BODY);
        combo.setMaximumSize(new Dimension(420, 32));
        combo.setAlignmentX(Component.LEFT_ALIGNMENT);
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

    /** What the dialog collected - same shape Person C defined. */
    public static class PlaceOrderResult {
        public final String vendorName;
        public final List<String> itemNames;

        public PlaceOrderResult(String vendorName, List<String> itemNames) {
            this.vendorName = vendorName;
            this.itemNames = itemNames;
        }
    }
}
