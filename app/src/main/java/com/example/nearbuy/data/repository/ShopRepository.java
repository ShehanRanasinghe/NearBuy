package com.example.nearbuy.data.repository;

import android.util.Log;

import com.example.nearbuy.core.firebase.FirebaseConfig;
import com.example.nearbuy.data.model.Shop;
import com.example.nearbuy.data.remote.firebase.FirebaseCollections;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * ShopRepository – reads shop documents that were written by the NearBuyHQ
 * admin app at  NearBuyHQ/{shopId}.
 *
 * This repository is READ-ONLY from the customer app's perspective.
 * Distance filtering is performed client-side using the Haversine formula
 * (see {@link SearchRepository#haversineKm}) because Firestore does not
 * natively support geo-radius queries without an additional indexing library.
 */
public class ShopRepository {

    private static final String TAG = "NearBuy.ShopRepo";

    private final FirebaseFirestore firestore;

    public ShopRepository() {
        this.firestore = FirebaseFirestore.getInstance();
    }

    // ── All active shops ───────────────────────────────────────────────────────

    /**
     * Loads every shop whose  status == "Active"  from the NearBuyHQ collection.
     * Results are unsorted; callers can sort by distance after enriching with
     * {@link Shop#setDistanceKm}.
     *
     * @param callback DataCallback<List<Shop>>
     */
    public void getAllActiveShops(DataCallback<List<Shop>> callback) {
        if (!FirebaseConfig.isFirebaseEnabled()) {
            callback.onError(new IllegalStateException("Firebase is not enabled."));
            return;
        }

        firestore.collection(FirebaseCollections.SHOPS)
                .whereEqualTo("status", "Active")
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Shop> shops = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        Shop shop = Shop.fromMap(doc.getId(), doc.getData());
                        if (shop != null) shops.add(shop);
                    }
                    if (!shops.isEmpty()) {
                        Log.d(TAG, "Loaded " + shops.size() + " active shops.");
                        callback.onSuccess(shops);
                    } else {
                        // Fallback: load ALL shops regardless of status
                        Log.d(TAG, "No 'Active' shops found – loading all shops.");
                        firestore.collection(FirebaseCollections.SHOPS)
                                .get()
                                .addOnSuccessListener(snap2 -> {
                                    List<Shop> all = new ArrayList<>();
                                    for (QueryDocumentSnapshot doc : snap2) {
                                        Shop s = Shop.fromMap(doc.getId(), doc.getData());
                                        if (s != null) all.add(s);
                                    }
                                    Log.d(TAG, "Fallback: loaded " + all.size() + " total shops.");
                                    callback.onSuccess(all);
                                })
                                .addOnFailureListener(callback::onError);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load shops", e);
                    callback.onError(e);
                });
    }

    // ── Single shop ────────────────────────────────────────────────────────────

    /**
     * Loads a single shop document by its Firestore document ID.
     * Used by StoreDetailsActivity to display full shop information.
     *
     * @param shopId   Firestore document ID (equals ownerUid in NearBuyHQ)
     * @param callback DataCallback<Shop>
     */
    public void getShopById(String shopId, DataCallback<Shop> callback) {
        if (!FirebaseConfig.isFirebaseEnabled()) {
            callback.onError(new IllegalStateException("Firebase is not enabled."));
            return;
        }

        firestore.collection(FirebaseCollections.SHOPS)
                .document(shopId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        callback.onError(new Exception("Shop not found: " + shopId));
                        return;
                    }
                    Shop shop = Shop.fromMap(doc.getId(), doc.getData());
                    if (shop == null) {
                        callback.onError(new Exception("Failed to parse shop document."));
                        return;
                    }
                    callback.onSuccess(shop);
                })
                .addOnFailureListener(callback::onError);
    }

    // ── Nearby shops ───────────────────────────────────────────────────────────

    /**
     * Loads all active shops and filters them to those within {@code radiusKm}
     * of the customer's position.  Distance is calculated client-side using the
     * Haversine formula and attached to each shop via {@link Shop#setDistanceKm}.
     *
     * Results are sorted by distance ascending (nearest first).
     *
     * @param customerLat latitude  of the customer
     * @param customerLng longitude of the customer
     * @param radiusKm    maximum distance in kilometres
     * @param callback    DataCallback<List<Shop>>
     */
    public void getNearbyShops(double customerLat, double customerLng,
                               float radiusKm, DataCallback<List<Shop>> callback) {
        if (!FirebaseConfig.isFirebaseEnabled()) {
            callback.onError(new IllegalStateException("Firebase is not enabled."));
            return;
        }

        // If customer location is unknown, return ALL active shops without distance filter
        boolean noLocation = (customerLat == 0.0 && customerLng == 0.0);
        if (noLocation) {
            Log.d(TAG, "No customer location – loading all active shops.");
            getAllActiveShops(callback);
            return;
        }

        // Load all active shops, then filter + sort client-side
        getAllActiveShops(new DataCallback<List<Shop>>() {
            @Override
            public void onSuccess(List<Shop> allShops) {
                List<Shop> nearby = new ArrayList<>();

                for (Shop shop : allShops) {
                    if (!shop.hasLocation()) {
                        // Shop has no GPS – include it without a distance label
                        nearby.add(shop);
                        continue;
                    }

                    double dist = SearchRepository.haversineKm(
                            customerLat, customerLng,
                            shop.getLatitude(), shop.getLongitude());

                    if (dist <= radiusKm) {
                        shop.setDistanceKm(dist);
                        nearby.add(shop);
                    }
                }

                // If nothing found within radius, fall back to ALL shops
                if (nearby.isEmpty()) {
                    Log.d(TAG, "No shops in " + radiusKm + " km – returning all active shops.");
                    for (Shop shop : allShops) {
                        if (shop.hasLocation()) {
                            double dist = SearchRepository.haversineKm(
                                    customerLat, customerLng,
                                    shop.getLatitude(), shop.getLongitude());
                            shop.setDistanceKm(dist);
                        }
                        nearby.add(shop);
                    }
                }

                // Sort ascending by distance (shops without distance go last)
                nearby.sort((a, b) -> {
                    if (!a.hasDistance() && !b.hasDistance()) return 0;
                    if (!a.hasDistance()) return 1;
                    if (!b.hasDistance()) return -1;
                    return Double.compare(a.getDistanceKm(), b.getDistanceKm());
                });

                Log.d(TAG, "Returning " + nearby.size() + " shops.");
                callback.onSuccess(nearby);
            }

            @Override
            public void onError(Exception e) {
                callback.onError(e);
            }
        });
    }
}
