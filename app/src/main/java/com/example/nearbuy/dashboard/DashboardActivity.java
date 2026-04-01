package com.example.nearbuy.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nearbuy.R;
import com.example.nearbuy.app.startup.WelcomeActivity;
import com.example.nearbuy.core.SessionManager;
import com.example.nearbuy.data.model.Customer;
import com.example.nearbuy.data.model.DealItem;
import com.example.nearbuy.data.model.Product;
import com.example.nearbuy.data.model.Shop;
import com.example.nearbuy.data.repository.DataCallback;
import com.example.nearbuy.data.repository.DealRepository;
import com.example.nearbuy.data.repository.OrderRepository;
import com.example.nearbuy.data.repository.ProductRepository;
import com.example.nearbuy.data.repository.ShopRepository;
import com.example.nearbuy.discounts.DealsAndPromoActivity;
import com.example.nearbuy.notifications.NotificationsActivity;
import com.example.nearbuy.orders.OrdersActivity;
import com.example.nearbuy.product.ProductDetailsActivity;
import com.example.nearbuy.profile.ProfileActivity;
import com.example.nearbuy.search.SearchActivity;
import com.example.nearbuy.store.StoreDetailsActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * DashboardActivity – the main home screen shown after a successful login.
 *
 * Responsibilities:
 *   • Display the customer's name and avatar initial from SessionManager.
 *   • Show lifetime stats (orders, spent, saved) loaded from OrderRepository.
 *   • Display latest deals in a horizontal RecyclerView (replaces hardcoded cards).
 *   • Display active promotions in a vertical RecyclerView (replaces hardcoded cards).
 *   • Display nearby shops in a vertical RecyclerView (replaces hardcoded cards).
 *
 * Performance fix:
 *   The original layout baked 15 static deal cards, 5 promo cards, and 5 shop cards
 *   directly into the XML (100+ views, 7-level nesting).  This caused the JIT compiler
 *   to allocate 5 MB just to compile ViewRootImpl.performTraversals().
 *   RecyclerView only inflates items visible on screen, cutting the live view count
 *   from 100+ to ~20 and the nesting depth from 7 to 4.
 */
public class DashboardActivity extends AppCompatActivity {

    private static final String TAG = "NearBuy.Dashboard";

    // ── Bottom navigation ──────────────────────────────────────────────────────
    private LinearLayout navHome, navSearch, navDeals, navProfile;
    private ImageView    navHomeIcon, navSearchIcon, navDealsIcon, navProfileIcon;
    private TextView     navHomeText, navSearchText, navDealsText, navProfileText;

    // ── Header / Welcome banner ────────────────────────────────────────────────
    private TextView tvUserName, tvAvatarInitial;

    // ── Stats card ─────────────────────────────────────────────────────────────
    private TextView tvOrderCount, tvTotalSpent, tvTotalSaved;

    // ── Section links ──────────────────────────────────────────────────────────
    private TextView tvViewAllDeals, tvViewAllPromos, tvViewAllProducts;

    // ── Notification bell ──────────────────────────────────────────────────────
    private ImageView btnNotifications;

    // ── RecyclerViews (replace all hardcoded static card lists) ───────────────
    private RecyclerView rvDeals, rvPromos, rvShops, rvProducts;

    // ── Adapters ───────────────────────────────────────────────────────────────
    private DashboardDealAdapter    dealAdapter;
    private DashboardPromoAdapter   promoAdapter;
    private DashboardShopAdapter    shopAdapter;
    private DashboardProductAdapter productAdapter;

    // ── Data lists (populated from Firestore) ──────────────────────────────────
    private final List<DealItem> dealList    = new ArrayList<>();
    private final List<DealItem> promoList   = new ArrayList<>();
    private final List<Shop>     shopList    = new ArrayList<>();
    private final List<Product>  productList = new ArrayList<>();

    // ── Repository dependencies ───────────────────────────────────────────────
    private ShopRepository    shopRepository;
    private OrderRepository   orderRepository;
    private DealRepository    dealRepository;
    private ProductRepository productRepository;

    // ── Session ────────────────────────────────────────────────────────────────
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.nb_primary_dark));
        window.setNavigationBarColor(ContextCompat.getColor(this, R.color.bg_white));

        setContentView(R.layout.activity_dashboard);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        sessionManager = SessionManager.getInstance(this);

        if (!sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, WelcomeActivity.class));
            finish();
            return;
        }

        shopRepository  = new ShopRepository();
        orderRepository = new OrderRepository();
        dealRepository  = new DealRepository();
        productRepository = new ProductRepository();

        initViews();
        setupRecyclerViews();
        populateFromSession();
        setupNavigation();
        loadCustomerStats();
        loadNearbyShops();
        loadDeals();
        loadPromos();
        loadProducts();
    }

    // ── View binding ───────────────────────────────────────────────────────────

    private void initViews() {
        navHome    = findViewById(R.id.navHome);
        navSearch  = findViewById(R.id.navSearch);
        navDeals   = findViewById(R.id.navDeals);
        navProfile = findViewById(R.id.navProfile);

        navHomeIcon    = findViewById(R.id.navHomeIcon);
        navSearchIcon  = findViewById(R.id.navSearchIcon);
        navDealsIcon   = findViewById(R.id.navDealsIcon);
        navProfileIcon = findViewById(R.id.navProfileIcon);

        navHomeText    = findViewById(R.id.navHomeText);
        navSearchText  = findViewById(R.id.navSearchText);
        navDealsText   = findViewById(R.id.navDealsText);
        navProfileText = findViewById(R.id.navProfileText);

        tvUserName      = findViewById(R.id.tvUserName);
        tvAvatarInitial = findViewById(R.id.tvAvatarInitial);

        tvOrderCount = findViewById(R.id.tvOrderCount);
        tvTotalSpent = findViewById(R.id.tvTotalSpent);
        tvTotalSaved = findViewById(R.id.tvTotalSaved);

        tvViewAllDeals  = findViewById(R.id.tvViewAllDeals);
        tvViewAllPromos = findViewById(R.id.tvViewAllPromos);
        tvViewAllProducts = findViewById(R.id.tvViewAllProducts);

        btnNotifications = findViewById(R.id.btnNotifications);

        rvDeals  = findViewById(R.id.rvDeals);
        rvPromos = findViewById(R.id.rvPromos);
        rvShops  = findViewById(R.id.rvShops);
        rvProducts = findViewById(R.id.rvProducts);
    }

    // ── RecyclerView setup ─────────────────────────────────────────────────────

    /**
     * Attaches adapters and LayoutManagers to the three RecyclerViews.
     * Called once in onCreate – data is pushed later via loadDeals/loadPromos/loadNearbyShops.
     *
     * Key performance setting: setHasFixedSize(true) skips a full re-layout when
     * only item content changes (text/colours), saving a measure pass per update.
     */
    private void setupRecyclerViews() {
        // Horizontal products strip (from NearBuyHQ shops)
        productAdapter = new DashboardProductAdapter(productList, product -> {
            Intent i = new Intent(this, ProductDetailsActivity.class);
            i.putExtra(ProductDetailsActivity.EXTRA_SHOP_ID,   product.getShopId());
            i.putExtra(ProductDetailsActivity.EXTRA_PRODUCT_ID, product.getId());
            i.putExtra(ProductDetailsActivity.EXTRA_NAME,       product.getName());
            i.putExtra(ProductDetailsActivity.EXTRA_SHOP_NAME,  product.getShopName());
            i.putExtra(ProductDetailsActivity.EXTRA_PRICE,      product.getPriceLabel());
            i.putExtra(ProductDetailsActivity.EXTRA_CATEGORY,   product.getCategory());
            i.putExtra(ProductDetailsActivity.EXTRA_DISTANCE,   product.getDistanceLabel());
            startActivity(i);
        });
        LinearLayoutManager productLM = new LinearLayoutManager(
                this, LinearLayoutManager.HORIZONTAL, false);
        rvProducts.setLayoutManager(productLM);
        rvProducts.setAdapter(productAdapter);
        rvProducts.setHasFixedSize(false);

        // Horizontal deals strip
        dealAdapter = new DashboardDealAdapter(dealList, deal -> {
            Intent i = new Intent(this, DealsAndPromoActivity.class);
            i.putExtra(DealsAndPromoActivity.EXTRA_TAB, "deals");
            startActivity(i);
        });
        LinearLayoutManager dealLM = new LinearLayoutManager(
                this, LinearLayoutManager.HORIZONTAL, false);
        rvDeals.setLayoutManager(dealLM);
        rvDeals.setAdapter(dealAdapter);
        rvDeals.setHasFixedSize(true);

        // Vertical promos list (nestedScrollingEnabled=false already set in XML)
        promoAdapter = new DashboardPromoAdapter(promoList, promo -> {
            Intent i = new Intent(this, DealsAndPromoActivity.class);
            i.putExtra(DealsAndPromoActivity.EXTRA_TAB, "promos");
            startActivity(i);
        });
        rvPromos.setLayoutManager(new LinearLayoutManager(this));
        rvPromos.setAdapter(promoAdapter);
        rvPromos.setHasFixedSize(false); // height changes as items load

        // Vertical shops list
        shopAdapter = new DashboardShopAdapter(shopList, shop -> {
            Intent intent = new Intent(this, StoreDetailsActivity.class);
            intent.putExtra(StoreDetailsActivity.EXTRA_SHOP_ID,   shop.getId());
            intent.putExtra(StoreDetailsActivity.EXTRA_SHOP_NAME, shop.getName());
            intent.putExtra(StoreDetailsActivity.EXTRA_DISTANCE,  shop.getDistanceLabel());
            intent.putExtra(StoreDetailsActivity.EXTRA_ADDRESS,   shop.getShopLocation());
            startActivity(intent);
        });
        rvShops.setLayoutManager(new LinearLayoutManager(this));
        rvShops.setAdapter(shopAdapter);
        rvShops.setHasFixedSize(false);
    }

    // ── Populate from session (instant – no network call needed) ──────────────

    private void populateFromSession() {
        String userName = sessionManager.getUserName();
        tvUserName.setText(userName);
        if (!userName.isEmpty()) {
            tvAvatarInitial.setText(
                    String.valueOf(userName.charAt(0)).toUpperCase());
        }
        tvOrderCount.setText("—");
        tvTotalSpent.setText("—");
        tvTotalSaved.setText("—");
    }

    // ── Navigation setup ───────────────────────────────────────────────────────

    private void setupNavigation() {
        navHomeIcon.setColorFilter(ContextCompat.getColor(this, R.color.nb_primary));
        navHomeText.setTextColor(ContextCompat.getColor(this, R.color.nb_primary));

        navSearch.setOnClickListener(v ->
                startActivity(new Intent(this, SearchActivity.class)));
        navDeals.setOnClickListener(v ->
                startActivity(new Intent(this, OrdersActivity.class)));
        navProfile.setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class)));
        btnNotifications.setOnClickListener(v ->
                startActivity(new Intent(this, NotificationsActivity.class)));

        tvViewAllDeals.setOnClickListener(v -> {
            Intent i = new Intent(this, DealsAndPromoActivity.class);
            i.putExtra(DealsAndPromoActivity.EXTRA_TAB, "deals");
            startActivity(i);
        });
        tvViewAllPromos.setOnClickListener(v -> {
            Intent i = new Intent(this, DealsAndPromoActivity.class);
            i.putExtra(DealsAndPromoActivity.EXTRA_TAB, "promos");
            startActivity(i);
        });
        if (tvViewAllProducts != null) {
            tvViewAllProducts.setOnClickListener(v ->
                    startActivity(new Intent(this, SearchActivity.class)));
        }
    }

    // ── Firebase data loading ──────────────────────────────────────────────────

    private void loadCustomerStats() {
        String uid = sessionManager.getUserId();
        if (uid.isEmpty()) return;

        orderRepository.getCustomerStats(uid, new DataCallback<Customer>() {
            @Override
            public void onSuccess(Customer customer) {
                tvOrderCount.setText(String.valueOf(customer.getTotalOrders()));
                tvTotalSpent.setText(customer.getTotalSpent() > 0
                        ? String.format("Rs.%.0f", customer.getTotalSpent()) : "Rs.0");
                tvTotalSaved.setText(customer.getTotalSaved() > 0
                        ? String.format("Rs.%.0f", customer.getTotalSaved()) : "Rs.0");
            }

            @Override
            public void onError(Exception e) {
                Log.w(TAG, "Could not load customer stats", e);
                tvOrderCount.setText("0");
                tvTotalSpent.setText("Rs.0");
                tvTotalSaved.setText("Rs.0");
            }
        });
    }

    /**
     * Loads nearby shops from Firestore and populates the shops RecyclerView.
     * Replaces the old updateShopCard / hideAllShopCards approach.
     */
    private void loadNearbyShops() {
        double lat = sessionManager.getLastLatitude();
        double lng = sessionManager.getLastLongitude();
        float radius = sessionManager.getSearchRadius();

        shopRepository.getNearbyShops(lat, lng, radius, new DataCallback<List<Shop>>() {
            @Override
            public void onSuccess(List<Shop> shops) {
                shopList.clear();
                shopList.addAll(shops);
                shopAdapter.notifyDataSetChanged();
            }

            @Override
            public void onError(Exception e) {
                Log.w(TAG, "Could not load nearby shops", e);
            }
        });
    }

    private void loadDeals() {
        double lat = sessionManager.getLastLatitude();
        double lng = sessionManager.getLastLongitude();
        float radius = sessionManager.getSearchRadius();

        dealRepository.getLatestDeals(lat, lng, radius, 10, new DataCallback<List<DealItem>>() {
            @Override
            public void onSuccess(List<DealItem> deals) {
                dealList.clear();
                dealList.addAll(deals);
                dealAdapter.notifyDataSetChanged();
            }

            @Override
            public void onError(Exception e) {
                Log.w(TAG, "Could not load deals for dashboard", e);
            }
        });
    }

    private void loadPromos() {
        double lat = sessionManager.getLastLatitude();
        double lng = sessionManager.getLastLongitude();
        float radius = sessionManager.getSearchRadius();

        dealRepository.getLatestPromotions(lat, lng, radius, 5, new DataCallback<List<DealItem>>() {
            @Override
            public void onSuccess(List<DealItem> promos) {
                promoList.clear();
                promoList.addAll(promos);
                promoAdapter.notifyDataSetChanged();
            }

            @Override
            public void onError(Exception e) {
                Log.w(TAG, "Could not load promotions for dashboard", e);
            }
        });
    }

    private void loadProducts() {
        double lat = sessionManager.getLastLatitude();
        double lng = sessionManager.getLastLongitude();

        float radius = sessionManager.getSearchRadius();

        productRepository.getFeaturedProducts(lat, lng, radius, 20,
                new DataCallback<List<Product>>() {
                    @Override
                    public void onSuccess(List<Product> products) {
                        productList.clear();
                        productList.addAll(products);
                        if (productAdapter != null) productAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.w(TAG, "Could not load products for dashboard", e);
                    }
                });
    }

    // ── Utility ────────────────────────────────────────────────────────────────

    @Override
    protected void onResume() {
        super.onResume();
        if (navHomeIcon != null)
            navHomeIcon.setColorFilter(ContextCompat.getColor(this, R.color.nb_primary));
        if (navHomeText != null)
            navHomeText.setTextColor(ContextCompat.getColor(this, R.color.nb_primary));
        if (navSearchIcon != null)
            navSearchIcon.setColorFilter(ContextCompat.getColor(this, R.color.text_dark_hint));
        if (navSearchText != null)
            navSearchText.setTextColor(ContextCompat.getColor(this, R.color.text_dark_hint));
        if (navDealsIcon != null)
            navDealsIcon.setColorFilter(ContextCompat.getColor(this, R.color.text_dark_hint));
        if (navDealsText != null)
            navDealsText.setTextColor(ContextCompat.getColor(this, R.color.text_dark_hint));
        if (navProfileIcon != null)
            navProfileIcon.setColorFilter(ContextCompat.getColor(this, R.color.text_dark_hint));
        if (navProfileText != null)
            navProfileText.setTextColor(ContextCompat.getColor(this, R.color.text_dark_hint));
    }
}
