package com.collegefest.service;

import com.collegefest.db.EtaEngine;
import com.collegefest.db.ItemDAO;
import com.collegefest.db.OrderDAO;
import com.collegefest.exceptions.OrderNotFoundException;
import com.collegefest.models.Item;
import com.collegefest.models.Order;
import com.collegefest.models.User;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Central service layer for all order operations.
 *
 * Design patterns used:
 *  - Observer   : notifies dashboards on every status change
 *  - Factory    : delegates Order construction to OrderFactory
 *  - Producer-Consumer : OrderQueue with LinkedBlockingQueue + ExecutorService
 *
 * Professor requirements ticked here:
 *  ConcurrentHashMap, LinkedBlockingQueue, ExecutorService, Lambda, Generics
 */
public class OrderService {

    // ── Singleton-style shared DAOs ────────────────────────────
    private final OrderDAO orderDAO = new OrderDAO();
    private final ItemDAO  itemDAO  = new ItemDAO();

    // ── Observer list ──────────────────────────────────────────
    private final List<OrderObserver> observers = new ArrayList<>();

    // ── Active user sessions (ConcurrentHashMap — professor req) ──
    // Key = userId, Value = User object. Put on login, remove on logout.
    private static final ConcurrentHashMap<String, User> activeSessions
            = new ConcurrentHashMap<>();

    // ── Producer-Consumer (professor req) ─────────────────────
    // Orders placed by students are put onto this queue.
    // A background ExecutorService thread consumes them and saves to MongoDB.
    private final LinkedBlockingQueue<Order> orderQueue
            = new LinkedBlockingQueue<>();
    private final ExecutorService consumerExecutor
            = Executors.newSingleThreadExecutor();

    // ══════════════════════════════════════════════════════════
    // CONSTRUCTOR — starts the consumer thread
    // ══════════════════════════════════════════════════════════

    public OrderService() {
        startOrderConsumer();
    }

    // ══════════════════════════════════════════════════════════
    // OBSERVER PATTERN
    // ══════════════════════════════════════════════════════════

    /** Register a dashboard to receive live status-change updates. */
    public void registerObserver(OrderObserver observer) {
        observers.add(observer);
    }

    /** Call this when a dashboard window closes, so we don't hold a stale ref. */
    public void removeObserver(OrderObserver observer) {
        observers.remove(observer);
    }

    /** Pushes an update to every registered dashboard. */
    private void notifyObservers(Order order) {
        // Lambda over generics — professor requirement
        observers.forEach(obs -> obs.update(order));
    }

    // ══════════════════════════════════════════════════════════
    // PLACE ORDER (Producer side of Producer-Consumer)
    // ══════════════════════════════════════════════════════════

    /**
     * Places a new order for a student.
     *
     * Flow:
     *   1. Fetch vendor items → calculate average prepTime
     *   2. Call EtaEngine.calculateEta() for dynamic ETA
     *   3. Build Order via OrderFactory (Factory pattern)
     *   4. Put onto orderQueue (Producer) — consumer saves to MongoDB
     *   5. Return the Order so the UI can show "ETA: X min"
     *
     * @param studentId logged-in student's ID
     * @param vendorId  which vendor to order from
     * @param itemNames items the student selected
     * @return the newly created Order
     */
    public Order placeOrder(String studentId, String vendorId, List<String> itemNames) {
        // 1. Average prep time of selected items
        List<Item> menu = itemDAO.findByVendor(vendorId);
        int avgPrepTime = (int) menu.stream()
                .filter(i -> itemNames.contains(i.getName()))
                .mapToInt(Item::getPrepTimeMinutes)
                .average()
                .orElse(5.0);

        // 2. Dynamic ETA from EtaEngine (Person A's class — available after Day 2 merge)
        int eta = EtaEngine.calculateEta(vendorId, avgPrepTime);

        // 3. Build via Factory
        Order order = OrderFactory.buildOrder(studentId, vendorId, itemNames, eta);

        // 4. Put onto queue (consumer thread will persist to MongoDB)
        try {
            orderQueue.put(order);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return order;
    }

    // ══════════════════════════════════════════════════════════
    // PRODUCER-CONSUMER — background consumer thread
    // ══════════════════════════════════════════════════════════

    /**
     * Starts a single background thread via ExecutorService.
     * Polls the orderQueue and saves each order to MongoDB.
     * Notifies observers so dashboards update immediately.
     */
    private void startOrderConsumer() {
        consumerExecutor.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Order order = orderQueue.take(); // blocks until an order arrives
                    orderDAO.insertOrder(order);     // save to MongoDB
                    notifyObservers(order);          // push to dashboards
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
    }

    /** Call on app exit to stop the consumer thread cleanly. */
    public void shutdown() {
        consumerExecutor.shutdownNow();
    }

    // ══════════════════════════════════════════════════════════
    // READ OPERATIONS
    // ══════════════════════════════════════════════════════════

    /** Returns all orders placed by a student. Used by StudentDashboard. */
    public List<Order> getOrdersForStudent(String studentId) {
        return orderDAO.findByStudent(studentId);
    }

    /** Returns all orders received by a vendor. Used by VendorDashboard. */
    public List<Order> getOrdersForVendor(String vendorId) {
        return orderDAO.findByVendor(vendorId);
    }

    // ══════════════════════════════════════════════════════════
    // STATUS UPDATE — forward-only transitions
    // ══════════════════════════════════════════════════════════

    /**
     * Advances an order's status (PLACED → PREPARING → READY → SERVED).
     * Enforces forward-only flow — throws IllegalStateException if reversed.
     * Notifies all observers after a successful update.
     *
     * @param orderId   MongoDB _id of the order
     * @param newStatus the target status string
     * @throws OrderNotFoundException  if orderId doesn't exist
     * @throws IllegalStateException   if the transition is invalid
     */
    public void updateOrderStatus(String orderId, String newStatus) {
        Order order = orderDAO.findById(orderId); // throws OrderNotFoundException if missing

        if (!isValidTransition(order.getStatus(), newStatus)) {
            throw new IllegalStateException(
                "Invalid status change: " + order.getStatus() + " → " + newStatus
            );
        }

        orderDAO.updateStatus(orderId, newStatus);
        order.setStatus(newStatus);
        notifyObservers(order);
    }

    // ══════════════════════════════════════════════════════════
    // ORDER CANCELLATION
    // ══════════════════════════════════════════════════════════

    /**
     * Cancels an order — only allowed while status is PLACED.
     *
     * @param orderId MongoDB _id of the order to cancel
     * @throws OrderNotFoundException if the order doesn't exist
     * @throws IllegalStateException  if order is already past PLACED
     */
    public void cancelOrder(String orderId) {
        Order order = orderDAO.findById(orderId); // throws OrderNotFoundException if missing

        if (!"PLACED".equals(order.getStatus())) {
            throw new IllegalStateException(
                "Order already past pending — cannot cancel"
            );
        }

        orderDAO.updateStatus(orderId, "CANCELLED");
        order.setStatus("CANCELLED");
        notifyObservers(order);
    }

    // ══════════════════════════════════════════════════════════
    // SESSION MANAGEMENT (ConcurrentHashMap)
    // ══════════════════════════════════════════════════════════

    /** Called on successful login to register the active session. */
    public static void registerSession(String userId, User user) {
        activeSessions.put(userId, user);
    }

    /** Called on logout or window close to remove the session. */
    public static void removeSession(String userId) {
        activeSessions.remove(userId);
    }

    /** Returns a snapshot of all currently active sessions. */
    public static ConcurrentHashMap<String, User> getActiveSessions() {
        return activeSessions;
    }

    // ══════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ══════════════════════════════════════════════════════════

    /** Only forward transitions in the order lifecycle are allowed. */
    private boolean isValidTransition(String current, String next) {
        switch (current) {
            case "PLACED":    return "PREPARING".equals(next) || "CANCELLED".equals(next);
            case "PREPARING": return "READY".equals(next);
            case "READY":     return "SERVED".equals(next);
            default:          return false; // SERVED and CANCELLED are terminal
        }
    }
}
