package com.example.nearbuy.data.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Customer – Firestore data model for a NearBuy registered customer.
 *
 * Stored at:  NearBuy/{customerId}
 *
 * Fields mirror what the customer enters during registration, plus runtime
 * stats (totalOrders, totalSpent) that are incremented by the order flow.
 */
public class Customer {

    // ── Fields ────────────────────────────────────────────────────────────────
    private String id;
    private String name;
    private String email;
    private String phone;
    private String status;          // "Active" | "Suspended"
    private int    totalOrders;
    private double totalSpent;      // lifetime spend in LKR
    private double totalSaved;      // lifetime savings from deals in LKR
    private long   createdAt;
    private long   updatedAt;

    // Required no-arg constructor for Firestore deserialization
    public Customer() {}

    /** Full constructor – used when creating a new account. */
    public Customer(String id, String name, String email, String phone) {
        this.id           = id;
        this.name         = name;
        this.email        = email;
        this.phone        = phone;
        this.status       = "Active";
        this.totalOrders  = 0;
        this.totalSpent   = 0.0;
        this.totalSaved   = 0.0;
        this.createdAt    = System.currentTimeMillis();
        this.updatedAt    = System.currentTimeMillis();
    }

    // ── Getters ───────────────────────────────────────────────────────────────
    public String getId()          { return id; }
    public String getName()        { return name; }
    public String getEmail()       { return email; }
    public String getPhone()       { return phone; }
    public String getStatus()      { return status; }
    public int    getTotalOrders() { return totalOrders; }
    public double getTotalSpent()  { return totalSpent; }
    public double getTotalSaved()  { return totalSaved; }
    public long   getCreatedAt()   { return createdAt; }
    public long   getUpdatedAt()   { return updatedAt; }

    /** Returns the first character of the customer name, used for avatar circles. */
    public String getAvatarInitial() {
        return (name != null && !name.isEmpty())
                ? String.valueOf(name.charAt(0)).toUpperCase() : "?";
    }

    // ── Setters ───────────────────────────────────────────────────────────────
    public void setStatus(String status)      { this.status = status; this.updatedAt = System.currentTimeMillis(); }
    public void setTotalOrders(int count)     { this.totalOrders = count; }
    public void setTotalSpent(double amount)  { this.totalSpent = amount; }
    public void setTotalSaved(double amount)  { this.totalSaved = amount; }

    // ── Firestore serialisation ───────────────────────────────────────────────

    /** Converts this model to a Firestore-compatible Map for writes. */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("name",         name);
        map.put("email",        email);
        map.put("phone",        phone);
        map.put("status",       status);
        map.put("totalOrders",  totalOrders);
        map.put("totalSpent",   totalSpent);
        map.put("totalSaved",   totalSaved);
        map.put("createdAt",    createdAt);
        map.put("updatedAt",    updatedAt);
        return map;
    }

    /** Reconstructs a Customer from a Firestore document map. */
    public static Customer fromMap(String id, Map<String, Object> map) {
        if (map == null) return null;
        Customer c = new Customer();
        c.id          = id;
        c.name        = str(map.get("name"));
        c.email       = str(map.get("email"));
        c.phone       = str(map.get("phone"));
        c.status      = defaultIfEmpty(str(map.get("status")), "Active");
        c.totalOrders = intVal(map.get("totalOrders"));
        c.totalSpent  = dbl(map.get("totalSpent"));
        c.totalSaved  = dbl(map.get("totalSaved"));
        c.createdAt   = lng(map.get("createdAt"));
        c.updatedAt   = lng(map.get("updatedAt"));
        return c;
    }

    // ── Private helpers ───────────────────────────────────────────────────────
    private static String str(Object v)  { return v == null ? "" : String.valueOf(v).trim(); }
    private static int    intVal(Object v) {
        if (v instanceof Number) return ((Number) v).intValue();
        try { return Integer.parseInt(String.valueOf(v)); } catch (Exception e) { return 0; }
    }
    private static double dbl(Object v) {
        if (v instanceof Number) return ((Number) v).doubleValue();
        try { return Double.parseDouble(String.valueOf(v)); } catch (Exception e) { return 0.0; }
    }
    private static long   lng(Object v) {
        if (v instanceof Number) return ((Number) v).longValue();
        try { return Long.parseLong(String.valueOf(v)); } catch (Exception e) { return 0L; }
    }
    private static String defaultIfEmpty(String v, String fallback) {
        return (v == null || v.isEmpty()) ? fallback : v;
    }
}

