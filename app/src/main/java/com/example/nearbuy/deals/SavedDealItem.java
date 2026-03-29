package com.example.nearbuy.deals;

/**
 * Data model for a Saved Deal card shown in SavedDealsActivity.
 */
public class SavedDealItem {
    private String emoji;
    private String dealName;
    private String shopName;
    private String discountLabel;
    private String dealPrice;
    private String originalPrice;
    private String expiryLabel;
    private String category;
    private double distanceKm;

    public SavedDealItem(String emoji, String dealName, String shopName,
                         String discountLabel, String dealPrice, String originalPrice,
                         String expiryLabel, String category, double distanceKm) {
        this.emoji         = emoji;
        this.dealName      = dealName;
        this.shopName      = shopName;
        this.discountLabel = discountLabel;
        this.dealPrice     = dealPrice;
        this.originalPrice = originalPrice;
        this.expiryLabel   = expiryLabel;
        this.category      = category;
        this.distanceKm    = distanceKm;
    }

    public String getEmoji()         { return emoji; }
    public String getDealName()      { return dealName; }
    public String getShopName()      { return shopName; }
    public String getDiscountLabel() { return discountLabel; }
    public String getDealPrice()     { return dealPrice; }
    public String getOriginalPrice() { return originalPrice; }
    public String getExpiryLabel()   { return expiryLabel; }
    public String getCategory()      { return category; }
    public double getDistanceKm()    { return distanceKm; }
    public String getDistanceLabel() { return String.format("%.1f km", distanceKm); }
}

