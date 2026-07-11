package com.collegefest.gui;

import org.junit.jupiter.api.Test;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ShortIdRenderer wraps AppTheme.shortId() for the table cell; these tests
 * drive getTableCellRendererComponent() directly (no window needs to be
 * shown - JTable/JLabel work fine headless) to check the visible text and
 * the full-id tooltip it keeps around for hover.
 */
public class ShortIdRendererTest {

    private final ShortIdRenderer renderer = new ShortIdRenderer();
    private final JTable table = new JTable(new DefaultTableModel(1, 1));

    @Test
    public void fullMongoIdIsShortenedToLastSixCharsUppercased() {
        JLabel label = (JLabel) renderer.getTableCellRendererComponent(
                table, "665f00000000000000004f2a1b", false, false, 0, 0);
        // exact format: "#" + last 6 chars, uppercased
        assertEquals("#4F2A1B", label.getText());
        assertEquals("665f00000000000000004f2a1b", label.getToolTipText(),
                "hovering must reveal the FULL id for support/debugging");
    }

    @Test
    public void nullValueRendersAsEmDashWithNoTooltip() {
        JLabel label = (JLabel) renderer.getTableCellRendererComponent(
                table, null, false, false, 0, 0);
        assertEquals("\u2014", label.getText());
        assertNull(label.getToolTipText(), "no id means nothing useful to show on hover");
    }

    @Test
    public void emptyStringValueRendersAsEmDashWithNoTooltip() {
        JLabel label = (JLabel) renderer.getTableCellRendererComponent(
                table, "", false, false, 0, 0);
        assertEquals("\u2014", label.getText());
        assertNull(label.getToolTipText());
    }

    @Test
    public void idShorterThanSixCharsIsKeptWhole() {
        JLabel label = (JLabel) renderer.getTableCellRendererComponent(
                table, "abc", false, false, 0, 0);
        assertEquals("#ABC", label.getText());
        assertEquals("abc", label.getToolTipText());
    }

    @Test
    public void nonStringValueIsCoercedViaToString() {
        JLabel label = (JLabel) renderer.getTableCellRendererComponent(
                table, 123456, false, false, 0, 0);
        assertEquals("#123456", label.getText());
        assertEquals("123456", label.getToolTipText());
    }
}
