package com.example.nearbuy.deals;
import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.example.nearbuy.R;
import com.example.nearbuy.store.StoreDetailsActivity;
public class DealDetailsActivity extends AppCompatActivity {
    public static final String EXTRA_EMOJI    = "emoji";
    public static final String EXTRA_TITLE    = "title";
    public static final String EXTRA_SHOP     = "shop";
    public static final String EXTRA_DISCOUNT = "discount";
    public static final String EXTRA_PRICE    = "price";
    public static final String EXTRA_ORIG     = "originalPrice";
    public static final String EXTRA_EXPIRY   = "expiry";
    public static final String EXTRA_CATEGORY = "category";
    public static final String EXTRA_DISTANCE = "distance";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window w = getWindow();
        w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        w.setStatusBarColor(ContextCompat.getColor(this, R.color.nb_primary_dark));
        setContentView(R.layout.activity_deal_details);
        if (getSupportActionBar() != null) getSupportActionBar().hide();
        populateData();
        setupButtons();
    }
    private void populateData() {
        String emoji    = getIntent().getStringExtra(EXTRA_EMOJI);
        String title    = getIntent().getStringExtra(EXTRA_TITLE);
        String shop     = getIntent().getStringExtra(EXTRA_SHOP);
        String discount = getIntent().getStringExtra(EXTRA_DISCOUNT);
        String price    = getIntent().getStringExtra(EXTRA_PRICE);
        String orig     = getIntent().getStringExtra(EXTRA_ORIG);
        String expiry   = getIntent().getStringExtra(EXTRA_EXPIRY);
        String distance = getIntent().getStringExtra(EXTRA_DISTANCE);
        setTv(R.id.tv_deal_emoji,    emoji    != null ? emoji    : "🍎");
        setTv(R.id.tv_deal_title,    title    != null ? title    : "Fresh Red Apples");
        setTv(R.id.tv_store_name,    shop     != null ? shop     : "FreshMart Grocery");
        setTv(R.id.tv_discount_badge,discount != null ? discount : "50% OFF");
        setTv(R.id.tv_deal_price,    price    != null ? price    : "Rs.100");
        setTv(R.id.tv_expiry_badge,  expiry   != null ? expiry   : "Expires in 3 days");
        setTv(R.id.tv_distance,      distance != null ? distance + " away" : "0.8 km away");
        TextView tvOrig = findViewById(R.id.tv_original_price);
        if (tvOrig != null) {
            tvOrig.setText(orig != null ? orig : "Rs.200");
            tvOrig.setPaintFlags(tvOrig.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        }
    }
    private void setupButtons() {
        android.view.View btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
        android.view.View btnShare = findViewById(R.id.btn_share);
        if (btnShare != null)
            btnShare.setOnClickListener(v ->
                    Toast.makeText(this, "Share deal link", Toast.LENGTH_SHORT).show());
        android.view.View btnSave = findViewById(R.id.btn_save_deal);
        if (btnSave != null)
            btnSave.setOnClickListener(v ->
                    Toast.makeText(this, "Deal saved!", Toast.LENGTH_SHORT).show());
        android.view.View btnRedeem = findViewById(R.id.btn_redeem_deal);
        if (btnRedeem != null)
            btnRedeem.setOnClickListener(v ->
                    Toast.makeText(this, "Deal redeemed!", Toast.LENGTH_SHORT).show());
    }
    private void setTv(int id, String text) {
        TextView tv = findViewById(id);
        if (tv != null) tv.setText(text);
    }
}