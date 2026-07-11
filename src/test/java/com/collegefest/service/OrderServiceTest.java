package com.collegefest.service;

import com.collegefest.models.Order;
import com.collegefest.models.User;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The forward-only status rule and the session map — both static, so no
 * database connection is opened by these tests.
 */
public class OrderServiceTest {

    @Test
    public void placedMayMoveToPreparing() {
        assertTrue(OrderService.isValidTransition(Order.STATUS_PLACED, Order.STATUS_PREPARING));
    }

    @Test
    public void placedMayBeCancelledButPreparingMayNot() {
        assertTrue(OrderService.isValidTransition(Order.STATUS_PLACED, Order.STATUS_CANCELLED));
        assertFalse(OrderService.isValidTransition(Order.STATUS_PREPARING, Order.STATUS_CANCELLED),
                "no take-backs once cooking started");
    }

    @Test
    public void happyPathFlowsForwardOnly() {
        assertTrue(OrderService.isValidTransition(Order.STATUS_PREPARING, Order.STATUS_READY));
        assertTrue(OrderService.isValidTransition(Order.STATUS_READY, Order.STATUS_SERVED));
    }

    @Test
    public void backwardsSkippingAndTerminalMovesAreRejected() {
        assertFalse(OrderService.isValidTransition(Order.STATUS_SERVED, Order.STATUS_PREPARING));
        assertFalse(OrderService.isValidTransition(Order.STATUS_PLACED, Order.STATUS_READY),
                "may not skip PREPARING");
        assertFalse(OrderService.isValidTransition(Order.STATUS_CANCELLED, Order.STATUS_PREPARING));
        assertFalse(OrderService.isValidTransition(null, Order.STATUS_PREPARING));
    }

    @Test
    public void sessionMapRegistersAndRemovesUsers() {
        User udita = new User("STU_TEST_1", "hash", "Udita", "student");
        OrderService.registerSession("STU_TEST_1", udita);
        assertEquals(udita, OrderService.getActiveSessions().get("STU_TEST_1"));

        OrderService.removeSession("STU_TEST_1");
        assertNull(OrderService.getActiveSessions().get("STU_TEST_1"));
    }
}
