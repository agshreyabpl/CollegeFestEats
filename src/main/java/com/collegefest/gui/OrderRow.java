package com.collegefest.gui;


/**
 * View-model for a single row in the orders tables (both dashboards).
 * Decouples the GUI from the real com.collegefest.models.Order class until
 * that's fully wired in from feature/orders.
 *
 * TODO(Step 3/4 merge): once Order.java is available on this branch, add a
 * factory here — static OrderRow fromOrder(Order o, String vendorName) —
 * that maps its fields (id, studentId, vendorId, itemNames, status,
 * placedAt, etaMinutes) into this shape.
 */
public class OrderRow {
    public final String orderId;
    public final String counterpartyName; // vendor name (student's view) or student name (vendor's view)
    public final String items;
    public final String status;
    public final Integer etaMinutes; // null when not applicable (history rows, vendor view)
    public final String placedAt;

    public OrderRow(String orderId, String counterpartyName, String items, String status,
                     Integer etaMinutes, String placedAt) {
        this.orderId = orderId;
        this.counterpartyName = counterpartyName;
        this.items = items;
        this.status = status;
        this.etaMinutes = etaMinutes;
        this.placedAt = placedAt;
    }
}
