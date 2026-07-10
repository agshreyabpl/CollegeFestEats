package com.collegefest.service;

import com.collegefest.models.Order;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Factory pattern — builds a correctly initialised Order object.
 *
 * Centralises all default-value assignment so no caller ever forgets
 * to set status, timestamp, or payment method.
 */
public class OrderFactory {

    /**
     * Builds a new Order with status=PLACED and placedAt=now.
     *
     * @param studentId  ID of the student placing the order
     * @param vendorId   ID of the vendor receiving the order
     * @param itemNames  list of item names chosen by the student
     * @param etaMinutes ETA calculated by EtaEngine
     * @return a ready-to-insert Order object
     */
    public static Order buildOrder(String studentId,
                                   String vendorId,
                                   List<String> itemNames,
                                   int etaMinutes) {
        Order order = new Order();
        order.setStudentId(studentId);
        order.setVendorId(vendorId);
        order.setItemNames(itemNames);
        order.setEtaMinutes(etaMinutes);
        order.setStatus("PLACED");
        order.setPlacedAt(LocalDateTime.now().toString());
        return order;
    }
}
