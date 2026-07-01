package com.collegefest.models;

public class Item {
    private String id;
    private String vendorId;
    private String name;
    private double price;
    private boolean available;
    private int prepTimeMinutes;

    public Item() {}

    public Item(String vendorId, String name, double price, boolean available, int prepTimeMinutes) {
        this.vendorId = vendorId;
        this.name = name;
        this.price = price;
        this.available = available;
        this.prepTimeMinutes = prepTimeMinutes;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getVendorId() { return vendorId; }
    public void setVendorId(String vendorId) { this.vendorId = vendorId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
    public int getPrepTimeMinutes() { return prepTimeMinutes; }
    public void setPrepTimeMinutes(int m) { this.prepTimeMinutes = m; }
}