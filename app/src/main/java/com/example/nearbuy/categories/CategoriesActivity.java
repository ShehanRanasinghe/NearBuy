package com.example.nearbuy.categories;

import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.nearbuy.R;
import com.example.nearbuy.search.SearchActivity;

/**
 * CategoriesActivity – Grid of all product categories.
 *
 * Tapping any category card opens SearchActivity pre-filtered
 * with the category name as the search query.
 *
 * Categories: Food, Electronics, Fashion, Groceries, Entertainment, Health
 */
public class CategoriesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window w = getWindow();
        w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        w.setStatusBarColor(ContextCompat.getColor(this, R.color.cat_toolbar_dark));

        setContentView(R.layout.activity_categories);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        setupCategoryCards();
    }

    private void setupCategoryCards() {
        cardSearch(R.id.card_food,          "Food");
        cardSearch(R.id.card_electronics,   "Electronics");
        cardSearch(R.id.card_fashion,       "Fashion");
        cardSearch(R.id.card_groceries,     "Groceries");
        cardSearch(R.id.card_entertainment, "Entertainment");
        cardSearch(R.id.card_health,        "Health");
    }

    private void cardSearch(int cardId, String query) {
        android.view.View card = findViewById(cardId);
        if (card != null) {
            card.setOnClickListener(v -> {
                Intent intent = new Intent(this, SearchActivity.class);
                intent.putExtra("query", query);
                startActivity(intent);
            });
        }
    }
}
