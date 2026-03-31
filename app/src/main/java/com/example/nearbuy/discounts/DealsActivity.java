package com.example.nearbuy.discounts;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.nearbuy.R;
import com.example.nearbuy.app.startup.WelcomeActivity;
import com.example.nearbuy.core.SessionManager;
import com.example.nearbuy.dashboard.DashboardActivity;
import com.example.nearbuy.data.repository.DataCallback;
import com.example.nearbuy.data.repository.OrderRepository;
import com.example.nearbuy.orders.OrderItem;
import com.example.nearbuy.orders.OrdersAdapter;
import com.example.nearbuy.profile.ProfileActivity;
import com.example.nearbuy.search.SearchActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * DealsActivity – the Orders tab in the bottom navigation bar.
 *
 * Despite its class name (kept for back-stack compatibility), this screen shows
 * the customer's order history, identical to OrdersActivity but with bottom-nav
 * integrated.  Orders are loaded from Firestore via OrderRepository.
 *
 * No sample / hardcoded data is used in this activity.
 */
public class DealsActivity extends AppCompatActivity {

    private static final String TAG = "NearBuy.Deals";

    // ── UI references ──────────────────────────────────────────────────────────
    private ListView     listOrders;
    private TextView     tvResultCount;
    private TextView     tabAll, tabDelivered, tabProcessing, tabCancelled;
    private LinearLayout navHome, navSearch, navDeals, navProfile;
    private ImageView    navHomeIcon, navSearchIcon, navDealsIcon, navProfileIcon;
    private TextView     navHomeText, navSearchText, navDealsText, navProfileText;
    private ImageView    btnBack;
    private View         layoutEmpty;

    // ── Data ───────────────────────────────────────────────────────────────────
    private OrdersAdapter    adapter;
    private List<OrderItem>  allOrders      = new ArrayList<>();
    private List<OrderItem>  filteredOrders = new ArrayList<>();
    private String           activeFilter   = "All";

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

        // ── Session guard ─────────────────────────────────────────────────────
        if (!sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, WelcomeActivity.class));
            finish();
            return;
        }

        initViews();
        setupTabs();
        setupNavigation();

        // Load real orders from Firestore
        loadOrders();
    }

    // ── View binding ───────────────────────────────────────────────────────────

    private void initViews() {
        listOrders    = findViewById(R.id.listOrders);
        tvResultCount = findViewById(R.id.tvResultCount);
        tabAll        = findViewById(R.id.tabAll);
        tabDelivered  = findViewById(R.id.tabDelivered);
        tabProcessing = findViewById(R.id.tabProcessing);
        tabCancelled  = findViewById(R.id.tabCancelled);
        navHome       = findViewById(R.id.navHome);
        navSearch     = findViewById(R.id.navSearch);
        navDeals      = findViewById(R.id.navDeals);
        navProfile    = findViewById(R.id.navProfile);
        navHomeIcon   = findViewById(R.id.navHomeIcon);
        navSearchIcon = findViewById(R.id.navSearchIcon);
        navDealsIcon  = findViewById(R.id.navDealsIcon);
        navProfileIcon= findViewById(R.id.navProfileIcon);
        navHomeText   = findViewById(R.id.navHomeText);
        navSearchText = findViewById(R.id.navSearchText);
        navDealsText  = findViewById(R.id.navDealsText);
        navProfileText= findViewById(R.id.navProfileText);
        btnBack       = findViewById(R.id.btnBack);
        layoutEmpty   = findViewById(R.id.layoutEmpty);

        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
    }

    // ── Firestore data load ────────────────────────────────────────────────────

    /**
     * Loads the customer's order history from Firestore.
     * On empty result → shows empty-state message.
     */
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

        adapter = new OrdersAdapter(this, filteredOrders);
        if (listOrders != null) listOrders.setAdapter(adapter);

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
        if (listOrders != null) listOrders.setVisibility(View.GONE);
    }

    private void hideEmptyState() {
        if (layoutEmpty != null) layoutEmpty.setVisibility(View.GONE);
        if (listOrders  != null) listOrders.setVisibility(View.VISIBLE);
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
        if (navDealsIcon != null)
            navDealsIcon.setColorFilter(ContextCompat.getColor(this, R.color.nb_primary));
        if (navDealsText != null)
            navDealsText.setTextColor(ContextCompat.getColor(this, R.color.nb_primary));
        if (navHomeIcon != null)
            navHomeIcon.setColorFilter(ContextCompat.getColor(this, R.color.text_dark_hint));
        if (navHomeText != null)
            navHomeText.setTextColor(ContextCompat.getColor(this, R.color.text_dark_hint));
        if (navSearchIcon != null)
            navSearchIcon.setColorFilter(ContextCompat.getColor(this, R.color.text_dark_hint));
        if (navSearchText != null)
            navSearchText.setTextColor(ContextCompat.getColor(this, R.color.text_dark_hint));
        if (navProfileIcon != null)
            navProfileIcon.setColorFilter(ContextCompat.getColor(this, R.color.text_dark_hint));
        if (navProfileText != null)
            navProfileText.setTextColor(ContextCompat.getColor(this, R.color.text_dark_hint));
    }
}
