package com.example.nearbuy.discounts;

/**
 * Data model for a Deal or Promotion card.
 */
public class Deal {
    private String emoji;
    private String title;
    private String shopName;
    private String discountLabel;  // e.g. "50% OFF"
    private String originalPrice;
    private String salePrice;
    private String category;
    private double distanceKm;
    private int    expiryDays;
    private boolean isPromotion;   // true = promotion, false = deal

    public Deal(String emoji, String title, String shopName, String discountLabel,
                String originalPrice, String salePrice, String category,
                double distanceKm, int expiryDays, boolean isPromotion) {
        this.emoji         = emoji;
        this.title         = title;
        this.shopName      = shopName;
        this.discountLabel = discountLabel;
        this.originalPrice = originalPrice;
        this.salePrice     = salePrice;
        this.category      = category;
        this.distanceKm    = distanceKm;
        this.expiryDays    = expiryDays;
        this.isPromotion   = isPromotion;
    }

    public String  getEmoji()         { return emoji; }
    public String  getTitle()         { return title; }
    public String  getShopName()      { return shopName; }
    public String  getDiscountLabel() { return discountLabel; }
    public String  getOriginalPrice() { return originalPrice; }
    public String  getSalePrice()     { return salePrice; }
    public String  getCategory()      { return category; }
    public double  getDistanceKm()    { return distanceKm; }
    public String  getDistanceLabel() { return String.format("%.1f km", distanceKm); }
    public int     getExpiryDays()    { return expiryDays; }
    public boolean isPromotion()      { return isPromotion; }
    public String  getExpiryLabel()   {
        if (expiryDays == 0) return "Expires Today!";
        if (expiryDays == 1) return "Expires Tomorrow";
        return "Expires in " + expiryDays + " days";
    }
}

