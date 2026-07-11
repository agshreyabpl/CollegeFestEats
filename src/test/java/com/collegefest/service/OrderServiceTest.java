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

    // ---------------- extra edge cases ----------------

    @Test
    public void sameStatusIsNotAValidTransition() {
        // A no-op "move" (e.g. double-clicking "Mark Preparing") must be
        // rejected just like a real backwards move would be.
        assertFalse(OrderService.isValidTransition(Order.STATUS_PLACED, Order.STATUS_PLACED));
        assertFalse(OrderService.isValidTransition(Order.STATUS_READY, Order.STATUS_READY));
    }

    @Test
    public void nullNextStatusIsAlwaysRejected() {
        assertFalse(OrderService.isValidTransition(Order.STATUS_PLACED, null));
        assertFalse(OrderService.isValidTransition(null, null));
    }

    @Test
    public void readyMayNotJumpStraightToCancelled() {
        // Cancellation is only allowed straight out of PLACED.
        assertFalse(OrderService.isValidTransition(Order.STATUS_READY, Order.STATUS_CANCELLED));
        assertFalse(OrderService.isValidTransition(Order.STATUS_SERVED, Order.STATUS_CANCELLED));
    }

    @Test
    public void unknownStatusStringIsNeverAValidStartingPoint() {
        assertFalse(OrderService.isValidTransition("BOGUS_STATUS", Order.STATUS_PREPARING));
    }

    @Test
    public void removingASessionThatWasNeverRegisteredIsANoOp() {
        // Must not throw even though "GHOST" was never put() into the map.
        assertDoesNotThrow(() -> OrderService.removeSession("GHOST_USER_ID"));
        assertNull(OrderService.getActiveSessions().get("GHOST_USER_ID"));
    }

    @Test
    public void reRegisteringTheSameUserIdOverwritesThePreviousSession() {
        User first = new User("STU_TEST_2", "hash1", "First Login", "student");
        User second = new User("STU_TEST_2", "hash2", "Second Login", "student");

        OrderService.registerSession("STU_TEST_2", first);
        OrderService.registerSession("STU_TEST_2", second);

        assertEquals(second, OrderService.getActiveSessions().get("STU_TEST_2"),
                "logging in again (e.g. after a stale session) should replace, not duplicate");

        OrderService.removeSession("STU_TEST_2"); // cleanup so other tests aren't affected
    }
}
