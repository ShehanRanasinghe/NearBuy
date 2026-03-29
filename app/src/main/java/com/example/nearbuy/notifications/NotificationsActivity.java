package com.example.nearbuy.notifications;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.example.nearbuy.R;
public class NotificationsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window w = getWindow();
        w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        w.setStatusBarColor(ContextCompat.getColor(this, R.color.nb_teal_dark));
        setContentView(R.layout.activity_notifications);
        if (getSupportActionBar() != null) getSupportActionBar().hide();
        setupMarkRead();
        setupCardClicks();
    }
    private void setupMarkRead() {
        TextView tvMarkRead = findViewById(R.id.tv_mark_read);
        if (tvMarkRead != null) {
            tvMarkRead.setOnClickListener(v -> {
                Toast.makeText(this, "All notifications marked as read", Toast.LENGTH_SHORT).show();
                tvMarkRead.setVisibility(View.GONE);
            });
        }
    }
    private void setupCardClicks() {
        setCardClick(R.id.card_nd1,    "Fresh Apples 50% OFF at FreshMart");
        setCardClick(R.id.card_nd2,    "Salmon Fillet Deal at SeaFresh Market");
        setCardClick(R.id.card_promo1, "Buy 2 Get 1 Free - Yoghurt at DairyPlus");
        setCardClick(R.id.card_promo2, "Weekend Flash Sale - GreenLeaf Store");
        setCardClick(R.id.card_exp1,   "Deal Expiring Tomorrow - Broccoli Offer");
        setCardClick(R.id.card_exp2,   "Flash Sale Ends Tonight!");
    }
    private void setCardClick(int cardId, String message) {
        View card = findViewById(cardId);
        if (card != null) {
            card.setOnClickListener(v ->
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
        }
    }
}