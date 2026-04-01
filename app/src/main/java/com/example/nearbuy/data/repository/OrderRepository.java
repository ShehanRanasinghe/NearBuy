package com.example.nearbuy.data.repository;

import android.util.Log;

import com.example.nearbuy.core.firebase.FirebaseConfig;
import com.example.nearbuy.data.model.Customer;
import com.example.nearbuy.data.remote.firebase.FirebaseCollections;
import com.example.nearbuy.orders.OrderItem;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * OrderRepository – reads and writes customer order documents from Firestore.
 *
 * Dual-write strategy:
 *   • NearBuyHQ/{shopId}/orders/{orderId}   ← shop-wise; readable by NearBuyHQ admin app
 *   • NearBuy/{customerId}/orders/{orderId} ← customer-wise; readable by this app
 *
 * Customer stats (totalOrders, totalSpent) are NOT updated here.
 * They are maintained exclusively by the NearBuyHQ admin app when orders are processed.
 */
public class OrderRepository {

    private static final String TAG = "NearBuy.OrderRepo";

    private final FirebaseFirestore firestore;

    public OrderRepository() {
        this.firestore = FirebaseFirestore.getInstance();
    }

    // ── Place order ────────────────────────────────────────────────────────────

    /**
     * Saves a new order document to BOTH:
     *   NearBuyHQ/{shopId}/orders/{orderId}   (admin / shop owner can see all orders per shop)
     *   NearBuy/{customerId}/orders/{orderId} (customer can see their own order history)
     *
     * The same auto-generated document ID is used in both paths so they can be
     * cross-referenced easily.
     *
     * Customer stats (totalOrders, totalSpent) are NOT modified here.
     * The NearBuyHQ admin app is responsible for updating those when it processes orders.
     *
     * @param customerId customer UID
     * @param order      the OrderItem to persist (must have shopId, customerId set)
     * @param callback   OperationCallback – onSuccess when both writes succeed
     */
    public void placeOrder(String customerId, OrderItem order, OperationCallback callback) {
        if (!FirebaseConfig.isFirebaseEnabled()) {
            callback.onError(new IllegalStateException("Firebase is not enabled."));
            return;
        }

        String shopId = order.getShopId();
        Map<String, Object> orderData = order.toMap();

        // ── Always write to customer's own orders sub-collection ──────────────
        DocumentReference customerOrderRef = firestore
                .collection(FirebaseCollections.CUSTOMERS)
                .document(customerId)
                .collection(FirebaseCollections.CUSTOMER_ORDERS)
                .document();                   // auto-generate the document ID

        String orderId = customerOrderRef.getId();

        WriteBatch batch = firestore.batch();
        batch.set(customerOrderRef, orderData);

        // ── Also write under the shop's orders sub-collection in NearBuyHQ ────
        if (shopId != null && !shopId.isEmpty()) {
            DocumentReference shopOrderRef = firestore
                    .collection(FirebaseCollections.SHOPS)
                    .document(shopId)
                    .collection(FirebaseCollections.SHOP_ORDERS)
                    .document(orderId);        // same ID as the customer-side doc
            batch.set(shopOrderRef, orderData);
        }

        batch.commit()
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "Order placed (id=" + orderId
                            + ", shopId=" + shopId + ", customerId=" + customerId + ")");
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
     * Reads from: NearBuy/{customerId}/orders
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

    // ── Customer stats (real-time) ─────────────────────────────────────────────

    /**
     * Attaches a real-time listener on the customer's Firestore profile document.
     * The callback fires immediately with the current values and again whenever
     * the NearBuyHQ admin app updates totalOrders / totalSpent / totalSaved.
     *
     * Call {@link ListenerRegistration#remove()} in the activity's onDestroy() to
     * prevent memory leaks.
     *
     * @param customerId customer UID
     * @param callback   fires on every update (or on error)
     * @return           registration token – call .remove() when done
     */
    public ListenerRegistration listenToCustomerStats(String customerId,
                                                      DataCallback<Customer> callback) {
        return firestore.collection(FirebaseCollections.CUSTOMERS)
                .document(customerId)
                .addSnapshotListener((doc, error) -> {
                    if (error != null) {
                        callback.onError(error);
                        return;
                    }
                    if (doc != null && doc.exists()) {
                        Customer c = Customer.fromMap(customerId, doc.getData());
                        callback.onSuccess(c != null ? c : new Customer(customerId, "", "", ""));
                    } else {
                        callback.onSuccess(new Customer(customerId, "", "", ""));
                    }
                });
    }

    /**
     * Computes customer stats directly from the orders sub-collection.
     * Used as a fallback when the customer document's cached stats are zero,
     * which can happen before the NearBuyHQ admin app has processed orders.
     *
     * - totalOrders = count of ALL order documents
     * - totalSpent  = sum of totalAmountRaw for non-Cancelled orders
     *
     * @param customerId customer UID
     * @param callback   DataCallback<Customer> with computed totals
     */
    public void computeStatsFromOrders(String customerId, DataCallback<Customer> callback) {
        if (!FirebaseConfig.isFirebaseEnabled()) {
            callback.onError(new IllegalStateException("Firebase is not enabled."));
            return;
        }
        firestore.collection(FirebaseCollections.CUSTOMERS)
                .document(customerId)
                .collection(FirebaseCollections.CUSTOMER_ORDERS)
                .get()
                .addOnSuccessListener(snapshot -> {
                    int    totalOrders = 0;
                    double totalSpent  = 0;
                    for (QueryDocumentSnapshot doc : snapshot) {
                        OrderItem order = OrderItem.fromMap(doc.getId(), doc.getData());
                        if (order != null) {
                            totalOrders++;
                            if (!"Cancelled".equals(order.getStatus())) {
                                totalSpent += order.getTotalAmountRaw();
                            }
                        }
                    }
                    Customer c = new Customer(customerId, "", "", "");
                    c.setTotalOrders(totalOrders);
                    c.setTotalSpent(totalSpent);
                    callback.onSuccess(c);
                })
                .addOnFailureListener(callback::onError);
    }

    // ── Customer stats (one-shot) ──────────────────────────────────────────────

    /**
     * Reads the customer's Firestore profile document to get lifetime stats:
     * totalOrders, totalSpent, and totalSaved.
     *
     * These stats are maintained by the NearBuyHQ admin app when orders are processed.
     * The customer app only READS these values – it never writes them.
     *
     * @param customerId customer UID
     * @param callback   DataCallback<Customer>
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
                        callback.onSuccess(new Customer(customerId, "", "", ""));
                        return;
                    }
                    Customer customer = Customer.fromMap(customerId, doc.getData());
                    callback.onSuccess(customer != null
                            ? customer : new Customer(customerId, "", "", ""));
                })
                .addOnFailureListener(callback::onError);
    }

    // ── Order reports ──────────────────────────────────────────────────────────

    /**
     * Checks whether the customer has already submitted a report for the given order.
     * Uses the orderId as the document ID in NearBuyHQ/{shopId}/reports/{orderId}.
     *
     * @param shopId    the shop's Firestore document ID
     * @param orderId   the order's Firestore document ID
     * @param callback  DataCallback<Boolean> – true if report already exists
     */
    public void checkReportExists(String shopId, String orderId,
                                  DataCallback<Boolean> callback) {
        if (!FirebaseConfig.isFirebaseEnabled()) {
            callback.onSuccess(false);
            return;
        }
        firestore.collection(FirebaseCollections.SHOPS)
                .document(shopId)
                .collection(FirebaseCollections.SHOP_REPORTS)
                .document(orderId)
                .get()
                .addOnSuccessListener(doc -> callback.onSuccess(doc.exists()))
                .addOnFailureListener(e -> {
                    Log.w(TAG, "checkReportExists failed", e);
                    callback.onSuccess(false);
                });
    }

    /**
     * Saves a customer order report to NearBuyHQ/{shopId}/reports/{orderId}.
     * Uses orderId as the document ID so only one report per order is possible.
     *
     * @param shopId       shop document ID
     * @param orderId      order document ID (becomes the report document ID)
     * @param customerId   customer UID
     * @param customerName customer display name
     * @param shopName     shop display name
     * @param reportText   the report content (max 200 words enforced by caller)
     * @param callback     OperationCallback
     */
    public void saveReport(String shopId, String orderId, String customerId,
                           String customerName, String shopName,
                           String reportText, OperationCallback callback) {
        if (!FirebaseConfig.isFirebaseEnabled()) {
            callback.onError(new IllegalStateException("Firebase is not enabled."));
            return;
        }

        java.util.Map<String, Object> report = new java.util.HashMap<>();
        report.put("orderId",      orderId);
        report.put("shopId",       shopId);
        report.put("shopName",     shopName != null ? shopName : "");
        report.put("customerId",   customerId);
        report.put("customerName", customerName != null ? customerName : "");
        report.put("reportText",   reportText);
        report.put("wordCount",    countWords(reportText));
        report.put("createdAt",    System.currentTimeMillis());

        firestore.collection(FirebaseCollections.SHOPS)
                .document(shopId)
                .collection(FirebaseCollections.SHOP_REPORTS)
                .document(orderId)   // orderId as doc ID → enforces one report per order
                .set(report)
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "Report saved for order=" + orderId + " shop=" + shopId);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save report", e);
                    callback.onError(e);
                });
    }

    /** Counts the number of words in a string. */
    private static int countWords(String text) {
        if (text == null || text.trim().isEmpty()) return 0;
        return text.trim().split("\\s+").length;
    }
}
