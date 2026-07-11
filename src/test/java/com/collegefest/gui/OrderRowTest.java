package com.collegefest.gui;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** OrderRow is the GUI's view-model — a dumb, immutable field carrier. */
public class OrderRowTest {

    @Test
    public void constructorMapsEveryFieldStraightThrough() {
        OrderRow row = new OrderRow("665f00000000000000000abc", "Dominos",
                "Peppy Paneer (Small), Garlic Bread", "pending", 14, "3:01 AM");
        assertEquals("665f00000000000000000abc", row.orderId);
        assertEquals("Dominos", row.counterpartyName);
        assertEquals("Peppy Paneer (Small), Garlic Bread", row.items);
        assertEquals("pending", row.status);
        assertEquals(Integer.valueOf(14), row.etaMinutes);
        assertEquals("3:01 AM", row.placedAt);
    }
}
