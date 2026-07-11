package com.collegefest.gui;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.collegefest.db.EtaEngine;
import com.collegefest.db.ItemDAO;
import com.collegefest.db.UserDAO;
import com.collegefest.models.Item;
import com.collegefest.models.Order;
import com.collegefest.models.User;
import com.collegefest.service.OrderService;

/**
 * THE INTEGRATION BRIDGE (written during the team merge).
 *
 * Person C designed the GUI against the OrderDataSource interface with a
 * fake in-memory implementation (SimulatedOrderDataSource). This class is
 * the REAL implementation she planned for: same interface, but backed by
 * MongoDB through Person B's OrderService and Person A's DAOs/EtaEngine.
 * Swapping one line in each dashboard was all the GUI needed - exactly as
 * her design intended.
 *
 * It also translates between two conventions the sub-teams used:
 *   DATABASE (Person B):  "PLACED" / "PREPARING" / "READY" / "SERVED"
 *   GUI     (Person C):  "pending" / "preparing" / "ready" / "served"
 * Rows going OUT to the GUI get lowercase; status updates coming IN from
 * the GUI get mapped back to the uppercase database values.
 *
 * Threading note: the fetch* methods run inside OrderPoller's SwingWorker
 * (background thread), so it is safe for them to hit MongoDB. Nothing in
 * this class may be called expecting instant answers on the EDT except
 * cachedVendorNames(), which only reads an in-memory cache.
 */
public class MongoOrderDataSource implements OrderDataSource {

    // ---- Singleton (one shared cache + one OrderService) ----
    private static MongoOrderDataSource instance;

    public static synchronized MongoOrderDataSource getInstance() {
        if (instance == null) {
            instance = new MongoOrderDataSource();
        }
        return instance;
    }

    private final OrderService orderService = OrderService.getInstance();
    private final UserDAO userDAO = new UserDAO();

    /** vendorId -> display name and the reverse, refreshed on background fetches. */
    private final Map<String, String> vendorNameById = new ConcurrentHashMap<>();
    private final Map<String, String> vendorIdByName = new ConcurrentHashMap<>();
    /** studentId -> name cache so the vendor view can show names, not IDs. */
    private final Map<String, String> studentNameById = new ConcurrentHashMap<>();

    /** SimpleDateFormat is not thread-safe and three pollers run at once — always format through formatTime(). */
    private static final SimpleDateFormat TIME_FMT = new SimpleDateFormat("h:mm a");

    private static synchronized String formatTime(java.util.Date date) {
        return TIME_FMT.format(date);
    }

    private MongoOrderDataSource() { }

    // ================================================================
    // OrderDataSource implementation
    // ================================================================

    @Override
    public List<OrderRow> fetchActiveOrdersForStudent(String studentId) {
        refreshVendorCache();   // background thread - safe to hit the DB
        List<OrderRow> rows = new ArrayList<>();
        for (Order o : orderService.getOrdersForStudent(studentId)) {
            if (isActive(o.getStatus())) {
                rows.add(toRow(o, vendorDisplayName(o.getVendorId()), true));
            }
        }
        return rows;
    }

    @Override
    public List<OrderRow> fetchHistoryForStudent(String studentId) {
        refreshVendorCache();
        List<OrderRow> rows = new ArrayList<>();
        for (Order o : orderService.getOrdersForStudent(studentId)) {
            if (!isActive(o.getStatus())) {
                rows.add(toRow(o, vendorDisplayName(o.getVendorId()), false));
            }
        }
        return rows;
    }

    @Override
    public List<OrderRow> fetchIncomingOrdersForVendor(String vendorKey) {
        List<OrderRow> rows = new ArrayList<>();
        for (Order o : orderService.getOrdersForVendor(vendorKey)) {
            // CANCELLED orders are included ON PURPOSE: the vendor must see
            // when a student cancels (red badge), or they would cook for a
            // ghost. Status buttons refuse to touch them (forward-only rule).
            rows.add(toRow(o, studentDisplayName(o.getStudentId()), true));
        }
        return rows;
    }

    /**
     * The GUI sends lowercase statuses ("preparing"); the database stores
     * uppercase ("PREPARING"). Interface contract says "no-op if orderId
     * isn't found", so business-rule rejections are logged, not thrown.
     */
    @Override
    public void updateOrderStatus(String orderId, String newStatus) {
        try {
            orderService.updateOrderStatus(orderId, uiToDb(newStatus));
        } catch (RuntimeException e) {
            System.err.println("Status update rejected: " + e.getMessage());
        }
    }

    /**
     * Places a real order. The GUI passes the vendor's display NAME
     * (that's what its dropdown shows); we map it back to the vendorId.
     *
     * Person B's OrderService persists the order on its consumer thread
     * (Producer-Consumer), so the MongoDB _id may not exist for a few
     * milliseconds - we wait briefly for it. This method must therefore
     * be called from a background thread (the dashboard uses SwingWorker).
     */
    @Override
    public String placeOrder(String studentId, String studentName,
                             String vendorName, List<String> items) {
        studentNameById.put(studentId, studentName);   // free cache warm-up
        refreshVendorCache();

        String vendorId = vendorIdByName.get(vendorName);
        if (vendorId == null) {
            // Maybe the GUI passed an ID directly - accept that too.
            if (vendorNameById.containsKey(vendorName)) {
                vendorId = vendorName;
            } else {
                throw new IllegalArgumentException(
                        "Unknown vendor: " + vendorName);
            }
        }

        // SAFETY NET: re-check availability at order time. The dialog already
        // disables out-of-stock rows, but an item can sell out between the
        // menu loading and the student pressing "Place order" — this catch
        // turns that race into a clear message instead of a ghost order.
        List<Item> menu = new ItemDAO().findByVendor(vendorId);
        for (String wanted : items) {
            for (Item item : menu) {
                if (item.getName().equals(wanted) && !item.isAvailable()) {
                    throw new IllegalStateException(
                            "\"" + wanted + "\" just went out of stock - please update your order.");
                }
            }
        }

        Order order = orderService.placeOrder(studentId, vendorId, items);

        // Wait up to ~2.5 s for the consumer thread to persist and set the id.
        long deadline = System.currentTimeMillis() + 2500;
        while (order.getId() == null && System.currentTimeMillis() < deadline) {
            try { Thread.sleep(50); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
        }
        return order.getId() != null ? order.getId() : "(saving...)";
    }

    // ================================================================
    // extras used by the GUI
    // ================================================================

    /**
     * Vendor names for the Place Order dropdown. EDT-SAFE: only reads the
     * cache (filled by the poller's background fetches, which run before
     * the user can open the dialog). Falls back to the known seeded vendor
     * if the cache is somehow still cold.
     */
    public String[] cachedVendorNames() {
        // Cold cache -> empty list (never invent a vendor that may not exist;
        // the dialog already shows a friendly "no vendors" message).
        return vendorNameById.values().toArray(new String[0]);
    }

    /**
     * The selected vendor's FULL live menu straight from MongoDB —
     * including out-of-stock items, so the dialog can SHOW them greyed-out
     * with an "Out of stock" tag (feature request: students may see
     * unavailable items, they just can't order them; the dialog disables
     * the +/- stepper and placeOrder() below double-checks anyway).
     * BACKGROUND THREAD ONLY (the order dialog calls it via SwingWorker).
     */
    public List<Item> menuForVendorName(String vendorName) {
        refreshVendorCache();
        String vendorId = vendorIdByName.get(vendorName);
        if (vendorId == null) return new ArrayList<>();
        return new ItemDAO().findByVendor(vendorId);
    }

    /** Fresh vendor names straight from MongoDB (background thread only). */
    public String[] freshVendorNames() {
        refreshVendorCache();
        return cachedVendorNames();
    }

    /**
     * Live ETA if an order with this base prep time were placed RIGHT NOW
     * at the given stall - the menu window shows one per item. Same
     * EtaEngine as real orders, so the preview always matches reality.
     * BACKGROUND THREAD ONLY.
     */
    public int etaFor(String vendorName, int baseMinutes) {
        refreshVendorCache();
        String vendorId = vendorIdByName.get(vendorName);
        if (vendorId == null) {
            throw new IllegalArgumentException("Unknown vendor: " + vendorName);
        }
        return new EtaEngine().calculateEta(vendorId, baseMinutes);
    }

    /**
     * "Popular Stalls" ranking for the student home screen: vendors sorted
     * by how many (non-cancelled) orders they have received. HashMap tally
     * fed into a PriorityQueue with a reversed comparator - the same
     * two-collection trick as the vendor's popular-items panel.
     * BACKGROUND THREAD ONLY.
     */
    public List<String> popularStallNames(int topN) {
        refreshVendorCache();
        java.util.Map<String, Integer> tally = new java.util.HashMap<>();
        for (java.util.Map.Entry<String, String> v : vendorNameById.entrySet()) {
            int count = 0;
            for (Order o : orderService.getOrdersForVendor(v.getKey())) {
                if (!Order.STATUS_CANCELLED.equals(o.getStatus())) count++;
            }
            if (count > 0) tally.put(v.getValue(), count);
        }
        java.util.PriorityQueue<java.util.Map.Entry<String, Integer>> heap =
                new java.util.PriorityQueue<>((a, b) -> b.getValue() - a.getValue());
        heap.addAll(tally.entrySet());
        List<String> top = new ArrayList<>();
        for (int i = 0; i < topN && !heap.isEmpty(); i++) {
            java.util.Map.Entry<String, Integer> e = heap.poll();
            top.add(e.getKey() + "  \u00B7  " + e.getValue() + " orders");
        }
        return top;
    }

    // ================================================================
    // private helpers
    // ================================================================

    private void refreshVendorCache() {
        try {
            for (User v : userDAO.findAllVendors()) {
                vendorNameById.put(v.getUserId(), v.getName());
                vendorIdByName.put(v.getName(), v.getUserId());
            }
        } catch (RuntimeException e) {
            // Network blip - keep the old cache; next poll retries.
        }
    }

    private String vendorDisplayName(String vendorId) {
        return vendorNameById.getOrDefault(vendorId, vendorId);
    }

    private String studentDisplayName(String studentId) {
        return studentNameById.computeIfAbsent(studentId, id -> {
            try { return userDAO.findByUserId(id).getName(); }
            catch (RuntimeException e) { return id; }   // show the id as fallback
        });
    }

    private static boolean isActive(String dbStatus) {
        return Order.STATUS_PLACED.equals(dbStatus)
                || Order.STATUS_PREPARING.equals(dbStatus)
                || Order.STATUS_READY.equals(dbStatus);
    }

    private OrderRow toRow(Order o, String counterparty, boolean withEta) {
        String placed = (o.getPlacedAt() == null) ? "" : formatTime(o.getPlacedAt());
        Integer eta = withEta && isActive(o.getStatus()) ? o.getEtaMinutes() : null;
        return new OrderRow(
                o.getId(),
                counterparty,
                String.join(", ", o.getItemNames()),
                dbToUi(o.getStatus()),
                eta,
                placed);
    }

    /** DB status -> the lowercase words C's StatusBadgeRenderer expects. */
    private static String dbToUi(String db) {
        if (db == null) return "";
        return switch (db) {
            case Order.STATUS_PLACED    -> "pending";
            case Order.STATUS_PREPARING -> "preparing";
            case Order.STATUS_READY     -> "ready";
            case Order.STATUS_SERVED    -> "served";
            case Order.STATUS_CANCELLED -> "cancelled";
            default                     -> db.toLowerCase();
        };
    }

    /** GUI status -> DB status. */
    private static String uiToDb(String ui) {
        if (ui == null) return "";
        return switch (ui.toLowerCase()) {
            case "pending"   -> Order.STATUS_PLACED;
            case "preparing" -> Order.STATUS_PREPARING;
            case "ready"     -> Order.STATUS_READY;
            case "served"    -> Order.STATUS_SERVED;
            case "cancelled" -> Order.STATUS_CANCELLED;
            default          -> ui.toUpperCase();
        };
    }
}
