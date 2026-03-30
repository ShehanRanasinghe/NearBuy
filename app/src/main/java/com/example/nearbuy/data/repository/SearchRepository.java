package com.example.nearbuy.data.repository;

import android.util.Log;

import com.example.nearbuy.core.firebase.FirebaseConfig;
import com.example.nearbuy.data.model.Product;
import com.example.nearbuy.data.model.Shop;
import com.example.nearbuy.data.remote.firebase.FirebaseCollections;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * SearchRepository – location-aware product search across all nearby shops.
 *
 * Search algorithm:
 *   1. Load all active shops from NearBuyHQ.
 *   2. Filter shops to those within the customer's chosen radius (Haversine).
 *   3. For each nearby shop, load its products sub-collection and filter by
 *      name/category match (case-insensitive prefix / contains search).
 *   4. Enrich each matching product with the parent shop's name, address,
 *      coordinates, and calculated distance.
 *   5. Sort results: nearest shop first, then lowest price for same distance.
 *
 * Firestore note: Firestore does not support full-text search natively.
 * For production-scale search, integrate Algolia or Typesense.  The current
 * implementation loads and filters client-side, which is acceptable for
 * small-to-medium product catalogues (< 10 000 products per shop).
 */
public class SearchRepository {

    private static final String TAG = "NearBuy.SearchRepo";

    private final FirebaseFirestore firestore;
    private final ShopRepository    shopRepo;

    public SearchRepository() {
        this.firestore = FirebaseFirestore.getInstance();
        this.shopRepo  = new ShopRepository();
    }

    // ── Main search ────────────────────────────────────────────────────────────

    /**
     * Searches for products matching {@code query} across all shops within
     * the customer's chosen radius.  Results are sorted: nearest shop first,
     * then by lowest price for products from the same shop.
     *
     * @param query       text typed in the search bar (e.g. "Apples")
     * @param customerLat customer's current latitude
     * @param customerLng customer's current longitude
     * @param radiusKm    maximum search radius (1, 2, 3, 5, or 10 km)
     * @param callback    DataCallback<List<Product>> – sorted results
     */
    public void search(String query,
                       double customerLat, double customerLng,
                       float radiusKm,
                       DataCallback<List<Product>> callback) {
        if (!FirebaseConfig.isFirebaseEnabled()) {
            callback.onError(new IllegalStateException("Firebase is not enabled."));
            return;
        }

        String lowerQuery = query.trim().toLowerCase(Locale.ROOT);

        // Step 1: find shops within radius
        shopRepo.getNearbyShops(customerLat, customerLng, radiusKm,
                new DataCallback<List<Shop>>() {
                    @Override
                    public void onSuccess(List<Shop> shops) {
                        if (shops.isEmpty()) {
                            callback.onSuccess(new ArrayList<>());
                            return;
                        }

                        List<Product>  results = new ArrayList<>();
                        AtomicInteger  pending = new AtomicInteger(shops.size());

                        // Step 2: fan out to each shop's products sub-collection
                        for (Shop shop : shops) {
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

                                            // Step 3: client-side name/category filter
                                            boolean nameMatch = p.getName() != null
                                                    && p.getName().toLowerCase(Locale.ROOT)
                                                    .contains(lowerQuery);
                                            boolean catMatch = p.getCategory() != null
                                                    && p.getCategory().toLowerCase(Locale.ROOT)
                                                    .contains(lowerQuery);

                                            if (!nameMatch && !catMatch) continue;

                                            // Step 4: enrich with shop data + distance
                                            p.setShopName(shop.getName());
                                            p.setShopLocation(shop.getShopLocation());
                                            p.setShopLatitude(shop.getLatitude());
                                            p.setShopLongitude(shop.getLongitude());
                                            p.setDistanceKm(shop.getDistanceKm());

                                            synchronized (results) { results.add(p); }
                                        }
                                        checkAndReturn(pending, results, callback);
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.w(TAG, "Products load failed for shop "
                                                + shop.getId(), e);
                                        checkAndReturn(pending, results, callback);
                                    });
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        callback.onError(e);
                    }
                });
    }

    // ── Category search ────────────────────────────────────────────────────────

    /**
     * Searches products by category across all nearby shops.
     * Triggered when the customer taps a category chip (Groceries, Fresh, Food…).
     *
     * @param category    must exactly match the product's category field in Firestore
     * @param customerLat customer latitude
     * @param customerLng customer longitude
     * @param radiusKm    filter radius
     * @param callback    DataCallback<List<Product>>
     */
    public void searchByCategory(String category,
                                  double customerLat, double customerLng,
                                  float radiusKm,
                                  DataCallback<List<Product>> callback) {
        // Re-use search() with the category string as the query –
        // the name/category filter in search() handles this transparently
        search(category, customerLat, customerLng, radiusKm, callback);
    }

    // ── Haversine distance utility ─────────────────────────────────────────────

    /**
     * Calculates the great-circle distance between two GPS points using the
     * Haversine formula.  Accurate to within ~0.5% at typical road distances.
     *
     * This is a pure utility method with no network calls.  ShopRepository and
     * DealRepository delegate distance calculations here.
     *
     * @param lat1 latitude  of point A (decimal degrees)
     * @param lng1 longitude of point A (decimal degrees)
     * @param lat2 latitude  of point B (decimal degrees)
     * @param lng2 longitude of point B (decimal degrees)
     * @return great-circle distance in kilometres
     */
    public static double haversineKm(double lat1, double lng1,
                                     double lat2, double lng2) {
        final double R = 6371.0; // Earth mean radius in km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    /**
     * Decrements the pending counter and, when all shops have responded,
     * sorts the results (nearest first → lowest price) and fires the callback.
     */
    private void checkAndReturn(AtomicInteger pending,
                                List<Product> results,
                                DataCallback<List<Product>> callback) {
        if (pending.decrementAndGet() == 0) {
            // Sort: nearest shop first, then lowest price as tie-breaker
            results.sort((a, b) -> {
                int distCmp = Double.compare(a.getDistanceKm(), b.getDistanceKm());
                return distCmp != 0 ? distCmp : Double.compare(a.getPrice(), b.getPrice());
            });
            callback.onSuccess(new ArrayList<>(results));
        }
    }
}
