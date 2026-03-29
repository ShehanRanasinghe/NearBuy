package com.example.nearbuy.search;
public class SearchResultItem {
    private String productName;
    private String shopName;
    private String emoji;
    private String price;
    private String originalPrice;
    private double distanceKm;
    private String category;
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
    public String getProductName()   { return productName; }
    public String getShopName()      { return shopName; }
    public String getEmoji()         { return emoji; }
    public String getPrice()         { return price; }
    public String getOriginalPrice() { return originalPrice; }
    public double getDistanceKm()    { return distanceKm; }
    public String getDistanceLabel() { return String.format("%.1f km", distanceKm); }
    public String getCategory()      { return category; }
}
