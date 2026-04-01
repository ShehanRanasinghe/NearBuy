package com.example.nearbuy.data.model;

import com.google.firebase.Timestamp;

import java.util.HashMap;
import java.util.Map;

/**
 * DealItem – the sole Firestore data model for a deal or promotion published by a shop.
 *
 * Read from:
 *   NearBuyHQ/{shopId}/deals/{dealId}        – deals
 *   NearBuyHQ/{shopId}/promotions/{promoId}  – promotions
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
    private String  occasion;           // e.g. "New Year", "Weekend Sale"
    private double  originalPrice;
    private double  salePrice;
    private String  discountLabel;      // e.g. "50% OFF", "Buy 1 Get 1"
    private int     discountPercentage; // e.g. 20  (used if discountLabel is empty)
    private String  imageUrl;
    private boolean isPromotion;        // false = deal, true = promotion
    private boolean isActive;
    private long    expiresAt;          // epoch ms  (mapped from expiresAt OR endDate)
    private long    createdAt;          // epoch ms

    // ── Runtime-only (populated from parent Shop document) ────────────────────
    private String shopName;
    private String shopLocation;
    private double shopLatitude;
    private double shopLongitude;
    private double distanceKm = -1;

    // Required no-arg constructor
    public DealItem() {}

    // ── Getters ───────────────────────────────────────────────────────────────
    public String  getId()               { return id; }
    public String  getShopId()           { return shopId; }
    public String  getTitle()            { return title; }
    public String  getDescription()      { return description; }
    public String  getCategory()         { return category; }
    public String  getOccasion()         { return occasion; }
    public double  getOriginalPrice()    { return originalPrice; }
    public double  getSalePrice()        { return salePrice; }
    public String  getImageUrl()         { return imageUrl; }
    public boolean isPromotion()         { return isPromotion; }
    public boolean isActive()            { return isActive; }
    public long    getExpiresAt()        { return expiresAt; }
    public long    getCreatedAt()        { return createdAt; }
    public int     getDiscountPercentage() { return discountPercentage; }

    /**
     * Returns the discount label.  If the stored label is empty but a numeric
     * discountPercentage was set (e.g. 20), returns "20% OFF" automatically.
     */
    public String getDiscountLabel() {
        if ((discountLabel == null || discountLabel.isEmpty()) && discountPercentage > 0) {
            return discountPercentage + "% OFF";
        }
        return discountLabel != null ? discountLabel : "";
    }

    // Runtime getters
    public String  getShopName()     { return shopName; }
    public String  getShopLocation() { return shopLocation; }
    public double  getShopLatitude() { return shopLatitude; }
    public double  getShopLongitude(){ return shopLongitude; }
    public double  getDistanceKm()   { return distanceKm; }
    public boolean hasDistance()     { return distanceKm >= 0; }

    /** True when an actual expiry date was stored in Firestore. */
    public boolean hasExpiry()       { return expiresAt > 0; }

    /** Human-readable distance label. */
    public String getDistanceLabel() {
        if (!hasDistance()) return "—";
        if (distanceKm < 1.0) return String.format("%.0f m", distanceKm * 1000);
        return String.format("%.1f km", distanceKm);
    }

    /**
     * Returns the number of days until expiry, or -1 if no expiry date is set.
     * Negative value (other than -1) means already expired.
     */
    public int daysUntilExpiry() {
        if (!hasExpiry()) return -1;          // sentinel: no expiry set
        long diff = expiresAt - System.currentTimeMillis();
        return (int) (diff / (1000L * 60 * 60 * 24));
    }

    /** Human-readable expiry label shown on deal cards (never shows huge numbers). */
    public String getExpiryLabel() {
        if (!hasExpiry()) return "No expiry";
        int days = daysUntilExpiry();
        if (days < 0)  return "Expired";
        if (days == 0) return "Expires Today!";
        if (days == 1) return "Expires Tomorrow";
        return "Expires in " + days + " days";
    }

    /** Human-readable "Valid until dd MMM yyyy" label shown on promo cards. */
    public String getValidDateLabel() {
        if (!hasExpiry()) return "No expiry";
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(
                "dd MMM yyyy", java.util.Locale.getDefault());
        return "Valid until " + sdf.format(new java.util.Date(expiresAt));
    }

    // ── Setters (runtime only) ─────────────────────────────────────────────────
    public void setShopName(String name)       { this.shopName      = name; }
    public void setShopLocation(String loc)    { this.shopLocation  = loc; }
    public void setShopLatitude(double lat)    { this.shopLatitude  = lat; }
    public void setShopLongitude(double lng)   { this.shopLongitude = lng; }
    public void setDistanceKm(double km)       { this.distanceKm    = km; }

    // ── Field setters ──────────────────────────────────────────────────────────
    public void setId(String id)                       { this.id                 = id; }
    public void setShopId(String shopId)               { this.shopId             = shopId; }
    public void setTitle(String title)                 { this.title              = title; }
    public void setDiscountLabel(String label)         { this.discountLabel      = label; }
    public void setDiscountPercentage(int pct)         { this.discountPercentage = pct; }
    public void setSalePrice(double price)             { this.salePrice          = price; }
    public void setOriginalPrice(double price)         { this.originalPrice      = price; }
    public void setCategory(String category)           { this.category           = category; }
    public void setOccasion(String occasion)           { this.occasion           = occasion; }
    public void setPromotion(boolean isPromo)          { this.isPromotion        = isPromo; }
    public void setExpiresAt(long ts)                  { this.expiresAt          = ts; }
    public void setCreatedAt(long ts)                  { this.createdAt          = ts; }

    // ── Firestore serialisation ───────────────────────────────────────────────

    public static DealItem fromMap(String id, String shopId, Map<String, Object> map) {
        if (map == null) return null;
        DealItem d = new DealItem();
        d.id                = id;
        d.shopId            = shopId;
        d.title             = str(map.get("title"));
        d.description       = str(map.get("description"));
        d.category          = str(map.get("category"));
        d.occasion          = str(map.get("occasion"));
        d.originalPrice     = dbl(map.get("originalPrice"));
        d.salePrice         = dbl(map.get("salePrice"));
        d.discountLabel     = str(map.get("discountLabel"));
        d.discountPercentage= intVal(map.get("discountPercentage"));
        d.imageUrl          = str(map.get("imageUrl"));
        d.isPromotion       = bool(map.get("isPromotion"), false);
        d.isActive          = bool(map.get("isActive"), true);

        // Support both "expiresAt" and "endDate" field names; handle Firestore Timestamp type
        d.expiresAt = parseTs(map.get("expiresAt"));
        if (d.expiresAt <= 0) d.expiresAt = parseTs(map.get("endDate"));

        // Support both "createdAt" and "startDate"
        d.createdAt = parseTs(map.get("createdAt"));
        if (d.createdAt <= 0) d.createdAt = parseTs(map.get("startDate"));

        return d;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("title",              title);
        map.put("description",        description);
        map.put("category",           category);
        map.put("occasion",           occasion);
        map.put("originalPrice",      originalPrice);
        map.put("salePrice",          salePrice);
        map.put("discountLabel",      discountLabel);
        map.put("discountPercentage", discountPercentage);
        map.put("imageUrl",           imageUrl);
        map.put("isPromotion",        isPromotion);
        map.put("isActive",           isActive);
        map.put("expiresAt",          expiresAt);
        map.put("createdAt",          createdAt);
        return map;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private static String  str(Object v) { return v == null ? "" : String.valueOf(v).trim(); }

    private static double dbl(Object v) {
        if (v instanceof Number) return ((Number) v).doubleValue();
        try { return Double.parseDouble(String.valueOf(v)); } catch (Exception e) { return 0.0; }
    }

    private static int intVal(Object v) {
        if (v instanceof Number) return ((Number) v).intValue();
        try { return Integer.parseInt(String.valueOf(v)); } catch (Exception e) { return 0; }
    }

    /**
     * Parses a Firestore timestamp field that may be stored as:
     *   • {@code com.google.firebase.Timestamp}  (native Firestore type)
     *   • {@code Long} / {@code Integer}         (epoch milliseconds as a number)
     *   • {@code String}                         (epoch milliseconds as text)
     * Returns epoch milliseconds, or 0 if the value is null / unparseable.
     */
    private static long parseTs(Object v) {
        if (v == null) return 0L;
        if (v instanceof Timestamp) {
            // Firestore Timestamp → convert to epoch ms
            return ((Timestamp) v).toDate().getTime();
        }
        if (v instanceof Number) return ((Number) v).longValue();
        try { return Long.parseLong(String.valueOf(v)); } catch (Exception e) { return 0L; }
    }

    private static boolean bool(Object v, boolean def) {
        if (v instanceof Boolean) return (Boolean) v;
        if (v instanceof String)  return Boolean.parseBoolean((String) v);
        return def;
    }
}

