package com.example.nearbuy.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.nearbuy.R;
import com.example.nearbuy.app.startup.WelcomeActivity;
import com.example.nearbuy.core.SessionManager;
import com.example.nearbuy.data.repository.AuthRepository;

/**
 * SettingsActivity – app preferences and account management screen.
 *
 * Contains:
 *   • Notification preference toggles (UI-only; FCM topic subscription can
 *     be wired here in a future iteration).
 *   • Account actions: Log Out, Delete Account (placeholder).
 *   • The Logout action uses AuthRepository.logout() to sign out of
 *     Firebase Auth and clear SessionManager – same as LogoutActivity.
 */
public class SettingsActivity extends AppCompatActivity {

    // Repository dependency
    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window w = getWindow();
        w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        w.setStatusBarColor(ContextCompat.getColor(this, R.color.nb_primary_dark));

        setContentView(R.layout.activity_settings);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        authRepository = new AuthRepository();

        // ── Session guard ─────────────────────────────────────────────────────
        // Settings must only be accessible when the customer is logged in.
        SessionManager sessionManager = SessionManager.getInstance(this);
        if (!sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, WelcomeActivity.class));
            finish();
            return;
        }

        setupClicks();
    }

    // ── Click handlers ─────────────────────────────────────────────────────────

    /** Wire up all action buttons on the settings screen. */
    private void setupClicks() {
        // Back arrow → return to previous screen
        safeClick(R.id.btn_back, v -> finish());

        // "Log Out" button → sign out of Firebase, clear session, go to Welcome
        safeClick(R.id.btnLogout, v -> {
            // Call the repository to handle both Firebase Auth sign-out and session clear
            authRepository.logout(this);
            toast("You have been logged out.");
            Intent intent = new Intent(this, WelcomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private void safeClick(int id, android.view.View.OnClickListener l) {
        android.view.View v = findViewById(id);
        if (v != null) v.setOnClickListener(l);
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
