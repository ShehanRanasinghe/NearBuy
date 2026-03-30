package com.example.nearbuy.data.repository;

import android.util.Log;

import com.example.nearbuy.core.firebase.FirebaseConfig;
import com.example.nearbuy.data.model.Product;
import com.example.nearbuy.data.model.Shop;
import com.example.nearbuy.data.remote.firebase.FirebaseCollections;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ProductRepository – reads product documents written by NearBuyHQ shops.
 *
 * Firestore path: NearBuyHQ/{shopId}/products/{productId}
 *
 * The customer app only reads product data – all writes are performed
 * through the companion NearBuyHQ admin / shop app.
 *
 * Methods:
 *   getProductsByShop()   – all available products for a specific shop.
 *   getProductById()      – single product by shop + product IDs.
 *   searchProducts()      – location-aware text search across nearby shops
 *                           (delegates to SearchRepository).
 *   getFeaturedProducts() – most recent products from the nearest shops,
 *                           used for the Dashboard's "Hot Products" section.
 */
public class ProductRepository {

    private static final String TAG = "NearBuy.ProductRepo";

    private final FirebaseFirestore firestore;
    private final ShopRepository    shopRepository;

    public ProductRepository() {
        this.firestore      = FirebaseFirestore.getInstance();
        this.shopRepository = new ShopRepository();
    }

    // ── Products for a specific shop ──────────────────────────────────────────

    /**
     * Loads all available (isAvailable=true) products for a specific shop,
     * ordered newest-first.  Used by StoreDetailsActivity and ProductDetailsActivity.
     *
     * @param shopId   the shop's Firestore document ID
     * @param callback DataCallback&lt;List&lt;Product&gt;&gt;
     */
    public void getProductsByShop(String shopId, DataCallback<List<Product>> callback) {
        if (!FirebaseConfig.isFirebaseEnabled()) {
            callback.onError(new IllegalStateException("Firebase not enabled."));
            return;
        }

        // NearBuyHQ stores availability as status:"Available" (not isAvailable boolean).
        // No orderBy combined with whereEqualTo to avoid requiring a composite Firestore index.
        firestore.collection(FirebaseCollections.SHOPS)
                .document(shopId)
                .collection(FirebaseCollections.SHOP_PRODUCTS)
                .whereEqualTo("status", "Available")
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Product> products = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        Product p = Product.fromMap(doc.getId(), shopId, doc.getData());
                        if (p != null) products.add(p);
                    }
                    if (!products.isEmpty()) {
                        // Sort newest first client-side
                        products.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
                        Log.d(TAG, "Loaded " + products.size() + " products for shop: " + shopId);
                        callback.onSuccess(products);
                    } else {
                        // Fallback: load all products without status filter
                        firestore.collection(FirebaseCollections.SHOPS)
                                .document(shopId)
                                .collection(FirebaseCollections.SHOP_PRODUCTS)
                                .get()
                                .addOnSuccessListener(snap2 -> {
                                    List<Product> all = new ArrayList<>();
                                    for (QueryDocumentSnapshot d : snap2) {
                                        Product p2 = Product.fromMap(d.getId(), shopId, d.getData());
                                        if (p2 != null) all.add(p2);
                                    }
                                    all.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
                                    Log.d(TAG, "Loaded " + all.size() + " products (fallback) for shop: " + shopId);
                                    callback.onSuccess(all);
                                })
                                .addOnFailureListener(callback::onError);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load products for shop: " + shopId, e);
                    callback.onError(e);
                });
    }

    // ── Single product by ID ──────────────────────────────────────────────────

    /**
     * Loads a single product document from a shop's products sub-collection.
     * Used by ProductDetailsActivity when launched with full shopId + productId.
     *
     * @param shopId    parent shop document ID
     * @param productId product document ID
     * @param callback  DataCallback&lt;Product&gt;
     */
    public void getProductById(String shopId, String productId,
                               DataCallback<Product> callback) {
        if (!FirebaseConfig.isFirebaseEnabled()) {
            callback.onError(new IllegalStateException("Firebase not enabled."));
            return;
        }

        firestore.collection(FirebaseCollections.SHOPS)
                .document(shopId)
                .collection(FirebaseCollections.SHOP_PRODUCTS)
                .document(productId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        callback.onError(new Exception(
                                "Product not found: " + productId));
                        return;
                    }
                    Product p = Product.fromMap(doc.getId(), shopId, doc.getData());
                    if (p == null) {
                        callback.onError(new Exception(
                                "Failed to parse product document."));
                        return;
                    }
                    callback.onSuccess(p);
                })
                .addOnFailureListener(callback::onError);
    }

    // ── Location-aware text search ─────────────────────────────────────────────

    /**
     * Searches for products matching {@code query} across all shops within the
     * customer's chosen radius.  Delegates the fan-out query to SearchRepository.
     *
     * Results are sorted: nearest shop first, then by lowest price.
     *
     * @param query       the search term typed by the customer (e.g. "Apples")
     * @param customerLat customer's current latitude
     * @param customerLng customer's current longitude
     * @param radiusKm    maximum distance filter in kilometres
     * @param callback    DataCallback&lt;List&lt;Product&gt;&gt;
     */
    public void searchProducts(String query,
                               double customerLat, double customerLng,
                               float radiusKm,
                               DataCallback<List<Product>> callback) {
        if (!FirebaseConfig.isFirebaseEnabled()) {
            callback.onError(new IllegalStateException("Firebase not enabled."));
            return;
        }

        // Delegate to SearchRepository which already implements the optimised fan-out
        new SearchRepository().search(query, customerLat, customerLng, radiusKm, callback);
    }

    // ── Dashboard: featured / hot products ───────────────────────────────────

    /**
     * Loads the most recently added products from the nearest shops for display
     * in the Dashboard's "Hot Products" or "Nearby" section.
     *
     * Strategy:
     *   1. Get the nearest (up to 5) active shops within the radius.
     *   2. For each shop, query its products collection ordered newest-first.
     *   3. Merge, sort by distance and cap to {@code limit}.
     *
     * Enriches each product with its parent shop's name, address and distance.
     *
     * @param customerLat customer's current latitude
     * @param customerLng customer's current longitude
     * @param radiusKm    neighbourhood radius in kilometres
     * @param limit       maximum number of products to return
     * @param callback    DataCallback&lt;List&lt;Product&gt;&gt;
     */
    public void getFeaturedProducts(double customerLat, double customerLng,
                                    float radiusKm, int limit,
                                    DataCallback<List<Product>> callback) {
        if (!FirebaseConfig.isFirebaseEnabled()) {
            callback.onError(new IllegalStateException("Firebase not enabled."));
            return;
        }

        // ShopRepository.getNearbyShops now returns all shops when lat==0 && lng==0,
        // or falls back to all shops when none are found within the radius.
        shopRepository.getNearbyShops(customerLat, customerLng, radiusKm,
                new DataCallback<List<Shop>>() {
                    @Override
                    public void onSuccess(List<Shop> shops) {
                        if (shops.isEmpty()) {
                            callback.onSuccess(new ArrayList<>());
                            return;
                        }

                        // Fan out to the closest 5 shops to keep query cost bounded
                        int shopCount      = Math.min(shops.size(), 5);
                        List<Product>  results = new ArrayList<>();
                        AtomicInteger  pending = new AtomicInteger(shopCount);

                        // Step 2: for each nearby shop, get its products
                        for (int i = 0; i < shopCount; i++) {
                            Shop shop = shops.get(i);
                            firestore.collection(FirebaseCollections.SHOPS)
                                    .document(shop.getId())
                                    .collection(FirebaseCollections.SHOP_PRODUCTS)
                                    .whereEqualTo("status", "Available")
                                    .get()
                                    .addOnSuccessListener(snapshot -> {
                                        for (QueryDocumentSnapshot doc : snapshot) {
                                            Product p = Product.fromMap(
                                                    doc.getId(), shop.getId(), doc.getData());
                                            if (p == null) continue;

                                            // Step 3: enrich each product with shop data + distance
                                            p.setShopName(shop.getName());
                                            p.setShopLocation(shop.getShopLocation());
                                            p.setShopLatitude(shop.getLatitude());
                                            p.setShopLongitude(shop.getLongitude());
                                            p.setDistanceKm(shop.getDistanceKm());
                                            synchronized (results) { results.add(p); }
                                        }
                                        finalizeIfDone(pending, results, limit, callback);
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.w(TAG, "Products load failed for shop: "
                                                + shop.getId(), e);
                                        // Count as complete even on partial failure
                                        finalizeIfDone(pending, results, limit, callback);
                                    });
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e(TAG, "Failed to load nearby shops for featured products", e);
                        callback.onError(e);
                    }
                });
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Decrements the pending counter and, when all shop queries have completed,
     * sorts the results by distance (nearest first) and caps to {@code limit}.
     */
    private void finalizeIfDone(AtomicInteger pending, List<Product> results,
                                int limit, DataCallback<List<Product>> callback) {
        if (pending.decrementAndGet() == 0) {
            // Sort nearest first
            results.sort((a, b) -> Double.compare(a.getDistanceKm(), b.getDistanceKm()));
            List<Product> capped = results.size() > limit
                    ? results.subList(0, limit) : results;
            Log.d(TAG, "Featured products loaded: " + capped.size());
            callback.onSuccess(new ArrayList<>(capped));
        }
    }
}
