package com.example.nearbuy.data.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Product – Firestore data model for a product listed by a shop.
 *
 * Read from:  NearBuyHQ/{shopId}/products/{productId}
 *
 * Used by the customer app for:
 *   • Search results – show product name, price, shop name, distance.
 *   • Product Details screen.
 *   • Dashboard "Hot Products" section.
 *
 * Runtime fields (distanceKm, shopName, shopLocation, shopLatitude, shopLongitude)
 * are NOT stored in Firestore – they are populated at query time from the parent
 * Shop document so that the customer app can display distance and shop info
 * alongside each product.
 */
public class Product {

    // ── Firestore fields ──────────────────────────────────────────────────────
    private String id;
    private String shopId;          // parent shop document ID
    private String name;
    private String description;
    private String category;
    private double price;
    private double originalPrice;   // 0 if no discount
    private String unit;            // e.g. "kg", "piece", "500g pack"
    private int    stockQty;
    private String imageUrl;
    private boolean isAvailable;
    private long   createdAt;
    private long   updatedAt;

    // ── Runtime-only (not persisted) ──────────────────────────────────────────
    private String shopName;
    private String shopLocation;
    private double shopLatitude;
    private double shopLongitude;
    private double distanceKm = -1; // -1 = not yet calculated

    // Required no-arg constructor
    public Product() {}

    // ── Getters ───────────────────────────────────────────────────────────────
    public String  getId()            { return id; }
    public String  getShopId()        { return shopId; }
    public String  getName()          { return name; }
    public String  getDescription()   { return description; }
    public String  getCategory()      { return category; }
    public double  getPrice()         { return price; }
    public double  getOriginalPrice() { return originalPrice; }
    public String  getUnit()          { return unit; }
    public int     getStockQty()      { return stockQty; }
    public String  getImageUrl()      { return imageUrl; }
    public boolean isAvailable()      { return isAvailable; }
    public long    getCreatedAt()     { return createdAt; }
    public long    getUpdatedAt()     { return updatedAt; }

    // Runtime getters
    public String getShopName()      { return shopName; }
    public String getShopLocation()  { return shopLocation; }
    public double getShopLatitude()  { return shopLatitude; }
    public double getShopLongitude() { return shopLongitude; }
    public double getDistanceKm()    { return distanceKm; }
    public boolean hasDistance()     { return distanceKm >= 0; }

    /** Returns a human-readable formatted price, e.g. "Rs. 120.00". */
    public String getPriceLabel()    { return String.format("Rs. %.2f", price); }

    /** Returns a human-readable distance label, e.g. "1.3 km". */
    public String getDistanceLabel() {
        if (!hasDistance()) return "—";
        if (distanceKm < 1.0) return String.format("%.0f m", distanceKm * 1000);
        return String.format("%.1f km", distanceKm);
    }

    /** Returns true when a discounted original price is set. */
    public boolean hasDiscount() { return originalPrice > 0 && originalPrice > price; }

    // ── Setters (runtime only) ────────────────────────────────────────────────
    public void setShopName(String name)         { this.shopName      = name; }
    public void setShopLocation(String loc)      { this.shopLocation  = loc; }
    public void setShopLatitude(double lat)      { this.shopLatitude  = lat; }
    public void setShopLongitude(double lng)     { this.shopLongitude = lng; }
    public void setDistanceKm(double km)         { this.distanceKm    = km; }

    // ── Firestore serialisation ───────────────────────────────────────────────

    /** Reconstructs a Product from a Firestore document map. */
    public static Product fromMap(String id, String shopId, Map<String, Object> map) {
        if (map == null) return null;
        Product p = new Product();
        p.id            = id;
        p.shopId        = shopId;
        p.name          = str(map.get("name"));
        p.description   = str(map.get("description"));
        p.category      = str(map.get("category"));
        p.price         = dbl(map.get("price"));
        p.originalPrice = dbl(map.get("originalPrice"));
        p.unit          = str(map.get("unit"));
        p.stockQty      = intVal(map.get("stockQty"));
        p.imageUrl      = str(map.get("imageUrl"));
        p.isAvailable   = bool(map.get("isAvailable"), true);
        p.createdAt     = lng(map.get("createdAt"));
        p.updatedAt     = lng(map.get("updatedAt"));
        return p;
    }

    /** Converts to Map for Firestore writes (does NOT include runtime fields). */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("name",          name);
        map.put("description",   description);
        map.put("category",      category);
        map.put("price",         price);
        map.put("originalPrice", originalPrice);
        map.put("unit",          unit);
        map.put("stockQty",      stockQty);
        map.put("imageUrl",      imageUrl);
        map.put("isAvailable",   isAvailable);
        map.put("createdAt",     createdAt);
        map.put("updatedAt",     updatedAt);
        return map;
    }

    // ── Private helpers ───────────────────────────────────────────────────────
    private static String  str(Object v)    { return v == null ? "" : String.valueOf(v).trim(); }
    private static double  dbl(Object v)    {
        if (v instanceof Number) return ((Number) v).doubleValue();
        try { return Double.parseDouble(String.valueOf(v)); } catch (Exception e) { return 0.0; }
    }
    private static int     intVal(Object v) {
        if (v instanceof Number) return ((Number) v).intValue();
        try { return Integer.parseInt(String.valueOf(v)); } catch (Exception e) { return 0; }
    }
    private static long    lng(Object v)    {
        if (v instanceof Number) return ((Number) v).longValue();
        try { return Long.parseLong(String.valueOf(v)); } catch (Exception e) { return 0L; }
    }
    private static boolean bool(Object v, boolean def) {
        if (v instanceof Boolean) return (Boolean) v;
        if (v instanceof String)  return Boolean.parseBoolean((String) v);
        return def;
    }
}

