package com.collegefest.gui;

import org.junit.jupiter.api.Test;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import static org.junit.jupiter.api.Assertions.*;

/**
 * StatusBadgeRenderer combines an emoji icon, a capitalized label, and an
 * AppTheme color per status. Exercised directly via
 * getTableCellRendererComponent() - no window needed, runs headless.
 */
public class StatusBadgeRendererTest {

    private final StatusBadgeRenderer renderer = new StatusBadgeRenderer();
    private final JTable table = new JTable(new DefaultTableModel(1, 1));

    @Test
    public void pendingStatusGetsHourglassIconAndCapitalizedText() {
        JLabel label = (JLabel) renderer.getTableCellRendererComponent(
                table, "pending", false, false, 0, 0);
        assertEquals("\u23F3 Pending", label.getText());
        assertEquals(AppTheme.STATUS_PENDING, label.getForeground());
    }

    @Test
    public void statusMatchingIsCaseInsensitiveForBothIconAndColor() {
        JLabel upper = (JLabel) renderer.getTableCellRendererComponent(
                table, "READY", false, false, 0, 0);
        JLabel mixed = (JLabel) renderer.getTableCellRendererComponent(
                table, "Ready", false, false, 0, 0);
        assertEquals(AppTheme.STATUS_READY, upper.getForeground());
        assertEquals(AppTheme.STATUS_READY, mixed.getForeground());
        // capitalize() only touches the first character, so "READY" stays
        // all-caps after the icon - it does not lowercase the rest.
        assertEquals("\u2705 READY", upper.getText());
        assertEquals("\u2705 Ready", mixed.getText());
    }

    @Test
    public void unknownStatusGetsNoIconButStillRendersSafely() {
        JLabel label = (JLabel) renderer.getTableCellRendererComponent(
                table, "weird_status", false, false, 0, 0);
        assertEquals(" Weird_status", label.getText(), "no icon, just a leading space + capitalized text");
        assertEquals(AppTheme.TEXT_SECONDARY, label.getForeground());
    }

    @Test
    public void nullValueRendersAsEmptyTextWithoutThrowing() {
        JLabel label = (JLabel) renderer.getTableCellRendererComponent(
                table, null, false, false, 0, 0);
        assertEquals(" ", label.getText(), "empty status: no icon, no capitalized text, just the separator space");
        assertEquals(AppTheme.TEXT_SECONDARY, label.getForeground());
    }

    @Test
    public void selectedRowUsesTheSelectionBackgroundColor() {
        JLabel selected = (JLabel) renderer.getTableCellRendererComponent(
                table, "ready", true, false, 0, 0);
        JLabel unselected = (JLabel) renderer.getTableCellRendererComponent(
                table, "ready", false, false, 0, 0);
        assertEquals(AppTheme.SURFACE_LIGHT, selected.getBackground());
        assertEquals(AppTheme.SURFACE, unselected.getBackground());
    }

    @Test
    public void servedStatusUsesACheckmarkNotTheReadyCheckmark() {
        // "ready" and "served" have visually similar checkmarks but are
        // different unicode glyphs - make sure they aren't accidentally
        // swapped or collapsed onto the same icon.
        JLabel ready = (JLabel) renderer.getTableCellRendererComponent(
                table, "ready", false, false, 0, 0);
        JLabel served = (JLabel) renderer.getTableCellRendererComponent(
                table, "served", false, false, 0, 0);
        assertNotEquals(ready.getText(), served.getText());
        assertTrue(served.getText().startsWith("\u2714"));
        assertTrue(ready.getText().startsWith("\u2705"));
    }

    @Test
    public void singleCharacterStatusIsCapitalizedWithoutIndexOutOfBounds() {
        // capitalize()'s substring(1) call on a 1-char string must not throw.
        assertDoesNotThrow(() -> renderer.getTableCellRendererComponent(
                table, "x", false, false, 0, 0));
        JLabel label = (JLabel) renderer.getTableCellRendererComponent(
                table, "x", false, false, 0, 0);
        assertEquals(" X", label.getText());
    }
}
