package com.example.nearbuy.orders;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
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
import com.example.nearbuy.data.repository.OperationCallback;
import com.example.nearbuy.data.repository.OrderRepository;
import com.example.nearbuy.map.NearbyMapActivity;
import com.example.nearbuy.profile.ProfileActivity;
import com.example.nearbuy.search.SearchActivity;
import com.example.nearbuy.store.StoreDetailsActivity;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.ArrayList;
import java.util.List;
/**
 * OrdersActivity - displays the customer order history with filter tabs.
 * Clicking an order shows a friendly status toast and opens a one-time
 * report dialog. Reports are saved to NearBuyHQ/{shopId}/reports/{orderId}.
 */
public class OrdersActivity extends AppCompatActivity {
    private static final String TAG = "NearBuy.Orders";
    private static final int MAX_REPORT_WORDS = 200;
    private RecyclerView recyclerOrders;
    private TextView tvResultLabel, tvOrderCount, tvStatTotal, tvStatSpent;
    private TextView tabAll, tabDelivered, tabProcessing, tabCancelled;
    private View layoutEmpty;
    private LinearLayout navHome, navSearch, navMap, navDeals, navProfile;
    private ImageView navDealsIcon;
    private TextView navDealsText;
    private OrdersAdapter adapter;
    private List<OrderItem> allOrders = new ArrayList<>();
    private List<OrderItem> filteredOrders = new ArrayList<>();
    private String activeFilter = "All";
    private OrderRepository orderRepository;
    private SessionManager sessionManager;
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
        sessionManager = SessionManager.getInstance(this);
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
    /** Binds all layout views and sets up the RecyclerView with its adapter. */
    private void initViews() {
        recyclerOrders = findViewById(R.id.recyclerOrders);
        tvResultLabel  = findViewById(R.id.tvResultLabel);
        tvOrderCount   = findViewById(R.id.tvOrderCount);
        tabAll         = findViewById(R.id.tabAll);
        tabDelivered   = findViewById(R.id.tabDelivered);
        tabProcessing  = findViewById(R.id.tabProcessing);
        tabCancelled   = findViewById(R.id.tabCancelled);
        layoutEmpty    = findViewById(R.id.layoutEmpty);
        tvStatTotal    = findViewById(R.id.tvStatTotal);
        tvStatSpent    = findViewById(R.id.tvStatSpent);
        if (tvStatTotal != null) tvStatTotal.setText("-");
        if (tvStatSpent != null) tvStatSpent.setText("-");
        if (recyclerOrders != null) {
            adapter = new OrdersAdapter(
                    filteredOrders,
                    this::showOrderReportDialog,
                    this::showCancelOrderDialog,
                    this::openStoreDetails);
            recyclerOrders.setLayoutManager(new LinearLayoutManager(this));
            recyclerOrders.setAdapter(adapter);
            recyclerOrders.setHasFixedSize(false);
        }
    }
    // ── Cancel Order ──────────────────────────────────────────────────────────

    /** Shows a confirmation dialog before cancelling a Processing order. */
    private void showCancelOrderDialog(OrderItem order) {
        new AlertDialog.Builder(this)
                .setTitle("Cancel Order")
                .setMessage("Are you sure you want to cancel your order from "
                        + order.getShopName() + "?\n\nThis action cannot be undone.")
                .setPositiveButton("Yes, Cancel", (dialog, which) -> cancelOrder(order))
                .setNegativeButton("Keep Order", null)
                .show();
    }

    /** Calls the repository to update the order status to Cancelled, then reloads the list. */
    private void cancelOrder(OrderItem order) {
        String uid = sessionManager.getUserId();
        if (uid.isEmpty()) return;
        orderRepository.cancelOrder(uid, order, new OperationCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(OrdersActivity.this,
                        "Order cancelled successfully.", Toast.LENGTH_SHORT).show();
                loadOrders();
            }
            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Failed to cancel order", e);
                Toast.makeText(OrdersActivity.this,
                        "Could not cancel order. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ── Visit Store ───────────────────────────────────────────────────────────

    /** Opens StoreDetailsActivity for the shop associated with the given order. */
    private void openStoreDetails(OrderItem order) {
        String shopId = order.getShopId();
        if (shopId == null || shopId.isEmpty()) {
            Toast.makeText(this, "Store details not available.", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, StoreDetailsActivity.class);
        intent.putExtra(StoreDetailsActivity.EXTRA_SHOP_ID,    shopId);
        intent.putExtra(StoreDetailsActivity.EXTRA_SHOP_NAME,  order.getShopName());
        intent.putExtra(StoreDetailsActivity.EXTRA_SHOP_EMOJI, order.getShopEmoji());
        startActivity(intent);
    }

    // ── Report Dialog ─────────────────────────────────────────────────────────
    /** Shows the order status in a toast, then checks if a report was already submitted before opening the input dialog. */
    private void showOrderReportDialog(OrderItem order) {
        // Show only the order status in toast - no long IDs
        Toast.makeText(this, order.getStatus(), Toast.LENGTH_SHORT).show();
        String shopId = order.getShopId();
        if (shopId == null || shopId.isEmpty()) return;
        orderRepository.checkReportExists(shopId, order.getOrderId(),
                new DataCallback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean exists) {
                        if (Boolean.TRUE.equals(exists)) {
                            new AlertDialog.Builder(OrdersActivity.this)
                                    .setTitle("Report Already Submitted")
                                    .setMessage("You have already submitted a report for this order.")
                                    .setPositiveButton("OK", null)
                                    .show();
                        } else {
                            openReportInputDialog(order);
                        }
                    }
                    @Override
                    public void onError(Exception e) {
                        openReportInputDialog(order);
                    }
                });
    }
    /** Inflates the report dialog, wires up the word counter and submit button, then shows it. */
    private void openReportInputDialog(OrderItem order) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_order_report, null);
        TextView tvInfo      = dialogView.findViewById(R.id.tvReportOrderInfo);
        EditText etReport    = dialogView.findViewById(R.id.etReportText);
        TextView tvWordCount = dialogView.findViewById(R.id.tvWordCount);
        Button   btnCancel   = dialogView.findViewById(R.id.btnReportCancel);
        Button   btnSubmit   = dialogView.findViewById(R.id.btnReportSubmit);
        if (tvInfo != null) {
            tvInfo.setText(order.getShopName() + " - " + order.getOrderDate());
        }
        if (etReport != null && tvWordCount != null) {
            final Button submitBtn = btnSubmit;
            etReport.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    int words = countWords(s.toString());
                    tvWordCount.setText(words + " / " + MAX_REPORT_WORDS + " words");
                    boolean over = words > MAX_REPORT_WORDS;
                    tvWordCount.setTextColor(ContextCompat.getColor(
                            OrdersActivity.this,
                            over ? R.color.stat_red : R.color.text_dark_hint));
                    if (submitBtn != null) submitBtn.setEnabled(!over);
                }
            });
        }
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();
        if (btnCancel != null) btnCancel.setOnClickListener(v -> dialog.dismiss());
        if (btnSubmit != null) {
            btnSubmit.setOnClickListener(v -> {
                String text = etReport != null ? etReport.getText().toString().trim() : "";
                if (text.isEmpty()) {
                    Toast.makeText(this, "Please write your report before submitting.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (countWords(text) > MAX_REPORT_WORDS) {
                    Toast.makeText(this, "Report exceeds " + MAX_REPORT_WORDS + " words. Please shorten it.", Toast.LENGTH_SHORT).show();
                    return;
                }
                btnSubmit.setEnabled(false);
                btnSubmit.setText("Submitting...");
                orderRepository.saveReport(
                        order.getShopId(),
                        order.getOrderId(),
                        sessionManager.getUserId(),
                        sessionManager.getUserName(),
                        order.getShopName(),
                        text,
                        new OperationCallback() {
                            @Override
                            public void onSuccess() {
                                dialog.dismiss();
                                Toast.makeText(OrdersActivity.this,
                                        "Report submitted successfully.", Toast.LENGTH_SHORT).show();
                            }
                            @Override
                            public void onError(Exception e) {
                                Log.e(TAG, "Failed to save report", e);
                                btnSubmit.setEnabled(true);
                                btnSubmit.setText("Submit Report");
                                Toast.makeText(OrdersActivity.this,
                                        "Failed to submit report. Please try again.", Toast.LENGTH_SHORT).show();
                            }
                        });
            });
        }
        dialog.show();
    }
    /** Returns the number of words in the given text string; returns 0 for null or blank input. */
    private static int countWords(String text) {
        if (text == null || text.trim().isEmpty()) return 0;
        return text.trim().split("\\s+").length;
    }
    // ── Bottom navigation ──────────────────────────────────────────────────────
    private void setupBottomNavigation() {
        navHome    = findViewById(R.id.navHome);
        navSearch  = findViewById(R.id.navSearch);
        navMap     = findViewById(R.id.navMap);
        navDeals   = findViewById(R.id.navDeals);
        navProfile = findViewById(R.id.navProfile);
        navDealsIcon = findViewById(R.id.navDealsIcon);
        navDealsText = findViewById(R.id.navDealsText);
        if (navDealsIcon != null)
            navDealsIcon.setColorFilter(ContextCompat.getColor(this, R.color.nb_primary));
        if (navDealsText != null)
            navDealsText.setTextColor(ContextCompat.getColor(this, R.color.nb_primary));
        if (navHome != null) navHome.setOnClickListener(v -> {
            startActivity(new Intent(this, DashboardActivity.class));
            finish();
        });
        if (navSearch  != null) navSearch.setOnClickListener(v ->
                startActivity(new Intent(this, SearchActivity.class)));
        if (navMap     != null) navMap.setOnClickListener(v ->
                startActivity(new Intent(this, NearbyMapActivity.class)));
        if (navProfile != null) navProfile.setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class)));
    }
    // ── Firestore: orders ──────────────────────────────────────────────────────
    /** Fetches the customer's full order history from Firestore and applies the current filter. */
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
    /** Calculates total order count and amount spent from the loaded list and updates the stats row. */
    private void updateStatsFromOrders(List<OrderItem> orders) {
        int totalOrders = orders.size();
        double totalSpent = 0;
        for (OrderItem o : orders) {
            if (!"Cancelled".equals(o.getStatus())) totalSpent += o.getTotalAmountRaw();
        }
        if (tvStatTotal != null) tvStatTotal.setText(String.valueOf(totalOrders));
        if (tvStatSpent != null)
            tvStatSpent.setText(totalSpent > 0 ? String.format("Rs.%.0f", totalSpent) : "Rs.0");
    }
    // ── Firestore: stats ───────────────────────────────────────────────────────
    /** Attaches a real-time listener on the customer document to keep the stats row up to date. */
    private void loadStats() {
        String uid = sessionManager.getUserId();
        if (uid.isEmpty()) return;
        if (statsListener != null) statsListener.remove();
        statsListener = orderRepository.listenToCustomerStats(uid, new DataCallback<Customer>() {
            @Override
            public void onSuccess(Customer customer) {
                if (customer.getTotalOrders() > 0) {
                    if (tvStatTotal != null)
                        tvStatTotal.setText(String.valueOf(customer.getTotalOrders()));
                    if (tvStatSpent != null) {
                        double spent = customer.getTotalSpent();
                        tvStatSpent.setText(spent > 0 ? String.format("Rs.%.0f", spent) : "Rs.0");
                    }
                } else {
                    orderRepository.computeStatsFromOrders(uid, new DataCallback<Customer>() {
                        @Override
                        public void onSuccess(Customer computed) {
                            if (tvStatTotal != null)
                                tvStatTotal.setText(String.valueOf(computed.getTotalOrders()));
                            if (tvStatSpent != null)
                                tvStatSpent.setText(computed.getTotalSpent() > 0
                                        ? String.format("Rs.%.0f", computed.getTotalSpent())
                                        : "Rs.0");
                        }
                        @Override public void onError(Exception e) { /* keep current */ }
                    });
                }
            }
            @Override
            public void onError(Exception e) { Log.w(TAG, "Stats load failed", e); }
        });
    }
    // ── Filter tabs ────────────────────────────────────────────────────────────
    /** Wires each tab chip to call applyFilter with the corresponding status string. */
    private void setupTabs() {
        if (tabAll        != null) tabAll.setOnClickListener(v        -> applyFilter("All"));
        if (tabDelivered  != null) tabDelivered.setOnClickListener(v  -> applyFilter("Delivered"));
        if (tabProcessing != null) tabProcessing.setOnClickListener(v -> applyFilter("Processing"));
        if (tabCancelled  != null) tabCancelled.setOnClickListener(v  -> applyFilter("Cancelled"));
    }
    /** Filters allOrders by the given status, refreshes the adapter, and highlights the active tab. */
    private void applyFilter(String filter) {
        activeFilter   = filter;
        filteredOrders = new ArrayList<>();
        for (OrderItem o : allOrders) {
            if (filter.equals("All") || o.getStatus().equalsIgnoreCase(filter))
                filteredOrders.add(o);
        }
        if (adapter != null) adapter.setItems(filteredOrders);
        int count = filteredOrders.size();
        if (tvResultLabel != null)
            tvResultLabel.setText(count + " order" + (count != 1 ? "s" : ""));
        if (tvOrderCount != null)
            tvOrderCount.setText(String.valueOf(allOrders.size()));
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
            if (tvMsg == null)
                tvMsg = (TextView) ((android.view.ViewGroup) layoutEmpty).getChildAt(0);
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