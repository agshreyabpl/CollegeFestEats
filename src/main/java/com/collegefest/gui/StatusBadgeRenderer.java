package main.java.com.collegefest.gui;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

/**
 * Renders the "Status" column in both dashboards' JTables as a colored,
 * capitalized badge instead of plain text.
 *
 * Colors come from AppTheme.statusColor() so this stays in sync with the
 * palette used everywhere else (pending=yellow, preparing=orange,
 * ready=green, served=grey — per Step 10 of the plan, done early here
 * since both tables needed it anyway).
 */
public class StatusBadgeRenderer extends DefaultTableCellRenderer {

    public StatusBadgeRenderer() {
        setHorizontalAlignment(SwingConstants.CENTER);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {

        JLabel label = (JLabel) super.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, column);

        String status = (value == null) ? "" : value.toString();

        label.setOpaque(true);
        label.setFont(AppTheme.FONT_TABLE_HEAD);
        label.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
        label.setBackground(isSelected ? AppTheme.SURFACE_LIGHT : AppTheme.SURFACE);
        label.setForeground(AppTheme.statusColor(status));
        label.setText(capitalize(status));

        return label;
    }

    private String capitalize(String s) {
        if (s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
