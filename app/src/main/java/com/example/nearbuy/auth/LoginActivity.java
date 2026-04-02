package com.example.nearbuy.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.text.InputType;
import android.graphics.Typeface;

import androidx.appcompat.app.AppCompatActivity;

import com.example.nearbuy.R;
import com.example.nearbuy.dashboard.DashboardActivity;
import com.example.nearbuy.data.repository.AuthRepository;
import com.example.nearbuy.data.repository.OperationCallback;

/**
 * LoginActivity – lets registered customers sign in with email and password.
 *
 * Flow:
 *   1. Validate the email and password fields locally (not empty, valid format).
 *   2. Call AuthRepository.login() which signs in via Firebase Auth and loads
 *      the Firestore customer profile into SessionManager.
 *   3. On success → navigate to DashboardActivity, clearing the back stack so
 *      the customer cannot press Back to return to the login screen.
 *   4. On failure → display the Firebase error message in a Toast.
 *
 * Session note: If the user is already signed in, SplashScreen will bypass
 * this activity entirely and go straight to DashboardActivity.
 */
public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "NearBuy.Login";

    // UI references
    private EditText    etEmail, etPassword;
    private ImageView   ivTogglePassword;
    private Button      btnLogin;
    private TextView    tvForgotPassword, tvSignUp;
    private ProgressBar progressBar;

    // Repository dependency
    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialise the auth repository
        authRepository = new AuthRepository();

        bindViews();
        setClickListeners();
    }

    // ── View binding ───────────────────────────────────────────────────────────

    /** Bind all view references from the layout. */
    private void bindViews() {
        etEmail          = findViewById(R.id.etEmail);
        etPassword       = findViewById(R.id.etPassword);
        ivTogglePassword = findViewById(R.id.ivTogglePassword);
        btnLogin         = findViewById(R.id.btnLogin);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        tvSignUp         = findViewById(R.id.tvSignUp);
        progressBar      = findViewById(R.id.progressBar);  // optional – may be null in layout
    }

    // ── Click listeners ────────────────────────────────────────────────────────

    /** Wire up all button and text-link click handlers. */
    private void setClickListeners() {
        // "Log In" button → attempt Firebase sign-in
        btnLogin.setOnClickListener(v -> attemptLogin());

        // Password visibility toggle
        ivTogglePassword.setOnClickListener(v -> togglePasswordVisibility(etPassword, ivTogglePassword));

        // "Forgot Password?" link → open password reset screen
        tvForgotPassword.setOnClickListener(v ->
                startActivity(new Intent(this, ForgotPasswordActivity.class)));

        // "Sign Up" link → open registration screen
        tvSignUp.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
            finish(); // remove Login from back stack so Back on Register goes to Welcome
        });
    }

    // ── Password toggle helper ─────────────────────────────────────────────────

    /**
     * Toggles an EditText between hidden (textPassword) and visible (visiblePassword) modes,
     * updates the eye icon accordingly, and preserves cursor position and typeface.
     */
    private void togglePasswordVisibility(EditText field, ImageView icon) {
        Typeface tf = field.getTypeface();
        boolean isVisible = (field.getInputType() ==
                (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD));
        if (isVisible) {
            // Currently visible → hide
            field.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            icon.setImageResource(R.drawable.ic_eye_off);
        } else {
            // Currently hidden → show
            field.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            icon.setImageResource(R.drawable.ic_eye);
        }
        // Restore typeface and move cursor to end
        field.setTypeface(tf);
        field.setSelection(field.getText().length());
    }

    // ── Login logic ────────────────────────────────────────────────────────────

    /**
     * Validates input fields and calls AuthRepository.login().
     * Shows a loading indicator while the Firebase request is in flight.
     */
    private void attemptLogin() {
        // Read and trim the inputs
        String email    = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Local validation – give immediate feedback without a network round-trip
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email address is required");
            etEmail.requestFocus();
            return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Please enter a valid email address");
            etEmail.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            etPassword.requestFocus();
            return;
        }
        if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            etPassword.requestFocus();
            return;
        }

        // Show loading state while Firebase processes the request
        setLoadingState(true);

        authRepository.login(email, password, this, new OperationCallback() {
            @Override
            public void onSuccess() {
                // Login succeeded → session is saved, go to Dashboard
                setLoadingState(false);
                Log.d(TAG, "Login successful – navigating to Dashboard.");
                Intent intent = new Intent(LoginActivity.this, DashboardActivity.class);
                // FLAG_ACTIVITY_NEW_TASK | CLEAR_TASK ensures login screen is removed from stack
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(Exception e) {
                // Login failed – show Firebase's error message (e.g. wrong password, no account)
                setLoadingState(false);
                Log.e(TAG, "Login failed", e);
                String msg = e.getMessage() != null ? e.getMessage()
                        : "Login failed. Please check your credentials.";
                Toast.makeText(LoginActivity.this, msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    /**
     * Enables or disables the loading state:
     * shows the ProgressBar and disables the login button while a request is in flight.
     *
     * @param loading true = show loading, false = ready for input
     */
    private void setLoadingState(boolean loading) {
        btnLogin.setEnabled(!loading);
        btnLogin.setText(loading ? "Logging in…" : getString(R.string.btn_login));
        if (progressBar != null)
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }
}
