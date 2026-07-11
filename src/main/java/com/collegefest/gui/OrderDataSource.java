package com.collegefest.gui;

import java.util.List;

/**
 * Everything the GUI needs to read and write orders, behind one interface.
 *
 * Right now the only implementation is SimulatedOrderDataSource (in-memory,
 * no MongoDB — see that class for why). Once feature/orders' real OrderDAO
 * and EtaEngine are merged in, write a Mongo-backed implementation of this
 * same interface and swap the one line in each dashboard's constructor
 * that creates the data source. No other GUI code should need to change.
 */
public interface OrderDataSource {

    List<OrderRow> fetchActiveOrdersForStudent(String studentId);

    List<OrderRow> fetchHistoryForStudent(String studentId);

    List<OrderRow> fetchIncomingOrdersForVendor(String vendorKey);

    /** Vendor marks an order's status. No-op if orderId isn't found. */
    void updateOrderStatus(String orderId, String newStatus);

    /** Student places a new order. Returns the generated order ID. */
    String placeOrder(String studentId, String studentName, String vendorName, List<String> items);
}
