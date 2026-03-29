package com.example.nearbuy.data.repository;

import com.example.nearbuy.core.firebase.FirebaseConfig;
import com.example.nearbuy.data.model.Product;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

/**
 * SearchRepository – the heart of NearBuy's location-aware product search.
 *
 * When a customer searches for e.g. "Apples":
 *   1. Fetch all active shops from  NearBuyHQ/.
 *   2. For each shop within the customer's chosen radius, fetch its products
 *      sub-collection and filter by name/category.
 *   3. Enrich each matching product with the parent shop's name, address,
 *      lat/lng, and the calculated distance from the customer.
 *   4. Sort results: nearest shop first, then by lowest price for same shop.
 *
 * Methods are stubs – Firebase + distance logic will be added in the backend phase.
 *
 * Distance calculation uses the Haversine formula (see {@link #haversineKm}).
 */
public class SearchRepository {

    private final FirebaseFirestore firestore;

    public SearchRepository() {
        this.firestore = FirebaseFirestore.getInstance();
    }

    // ── Main search entry point ────────────────────────────────────────────────

    /**
     * Searches for products matching {@code query} within the customer's radius,
     * enriches each result with shop info and distance, then returns results
     * sorted by distance (nearest first) then by price (lowest first).
     *
     * @param query       text typed in the search bar (e.g. "Apples")
     * @param customerLat customer's current latitude
     * @param customerLng customer's current longitude
     * @param radiusKm    filter radius chosen by the customer (1, 2, 3, 5, 10 km)
     * @param callback    DataCallback<List<Product>> – results already sorted
     */
    public void search(String query,
                       double customerLat, double customerLng,
                       float radiusKm,
                       DataCallback<List<Product>> callback) {
        if (!FirebaseConfig.isFirebaseEnabled()) {
            callback.onError(new IllegalStateException("Firebase not enabled."));
            return;
        }
        // TODO: implement in the search backend phase
        callback.onError(new UnsupportedOperationException("search() – not yet implemented"));
    }

    // ── Category search ────────────────────────────────────────────────────────

    /**
     * Same as {@link #search} but filters by product category instead of name.
     * Triggered when the customer taps a category chip (Groceries, Fresh, Food …).
     *
     * @param category    category string matching the product's category field
     * @param customerLat customer latitude
     * @param customerLng customer longitude
     * @param radiusKm    filter radius
     * @param callback    DataCallback<List<Product>>
     */
    public void searchByCategory(String category,
                                  double customerLat, double customerLng,
                                  float radiusKm,
                                  DataCallback<List<Product>> callback) {
        if (!FirebaseConfig.isFirebaseEnabled()) {
            callback.onError(new IllegalStateException("Firebase not enabled."));
            return;
        }
        // TODO: implement in the search backend phase
        callback.onError(new UnsupportedOperationException("searchByCategory() – not yet implemented"));
    }

    // ── Distance utility ──────────────────────────────────────────────────────

    /**
     * Calculates the great-circle distance between two GPS coordinates using the
     * Haversine formula.
     *
     * This is a pure utility method – no Firebase calls.  Repositories and
     * Activities can call this directly for client-side distance computation.
     *
     * @param lat1 latitude  of point A  (degrees)
     * @param lng1 longitude of point A  (degrees)
     * @param lat2 latitude  of point B  (degrees)
     * @param lng2 longitude of point B  (degrees)
     * @return distance in kilometres
     */
    public static double haversineKm(double lat1, double lng1,
                                     double lat2, double lng2) {
        final double R = 6371.0; // Earth radius in km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}

