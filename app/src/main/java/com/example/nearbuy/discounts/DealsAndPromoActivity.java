package com.example.nearbuy.discounts;

import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nearbuy.R;

import java.util.ArrayList;
import java.util.List;

/**
 * DealsAndPromoActivity – Full list of all deals and promotions.
 * Accessible ONLY via "View All" in Dashboard home page.
 * Toggle between Deals and Promotions using the tab buttons.
 */
public class DealsAndPromoActivity extends AppCompatActivity {

    public static final String EXTRA_TAB = "startTab"; // "deals" or "promos"

    private TextView   tabDeals, tabPromos, tvResultLabel;
    private RecyclerView rvDealPromo;
    private DealPromoAdapter adapter;

    private List<DealPromoItem> allDeals;
    private List<DealPromoItem> allPromos;
    private String activeTab = "deals";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window w = getWindow();
        w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        w.setStatusBarColor(ContextCompat.getColor(this, R.color.nb_primary_dark));

        setContentView(R.layout.activity_deals_and_promo);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        // Determine which tab to start on
        String startTab = getIntent().getStringExtra(EXTRA_TAB);
        if ("promos".equals(startTab)) activeTab = "promos";

        initViews();
        loadSampleData();
        setupTabs();
        showTab(activeTab);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void initViews() {
        tabDeals      = findViewById(R.id.tabDeals);
        tabPromos     = findViewById(R.id.tabPromos);
        tvResultLabel = findViewById(R.id.tvResultLabel);
        rvDealPromo   = findViewById(R.id.rvDealPromo);
        rvDealPromo.setLayoutManager(new LinearLayoutManager(this));
    }

    private void loadSampleData() {
        // ── Deals ─────────────────────────────────────────────────────────
        allDeals = new ArrayList<>();
        allDeals.add(new DealPromoItem(DealPromoItem.TYPE_DEAL, "🍎", "50% OFF Fresh Apples",
                "FreshMart Grocery", "Farm-fresh, buy 1 kg get 1 free", "50% OFF", "Expires Apr 02"));
        allDeals.add(new DealPromoItem(DealPromoItem.TYPE_DEAL, "🧃", "BUY 1 GET 1 Orange Juice",
                "QuickMart", "1L cartons, selected brands only", "BOGO", "Today only"));
        allDeals.add(new DealPromoItem(DealPromoItem.TYPE_DEAL, "🍟", "30% OFF Potato Chips",
                "SnackHub", "100g packs, all flavours", "30% OFF", "3 days left"));
        allDeals.add(new DealPromoItem(DealPromoItem.TYPE_DEAL, "🍞", "Bread 20% OFF",
                "BakeryPlus", "Whole wheat and multigrain loaves", "20% OFF", "Expires Apr 05"));
        allDeals.add(new DealPromoItem(DealPromoItem.TYPE_DEAL, "🥚", "Eggs 15% OFF",
                "NatureFarm", "Farm fresh 12-pack", "15% OFF", "2 days left"));
        allDeals.add(new DealPromoItem(DealPromoItem.TYPE_DEAL, "🥛", "Milk 10% OFF",
                "DairyPlus", "Full cream and low fat 1L", "10% OFF", "Expires Apr 10"));
        allDeals.add(new DealPromoItem(DealPromoItem.TYPE_DEAL, "🐟", "Salmon 25% OFF",
                "SeaFresh Market", "500g fillet, fresh catch", "25% OFF", "Tomorrow"));
        allDeals.add(new DealPromoItem(DealPromoItem.TYPE_DEAL, "🥭", "Mango Season Deal",
                "TropicFresh", "Alphonso & Nam Doc Mai variety", "35% OFF", "5 days left"));
        allDeals.add(new DealPromoItem(DealPromoItem.TYPE_DEAL, "🍗", "Chicken Bulk Offer",
                "MeatHub", "Buy 2kg get 500g free", "SAVE Rs.200", "Expires Apr 08"));
        allDeals.add(new DealPromoItem(DealPromoItem.TYPE_DEAL, "🥦", "Veggie Bundle",
                "GreenLeaf Store", "Broccoli, tomatoes, carrots pack", "40% OFF", "Today only"));

        // ── Promotions ────────────────────────────────────────────────────
        allPromos = new ArrayList<>();
        allPromos.add(new DealPromoItem(DealPromoItem.TYPE_PROMO, "🎁", "Double Points Weekend",
                "All Partner Stores", "Earn 2x reward points at all stores", "2x POINTS", "2 days left"));
        allPromos.add(new DealPromoItem(DealPromoItem.TYPE_PROMO, "🚚", "Free Delivery Over Rs.500",
                "Select Stores", "Valid at select stores this weekend", "FREE DELIVERY", "Today only"));
        allPromos.add(new DealPromoItem(DealPromoItem.TYPE_PROMO, "💳", "Rs.100 Cashback",
                "FreshMart Grocery", "On orders above Rs.1000", "Rs.100 BACK", "4 days left"));
        allPromos.add(new DealPromoItem(DealPromoItem.TYPE_PROMO, "🏷", "Weekend Flash Sale",
                "QuickMart", "Up to 40% off selected items", "UP TO 40%", "Sat & Sun only"));
        allPromos.add(new DealPromoItem(DealPromoItem.TYPE_PROMO, "🎉", "New Member Bonus",
                "NearBuy Partner Stores", "First order 15% off with code WELCOME", "15% OFF", "Ongoing"));
        allPromos.add(new DealPromoItem(DealPromoItem.TYPE_PROMO, "⭐", "Loyalty Tier Upgrade",
                "All Stores", "Shop Rs.5000 this month, earn Gold tier", "GOLD TIER", "Month end"));
        allPromos.add(new DealPromoItem(DealPromoItem.TYPE_PROMO, "🛒", "Combo Saver",
                "GreenLeaf Store", "Buy veggies + fruits combo and save", "COMBO DEAL", "3 days left"));
        allPromos.add(new DealPromoItem(DealPromoItem.TYPE_PROMO, "📦", "Bulk Buy Bonus",
                "SnackHub", "Buy 5 snack packs get 1 free", "BUY 5+1", "Weekly offer"));
        allPromos.add(new DealPromoItem(DealPromoItem.TYPE_PROMO, "🍰", "Birthday Month Special",
                "BakeryPlus", "10% extra off all month for birthday shoppers", "BIRTHDAY 10%", "This month"));
        allPromos.add(new DealPromoItem(DealPromoItem.TYPE_PROMO, "🌙", "Evening Rush Hour Deal",
                "DairyPlus", "6 PM – 8 PM daily: 20% off dairy", "EVENING 20%", "Daily 6–8 PM"));
    }

    private void setupTabs() {
        tabDeals.setOnClickListener(v  -> showTab("deals"));
        tabPromos.setOnClickListener(v -> showTab("promos"));
    }

    private void showTab(String tab) {
        activeTab = tab;
        List<DealPromoItem> list = tab.equals("deals") ? allDeals : allPromos;
        adapter = new DealPromoAdapter(this, list);
        rvDealPromo.setAdapter(adapter);

        int count = list.size();
        String label = tab.equals("deals") ? "deal" : "promotion";
        tvResultLabel.setText(count + " " + label + (count != 1 ? "s" : "") + " available");

        boolean isDeals = tab.equals("deals");
        setTabActive(tabDeals,  isDeals);
        setTabActive(tabPromos, !isDeals);
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

