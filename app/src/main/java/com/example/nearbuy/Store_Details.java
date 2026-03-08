package com.example.nearbuy;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class Store_Details extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_store_details);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Get store data from intent if available
        Intent intent = getIntent();
        String storeName = intent.getStringExtra("store_name");
        String storeLocation = intent.getStringExtra("store_location");

        // Display sample data
        if (storeName != null) {
            Toast.makeText(this, "Store: " + storeName, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Sample Store Details Loaded", Toast.LENGTH_SHORT).show();
        }

        setupStoreDetails();
    }

    private void setupStoreDetails() {
        // Setup store information display
        // Example: Store hours, contact info, deals available, etc.
    }
}