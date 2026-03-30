package com.example.nearbuy.auth;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.nearbuy.R;
import com.example.nearbuy.data.repository.AuthRepository;
import com.example.nearbuy.data.repository.OperationCallback;

/**
 * OTPVerificationActivity – verifies the 6-digit OTP sent to the customer's
 * email address after they submit the registration form.
 *
 * Flow:
 *   1. OTP was already sent by RegisterActivity via AuthRepository.initiateRegistration().
 *      On arrival here, the resend countdown timer starts immediately.
 *   2. Customer enters the 6 digits (auto-advance between boxes).
 *   3. On "Verify" → AuthRepository.verifyOtpAndRegister() checks the code against
 *      Firestore, creates the Firebase Auth account, writes the customer profile,
 *      and saves the session.
 *   4. On success → navigate to DashboardActivity (back-stack fully cleared).
 *   5. "Resend" → only enabled after the 60-second cooldown expires; calls
 *      AuthRepository.initiateRegistration() to generate and email a fresh code.
 */
public class OTPVerificationActivity extends AppCompatActivity {

    private static final String TAG               = "NearBuy.OTPVerify";
    private static final long   RESEND_COOLDOWN   = 60_000L; // 60 seconds

    // ── Intent extra keys ──────────────────────────────────────────────────────
    public static final String EXTRA_NAME     = "name";
    public static final String EXTRA_EMAIL    = "email";
    public static final String EXTRA_PHONE    = "phone";
    public static final String EXTRA_PASSWORD = "password";

    // ── UI references ──────────────────────────────────────────────────────────
    private EditText  otp1, otp2, otp3, otp4, otp5, otp6;
    private Button    btnVerify;
    private TextView  btnResendOTP, tvCountdown, tvEmailSentTo;

    // ── Registration data (received from RegisterActivity) ─────────────────────
    private String regName, regEmail, regPhone, regPassword;

    // ── Dependencies ───────────────────────────────────────────────────────────
    private AuthRepository authRepository;
    private final Handler  mainHandler = new Handler(Looper.getMainLooper());

    // ── Resend timer ───────────────────────────────────────────────────────────
    private CountDownTimer resendTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp_verification);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        authRepository = new AuthRepository();

        // Read all registration data passed from RegisterActivity
        regName     = getIntent().getStringExtra(EXTRA_NAME);
        regEmail    = getIntent().getStringExtra(EXTRA_EMAIL);
        regPhone    = getIntent().getStringExtra(EXTRA_PHONE);
        regPassword = getIntent().getStringExtra(EXTRA_PASSWORD);
        if (regName == null || regName.isEmpty()) regName = "Customer";

        bindViews();

        // Show the email address the OTP was sent to
        if (tvEmailSentTo != null)
            tvEmailSentTo.setText(getString(R.string.otp_code_sent_to, regEmail));

        setupOtpAutoAdvance();

        // OTP was already sent from RegisterActivity – start the resend cooldown right away
        startResendCountdown();

        btnVerify.setOnClickListener(v -> verifyOtp());

        btnResendOTP.setOnClickListener(v -> {
            clearOtpFields();
            resendOtp();
        });
    }

    // ── View binding ───────────────────────────────────────────────────────────

    private void bindViews() {
        otp1         = findViewById(R.id.otp1);
        otp2         = findViewById(R.id.otp2);
        otp3         = findViewById(R.id.otp3);
        otp4         = findViewById(R.id.otp4);
        otp5         = findViewById(R.id.otp5);
        otp6         = findViewById(R.id.otp6);
        btnVerify    = findViewById(R.id.btnVerify);
        btnResendOTP = findViewById(R.id.btnResendOTP);
        tvCountdown  = findViewById(R.id.tvCountdown);
        tvEmailSentTo = findViewById(R.id.tvEmailSentTo);
    }

    // ── OTP Verification ───────────────────────────────────────────────────────

    /**
     * Reads the 6 digit boxes, validates completeness, then calls
     * AuthRepository.verifyOtpAndRegister() to check the code and create the account.
     */
    private void verifyOtp() {
        String code = getEnteredOtp();
        if (code.length() < 6) {
            Toast.makeText(this,
                    "Please enter the complete 6-digit code.", Toast.LENGTH_SHORT).show();
            return;
        }

        setVerifyEnabled(false);
        btnVerify.setText(R.string.otp_verifying);

        authRepository.verifyOtpAndRegister(
                regName, regEmail, regPhone, regPassword, code, this,
                new OperationCallback() {
                    @Override
                    public void onSuccess() {
                        // Account created → prompt user to log in
                        Log.d(TAG, "OTP verified – registration complete.");
                        Toast.makeText(OTPVerificationActivity.this,
                                "Account verified! Please log in to continue.",
                                Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(OTPVerificationActivity.this,
                                LoginActivity.class);
                        // Clear back-stack so the customer cannot navigate back to register/OTP
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onError(Exception e) {
                        setVerifyEnabled(true);
                        Log.e(TAG, "OTP verification failed", e);

                        // Show a descriptive dialog for the most common failure cases
                        String msg = e.getMessage() != null ? e.getMessage() : "";
                        if (msg.contains("expired") || msg.contains("Expired")) {
                            showErrorDialog("Code Expired",
                                    "Your code has expired. Tap Resend to get a new one.");
                            clearOtpFields();
                        } else if (msg.contains("Incorrect") || msg.contains("incorrect")) {
                            showErrorDialog("Incorrect Code",
                                    "The code you entered is wrong. Please try again.");
                            clearOtpFields();
                        } else if (msg.contains("not found") || msg.contains("Not found")) {
                            showErrorDialog("Code Not Found",
                                    "No verification code was found. Please request a new one.");
                        } else {
                            Toast.makeText(OTPVerificationActivity.this,
                                    msg.isEmpty() ? "Verification failed. Please try again." : msg,
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    // ── Resend OTP ─────────────────────────────────────────────────────────────

    /**
     * Generates a fresh OTP, stores it in Firestore, and emails it.
     * Starts the 60-second resend cooldown on success.
     */
    private void resendOtp() {
        setVerifyEnabled(false);
        btnResendOTP.setEnabled(false);
        if (tvCountdown != null) {
            tvCountdown.setVisibility(View.VISIBLE);
            tvCountdown.setText(R.string.otp_sending);
        }

        authRepository.initiateRegistration(regPhone, regEmail, regName,
                new com.example.nearbuy.data.repository.DataCallback<String>() {
                    @Override
                    public void onSuccess(String newCode) {
                        Log.d(TAG, "OTP resent to: " + regEmail);
                        setVerifyEnabled(true);
                        Toast.makeText(OTPVerificationActivity.this,
                                "New code sent to " + regEmail, Toast.LENGTH_LONG).show();
                        startResendCountdown();
                    }

                    @Override
                    public void onError(Exception e) {
                        setVerifyEnabled(true);
                        btnResendOTP.setEnabled(true);
                        if (tvCountdown != null) tvCountdown.setVisibility(View.GONE);
                        Log.e(TAG, "Resend failed", e);
                        Toast.makeText(OTPVerificationActivity.this,
                                "Could not resend code. Check your connection.",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ── Resend countdown ───────────────────────────────────────────────────────

    /**
     * Starts a 60-second countdown during which the Resend button is disabled.
     * Shows the remaining time in tvCountdown.  When the timer finishes the
     * Resend button is re-enabled.
     */
    private void startResendCountdown() {
        if (resendTimer != null) resendTimer.cancel();

        btnResendOTP.setEnabled(false);
        if (tvCountdown != null) tvCountdown.setVisibility(View.VISIBLE);

        resendTimer = new CountDownTimer(RESEND_COOLDOWN, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (tvCountdown != null)
                    tvCountdown.setText(getString(
                            R.string.otp_resend_in, millisUntilFinished / 1000));
            }

            @Override
            public void onFinish() {
                btnResendOTP.setEnabled(true);
                if (tvCountdown != null)
                    tvCountdown.setText(R.string.otp_didnt_receive);
            }
        }.start();
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    /** Collects the text from all 6 digit boxes into one string. */
    private String getEnteredOtp() {
        return otp1.getText().toString().trim()
                + otp2.getText().toString().trim()
                + otp3.getText().toString().trim()
                + otp4.getText().toString().trim()
                + otp5.getText().toString().trim()
                + otp6.getText().toString().trim();
    }

    /** Clears all 6 digit boxes and moves focus back to the first box. */
    private void clearOtpFields() {
        otp1.setText(""); otp2.setText(""); otp3.setText("");
        otp4.setText(""); otp5.setText(""); otp6.setText("");
        otp1.requestFocus();
    }

    /**
     * Enables or disables the verify button and adjusts its alpha.
     * Also resets the button text to the default label when re-enabling.
     */
    private void setVerifyEnabled(boolean enabled) {
        btnVerify.setEnabled(enabled);
        btnVerify.setAlpha(enabled ? 1f : 0.6f);
        if (enabled) btnVerify.setText(R.string.otp_verify_code);
    }

    /**
     * Attaches TextWatchers to the 6 OTP boxes so focus moves forward when a
     * digit is typed and backward when it is deleted.
     * Uses afterTextChanged (fires after the change) for reliable length checks.
     */
    private void setupOtpAutoAdvance() {
        EditText[] fields = {otp1, otp2, otp3, otp4, otp5, otp6};
        for (int i = 0; i < fields.length; i++) {
            final int index = i;
            fields[i].addTextChangedListener(new TextWatcher() {
                public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
                public void onTextChanged(CharSequence s, int a, int b, int c) {}
                public void afterTextChanged(Editable s) {
                    if (s.length() == 1 && index < fields.length - 1)
                        fields[index + 1].requestFocus();  // digit typed → move forward
                    else if (s.length() == 0 && index > 0)
                        fields[index - 1].requestFocus();  // deleted → move backward
                }
            });
        }
    }

    /** Shows a simple AlertDialog with a title and message for error feedback. */
    private void showErrorDialog(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Cancel the countdown timer to prevent memory leaks after the activity is destroyed
        if (resendTimer != null) resendTimer.cancel();
    }
}
