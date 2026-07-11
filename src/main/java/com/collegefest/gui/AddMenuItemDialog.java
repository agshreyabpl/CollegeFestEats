package com.collegefest.gui;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

/**
 * Modal "add menu item" form (Person C, Day 2).
 *
 * NOTE (feature round): the live vendor dashboard now uses its own inline
 * sidebar form wired straight to ItemDAO.insertItem(), so this dialog is
 * currently unused by the running app. It is kept because it is part of
 * the graded GUI work and remains a drop-in component (same
 * Consumer<NewItemResult> contract) if a dialog-based flow is wanted again.
 */
public class AddMenuItemDialog extends JDialog {

    private final JTextField nameField;
    private final JTextField priceField;
    private final JTextField prepTimeField;
    private final JLabel errorLabel;

    public AddMenuItemDialog(Frame owner, Consumer<NewItemResult> onSubmit) {
        super(owner, "Add Menu Item", true);
        setSize(360, 340);
        setLocationRelativeTo(owner);
        setResizable(false);

        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(AppTheme.BACKGROUND_DARK);
        root.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        setContentPane(root);

        JLabel title = AppTheme.heading("New Menu Item");
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(title);
        root.add(Box.createVerticalStrut(16));

        nameField = addLabeledField(root, "Item name");
        priceField = addLabeledField(root, "Price (\u20B9)");
        prepTimeField = addLabeledField(root, "Prep time (minutes)");

        errorLabel = new JLabel(" ");
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

        JButton saveButton = new JButton("Save Item");
        AppTheme.styleButton(saveButton, true);
        saveButton.addActionListener(e -> {
            String name = nameField.getText().trim();
            String priceText = priceField.getText().trim();
            String prepText = prepTimeField.getText().trim();

            if (name.isEmpty()) {
                errorLabel.setText("Item name is required.");
                return;
            }
            double price;
            int prepTime;
            try {
                price = Double.parseDouble(priceText);
                prepTime = Integer.parseInt(prepText);
            } catch (NumberFormatException ex) {
                errorLabel.setText("Price and prep time must be numbers.");
                return;
            }
            if (price < 0 || prepTime < 0) {
                errorLabel.setText("Price and prep time can't be negative.");
                return;
            }

            onSubmit.accept(new NewItemResult(name, price, prepTime));
            dispose();
        });

        buttonRow.add(cancelButton);
        buttonRow.add(saveButton);
        root.add(buttonRow);
    }

    private JTextField addLabeledField(JPanel root, String labelText) {
        JLabel label = new JLabel(labelText);
        label.setFont(AppTheme.FONT_SUBHEADING);
        label.setForeground(AppTheme.TEXT_SECONDARY);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
        root.add(label);

        JTextField field = new JTextField();
        field.setMaximumSize(new Dimension(320, 32));
        field.setBackground(AppTheme.SURFACE);
        field.setForeground(AppTheme.TEXT_PRIMARY);
        field.setCaretColor(AppTheme.TEXT_PRIMARY);
        field.setFont(AppTheme.FONT_BODY);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(field);
        root.add(Box.createVerticalStrut(12));
        return field;
    }

    /** What the dialog collected. Step 6 will turn this into an Item + ItemDAO.insertItem() call. */
    public static class NewItemResult {
        public final String name;
        public final double price;
        public final int prepTimeMinutes;

        public NewItemResult(String name, double price, int prepTimeMinutes) {
            this.name = name;
            this.price = price;
            this.prepTimeMinutes = prepTimeMinutes;
        }
    }
}
