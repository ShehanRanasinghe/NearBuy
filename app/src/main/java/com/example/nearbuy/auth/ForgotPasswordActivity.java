package com.example.nearbuy.auth;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.nearbuy.R;
import com.example.nearbuy.data.repository.AuthRepository;
import com.example.nearbuy.data.repository.OperationCallback;

/**
 * ForgotPasswordActivity – lets customers request a Firebase password reset email.
 *
 * Flow:
 *   1. Customer enters their registered email address.
 *   2. AuthRepository.sendPasswordReset() triggers Firebase Auth to send a reset link.
 *   3. On success → show a Toast confirming the email was sent, then close this screen.
 *   4. On failure → show Firebase's error message (e.g. "email not found").
 */
public class ForgotPasswordActivity extends AppCompatActivity {

    private static final String TAG = "NearBuy.ForgotPwd";

    // UI references
    private EditText    etEmail;
    private Button      btnSendReset;
    private TextView    tvBackToLogin;
    private ProgressBar progressBar;

    // Repository dependency
    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        authRepository = new AuthRepository();

        bindViews();
        setClickListeners();
    }

    // ── View binding ───────────────────────────────────────────────────────────

    private void bindViews() {
        etEmail      = findViewById(R.id.etEmail);
        btnSendReset = findViewById(R.id.btnSendReset);
        tvBackToLogin = findViewById(R.id.tvBackToLogin);
        progressBar  = findViewById(R.id.progressBar);
    }

    // ── Click listeners ────────────────────────────────────────────────────────

    private void setClickListeners() {
        // "Send Reset Link" → trigger Firebase password reset email
        btnSendReset.setOnClickListener(v -> sendPasswordReset());

        // "Back to Login" → return to previous screen
        tvBackToLogin.setOnClickListener(v -> finish());
    }

    // ── Password reset logic ───────────────────────────────────────────────────

    /**
     * Validates the email field, then calls AuthRepository.sendPasswordReset().
     * Firebase Auth validates whether the email is registered; if not, an error
     * is returned and shown to the customer.
     */
    private void sendPasswordReset() {
        String email = etEmail.getText().toString().trim();

        // Local validation before making a network call
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Please enter your email address");
            etEmail.requestFocus();
            return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Please enter a valid email address");
            etEmail.requestFocus();
            return;
        }

        setLoadingState(true);

        authRepository.sendPasswordReset(email, new OperationCallback() {
            @Override
            public void onSuccess() {
                setLoadingState(false);
                Log.d(TAG, "Password reset email sent to: " + email);
                Toast.makeText(ForgotPasswordActivity.this,
                        "Password reset link sent to " + email
                                + ". Check your inbox.",
                        Toast.LENGTH_LONG).show();
                finish(); // Return to Login screen
            }

            @Override
            public void onError(Exception e) {
                setLoadingState(false);
                Log.e(TAG, "Password reset failed", e);
                String msg = e.getMessage() != null ? e.getMessage()
                        : "Failed to send reset link. Please try again.";
                Toast.makeText(ForgotPasswordActivity.this, msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    /** Toggle loading state on the send button and progress bar. */
    private void setLoadingState(boolean loading) {
        btnSendReset.setEnabled(!loading);
        btnSendReset.setText(loading ? "Sending…" : getString(R.string.btn_send_reset));
        if (progressBar != null)
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }
}
