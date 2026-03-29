package com.example.nearbuy.discounts;

import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.nearbuy.R;
import com.example.nearbuy.dashboard.DashboardActivity;
import com.example.nearbuy.orders.OrderItem;
import com.example.nearbuy.orders.OrdersAdapter;
import com.example.nearbuy.profile.ProfileActivity;
import com.example.nearbuy.search.SearchActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * DealsActivity – Now shows customer's own orders with lifetime stats.
 */
public class DealsActivity extends AppCompatActivity {

    private ListView     listOrders;
    private TextView     tvResultCount;
    private TextView     tabAll, tabDelivered, tabProcessing, tabCancelled;
    private LinearLayout navHome, navSearch, navDeals, navProfile;
    private ImageView    navHomeIcon, navSearchIcon, navDealsIcon, navProfileIcon;
    private TextView     navHomeText, navSearchText, navDealsText, navProfileText;
    private ImageView    btnBack;

    private OrdersAdapter    adapter;
    private List<OrderItem>  allOrders;
    private List<OrderItem>  filteredOrders;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window w = getWindow();
        w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        w.setStatusBarColor(ContextCompat.getColor(this, R.color.nb_primary_dark));
        setContentView(R.layout.activity_deals);
        if (getSupportActionBar() != null) getSupportActionBar().hide();
        initViews();
        loadSampleOrders();
        setupTabs();
        setupNavigation();
        applyFilter("All");
    }

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
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
    }

    private void loadSampleOrders() {
        allOrders = new ArrayList<>();
        allOrders.add(new OrderItem("ORD-2026-001","FreshMart Grocery","🏪","Mar 25, 2026","Fresh Apples x2, Milk x1","Rs.350","Delivered",3));
        allOrders.add(new OrderItem("ORD-2026-002","GreenLeaf Store","🥦","Mar 22, 2026","Broccoli x1, Tomatoes x2","Rs.220","Delivered",3));
        allOrders.add(new OrderItem("ORD-2026-003","QuickMart","🛒","Mar 20, 2026","Orange Juice x2, Eggs x1","Rs.560","Delivered",3));
        allOrders.add(new OrderItem("ORD-2026-004","SnackHub","🍟","Mar 18, 2026","Potato Chips x3","Rs.285","Delivered",3));
        allOrders.add(new OrderItem("ORD-2026-005","BakeryPlus","🍞","Mar 15, 2026","Whole Wheat Bread x2","Rs.260","Delivered",2));
        allOrders.add(new OrderItem("ORD-2026-006","NatureFarm","🥚","Mar 12, 2026","Farm Fresh Eggs x2, Milk x1","Rs.495","Delivered",3));
        allOrders.add(new OrderItem("ORD-2026-007","DairyPlus","🥛","Mar 10, 2026","Fresh Milk x3, Yoghurt x1","Rs.465","Delivered",4));
        allOrders.add(new OrderItem("ORD-2026-008","FreshMart Grocery","🏪","Mar 8, 2026","Bananas x2, Mango x3","Rs.420","Delivered",5));
        allOrders.add(new OrderItem("ORD-2026-009","MeatHub","🍗","Mar 28, 2026","Chicken Breast x1","Rs.550","Processing",1));
        allOrders.add(new OrderItem("ORD-2026-010","SeaFresh Market","🐟","Mar 27, 2026","Salmon Fillet x1","Rs.750","Processing",1));
        allOrders.add(new OrderItem("ORD-2026-011","TropicFresh","🥭","Mar 5, 2026","Mango x5","Rs.500","Cancelled",5));
        allOrders.add(new OrderItem("ORD-2026-012","QuickMart","🛒","Mar 1, 2026","Orange Juice x1","Rs.180","Cancelled",1));
        filteredOrders = new ArrayList<>(allOrders);
    }

    private void setupTabs() {
        tabAll.setOnClickListener(v       -> applyFilter("All"));
        tabDelivered.setOnClickListener(v -> applyFilter("Delivered"));
        tabProcessing.setOnClickListener(v-> applyFilter("Processing"));
        tabCancelled.setOnClickListener(v -> applyFilter("Cancelled"));
    }

    private void applyFilter(String filter) {
        filteredOrders = new ArrayList<>();
        for (OrderItem o : allOrders) {
            boolean match;
            switch (filter) {
                case "Delivered":  match = "Delivered".equals(o.getStatus());  break;
                case "Processing": match = "Processing".equals(o.getStatus()); break;
                case "Cancelled":  match = "Cancelled".equals(o.getStatus());  break;
                default:           match = true;
            }
            if (match) filteredOrders.add(o);
        }
        adapter = new OrdersAdapter(this, filteredOrders);
        listOrders.setAdapter(adapter);
        int c = filteredOrders.size();
        tvResultCount.setText(c + " order" + (c != 1 ? "s" : ""));
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

    private void setupNavigation() {
        navDealsIcon.setColorFilter(ContextCompat.getColor(this, R.color.nb_primary));
        navDealsText.setTextColor(ContextCompat.getColor(this, R.color.nb_primary));
        navHome.setOnClickListener(v -> { startActivity(new Intent(this, DashboardActivity.class)); finish(); });
        navSearch.setOnClickListener(v -> startActivity(new Intent(this, SearchActivity.class)));
        navProfile.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        navDealsIcon.setColorFilter(ContextCompat.getColor(this, R.color.nb_primary));
        navDealsText.setTextColor(ContextCompat.getColor(this, R.color.nb_primary));
        navHomeIcon.setColorFilter(ContextCompat.getColor(this, R.color.text_dark_hint));
        navHomeText.setTextColor(ContextCompat.getColor(this, R.color.text_dark_hint));
        navSearchIcon.setColorFilter(ContextCompat.getColor(this, R.color.text_dark_hint));
        navSearchText.setTextColor(ContextCompat.getColor(this, R.color.text_dark_hint));
        navProfileIcon.setColorFilter(ContextCompat.getColor(this, R.color.text_dark_hint));
        navProfileText.setTextColor(ContextCompat.getColor(this, R.color.text_dark_hint));
    }
}
