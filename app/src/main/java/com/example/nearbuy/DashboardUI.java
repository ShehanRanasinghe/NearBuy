package com.example.nearbuy;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class DashboardUI extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dashboard_ui);

        // Initialize bottom navigation
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_home);

        // Setup bottom navigation listener
        bottomNavigationView.setOnItemSelectedListener(new BottomNavigationView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();

                if (itemId == R.id.nav_home) {
                    // Already on home/dashboard
                    return true;
                } else if (itemId == R.id.nav_categories) {
                    startActivity(new Intent(DashboardUI.this, CategoriesActivity.class));
                    overridePendingTransition(0, 0);
                    return true;
                } else if (itemId == R.id.nav_saved) {
                    startActivity(new Intent(DashboardUI.this, Saved_Deals.class));
                    overridePendingTransition(0, 0);
                    return true;
                } else if (itemId == R.id.nav_profile) {
                    startActivity(new Intent(DashboardUI.this, Profile.class));
                    overridePendingTransition(0, 0);
                    return true;
                } else if (itemId == R.id.nav_settings) {
                    startActivity(new Intent(DashboardUI.this, Settings.class));
                    overridePendingTransition(0, 0);
                    return true;
                }
                return false;
            }
        });

        setupClickListeners();
    }

    private void setupClickListeners() {
        // Setup notification bell click
        ImageView imgNotification = findViewById(R.id.imgNotification);
        if (imgNotification != null) {
            imgNotification.setOnClickListener(v -> {
                Intent intent = new Intent(DashboardUI.this, NotificationsActivity.class);
                startActivity(intent);
            });
        }

        // Setup search bar click
        android.widget.EditText etSearch = findViewById(R.id.etSearch);
        if (etSearch != null) {
            etSearch.setOnClickListener(v -> {
                Intent intent = new Intent(DashboardUI.this, SearchActivity.class);
                startActivity(intent);
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Re-select the home item when returning to this activity
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
        }
    }
}