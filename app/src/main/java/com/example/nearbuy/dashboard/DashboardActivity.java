package com.example.nearbuy.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.nearbuy.R;
import com.example.nearbuy.categories.CategoriesActivity;
import com.example.nearbuy.deals.DealsActivity;
import com.example.nearbuy.notifications.NotificationsActivity;
import com.example.nearbuy.profile.ProfileActivity;
import com.example.nearbuy.search.SearchActivity;
import com.example.nearbuy.store.StoreDetailsActivity;

/**
 * DashboardActivity – Main home screen for the NearBuy customer app.
 */
public class DashboardActivity extends AppCompatActivity {

    // Bottom nav
    private LinearLayout navHome, navSearch, navDeals, navProfile;
    private ImageView navHomeIcon, navSearchIcon, navDealsIcon, navProfileIcon;
    private TextView navHomeText, navSearchText, navDealsText, navProfileText;

    // Header / Banner
    private TextView tvUserName, tvAvatarInitial;
    private TextView tvOrderCount, tvTotalSpent, tvTotalSaved;

    // View All links
    private TextView tvViewAllDeals, tvViewAllPromos;

    // Notification
    private ImageView btnNotifications;

    // Categories
    private LinearLayout catGroceries, catFresh, catFood, catMore;

    // Nearby Shops
    private LinearLayout shopFreshMart, shopQuickMart, shopGreenLeaf;

    // Sample data
    private String userName   = "John Doe";
    private int    orderCount = 12;
    private String totalSpent = "Rs.4,250";
    private String totalSaved = "Rs.890";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.nb_primary_dark));
        window.setNavigationBarColor(ContextCompat.getColor(this, R.color.bg_white));

        setContentView(R.layout.activity_dashboard);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        String intentName = getIntent().getStringExtra("userName");
        if (intentName != null && !intentName.isEmpty()) userName = intentName;

        initViews();
        populateData();
        setupNavigation();
        setupCategoryClicks();
        setupNearbyShopClicks();
    }

    private void initViews() {
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

        tvUserName      = findViewById(R.id.tvUserName);
        tvAvatarInitial = findViewById(R.id.tvAvatarInitial);
        tvOrderCount    = findViewById(R.id.tvOrderCount);
        tvTotalSpent    = findViewById(R.id.tvTotalSpent);
        tvTotalSaved    = findViewById(R.id.tvTotalSaved);

        tvViewAllDeals  = findViewById(R.id.tvViewAllDeals);
        tvViewAllPromos = findViewById(R.id.tvViewAllPromos);

        btnNotifications = findViewById(R.id.btnNotifications);

        catGroceries = findViewById(R.id.catGroceries);
        catFresh     = findViewById(R.id.catFresh);
        catFood      = findViewById(R.id.catFood);
        catMore      = findViewById(R.id.catMore);

        // Nearby shop cards (optional – may not exist in all layout versions)
        shopFreshMart = safeFind(R.id.shopFreshMart);
        shopQuickMart = safeFind(R.id.shopQuickMart);
        shopGreenLeaf = safeFind(R.id.shopGreenLeaf);
    }

    private void populateData() {
        tvUserName.setText(userName);
        if (!userName.isEmpty())
            tvAvatarInitial.setText(String.valueOf(userName.charAt(0)).toUpperCase());
        tvOrderCount.setText(String.valueOf(orderCount));
        tvTotalSpent.setText(totalSpent);
        tvTotalSaved.setText(totalSaved);
    }

    private void setupNavigation() {
        navHomeIcon.setColorFilter(ContextCompat.getColor(this, R.color.nb_primary));
        navHomeText.setTextColor(ContextCompat.getColor(this, R.color.nb_primary));

        navSearch.setOnClickListener(v ->
                startActivity(new Intent(this, SearchActivity.class)));
        navDeals.setOnClickListener(v ->
                startActivity(new Intent(this, DealsActivity.class)));
        navProfile.setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class)));
        btnNotifications.setOnClickListener(v ->
                startActivity(new Intent(this, NotificationsActivity.class)));

        tvViewAllDeals.setOnClickListener(v ->
                startActivity(new Intent(this, DealsActivity.class)));
        tvViewAllPromos.setOnClickListener(v ->
                startActivity(new Intent(this, DealsActivity.class)));
    }

    private void setupCategoryClicks() {
        if (catGroceries != null) catGroceries.setOnClickListener(v -> startSearch("Groceries"));
        if (catFresh     != null) catFresh.setOnClickListener(v ->     startSearch("Fresh Produce"));
        if (catFood      != null) catFood.setOnClickListener(v ->      startSearch("Food"));
        if (catMore      != null) catMore.setOnClickListener(v ->
                startActivity(new Intent(this, CategoriesActivity.class)));
    }

    private void setupNearbyShopClicks() {
        openShop(shopFreshMart, "FreshMart Grocery", "🏪", "0.8", "No. 10, Main St, Colombo 01");
        openShop(shopQuickMart, "QuickMart",          "🛒", "1.1", "No. 22, High St, Colombo 03");
        openShop(shopGreenLeaf, "GreenLeaf Store",    "🥦", "0.5", "No. 5, Park Rd, Colombo 05");
    }

    private void openShop(LinearLayout card, String name, String emoji,
                           String dist, String addr) {
        if (card == null) return;
        card.setOnClickListener(v -> {
            Intent i = new Intent(this, StoreDetailsActivity.class);
            i.putExtra(StoreDetailsActivity.EXTRA_SHOP_NAME,  name);
            i.putExtra(StoreDetailsActivity.EXTRA_SHOP_EMOJI, emoji);
            i.putExtra(StoreDetailsActivity.EXTRA_DISTANCE,   dist);
            i.putExtra(StoreDetailsActivity.EXTRA_ADDRESS,    addr);
            startActivity(i);
        });
    }

    private void startSearch(String query) {
        Intent intent = new Intent(this, SearchActivity.class);
        intent.putExtra("query", query);
        startActivity(intent);
    }

    @SuppressWarnings("unchecked")
    private <T extends android.view.View> T safeFind(int id) {
        try { return (T) findViewById(id); } catch (Exception e) { return null; }
    }

    @Override
    protected void onResume() {
        super.onResume();
        navHomeIcon.setColorFilter(ContextCompat.getColor(this, R.color.nb_primary));
        navHomeText.setTextColor(ContextCompat.getColor(this, R.color.nb_primary));
        navSearchIcon.setColorFilter(ContextCompat.getColor(this, R.color.text_dark_hint));
        navSearchText.setTextColor(ContextCompat.getColor(this, R.color.text_dark_hint));
        navDealsIcon.setColorFilter(ContextCompat.getColor(this, R.color.text_dark_hint));
        navDealsText.setTextColor(ContextCompat.getColor(this, R.color.text_dark_hint));
        navProfileIcon.setColorFilter(ContextCompat.getColor(this, R.color.text_dark_hint));
        navProfileText.setTextColor(ContextCompat.getColor(this, R.color.text_dark_hint));
    }
}
