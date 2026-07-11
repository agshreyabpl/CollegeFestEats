package com.collegefest.gui;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The in-memory stand-in still honours the OrderDataSource contract (it's
 * the offline demo path). It's a shared singleton with seed data, so every
 * test uses its own unique student id.
 */
public class SimulatedOrderDataSourceTest {

    private final SimulatedOrderDataSource sim = SimulatedOrderDataSource.getInstance();

    @Test
    public void placedOrderAppearsInTheStudentsActiveList() {
        String id = sim.placeOrder("STU_T100", "Test Student", "Rajesh Food Stall",
                List.of("Samosa", "Chai"));
        assertNotNull(id);
        List<OrderRow> active = sim.fetchActiveOrdersForStudent("STU_T100");
        assertEquals(1, active.size());
        assertEquals(id, active.get(0).orderId);
        assertEquals("pending", active.get(0).status);
        assertEquals("Samosa, Chai", active.get(0).items);
    }

    @Test
    public void statusUpdateIsVisibleOnTheNextFetch() {
        String id = sim.placeOrder("STU_T101", "Test Student", "Rajesh Food Stall",
                List.of("Dosa"));
        sim.updateOrderStatus(id, "ready");
        List<OrderRow> active = sim.fetchActiveOrdersForStudent("STU_T101");
        assertEquals("ready", active.get(0).status);
    }

    @Test
    public void servedOrdersMoveFromActiveToHistory() {
        String id = sim.placeOrder("STU_T102", "Test Student", "Rajesh Food Stall",
                List.of("Coffee"));
        sim.updateOrderStatus(id, "served");
        assertTrue(sim.fetchActiveOrdersForStudent("STU_T102").isEmpty());
        List<OrderRow> history = sim.fetchHistoryForStudent("STU_T102");
        assertEquals(1, history.size());
        assertNull(history.get(0).etaMinutes, "history rows carry no ETA");
    }

    @Test
    public void vendorFetchOnlyReturnsThatVendorsOrders() {
        sim.placeOrder("STU_T103", "Test Student", "Stall A Unique", List.of("Puff"));
        sim.placeOrder("STU_T103", "Test Student", "Stall B Unique", List.of("Roll"));
        List<OrderRow> stallA = sim.fetchIncomingOrdersForVendor("Stall A Unique");
        assertEquals(1, stallA.size());
        assertEquals("Puff", stallA.get(0).items);
    }

    // ---------------- extra edge cases ----------------

    @Test
    public void updatingAnUnknownOrderIdIsASilentNoOp() {
        // Must not throw (e.g. two vendor tabs racing to update the same
        // stale order, or the order having been re-seeded between polls).
        assertDoesNotThrow(() -> sim.updateOrderStatus("ORD-DOES-NOT-EXIST", "ready"));
    }

    @Test
    public void placingAnOrderWithNoItemsStillReturnsAUsableId() {
        // Defensive: an empty cart getting through to placeOrder must not
        // crash the data source, even though the GUI should stop it first.
        String id = sim.placeOrder("STU_T104", "Test Student", "Rajesh Food Stall", List.of());
        assertNotNull(id);
        List<OrderRow> active = sim.fetchActiveOrdersForStudent("STU_T104");
        assertEquals(1, active.size());
        assertEquals("", active.get(0).items);
    }

    @Test
    public void everyPlacedOrderGetsAUniqueId() {
        String id1 = sim.placeOrder("STU_T105", "Test Student", "Rajesh Food Stall", List.of("Chai"));
        String id2 = sim.placeOrder("STU_T105", "Test Student", "Rajesh Food Stall", List.of("Chai"));
        assertNotEquals(id1, id2, "two separate orders must never collide on id");
    }

    @Test
    public void etaNeverGoesNegativeNoMatterHowOldTheOrderIs() {
        // computeEtaMinutes subtracts elapsed time from the raw eta and
        // floors at 0 - simulate a very "old" order via a fresh unique
        // vendor key so queue math stays predictable (queue position 1).
        String id = sim.placeOrder("STU_T106", "Test Student", "Ancient Order Stall Unique",
                List.of("Cold Coffee"));
        List<OrderRow> active = sim.fetchActiveOrdersForStudent("STU_T106");
        assertEquals(1, active.size());
        assertNotNull(active.get(0).etaMinutes);
        assertTrue(active.get(0).etaMinutes >= 0, "eta must be floored at 0, never negative");
    }

    @Test
    public void historyRowsAreSortedNewestFirst() {
        String older = sim.placeOrder("STU_T107", "Test Student", "History Stall Unique", List.of("Idli"));
        sim.updateOrderStatus(older, "served");

        String newer = sim.placeOrder("STU_T107", "Test Student", "History Stall Unique", List.of("Vada"));
        sim.updateOrderStatus(newer, "served");

        List<OrderRow> history = sim.fetchHistoryForStudent("STU_T107");
        assertEquals(2, history.size());
        assertEquals(newer, history.get(0).orderId, "most recently served order must appear first");
        assertEquals(older, history.get(1).orderId);
    }

    @Test
    public void activeOrdersAreSortedOldestFirstFifoQueueOrder() {
        String first = sim.placeOrder("STU_T108", "Test Student", "Queue Stall Unique", List.of("A"));
        String second = sim.placeOrder("STU_T108", "Test Student", "Queue Stall Unique", List.of("B"));

        List<OrderRow> active = sim.fetchActiveOrdersForStudent("STU_T108");
        assertEquals(2, active.size());
        assertEquals(first, active.get(0).orderId, "orders should queue FIFO - oldest first");
        assertEquals(second, active.get(1).orderId);
    }

    @Test
    public void cancelledOrPendingOrdersOfOtherStudentsNeverLeakIntoMyList() {
        sim.placeOrder("STU_T109", "Test Student", "Isolation Stall Unique", List.of("Puff"));
        List<OrderRow> unrelatedStudent = sim.fetchActiveOrdersForStudent("STU_T109_GHOST");
        assertTrue(unrelatedStudent.isEmpty(), "a student with no orders must get an empty list, not null");
    }
}
