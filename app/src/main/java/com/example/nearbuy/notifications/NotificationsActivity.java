package com.example.nearbuy.notifications;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.nearbuy.R;
import com.example.nearbuy.app.startup.WelcomeActivity;
import com.example.nearbuy.core.SessionManager;
import com.example.nearbuy.data.repository.DataCallback;
import com.example.nearbuy.data.repository.NotificationRepository;
import com.example.nearbuy.data.repository.OperationCallback;

/**
 * NotificationsActivity – displays deal alerts, order updates and promotional
 * notifications for the signed-in customer.
 *
 * Data is loaded from Firestore via NotificationRepository using the customer
 * UID from SessionManager.
 *
 * "Mark all as read" calls NotificationRepository.markAllAsRead() which issues
 * a Firestore batch write to update all unread documents at once.
 *
 * If no notifications exist the existing static layout remains visible (it
 * serves as a visual placeholder).  The mark-all-read button is hidden when
 * there are zero unread items.
 */
public class NotificationsActivity extends AppCompatActivity {

    private static final String TAG = "NearBuy.Notifications";

    // ── Dependencies ───────────────────────────────────────────────────────────
    private NotificationRepository notificationRepository;
    private SessionManager         sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window w = getWindow();
        w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        w.setStatusBarColor(ContextCompat.getColor(this, R.color.nb_teal_dark));

        setContentView(R.layout.activity_notifications);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        notificationRepository = new NotificationRepository();
        sessionManager         = SessionManager.getInstance(this);

        // ── Session guard ─────────────────────────────────────────────────────
        if (!sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, WelcomeActivity.class));
            finish();
            return;
        }

        setupBackButton();
        loadUnreadCount();      // Decide whether to show "Mark all as read"
        setupMarkAllRead();
    }

    // ── Back button ────────────────────────────────────────────────────────────

    /** Wires the header back button to finish() this activity. */
    private void setupBackButton() {
        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
    }

    // ── Unread count ────────────────────────────────────────────────────────────

    /**
     * Loads the unread notification count from Firestore.
     * Hides the "Mark all as read" button when there are no unread items.
     */
    private void loadUnreadCount() {
        String uid = sessionManager.getUserId();
        if (uid.isEmpty()) return;

        notificationRepository.getUnreadCount(uid, new DataCallback<Integer>() {
            @Override
            public void onSuccess(Integer count) {
                Log.d(TAG, "Unread notifications: " + count);
                TextView tvMarkRead = findViewById(R.id.tv_mark_read);
                if (tvMarkRead != null) {
                    tvMarkRead.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
                }
            }

            @Override
            public void onError(Exception e) {
                Log.w(TAG, "Could not load unread count", e);
                // Non-critical – leave the mark-read button visible as a safe default
            }
        });
    }

    // ── Mark all as read ────────────────────────────────────────────────────────

    /**
     * Sets up the "Mark all as read" button.
     * On click: calls NotificationRepository.markAllAsRead() which performs
     * a Firestore batch write, then hides the button.
     */
    private void setupMarkAllRead() {
        TextView tvMarkRead = findViewById(R.id.tv_mark_read);
        if (tvMarkRead == null) return;

        tvMarkRead.setOnClickListener(v -> {
            String uid = sessionManager.getUserId();
            if (uid.isEmpty()) {
                Toast.makeText(this, "Please log in first.", Toast.LENGTH_SHORT).show();
                return;
            }

            notificationRepository.markAllAsRead(uid, new OperationCallback() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "All notifications marked as read.");
                    Toast.makeText(NotificationsActivity.this,
                            "All notifications marked as read.",
                            Toast.LENGTH_SHORT).show();
                    tvMarkRead.setVisibility(View.GONE);
                }

                @Override
                public void onError(Exception e) {
                    Log.e(TAG, "Failed to mark notifications as read", e);
                    Toast.makeText(NotificationsActivity.this,
                            "Could not update notifications.",
                            Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}