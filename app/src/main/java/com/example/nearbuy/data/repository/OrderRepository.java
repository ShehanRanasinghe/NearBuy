package com.example.nearbuy.data.repository;

import android.util.Log;

import com.example.nearbuy.core.firebase.FirebaseConfig;
import com.example.nearbuy.data.model.Customer;
import com.example.nearbuy.data.remote.firebase.FirebaseCollections;
import com.example.nearbuy.orders.OrderItem;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * OrderRepository – reads and writes customer order documents from Firestore.
 *
 * Firestore path: NearBuy/{customerId}/orders/{orderId}
 *
 * All order writes are performed by the companion NearBuyHQ admin / shop app.
 * The customer app only READS order history and customer stats here.
 */
public class OrderRepository {

    private static final String TAG = "NearBuy.OrderRepo";

    private final FirebaseFirestore firestore;

    public OrderRepository() {
        this.firestore = FirebaseFirestore.getInstance();
    }

    // ── Place order ────────────────────────────────────────────────────────────

    /**
     * Saves a new order document to NearBuy/{customerId}/orders.
     * Called from ProductDetailsActivity when the customer taps "Place Order".
     *
     * @param customerId customer UID
     * @param order      the OrderItem to persist
     * @param callback   OperationCallback – onSuccess when saved, onError otherwise
     */
    public void placeOrder(String customerId, OrderItem order, OperationCallback callback) {
        if (!FirebaseConfig.isFirebaseEnabled()) {
            callback.onError(new IllegalStateException("Firebase is not enabled."));
            return;
        }

        firestore.collection(FirebaseCollections.CUSTOMERS)
                .document(customerId)
                .collection(FirebaseCollections.CUSTOMER_ORDERS)
                .add(order.toMap())
                .addOnSuccessListener(ref -> {
                    Log.d(TAG, "Order placed: " + ref.getId());
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to place order", e);
                    callback.onError(e);
                });
    }

    // ── Order history ──────────────────────────────────────────────────────────

    /**
     * Loads all orders placed by the customer, sorted newest-first.
     * Used in OrdersActivity and DealsActivity to display the order list.
     *
     * @param customerId customer UID
     * @param callback   DataCallback<List<OrderItem>>
     */
    public void getOrderHistory(String customerId,
                                DataCallback<List<OrderItem>> callback) {
        if (!FirebaseConfig.isFirebaseEnabled()) {
            callback.onError(new IllegalStateException("Firebase is not enabled."));
            return;
        }

        firestore.collection(FirebaseCollections.CUSTOMERS)
                .document(customerId)
                .collection(FirebaseCollections.CUSTOMER_ORDERS)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<OrderItem> orders = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        OrderItem order = OrderItem.fromMap(doc.getId(), doc.getData());
                        if (order != null) orders.add(order);
                    }
                    Log.d(TAG, "Loaded " + orders.size() + " orders for customer: " + customerId);
                    callback.onSuccess(orders);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load orders", e);
                    callback.onError(e);
                });
    }

    // ── Customer stats ─────────────────────────────────────────────────────────

    /**
     * Reads the customer's Firestore profile document to get lifetime stats:
     * total orders placed, total amount spent, and total amount saved via deals.
     *
     * These stats are maintained by the admin / shop app when orders are processed.
     * The Dashboard stats card (Orders / Total Spent / Total Saved) is populated
     * using the values returned here.
     *
     * @param customerId customer UID
     * @param callback   DataCallback<Customer> – the Customer model contains the stats
     */
    public void getCustomerStats(String customerId,
                                 DataCallback<Customer> callback) {
        if (!FirebaseConfig.isFirebaseEnabled()) {
            callback.onError(new IllegalStateException("Firebase is not enabled."));
            return;
        }

        firestore.collection(FirebaseCollections.CUSTOMERS)
                .document(customerId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        // Return a zeroed-out Customer so the Dashboard shows 0/0/0
                        callback.onSuccess(new Customer(customerId, "", "", ""));
                        return;
                    }
                    Customer customer = Customer.fromMap(customerId, doc.getData());
                    callback.onSuccess(customer != null
                            ? customer : new Customer(customerId, "", "", ""));
                })
                .addOnFailureListener(callback::onError);
    }
}
