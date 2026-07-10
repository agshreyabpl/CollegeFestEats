package com.collegefest.db;

/**
 * Calculates rush-aware ETA for an order at a given vendor.
 * Formula: eta = baseMinutes × (activeOrders + 1) + BUFFER
 * As more orders pile up at a vendor, every new student gets a longer ETA.
 */
public class EtaEngine {

    private static final int BUFFER_MINUTES = 2;
    private OrderDAO orderDAO;

    public EtaEngine() {
        this.orderDAO = new OrderDAO();
    }

    /**
     * @param vendorId    the vendor this order is going to
     * @param baseMinutes average prep time per order (from item data)
     * @return estimated minutes until order is ready
     */
    public int calculateEta(String vendorId, int baseMinutes) {
        long activeOrders = orderDAO.countActiveOrders(vendorId);
        int eta = (int) (baseMinutes * (activeOrders + 1)) + BUFFER_MINUTES;
        return eta;
    }

    /**
     * Convenience method when base time is unknown — uses default 5 min.
     */
    public int calculateEta(String vendorId) {
        return calculateEta(vendorId, 5);
    }
}