package com.example.nearbuy.orders;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nearbuy.R;
import com.example.nearbuy.app.startup.WelcomeActivity;
import com.example.nearbuy.core.SessionManager;
import com.example.nearbuy.data.model.Customer;
import com.example.nearbuy.data.repository.DataCallback;
import com.example.nearbuy.data.repository.OrderRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * OrdersActivity – displays the customer's order history with filter tabs.
 *
 * Orders are loaded from Firestore via OrderRepository using the customer UID
 * from SessionManager.  If no orders exist yet, an empty-state message is shown.
 *
 * Filter tabs: All | Delivered | Processing | Cancelled
 *
 * Stats shown (Total Orders / Total Spent) come from the customer's Firestore
 * profile document – maintained exclusively by the NearBuyHQ admin app.
 * This app NEVER writes to those counters.
 */
public class OrdersActivity extends AppCompatActivity {

    private static final String TAG = "NearBuy.Orders";

    // ── UI references ──────────────────────────────────────────────────────────
    private RecyclerView recyclerOrders;
    private TextView tvResultLabel, tvOrderCount, tvStatTotal, tvStatSpent;
    private TextView tabAll, tabDelivered, tabProcessing, tabCancelled;
    private View     layoutEmpty;

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

        setContentView(R.layout.activity_orders);
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

        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        // Load orders and stats independently
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

        // Stats placeholders
        tvStatTotal = findViewById(R.id.tvStatTotal);
        tvStatSpent = findViewById(R.id.tvStatSpent);
        if (tvStatTotal != null) tvStatTotal.setText("—");
        if (tvStatSpent != null) tvStatSpent.setText("—");

        // Set up RecyclerView
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
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Failed to load orders", e);
                showEmptyState("Could not load orders. Please try again.");
                Toast.makeText(OrdersActivity.this,
                        "Error loading orders: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ── Firestore: stats (read-only; maintained by NearBuyHQ admin app) ────────

    /**
     * Loads lifetime stats from the customer's Firestore profile document.
     * These values are updated by the NearBuyHQ admin app, never by this app.
     */
    private void loadStats() {
        String uid = sessionManager.getUserId();
        if (uid.isEmpty()) return;

        orderRepository.getCustomerStats(uid, new DataCallback<Customer>() {
            @Override
            public void onSuccess(Customer customer) {
                if (tvStatTotal != null)
                    tvStatTotal.setText(String.valueOf(customer.getTotalOrders()));
                if (tvStatSpent != null) {
                    double spent = customer.getTotalSpent();
                    tvStatSpent.setText(spent > 0
                            ? String.format("Rs.%.0f", spent) : "Rs.0");
                }
            }

            @Override
            public void onError(Exception e) {
                Log.w(TAG, "Could not load customer stats", e);
                if (tvStatTotal != null) tvStatTotal.setText("0");
                if (tvStatSpent != null) tvStatSpent.setText("Rs.0");
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
            if (filter.equals("All") || o.getStatus().equals(filter)) {
                filteredOrders.add(o);
            }
        }

        if (adapter != null) adapter.setItems(filteredOrders);

        int count = filteredOrders.size();
        if (tvResultLabel != null)
            tvResultLabel.setText(count + " order" + (count != 1 ? "s" : ""));
        if (tvOrderCount  != null)
            tvOrderCount.setText(allOrders.size() + " Orders");

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
}
