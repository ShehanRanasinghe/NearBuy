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
    private TextView tvSalePrice, tvOriginalPrice, tvSavings, tvDiscountBadge;
    private TextView tvShopEmoji, tvShopName, tvDistance, tvRating;
    private TextView tvDescription, tvWeight, tvCategoryDetail, tvDealExpiry;
    private TextView btnViewStore, btnSaveDeal;
    private Button   btnAddToCart;

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
        tvSavings        = findViewById(R.id.tvSavings);
        tvDiscountBadge  = findViewById(R.id.tvDiscountBadge);
        tvShopEmoji      = findViewById(R.id.tvShopEmoji);
        tvShopName       = findViewById(R.id.tvShopName);
        tvDistance       = findViewById(R.id.tvDistance);
        tvRating         = findViewById(R.id.tvRating);
        tvDescription    = findViewById(R.id.tvDescription);
        tvWeight         = findViewById(R.id.tvWeight);
        tvCategoryDetail = findViewById(R.id.tvCategoryDetail);
        tvDealExpiry     = findViewById(R.id.tvDealExpiry);
        btnViewStore     = findViewById(R.id.btnViewStore);
        btnSaveDeal      = findViewById(R.id.btnSaveDeal);
        btnAddToCart     = findViewById(R.id.btnAddToCart);
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
        String discount  = getIntent().getStringExtra(EXTRA_DISCOUNT);

        tvProductEmoji.setText(emoji   != null ? emoji    : "🍎");
        tvProductName.setText(name     != null ? name     : "Fresh Red Apples");
        tvShopName.setText(shopName    != null ? shopName : "FreshMart Grocery");
        tvShopEmoji.setText(shopEmoji  != null ? shopEmoji: "🏪");
        tvSalePrice.setText(price      != null ? price    : "Rs.120");
        tvDistance.setText("📍 " + (distance != null ? distance : "0.8 km") + " away");
        tvCategory.setText(category    != null ? category : "Fruits");
        tvCategoryDetail.setText(category != null ? category : "Fruits");
        tvDiscountBadge.setText(discount != null ? discount : "40% OFF");

        // Strike-through original price
        tvOriginalPrice.setText(origPrice != null ? origPrice : "Rs.200");
        tvOriginalPrice.setPaintFlags(tvOriginalPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

        // Compute savings if possible
        if (price != null && origPrice != null) {
            try {
                int sale = Integer.parseInt(price.replaceAll("[^0-9]", ""));
                int orig = Integer.parseInt(origPrice.replaceAll("[^0-9]", ""));
                tvSavings.setText("Save Rs." + (orig - sale));
            } catch (NumberFormatException ignored) {
                tvSavings.setText("Special Offer");
            }
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

        btnSaveDeal.setOnClickListener(v ->
                Toast.makeText(this, "Deal saved! 🔖", Toast.LENGTH_SHORT).show());

        btnAddToCart.setOnClickListener(v ->
                Toast.makeText(this,
                        tvProductName.getText() + " added to cart 🛒",
                        Toast.LENGTH_SHORT).show());

        findViewById(R.id.btnCart).setOnClickListener(v ->
                Toast.makeText(this, "Cart (coming soon)", Toast.LENGTH_SHORT).show());
    }
}

