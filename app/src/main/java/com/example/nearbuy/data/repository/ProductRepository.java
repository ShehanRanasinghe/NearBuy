package com.example.nearbuy.data.repository;

import com.example.nearbuy.core.firebase.FirebaseConfig;
import com.example.nearbuy.data.model.Product;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

/**
 * ProductRepository – reads product documents written by NearBuyHQ shops.
 *
 * Products are stored at  NearBuyHQ/{shopId}/products/{productId}.
 * This repository provides customer-facing read access.
 *
 * Methods are stubs – Firebase query logic will be added in the backend phase.
 */
public class ProductRepository {

    private final FirebaseFirestore firestore;

    public ProductRepository() {
        this.firestore = FirebaseFirestore.getInstance();
    }

    // ── Products for a specific shop ──────────────────────────────────────────

    /**
     * Loads all available products for a specific shop.
     *
     * @param shopId   the shop's Firestore document ID
     * @param callback DataCallback<List<Product>>
     */
    public void getProductsByShop(String shopId, DataCallback<List<Product>> callback) {
        if (!FirebaseConfig.isFirebaseEnabled()) {
            callback.onError(new IllegalStateException("Firebase not enabled."));
            return;
        }
        // TODO: implement in the product backend phase
        callback.onError(new UnsupportedOperationException("getProductsByShop() – not yet implemented"));
    }

    // ── Product detail ────────────────────────────────────────────────────────

    /**
     * Loads a single product document.
     *
     * @param shopId    parent shop ID
     * @param productId product document ID
     * @param callback  DataCallback<Product>
     */
    public void getProductById(String shopId, String productId,
                               DataCallback<Product> callback) {
        if (!FirebaseConfig.isFirebaseEnabled()) {
            callback.onError(new IllegalStateException("Firebase not enabled."));
            return;
        }
        // TODO: implement in the product backend phase
        callback.onError(new UnsupportedOperationException("getProductById() – not yet implemented"));
    }

    // ── Search ────────────────────────────────────────────────────────────────

    /**
     * Searches for products by name across ALL shops, then enriches each result
     * with the parent shop's location and calculates distance from the customer.
     *
     * Results are sorted: nearest first, then by lowest price.
     *
     * @param query       the search term typed by the customer (e.g. "Apples")
     * @param customerLat customer's current latitude
     * @param customerLng customer's current longitude
     * @param radiusKm    maximum distance filter chosen by the customer
     * @param callback    DataCallback<List<Product>>
     */
    public void searchProducts(String query,
                               double customerLat, double customerLng,
                               float radiusKm,
                               DataCallback<List<Product>> callback) {
        if (!FirebaseConfig.isFirebaseEnabled()) {
            callback.onError(new IllegalStateException("Firebase not enabled."));
            return;
        }
        // TODO: implement in the search backend phase
        callback.onError(new UnsupportedOperationException("searchProducts() – not yet implemented"));
    }

    // ── Dashboard: hot products ───────────────────────────────────────────────

    /**
     * Loads the most recently added / featured products across all shops for
     * display in the Dashboard's "Hot Products" or "Nearby" section.
     *
     * @param customerLat latitude  of the customer
     * @param customerLng longitude of the customer
     * @param radiusKm    neighbourhood radius
     * @param limit       maximum number of products to return
     * @param callback    DataCallback<List<Product>>
     */
    public void getFeaturedProducts(double customerLat, double customerLng,
                                    float radiusKm, int limit,
                                    DataCallback<List<Product>> callback) {
        if (!FirebaseConfig.isFirebaseEnabled()) {
            callback.onError(new IllegalStateException("Firebase not enabled."));
            return;
        }
        // TODO: implement in the product backend phase
        callback.onError(new UnsupportedOperationException("getFeaturedProducts() – not yet implemented"));
    }
}

