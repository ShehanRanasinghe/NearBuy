package com.example.nearbuy.data.remote.firebase;

/**
 * FirebaseCollections – all Firestore collection / sub-collection names used by
 * the NearBuy customer app.
 *
 * Shared collections (written by NearBuyHQ admin app, READ by this app):
 * <pre>
 *   NearBuyHQ/                          ← SHOPS root (NearBuyHQ collection)
 *    └── {shopId}/                      ← one document per shop owner / shop
 *         ├── products/                 ← SHOP_PRODUCTS  – products listed by the shop
 *         ├── deals/                    ← SHOP_DEALS     – active deals
 *         └── promotions/              ← SHOP_PROMOTIONS – active promotions
 * </pre>
 *
 * Customer-owned collections (written AND read by this app):
 * <pre>
 *   NearBuy/                            ← CUSTOMERS root
 *    └── {customerId}/                  ← one document per registered customer
 *         ├── orders/                   ← CUSTOMER_ORDERS
 *         ├── saved_deals/              ← CUSTOMER_SAVED_DEALS
 *         └── notifications/           ← CUSTOMER_NOTIFICATIONS
 * </pre>
 *
 * Top-level utility collections:
 * <pre>
 *   otp_codes/   ← OTP_CODES – pre-auth OTP verification codes
 * </pre>
 */
public final class FirebaseCollections {

    private FirebaseCollections() { /* utility class */ }

    // ── Shared: Shop collections (owned by NearBuyHQ, read by NearBuy) ────────
    /** Root collection for shop documents – mirrors NearBuyHQ's root collection. */
    public static final String SHOPS             = "NearBuyHQ";      // NearBuyHQ/{shopId}

    /** Products sub-collection inside each shop document. */
    public static final String SHOP_PRODUCTS     = "products";       // NearBuyHQ/{shopId}/products

    /** Deals sub-collection inside each shop document. */
    public static final String SHOP_DEALS        = "deals";          // NearBuyHQ/{shopId}/deals

    /** Promotions sub-collection inside each shop document. */
    public static final String SHOP_PROMOTIONS   = "promotions";     // NearBuyHQ/{shopId}/promotions

    /**
     * Orders sub-collection inside each shop document.
     * Written by the customer app; read by both NearBuyHQ admin and customer app.
     * Firestore path: NearBuyHQ/{shopId}/orders/{orderId}
     */
    public static final String SHOP_ORDERS       = "orders";         // NearBuyHQ/{shopId}/orders

    /**
     * Reports sub-collection inside each shop document.
     * Written by customer app when a user submits an order report.
     * One report per order (orderId used as document ID).
     * Firestore path: NearBuyHQ/{shopId}/reports/{orderId}
     */
    public static final String SHOP_REPORTS      = "reports";        // NearBuyHQ/{shopId}/reports

    // ── Customer-owned collections ─────────────────────────────────────────────
    /** Root collection for customer profile documents. */
    public static final String CUSTOMERS               = "NearBuy";             // NearBuy/{customerId}

    /** Orders placed by a customer. */
    public static final String CUSTOMER_ORDERS         = "orders";              // NearBuy/{cid}/orders

    /** Deals saved/bookmarked by a customer. */
    public static final String CUSTOMER_SAVED_DEALS    = "saved_deals";         // NearBuy/{cid}/saved_deals

    /** Notifications delivered to a customer. */
    public static final String CUSTOMER_NOTIFICATIONS  = "notifications";       // NearBuy/{cid}/notifications

    // ── Top-level utility ──────────────────────────────────────────────────────
    /**
     * OTP verification codes – written before a user is authenticated,
     * so they live at the root level rather than inside a user document.
     */
    public static final String OTP_CODES = "otp_codes";
}

