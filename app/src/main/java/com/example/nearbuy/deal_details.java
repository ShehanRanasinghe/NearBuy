package com.example.nearbuy;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class deal_details extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_deal_details);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Get deal data from intent if available
        Intent intent = getIntent();
        String dealTitle = intent.getStringExtra("deal_title");
        String dealDiscount = intent.getStringExtra("deal_discount");

        // Display sample data
        if (dealTitle != null) {
            Toast.makeText(this, "Deal: " + dealTitle, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Sample Deal Details Loaded", Toast.LENGTH_SHORT).show();
        }

        setupButtons();
    }

    private void setupButtons() {
        // Setup any buttons in the layout if they exist
        // Example: Save deal button, share button, etc.
    }
}