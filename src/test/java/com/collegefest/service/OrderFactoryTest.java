package com.collegefest.service;

import com.collegefest.models.Order;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/** Factory pattern: every order is born correctly initialised. */
public class OrderFactoryTest {

    @Test
    public void factoryOrdersStartAsPlaced() {
        Order order = OrderFactory.buildOrder("STU001", "VEN001", List.of("Samosa"), 9);
        assertEquals(Order.STATUS_PLACED, order.getStatus());
    }

    @Test
    public void factoryStampsPlacedAtWithNow() {
        Order order = OrderFactory.buildOrder("STU001", "VEN001", List.of("Samosa"), 9);
        assertNotNull(order.getPlacedAt());
        long age = System.currentTimeMillis() - order.getPlacedAt().getTime();
        assertTrue(age >= 0 && age < 5_000);
    }

    @Test
    public void factoryCopiesEveryArgumentIntoTheOrder() {
        List<String> items = List.of("Peppy Paneer (Small)", "Garlic Bread");
        Order order = OrderFactory.buildOrder("STU042", "VEN007", items, 23);
        assertEquals("STU042", order.getStudentId());
        assertEquals("VEN007", order.getVendorId());
        assertEquals(items, order.getItemNames());
        assertEquals(23, order.getEtaMinutes());
    }
}
