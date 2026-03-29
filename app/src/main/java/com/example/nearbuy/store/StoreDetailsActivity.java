package com.example.nearbuy.store;

import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.nearbuy.R;
import com.example.nearbuy.search.SearchActivity;

/**
 * StoreDetailsActivity – Shows detailed info about a nearby shop.
 *
 * Displays: shop name/logo, address, hours, contact, rating,
 *           active deals count, distance, products preview.
 */
public class StoreDetailsActivity extends AppCompatActivity {

    public static final String EXTRA_SHOP_NAME  = "shopName";
    public static final String EXTRA_SHOP_EMOJI = "shopEmoji";
    public static final String EXTRA_DISTANCE   = "distance";
    public static final String EXTRA_ADDRESS    = "address";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window w = getWindow();
        w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        w.setStatusBarColor(ContextCompat.getColor(this, R.color.nb_primary_dark));

        setContentView(R.layout.activity_store_details);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        String shopName  = getIntent().getStringExtra(EXTRA_SHOP_NAME);
        String shopEmoji = getIntent().getStringExtra(EXTRA_SHOP_EMOJI);
        String distance  = getIntent().getStringExtra(EXTRA_DISTANCE);
        String address   = getIntent().getStringExtra(EXTRA_ADDRESS);

        populateData(shopName, shopEmoji, distance, address);
        setupButtons(shopName);
    }

    private void populateData(String shopName, String shopEmoji,
                               String distance, String address) {
        // Defaults for sample data
        if (shopName  == null) shopName  = "FreshMart Grocery";
        if (shopEmoji == null) shopEmoji = "🏪";
        if (distance  == null) distance  = "0.8";
        if (address   == null) address   = "No. 45, Galle Road, Colombo 03";

        setText(R.id.tv_store_name, shopName);

        // Store Info card
        setText(R.id.tv_location, address);
        setText(R.id.tv_contact,  "+94 11 234 5678");
        setText(R.id.tv_hours,    "7:00 AM – 10:00 PM Daily");
    }

    private void setupButtons(String shopName) {
        // Back
        safeClick(R.id.btn_back, v -> finish());

        // "View All Deals" button
        safeClick(R.id.btn_view_deals, v -> {
            Intent i = new Intent(this, SearchActivity.class);
            i.putExtra("query", shopName);
            startActivity(i);
        });

        // "Call Store" button
        safeClick(R.id.btn_call_store,
                v -> Toast.makeText(this, "Calling store…", Toast.LENGTH_SHORT).show());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void setText(int id, String text) {
        TextView tv = safeFind(id);
        if (tv != null) tv.setText(text);
    }

    private void safeClick(int id, android.view.View.OnClickListener l) {
        android.view.View v = safeFind(id);
        if (v != null) v.setOnClickListener(l);
    }

    @SuppressWarnings("unchecked")
    private <T extends android.view.View> T safeFind(int id) {
        try { return (T) findViewById(id); } catch (Exception e) { return null; }
    }
}
