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
}
