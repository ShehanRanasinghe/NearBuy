package com.example.nearbuy.search;

/**
 * SearchResultItem – UI model for a single row in the search results list.
 * Carries both display data and Firestore IDs so ProductDetailsActivity
 * can load the full product when a result is tapped.
 */
public class SearchResultItem {
    private final String productName;
    private final String shopName;
    private final String emoji;
    private final String price;
    private final String originalPrice;
    private final double distanceKm;
    private final String category;
    // Firestore IDs – needed to navigate to full product details
    private String shopId;
    private String productId;

    public SearchResultItem(String productName, String shopName, String emoji,
                            String price, String originalPrice, double distanceKm, String category) {
        this.productName   = productName;
        this.shopName      = shopName;
        this.emoji         = emoji;
        this.price         = price;
        this.originalPrice = originalPrice;
        this.distanceKm    = distanceKm;
        this.category      = category;
    }

    // ── Setters for Firestore IDs ─────────────────────────────────────────────
    public void setShopId(String shopId)       { this.shopId    = shopId; }
    public void setProductId(String productId) { this.productId = productId; }

    // ── Getters ───────────────────────────────────────────────────────────────
    public String getProductName()   { return productName; }
    public String getShopName()      { return shopName; }
    public String getEmoji()         { return emoji; }
    public String getPrice()         { return price; }
    public String getOriginalPrice() { return originalPrice; }
    public double getDistanceKm()    { return distanceKm; }
    public String getCategory()      { return category; }
    public String getShopId()        { return shopId != null ? shopId : ""; }
    public String getProductId()     { return productId != null ? productId : ""; }
    public String getDistanceLabel() {
        if (distanceKm <= 0) return "Nearby";
        if (distanceKm < 1.0) return String.format("%.0f m", distanceKm * 1000);
        return String.format("%.1f km", distanceKm);
    }
}
