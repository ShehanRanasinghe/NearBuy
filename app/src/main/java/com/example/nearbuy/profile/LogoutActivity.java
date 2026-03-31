package com.example.nearbuy.profile;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.example.nearbuy.R;
import com.example.nearbuy.app.startup.WelcomeActivity;
import com.example.nearbuy.data.repository.AuthRepository;

/**
 * LogoutActivity – logout confirmation screen.
 *
 * Shown when the customer taps "Log Out" in ProfileActivity.
 * Presents two options:
 *   • "Yes, Log Out" → calls AuthRepository.logout() which signs out of
 *     Firebase Auth and clears SessionManager, then navigates to WelcomeActivity.
 *   • "Cancel"       → finish() – returns to the previous screen.
 *
 * The Firebase sign-out and SessionManager clear happen here so that both
 * the server session and local cache are always wiped together.
 */
public class LogoutActivity extends AppCompatActivity {

    private static final String TAG = "NearBuy.Logout";

    // Repository dependency
    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logout);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        authRepository = new AuthRepository();

        // "Yes, Log Out" button – sign out and clear session
        findViewById(R.id.btnYes).setOnClickListener(v -> performLogout());

        // "Cancel" button – return to the previous screen without logging out
        findViewById(R.id.btnCancel).setOnClickListener(v -> finish());
    }

    // ── Logout logic ───────────────────────────────────────────────────────────

    /**
     * Calls AuthRepository.logout() to:
     *   1. Sign out of Firebase Auth (invalidates the server token).
     *   2. Clear all customer data from SessionManager (name, UID, email, phone,
     *      location and search preferences).
     *
     * Then redirects to WelcomeActivity with all previous activities cleared
     * from the back stack so pressing Back does not return to authenticated screens.
     */
    private void performLogout() {
        Log.d(TAG, "Customer confirmed logout.");

        // Sign out of Firebase Auth and wipe local session cache
        authRepository.logout(this);

        // Navigate to the Welcome screen; clear the entire activity back stack
        Intent intent = new Intent(this, WelcomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
