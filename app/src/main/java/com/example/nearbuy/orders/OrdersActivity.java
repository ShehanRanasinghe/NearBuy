package com.example.nearbuy.orders;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.nearbuy.R;
import com.example.nearbuy.app.startup.WelcomeActivity;
import com.example.nearbuy.core.SessionManager;
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
 * Stats shown: total order count and a formatted total spent amount.
 *
 * No sample / hardcoded order data is used in this activity.
 */
public class OrdersActivity extends AppCompatActivity {

    private static final String TAG = "NearBuy.Orders";

    // ── UI references ──────────────────────────────────────────────────────────
    private ListView listOrders;
    private TextView tvResultLabel, tvOrderCount, tvStatTotal, tvStatSpent;
    private TextView tabAll, tabDelivered, tabProcessing, tabCancelled;
    private View     layoutEmpty;  // empty-state container (if present in layout)

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

        // Back button
        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        // Load real orders from Firestore
        loadOrders();
    }

    // ── View binding ───────────────────────────────────────────────────────────

    private void initViews() {
        listOrders   = findViewById(R.id.listOrders);
        tvResultLabel = findViewById(R.id.tvResultLabel);
        tvOrderCount = findViewById(R.id.tvOrderCount);
        tabAll       = findViewById(R.id.tabAll);
        tabDelivered = findViewById(R.id.tabDelivered);
        tabProcessing= findViewById(R.id.tabProcessing);
        tabCancelled = findViewById(R.id.tabCancelled);
        layoutEmpty  = findViewById(R.id.layoutEmpty);  // optional – hide if not in layout

        // Stats row – show loading indicators until Firestore responds
        tvStatTotal = findViewById(R.id.tvStatTotal);
        tvStatSpent = findViewById(R.id.tvStatSpent);
        if (tvStatTotal != null) tvStatTotal.setText("—");
        if (tvStatSpent != null) tvStatSpent.setText("—");
    }

    // ── Firestore data load ────────────────────────────────────────────────────

    /**
     * Loads the signed-in customer's order history from Firestore.
     * On success: updates the order list and stats.
     * On failure: shows a toast and an empty state.
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

                // Calculate displayed stats from the loaded data
                updateStats();

                // Apply the currently selected filter
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

    // ── Stats ──────────────────────────────────────────────────────────────────

    /**
     * Calculates and updates the header stats (order count + total spent)
     * from the currently loaded allOrders list.
     */
    private void updateStats() {
        double totalSpent = 0;
        for (OrderItem o : allOrders) totalSpent += o.getTotalAmountRaw();

        if (tvStatTotal != null)
            tvStatTotal.setText(String.valueOf(allOrders.size()));
        if (tvStatSpent != null)
            tvStatSpent.setText(totalSpent > 0
                    ? String.format("Rs.%.0f", totalSpent) : "Rs.0");
    }

    // ── Filter tabs ────────────────────────────────────────────────────────────

    /** Wires up the four filter tabs and the list item click handler. */
    private void setupTabs() {
        if (tabAll       != null) tabAll.setOnClickListener(v       -> applyFilter("All"));
        if (tabDelivered != null) tabDelivered.setOnClickListener(v -> applyFilter("Delivered"));
        if (tabProcessing!= null) tabProcessing.setOnClickListener(v-> applyFilter("Processing"));
        if (tabCancelled != null) tabCancelled.setOnClickListener(v -> applyFilter("Cancelled"));

        if (listOrders != null) {
            listOrders.setOnItemClickListener((parent, view, pos, id) -> {
                OrderItem item = filteredOrders.get(pos);
                Toast.makeText(this,
                        "Order #" + item.getOrderId() + " — " + item.getStatus(),
                        Toast.LENGTH_SHORT).show();
            });
        }
    }

    /**
     * Filters the loaded allOrders list by the given status string and
     * refreshes the ListView adapter.
     *
     * @param filter "All", "Delivered", "Processing", or "Cancelled"
     */
    private void applyFilter(String filter) {
        activeFilter   = filter;
        filteredOrders = new ArrayList<>();

        for (OrderItem o : allOrders) {
            if (filter.equals("All") || o.getStatus().equals(filter)) {
                filteredOrders.add(o);
            }
        }

        adapter = new OrdersAdapter(this, filteredOrders);
        if (listOrders != null) listOrders.setAdapter(adapter);

        int count = filteredOrders.size();
        if (tvResultLabel != null)
            tvResultLabel.setText(count + " order" + (count != 1 ? "s" : ""));
        if (tvOrderCount  != null)
            tvOrderCount.setText(allOrders.size() + " Orders");

        // Update tab highlight state
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
}
