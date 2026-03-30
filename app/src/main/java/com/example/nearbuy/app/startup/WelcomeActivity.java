package com.example.nearbuy.app.startup;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.nearbuy.R;
import com.example.nearbuy.auth.LoginActivity;
import com.example.nearbuy.auth.RegisterActivity;

/**
 * WelcomeActivity – the entry screen shown to first-time or logged-out users.
 *
 * Reached from:
 *   • SplashScreen → when no valid Firebase Auth session is found.
 *   • LogoutActivity / SettingsActivity → after explicit logout.
 *   • Any session-guarded activity → when the local session has been cleared.
 *
 * This screen is intentionally simple: it presents the NearBuy value proposition
 * and offers two call-to-action buttons:
 *   • "Get Started"  → RegisterActivity (create a new account)
 *   • "Log In"       → LoginActivity    (sign in to an existing account)
 *
 * Navigation note: both destinations use finish() so that pressing Back from
 * Register or Login does not return to this screen unexpectedly.  The full back
 * stack is managed by the FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK
 * flags set on post-login navigation to DashboardActivity.
 */
public class WelcomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        // ── Bind buttons ──────────────────────────────────────────────────────
        Button   btnGetStarted        = findViewById(R.id.btnGetStarted);
        TextView tvAlreadyHaveAccount = findViewById(R.id.tvAlreadyHaveAccount);

        // "Get Started" → open the registration flow
        btnGetStarted.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));

        // "Already have an account? Log In" → open the login flow
        tvAlreadyHaveAccount.setOnClickListener(v ->
                startActivity(new Intent(this, LoginActivity.class)));
    }
}
