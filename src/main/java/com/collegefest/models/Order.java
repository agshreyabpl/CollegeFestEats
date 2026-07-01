package com.collegefest.models;

import java.util.Date;
import java.util.List;

public class Order {
    private String id;
    private String studentId;
    private String vendorId;
    private List<String> itemNames;
    private String status;
    private Date placedAt;
    private int etaMinutes;

    public Order() {}

    public Order(String studentId, String vendorId, List<String> itemNames) {
        this.studentId = studentId;
        this.vendorId = vendorId;
        this.itemNames = itemNames;
        this.status = "pending";
        this.placedAt = new Date();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getStudentId() { return studentId; }
    public void setStudentId(String s) { this.studentId = s; }
    public String getVendorId() { return vendorId; }
    public void setVendorId(String v) { this.vendorId = v; }
    public List<String> getItemNames() { return itemNames; }
    public void setItemNames(List<String> i) { this.itemNames = i; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Date getPlacedAt() { return placedAt; }
    public void setPlacedAt(Date d) { this.placedAt = d; }
    public int getEtaMinutes() { return etaMinutes; }
    public void setEtaMinutes(int e) { this.etaMinutes = e; }
}