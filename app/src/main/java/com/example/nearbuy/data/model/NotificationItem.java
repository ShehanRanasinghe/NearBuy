package com.example.nearbuy.data.model;

import java.util.HashMap;
import java.util.Map;

/**
 * NotificationItem – Firestore data model for a customer notification.
 *
 * Stored at:  NearBuy/{customerId}/notifications/{notificationId}
 *
 * Notifications can originate from:
 *   • A shop publishing a new deal/promotion near the customer.
 *   • An order status update.
 *   • A system announcement.
 */
public class NotificationItem {

    /** Type constants – used to control icon and navigation on tap. */
    public static final String TYPE_DEAL       = "deal";
    public static final String TYPE_ORDER      = "order";
    public static final String TYPE_PROMO      = "promo";
    public static final String TYPE_SYSTEM     = "system";

    // ── Firestore fields ──────────────────────────────────────────────────────
    private String  id;
    private String  type;           // one of the TYPE_* constants above
    private String  title;
    private String  body;
    private String  relatedId;      // dealId / orderId / promoId (optional)
    private String  shopId;         // originating shop (optional)
    private String  shopName;
    private boolean isRead;
    private long    createdAt;

    // Required no-arg constructor
    public NotificationItem() {}

    // ── Getters ───────────────────────────────────────────────────────────────
    public String  getId()         { return id; }
    public String  getType()       { return type; }
    public String  getTitle()      { return title; }
    public String  getBody()       { return body; }
    public String  getRelatedId()  { return relatedId; }
    public String  getShopId()     { return shopId; }
    public String  getShopName()   { return shopName; }
    public boolean isRead()        { return isRead; }
    public long    getCreatedAt()  { return createdAt; }

    // ── Setters ───────────────────────────────────────────────────────────────
    public void setRead(boolean read) { this.isRead = read; }

    // ── Firestore serialisation ───────────────────────────────────────────────

    public static NotificationItem fromMap(String id, Map<String, Object> map) {
        if (map == null) return null;
        NotificationItem n = new NotificationItem();
        n.id        = id;
        n.type      = str(map.get("type"));
        n.title     = str(map.get("title"));
        n.body      = str(map.get("body"));
        n.relatedId = str(map.get("relatedId"));
        n.shopId    = str(map.get("shopId"));
        n.shopName  = str(map.get("shopName"));
        n.isRead    = bool(map.get("isRead"), false);
        n.createdAt = lng(map.get("createdAt"));
        return n;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("type",      type);
        map.put("title",     title);
        map.put("body",      body);
        map.put("relatedId", relatedId);
        map.put("shopId",    shopId);
        map.put("shopName",  shopName);
        map.put("isRead",    isRead);
        map.put("createdAt", createdAt);
        return map;
    }

    // ── Private helpers ───────────────────────────────────────────────────────
    private static String  str(Object v)  { return v == null ? "" : String.valueOf(v).trim(); }
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

