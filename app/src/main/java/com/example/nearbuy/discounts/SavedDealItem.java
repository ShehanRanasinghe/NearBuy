package com.example.nearbuy.discounts;

/**
 * SavedDealItem – lightweight UI model used to populate the SavedDealsActivity
 * RecyclerView cards.
 *
 * Populated from a {@link com.example.nearbuy.data.model.DealItem} object returned
 * by {@link com.example.nearbuy.data.repository.DealRepository#getSavedDeals}.
 *
 * The {@code id} field holds the Firestore document ID so that remove-deal
 * calls can reference the correct document in the customer's saved_deals sub-collection.
 */
public class SavedDealItem {

    // Firestore document ID – needed to delete the saved deal
    private String id;

    private String emoji;
    private String dealName;
    private String shopName;
    private String discountLabel;
    private String dealPrice;
    private String originalPrice;
    private String expiryLabel;
    private String category;
    private double distanceKm;

    /** Constructor for legacy / manual creation (id defaults to empty string). */
    public SavedDealItem(String emoji, String dealName, String shopName,
                         String discountLabel, String dealPrice, String originalPrice,
                         String expiryLabel, String category, double distanceKm) {
        this("", emoji, dealName, shopName, discountLabel, dealPrice,
                originalPrice, expiryLabel, category, distanceKm);
    }

    /** Full constructor used when converting from a Firestore-backed DealItem. */
    public SavedDealItem(String id, String emoji, String dealName, String shopName,
                         String discountLabel, String dealPrice, String originalPrice,
                         String expiryLabel, String category, double distanceKm) {
        this.id            = id;
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

    // ── Getters ───────────────────────────────────────────────────────────────
    public String getId()            { return id; }
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
