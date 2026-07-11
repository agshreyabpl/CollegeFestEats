package com.collegefest.gui;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** AppTheme's pure helpers: status colours and the shared short-id format. */
public class ThemeTest {

    @Test
    public void everyStatusHasItsOwnColourCaseInsensitive() {
        assertEquals(AppTheme.STATUS_PENDING,   AppTheme.statusColor("pending"));
        assertEquals(AppTheme.STATUS_PREPARING, AppTheme.statusColor("PREPARING"));
        assertEquals(AppTheme.STATUS_READY,     AppTheme.statusColor("Ready"));
        assertEquals(AppTheme.STATUS_SERVED,    AppTheme.statusColor("served"));
        assertEquals(AppTheme.STATUS_CANCELLED, AppTheme.statusColor("cancelled"));
    }

    @Test
    public void unknownOrNullStatusFallsBackToSecondaryText() {
        assertEquals(AppTheme.TEXT_SECONDARY, AppTheme.statusColor("weird"));
        assertEquals(AppTheme.TEXT_SECONDARY, AppTheme.statusColor(null));
    }

    @Test
    public void shortIdIsTheSameSixCharFormEverywhere() {
        // This one format is what BOTH dashboards display — same order,
        // same id, on the student's screen and the vendor's screen.
        assertEquals("#00ABC1", AppTheme.shortId("665f000000000000000abc1".substring(0, 17) + "00abc1"));
        assertEquals("#4F2A1B", AppTheme.shortId("665f0000000000000000000000004f2a1b"));
        assertEquals("#ABC",    AppTheme.shortId("abc"));   // shorter than 6 -> kept whole
        assertEquals("—",       AppTheme.shortId(null));
        assertEquals("—",       AppTheme.shortId(""));
    }
}
