package com.collegefest.gui;

import java.awt.Component;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

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
        label.setFont(new Font("Segoe UI Emoji", Font.BOLD, 12));
        label.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
        label.setBackground(isSelected ? AppTheme.SURFACE_LIGHT : AppTheme.SURFACE);
        label.setForeground(AppTheme.statusColor(status));
        label.setText(statusIcon(status) + " " + capitalize(status));

        return label;
    }

    private String statusIcon(String status) {
        if (status == null) return "";
        switch (status.toLowerCase()) {
            case "pending":   return "⏳";
            case "preparing": return "🍳";
            case "ready":     return "✅";
            case "served":    return "✔";
            case "cancelled": return "❌";
            default:          return "";
        }
    }

    private String capitalize(String s) {
        if (s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
