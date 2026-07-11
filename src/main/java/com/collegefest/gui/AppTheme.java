package com.collegefest.gui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.UIManager;

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
    public static final Color STATUS_CANCELLED  = new Color(0xEF5350);

    // ---- Fonts ----
    public static final Font FONT_HEADING    = new Font("Segoe UI", Font.BOLD, 20);
    public static final Font FONT_SUBHEADING = new Font("Segoe UI Emoji", Font.BOLD, 14);
    public static final Font FONT_BODY       = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_TABLE      = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font FONT_TABLE_HEAD = new Font("Segoe UI", Font.BOLD, 12);
    public static final Font FONT_BUTTON     = new Font("Segoe UI Emoji", Font.BOLD, 14);

    /** Maps an order status string to its display color for JTable cells / badges. */
    public static Color statusColor(String status) {
        if (status == null) return TEXT_SECONDARY;
        switch (status.toLowerCase()) {
            case "pending":   return STATUS_PENDING;
            case "preparing": return STATUS_PREPARING;
            case "ready":     return STATUS_READY;
            case "served":    return STATUS_SERVED;
            case "cancelled": return STATUS_CANCELLED;
            default:          return TEXT_SECONDARY;
        }
    }

    /**
     * Makes JTabbedPane readable on the dark theme (default look-and-feel
     * tab colours are built for light backgrounds): dark tab strip, light
     * text on every tab, dark content area.
     */
    public static void styleTabs(javax.swing.JTabbedPane tabs) {
        javax.swing.UIManager.put("TabbedPane.selected", SURFACE_LIGHT);
        javax.swing.UIManager.put("TabbedPane.contentAreaColor", BACKGROUND_DARK);
        tabs.setBackground(SURFACE);
        tabs.setForeground(TEXT_PRIMARY);
        tabs.setOpaque(true);
        for (int i = 0; i < tabs.getTabCount(); i++) {
            tabs.setBackgroundAt(i, SURFACE);
            tabs.setForegroundAt(i, TEXT_PRIMARY);
        }
    }

    /** Styles a JButton as either the primary amber action or a secondary flat one. */
    public static void styleButton(JButton button, boolean primary) {
        button.setFocusPainted(false);
        button.setFont(FONT_BUTTON);
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

    /**
     * One short, human-friendly form of a MongoDB order id, used EVERYWHERE
     * an order id is shown (student table, vendor table, ETA banner,
     * dialogs). Both dashboards calling this same helper is what guarantees
     * the student and the vendor see the SAME id for the same order.
     */
    public static String shortId(String fullId) {
        if (fullId == null || fullId.isEmpty()) return "—";
        String tail = fullId.length() <= 6 ? fullId : fullId.substring(fullId.length() - 6);
        return "#" + tail.toUpperCase();
    }

    /** Convenience: a JLabel styled as a section title. */
    public static JLabel heading(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FONT_HEADING);
        label.setForeground(TEXT_PRIMARY);
        return label;
    }

    /**
     * Applies dark theme defaults to Swing's built-in dialogs (JOptionPane,
     * tooltips) via UIManager, so warning/info popups match the rest of the
     * app instead of showing up as default light-grey Swing dialogs.
     *
     * MERGE DAY NOTE: call this once, as early as possible — ideally the
     * very first line of main() before any frame is constructed. Right now
     * each dashboard's own main() calls it for standalone testing. Once
     * Person A's real app entry point (com.collegefest.Main, per the
     * package structure) exists, move this call there instead, before
     * LoginFrame is shown, so the login screen's dialogs are themed too.
     */
    public static void applyGlobalDefaults() {
        UIManager.put("OptionPane.background", SURFACE);
        UIManager.put("Panel.background", SURFACE);
        UIManager.put("OptionPane.messageForeground", TEXT_PRIMARY);
        UIManager.put("Button.background", SURFACE_LIGHT);
        UIManager.put("Button.foreground", TEXT_PRIMARY);
        UIManager.put("ToolTip.background", SURFACE_LIGHT);
        UIManager.put("ToolTip.foreground", TEXT_PRIMARY);
    }
}
