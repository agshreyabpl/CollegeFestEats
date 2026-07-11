package com.collegefest.gui;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * In-memory stand-in for OrderDAO + EtaEngine, so live SwingWorker polling
 * can be built and demoed today without waiting on the real MongoDB-backed
 * classes from feature/orders.
 *
 * Singleton — same pattern as the project's Database class — so the
 * student and vendor screens see the same simulated orders. A status update from the vendor screen becomes visible on the
 * student screen on its next poll, exactly like the real thing will behave.
 *
 * ETA is computed the way the spec describes EtaEngine working:
 *   ETA = baseTimePerOrder * queuePositionAtVendor + bufferTime
 * minus minutes already elapsed since the order was placed, floored at 0.
 * This is a placeholder for Step 2's real EtaEngine, not a replacement —
 * once that class exists, its output should be used instead of
 * computeEtaMinutes() below.
 *
 * DONE(merge): MongoOrderDataSource is that real implementation and is
 * what both dashboards use. This class stays for offline demos and for
 * the unit tests, which must not open a database connection.
 */
public class SimulatedOrderDataSource implements OrderDataSource {

    private static final SimulatedOrderDataSource INSTANCE = new SimulatedOrderDataSource();

    public static SimulatedOrderDataSource getInstance() {
        return INSTANCE;
    }

    private static final int BUFFER_MINUTES = 2;

    private final Map<String, SimOrder> orders = new ConcurrentHashMap<>();
    private final AtomicInteger idCounter = new AtomicInteger(1000);

    private SimulatedOrderDataSource() {
        seedDemoData();
    }

    @Override
    public List<OrderRow> fetchActiveOrdersForStudent(String studentId) {
        return orders.values().stream()
                .filter(o -> o.studentId.equals(studentId) && !o.status.equals("served"))
                .sorted(Comparator.comparingLong(o -> o.placedAtMillis))
                .map(this::toRowWithEta)
                .collect(Collectors.toList());
    }

    @Override
    public List<OrderRow> fetchHistoryForStudent(String studentId) {
        return orders.values().stream()
                .filter(o -> o.studentId.equals(studentId) && o.status.equals("served"))
                .sorted(Comparator.comparingLong((SimOrder o) -> o.placedAtMillis).reversed())
                .map(this::toRowWithEta)
                .collect(Collectors.toList());
    }

    @Override
    public List<OrderRow> fetchIncomingOrdersForVendor(String vendorKey) {
        return orders.values().stream()
                .filter(o -> o.vendorKey.equals(vendorKey))
                .sorted(Comparator.comparingLong(o -> o.placedAtMillis))
                .map(o -> new OrderRow(o.orderId, o.studentName, o.items, o.status, null, formatTime(o.placedAtMillis)))
                .collect(Collectors.toList());
    }

    @Override
    public void updateOrderStatus(String orderId, String newStatus) {
        SimOrder o = orders.get(orderId);
        if (o != null) {
            o.status = newStatus;
        }
    }

    @Override
    public String placeOrder(String studentId, String studentName, String vendorName, List<String> items) {
        String orderId = "ORD-" + idCounter.incrementAndGet();
        SimOrder o = new SimOrder();
        o.orderId = orderId;
        o.studentId = studentId;
        o.studentName = studentName;
        o.vendorKey = vendorName; // TODO: swap for a real vendorId once vendor lookup via UserDAO exists
        o.items = String.join(", ", items);
        o.status = "pending";
        o.placedAtMillis = System.currentTimeMillis();
        o.basePrepMinutes = 5; // TODO(Step 2): should come from Item.prepTimeMinutes per item, averaged
        orders.put(orderId, o);
        return orderId;
    }

    // ---------------------------------------------------------------
    private OrderRow toRowWithEta(SimOrder o) {
        Integer eta = o.status.equals("served") ? null : computeEtaMinutes(o);
        return new OrderRow(o.orderId, o.vendorKey, o.items, o.status, eta, formatTime(o.placedAtMillis));
    }

    private int computeEtaMinutes(SimOrder o) {
        int queuePosition = (int) orders.values().stream()
                .filter(other -> other.vendorKey.equals(o.vendorKey))
                .filter(other -> !other.status.equals("served"))
                .filter(other -> other.placedAtMillis <= o.placedAtMillis)
                .count();
        int rawEta = o.basePrepMinutes * queuePosition + BUFFER_MINUTES;
        long elapsedMinutes = (System.currentTimeMillis() - o.placedAtMillis) / 60000;
        int remaining = (int) (rawEta - elapsedMinutes);
        return Math.max(remaining, 0);
    }

    private String formatTime(long millis) {
        return new java.text.SimpleDateFormat("h:mm a").format(new Date(millis));
    }

    private void seedDemoData() {
        long now = System.currentTimeMillis();
        addSeed("STU001", "Shreya Agrawal", "Rajesh Food Stall", "Veg Puff, Cold Coffee", "preparing", now - 3 * 60_000L, 4);
        addSeed("STU001", "Shreya Agrawal", "Rajesh Food Stall", "Samosa Chaat", "pending", now - 1 * 60_000L, 5);
        addSeed("STU001", "Shreya Agrawal", "Campus Cafe", "Masala Dosa", "served", now - 90 * 60_000L, 6);
        addSeed("STU002", "Aman Verma", "Rajesh Food Stall", "Cold Coffee", "ready", now - 6 * 60_000L, 3);
    }

    private void addSeed(String studentId, String studentName, String vendorName, String items,
                          String status, long placedAtMillis, int basePrepMinutes) {
        String orderId = "ORD-" + idCounter.incrementAndGet();
        SimOrder o = new SimOrder();
        o.orderId = orderId;
        o.studentId = studentId;
        o.studentName = studentName;
        o.vendorKey = vendorName;
        o.items = items;
        o.status = status;
        o.placedAtMillis = placedAtMillis;
        o.basePrepMinutes = basePrepMinutes;
        orders.put(orderId, o);
    }

    /** Internal mutable record — not exposed outside this class. */
    private static class SimOrder {
        String orderId;
        String studentId;
        String studentName;
        String vendorKey;
        String items;
        volatile String status;
        long placedAtMillis;
        int basePrepMinutes;
    }
}
