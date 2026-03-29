package com.example.nearbuy.profile;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.nearbuy.R;
import com.example.nearbuy.dashboard.DashboardActivity;
import com.example.nearbuy.discounts.DealsActivity;
import com.example.nearbuy.search.SearchActivity;

/**
 * ProfileActivity – Customer profile with account info, edit profile and logout.
 */
public class ProfileActivity extends AppCompatActivity {

    private String currentName     = "Kamal Perera";
    private String currentEmail    = "kamal.perera@gmail.com";
    private String currentPhone    = "+94 77 123 4567";
    private String currentLocation = "Colombo 03, Sri Lanka";

    // Bottom nav
    private LinearLayout navHome, navSearch, navDeals, navProfile;
    private ImageView navHomeIcon, navSearchIcon, navDealsIcon, navProfileIcon;
    private TextView navHomeText, navSearchText, navDealsText, navProfileText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window w = getWindow();
        w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        w.setStatusBarColor(ContextCompat.getColor(this, R.color.nb_primary_dark));
        w.setNavigationBarColor(ContextCompat.getColor(this, R.color.bg_white));

        setContentView(R.layout.activity_profile);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        String intentName = getIntent().getStringExtra("userName");
        if (intentName != null && !intentName.isEmpty()) currentName = intentName;

        populateProfileData();
        setupButtons();
        setupBottomNavigation();
    }

    private void populateProfileData() {
        setTextSafe(R.id.tv_user_name,  currentName);
        setTextSafe(R.id.tv_user_role,  "Customer");
        setTextSafe(R.id.tv_full_name,  currentName);
        setTextSafe(R.id.tv_email,      currentEmail);
        setTextSafe(R.id.tv_phone,      currentPhone);
        setTextSafe(R.id.tv_location,   currentLocation);
    }

    private void setupButtons() {
        clickSafe(R.id.btn_back, v -> finish());

        clickSafe(R.id.btn_edit_profile, v -> showEditProfileDialog());

        clickSafe(R.id.btn_logout, v -> {
            Intent intent = new Intent(this, LogoutActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    private void showEditProfileDialog() {
        buildSimpleEditDialog();
    }

    private void buildSimpleEditDialog() {
        // Build a simple inline edit dialog using AlertDialog
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = (int)(16 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad, pad, pad);

        final EditText etName     = makeEditField("Full Name",      currentName);
        final EditText etEmail    = makeEditField("Email",          currentEmail);
        final EditText etPhone    = makeEditField("Phone Number",   currentPhone);
        final EditText etLocation = makeEditField("Location",       currentLocation);

        layout.addView(etName);
        layout.addView(etEmail);
        layout.addView(etPhone);
        layout.addView(etLocation);

        new AlertDialog.Builder(this)
                .setTitle("Edit Profile")
                .setView(layout)
                .setPositiveButton("Save", (dialog, which) -> {
                    currentName     = etName.getText().toString().trim().isEmpty()     ? currentName     : etName.getText().toString().trim();
                    currentEmail    = etEmail.getText().toString().trim().isEmpty()    ? currentEmail    : etEmail.getText().toString().trim();
                    currentPhone    = etPhone.getText().toString().trim().isEmpty()    ? currentPhone    : etPhone.getText().toString().trim();
                    currentLocation = etLocation.getText().toString().trim().isEmpty() ? currentLocation : etLocation.getText().toString().trim();
                    populateProfileData();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private EditText makeEditField(String hint, String value) {
        EditText et = new EditText(this);
        et.setHint(hint);
        et.setText(value);
        int margin = (int)(4 * getResources().getDisplayMetrics().density);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, margin, 0, margin);
        et.setLayoutParams(lp);
        return et;
    }

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

        if (navProfileIcon != null)
            navProfileIcon.setColorFilter(ContextCompat.getColor(this, R.color.nb_primary));
        if (navProfileText != null)
            navProfileText.setTextColor(ContextCompat.getColor(this, R.color.nb_primary));

        if (navHome   != null) navHome.setOnClickListener(v   -> { startActivity(new Intent(this, DashboardActivity.class)); finish(); });
        if (navSearch != null) navSearch.setOnClickListener(v -> startActivity(new Intent(this, SearchActivity.class)));
        if (navDeals  != null) navDeals.setOnClickListener(v  -> startActivity(new Intent(this, DealsActivity.class)));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void setTextSafe(int id, String text) {
        TextView tv = findViewById(id);
        if (tv != null) tv.setText(text);
    }

    private void clickSafe(int id, View.OnClickListener listener) {
        View v = findViewById(id);
        if (v != null) v.setOnClickListener(listener);
    }
}
