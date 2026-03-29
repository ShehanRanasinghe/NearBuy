package com.example.nearbuy.deals;

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
import com.example.nearbuy.notifications.NotificationsActivity;
import com.example.nearbuy.product.ProductDetailsActivity;
import com.example.nearbuy.profile.ProfileActivity;
import com.example.nearbuy.search.SearchActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * DealsActivity – Browsable deals & promotions screen with filter tabs.
 * Tapping a deal opens ProductDetailsActivity.
 */
public class DealsActivity extends AppCompatActivity {

    private ListView     listDeals;
    private TextView     tvResultCount;
    private TextView     tabAll, tabFood, tabGroceries, tabFresh, tabPromos;
    private LinearLayout navHome, navSearch, navDeals, navProfile;
    private ImageView    navHomeIcon, navSearchIcon, navDealsIcon, navProfileIcon;
    private TextView     navHomeText, navSearchText, navDealsText, navProfileText;

    private DealsAdapter adapter;
    private List<Deal>   allDeals;
    private List<Deal>   filteredDeals;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window w = getWindow();
        w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        w.setStatusBarColor(ContextCompat.getColor(this, R.color.nb_primary_dark));
        setContentView(R.layout.activity_deals);
        if (getSupportActionBar() != null) getSupportActionBar().hide();
        initViews();
        loadSampleDeals();
        setupTabs();
        setupNavigation();
        applyFilter("All");
    }

    private void initViews() {
        listDeals     = findViewById(R.id.listDeals);
        tvResultCount = findViewById(R.id.tvResultCount);
        tabAll        = findViewById(R.id.tabAll);
        tabFood       = findViewById(R.id.tabFood);
        tabGroceries  = findViewById(R.id.tabGroceries);
        tabFresh      = findViewById(R.id.tabFresh);
        tabPromos     = findViewById(R.id.tabPromos);
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
        ImageView btnNotif = findViewById(R.id.btnNotifications);
        if (btnNotif != null)
            btnNotif.setOnClickListener(v ->
                    startActivity(new Intent(this, NotificationsActivity.class)));
    }

    private void loadSampleDeals() {
        allDeals = new ArrayList<>();
        allDeals.add(new Deal("🍎","Fresh Red Apples 1kg","FreshMart Grocery","50% OFF","Rs.200","Rs.100","Fruits",0.8,3,false));
        allDeals.add(new Deal("🥦","Organic Broccoli 500g","GreenLeaf Store","30% OFF","Rs.110","Rs.77","Vegetables",0.5,1,false));
        allDeals.add(new Deal("🧃","Orange Juice 1L","QuickMart","28% OFF","Rs.250","Rs.180","Beverages",1.1,5,false));
        allDeals.add(new Deal("🍟","Potato Chips 100g","SnackHub","25% OFF","Rs.130","Rs.95","Snacks",2.0,7,false));
        allDeals.add(new Deal("🍞","Whole Wheat Bread","BakeryPlus","20% OFF","Rs.160","Rs.130","Bakery",1.5,2,false));
        allDeals.add(new Deal("🥚","Farm Fresh Eggs 12","NatureFarm","17% OFF","Rs.240","Rs.200","Dairy",0.9,4,false));
        allDeals.add(new Deal("🐟","Salmon Fillet 500g","SeaFresh Market","17% OFF","Rs.900","Rs.750","Seafood",3.2,2,false));
        allDeals.add(new Deal("🥛","Fresh Milk 1L","DairyPlus","20% OFF","Rs.120","Rs.95","Dairy",0.6,6,false));
        allDeals.add(new Deal("🍌","Bananas 1kg","FreshMart Grocery","25% OFF","Rs.80","Rs.60","Fruits",0.8,3,false));
        allDeals.add(new Deal("🍗","Chicken Breast 1kg","MeatHub","15% OFF","Rs.650","Rs.550","Meat",4.1,1,false));
        allDeals.add(new Deal("🎉","Buy 2 Get 1 Free – Yoghurt","DairyPlus","FREE ITEM","Rs.180","Rs.120","Dairy",0.6,3,true));
        allDeals.add(new Deal("🎁","Weekend Flash Sale – 60% off Veggies","GreenLeaf Store","60% OFF","Rs.200","Rs.80","Vegetables",0.5,0,true));
        allDeals.add(new Deal("🏷️","New Customer Deal – 40% off first order","QuickMart","40% OFF","Rs.500","Rs.300","Groceries",1.1,7,true));
        allDeals.add(new Deal("🎊","Combo: Bread + Eggs + Milk","BakeryPlus","30% OFF","Rs.580","Rs.406","Combo",1.5,2,true));
        filteredDeals = new ArrayList<>(allDeals);
    }

    private void setupTabs() {
        tabAll.setOnClickListener(v       -> applyFilter("All"));
        tabFood.setOnClickListener(v      -> applyFilter("Food"));
        tabGroceries.setOnClickListener(v -> applyFilter("Groceries"));
        tabFresh.setOnClickListener(v     -> applyFilter("Fresh"));
        tabPromos.setOnClickListener(v    -> applyFilter("Promos"));

        listDeals.setOnItemClickListener((parent, view, pos, id) -> {
            Deal deal = filteredDeals.get(pos);
            Intent intent = new Intent(this, ProductDetailsActivity.class);
            intent.putExtra(ProductDetailsActivity.EXTRA_EMOJI,          deal.getEmoji());
            intent.putExtra(ProductDetailsActivity.EXTRA_NAME,           deal.getTitle());
            intent.putExtra(ProductDetailsActivity.EXTRA_SHOP_NAME,      deal.getShopName());
            intent.putExtra(ProductDetailsActivity.EXTRA_PRICE,          deal.getSalePrice());
            intent.putExtra(ProductDetailsActivity.EXTRA_ORIGINAL_PRICE, deal.getOriginalPrice());
            intent.putExtra(ProductDetailsActivity.EXTRA_DISTANCE,       deal.getDistanceLabel());
            intent.putExtra(ProductDetailsActivity.EXTRA_CATEGORY,       deal.getCategory());
            intent.putExtra(ProductDetailsActivity.EXTRA_DISCOUNT,       deal.getDiscountLabel());
            startActivity(intent);
        });
    }

    private void applyFilter(String filter) {
        filteredDeals = new ArrayList<>();
        for (Deal d : allDeals) {
            boolean match;
            switch (filter) {
                case "Food":      match = d.getCategory().equals("Meat") || d.getCategory().equals("Seafood") || d.getCategory().equals("Snacks") || d.getCategory().equals("Bakery"); break;
                case "Groceries": match = d.getCategory().equals("Dairy") || d.getCategory().equals("Beverages") || d.getCategory().equals("Groceries"); break;
                case "Fresh":     match = d.getCategory().equals("Fruits") || d.getCategory().equals("Vegetables"); break;
                case "Promos":    match = d.isPromotion(); break;
                default:          match = true;
            }
            if (match) filteredDeals.add(d);
        }
        adapter = new DealsAdapter(this, filteredDeals);
        listDeals.setAdapter(adapter);
        int c = filteredDeals.size();
        tvResultCount.setText(c + " deal" + (c != 1 ? "s" : "") + " available");
        setTabActive(tabAll,       filter.equals("All"));
        setTabActive(tabFood,      filter.equals("Food"));
        setTabActive(tabGroceries, filter.equals("Groceries"));
        setTabActive(tabFresh,     filter.equals("Fresh"));
        setTabActive(tabPromos,    filter.equals("Promos"));
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
