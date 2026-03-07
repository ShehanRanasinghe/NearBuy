package com.example.nearbuy;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;

public class CategoriesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_categories);

        setupCategoryCards();
    }

    private void setupCategoryCards() {
        int[] cardIds = {
            R.id.card_food,
            R.id.card_electronics,
            R.id.card_fashion,
            R.id.card_groceries,
            R.id.card_entertainment,
            R.id.card_health
        };

        String[] categoryNames = {
            getString(R.string.cat_food),
            getString(R.string.cat_electronics),
            getString(R.string.cat_fashion),
            getString(R.string.cat_groceries),
            getString(R.string.cat_entertainment),
            getString(R.string.cat_health)
        };

        for (int i = 0; i < cardIds.length; i++) {
            MaterialCardView card = findViewById(cardIds[i]);
            final String name = categoryNames[i];
            if (card != null) {
                card.setOnClickListener(v ->
                    Toast.makeText(this,
                        getString(R.string.cat_tap_message, name),
                        Toast.LENGTH_SHORT).show()
                );
            }
        }
    }
}
