package com.example.nearbuy;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;

public class NotificationsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        // Enable back button in action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        setupCardListeners();
        setupMarkAllRead();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupCardListeners() {
        int[] cardIds = {
            R.id.card_nd1,
            R.id.card_nd2,
            R.id.card_exp1,
            R.id.card_exp2,
            R.id.card_promo1,
            R.id.card_promo2
        };

        String[] messages = {
            "50% off Pizza Palace",
            "Buy 1 Get 1 at FreshMart",
            "Tech Zone deal – expiring in 2 hours!",
            "Fashion Hub Flash Sale – ends midnight!",
            "Earn 200 Reward Points today",
            "Free Delivery Weekend activated"
        };

        for (int i = 0; i < cardIds.length; i++) {
            MaterialCardView card = findViewById(cardIds[i]);
            final String msg = messages[i];
            if (card != null) {
                card.setOnClickListener(v -> {
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                    // Navigate to deal details if needed
                    Intent intent = new Intent(NotificationsActivity.this, deal_details.class);
                    intent.putExtra("deal_title", msg);
                    startActivity(intent);
                });
            }
        }
    }

    private void setupMarkAllRead() {
        TextView tvMarkRead = findViewById(R.id.tv_mark_read);
        if (tvMarkRead != null) {
            tvMarkRead.setOnClickListener(v ->
                Toast.makeText(this, "All notifications marked as read", Toast.LENGTH_SHORT).show()
            );
        }
    }
}
