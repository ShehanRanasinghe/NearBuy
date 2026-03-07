package com.example.nearbuy;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;

public class NotificationsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        setupCardListeners();
        setupMarkAllRead();
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
                card.setOnClickListener(v ->
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                );
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
