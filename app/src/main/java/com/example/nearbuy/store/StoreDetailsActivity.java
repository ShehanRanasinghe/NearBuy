package com.example.nearbuy.store;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nearbuy.R;
import com.example.nearbuy.app.startup.WelcomeActivity;
import com.example.nearbuy.core.SessionManager;
import com.example.nearbuy.data.model.DealItem;
import com.example.nearbuy.data.model.Product;
import com.example.nearbuy.data.model.Shop;
import com.example.nearbuy.data.repository.DataCallback;
import com.example.nearbuy.data.repository.DealRepository;
import com.example.nearbuy.data.repository.ProductRepository;
import com.example.nearbuy.data.repository.ShopRepository;
import com.example.nearbuy.product.ProductDetailsActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * StoreDetailsActivity – shows full details of a NearBuyHQ shop including
 * its live products, deals, and promotions fetched from Firestore.
 *
 * Data path:
 *   Shop info   → NearBuyHQ/{shopId}
 *   Products    → NearBuyHQ/{shopId}/products
 *   Deals       → NearBuyHQ/{shopId}/deals
 *   Promotions  → NearBuyHQ/{shopId}/promotions
 */
public class StoreDetailsActivity extends AppCompatActivity {

    private static final String TAG = "NearBuy.StoreDetails";

    public static final String EXTRA_SHOP_ID    = "shopId";
    public static final String EXTRA_SHOP_NAME  = "shopName";
    public static final String EXTRA_SHOP_EMOJI = "shopEmoji";
    public static final String EXTRA_DISTANCE   = "distance";
    public static final String EXTRA_ADDRESS    = "address";

    // ── Repositories ──────────────────────────────────────────────────────────
    private ShopRepository    shopRepository;
    private ProductRepository productRepository;
    private DealRepository    dealRepository;

    // ── RecyclerViews ─────────────────────────────────────────────────────────
    private RecyclerView rvProducts, rvDeals, rvPromos;

    // ── Stat views ────────────────────────────────────────────────────────────
    private TextView tvActiveDeals, tvDistance;

    // ── Adapters ──────────────────────────────────────────────────────────────
    private StoreItemAdapter productsAdapter, dealsAdapter, promosAdapter;

    // ── Data lists ────────────────────────────────────────────────────────────
    private final List<StoreItemAdapter.RowItem> productRows = new ArrayList<>();
    private final List<StoreItemAdapter.RowItem> dealRows    = new ArrayList<>();
    private final List<StoreItemAdapter.RowItem> promoRows   = new ArrayList<>();

    // Product list (kept for click navigation)
    private final List<Product> productList = new ArrayList<>();

    // Current shopId for click navigation
    private String shopId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window w = getWindow();
        w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        w.setStatusBarColor(ContextCompat.getColor(this, R.color.nb_primary_dark));

        setContentView(R.layout.activity_store_details);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        SessionManager session = SessionManager.getInstance(this);
        if (!session.isLoggedIn()) {
            startActivity(new Intent(this, WelcomeActivity.class));
            finish();
            return;
        }

        shopRepository    = new ShopRepository();
        productRepository = new ProductRepository();
        dealRepository    = new DealRepository();

        shopId = getIntent().getStringExtra(EXTRA_SHOP_ID);

        initRecyclerViews();

        if (shopId != null && !shopId.isEmpty()) {
            loadShopFromFirestore(shopId);
            loadProducts(shopId);
            loadDeals(shopId);
            loadPromotions(shopId);
        } else {
            // Fallback: display the data provided in the intent
            String name     = getIntent().getStringExtra(EXTRA_SHOP_NAME);
            String address  = getIntent().getStringExtra(EXTRA_ADDRESS);
            String distance = getIntent().getStringExtra(EXTRA_DISTANCE);
            populateStoreInfo(name, distance, address, null, null);
        }

        setupButtons();
    }

    // ── RecyclerView init ─────────────────────────────────────────────────────

    private void initRecyclerViews() {
        rvProducts    = findViewById(R.id.rvStoreProducts);
        rvDeals       = findViewById(R.id.rvStoreDeals);
        rvPromos      = findViewById(R.id.rvStorePromos);
        tvActiveDeals = findViewById(R.id.tv_active_deals);
        tvDistance    = findViewById(R.id.tv_distance);

        if (rvProducts != null) {
            productsAdapter = new StoreItemAdapter(productRows, pos -> {
                if (pos >= 0 && pos < productList.size()) {
                    Product p = productList.get(pos);
                    Intent i  = new Intent(this, ProductDetailsActivity.class);
                    i.putExtra(ProductDetailsActivity.EXTRA_SHOP_ID,   p.getShopId());
                    i.putExtra(ProductDetailsActivity.EXTRA_PRODUCT_ID, p.getId());
                    i.putExtra(ProductDetailsActivity.EXTRA_NAME,       p.getName());
                    i.putExtra(ProductDetailsActivity.EXTRA_SHOP_NAME,
                            getIntent().getStringExtra(EXTRA_SHOP_NAME));
                    i.putExtra(ProductDetailsActivity.EXTRA_PRICE,      p.getPriceLabel());
                    i.putExtra(ProductDetailsActivity.EXTRA_CATEGORY,   p.getCategory());
                    i.putExtra(ProductDetailsActivity.EXTRA_DESCRIPTION, p.getDescription());
                    i.putExtra(ProductDetailsActivity.EXTRA_UNIT,       p.getUnit());
                    startActivity(i);
                }
            });
            rvProducts.setLayoutManager(new LinearLayoutManager(this));
            rvProducts.setAdapter(productsAdapter);
            rvProducts.setNestedScrollingEnabled(false);
        }

        if (rvDeals != null) {
            dealsAdapter = new StoreItemAdapter(dealRows, pos -> { /* navigate if needed */ });
            rvDeals.setLayoutManager(new LinearLayoutManager(this));
            rvDeals.setAdapter(dealsAdapter);
            rvDeals.setNestedScrollingEnabled(false);
        }

        if (rvPromos != null) {
            promosAdapter = new StoreItemAdapter(promoRows, pos -> { /* navigate if needed */ });
            rvPromos.setLayoutManager(new LinearLayoutManager(this));
            rvPromos.setAdapter(promosAdapter);
            rvPromos.setNestedScrollingEnabled(false);
        }
    }

    // ── Firestore load ────────────────────────────────────────────────────────

    private void loadShopFromFirestore(String sId) {
        // Read the distance that was calculated client-side before opening this screen.
        // The Shop object fetched from Firestore will have distanceKm == -1 (never stored),
        // so we must use the value passed via EXTRA_DISTANCE instead.
        final String intentDistance = getIntent().getStringExtra(EXTRA_DISTANCE);

        shopRepository.getShopById(sId, new DataCallback<Shop>() {
            @Override
            public void onSuccess(Shop shop) {
                // If no distance was passed via intent, calculate it from the
                // shop's GPS coordinates and the user's last known location.
                String dist = (intentDistance != null && !intentDistance.isEmpty())
                        ? intentDistance
                        : computeDistance(shop.getLatitude(), shop.getLongitude());
                populateStoreInfo(shop.getName(), dist,
                        shop.getShopLocation(), shop.getPhone(), shop.getOpeningHours());
            }
            @Override
            public void onError(Exception e) {
                Log.w(TAG, "Could not load shop: " + sId, e);
                String name    = getIntent().getStringExtra(EXTRA_SHOP_NAME);
                String address = getIntent().getStringExtra(EXTRA_ADDRESS);
                populateStoreInfo(name, intentDistance, address, null, null);
            }
        });
    }

    /**
     * Calculates the distance between the shop's GPS coordinates and the
     * customer's last saved location. Returns a formatted label like "1.3 km"
     * or "350 m", or null if either set of coordinates is unavailable.
     */
    private String computeDistance(double shopLat, double shopLng) {
        if (shopLat == 0.0 && shopLng == 0.0) return null;
        SessionManager session = SessionManager.getInstance(this);
        double userLat = session.getLastLatitude();
        double userLng = session.getLastLongitude();
        if (userLat == 0.0 && userLng == 0.0) return null;

        float[] results = new float[1];
        Location.distanceBetween(userLat, userLng, shopLat, shopLng, results);
        double km = results[0] / 1000.0;
        if (km < 1.0) return String.format(Locale.ROOT, "%.0f m", km * 1000);
        return String.format(Locale.ROOT, "%.1f km", km);
    }

    private void loadProducts(String sId) {
        productRepository.getProductsByShop(sId, new DataCallback<List<Product>>() {
            @Override
            public void onSuccess(List<Product> products) {
                productList.clear();
                productList.addAll(products);
                productRows.clear();
                int green = ContextCompat.getColor(StoreDetailsActivity.this, R.color.stat_green);
                int red   = ContextCompat.getColor(StoreDetailsActivity.this, R.color.stat_red);
                for (Product p : products) {
                    String subtitle = (p.getCategory() != null ? p.getCategory() : "")
                            + "  •  " + p.getPriceLabel();
                    String badge    = p.isAvailable() ? "In Stock" : "Out of Stock";
                    int    badgeClr = p.isAvailable() ? green : red;
                    productRows.add(new StoreItemAdapter.RowItem(
                            emojiForCategory(p.getCategory()),
                            p.getName(),
                            subtitle,
                            badge,
                            badgeClr));
                }
                if (productsAdapter != null) productsAdapter.notifyDataSetChanged();
                // Update the products count chip with the real count
                if (tvActiveDeals != null) {
                    tvActiveDeals.setText(String.valueOf(products.size()));
                }
            }
            @Override
            public void onError(Exception e) {
                Log.w(TAG, "Could not load products for shop: " + sId, e);
                if (tvActiveDeals != null) tvActiveDeals.setText("0");
            }
        });
    }

    private void loadDeals(String sId) {
        dealRepository.getDealsByShop(sId, new DataCallback<List<DealItem>>() {
            @Override
            public void onSuccess(List<DealItem> deals) {
                dealRows.clear();
                int white = 0xFFFFFFFF;
                for (DealItem d : deals) {
                    String label    = d.getDiscountLabel() != null && !d.getDiscountLabel().isEmpty()
                            ? d.getDiscountLabel() : "DEAL";
                    String subtitle = (d.getSalePrice() > 0
                            ? String.format("Rs.%.0f", d.getSalePrice()) : "")
                            + (d.getExpiresAt() > 0 ? "  •  " + d.getExpiryLabel() : "");
                    dealRows.add(new StoreItemAdapter.RowItem("🏷️",
                            d.getTitle(), subtitle, label, white));
                }
                if (dealsAdapter != null) dealsAdapter.notifyDataSetChanged();
            }
            @Override
            public void onError(Exception e) {
                Log.w(TAG, "Could not load deals for shop: " + sId, e);
            }
        });
    }

    private void loadPromotions(String sId) {
        dealRepository.getPromotionsByShop(sId, new DataCallback<List<DealItem>>() {
            @Override
            public void onSuccess(List<DealItem> promos) {
                promoRows.clear();
                for (DealItem p : promos) {
                    String subtitle = p.getDescription() != null ? p.getDescription() : "";
                    if (p.getExpiresAt() > 0) subtitle += "  •  " + p.getExpiryLabel();
                    promoRows.add(new StoreItemAdapter.RowItem("🎁",
                            p.getTitle(), subtitle, "", 0));
                }
                if (promosAdapter != null) promosAdapter.notifyDataSetChanged();
            }
            @Override
            public void onError(Exception e) {
                Log.w(TAG, "Could not load promotions for shop: " + sId, e);
            }
        });
    }

    // ── UI population ─────────────────────────────────────────────────────────

    private void populateStoreInfo(String shopName, String distance,
                                   String address, String phone, String hours) {
        if (shopName == null || shopName.isEmpty()) shopName = "Nearby Shop";
        if (address  == null || address.isEmpty())  address  = "Address not available";
        if (phone    == null || phone.isEmpty())     phone    = "Not available";
        if (hours    == null || hours.isEmpty())     hours    = "Contact shop for hours";

        setText(R.id.tv_store_name, shopName);
        setText(R.id.tv_location,   address);
        setText(R.id.tv_contact,    phone);
        setText(R.id.tv_hours,      hours);

        // Update the distance chip with real data (fallback to "N/A")
        if (tvDistance != null) {
            tvDistance.setText((distance != null && !distance.isEmpty()) ? distance : "N/A");
        }
    }

    // ── Buttons ───────────────────────────────────────────────────────────────

    private void setupButtons() {
        safeClick(R.id.btn_back, v -> finish());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String emojiForCategory(String cat) {
        if (cat == null || cat.isEmpty()) return "🛒";
        switch (cat.toLowerCase()) {
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
