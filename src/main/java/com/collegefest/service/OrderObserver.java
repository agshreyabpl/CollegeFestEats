package com.collegefest.service;

import com.collegefest.models.Order;

/**
 * Observer pattern interface.
 *
 * Any class that wants live order-status updates must implement this
 * and register itself with OrderService.registerObserver().
 *
 * Implementors: VendorDashboard (Person B), StudentDashboard (Person C).
 */
public interface OrderObserver {

    /**
     * Called automatically by OrderService.notifyObservers()
     * whenever an order's status changes.
     *
     * @param order the updated Order object (contains new status)
     */
    void update(Order order);
}
