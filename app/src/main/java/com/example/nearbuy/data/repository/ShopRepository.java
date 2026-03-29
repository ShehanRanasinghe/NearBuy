package com.example.nearbuy.data.repository;

import com.example.nearbuy.core.firebase.FirebaseConfig;
import com.example.nearbuy.data.model.Shop;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

/**
 * ShopRepository – reads shop documents written by the NearBuyHQ admin app.
 *
 * All shops are stored at  NearBuyHQ/{shopId}  by shop owners.
 * This repository provides read-only access for the customer app.
 *
 * Methods are stubs – Firebase query logic will be added in the backend phase.
 */
public class ShopRepository {

    private final FirebaseFirestore firestore;

    public ShopRepository() {
        this.firestore = FirebaseFirestore.getInstance();
    }

    // ── Fetch all active shops ─────────────────────────────────────────────────

    /**
     * Loads all shops with status == "Active" from the NearBuyHQ root collection.
     *
     * @param callback DataCallback<List<Shop>>
     */
    public void getAllActiveShops(DataCallback<List<Shop>> callback) {
        if (!FirebaseConfig.isFirebaseEnabled()) {
            callback.onError(new IllegalStateException("Firebase not enabled."));
            return;
        }
        // TODO: implement in the shop backend phase
        callback.onError(new UnsupportedOperationException("getAllActiveShops() – not yet implemented"));
    }

    // ── Fetch a single shop ────────────────────────────────────────────────────

    /**
     * Loads a single shop document by its ID.
     *
     * @param shopId   Firestore document ID (== ownerUid in NearBuyHQ)
     * @param callback DataCallback<Shop>
     */
    public void getShopById(String shopId, DataCallback<Shop> callback) {
        if (!FirebaseConfig.isFirebaseEnabled()) {
            callback.onError(new IllegalStateException("Firebase not enabled."));
            return;
        }
        // TODO: implement in the shop backend phase
        callback.onError(new UnsupportedOperationException("getShopById() – not yet implemented"));
    }

    // ── Nearby shops ──────────────────────────────────────────────────────────

    /**
     * Loads all active shops whose GPS coordinates fall within {@code radiusKm}
     * of the customer's current position.
     *
     * Distance filtering is done client-side after loading all active shops
     * (Firestore does not natively support geo-radius queries without an
     *  external library like GeoFirestore or geohash indexing).
     *
     * @param customerLat latitude of the customer
     * @param customerLng longitude of the customer
     * @param radiusKm    maximum distance in kilometres
     * @param callback    DataCallback<List<Shop>>
     */
    public void getNearbyShops(double customerLat, double customerLng,
                               float radiusKm, DataCallback<List<Shop>> callback) {
        if (!FirebaseConfig.isFirebaseEnabled()) {
            callback.onError(new IllegalStateException("Firebase not enabled."));
            return;
        }
        // TODO: implement in the shop backend phase
        callback.onError(new UnsupportedOperationException("getNearbyShops() – not yet implemented"));
    }
}

