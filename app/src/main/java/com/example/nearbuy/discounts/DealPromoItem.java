package com.example.nearbuy.discounts;

/**
 * DealPromoItem – data model shared by the DealsAndPromoActivity RecyclerView.
 * type = TYPE_DEAL for deals, TYPE_PROMO for promotions.
 */
public class DealPromoItem {

    public static final int TYPE_DEAL  = 0;
    public static final int TYPE_PROMO = 1;

    private final int    type;
    private final String emoji;
    private final String title;
    private final String shopName;
    private final String description;
    private final String badge;       // "30% OFF" / "2x Points" / "Free Delivery"
    private final String validity;    // "Expires Mar 10" / "2 days left"

    public DealPromoItem(int type, String emoji, String title,
                         String shopName, String description,
                         String badge, String validity) {
        this.type        = type;
        this.emoji       = emoji;
        this.title       = title;
        this.shopName    = shopName;
        this.description = description;
        this.badge       = badge;
        this.validity    = validity;
    }

    public int    getType()        { return type; }
    public String getEmoji()       { return emoji; }
    public String getTitle()       { return title; }
    public String getShopName()    { return shopName; }
    public String getDescription() { return description; }
    public String getBadge()       { return badge; }
    public String getValidity()    { return validity; }
}

