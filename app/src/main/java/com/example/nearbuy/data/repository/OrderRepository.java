package com.example.nearbuy.data.repository;

import com.example.nearbuy.core.firebase.FirebaseConfig;
import com.example.nearbuy.data.remote.firebase.FirebaseCollections;
import com.example.nearbuy.orders.OrderItem;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

/**
 * OrderRepository – manages customer orders in Firestore.
 *
 * Orders are stored at  NearBuy/{customerId}/orders/{orderId}.
 *
 * Methods are stubs – Firebase query logic will be added in the backend phase.
 */
public class OrderRepository {

    private final FirebaseFirestore firestore;

    public OrderRepository() {
        this.firestore = FirebaseFirestore.getInstance();
    }

    // ── Customer order history ─────────────────────────────────────────────────

    /**
     * Loads all orders placed by the customer, sorted by date descending.
     * Used in OrdersActivity.
     *
     * @param customerId customer UID
     * @param callback   DataCallback<List<OrderItem>>
     */
    public void getOrderHistory(String customerId, DataCallback<List<OrderItem>> callback) {
        if (!FirebaseConfig.isFirebaseEnabled()) {
            callback.onError(new IllegalStateException("Firebase not enabled."));
            return;
        }
        // TODO: implement in the orders backend phase
        callback.onError(new UnsupportedOperationException("getOrderHistory() – not yet implemented"));
    }

    // ── Order counts / stats ───────────────────────────────────────────────────

    /**
     * Returns a summary of the customer's order activity (count, total spent,
     * total saved). Used to populate the stats card on the Dashboard.
     *
     * @param customerId customer UID
     * @param callback   DataCallback wrapping a {@code CustomerStats} or the
     *                   full Customer document (totalOrders, totalSpent, totalSaved)
     */
    public void getCustomerStats(String customerId,
                                 DataCallback<com.example.nearbuy.data.model.Customer> callback) {
        if (!FirebaseConfig.isFirebaseEnabled()) {
            callback.onError(new IllegalStateException("Firebase not enabled."));
            return;
        }
        // TODO: implement in the orders backend phase
        callback.onError(new UnsupportedOperationException("getCustomerStats() – not yet implemented"));
    }
}

