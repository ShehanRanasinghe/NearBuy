package com.example.nearbuy.product;

import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.nearbuy.R;
import com.example.nearbuy.app.startup.WelcomeActivity;
import com.example.nearbuy.core.SessionManager;
import com.example.nearbuy.data.model.Product;
import com.example.nearbuy.data.repository.DataCallback;
import com.example.nearbuy.data.repository.OperationCallback;
import com.example.nearbuy.data.repository.OrderRepository;
import com.example.nearbuy.data.repository.ProductRepository;
import com.example.nearbuy.orders.OrderItem;
import com.example.nearbuy.store.StoreDetailsActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * ProductDetailsActivity – Shows full details of a product from a NearBuyHQ shop.
 *
 * Can receive data two ways:
 *   1. EXTRA_SHOP_ID + EXTRA_PRODUCT_ID → loads full product from Firestore.
 *   2. Plain string extras (name, price, etc.) → displays immediately (search/deal flow).
 *
 * "Place Order" saves a real order document to NearBuy/{customerId}/orders.
 */
public class ProductDetailsActivity extends AppCompatActivity {

    // Intent extra keys
    public static final String EXTRA_SHOP_ID        = "shopId";
    public static final String EXTRA_PRODUCT_ID     = "productId";
    public static final String EXTRA_EMOJI          = "emoji";
    public static final String EXTRA_NAME           = "name";
    public static final String EXTRA_SHOP_NAME      = "shopName";
    public static final String EXTRA_SHOP_EMOJI     = "shopEmoji";
    public static final String EXTRA_PRICE          = "price";
    public static final String EXTRA_ORIGINAL_PRICE = "originalPrice";
    public static final String EXTRA_DISTANCE       = "distance";
    public static final String EXTRA_CATEGORY       = "category";
    public static final String EXTRA_DISCOUNT       = "discount";
    public static final String EXTRA_DESCRIPTION    = "description";
    public static final String EXTRA_UNIT           = "unit";
    public static final String EXTRA_SHOP_LOCATION  = "shopLocation";

    private TextView tvProductEmoji, tvProductName, tvCategory, tvInStock;
    private TextView tvSalePrice, tvOriginalPrice;
    private TextView tvShopEmoji, tvShopName, tvDistance, tvRating;
    private TextView tvDescription, tvWeight, tvCategoryDetail, tvDealExpiry;
    private TextView btnViewStore;
    private TextView btnDelivery, btnPickUp;
    private Button   btnPlaceOrder;
    private ProgressBar progressBar;

    private boolean isDeliverySelected = true;

    // Loaded product (populated from Firestore when shopId+productId are provided)
    private Product loadedProduct;

    // Repositories & session
    private ProductRepository productRepository;
    private OrderRepository   orderRepository;
    private SessionManager    sessionManager;

    // Retained intent data for fallback display
    private String shopId, productId, shopName, shopLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window w = getWindow();
        w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        w.setStatusBarColor(ContextCompat.getColor(this, R.color.nb_primary_dark));

        setContentView(R.layout.activity_product_details);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        sessionManager    = SessionManager.getInstance(this);
        if (!sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, WelcomeActivity.class));
            finish();
            return;
        }

        productRepository = new ProductRepository();
        orderRepository   = new OrderRepository();

        shopId        = getIntent().getStringExtra(EXTRA_SHOP_ID);
        productId     = getIntent().getStringExtra(EXTRA_PRODUCT_ID);
        shopName      = getIntent().getStringExtra(EXTRA_SHOP_NAME);
        shopLocation  = getIntent().getStringExtra(EXTRA_SHOP_LOCATION);

        initViews();
        setupButtons();

        // If we have both shopId and productId, load real data from Firestore
        if (shopId != null && !shopId.isEmpty()
                && productId != null && !productId.isEmpty()) {
            loadProductFromFirestore(shopId, productId);
        } else {
            // Fallback: display data from intent extras
            populateFromExtras();
        }
    }

    // ── View binding ─────────────────────────────────────────────────────────

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
        // ProgressBar may not exist in the current layout – use null-safe access
        progressBar      = findViewById(R.id.progressBar);
    }

    // ── Firestore load ────────────────────────────────────────────────────────

    private void loadProductFromFirestore(String sId, String pId) {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        if (btnPlaceOrder != null) btnPlaceOrder.setEnabled(false);

        productRepository.getProductById(sId, pId, new DataCallback<Product>() {
            @Override
            public void onSuccess(Product product) {
                loadedProduct = product;
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                if (btnPlaceOrder != null) btnPlaceOrder.setEnabled(true);
                populateFromProduct(product);
            }

            @Override
            public void onError(Exception e) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                if (btnPlaceOrder != null) btnPlaceOrder.setEnabled(true);
                // Fall back to intent extras on error
                populateFromExtras();
            }
        });
    }

    // ── UI population ─────────────────────────────────────────────────────────

    /** Populate UI from a fully loaded Firestore Product object. */
    private void populateFromProduct(Product p) {
        safe(tvProductEmoji,   emojiFor(p.getCategory()));
        safe(tvProductName,    p.getName());
        safe(tvCategory,       p.getCategory());
        safe(tvInStock,        p.isAvailable() ? "In Stock" : "Out of Stock");
        safe(tvSalePrice,      p.getPriceLabel());
        safe(tvShopName,       shopName != null ? shopName : "");
        safe(tvShopEmoji,      "🏪");
        safe(tvDistance,       p.hasDistance()
                ? "📍 " + p.getDistanceLabel() + " away" : "📍 Nearby");
        safe(tvDescription,    p.getDescription());
        safe(tvWeight,         p.getUnit() != null && !p.getUnit().isEmpty()
                ? p.getUnit() : "—");
        safe(tvCategoryDetail, p.getCategory());
        safe(tvDealExpiry,     "—");

        if (tvOriginalPrice != null && p.hasDiscount()) {
            tvOriginalPrice.setText(String.format("Rs. %.0f", p.getOriginalPrice()));
            tvOriginalPrice.setPaintFlags(
                    tvOriginalPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            tvOriginalPrice.setVisibility(View.VISIBLE);
        }

        if (tvInStock != null) {
            tvInStock.setTextColor(ContextCompat.getColor(this,
                    p.isAvailable() ? R.color.stat_green : R.color.stat_red));
        }
    }

    /** Fallback: populate from raw intent extras (search / deal card flow). */
    private void populateFromExtras() {
        String emoji    = getIntent().getStringExtra(EXTRA_EMOJI);
        String name     = getIntent().getStringExtra(EXTRA_NAME);
        String sName    = getIntent().getStringExtra(EXTRA_SHOP_NAME);
        String sEmoji   = getIntent().getStringExtra(EXTRA_SHOP_EMOJI);
        String price    = getIntent().getStringExtra(EXTRA_PRICE);
        String origP    = getIntent().getStringExtra(EXTRA_ORIGINAL_PRICE);
        String distance = getIntent().getStringExtra(EXTRA_DISTANCE);
        String category = getIntent().getStringExtra(EXTRA_CATEGORY);
        String desc     = getIntent().getStringExtra(EXTRA_DESCRIPTION);
        String unit     = getIntent().getStringExtra(EXTRA_UNIT);

        safe(tvProductEmoji,   emoji   != null ? emoji    : "🛍️");
        safe(tvProductName,    name    != null ? name     : "");
        safe(tvShopName,       sName   != null ? sName    : "");
        safe(tvShopEmoji,      sEmoji  != null ? sEmoji   : "🏪");
        safe(tvSalePrice,      price   != null ? price    : "");
        safe(tvCategory,       category != null ? category : "");
        safe(tvCategoryDetail, category != null ? category : "");
        safe(tvDescription,    desc    != null ? desc     : "");
        safe(tvWeight,         unit    != null ? unit     : "—");
        safe(tvDealExpiry,     "—");

        if (distance != null) safe(tvDistance, "📍 " + distance + " away");

        if (tvOriginalPrice != null && origP != null && !origP.isEmpty()) {
            tvOriginalPrice.setText(origP);
            tvOriginalPrice.setPaintFlags(
                    tvOriginalPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        }
    }

    // ── Button setup ─────────────────────────────────────────────────────────

    private void setupButtons() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        if (btnViewStore != null) {
            btnViewStore.setOnClickListener(v -> {
                Intent intent = new Intent(this, StoreDetailsActivity.class);
                if (shopId != null && !shopId.isEmpty()) {
                    intent.putExtra(StoreDetailsActivity.EXTRA_SHOP_ID, shopId);
                }
                intent.putExtra(StoreDetailsActivity.EXTRA_SHOP_NAME,
                        shopName != null ? shopName
                                : getIntent().getStringExtra(EXTRA_SHOP_NAME));
                startActivity(intent);
            });
        }

        // Delivery / Pick Up toggle
        if (btnDelivery != null) {
            btnDelivery.setOnClickListener(v -> {
                isDeliverySelected = true;
                btnDelivery.setBackgroundResource(R.drawable.bg_distance_chip_active);
                btnDelivery.setTextColor(ContextCompat.getColor(this, R.color.white));
                if (btnPickUp != null) {
                    btnPickUp.setBackgroundResource(R.drawable.bg_distance_chip);
                    btnPickUp.setTextColor(ContextCompat.getColor(this, R.color.nb_primary));
                }
            });
        }
        if (btnPickUp != null) {
            btnPickUp.setOnClickListener(v -> {
                isDeliverySelected = false;
                btnPickUp.setBackgroundResource(R.drawable.bg_distance_chip_active);
                btnPickUp.setTextColor(ContextCompat.getColor(this, R.color.white));
                if (btnDelivery != null) {
                    btnDelivery.setBackgroundResource(R.drawable.bg_distance_chip);
                    btnDelivery.setTextColor(ContextCompat.getColor(this, R.color.nb_primary));
                }
            });
        }

        if (btnPlaceOrder != null) {
            btnPlaceOrder.setOnClickListener(v -> placeOrder());
        }
    }

    // ── Order placement ───────────────────────────────────────────────────────

    /**
     * Builds an OrderItem from the displayed product and saves it to:
     *   NearBuyHQ/{shopId}/orders/{orderId}   – visible to admin / shop owner
     *   NearBuy/{customerId}/orders/{orderId} – visible to the customer
     *
     * Customer stats (totalOrders, totalSpent) are NOT updated here;
     * they are managed by the NearBuyHQ admin app.
     */
    private void placeOrder() {
        String customerId   = sessionManager.getUserId();
        String customerName = sessionManager.getUserName();
        if (customerId == null || customerId.isEmpty()) {
            Toast.makeText(this, "Please log in to place an order.", Toast.LENGTH_SHORT).show();
            return;
        }

        // ── Resolve product details (loaded product takes priority over extras) ──
        String pName  = loadedProduct != null ? loadedProduct.getName()
                : getIntent().getStringExtra(EXTRA_NAME);
        String sName  = loadedProduct != null
                ? (loadedProduct.getShopName() != null ? loadedProduct.getShopName() : shopName)
                : (shopName != null ? shopName : getIntent().getStringExtra(EXTRA_SHOP_NAME));
        // shopId comes from the product so the order lands in the correct shop collection
        String sId    = loadedProduct != null ? loadedProduct.getShopId()
                : (shopId != null ? shopId : "");
        double price  = loadedProduct != null ? loadedProduct.getPrice() : 0.0;
        if (price == 0.0) {
            String priceStr = getIntent().getStringExtra(EXTRA_PRICE);
            if (priceStr != null) {
                try { price = Double.parseDouble(priceStr.replaceAll("[^\\d.]", "")); }
                catch (NumberFormatException ignore) { }
            }
        }

        String fulfillment = isDeliverySelected ? "Delivery" : "Pick Up";
        String dateStr     = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                .format(new Date());
        String summary     = (pName != null ? pName : "Item") + " x1";
        String totalStr    = price > 0 ? String.format("Rs.%.0f", price) : "Rs.0";

        // Build the order
        OrderItem order = new OrderItem(
                "",                          // orderId – auto-generated by Firestore
                sName != null ? sName : "",
                "🏪",
                dateStr,
                summary,
                totalStr,
                "Processing",
                1
        );
        order.setShopId(sId);
        order.setTotalAmountRaw(price);
        order.setCustomerId(customerId);
        order.setCustomerName(customerName != null ? customerName : "");
        order.setFulfillmentType(fulfillment);

        if (btnPlaceOrder != null) btnPlaceOrder.setEnabled(false);

        orderRepository.placeOrder(customerId, order, new OperationCallback() {
            @Override
            public void onSuccess() {
                if (btnPlaceOrder != null) btnPlaceOrder.setEnabled(true);
                Toast.makeText(ProductDetailsActivity.this,
                        "✅ Order placed! (" + fulfillment + " – Cash on Delivery)",
                        Toast.LENGTH_LONG).show();
                finish();
            }

            @Override
            public void onError(Exception e) {
                if (btnPlaceOrder != null) btnPlaceOrder.setEnabled(true);
                Toast.makeText(ProductDetailsActivity.this,
                        "Failed to place order. Please try again.",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void safe(TextView tv, String text) {
        if (tv != null && text != null) tv.setText(text);
    }

    private String emojiFor(String category) {
        if (category == null) return "🛒";
        switch (category.toLowerCase()) {
            case "fruits":     return "🍎";
            case "vegetables": return "🥦";
            case "dairy":      return "🥛";
            case "bakery":     return "🍞";
            case "meat":       return "🥩";
            case "seafood":    return "🐟";
            case "beverages":  return "🥤";
            case "snacks":     return "🍪";
            default:           return "🛒";
        }
    }
}
