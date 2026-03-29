package com.example.nearbuy.discounts;

import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nearbuy.R;
import com.example.nearbuy.product.ProductDetailsActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * SavedDealsActivity – Shows all deals bookmarked by the customer.
 *
 * Features:
 *  - Summary stats: total saved, expiring soon, total savings
 *  - Filter tabs: All / Food / Beverages / Household
 *  - RecyclerView list with swipe-to-remove support
 */
public class SavedDealsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private SavedDealsAdapter adapter;
    private List<SavedDealItem> allItems;
    private List<SavedDealItem> displayedItems;

    private TextView tvTotalSaved, tvExpiringSoon, tvTotalSavings, tvDealCount;
    private TextView tabAll, tabFood, tabBeverages, tabHousehold;
    private String activeTab = "All";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window w = getWindow();
        w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        w.setStatusBarColor(ContextCompat.getColor(this, R.color.nb_primary_dark));

        setContentView(R.layout.activity_saved_deals);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        initViews();
        loadSampleData();
        setupTabs();
        setupRecycler();
        updateStats();
    }

    private void initViews() {
        recyclerView   = findViewById(R.id.recycler_saved_deals);
        tvTotalSaved   = findViewById(R.id.tv_total_saved);
        tvExpiringSoon = findViewById(R.id.tv_expiring_soon);
        tvTotalSavings = findViewById(R.id.tv_total_savings);
        tvDealCount    = findViewById(R.id.tv_deal_count);
        tabAll         = findViewById(R.id.tab_all);
        tabFood        = findViewById(R.id.tab_food);
        tabBeverages   = findViewById(R.id.tab_beverages);
        tabHousehold   = findViewById(R.id.tab_household);

        // Back button
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }

    private void loadSampleData() {
        allItems = new ArrayList<>();
        allItems.add(new SavedDealItem("🍎","Fresh Red Apples 1kg","FreshMart Grocery",
                "50% OFF","Rs.100","Rs.200","Expires Tomorrow","Food",0.8));
        allItems.add(new SavedDealItem("🧃","Orange Juice 1L","QuickMart",
                "28% OFF","Rs.180","Rs.250","Expires in 3 days","Beverages",1.1));
        allItems.add(new SavedDealItem("🥦","Organic Broccoli 500g","GreenLeaf Store",
                "30% OFF","Rs.77","Rs.110","Expires in 5 days","Food",0.5));
        allItems.add(new SavedDealItem("🧴","Dish Washing Liquid 1L","SuperMart",
                "20% OFF","Rs.140","Rs.175","Expires in 7 days","Household",1.3));
        allItems.add(new SavedDealItem("🥛","Fresh Milk 1L","DairyPlus",
                "20% OFF","Rs.95","Rs.120","Expires in 2 days","Beverages",0.6));
        allItems.add(new SavedDealItem("🧹","Floor Cleaner 2L","CleanCo",
                "15% OFF","Rs.170","Rs.200","Expires in 6 days","Household",2.2));
        displayedItems = new ArrayList<>(allItems);
    }

    private void setupTabs() {
        tabAll.setOnClickListener(v       -> applyFilter("All"));
        tabFood.setOnClickListener(v      -> applyFilter("Food"));
        tabBeverages.setOnClickListener(v -> applyFilter("Beverages"));
        tabHousehold.setOnClickListener(v -> applyFilter("Household"));
    }

    private void applyFilter(String filter) {
        activeTab = filter;
        displayedItems = new ArrayList<>();
        for (SavedDealItem item : allItems) {
            if (filter.equals("All") || item.getCategory().equals(filter)) {
                displayedItems.add(item);
            }
        }
        adapter = new SavedDealsAdapter(this, displayedItems);
        setupAdapterCallbacks();
        recyclerView.setAdapter(adapter);
        updateTabHighlight();
        updateStats();
    }

    private void setupRecycler() {
        displayedItems = new ArrayList<>(allItems);
        adapter = new SavedDealsAdapter(this, displayedItems);
        setupAdapterCallbacks();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupAdapterCallbacks() {
        adapter.setOnItemClickListener(new SavedDealsAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(SavedDealItem item) {
                Intent intent = new Intent(SavedDealsActivity.this, ProductDetailsActivity.class);
                intent.putExtra(ProductDetailsActivity.EXTRA_EMOJI,          item.getEmoji());
                intent.putExtra(ProductDetailsActivity.EXTRA_NAME,           item.getDealName());
                intent.putExtra(ProductDetailsActivity.EXTRA_SHOP_NAME,      item.getShopName());
                intent.putExtra(ProductDetailsActivity.EXTRA_PRICE,          item.getDealPrice());
                intent.putExtra(ProductDetailsActivity.EXTRA_ORIGINAL_PRICE, item.getOriginalPrice());
                intent.putExtra(ProductDetailsActivity.EXTRA_DISTANCE,       item.getDistanceLabel());
                intent.putExtra(ProductDetailsActivity.EXTRA_CATEGORY,       item.getCategory());
                intent.putExtra(ProductDetailsActivity.EXTRA_DISCOUNT,       item.getDiscountLabel());
                startActivity(intent);
            }

            @Override
            public void onRemoveClick(SavedDealItem item, int position) {
                adapter.removeItem(position);
                // Also remove from allItems
                allItems.remove(item);
                updateStats();
                Toast.makeText(SavedDealsActivity.this,
                        item.getDealName() + " removed from saved deals", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateStats() {
        int total       = allItems.size();
        int expiringSoon = 0;
        int totalSaving  = 0;
        for (SavedDealItem item : allItems) {
            if (item.getExpiryLabel().toLowerCase().contains("tomorrow")
                    || item.getExpiryLabel().contains("2 days")) {
                expiringSoon++;
            }
            try {
                int sale = Integer.parseInt(item.getDealPrice().replaceAll("[^0-9]", ""));
                int orig = Integer.parseInt(item.getOriginalPrice().replaceAll("[^0-9]", ""));
                totalSaving += (orig - sale);
            } catch (Exception ignored) {}
        }
        if (tvTotalSaved   != null) tvTotalSaved.setText(String.valueOf(total));
        if (tvExpiringSoon != null) tvExpiringSoon.setText(String.valueOf(expiringSoon));
        if (tvTotalSavings != null) tvTotalSavings.setText("Rs." + totalSaving);
        if (tvDealCount    != null) tvDealCount.setText(total + " Deals");
    }

    private void updateTabHighlight() {
        int white = ContextCompat.getColor(this, R.color.white);
        int grey  = 0xFF9AA0AC;
        for (TextView tab : new TextView[]{tabAll, tabFood, tabBeverages, tabHousehold}) {
            boolean active = tab.getText().toString().equals(activeTab);
            tab.setBackgroundResource(active ? R.drawable.bg_tab_active : R.drawable.bg_tab_inactive);
            tab.setTextColor(active ? white : grey);
        }
    }
}

