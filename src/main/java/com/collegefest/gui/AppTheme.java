package com.collegefest.gui;

import javax.swing.*;
import java.awt.*;

/**
 * Central place for colors, fonts, and small style helpers so every screen
 * (Login, StudentDashboard, VendorDashboard) looks consistent.
 *
 * Palette: dark charcoal background, warm amber accent for primary actions,
 * muted greys for secondary surfaces and text.
 */
public class AppTheme {

    // ---- Core palette ----
    public static final Color BACKGROUND_DARK   = new Color(0x1E1E1E);
    public static final Color SURFACE           = new Color(0x2A2A2A);
    public static final Color SURFACE_LIGHT     = new Color(0x333333);
    public static final Color BORDER            = new Color(0x3D3D3D);

    public static final Color ACCENT_AMBER      = new Color(0xFFA726);
    public static final Color ACCENT_AMBER_DARK = new Color(0xE0891A);

    public static final Color TEXT_PRIMARY      = new Color(0xF5F5F5);
    public static final Color TEXT_SECONDARY    = new Color(0xAAAAAA);

    // ---- Status colors (used by both dashboards' JTables) ----
    public static final Color STATUS_PENDING    = new Color(0xFFD54F);
    public static final Color STATUS_PREPARING  = new Color(0xFF9800);
    public static final Color STATUS_READY      = new Color(0x66BB6A);
    public static final Color STATUS_SERVED     = new Color(0x8A8A8A);

    // ---- Fonts ----
    public static final Font FONT_HEADING    = new Font("Segoe UI", Font.BOLD, 20);
    public static final Font FONT_SUBHEADING = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font FONT_BODY       = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_TABLE      = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font FONT_TABLE_HEAD = new Font("Segoe UI", Font.BOLD, 12);

    /** Maps an order status string to its display color for JTable cells / badges. */
    public static Color statusColor(String status) {
        if (status == null) return TEXT_SECONDARY;
        switch (status.toLowerCase()) {
            case "pending":   return STATUS_PENDING;
            case "preparing": return STATUS_PREPARING;
            case "ready":     return STATUS_READY;
            case "served":    return STATUS_SERVED;
            default:          return TEXT_SECONDARY;
        }
    }

    /** Styles a JButton as either the primary amber action or a secondary flat one. */
    public static void styleButton(JButton button, boolean primary) {
        button.setFocusPainted(false);
        button.setFont(FONT_SUBHEADING);
        button.setForeground(primary ? BACKGROUND_DARK : TEXT_PRIMARY);
        button.setBackground(primary ? ACCENT_AMBER : SURFACE_LIGHT);
        button.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setOpaque(true);
        button.setBorderPainted(false);
    }

    /** Applies the dark theme to a JTable (rows, header, selection, grid lines). */
    public static void styleTable(JTable table) {
        table.setBackground(SURFACE);
        table.setForeground(TEXT_PRIMARY);
        table.setFont(FONT_TABLE);
        table.setRowHeight(28);
        table.setGridColor(BORDER);
        table.setSelectionBackground(SURFACE_LIGHT);
        table.setSelectionForeground(ACCENT_AMBER);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 4));
        table.setFillsViewportHeight(true);

        table.getTableHeader().setBackground(BACKGROUND_DARK);
        table.getTableHeader().setForeground(TEXT_SECONDARY);
        table.getTableHeader().setFont(FONT_TABLE_HEAD);
        table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER));
    }

    /** Convenience: a JLabel styled as a section title. */
    public static JLabel heading(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FONT_HEADING);
        label.setForeground(TEXT_PRIMARY);
        return label;
    }
}
