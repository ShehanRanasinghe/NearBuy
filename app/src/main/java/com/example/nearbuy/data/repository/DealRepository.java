package com.example.nearbuy.data.repository;

import com.example.nearbuy.core.firebase.FirebaseConfig;
import com.example.nearbuy.data.model.DealItem;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

/**
 * DealRepository – reads deal and promotion documents from NearBuyHQ shops.
 *
 * Deals:      NearBuyHQ/{shopId}/deals/{dealId}
 * Promotions: NearBuyHQ/{shopId}/promotions/{promoId}
 *
 * Also manages the customer's saved deals sub-collection:
 *   NearBuy/{customerId}/saved_deals/{dealId}
 *
 * Methods are stubs – Firebase query logic will be added in the backend phase.
 */
public class DealRepository {

    private final FirebaseFirestore firestore;

    public DealRepository() {
        this.firestore = FirebaseFirestore.getInstance();
    }

    // ── Dashboard: latest deals ───────────────────────────────────────────────

    /**
     * Loads the most recently created active deals across all shops near the customer.
     * Used to populate the "Latest Deals" section on the Dashboard.
     *
     * @param customerLat latitude  of the customer
     * @param customerLng longitude of the customer
     * @param radiusKm    neighbourhood radius
     * @param limit       max number of deals to return
     * @param callback    DataCallback<List<DealItem>>
     */
    public void getLatestDeals(double customerLat, double customerLng,
                               float radiusKm, int limit,
                               DataCallback<List<DealItem>> callback) {
        if (!FirebaseConfig.isFirebaseEnabled()) {
            callback.onError(new IllegalStateException("Firebase not enabled."));
            return;
        }
        // TODO: implement in the deals backend phase
        callback.onError(new UnsupportedOperationException("getLatestDeals() – not yet implemented"));
    }

    // ── Dashboard: latest promotions ──────────────────────────────────────────

    /**
     * Loads the most recently created active promotions across all nearby shops.
     * Used to populate the "Promotions" section on the Dashboard.
     *
     * @param customerLat latitude  of the customer
     * @param customerLng longitude of the customer
     * @param radiusKm    neighbourhood radius
     * @param limit       max number of promotions to return
     * @param callback    DataCallback<List<DealItem>>
     */
    public void getLatestPromotions(double customerLat, double customerLng,
                                    float radiusKm, int limit,
                                    DataCallback<List<DealItem>> callback) {
        if (!FirebaseConfig.isFirebaseEnabled()) {
            callback.onError(new IllegalStateException("Firebase not enabled."));
            return;
        }
        // TODO: implement in the deals backend phase
        callback.onError(new UnsupportedOperationException("getLatestPromotions() – not yet implemented"));
    }

    // ── Full deal listing ─────────────────────────────────────────────────────

    /**
     * Loads ALL active deals (and optionally promotions) within the customer's radius.
     * Used in DealsActivity.
     *
     * @param customerLat  customer latitude
     * @param customerLng  customer longitude
     * @param radiusKm     filter radius
     * @param includePromos include promotions alongside deals
     * @param callback     DataCallback<List<DealItem>>
     */
    public void getAllDealsNearby(double customerLat, double customerLng,
                                  float radiusKm, boolean includePromos,
                                  DataCallback<List<DealItem>> callback) {
        if (!FirebaseConfig.isFirebaseEnabled()) {
            callback.onError(new IllegalStateException("Firebase not enabled."));
            return;
        }
        // TODO: implement in the deals backend phase
        callback.onError(new UnsupportedOperationException("getAllDealsNearby() – not yet implemented"));
    }

    // ── Deals for a specific shop ─────────────────────────────────────────────

    /**
     * Loads all active deals belonging to a specific shop.
     * Used in StoreDetailsActivity.
     *
     * @param shopId   shop document ID
     * @param callback DataCallback<List<DealItem>>
     */
    public void getDealsByShop(String shopId, DataCallback<List<DealItem>> callback) {
        if (!FirebaseConfig.isFirebaseEnabled()) {
            callback.onError(new IllegalStateException("Firebase not enabled."));
            return;
        }
        // TODO: implement in the deals backend phase
        callback.onError(new UnsupportedOperationException("getDealsByShop() – not yet implemented"));
    }

    // ── Saved deals ───────────────────────────────────────────────────────────

    /**
     * Loads the deals saved/bookmarked by the customer.
     * Stored at  NearBuy/{customerId}/saved_deals.
     *
     * @param customerId customer UID
     * @param callback   DataCallback<List<DealItem>>
     */
    public void getSavedDeals(String customerId, DataCallback<List<DealItem>> callback) {
        if (!FirebaseConfig.isFirebaseEnabled()) {
            callback.onError(new IllegalStateException("Firebase not enabled."));
            return;
        }
        // TODO: implement in the deals backend phase
        callback.onError(new UnsupportedOperationException("getSavedDeals() – not yet implemented"));
    }

    /**
     * Saves (bookmarks) a deal for the customer.
     *
     * @param customerId customer UID
     * @param deal       the deal to save
     * @param callback   OperationCallback
     */
    public void saveDeal(String customerId, DealItem deal, OperationCallback callback) {
        if (!FirebaseConfig.isFirebaseEnabled()) {
            callback.onError(new IllegalStateException("Firebase not enabled."));
            return;
        }
        // TODO: implement in the deals backend phase
        callback.onError(new UnsupportedOperationException("saveDeal() – not yet implemented"));
    }

    /**
     * Removes a previously saved deal from the customer's saved_deals sub-collection.
     *
     * @param customerId customer UID
     * @param dealId     deal document ID to remove
     * @param callback   OperationCallback
     */
    public void removeSavedDeal(String customerId, String dealId, OperationCallback callback) {
        if (!FirebaseConfig.isFirebaseEnabled()) {
            callback.onError(new IllegalStateException("Firebase not enabled."));
            return;
        }
        // TODO: implement in the deals backend phase
        callback.onError(new UnsupportedOperationException("removeSavedDeal() – not yet implemented"));
    }
}

