package com.collegefest.models;

import java.util.Date;
import java.util.List;

public class Order {

    // Canonical status values - ALWAYS use these constants, never raw
    // strings, so a typo becomes a compile error instead of a silent bug.
    // Flow (forward only): PLACED -> PREPARING -> READY -> SERVED;
    // a PLACED order may instead become CANCELLED.
    public static final String STATUS_PLACED    = "PLACED";
    public static final String STATUS_PREPARING = "PREPARING";
    public static final String STATUS_READY     = "READY";
    public static final String STATUS_SERVED    = "SERVED";
    public static final String STATUS_CANCELLED = "CANCELLED";

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
        this.status = STATUS_PLACED;
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