package com.example.nearbuy.orders;

import java.util.HashMap;
import java.util.Map;

/**
 * OrderItem – represents a single customer order.
 *
 * Firestore paths (dual-write):
 *   NearBuyHQ/{shopId}/orders/{orderId}   ← visible to admin / shop owner
 *   NearBuy/{customerId}/orders/{orderId} ← visible to the customer
 *
 * Customer stats (totalOrders, totalSpent) are NOT updated by this app.
 * They are maintained exclusively by the NearBuyHQ admin app.
 */
public class OrderItem {

    private String orderId;
    private String shopId;          // Firestore shopId reference
    private String shopName;
    private String shopEmoji;
    private String orderDate;       // formatted display string, e.g. "Mar 25, 2026"
    private String itemsSummary;    // e.g. "Apples x2, Milk x1"
    private String totalAmount;     // formatted display string, e.g. "Rs.350"
    private String status;          // "Delivered" | "Processing" | "Cancelled"
    private int    itemCount;
    private double totalAmountRaw;  // numeric value for stats calculation
    private long   createdAt;       // epoch ms

    // ── Fields for admin visibility ───────────────────────────────────────────
    private String customerId;      // UID of the customer who placed the order
    private String customerName;    // display name of the customer
    private String customerPhone;   // phone number of the customer
    private String customerAddress; // delivery address of the customer
    private String fulfillmentType; // "Delivery" | "Pick Up"

    /** Constructor used by the OrdersAdapter (existing UI compatibility). */
    public OrderItem(String orderId, String shopName, String shopEmoji,
                     String orderDate, String itemsSummary,
                     String totalAmount, String status, int itemCount) {
        this.orderId      = orderId;
        this.shopName     = shopName;
        this.shopEmoji    = shopEmoji;
        this.orderDate    = orderDate;
        this.itemsSummary = itemsSummary;
        this.totalAmount  = totalAmount;
        this.status       = status;
        this.itemCount    = itemCount;
    }

    // ── Getters ───────────────────────────────────────────────────────────────
    public String getOrderId()        { return orderId; }
    public String getShopId()         { return shopId; }
    public String getShopName()       { return shopName; }
    public String getShopEmoji()      { return shopEmoji; }
    public String getOrderDate()      { return orderDate; }
    public String getItemsSummary()   { return itemsSummary; }
    public String getTotalAmount()    { return totalAmount; }
    public String getStatus()         { return status; }
    public int    getItemCount()      { return itemCount; }
    public double getTotalAmountRaw() { return totalAmountRaw; }
    public long   getCreatedAt()      { return createdAt; }
    public String getCustomerId()     { return customerId; }
    public String getCustomerName()   { return customerName; }
    public String getCustomerPhone()  { return customerPhone; }
    public String getCustomerAddress(){ return customerAddress; }
    public String getFulfillmentType(){ return fulfillmentType; }

    // ── Setters ───────────────────────────────────────────────────────────────
    public void setShopId(String shopId)              { this.shopId = shopId; }
    public void setTotalAmountRaw(double amount)      { this.totalAmountRaw = amount; }
    public void setCustomerId(String customerId)      { this.customerId = customerId; }
    public void setCustomerName(String customerName)  { this.customerName = customerName; }
    public void setCustomerPhone(String phone)        { this.customerPhone = phone; }
    public void setCustomerAddress(String address)    { this.customerAddress = address; }
    public void setFulfillmentType(String type)       { this.fulfillmentType = type; }
    public void setStatus(String status)              { this.status = status; }

    // ── Firestore serialisation ───────────────────────────────────────────────

    /**
     * Reconstructs an OrderItem from a Firestore document map.
     *
     * @param id  Firestore document ID
     * @param map data map from Firestore
     * @return OrderItem, or null if map is null
     */
    public static OrderItem fromMap(String id, Map<String, Object> map) {
        if (map == null) return null;

        String shopName       = str(map.get("shopName"));
        String shopEmoji      = str(map.get("shopEmoji"));
        String dateStr        = str(map.get("orderDate"));
        String summary        = str(map.get("itemsSummary"));
        String totalStr       = str(map.get("totalAmount"));
        String status         = defaultIfEmpty(str(map.get("status")), "Processing");
        int    itemCount      = intVal(map.get("itemCount"));
        double rawAmount      = dbl(map.get("totalAmountRaw"));
        long   createdAt      = lng(map.get("createdAt"));

        OrderItem order = new OrderItem(id, shopName, shopEmoji,
                dateStr, summary, totalStr, status, itemCount);
        order.shopId          = str(map.get("shopId"));
        order.totalAmountRaw  = rawAmount;
        order.createdAt       = createdAt;
        order.customerId      = str(map.get("customerId"));
        order.customerName    = str(map.get("customerName"));
        order.customerPhone   = str(map.get("customerPhone"));
        order.customerAddress = str(map.get("customerAddress"));
        order.fulfillmentType = str(map.get("fulfillmentType"));
        return order;
    }

    /**
     * Converts this order to a Firestore-compatible Map for writes.
     * Includes customerId/customerName so the admin app can identify the customer.
     *
     * @return Map suitable for Firestore set() / update()
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("shopId",          shopId != null ? shopId : "");
        map.put("shopName",        shopName);
        map.put("shopEmoji",       shopEmoji);
        map.put("orderDate",       orderDate);
        map.put("itemsSummary",    itemsSummary);
        map.put("totalAmount",     totalAmount);
        map.put("totalAmountRaw",  totalAmountRaw);
        map.put("status",          status);
        map.put("itemCount",       itemCount);
        map.put("createdAt",       createdAt > 0 ? createdAt : System.currentTimeMillis());
        map.put("customerId",      customerId != null ? customerId : "");
        map.put("customerName",    customerName != null ? customerName : "");
        map.put("customerPhone",   customerPhone != null ? customerPhone : "");
        map.put("customerAddress", customerAddress != null ? customerAddress : "");
        map.put("fulfillmentType", fulfillmentType != null ? fulfillmentType : "Delivery");
        return map;
    }

    // ── Private helpers ───────────────────────────────────────────────────────
    private static String str(Object v) { return v == null ? "" : String.valueOf(v).trim(); }
    private static int intVal(Object v) {
        if (v instanceof Number) return ((Number) v).intValue();
        try { return Integer.parseInt(String.valueOf(v)); } catch (Exception e) { return 0; }
    }
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
