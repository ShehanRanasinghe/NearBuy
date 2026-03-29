package com.example.nearbuy.orders;

/**
 * Data model representing a customer order.
 */
public class OrderItem {
    private String orderId;
    private String shopName;
    private String shopEmoji;
    private String orderDate;
    private String itemsSummary;   // e.g. "Apples x2, Milk x1"
    private String totalAmount;
    private String status;          // "Delivered", "Processing", "Cancelled"
    private int    itemCount;

    public OrderItem(String orderId, String shopName, String shopEmoji,
                     String orderDate, String itemsSummary,
                     String totalAmount, String status, int itemCount) {
        this.orderId       = orderId;
        this.shopName      = shopName;
        this.shopEmoji     = shopEmoji;
        this.orderDate     = orderDate;
        this.itemsSummary  = itemsSummary;
        this.totalAmount   = totalAmount;
        this.status        = status;
        this.itemCount     = itemCount;
    }

    public String getOrderId()      { return orderId; }
    public String getShopName()     { return shopName; }
    public String getShopEmoji()    { return shopEmoji; }
    public String getOrderDate()    { return orderDate; }
    public String getItemsSummary() { return itemsSummary; }
    public String getTotalAmount()  { return totalAmount; }
    public String getStatus()       { return status; }
    public int    getItemCount()    { return itemCount; }
}

