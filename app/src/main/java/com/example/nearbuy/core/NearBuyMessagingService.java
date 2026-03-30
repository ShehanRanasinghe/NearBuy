package com.example.nearbuy.core;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.example.nearbuy.data.remote.firebase.FirebaseCollections;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.example.nearbuy.R;
import com.example.nearbuy.dashboard.DashboardActivity;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

/**
 * NearBuyMessagingService – handles incoming Firebase Cloud Messaging (FCM)
 * push notifications for the NearBuy customer app.
 *
 * Responsibilities:
 *   • onMessageReceived()  – displays a system notification when a push message
 *                            arrives while the app is in the foreground.
 *   • onNewToken()         – called when FCM issues a new registration token.
 *                            The token is saved to SessionManager so it can be
 *                            sent to Firestore for server-side targeting.
 *
 * Notification types sent by NearBuyHQ:
 *   • "new_deal"    – a nearby shop published a new deal
 *   • "expiring"    – a saved deal is expiring soon
 *   • "order_update"– an order status changed (Processing → Delivered / Cancelled)
 *   • "promo"       – a new promotional offer is available nearby
 *
 * Channel IDs:
 *   CHANNEL_DEALS   – deal and promotion alerts (default importance)
 *   CHANNEL_ORDERS  – order status updates (high importance)
 */
public class NearBuyMessagingService extends FirebaseMessagingService {

    private static final String TAG = "NearBuy.FCM";

    // Notification channel IDs
    public static final String CHANNEL_DEALS  = "nearbuy_deals";
    public static final String CHANNEL_ORDERS = "nearbuy_orders";

    // Notification IDs (incremented per message type in production;
    // fixed values used here for simplicity)
    private static final int NOTIF_ID_DEAL  = 1001;
    private static final int NOTIF_ID_ORDER = 1002;

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.d(TAG, "Message received from: " + remoteMessage.getFrom());

        // Determine the notification type from the data payload
        String type  = remoteMessage.getData().get("type");
        String title = remoteMessage.getData().get("title");
        String body  = remoteMessage.getData().get("body");

        // Fall back to the notification payload if the data payload is empty
        if (title == null && remoteMessage.getNotification() != null) {
            title = remoteMessage.getNotification().getTitle();
            body  = remoteMessage.getNotification().getBody();
        }

        if (title == null) title = getString(R.string.app_name);
        if (body  == null) body  = "You have a new notification.";

        // Route to the appropriate channel based on message type
        if ("order_update".equals(type)) {
            showNotification(title, body, CHANNEL_ORDERS, NOTIF_ID_ORDER);
        } else {
            // Deals, promos, expiry alerts – all go to the deals channel
            showNotification(title, body, CHANNEL_DEALS, NOTIF_ID_DEAL);
        }
    }

    /**
     * Called by FCM when a new registration token is issued for this device.
     * This happens on first install, after the app clears data, or when FCM
     * rotates the token for security reasons.
     *
     * Steps:
     *   1. Save the token locally in SessionManager for quick access.
     *   2. If a customer session is active, persist the token to their Firestore
     *      profile so the NearBuyHQ admin app can send targeted push notifications.
     *
     * @param token the new FCM device registration token
     */
    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "New FCM token received.");

        // Step 1: persist locally so it is available even without network
        SessionManager session = SessionManager.getInstance(getApplicationContext());
        session.saveFcmToken(token);

        // Step 2: if the customer is logged in, sync the token to their Firestore profile
        String userId = session.getUserId();
        if (!userId.isEmpty()) {
            syncTokenToFirestore(userId, token);
        }
    }

    /**
     * Writes the FCM registration token to the customer's Firestore document so
     * the NearBuyHQ admin app can send targeted deal / order push notifications.
     *
     * Firestore path: NearBuy/{userId}  (fields: fcmToken, tokenUpdatedAt)
     *
     * @param userId the signed-in customer's Firebase UID
     * @param token  the new FCM registration token
     */
    private void syncTokenToFirestore(String userId, String token) {
        Map<String, Object> update = new HashMap<>();
        update.put("fcmToken",       token);
        update.put("tokenUpdatedAt", System.currentTimeMillis());

        FirebaseFirestore.getInstance()
                .collection(FirebaseCollections.CUSTOMERS)
                .document(userId)
                .update(update)
                .addOnSuccessListener(v ->
                        Log.d(TAG, "FCM token synced to Firestore for user: " + userId))
                .addOnFailureListener(e ->
                        Log.w(TAG, "Failed to sync FCM token to Firestore.", e));
    }

    // ── Notification helpers ───────────────────────────────────────────────────

    /**
     * Builds and displays a system notification for the given channel.
     * Creates the notification channel on Android 8+ (API 26+) if it does not
     * already exist.
     *
     * @param title     notification title text
     * @param body      notification body text
     * @param channelId channel ID to post to (CHANNEL_DEALS or CHANNEL_ORDERS)
     * @param notifId   unique notification ID (determines stack/replace behaviour)
     */
    private void showNotification(String title, String body,
                                  String channelId, int notifId) {
        // Tapping the notification opens the Dashboard
        Intent intent = new Intent(this, DashboardActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                ? PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
                : PendingIntent.FLAG_UPDATE_CURRENT;
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, notifId, intent, flags);

        // Ensure the channel exists before posting
        createChannelIfNeeded(channelId);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(CHANNEL_ORDERS.equals(channelId)
                        ? NotificationCompat.PRIORITY_HIGH
                        : NotificationCompat.PRIORITY_DEFAULT);

        NotificationManager manager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(notifId, builder.build());
        }
    }

    /**
     * Creates a notification channel if it does not already exist.
     * No-op on API < 26 where channels are not supported.
     *
     * @param channelId the channel ID to create (CHANNEL_DEALS or CHANNEL_ORDERS)
     */
    private void createChannelIfNeeded(String channelId) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        NotificationManager manager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;
        if (manager.getNotificationChannel(channelId) != null) return; // already exists

        String name;
        String description;
        int    importance;

        if (CHANNEL_ORDERS.equals(channelId)) {
            name        = "Order Updates";
            description = "Notifications about your order status changes";
            importance  = NotificationManager.IMPORTANCE_HIGH;
        } else {
            name        = "Deals & Promotions";
            description = "New deals, promotions and expiry alerts from nearby shops";
            importance  = NotificationManager.IMPORTANCE_DEFAULT;
        }

        NotificationChannel channel = new NotificationChannel(channelId, name, importance);
        channel.setDescription(description);
        manager.createNotificationChannel(channel);
        Log.d(TAG, "Notification channel created: " + channelId);
    }
}

