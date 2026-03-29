package com.example.nearbuy.orders;

import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.nearbuy.R;

import java.util.ArrayList;
import java.util.List;

/**
 * OrdersActivity – Shows customer order history with filter tabs.
 *
 * Displays all past orders with status (Delivered / Processing / Cancelled),
 * item summary, shop name, order date and total amount.
 */
public class OrdersActivity extends AppCompatActivity {

    private ListView   listOrders;
    private TextView   tvResultLabel, tvOrderCount;
    private TextView   tabAll, tabDelivered, tabProcessing, tabCancelled;

    private OrdersAdapter  adapter;
    private List<OrderItem> allOrders;
    private List<OrderItem> filteredOrders;
    private String activeFilter = "All";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window w = getWindow();
        w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        w.setStatusBarColor(ContextCompat.getColor(this, R.color.nb_primary_dark));

        setContentView(R.layout.activity_orders);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        initViews();
        loadSampleOrders();
        setupTabs();

        // Back button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        applyFilter("All");
    }

    private void initViews() {
        listOrders   = findViewById(R.id.listOrders);
        tvResultLabel= findViewById(R.id.tvResultLabel);
        tvOrderCount = findViewById(R.id.tvOrderCount);
        tabAll       = findViewById(R.id.tabAll);
        tabDelivered = findViewById(R.id.tabDelivered);
        tabProcessing= findViewById(R.id.tabProcessing);
        tabCancelled = findViewById(R.id.tabCancelled);

        // Stats
        ((TextView) findViewById(R.id.tvStatTotal)).setText("12");
        ((TextView) findViewById(R.id.tvStatDelivered)).setText("9");
        ((TextView) findViewById(R.id.tvStatSpent)).setText("Rs.4,250");
    }

    private void loadSampleOrders() {
        allOrders = new ArrayList<>();
        allOrders.add(new OrderItem("2024001", "FreshMart Grocery", "🏪",
                "Mar 25, 2026", "Red Apples x2, Milk 1L x1", "Rs.350", "Delivered", 3));
        allOrders.add(new OrderItem("2024002", "QuickMart", "🛒",
                "Mar 22, 2026", "Orange Juice 1L, Potato Chips", "Rs.275", "Delivered", 2));
        allOrders.add(new OrderItem("2024003", "GreenLeaf Store", "🥦",
                "Mar 20, 2026", "Broccoli 500g, Tomatoes 1kg", "Rs.150", "Delivered", 2));
        allOrders.add(new OrderItem("2024004", "DairyPlus", "🥛",
                "Mar 18, 2026", "Milk 2L x2, Curd 400g", "Rs.420", "Delivered", 3));
        allOrders.add(new OrderItem("2024005", "BakeryPlus", "🍞",
                "Mar 15, 2026", "Whole Wheat Bread, Croissant x4", "Rs.310", "Delivered", 2));
        allOrders.add(new OrderItem("2024006", "SeaFresh Market", "🐟",
                "Mar 12, 2026", "Salmon Fillet 500g", "Rs.750", "Delivered", 1));
        allOrders.add(new OrderItem("2024007", "NatureFarm", "🥚",
                "Mar 10, 2026", "Farm Fresh Eggs 12, Butter 200g", "Rs.460", "Delivered", 2));
        allOrders.add(new OrderItem("2024008", "SnackHub", "🍟",
                "Mar 08, 2026", "Chips 100g x3, Cookies 200g", "Rs.380", "Delivered", 4));
        allOrders.add(new OrderItem("2024009", "TropicFresh", "🥭",
                "Mar 05, 2026", "Mango 1kg, Pineapple 1 unit", "Rs.290", "Delivered", 2));
        allOrders.add(new OrderItem("2024010", "MeatHub", "🍗",
                "Mar 03, 2026", "Chicken Breast 1kg", "Rs.550", "Processing", 1));
        allOrders.add(new OrderItem("2024011", "FreshMart Grocery", "🏪",
                "Mar 01, 2026", "Mixed Berries, Blueberries 250g", "Rs.480", "Processing", 2));
        allOrders.add(new OrderItem("2024012", "QuickMart", "🛒",
                "Feb 27, 2026", "Energy Drink x4, Vitamin C", "Rs.620", "Cancelled", 5));
        filteredOrders = new ArrayList<>(allOrders);
    }

    private void setupTabs() {
        tabAll.setOnClickListener(v       -> applyFilter("All"));
        tabDelivered.setOnClickListener(v -> applyFilter("Delivered"));
        tabProcessing.setOnClickListener(v-> applyFilter("Processing"));
        tabCancelled.setOnClickListener(v -> applyFilter("Cancelled"));

        listOrders.setOnItemClickListener((parent, view, pos, id) -> {
            OrderItem item = filteredOrders.get(pos);
            Toast.makeText(this,
                    "Order #" + item.getOrderId() + " — " + item.getStatus(),
                    Toast.LENGTH_SHORT).show();
        });
    }

    private void applyFilter(String filter) {
        activeFilter = filter;
        filteredOrders = new ArrayList<>();
        for (OrderItem o : allOrders) {
            if (filter.equals("All") || o.getStatus().equals(filter)) {
                filteredOrders.add(o);
            }
        }
        adapter = new OrdersAdapter(this, filteredOrders);
        listOrders.setAdapter(adapter);

        int count = filteredOrders.size();
        tvResultLabel.setText(count + " order" + (count != 1 ? "s" : "") + " found");
        tvOrderCount.setText(allOrders.size() + " Orders");

        // Highlight active tab
        setTabActive(tabAll,        filter.equals("All"));
        setTabActive(tabDelivered,  filter.equals("Delivered"));
        setTabActive(tabProcessing, filter.equals("Processing"));
        setTabActive(tabCancelled,  filter.equals("Cancelled"));
    }

    private void setTabActive(TextView tab, boolean active) {
        if (active) {
            tab.setBackgroundResource(R.drawable.bg_distance_chip_active);
            tab.setTextColor(ContextCompat.getColor(this, R.color.white));
        } else {
            tab.setBackgroundResource(R.drawable.bg_distance_chip);
            tab.setTextColor(ContextCompat.getColor(this, R.color.nb_primary));
        }
    }
}

