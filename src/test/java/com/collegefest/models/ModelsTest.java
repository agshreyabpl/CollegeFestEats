package com.collegefest.models;

import org.junit.jupiter.api.Test;
import java.util.Date;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/** Model layer: constants, constructor defaults, getter/setter integrity. */
public class ModelsTest {

    @Test
    public void orderStatusConstantsHaveCanonicalValues() {
        assertEquals("PLACED",    Order.STATUS_PLACED);
        assertEquals("PREPARING", Order.STATUS_PREPARING);
        assertEquals("READY",     Order.STATUS_READY);
        assertEquals("SERVED",    Order.STATUS_SERVED);
        assertEquals("CANCELLED", Order.STATUS_CANCELLED);
    }

    @Test
    public void orderThreeArgConstructorSetsPlacedDefaults() {
        Order order = new Order("STU001", "VEN001", List.of("Samosa"));
        assertEquals(Order.STATUS_PLACED, order.getStatus());
        assertNotNull(order.getPlacedAt(), "placedAt must be stamped at construction");
        long age = System.currentTimeMillis() - order.getPlacedAt().getTime();
        assertTrue(age >= 0 && age < 5_000, "placedAt should be 'now'");
    }

    @Test
    public void orderGettersReturnWhatSettersStored() {
        Order order = new Order();
        Date when = new Date(1_720_000_000_000L);
        order.setId("665f00000000000000000abc");
        order.setStudentId("STU009");
        order.setVendorId("VEN007");
        order.setItemNames(List.of("Dosa", "Chai"));
        order.setStatus(Order.STATUS_READY);
        order.setPlacedAt(when);
        order.setEtaMinutes(17);

        assertEquals("665f00000000000000000abc", order.getId());
        assertEquals("STU009", order.getStudentId());
        assertEquals("VEN007", order.getVendorId());
        assertEquals(List.of("Dosa", "Chai"), order.getItemNames());
        assertEquals(Order.STATUS_READY, order.getStatus());
        assertEquals(when, order.getPlacedAt());
        assertEquals(17, order.getEtaMinutes());
    }

    @Test
    public void itemFiveArgConstructorStoresEveryField() {
        Item item = new Item("VEN001", "Peppy Paneer (Small)", 199.0, true, 15);
        assertEquals("VEN001", item.getVendorId());
        assertEquals("Peppy Paneer (Small)", item.getName());
        assertEquals(199.0, item.getPrice(), 0.0001);
        assertTrue(item.isAvailable());
        assertEquals(15, item.getPrepTimeMinutes());
    }

    @Test
    public void itemAvailabilityToggleRoundTrips() {
        Item item = new Item("VEN001", "Garlic Bread", 109.0, true, 12);
        item.setAvailable(false);           // vendor marks OUT OF STOCK
        assertFalse(item.isAvailable());
        item.setAvailable(true);            // ...and brings it back any time
        assertTrue(item.isAvailable());
        item.setId("665f00000000000000000def");
        assertEquals("665f00000000000000000def", item.getId());
    }

    @Test
    public void userConstructorStoresIdentityAndRole() {
        User user = new User("VEN001", "hash", "Rajesh Food Stall", "vendor");
        assertEquals("VEN001", user.getUserId());
        assertEquals("hash", user.getPassword());
        assertEquals("Rajesh Food Stall", user.getName());
        assertEquals("vendor", user.getRole());
    }
}
