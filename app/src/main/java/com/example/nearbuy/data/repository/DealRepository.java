package com.example.nearbuy.data.repository;

import android.util.Log;

import com.example.nearbuy.core.firebase.FirebaseConfig;
import com.example.nearbuy.data.model.DealItem;
import com.example.nearbuy.data.model.Shop;
import com.example.nearbuy.data.remote.firebase.FirebaseCollections;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * DealRepository – reads deal and promotion documents published by shops through
 * the NearBuyHQ admin app, and manages the customer's saved-deals sub-collection.
 *
 * Firestore paths:
 *   NearBuyHQ/{shopId}/deals/{dealId}           ← shop deals (READ)
 *   NearBuyHQ/{shopId}/promotions/{promoId}      ← shop promotions (READ)
 *   NearBuy/{customerId}/saved_deals/{dealId}    ← customer saved deals (READ/WRITE)
 *
 * Strategy: load active shops first, then fan out to each shop's deals/promotions
 * sub-collection in parallel using AtomicInteger to track completion.
 */
public class DealRepository {

    private static final String TAG = "NearBuy.DealRepo";

    private final FirebaseFirestore firestore;
    private final ShopRepository    shopRepo;

    public DealRepository() {
        this.firestore = FirebaseFirestore.getInstance();
        this.shopRepo  = new ShopRepository();
    }

    // ── Dashboard: latest deals ────────────────────────────────────────────────

    /**
     * Loads the most recently created active deals across all shops within the
     * customer's neighbourhood.  Used for the "Latest Deals" section on the Dashboard.
     *
     * @param customerLat latitude  of the customer
     * @param customerLng longitude of the customer
     * @param radiusKm    neighbourhood radius in kilometres
     * @param limit       maximum number of deals to return
     * @param callback    DataCallback<List<DealItem>>
     */
    public void getLatestDeals(double customerLat, double customerLng,
                               float radiusKm, int limit,
                               DataCallback<List<DealItem>> callback) {
        fetchDealsFromNearbyShops(customerLat, customerLng, radiusKm,
                FirebaseCollections.SHOP_DEALS, false, limit, callback);
    }

    // ── Dashboard: latest promotions ───────────────────────────────────────────

    /**
     * Loads the most recently created active promotions across all nearby shops.
     * Used for the "Active Promotions" section on the Dashboard.
     *
     * @param customerLat latitude  of the customer
     * @param customerLng longitude of the customer
     * @param radiusKm    neighbourhood radius in kilometres
     * @param limit       maximum number of promotions to return
     * @param callback    DataCallback<List<DealItem>>
     */
    public void getLatestPromotions(double customerLat, double customerLng,
                                    float radiusKm, int limit,
                                    DataCallback<List<DealItem>> callback) {
        fetchDealsFromNearbyShops(customerLat, customerLng, radiusKm,
                FirebaseCollections.SHOP_PROMOTIONS, true, limit, callback);
    }

    // ── Full deal listing ──────────────────────────────────────────────────────

    /**
     * Loads ALL active deals (and optionally promotions) from nearby shops.
     * Used in DealsActivity for the complete deals list.
     *
     * @param customerLat   customer latitude
     * @param customerLng   customer longitude
     * @param radiusKm      filter radius
     * @param includePromos whether to merge promotions alongside deals
     * @param callback      DataCallback<List<DealItem>>
     */
    public void getAllDealsNearby(double customerLat, double customerLng,
                                  float radiusKm, boolean includePromos,
                                  DataCallback<List<DealItem>> callback) {
        if (!includePromos) {
            fetchDealsFromNearbyShops(customerLat, customerLng, radiusKm,
                    FirebaseCollections.SHOP_DEALS, false, Integer.MAX_VALUE, callback);
            return;
        }

        // Merge deals + promotions into one list
        List<DealItem> combined = new ArrayList<>();
        AtomicInteger  pending  = new AtomicInteger(2);

        DataCallback<List<DealItem>> mergeCallback = new DataCallback<List<DealItem>>() {
            @Override
            public void onSuccess(List<DealItem> items) {
                synchronized (combined) { combined.addAll(items); }
                if (pending.decrementAndGet() == 0) {
                    // Sort by creation date descending (newest first)
                    combined.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
                    callback.onSuccess(combined);
                }
            }
            @Override
            public void onError(Exception e) {
                if (pending.decrementAndGet() == 0) callback.onSuccess(combined); // return partial
            }
        };

        fetchDealsFromNearbyShops(customerLat, customerLng, radiusKm,
                FirebaseCollections.SHOP_DEALS, false, Integer.MAX_VALUE, mergeCallback);
        fetchDealsFromNearbyShops(customerLat, customerLng, radiusKm,
                FirebaseCollections.SHOP_PROMOTIONS, true, Integer.MAX_VALUE, mergeCallback);
    }

    // ── Deals for a specific shop ──────────────────────────────────────────────

    /**
     * Loads all active deals belonging to a single shop.
     * Used in StoreDetailsActivity to show deals for that shop.
     *
     * @param shopId   shop document ID
     * @param callback DataCallback<List<DealItem>>
     */
    public void getDealsByShop(String shopId, DataCallback<List<DealItem>> callback) {
        if (!FirebaseConfig.isFirebaseEnabled()) {
            callback.onError(new IllegalStateException("Firebase is not enabled."));
            return;
        }

        firestore.collection(FirebaseCollections.SHOPS)
                .document(shopId)
                .collection(FirebaseCollections.SHOP_DEALS)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<DealItem> deals = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        DealItem deal = DealItem.fromMap(doc.getId(), shopId, doc.getData());
                        if (deal != null) deals.add(deal);
                    }
                    callback.onSuccess(deals);
                })
                .addOnFailureListener(callback::onError);
    }

    /**
     * Loads all promotions belonging to a single shop.
     * Used in StoreDetailsActivity to show promotions for that shop.
     *
     * @param shopId   shop document ID
     * @param callback DataCallback<List<DealItem>>
     */
    public void getPromotionsByShop(String shopId, DataCallback<List<DealItem>> callback) {
        if (!FirebaseConfig.isFirebaseEnabled()) {
            callback.onError(new IllegalStateException("Firebase is not enabled."));
            return;
        }

        firestore.collection(FirebaseCollections.SHOPS)
                .document(shopId)
                .collection(FirebaseCollections.SHOP_PROMOTIONS)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<DealItem> promos = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        DealItem promo = DealItem.fromMap(doc.getId(), shopId, doc.getData());
                        if (promo != null) promos.add(promo);
                    }
                    callback.onSuccess(promos);
                })
                .addOnFailureListener(callback::onError);
    }

    // ── Saved deals ────────────────────────────────────────────────────────────

    /**
     * Loads all deals the customer has bookmarked, stored at
     * NearBuy/{customerId}/saved_deals.
     *
     * @param customerId customer UID
     * @param callback   DataCallback<List<DealItem>>
     */
    public void getSavedDeals(String customerId, DataCallback<List<DealItem>> callback) {
        if (!FirebaseConfig.isFirebaseEnabled()) {
            callback.onError(new IllegalStateException("Firebase is not enabled."));
            return;
        }

        firestore.collection(FirebaseCollections.CUSTOMERS)
                .document(customerId)
                .collection(FirebaseCollections.CUSTOMER_SAVED_DEALS)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<DealItem> saved = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        DealItem deal = DealItem.fromMap(doc.getId(), "", doc.getData());
                        if (deal != null) saved.add(deal);
                    }
                    callback.onSuccess(saved);
                })
                .addOnFailureListener(callback::onError);
    }

    /**
     * Saves (bookmarks) a deal under the customer's saved_deals sub-collection.
     *
     * @param customerId customer UID
     * @param deal       the deal to bookmark
     * @param callback   OperationCallback
     */
    public void saveDeal(String customerId, DealItem deal, OperationCallback callback) {
        if (!FirebaseConfig.isFirebaseEnabled()) {
            callback.onError(new IllegalStateException("Firebase is not enabled."));
            return;
        }

        firestore.collection(FirebaseCollections.CUSTOMERS)
                .document(customerId)
                .collection(FirebaseCollections.CUSTOMER_SAVED_DEALS)
                .document(deal.getId())
                .set(deal.toMap())
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }

    /**
     * Removes a bookmarked deal from the customer's saved_deals sub-collection.
     *
     * @param customerId customer UID
     * @param dealId     Firestore document ID of the deal to remove
     * @param callback   OperationCallback
     */
    public void removeSavedDeal(String customerId, String dealId, OperationCallback callback) {
        if (!FirebaseConfig.isFirebaseEnabled()) {
            callback.onError(new IllegalStateException("Firebase is not enabled."));
            return;
        }

        firestore.collection(FirebaseCollections.CUSTOMERS)
                .document(customerId)
                .collection(FirebaseCollections.CUSTOMER_SAVED_DEALS)
                .document(dealId)
                .delete()
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }

    // ── Private helper ─────────────────────────────────────────────────────────

    /**
     * Core fan-out query: loads all active shops within radius, then in parallel
     * fetches their {@code subCollection} (deals or promotions), enriches each
     * item with shop location info, and returns the merged list.
     *
     * @param subCollection FirebaseCollections.SHOP_DEALS or SHOP_PROMOTIONS
     * @param isPromo       true when loading promotions (sets DealItem.isPromotion)
     * @param limit         cap on the number of results (sorted newest-first)
     */
    private void fetchDealsFromNearbyShops(double customerLat, double customerLng,
                                           float radiusKm, String subCollection,
                                           boolean isPromo, int limit,
                                           DataCallback<List<DealItem>> callback) {
        if (!FirebaseConfig.isFirebaseEnabled()) {
            callback.onError(new IllegalStateException("Firebase is not enabled."));
            return;
        }

        // Step 1: get nearby shops
        shopRepo.getNearbyShops(customerLat, customerLng, radiusKm,
                new DataCallback<List<Shop>>() {
                    @Override
                    public void onSuccess(List<Shop> shops) {
                        if (shops.isEmpty()) {
                            callback.onSuccess(new ArrayList<>());
                            return;
                        }

                        List<DealItem>  results = new ArrayList<>();
                        AtomicInteger   pending = new AtomicInteger(shops.size());

                        // Step 2: fan out to each shop's sub-collection in parallel
                        for (Shop shop : shops) {
                            firestore.collection(FirebaseCollections.SHOPS)
                                    .document(shop.getId())
                                    .collection(subCollection)
                                    .orderBy("createdAt", Query.Direction.DESCENDING)
                                    .get()
                                    .addOnSuccessListener(snapshot -> {
                                        for (QueryDocumentSnapshot doc : snapshot) {
                                            DealItem item = DealItem.fromMap(
                                                    doc.getId(), shop.getId(), doc.getData());
                                            if (item == null) continue;
                                            // Enrich with shop location data
                                            item.setShopName(shop.getName());
                                            item.setShopLocation(shop.getShopLocation());
                                            item.setShopLatitude(shop.getLatitude());
                                            item.setShopLongitude(shop.getLongitude());
                                            item.setDistanceKm(shop.getDistanceKm());
                                            synchronized (results) { results.add(item); }
                                        }
                                        checkAndReturn(pending, results, limit, callback);
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.w(TAG, "Failed to load " + subCollection
                                                + " for shop " + shop.getId(), e);
                                        checkAndReturn(pending, results, limit, callback);
                                    });
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        callback.onError(e);
                    }
                });
    }

    /** Decrements the pending counter and fires callback when all shops have responded. */
    private void checkAndReturn(AtomicInteger pending, List<DealItem> results,
                                int limit, DataCallback<List<DealItem>> callback) {
        if (pending.decrementAndGet() == 0) {
            // Sort newest first, then cap to limit
            results.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
            List<DealItem> capped = results.size() > limit
                    ? results.subList(0, limit) : results;
            callback.onSuccess(new ArrayList<>(capped));
        }
    }
}
