package com.collegefest.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;

import com.collegefest.db.ItemDAO;
import com.collegefest.models.Item;

/**
 * VENDOR-FACING MENU MANAGER - its own window (feature request: "make a
 * separate window for my menu and add items"), opened from the "My Menu"
 * button on the Vendor Dashboard's top bar.
 *
 * Left:  "Add Menu Item" form (name, price, prep minutes, available) with
 *        regex validation, saved via ItemDAO.insertItem on a SwingWorker.
 * Right: "My Menu" - every item this vendor has EVER added, each with an
 *        IN STOCK checkbox. Untick = students see it greyed "OUT OF STOCK"
 *        and cannot add it; tick = back on sale instantly. Because the list
 *        shows out-of-stock items too, nothing ever disappears from the
 *        vendor's view - the exact problem this window was built to fix.
 *
 * Every DB call (load, add, toggle) runs on a SwingWorker; the list
 * reloads after each change so the window always mirrors MongoDB.
 */
public class VendorMenuManagerFrame extends JFrame {

    private final ItemDAO itemDAO = new ItemDAO();
    private final String vendorId;

    private final JPanel listPanel = new JPanel();
    private final JLabel statusLabel = new JLabel(" ");
    private JTextField nameField, priceField, prepField;
    private JCheckBox availableCheck;

    public VendorMenuManagerFrame(String vendorId) {
        super("My Menu - manage items");
        this.vendorId = vendorId;

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setSize(760, 520);
        setLocationRelativeTo(null);
        setIconImage(AppIcon.generate());

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(AppTheme.BACKGROUND_DARK);
        setContentPane(root);

        // ---- top bar ----
        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(AppTheme.SURFACE);
        top.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));
        JPanel titleBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        titleBox.setOpaque(false);
        JLabel icon = new JLabel("\uD83C\uDF54");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 26));
        icon.setForeground(AppTheme.TEXT_PRIMARY);
        JLabel title = new JLabel("My Menu");
        title.setFont(AppTheme.FONT_HEADING);
        title.setForeground(AppTheme.TEXT_PRIMARY);
        titleBox.add(icon);
        titleBox.add(title);

        JLabel sub = new JLabel("Add items on the left  \u00B7  tick / untick stock on the right");
        sub.setFont(AppTheme.FONT_BODY);
        sub.setForeground(AppTheme.TEXT_SECONDARY);
        JPanel box = new JPanel(new GridLayout(0, 1));
        box.setOpaque(false);
        box.add(titleBox);
        box.add(sub);
        top.add(box, BorderLayout.WEST);
        root.add(top, BorderLayout.NORTH);

        // ---- split: add form (left) | live list (right) ----
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                buildAddForm(), buildListPane());
        split.setDividerLocation(300);
        split.setBorder(null);
        split.setBackground(AppTheme.BACKGROUND_DARK);
        root.add(split, BorderLayout.CENTER);

        // ---- status bar ----
        statusLabel.setFont(AppTheme.FONT_BODY);
        statusLabel.setForeground(AppTheme.TEXT_SECONDARY);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBackground(AppTheme.SURFACE);
        bottom.add(statusLabel, BorderLayout.WEST);
        root.add(bottom, BorderLayout.SOUTH);

        reloadItems();
    }

    // ---------------- Add Menu Item ----------------
    private JPanel buildAddForm() {
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBackground(AppTheme.BACKGROUND_DARK);
        form.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        form.add(sectionLabel("\uFF0B  Add Menu Item"));
        form.add(Box.createVerticalStrut(10));
        nameField  = addLabelled(form, "Item name");
        priceField = addLabelled(form, "Price (\u20B9)");
        prepField  = addLabelled(form, "Prep time (minutes)");

        availableCheck = new JCheckBox("Available now", true);
        availableCheck.setFont(AppTheme.FONT_BODY);
        availableCheck.setForeground(AppTheme.TEXT_PRIMARY);
        availableCheck.setOpaque(false);
        availableCheck.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(availableCheck);
        form.add(Box.createVerticalStrut(14));

        JButton addBtn = new JButton("Add item");
        AppTheme.styleButton(addBtn, true);
        addBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        addBtn.addActionListener(e -> addItem());
        form.add(addBtn);
        form.add(Box.createVerticalGlue());
        return form;
    }

    private void addItem() {
        String name = nameField.getText().trim();
        String price = priceField.getText().trim();
        String prep = prepField.getText().trim();
        // REGEX validation, same rules as everywhere else in the app.
        if (name.isEmpty() || !price.matches("^[0-9]+(\\.[0-9]+)?$")
                || !prep.matches("^[1-9][0-9]*$")) {
            statusLabel.setText("Fill all fields: name, numeric price, positive prep minutes.");
            return;
        }
        final Item item = new Item(vendorId, name, Double.parseDouble(price),
                availableCheck.isSelected(), Integer.parseInt(prep));

        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() {
                itemDAO.insertItem(item);
                return null;
            }
            @Override protected void done() {
                try {
                    get();
                    nameField.setText(""); priceField.setText(""); prepField.setText("");
                    statusLabel.setText("\u2713  '" + item.getName() + "' added.");
                    reloadItems();
                } catch (Exception ex) {
                    statusLabel.setText("Add failed: " + root(ex));
                }
            }
        }.execute();
    }

    // ---------------- My Menu list with stock toggles ----------------
    private JScrollPane buildListPane() {
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(AppTheme.BACKGROUND_DARK);
        listPanel.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        JScrollPane scroll = new JScrollPane(listPanel,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(AppTheme.BACKGROUND_DARK);
        return scroll;
    }

    private void reloadItems() {
        new SwingWorker<List<Item>, Void>() {
            @Override protected List<Item> doInBackground() {
                return itemDAO.findByVendor(vendorId);
            }
            @Override protected void done() {
                listPanel.removeAll();
                listPanel.add(sectionLabel("🍔  My Menu  (check = in stock)"));
                listPanel.add(Box.createVerticalStrut(10));
                try {
                    List<Item> items = get();
                    if (items.isEmpty()) {
                        JLabel none = new JLabel("No items yet - add your first one on the left.");
                        none.setFont(AppTheme.FONT_BODY);
                        none.setForeground(AppTheme.TEXT_SECONDARY);
                        listPanel.add(none);
                    }
                    for (Item item : items) {
                        listPanel.add(itemRow(item));
                        listPanel.add(Box.createVerticalStrut(6));
                    }
                } catch (Exception ex) {
                    statusLabel.setText("Load failed: " + root(ex));
                }
                listPanel.revalidate();
                listPanel.repaint();
            }
        }.execute();
    }

    /** "Samosa  ₹20  (2 min)          [ ] In stock" - toggle writes to Mongo. */
    private JPanel itemRow(Item item) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setBackground(AppTheme.SURFACE);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        JLabel name = new JLabel(item.getName() + "   \u20B9" + (int) item.getPrice()
                + "   (" + item.getPrepTimeMinutes() + " min)");
        name.setFont(AppTheme.FONT_BODY);
        name.setForeground(item.isAvailable()
                ? AppTheme.TEXT_PRIMARY : AppTheme.TEXT_SECONDARY);
        row.add(name, BorderLayout.CENTER);

        JCheckBox stock = new JCheckBox("In stock", item.isAvailable());
        stock.setFont(AppTheme.FONT_BODY);
        stock.setForeground(item.isAvailable()
                ? AppTheme.STATUS_READY : AppTheme.STATUS_CANCELLED);
        stock.setOpaque(false);
        stock.addActionListener(e -> toggleAvailability(item, stock.isSelected()));
        row.add(stock, BorderLayout.EAST);
        return row;
    }

    private void toggleAvailability(Item item, boolean nowInStock) {
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() {
                itemDAO.updateAvailability(item.getId(), nowInStock);
                return null;
            }
            @Override protected void done() {
                try {
                    get();
                    statusLabel.setText("\u2713  '" + item.getName() + "' is now "
                            + (nowInStock ? "IN STOCK" : "OUT OF STOCK") + ".");
                    reloadItems();
                } catch (Exception ex) {
                    statusLabel.setText("Update failed: " + root(ex));
                    reloadItems();
                }
            }
        }.execute();
    }

    // ---------------- helpers ----------------
    private JLabel sectionLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(AppTheme.FONT_SUBHEADING);
        l.setForeground(AppTheme.ACCENT_AMBER);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private JTextField addLabelled(JPanel parent, String label) {
        JLabel l = new JLabel(label);
        l.setFont(AppTheme.FONT_BODY);
        l.setForeground(AppTheme.TEXT_SECONDARY);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        parent.add(l);
        parent.add(Box.createVerticalStrut(4));
        JTextField f = new JTextField();
        f.setBackground(AppTheme.SURFACE);
        f.setForeground(AppTheme.TEXT_PRIMARY);
        f.setCaretColor(AppTheme.TEXT_PRIMARY);
        f.setFont(AppTheme.FONT_BODY);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER),
                BorderFactory.createEmptyBorder(7, 8, 7, 8)));
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        f.setAlignmentX(Component.LEFT_ALIGNMENT);
        parent.add(f);
        parent.add(Box.createVerticalStrut(12));
        return f;
    }

    private static String root(Exception e) {
        Throwable c = (e.getCause() != null) ? e.getCause() : e;
        return c.getMessage();
    }
}
