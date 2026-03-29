package com.example.nearbuy.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.nearbuy.R;
import com.example.nearbuy.app.startup.WelcomeActivity;
import com.example.nearbuy.deals.SavedDealsActivity;
import com.example.nearbuy.notifications.NotificationsActivity;
import com.example.nearbuy.orders.OrdersActivity;

/**
 * ProfileActivity – Customer profile, account info and settings.
 */
public class ProfileActivity extends AppCompatActivity {

    // Sample customer data (replace with session/shared-prefs later)
    private static final String SAMPLE_NAME  = "Kamal Perera";
    private static final String SAMPLE_EMAIL = "kamal.perera@gmail.com";
    private static final String SAMPLE_PHONE = "+94 77 123 4567";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window w = getWindow();
        w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        w.setStatusBarColor(ContextCompat.getColor(this, R.color.nb_primary_dark));

        setContentView(R.layout.activity_profile);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        populateProfileData();
        setupButtons();
    }

    private void populateProfileData() {
        // Name from intent (passed from login) or fall back to sample
        String name = getIntent().getStringExtra("userName");
        if (name == null || name.isEmpty()) name = SAMPLE_NAME;

        setTextSafe(R.id.tv_user_name,  name);
        setTextSafe(R.id.tv_user_role,  "Customer");    // always "Customer" in this app
        setTextSafe(R.id.tv_full_name,  name);
        setTextSafe(R.id.tv_email,      SAMPLE_EMAIL);
        setTextSafe(R.id.tv_phone,      SAMPLE_PHONE);
    }

    private void setupButtons() {
        // Back button
        clickSafe(R.id.btn_back,            v -> finish());

        // My Orders row (if the layout has one)
        clickSafe(R.id.btn_my_orders,       v ->
                startActivity(new Intent(this, OrdersActivity.class)));

        // Saved Deals
        clickSafe(R.id.btn_saved_deals,     v ->
                startActivity(new Intent(this, SavedDealsActivity.class)));

        // Change Password
        clickSafe(R.id.btn_change_password, v ->
                Toast.makeText(this, "Change Password – coming soon", Toast.LENGTH_SHORT).show());

        // Notifications settings row
        clickSafe(R.id.btn_notifications,   v ->
                startActivity(new Intent(this, NotificationsActivity.class)));

        // Language
        clickSafe(R.id.btn_language,        v ->
                Toast.makeText(this, "Language settings – coming soon", Toast.LENGTH_SHORT).show());

        // Help and Support
        clickSafe(R.id.btn_help,            v ->
                Toast.makeText(this, "Help & Support – coming soon", Toast.LENGTH_SHORT).show());

        // Edit Profile
        clickSafe(R.id.btn_edit_profile,    v ->
                Toast.makeText(this, "Edit Profile – coming soon", Toast.LENGTH_SHORT).show());

        // Logout  (ID from layout is btn_logout)
        clickSafe(R.id.btn_logout, v -> {
            Toast.makeText(this, "You have been logged out", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, WelcomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void setTextSafe(int id, String text) {
        TextView tv = findViewById(id);
        if (tv != null) tv.setText(text);
    }

    private void clickSafe(int id, android.view.View.OnClickListener listener) {
        android.view.View v = findViewById(id);
        if (v != null) v.setOnClickListener(listener);
    }
}
