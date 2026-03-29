package com.example.nearbuy.data.model;

import java.util.HashMap;
import java.util.Map;

/**
 * DealItem – Firestore data model for a deal or promotion published by a shop.
 *
 * Read from:
 *   NearBuyHQ/{shopId}/deals/{dealId}        – deals
 *   NearBuyHQ/{shopId}/promotions/{promoId}  – promotions
 *
 * NOTE: This is the Firestore-backed model.  The existing {@code deals.Deal} class
 * is a UI-only view model used to populate RecyclerView cards; this class handles
 * serialisation to/from Firestore.
 *
 * Used by:
 *   • Dashboard – "Latest Deals" and "Promotions" sections.
 *   • DealsActivity – full deal listing.
 *   • SavedDealsActivity – deals the customer has bookmarked.
 */
public class DealItem {

    // ── Firestore fields ──────────────────────────────────────────────────────
    private String  id;
    private String  shopId;
    private String  title;
    private String  description;
    private String  category;
    private double  originalPrice;
    private double  salePrice;
    private String  discountLabel;   // e.g. "50% OFF", "Buy 1 Get 1"
    private String  imageUrl;
    private boolean isPromotion;     // false = deal, true = promotion
    private boolean isActive;
    private long    expiresAt;       // epoch ms
    private long    createdAt;

    // ── Runtime-only (populated from parent Shop document) ────────────────────
    private String shopName;
    private String shopLocation;
    private double shopLatitude;
    private double shopLongitude;
    private double distanceKm = -1;

    // Required no-arg constructor
    public DealItem() {}

    // ── Getters ───────────────────────────────────────────────────────────────
    public String  getId()            { return id; }
    public String  getShopId()        { return shopId; }
    public String  getTitle()         { return title; }
    public String  getDescription()   { return description; }
    public String  getCategory()      { return category; }
    public double  getOriginalPrice() { return originalPrice; }
    public double  getSalePrice()     { return salePrice; }
    public String  getDiscountLabel() { return discountLabel; }
    public String  getImageUrl()      { return imageUrl; }
    public boolean isPromotion()      { return isPromotion; }
    public boolean isActive()         { return isActive; }
    public long    getExpiresAt()     { return expiresAt; }
    public long    getCreatedAt()     { return createdAt; }

    // Runtime getters
    public String getShopName()      { return shopName; }
    public String getShopLocation()  { return shopLocation; }
    public double getShopLatitude()  { return shopLatitude; }
    public double getShopLongitude() { return shopLongitude; }
    public double getDistanceKm()    { return distanceKm; }
    public boolean hasDistance()     { return distanceKm >= 0; }

    /** Human-readable distance label. */
    public String getDistanceLabel() {
        if (!hasDistance()) return "—";
        if (distanceKm < 1.0) return String.format("%.0f m", distanceKm * 1000);
        return String.format("%.1f km", distanceKm);
    }

    /** Returns the number of days until this deal expires (negative = already expired). */
    public int daysUntilExpiry() {
        if (expiresAt <= 0) return Integer.MAX_VALUE;
        long diff = expiresAt - System.currentTimeMillis();
        return (int) (diff / (1000 * 60 * 60 * 24));
    }

    /** Human-readable expiry label shown on deal cards. */
    public String getExpiryLabel() {
        int days = daysUntilExpiry();
        if (days < 0)  return "Expired";
        if (days == 0) return "Expires Today!";
        if (days == 1) return "Expires Tomorrow";
        return "Expires in " + days + " days";
    }

    // ── Setters (runtime only) ─────────────────────────────────────────────────
    public void setShopName(String name)     { this.shopName      = name; }
    public void setShopLocation(String loc)  { this.shopLocation  = loc; }
    public void setShopLatitude(double lat)  { this.shopLatitude  = lat; }
    public void setShopLongitude(double lng) { this.shopLongitude = lng; }
    public void setDistanceKm(double km)     { this.distanceKm    = km; }

    // ── Firestore serialisation ───────────────────────────────────────────────

    public static DealItem fromMap(String id, String shopId, Map<String, Object> map) {
        if (map == null) return null;
        DealItem d = new DealItem();
        d.id            = id;
        d.shopId        = shopId;
        d.title         = str(map.get("title"));
        d.description   = str(map.get("description"));
        d.category      = str(map.get("category"));
        d.originalPrice = dbl(map.get("originalPrice"));
        d.salePrice     = dbl(map.get("salePrice"));
        d.discountLabel = str(map.get("discountLabel"));
        d.imageUrl      = str(map.get("imageUrl"));
        d.isPromotion   = bool(map.get("isPromotion"), false);
        d.isActive      = bool(map.get("isActive"), true);
        d.expiresAt     = lng(map.get("expiresAt"));
        d.createdAt     = lng(map.get("createdAt"));
        return d;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("title",         title);
        map.put("description",   description);
        map.put("category",      category);
        map.put("originalPrice", originalPrice);
        map.put("salePrice",     salePrice);
        map.put("discountLabel", discountLabel);
        map.put("imageUrl",      imageUrl);
        map.put("isPromotion",   isPromotion);
        map.put("isActive",      isActive);
        map.put("expiresAt",     expiresAt);
        map.put("createdAt",     createdAt);
        return map;
    }

    // ── Private helpers ───────────────────────────────────────────────────────
    private static String  str(Object v)  { return v == null ? "" : String.valueOf(v).trim(); }
    private static double  dbl(Object v)  {
        if (v instanceof Number) return ((Number) v).doubleValue();
        try { return Double.parseDouble(String.valueOf(v)); } catch (Exception e) { return 0.0; }
    }
    private static long    lng(Object v)  {
        if (v instanceof Number) return ((Number) v).longValue();
        try { return Long.parseLong(String.valueOf(v)); } catch (Exception e) { return 0L; }
    }
    private static boolean bool(Object v, boolean def) {
        if (v instanceof Boolean) return (Boolean) v;
        if (v instanceof String)  return Boolean.parseBoolean((String) v);
        return def;
    }
}

