package com.example.nearbuy.discounts;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nearbuy.R;
import com.example.nearbuy.app.startup.WelcomeActivity;
import com.example.nearbuy.core.SessionManager;
import com.example.nearbuy.dashboard.DashboardActivity;
import com.example.nearbuy.data.model.Customer;
import com.example.nearbuy.data.repository.DataCallback;
import com.example.nearbuy.data.repository.OrderRepository;
import com.example.nearbuy.orders.OrderItem;
import com.example.nearbuy.orders.OrdersAdapter;
import com.example.nearbuy.profile.ProfileActivity;
import com.example.nearbuy.search.SearchActivity;
import com.example.nearbuy.store.StoreDetailsActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * DealsActivity – the Orders tab in the bottom navigation bar.
 *
 * Shows the customer's order history (RecyclerView) with filter tabs.
 * Lifetime stats (Total Orders / Total Spent / Delivered) are loaded from
 * the customer's Firestore profile – maintained by the NearBuyHQ admin app.
 * This app NEVER writes to those counters.
 */
public class DealsActivity extends AppCompatActivity {

    private static final String TAG = "NearBuy.Deals";

    // ── UI references ──────────────────────────────────────────────────────────
    private RecyclerView  recyclerOrders;
    private TextView      tvResultCount;
    private TextView      tvTotalOrdersStat, tvTotalSpentStat, tvDeliveredStat, tvAvgOrderStat;
    private TextView      tabAll, tabDelivered, tabProcessing, tabCancelled;
    private LinearLayout  navHome, navSearch, navDeals, navProfile;
    private ImageView     navHomeIcon, navSearchIcon, navDealsIcon, navProfileIcon;
    private TextView      navHomeText, navSearchText, navDealsText, navProfileText;
    private ImageView     btnBack;
    private View          layoutEmpty;

    // ── Data ───────────────────────────────────────────────────────────────────
    private OrdersAdapter   adapter;
    private List<OrderItem> allOrders      = new ArrayList<>();
    private List<OrderItem> filteredOrders = new ArrayList<>();
    private String          activeFilter   = "All";

    // ── Dependencies ───────────────────────────────────────────────────────────
    private OrderRepository orderRepository;
    private SessionManager  sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window w = getWindow();
        w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        w.setStatusBarColor(ContextCompat.getColor(this, R.color.nb_primary_dark));
        setContentView(R.layout.activity_deals);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        orderRepository = new OrderRepository();
        sessionManager  = SessionManager.getInstance(this);

        if (!sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, WelcomeActivity.class));
            finish();
            return;
        }

        initViews();
        setupTabs();
        setupNavigation();

        loadOrders();
        loadStats();
    }

    // ── View binding ───────────────────────────────────────────────────────────

    private void initViews() {
        recyclerOrders     = findViewById(R.id.recyclerOrders);
        tvResultCount      = findViewById(R.id.tvResultCount);
        tvTotalOrdersStat  = findViewById(R.id.tvTotalOrdersStat);
        tvTotalSpentStat   = findViewById(R.id.tvTotalSpentStat);
        tvDeliveredStat    = findViewById(R.id.tvDeliveredStat);
        tvAvgOrderStat     = findViewById(R.id.tvAvgOrderStat);
        tabAll             = findViewById(R.id.tabAll);
        tabDelivered       = findViewById(R.id.tabDelivered);
        tabProcessing      = findViewById(R.id.tabProcessing);
        tabCancelled       = findViewById(R.id.tabCancelled);
        navHome            = findViewById(R.id.navHome);
        navSearch          = findViewById(R.id.navSearch);
        navDeals           = findViewById(R.id.navDeals);
        navProfile         = findViewById(R.id.navProfile);
        navHomeIcon        = findViewById(R.id.navHomeIcon);
        navSearchIcon      = findViewById(R.id.navSearchIcon);
        navDealsIcon       = findViewById(R.id.navDealsIcon);
        navProfileIcon     = findViewById(R.id.navProfileIcon);
        navHomeText        = findViewById(R.id.navHomeText);
        navSearchText      = findViewById(R.id.navSearchText);
        navDealsText       = findViewById(R.id.navDealsText);
        navProfileText     = findViewById(R.id.navProfileText);
        btnBack            = findViewById(R.id.btnBack);
        layoutEmpty        = findViewById(R.id.layoutEmpty);

        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        // Placeholder until Firestore responds
        if (tvTotalOrdersStat != null) tvTotalOrdersStat.setText("—");
        if (tvTotalSpentStat  != null) tvTotalSpentStat.setText("—");
        if (tvDeliveredStat   != null) tvDeliveredStat.setText("—");
        if (tvAvgOrderStat    != null) tvAvgOrderStat.setText("—");

        // Set up RecyclerView
        if (recyclerOrders != null) {
            adapter = new OrdersAdapter(
                    filteredOrders,
                    order -> Toast.makeText(this,
                            "Order #" + order.getOrderId() + " — " + order.getStatus(),
                            Toast.LENGTH_SHORT).show(),
                    null,   // cancel not available from Deals screen
                    order -> {
                        // Visit Store navigation
                        String shopId = order.getShopId();
                        if (shopId == null || shopId.isEmpty()) return;
                        Intent i = new Intent(this, StoreDetailsActivity.class);
                        i.putExtra(StoreDetailsActivity.EXTRA_SHOP_ID,   shopId);
                        i.putExtra(StoreDetailsActivity.EXTRA_SHOP_NAME, order.getShopName());
                        i.putExtra(StoreDetailsActivity.EXTRA_SHOP_EMOJI, order.getShopEmoji());
                        startActivity(i);
                    });
            recyclerOrders.setLayoutManager(new LinearLayoutManager(this));
            recyclerOrders.setAdapter(adapter);
            recyclerOrders.setHasFixedSize(false);
        }
    }

    // ── Firestore: orders ──────────────────────────────────────────────────────

    private void loadOrders() {
        String uid = sessionManager.getUserId();
        if (uid.isEmpty()) {
            showEmptyState("Please log in to view your orders.");
            return;
        }

        orderRepository.getOrderHistory(uid, new DataCallback<List<OrderItem>>() {
            @Override
            public void onSuccess(List<OrderItem> orders) {
                allOrders = orders;
                applyFilter(activeFilter);
                if (orders.isEmpty()) showEmptyState("No orders yet.");
                else                  hideEmptyState();
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Failed to load orders", e);
                showEmptyState("Could not load orders. Please try again.");
            }
        });
    }

    // ── Firestore: stats (read-only; maintained by NearBuyHQ admin app) ────────

    /**
     * Loads lifetime stats from the customer's Firestore profile document.
     * These values are maintained by the NearBuyHQ admin app, never by this app.
     */
    private void loadStats() {
        String uid = sessionManager.getUserId();
        if (uid.isEmpty()) return;

        orderRepository.getCustomerStats(uid, new DataCallback<Customer>() {
            @Override
            public void onSuccess(Customer customer) {
                if (tvTotalOrdersStat != null)
                    tvTotalOrdersStat.setText(String.valueOf(customer.getTotalOrders()));
                if (tvTotalSpentStat != null) {
                    double spent = customer.getTotalSpent();
                    tvTotalSpentStat.setText(spent > 0
                            ? String.format("Rs.%.0f", spent) : "Rs.0");
                }
                // Delivered count: count from loaded orders for now
                // (admin will set totalOrders; delivered is derived locally)
                if (tvDeliveredStat != null) {
                    long delivered = allOrders.stream()
                            .filter(o -> "Delivered".equals(o.getStatus())).count();
                    tvDeliveredStat.setText(String.valueOf(delivered));
                }
                // Avg order
                if (tvAvgOrderStat != null) {
                    double spent = customer.getTotalSpent();
                    int total = customer.getTotalOrders();
                    if (total > 0)
                        tvAvgOrderStat.setText(String.format("Rs.%.0f", spent / total));
                    else
                        tvAvgOrderStat.setText("Rs.0");
                }
            }

            @Override
            public void onError(Exception e) {
                Log.w(TAG, "Could not load customer stats", e);
                if (tvTotalOrdersStat != null) tvTotalOrdersStat.setText("0");
                if (tvTotalSpentStat  != null) tvTotalSpentStat.setText("Rs.0");
                if (tvDeliveredStat   != null) tvDeliveredStat.setText("0");
                if (tvAvgOrderStat    != null) tvAvgOrderStat.setText("Rs.0");
            }
        });
    }

    // ── Tabs ───────────────────────────────────────────────────────────────────

    private void setupTabs() {
        if (tabAll       != null) tabAll.setOnClickListener(v       -> applyFilter("All"));
        if (tabDelivered != null) tabDelivered.setOnClickListener(v -> applyFilter("Delivered"));
        if (tabProcessing!= null) tabProcessing.setOnClickListener(v-> applyFilter("Processing"));
        if (tabCancelled != null) tabCancelled.setOnClickListener(v -> applyFilter("Cancelled"));
    }

    private void applyFilter(String filter) {
        activeFilter   = filter;
        filteredOrders = new ArrayList<>();
        for (OrderItem o : allOrders) {
            if (filter.equals("All") || o.getStatus().equals(filter))
                filteredOrders.add(o);
        }

        if (adapter != null) adapter.setItems(filteredOrders);

        int c = filteredOrders.size();
        if (tvResultCount != null)
            tvResultCount.setText(c + " order" + (c != 1 ? "s" : ""));

        setTabActive(tabAll,        filter.equals("All"));
        setTabActive(tabDelivered,  filter.equals("Delivered"));
        setTabActive(tabProcessing, filter.equals("Processing"));
        setTabActive(tabCancelled,  filter.equals("Cancelled"));
    }

    // ── Bottom navigation ──────────────────────────────────────────────────────

    private void setupNavigation() {
        if (navDealsIcon != null)
            navDealsIcon.setColorFilter(ContextCompat.getColor(this, R.color.nb_primary));
        if (navDealsText != null)
            navDealsText.setTextColor(ContextCompat.getColor(this, R.color.nb_primary));

        if (navHome    != null) navHome.setOnClickListener(v -> {
            startActivity(new Intent(this, DashboardActivity.class));
            finish();
        });
        if (navSearch  != null) navSearch.setOnClickListener(v ->
                startActivity(new Intent(this, SearchActivity.class)));
        if (navProfile != null) navProfile.setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class)));
    }

    // ── Empty state ────────────────────────────────────────────────────────────

    private void showEmptyState(String message) {
        if (layoutEmpty != null) {
            layoutEmpty.setVisibility(View.VISIBLE);
            if (layoutEmpty instanceof android.view.ViewGroup) {
                View child = ((android.view.ViewGroup) layoutEmpty).getChildAt(0);
                if (child instanceof TextView) ((TextView) child).setText(message);
            }
        }
        if (recyclerOrders != null) recyclerOrders.setVisibility(View.GONE);
    }

    private void hideEmptyState() {
        if (layoutEmpty    != null) layoutEmpty.setVisibility(View.GONE);
        if (recyclerOrders != null) recyclerOrders.setVisibility(View.VISIBLE);
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private void setTabActive(TextView tab, boolean active) {
        if (tab == null) return;
        if (active) {
            tab.setBackgroundResource(R.drawable.bg_distance_chip_active);
            tab.setTextColor(ContextCompat.getColor(this, R.color.white));
        } else {
            tab.setBackgroundResource(R.drawable.bg_distance_chip);
            tab.setTextColor(ContextCompat.getColor(this, R.color.nb_primary));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (navDealsIcon   != null) navDealsIcon.setColorFilter(ContextCompat.getColor(this, R.color.nb_primary));
        if (navDealsText   != null) navDealsText.setTextColor(ContextCompat.getColor(this, R.color.nb_primary));
        if (navHomeIcon    != null) navHomeIcon.setColorFilter(ContextCompat.getColor(this, R.color.text_dark_hint));
        if (navHomeText    != null) navHomeText.setTextColor(ContextCompat.getColor(this, R.color.text_dark_hint));
        if (navSearchIcon  != null) navSearchIcon.setColorFilter(ContextCompat.getColor(this, R.color.text_dark_hint));
        if (navSearchText  != null) navSearchText.setTextColor(ContextCompat.getColor(this, R.color.text_dark_hint));
        if (navProfileIcon != null) navProfileIcon.setColorFilter(ContextCompat.getColor(this, R.color.text_dark_hint));
        if (navProfileText != null) navProfileText.setTextColor(ContextCompat.getColor(this, R.color.text_dark_hint));
    }
}
