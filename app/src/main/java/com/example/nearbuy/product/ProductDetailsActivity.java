package com.example.nearbuy.product;

import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.nearbuy.R;
import com.example.nearbuy.store.StoreDetailsActivity;

/**
 * ProductDetailsActivity – Shows full details of a product/deal.
 *
 * Receives product data via Intent extras and displays:
 *  - Product emoji, name, category, in-stock status
 *  - Sale price, original price, savings amount
 *  - Shop name, distance, rating
 *  - Description, weight, expiry
 *  - Add to cart / Save deal buttons
 */
public class ProductDetailsActivity extends AppCompatActivity {

    // Intent extra keys (used by callers)
    public static final String EXTRA_EMOJI         = "emoji";
    public static final String EXTRA_NAME          = "name";
    public static final String EXTRA_SHOP_NAME     = "shopName";
    public static final String EXTRA_SHOP_EMOJI    = "shopEmoji";
    public static final String EXTRA_PRICE         = "price";
    public static final String EXTRA_ORIGINAL_PRICE= "originalPrice";
    public static final String EXTRA_DISTANCE      = "distance";
    public static final String EXTRA_CATEGORY      = "category";
    public static final String EXTRA_DISCOUNT      = "discount";

    private TextView tvProductEmoji, tvProductName, tvCategory, tvInStock;
    private TextView tvSalePrice, tvOriginalPrice;
    private TextView tvShopEmoji, tvShopName, tvDistance, tvRating;
    private TextView tvDescription, tvWeight, tvCategoryDetail, tvDealExpiry;
    private TextView btnViewStore;
    private TextView btnDelivery, btnPickUp;
    private Button   btnPlaceOrder;
    private boolean  isDeliverySelected = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window w = getWindow();
        w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        w.setStatusBarColor(ContextCompat.getColor(this, R.color.nb_primary_dark));

        setContentView(R.layout.activity_product_details);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        initViews();
        populateData();
        setupButtons();
    }

    private void initViews() {
        tvProductEmoji   = findViewById(R.id.tvProductEmoji);
        tvProductName    = findViewById(R.id.tvProductName);
        tvCategory       = findViewById(R.id.tvCategory);
        tvInStock        = findViewById(R.id.tvInStock);
        tvSalePrice      = findViewById(R.id.tvSalePrice);
        tvOriginalPrice  = findViewById(R.id.tvOriginalPrice);
        tvShopEmoji      = findViewById(R.id.tvShopEmoji);
        tvShopName       = findViewById(R.id.tvShopName);
        tvDistance       = findViewById(R.id.tvDistance);
        tvRating         = findViewById(R.id.tvRating);
        tvDescription    = findViewById(R.id.tvDescription);
        tvWeight         = findViewById(R.id.tvWeight);
        tvCategoryDetail = findViewById(R.id.tvCategoryDetail);
        tvDealExpiry     = findViewById(R.id.tvDealExpiry);
        btnViewStore     = findViewById(R.id.btnViewStore);
        btnDelivery      = findViewById(R.id.btnDelivery);
        btnPickUp        = findViewById(R.id.btnPickUp);
        btnPlaceOrder    = findViewById(R.id.btnPlaceOrder);
    }

    private void populateData() {
        // Read from intent or use defaults (sample data)
        String emoji     = getIntent().getStringExtra(EXTRA_EMOJI);
        String name      = getIntent().getStringExtra(EXTRA_NAME);
        String shopName  = getIntent().getStringExtra(EXTRA_SHOP_NAME);
        String shopEmoji = getIntent().getStringExtra(EXTRA_SHOP_EMOJI);
        String price     = getIntent().getStringExtra(EXTRA_PRICE);
        String origPrice = getIntent().getStringExtra(EXTRA_ORIGINAL_PRICE);
        String distance  = getIntent().getStringExtra(EXTRA_DISTANCE);
        String category  = getIntent().getStringExtra(EXTRA_CATEGORY);

        tvProductEmoji.setText(emoji   != null ? emoji    : "🍎");
        tvProductName.setText(name     != null ? name     : "Fresh Red Apples");
        tvShopName.setText(shopName    != null ? shopName : "FreshMart Grocery");
        tvShopEmoji.setText(shopEmoji  != null ? shopEmoji: "🏪");
        tvSalePrice.setText(price      != null ? price    : "Rs.120");
        tvDistance.setText("📍 " + (distance != null ? distance : "0.8 km") + " away");
        tvCategory.setText(category    != null ? category : "Fruits");
        tvCategoryDetail.setText(category != null ? category : "Fruits");

        // Strike-through original price
        if (tvOriginalPrice != null) {
            tvOriginalPrice.setText(origPrice != null ? origPrice : "Rs.200");
            tvOriginalPrice.setPaintFlags(tvOriginalPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        }
    }

    private void setupButtons() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        btnViewStore.setOnClickListener(v -> {
            Intent intent = new Intent(this, StoreDetailsActivity.class);
            intent.putExtra(StoreDetailsActivity.EXTRA_SHOP_NAME,
                    getIntent().getStringExtra(EXTRA_SHOP_NAME));
            intent.putExtra(StoreDetailsActivity.EXTRA_SHOP_EMOJI,
                    getIntent().getStringExtra(EXTRA_SHOP_EMOJI));
            startActivity(intent);
        });

        // Delivery / Pick Up toggle
        btnDelivery.setOnClickListener(v -> {
            isDeliverySelected = true;
            btnDelivery.setBackgroundResource(R.drawable.bg_distance_chip_active);
            btnDelivery.setTextColor(ContextCompat.getColor(this, R.color.white));
            btnPickUp.setBackgroundResource(R.drawable.bg_distance_chip);
            btnPickUp.setTextColor(ContextCompat.getColor(this, R.color.nb_primary));
        });

        btnPickUp.setOnClickListener(v -> {
            isDeliverySelected = false;
            btnPickUp.setBackgroundResource(R.drawable.bg_distance_chip_active);
            btnPickUp.setTextColor(ContextCompat.getColor(this, R.color.white));
            btnDelivery.setBackgroundResource(R.drawable.bg_distance_chip);
            btnDelivery.setTextColor(ContextCompat.getColor(this, R.color.nb_primary));
        });

        btnPlaceOrder.setOnClickListener(v -> {
            String type = isDeliverySelected ? "Delivery" : "Pick Up";
            Toast.makeText(this,
                    "Order placed! (" + type + " – Cash on Delivery) ✅",
                    Toast.LENGTH_LONG).show();
        });
    }
}

