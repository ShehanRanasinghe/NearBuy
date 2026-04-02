package com.example.nearbuy.auth;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import com.example.nearbuy.data.repository.AuthRepository;
import com.example.nearbuy.data.repository.DataCallback;

/**
 * RegisterActivity – collects customer details and initiates OTP verification.
 *
 * Flow:
 *   1. Validate all input fields (name, email, phone, password, confirm-password).
 *   2. Call AuthRepository.initiateRegistration() which generates a 6-digit OTP,
 *      stores it in Firestore at  otp_codes/{phone}, and returns the code.
 *   3. For development: the code is shown in a Toast so it can be entered manually.
 *      In production, replace the Toast with an SMS gateway call.
 *   4. Navigate to OTPVerificationActivity, passing all registration data so the
 *      account can be created after the OTP is verified.
 *
 * Important: the Firebase Auth account is NOT created here.  It is created only
 * after the OTP is successfully verified in OTPVerificationActivity.
 */
public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "NearBuy.Register";

    // UI references
    private EditText    etFullName, etEmail, etPhone, etPassword, etConfirmPassword;
    private ImageView   ivTogglePass, ivToggleConfirmPass;
    private Button      btnRegister;
    private TextView    tvLogin;
    private ProgressBar progressBar;

    // Repository dependency
    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        authRepository = new AuthRepository();

        bindViews();
        setClickListeners();
    }

    // ── View binding ───────────────────────────────────────────────────────────

    private void bindViews() {
        etFullName        = findViewById(R.id.etFullName);
        etEmail           = findViewById(R.id.etEmail);
        etPhone           = findViewById(R.id.etPhone);
        etPassword        = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        ivTogglePass        = findViewById(R.id.ivTogglePass);
        ivToggleConfirmPass = findViewById(R.id.ivToggleConfirmPass);
        btnRegister       = findViewById(R.id.btnRegister);
        tvLogin           = findViewById(R.id.tvLogin);
        progressBar       = findViewById(R.id.progressBar);
    }

    // ── Click listeners ────────────────────────────────────────────────────────

    private void setClickListeners() {
        // "Create Account" button → validate + request OTP
        btnRegister.setOnClickListener(v -> attemptRegister());

        // Password visibility toggles
        ivTogglePass.setOnClickListener(v -> togglePasswordVisibility(etPassword, ivTogglePass));
        ivToggleConfirmPass.setOnClickListener(v -> togglePasswordVisibility(etConfirmPassword, ivToggleConfirmPass));

        // "Already have an account? Log In" link → go back to Login
        tvLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
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

    // ── Registration logic ─────────────────────────────────────────────────────

    /**
     * Validates all input fields with detailed error messages, then calls
     * AuthRepository.initiateRegistration() to generate and store the OTP.
     */
    private void attemptRegister() {
        // Read and trim all inputs
        String name     = etFullName.getText().toString().trim();
        String email    = etEmail.getText().toString().trim();
        String phone    = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirm  = etConfirmPassword.getText().toString().trim();

        // ── Field validation ──
        if (TextUtils.isEmpty(name)) {
            etFullName.setError("Full name is required");
            etFullName.requestFocus();
            return;
        }
        if (name.length() < 2) {
            etFullName.setError("Name must be at least 2 characters");
            etFullName.requestFocus();
            return;
        }
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
        if (TextUtils.isEmpty(phone)) {
            etPhone.setError("Phone number is required");
            etPhone.requestFocus();
            return;
        }
        if (phone.length() < 9) {
            etPhone.setError("Please enter a valid phone number");
            etPhone.requestFocus();
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
        if (!password.equals(confirm)) {
            etConfirmPassword.setError("Passwords do not match");
            etConfirmPassword.requestFocus();
            return;
        }

        // Show loading state while generating/storing OTP
        setLoadingState(true);

        // ── Timeout guard ──────────────────────────────────────────────────────
        // If the Firestore write never calls back (e.g. permission denied, offline),
        // this handler fires after 20 seconds, re-enables the button, and shows
        // a clear error so the user is never stuck on "Sending OTP…" forever.
        final boolean[] responded = {false};
        final Handler timeoutHandler = new Handler(Looper.getMainLooper());
        timeoutHandler.postDelayed(() -> {
            if (!responded[0]) {
                responded[0] = true;
                setLoadingState(false);
                Toast.makeText(RegisterActivity.this,
                        "Request timed out. Check your internet connection and try again.",
                        Toast.LENGTH_LONG).show();
            }
        }, 20_000); // 20 seconds

        // Request an OTP for this phone number and email address
        authRepository.initiateRegistration(phone, email, name, new DataCallback<String>() {
            @Override
            public void onSuccess(String otpCode) {
                if (responded[0]) return; // timeout already fired – ignore late callback
                responded[0] = true;
                timeoutHandler.removeCallbacksAndMessages(null);
                setLoadingState(false);

                Log.d(TAG, "OTP generated for " + phone + " — sent to " + email);

                // Build the intent now so it's ready when the user taps "Continue"
                Intent intent = new Intent(RegisterActivity.this,
                        OTPVerificationActivity.class);
                intent.putExtra(OTPVerificationActivity.EXTRA_NAME,     name);
                intent.putExtra(OTPVerificationActivity.EXTRA_EMAIL,    email);
                intent.putExtra(OTPVerificationActivity.EXTRA_PHONE,    phone);
                intent.putExtra(OTPVerificationActivity.EXTRA_PASSWORD, password);

                startActivity(intent);
            }

            @Override
            public void onError(Exception e) {
                if (responded[0]) return; // timeout already fired
                responded[0] = true;
                timeoutHandler.removeCallbacksAndMessages(null);
                setLoadingState(false);
                Log.e(TAG, "OTP generation failed", e);
                String msg = e.getMessage() != null ? e.getMessage()
                        : "Failed to send OTP. Please try again.";
                Toast.makeText(RegisterActivity.this, msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    /** Toggle loading state on the register button and progress bar. */
    private void setLoadingState(boolean loading) {
        btnRegister.setEnabled(!loading);
        btnRegister.setText(loading ? "Sending OTP…" : getString(R.string.btn_register));
        if (progressBar != null)
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }
}
