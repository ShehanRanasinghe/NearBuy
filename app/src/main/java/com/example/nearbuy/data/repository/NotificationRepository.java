package com.example.nearbuy.data.repository;

import android.util.Log;

import com.example.nearbuy.core.firebase.FirebaseConfig;
import com.example.nearbuy.data.model.NotificationItem;
import com.example.nearbuy.data.remote.firebase.FirebaseCollections;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * NotificationRepository – manages customer notifications stored in Firestore.
 *
 * Firestore path: NearBuy/{customerId}/notifications/{notificationId}
 *
 * Notifications are written by:
 *   • The NearBuyHQ admin / shop app when a new deal goes live near the customer.
 *   • Order status updates (Processing → Delivered / Cancelled).
 *   • System announcements.
 *
 * This repository handles reading, counting unread, and marking as read.
 */
public class NotificationRepository {

    private static final String TAG = "NearBuy.NotifRepo";

    private final FirebaseFirestore firestore;

    public NotificationRepository() {
        this.firestore = FirebaseFirestore.getInstance();
    }

    // ── Load notifications ─────────────────────────────────────────────────────

    /**
     * Loads all notifications for the customer, sorted newest-first.
     * Used to populate the NotificationsActivity list.
     *
     * @param customerId customer UID
     * @param callback   DataCallback<List<NotificationItem>>
     */
    public void getNotifications(String customerId,
                                 DataCallback<List<NotificationItem>> callback) {
        if (!FirebaseConfig.isFirebaseEnabled()) {
            callback.onError(new IllegalStateException("Firebase is not enabled."));
            return;
        }

        firestore.collection(FirebaseCollections.CUSTOMERS)
                .document(customerId)
                .collection(FirebaseCollections.CUSTOMER_NOTIFICATIONS)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<NotificationItem> notifications = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        NotificationItem n = NotificationItem.fromMap(doc.getId(), doc.getData());
                        if (n != null) notifications.add(n);
                    }
                    Log.d(TAG, "Loaded " + notifications.size() + " notifications.");
                    callback.onSuccess(notifications);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load notifications", e);
                    callback.onError(e);
                });
    }

    // ── Unread count ───────────────────────────────────────────────────────────

    /**
     * Returns the number of unread notifications for the customer.
     * Used to show / hide the badge count on the notification bell icon.
     *
     * @param customerId customer UID
     * @param callback   DataCallback<Integer>
     */
    public void getUnreadCount(String customerId, DataCallback<Integer> callback) {
        if (!FirebaseConfig.isFirebaseEnabled()) {
            callback.onError(new IllegalStateException("Firebase is not enabled."));
            return;
        }

        firestore.collection(FirebaseCollections.CUSTOMERS)
                .document(customerId)
                .collection(FirebaseCollections.CUSTOMER_NOTIFICATIONS)
                .whereEqualTo("isRead", false)
                .get()
                .addOnSuccessListener(snapshot -> callback.onSuccess(snapshot.size()))
                .addOnFailureListener(callback::onError);
    }

    // ── Mark as read ───────────────────────────────────────────────────────────

    /**
     * Marks a single notification as read in Firestore.
     *
     * @param customerId     customer UID
     * @param notificationId notification document ID
     * @param callback       OperationCallback
     */
    public void markAsRead(String customerId, String notificationId,
                           OperationCallback callback) {
        if (!FirebaseConfig.isFirebaseEnabled()) {
            callback.onError(new IllegalStateException("Firebase is not enabled."));
            return;
        }

        Map<String, Object> update = new HashMap<>();
        update.put("isRead", true);

        firestore.collection(FirebaseCollections.CUSTOMERS)
                .document(customerId)
                .collection(FirebaseCollections.CUSTOMER_NOTIFICATIONS)
                .document(notificationId)
                .update(update)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }

    /**
     * Marks ALL unread notifications for the customer as read using a batch write
     * for efficiency (single network round-trip instead of N individual updates).
     *
     * @param customerId customer UID
     * @param callback   OperationCallback
     */
    public void markAllAsRead(String customerId, OperationCallback callback) {
        if (!FirebaseConfig.isFirebaseEnabled()) {
            callback.onError(new IllegalStateException("Firebase is not enabled."));
            return;
        }

        // First query all unread notifications, then batch-update them
        firestore.collection(FirebaseCollections.CUSTOMERS)
                .document(customerId)
                .collection(FirebaseCollections.CUSTOMER_NOTIFICATIONS)
                .whereEqualTo("isRead", false)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        callback.onSuccess(); // nothing to mark
                        return;
                    }

                    // Batch write – Firestore limit is 500 per batch
                    WriteBatch batch = firestore.batch();
                    Map<String, Object> update = new HashMap<>();
                    update.put("isRead", true);

                    for (QueryDocumentSnapshot doc : snapshot) {
                        batch.update(doc.getReference(), update);
                    }

                    batch.commit()
                            .addOnSuccessListener(unused -> {
                                Log.d(TAG, "Marked " + snapshot.size() + " notifications as read.");
                                callback.onSuccess();
                            })
                            .addOnFailureListener(callback::onError);
                })
                .addOnFailureListener(callback::onError);
    }
}
