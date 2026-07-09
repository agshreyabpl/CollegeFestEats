package main.java.com.collegefest.gui;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.function.Consumer;

/**
 * Modal dialog opened from StudentDashboardFrame's "+ Place New Order" button.
 *
 * DAY 2: real layout, vendor dropdown, multi-select item list, validation
 * (must pick at least one item). Vendor/item lists are still hardcoded.
 *
 * TODO(Step 3): populate the vendor dropdown from UserDAO (all vendors) and
 * reload the item list from ItemDAO.findByVendor(vendorId) whenever the
 * vendor selection changes. On submit, call OrderDAO.insertOrder(...) and
 * EtaEngine.calculateEta(...) instead of just handing the raw selection
 * back to the caller via onSubmit.
 */
public class PlaceOrderDialog extends JDialog {

    private final JComboBox<String> vendorDropdown;
    private final JList<String> itemList;

    public PlaceOrderDialog(Frame owner, Consumer<PlaceOrderResult> onSubmit) {
        super(owner, "Place New Order", true);
        setSize(420, 440);
        setLocationRelativeTo(owner);
        setResizable(false);

        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(AppTheme.BACKGROUND_DARK);
        root.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        setContentPane(root);

        JLabel title = AppTheme.heading("New Order");
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(title);
        root.add(Box.createVerticalStrut(16));

        root.add(sectionLabel("Vendor"));
        // TODO(Step 3): replace with vendors loaded from UserDAO
        vendorDropdown = new JComboBox<>(new String[]{
                "Rajesh Food Stall", "Campus Cafe", "Chaat Corner"
        });
        styleCombo(vendorDropdown);
        root.add(vendorDropdown);
        root.add(Box.createVerticalStrut(16));

        root.add(sectionLabel("Items (select one or more)"));
        // TODO(Step 3): replace with ItemDAO.findByVendor(vendorId) results,
        // reloaded whenever vendorDropdown's selection changes
        DefaultListModel<String> itemModel = new DefaultListModel<>();
        itemModel.addElement("Veg Puff");
        itemModel.addElement("Samosa Chaat");
        itemModel.addElement("Cold Coffee");
        itemModel.addElement("Masala Dosa");

        itemList = new JList<>(itemModel);
        itemList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        itemList.setBackground(AppTheme.SURFACE);
        itemList.setForeground(AppTheme.TEXT_PRIMARY);
        itemList.setSelectionBackground(AppTheme.ACCENT_AMBER);
        itemList.setSelectionForeground(AppTheme.BACKGROUND_DARK);
        itemList.setFont(AppTheme.FONT_BODY);

        JScrollPane itemScroll = new JScrollPane(itemList);
        itemScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        itemScroll.setPreferredSize(new Dimension(360, 140));
        itemScroll.setMaximumSize(new Dimension(360, 140));
        itemScroll.setBorder(BorderFactory.createLineBorder(AppTheme.BORDER));
        root.add(itemScroll);
        root.add(Box.createVerticalStrut(12));

        JLabel errorLabel = new JLabel(" ");
        errorLabel.setForeground(new Color(0xEF5350));
        errorLabel.setFont(AppTheme.FONT_BODY);
        errorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(errorLabel);
        root.add(Box.createVerticalStrut(8));

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonRow.setOpaque(false);
        buttonRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton cancelButton = new JButton("Cancel");
        AppTheme.styleButton(cancelButton, false);
        cancelButton.addActionListener(e -> dispose());

        JButton submitButton = new JButton("Place Order");
        AppTheme.styleButton(submitButton, true);
        submitButton.addActionListener(e -> {
            List<String> selectedItems = itemList.getSelectedValuesList();
            if (selectedItems.isEmpty()) {
                errorLabel.setText("Select at least one item.");
                return;
            }
            String vendor = (String) vendorDropdown.getSelectedItem();
            onSubmit.accept(new PlaceOrderResult(vendor, selectedItems));
            dispose();
        });

        buttonRow.add(cancelButton);
        buttonRow.add(submitButton);
        root.add(buttonRow);
    }

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
        combo.setMaximumSize(new Dimension(360, 32));
        combo.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    /** What the dialog collected. Step 3 will turn this into a real Order + OrderDAO.insertOrder() call. */
    public static class PlaceOrderResult {
        public final String vendorName;
        public final List<String> itemNames;

        public PlaceOrderResult(String vendorName, List<String> itemNames) {
            this.vendorName = vendorName;
            this.itemNames = itemNames;
        }
    }
}
