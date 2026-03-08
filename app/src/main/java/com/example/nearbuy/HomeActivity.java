package com.example.nearbuy;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class HomeActivity extends AppCompatActivity {

    private EditText searchBar;
    private CardView categoryVegetables, categoryMeats, categoryBeverages, categoryFruits, categorySnacks, categoryBreads;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Initialize views
        searchBar = findViewById(R.id.search_bar);

        // Categories
        categoryVegetables = findViewById(R.id.category_vegetables);
        categoryMeats = findViewById(R.id.category_meats);
        categoryBeverages = findViewById(R.id.category_beverages);
        categoryFruits = findViewById(R.id.category_fruits);
        categorySnacks = findViewById(R.id.category_snacks);
        categoryBreads = findViewById(R.id.category_breads);

        // Set click listeners
        searchBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(HomeActivity.this, SearchActivity.class));
            }
        });

        // Category click listeners
        categoryVegetables.setOnClickListener(v -> openCategory("Vegetables"));
        categoryMeats.setOnClickListener(v -> openCategory("Meats"));
        categoryBeverages.setOnClickListener(v -> openCategory("Beverages"));
        categoryFruits.setOnClickListener(v -> openCategory("Fruits"));
        categorySnacks.setOnClickListener(v -> openCategory("Snacks"));
        categoryBreads.setOnClickListener(v -> openCategory("Breads"));
    }

    private void openCategory(String categoryName) {
        Intent intent = new Intent(HomeActivity.this, CategoriesActivity.class);
        intent.putExtra("category", categoryName);
        startActivity(intent);
    }
}

