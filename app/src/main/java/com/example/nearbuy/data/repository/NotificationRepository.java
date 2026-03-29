package com.example.nearbuy.data.repository;

import com.example.nearbuy.core.firebase.FirebaseConfig;
import com.example.nearbuy.data.model.NotificationItem;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

/**
 * NotificationRepository – manages customer notifications in Firestore.
 *
 * Notifications are stored at  NearBuy/{customerId}/notifications/{notificationId}.
 *
 * Methods are stubs – Firebase query logic will be added in the backend phase.
 */
public class NotificationRepository {

    private final FirebaseFirestore firestore;

    public NotificationRepository() {
        this.firestore = FirebaseFirestore.getInstance();
    }

    // ── Load notifications ─────────────────────────────────────────────────────

    /**
     * Loads all notifications for the customer, sorted newest first.
     *
     * @param customerId customer UID
     * @param callback   DataCallback<List<NotificationItem>>
     */
    public void getNotifications(String customerId,
                                 DataCallback<List<NotificationItem>> callback) {
        if (!FirebaseConfig.isFirebaseEnabled()) {
            callback.onError(new IllegalStateException("Firebase not enabled."));
            return;
        }
        // TODO: implement in the notifications backend phase
        callback.onError(new UnsupportedOperationException("getNotifications() – not yet implemented"));
    }

    // ── Unread count ───────────────────────────────────────────────────────────

    /**
     * Returns the count of unread notifications for the customer.
     * Used to display a badge on the notification bell icon.
     *
     * @param customerId customer UID
     * @param callback   DataCallback<Integer>
     */
    public void getUnreadCount(String customerId, DataCallback<Integer> callback) {
        if (!FirebaseConfig.isFirebaseEnabled()) {
            callback.onError(new IllegalStateException("Firebase not enabled."));
            return;
        }
        // TODO: implement in the notifications backend phase
        callback.onError(new UnsupportedOperationException("getUnreadCount() – not yet implemented"));
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
            callback.onError(new IllegalStateException("Firebase not enabled."));
            return;
        }
        // TODO: implement in the notifications backend phase
        callback.onError(new UnsupportedOperationException("markAsRead() – not yet implemented"));
    }

    /**
     * Marks ALL notifications for the customer as read.
     *
     * @param customerId customer UID
     * @param callback   OperationCallback
     */
    public void markAllAsRead(String customerId, OperationCallback callback) {
        if (!FirebaseConfig.isFirebaseEnabled()) {
            callback.onError(new IllegalStateException("Firebase not enabled."));
            return;
        }
        // TODO: implement in the notifications backend phase
        callback.onError(new UnsupportedOperationException("markAllAsRead() – not yet implemented"));
    }
}

