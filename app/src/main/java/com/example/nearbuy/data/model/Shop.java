package com.example.nearbuy.data.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Shop – Firestore data model for a shop registered through the NearBuyHQ admin app.
 *
 * Read from:  NearBuyHQ/{shopId}
 *
 * The customer app reads shop documents to:
 *   • Display shop cards on the Dashboard (nearby shops section).
 *   • Show store details on the Store Details screen.
 *   • Calculate distance from the customer's current location to the shop.
 *   • Link search results to their originating shop.
 */
public class Shop {

    // ── Fields ────────────────────────────────────────────────────────────────
    private String id;              // Firestore document ID (== ownerUid in NearBuyHQ)
    private String name;            // display name of the shop
    private String ownerName;       // registered owner's full name
    private String email;
    private String phone;
    private String shopLocation;    // human-readable address
    private String openingHours;
    private double latitude;
    private double longitude;
    private String status;          // "Active" | "Inactive"
    private long   createdAt;

    // Runtime-only field (not stored in Firestore – calculated client-side)
    private double distanceKm = -1;

    // Required no-arg constructor for Firestore deserialization
    public Shop() {}

    // ── Getters ───────────────────────────────────────────────────────────────
    public String getId()            { return id; }
    public String getName()          { return name; }
    public String getOwnerName()     { return ownerName; }
    public String getEmail()         { return email; }
    public String getPhone()         { return phone; }
    public String getShopLocation()  { return shopLocation; }
    public String getOpeningHours()  { return openingHours; }
    public double getLatitude()      { return latitude; }
    public double getLongitude()     { return longitude; }
    public String getStatus()        { return status; }
    public long   getCreatedAt()     { return createdAt; }

    /** Returns true when the shop has valid GPS coordinates. */
    public boolean hasLocation()     { return latitude != 0.0 || longitude != 0.0; }

    // ── Distance helpers (calculated client-side, not persisted) ─────────────

    public void setDistanceKm(double km)    { this.distanceKm = km; }
    public double getDistanceKm()           { return distanceKm; }
    public boolean hasDistance()            { return distanceKm >= 0; }

    /** Returns a human-readable distance label, e.g. "1.3 km". */
    public String getDistanceLabel() {
        if (!hasDistance()) return "—";
        if (distanceKm < 1.0) return String.format("%.0f m", distanceKm * 1000);
        return String.format("%.1f km", distanceKm);
    }

    // ── Firestore serialisation ───────────────────────────────────────────────

    /** Reconstructs a Shop from a Firestore document map (as stored by NearBuyHQ). */
    public static Shop fromMap(String id, Map<String, Object> map) {
        if (map == null) return null;
        Shop s = new Shop();
        s.id           = id;
        // NearBuyHQ stores the owner's display name under "name"
        s.ownerName    = str(map.get("name"));
        s.name         = defaultIfEmpty(str(map.get("shopName")), s.ownerName + "'s Shop");
        s.email        = str(map.get("email"));
        s.phone        = str(map.get("phone"));
        s.shopLocation = str(map.get("shopLocation"));
        s.openingHours = str(map.get("openingHours"));
        s.latitude     = dbl(map.get("latitude"));
        s.longitude    = dbl(map.get("longitude"));
        s.status       = defaultIfEmpty(str(map.get("status")), "Active");
        s.createdAt    = lng(map.get("createdAt"));
        return s;
    }

    /** Converts to Map for any customer-side writes (e.g. caching). */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("shopName",     name);
        map.put("ownerName",    ownerName);
        map.put("email",        email);
        map.put("phone",        phone);
        map.put("shopLocation", shopLocation);
        map.put("openingHours", openingHours);
        map.put("latitude",     latitude);
        map.put("longitude",    longitude);
        map.put("status",       status);
        map.put("createdAt",    createdAt);
        return map;
    }

    // ── Private helpers ───────────────────────────────────────────────────────
    private static String str(Object v) { return v == null ? "" : String.valueOf(v).trim(); }
    private static double dbl(Object v) {
        if (v instanceof Number) return ((Number) v).doubleValue();
        try { return Double.parseDouble(String.valueOf(v)); } catch (Exception e) { return 0.0; }
    }
    private static long lng(Object v) {
        if (v instanceof Number) return ((Number) v).longValue();
        try { return Long.parseLong(String.valueOf(v)); } catch (Exception e) { return 0L; }
    }
    private static String defaultIfEmpty(String v, String fallback) {
        return (v == null || v.isEmpty()) ? fallback : v;
    }
}

