package com.example.nearbuy.profile;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.nearbuy.R;
import com.example.nearbuy.core.SessionManager;
import com.example.nearbuy.dashboard.DashboardActivity;
import com.example.nearbuy.data.repository.AuthRepository;
import com.example.nearbuy.data.repository.OperationCallback;
import com.example.nearbuy.orders.OrdersActivity;
import com.example.nearbuy.search.SearchActivity;

/**
 * ProfileActivity – displays the signed-in customer's profile information
 * and provides Edit Profile and Log Out functionality.
 *
 * All displayed data comes from SessionManager (populated during login from
 * Firestore).  Profile updates are written back to both Firestore (via
 * AuthRepository.updateProfile) and the local SessionManager cache.
 *
 * Session guard: if SessionManager has no UID (should not happen in normal
 * flow since SplashScreen redirects to Welcome if not logged in), the activity
 * redirects to WelcomeActivity.
 */
public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "NearBuy.Profile";

    // ── Bottom navigation ──────────────────────────────────────────────────────
    private LinearLayout navHome, navSearch, navDeals, navProfile;
    private ImageView    navHomeIcon, navSearchIcon, navDealsIcon, navProfileIcon;
    private TextView     navHomeText, navSearchText, navDealsText, navProfileText;

    // ── Dependencies ───────────────────────────────────────────────────────────
    private SessionManager sessionManager;
    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window w = getWindow();
        w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        w.setStatusBarColor(ContextCompat.getColor(this, R.color.nb_primary_dark));
        w.setNavigationBarColor(ContextCompat.getColor(this, R.color.bg_white));

        setContentView(R.layout.activity_profile);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        sessionManager = SessionManager.getInstance(this);
        authRepository = new AuthRepository();

        // ── Session guard ─────────────────────────────────────────────────────
        // Redirect to Welcome if the session has expired or been cleared.
        if (!sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, com.example.nearbuy.app.startup.WelcomeActivity.class));
            finish();
            return;
        }

        populateProfileData();
        setupButtons();
        setupBottomNavigation();
    }

    // ── Populate from SessionManager ───────────────────────────────────────────

    /**
     * Reads the customer's profile data from SessionManager (which was populated
     * from Firestore during login) and fills all the profile display fields.
     * This is instant – no network call required.
     */
    private void populateProfileData() {
        String name  = sessionManager.getUserName();
        String email = sessionManager.getUserEmail();
        String phone = sessionManager.getUserPhone();

        setTextSafe(R.id.tv_user_name,  name);
        setTextSafe(R.id.tv_user_role,  "Customer");
        setTextSafe(R.id.tv_full_name,  name);
        setTextSafe(R.id.tv_email,      email.isEmpty() ? "Not set" : email);
        setTextSafe(R.id.tv_phone,      phone.isEmpty() ? "Not set" : phone);
        // Location is shown from the last known coordinates saved in session
        String loc = sessionManager.hasLocation()
                ? String.format("%.4f, %.4f",
                sessionManager.getLastLatitude(),
                sessionManager.getLastLongitude())
                : "Location not set";
        setTextSafe(R.id.tv_location, loc);
    }

    // ── Button wiring ──────────────────────────────────────────────────────────

    /** Sets up the Edit Profile dialog and the Logout button. */
    private void setupButtons() {
        clickSafe(R.id.btn_back, v -> finish());

        // "Edit Profile" → show an in-line dialog to update name / email / phone
        clickSafe(R.id.btn_edit_profile, v -> showEditProfileDialog());

        // "Log Out" → show the logout confirmation screen
        clickSafe(R.id.btn_logout, v -> {
            Intent intent = new Intent(this, LogoutActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    // ── Edit Profile dialog ────────────────────────────────────────────────────

    /**
     * Shows an AlertDialog with editable text fields pre-filled with the
     * customer's current profile data.  On "Save", the changes are written
     * to Firestore via AuthRepository.updateProfile() and the local
     * SessionManager cache is refreshed immediately.
     */
    private void showEditProfileDialog() {
        // Build the dialog layout programmatically
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad, pad, pad);

        final EditText etName  = makeEditField("Full Name",    sessionManager.getUserName());
        final EditText etEmail = makeEditField("Email",        sessionManager.getUserEmail());
        final EditText etPhone = makeEditField("Phone Number", sessionManager.getUserPhone());

        layout.addView(etName);
        layout.addView(etEmail);
        layout.addView(etPhone);

        new AlertDialog.Builder(this)
                .setTitle("Edit Profile")
                .setView(layout)
                .setPositiveButton("Save", (dialog, which) -> saveProfileChanges(
                        etName.getText().toString().trim(),
                        etEmail.getText().toString().trim(),
                        etPhone.getText().toString().trim()))
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Validates the edited values and calls AuthRepository.updateProfile()
     * to persist the changes to Firestore and refresh SessionManager.
     */
    private void saveProfileChanges(String newName, String newEmail, String newPhone) {
        if (TextUtils.isEmpty(newName)) {
            Toast.makeText(this, "Name cannot be empty.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Use existing values for any blank fields
        if (newEmail.isEmpty()) newEmail = sessionManager.getUserEmail();
        if (newPhone.isEmpty()) newPhone = sessionManager.getUserPhone();

        String uid = sessionManager.getUserId();
        if (uid.isEmpty()) {
            Toast.makeText(this, "Session error. Please log in again.", Toast.LENGTH_SHORT).show();
            return;
        }

        authRepository.updateProfile(uid, newName, newEmail, newPhone, this,
                new OperationCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "Profile updated successfully.");
                        Toast.makeText(ProfileActivity.this,
                                "Profile updated successfully.",
                                Toast.LENGTH_SHORT).show();
                        // Refresh the displayed values from the now-updated SessionManager
                        populateProfileData();
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e(TAG, "Profile update failed", e);
                        String msg = e.getMessage() != null ? e.getMessage()
                                : "Profile update failed. Please try again.";
                        Toast.makeText(ProfileActivity.this, msg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    // ── Bottom navigation ──────────────────────────────────────────────────────

    private void setupBottomNavigation() {
        navHome    = findViewById(R.id.navHome);
        navSearch  = findViewById(R.id.navSearch);
        navDeals   = findViewById(R.id.navDeals);
        navProfile = findViewById(R.id.navProfile);

        navHomeIcon    = findViewById(R.id.navHomeIcon);
        navSearchIcon  = findViewById(R.id.navSearchIcon);
        navDealsIcon   = findViewById(R.id.navDealsIcon);
        navProfileIcon = findViewById(R.id.navProfileIcon);

        navHomeText    = findViewById(R.id.navHomeText);
        navSearchText  = findViewById(R.id.navSearchText);
        navDealsText   = findViewById(R.id.navDealsText);
        navProfileText = findViewById(R.id.navProfileText);

        // Highlight the active Profile tab
        if (navProfileIcon != null)
            navProfileIcon.setColorFilter(ContextCompat.getColor(this, R.color.nb_primary));
        if (navProfileText != null)
            navProfileText.setTextColor(ContextCompat.getColor(this, R.color.nb_primary));

        if (navHome   != null) navHome.setOnClickListener(v -> {
            startActivity(new Intent(this, DashboardActivity.class));
            finish();
        });
        if (navSearch != null) navSearch.setOnClickListener(v ->
                startActivity(new Intent(this, SearchActivity.class)));
        if (navDeals  != null) navDeals.setOnClickListener(v ->
                startActivity(new Intent(this, OrdersActivity.class)));
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private EditText makeEditField(String hint, String value) {
        EditText et = new EditText(this);
        et.setHint(hint);
        et.setText(value);
        int margin = (int) (4 * getResources().getDisplayMetrics().density);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, margin, 0, margin);
        et.setLayoutParams(lp);
        return et;
    }

    private void setTextSafe(int id, String text) {
        TextView tv = findViewById(id);
        if (tv != null) tv.setText(text);
    }

    private void clickSafe(int id, View.OnClickListener listener) {
        View v = findViewById(id);
        if (v != null) v.setOnClickListener(listener);
    }
}
