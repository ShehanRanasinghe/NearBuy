package com.example.nearbuy.orders;

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
import com.example.nearbuy.profile.ProfileActivity;
import com.example.nearbuy.search.SearchActivity;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

/**
 * OrdersActivity – displays the customer's order history with filter tabs.
 *
 * Filter tabs: All | Processing | Delivered | Cancelled
 * Stats (Total Orders / Total Spent) are computed from actual orders AND
 * from the customer Firestore profile (maintained by NearBuyHQ admin).
 */
public class OrdersActivity extends AppCompatActivity {

    private static final String TAG = "NearBuy.Orders";

    // ── UI references ──────────────────────────────────────────────────────────
    private RecyclerView recyclerOrders;
    private TextView tvResultLabel, tvOrderCount, tvStatTotal, tvStatSpent;
    private TextView tabAll, tabDelivered, tabProcessing, tabCancelled;
    private View     layoutEmpty;

    // ── Bottom navigation ──────────────────────────────────────────────────────
    private LinearLayout navHome, navSearch, navDeals, navProfile;
    private ImageView    navHomeIcon, navSearchIcon, navDealsIcon, navProfileIcon;
    private TextView     navHomeText, navSearchText, navDealsText, navProfileText;

    // ── Data ───────────────────────────────────────────────────────────────────
    private OrdersAdapter   adapter;
    private List<OrderItem> allOrders      = new ArrayList<>();
    private List<OrderItem> filteredOrders = new ArrayList<>();
    private String          activeFilter   = "All";

    // ── Dependencies ───────────────────────────────────────────────────────────
    private OrderRepository orderRepository;
    private SessionManager  sessionManager;

    // ── Real-time stats listener ───────────────────────────────────────────────
    private ListenerRegistration statsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window w = getWindow();
        w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        w.setStatusBarColor(ContextCompat.getColor(this, R.color.nb_primary_dark));
        w.setNavigationBarColor(ContextCompat.getColor(this, R.color.bg_white));

        setContentView(R.layout.activity_orders);
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
        setupBottomNavigation();

        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        loadOrders();
        loadStats();
    }

    // ── View binding ───────────────────────────────────────────────────────────

    private void initViews() {
        recyclerOrders = findViewById(R.id.recyclerOrders);
        tvResultLabel  = findViewById(R.id.tvResultLabel);
        tvOrderCount   = findViewById(R.id.tvOrderCount);
        tabAll         = findViewById(R.id.tabAll);
        tabDelivered   = findViewById(R.id.tabDelivered);
        tabProcessing  = findViewById(R.id.tabProcessing);
        tabCancelled   = findViewById(R.id.tabCancelled);
        layoutEmpty    = findViewById(R.id.layoutEmpty);

        tvStatTotal = findViewById(R.id.tvStatTotal);
        tvStatSpent = findViewById(R.id.tvStatSpent);
        if (tvStatTotal != null) tvStatTotal.setText("—");
        if (tvStatSpent != null) tvStatSpent.setText("—");

        if (recyclerOrders != null) {
            adapter = new OrdersAdapter(filteredOrders, order ->
                    Toast.makeText(this,
                            "Order #" + order.getOrderId() + " — " + order.getStatus(),
                            Toast.LENGTH_SHORT).show());
            recyclerOrders.setLayoutManager(new LinearLayoutManager(this));
            recyclerOrders.setAdapter(adapter);
            recyclerOrders.setHasFixedSize(false);
        }
    }

    // ── Bottom navigation ──────────────────────────────────────────────────────

    private void setupBottomNavigation() {
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

        // Highlight Orders tab as active
        if (navDealsIcon != null)
            navDealsIcon.setColorFilter(ContextCompat.getColor(this, R.color.nb_primary));
        if (navDealsText != null)
            navDealsText.setTextColor(ContextCompat.getColor(this, R.color.nb_primary));

        if (navHome != null) navHome.setOnClickListener(v -> {
            startActivity(new Intent(this, DashboardActivity.class));
            finish();
        });
        if (navSearch != null) navSearch.setOnClickListener(v ->
                startActivity(new Intent(this, SearchActivity.class)));
        // navDeals = current page, no action
        if (navProfile != null) navProfile.setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class)));
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
                if (orders.isEmpty()) {
                    showEmptyState("You haven't placed any orders yet.");
                } else {
                    hideEmptyState();
                    // Update stats from actual orders (immediate, no admin dependency)
                    updateStatsFromOrders(orders);
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Failed to load orders", e);
                showEmptyState("Could not load orders. Please try again.");
            }
        });
    }

    /** Computes and displays stats directly from the loaded order list. */
    private void updateStatsFromOrders(List<OrderItem> orders) {
        int totalOrders = orders.size();
        double totalSpent = 0;
        for (OrderItem o : orders) {
            if (!"Cancelled".equals(o.getStatus())) {
                totalSpent += o.getTotalAmountRaw();
            }
        }
        if (tvStatTotal != null) tvStatTotal.setText(String.valueOf(totalOrders));
        if (tvStatSpent != null) {
            tvStatSpent.setText(totalSpent > 0
                    ? String.format("Rs.%.0f", totalSpent) : "Rs.0");
        }
    }

    // ── Firestore: stats (real-time listener on customer document) ─────────────

    private void loadStats() {
        String uid = sessionManager.getUserId();
        if (uid.isEmpty()) return;

        if (statsListener != null) statsListener.remove();

        // Real-time listener: updates UI the moment admin processes orders
        statsListener = orderRepository.listenToCustomerStats(uid, new DataCallback<Customer>() {
            @Override
            public void onSuccess(Customer customer) {
                // Admin-maintained stats take priority; fallback is computed in loadOrders()
                if (customer.getTotalOrders() > 0) {
                    if (tvStatTotal != null)
                        tvStatTotal.setText(String.valueOf(customer.getTotalOrders()));
                    if (tvStatSpent != null) {
                        double spent = customer.getTotalSpent();
                        tvStatSpent.setText(spent > 0
                                ? String.format("Rs.%.0f", spent) : "Rs.0");
                    }
                } else {
                    // Fall back to computing from actual orders
                    orderRepository.computeStatsFromOrders(uid, new DataCallback<Customer>() {
                        @Override
                        public void onSuccess(Customer computed) {
                            if (tvStatTotal != null)
                                tvStatTotal.setText(String.valueOf(computed.getTotalOrders()));
                            if (tvStatSpent != null) {
                                tvStatSpent.setText(computed.getTotalSpent() > 0
                                        ? String.format("Rs.%.0f", computed.getTotalSpent())
                                        : "Rs.0");
                            }
                        }
                        @Override
                        public void onError(Exception e) { /* keep current values */ }
                    });
                }
            }

            @Override
            public void onError(Exception e) {
                Log.w(TAG, "Could not load customer stats", e);
            }
        });
    }

    // ── Filter tabs ────────────────────────────────────────────────────────────

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
            if (filter.equals("All") || o.getStatus().equalsIgnoreCase(filter)) {
                filteredOrders.add(o);
            }
        }

        if (adapter != null) adapter.setItems(filteredOrders);

        int count = filteredOrders.size();
        if (tvResultLabel != null)
            tvResultLabel.setText(count + " order" + (count != 1 ? "s" : ""));
        if (tvOrderCount  != null)
            tvOrderCount.setText(allOrders.size() + "");

        setTabActive(tabAll,        filter.equals("All"));
        setTabActive(tabDelivered,  filter.equals("Delivered"));
        setTabActive(tabProcessing, filter.equals("Processing"));
        setTabActive(tabCancelled,  filter.equals("Cancelled"));
    }

    // ── Empty state ────────────────────────────────────────────────────────────

    private void showEmptyState(String message) {
        if (layoutEmpty != null) {
            layoutEmpty.setVisibility(View.VISIBLE);
            TextView tvMsg = layoutEmpty.findViewWithTag("emptyMsg");
            if (tvMsg == null) tvMsg = (TextView) ((android.view.ViewGroup) layoutEmpty)
                    .getChildAt(0);
            if (tvMsg != null) tvMsg.setText(message);
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
    protected void onDestroy() {
        super.onDestroy();
        if (statsListener != null) statsListener.remove();
    }
}
