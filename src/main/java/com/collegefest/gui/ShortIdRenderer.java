package com.collegefest.gui;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Component;

/**
 * Renders the "Order ID" column as the short form (#A1B2C3) while the table
 * MODEL keeps the FULL MongoDB id underneath.
 *
 * Why both: buttons like "Mark Preparing" and "Cancel Order" read the model
 * value to get the exact _id for the database call (full id, always
 * correct), while the person only ever sees the tidy 6-character form —
 * and because AppTheme.shortId() is the single formatter, the student and
 * the vendor see the IDENTICAL id for the same order.
 */
public class ShortIdRenderer extends DefaultTableCellRenderer {

    public ShortIdRenderer() {
        setHorizontalAlignment(SwingConstants.LEFT);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {

        JLabel label = (JLabel) super.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, column);

        String fullId = (value == null) ? "" : value.toString();
        label.setText(AppTheme.shortId(fullId));
        label.setToolTipText(fullId.isEmpty() ? null : fullId); // hover shows the full id
        label.setFont(AppTheme.FONT_TABLE);
        return label;
    }
}
