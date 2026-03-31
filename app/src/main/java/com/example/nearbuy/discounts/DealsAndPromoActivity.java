package com.example.nearbuy.discounts;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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
import com.example.nearbuy.data.model.DealItem;
import com.example.nearbuy.data.repository.DataCallback;
import com.example.nearbuy.data.repository.DealRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * DealsAndPromoActivity – full listing of all active deals and promotions from
 * nearby shops.  Accessible ONLY via the "View All" links on the Dashboard home page.
 *
 * Data is loaded from Firestore via DealRepository.getAllDealsNearby().
 * The customer's GPS location and search radius are read from SessionManager.
 *
 * Two tabs:
 *   • Deals      – shop discounts (isPromotion = false)
 *   • Promotions – limited-time promo campaigns (isPromotion = true)
 *
 * Session guard: redirects to WelcomeActivity if no session is active.
 */
public class DealsAndPromoActivity extends AppCompatActivity {

    private static final String TAG = "NearBuy.DealsAndPromo";

    /** Intent extra key – pass "deals" or "promos" to open on a specific tab. */
    public static final String EXTRA_TAB = "startTab";

    // ── UI references ──────────────────────────────────────────────────────────
    private TextView     tabDeals, tabPromos, tvResultLabel;
    private RecyclerView rvDealPromo;

    // ── Data ───────────────────────────────────────────────────────────────────
    // Loaded once from Firestore and kept in memory for fast tab switching
    private List<DealPromoItem> allDeals  = new ArrayList<>();
    private List<DealPromoItem> allPromos = new ArrayList<>();

    private String activeTab = "deals"; // currently visible tab

    // ── Dependencies ───────────────────────────────────────────────────────────
    private DealRepository dealRepository;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window w = getWindow();
        w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        w.setStatusBarColor(ContextCompat.getColor(this, R.color.nb_primary_dark));

        setContentView(R.layout.activity_deals_and_promo);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        dealRepository = new DealRepository();
        sessionManager = SessionManager.getInstance(this);

        // ── Session guard ─────────────────────────────────────────────────────
        if (!sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, WelcomeActivity.class));
            finish();
            return;
        }

        // Determine which tab to open on (passed from Dashboard "View All" links)
        String startTab = getIntent().getStringExtra(EXTRA_TAB);
        if ("promos".equals(startTab)) activeTab = "promos";

        initViews();
        setupTabs();
        showTab(activeTab); // Show the tab immediately (empty) while data loads

        // Load real deals and promotions from Firestore
        loadDealsFromFirestore();

        // Back arrow
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    // ── View binding ───────────────────────────────────────────────────────────

    /** Bind all layout views. */
    private void initViews() {
        tabDeals      = findViewById(R.id.tabDeals);
        tabPromos     = findViewById(R.id.tabPromos);
        tvResultLabel = findViewById(R.id.tvResultLabel);
        rvDealPromo   = findViewById(R.id.rvDealPromo);
        rvDealPromo.setLayoutManager(new LinearLayoutManager(this));
    }

    // ── Firestore data load ────────────────────────────────────────────────────

    /**
     * Loads both deals and promotions from all nearby shops in two parallel
     * Firestore fan-out queries via DealRepository.getAllDealsNearby().
     *
     * Customer location is read from SessionManager; defaults to Colombo centre
     * if no GPS fix has been saved yet.
     */
    private void loadDealsFromFirestore() {
        // Resolve customer location from the session cache
        double lat    = sessionManager.getLastLatitude();
        double lng    = sessionManager.getLastLongitude();
        float  radius = sessionManager.getSearchRadius();

        // Default to Colombo city centre if no GPS fix is available
        if (lat == 0.0 && lng == 0.0) {
            lat = 6.9271;
            lng = 79.8612;
        }

        final double finalLat = lat;
        final double finalLng = lng;

        // ── Load deals (isPromotion = false) ──────────────────────────────────
        dealRepository.getAllDealsNearby(finalLat, finalLng, radius, false,
                new DataCallback<List<DealItem>>() {
                    @Override
                    public void onSuccess(List<DealItem> deals) {
                        allDeals = convertToDealPromoItems(deals, DealPromoItem.TYPE_DEAL);
                        Log.d(TAG, "Loaded " + allDeals.size() + " deals.");
                        // Refresh the displayed tab if it is currently showing deals
                        if (activeTab.equals("deals")) showTab("deals");
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e(TAG, "Failed to load deals", e);
                        if (activeTab.equals("deals")) {
                            Toast.makeText(DealsAndPromoActivity.this,
                                    "Could not load deals. Check your connection.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        // ── Load promotions (isPromotion = true) ──────────────────────────────
        dealRepository.getAllDealsNearby(finalLat, finalLng, radius, true,
                new DataCallback<List<DealItem>>() {
                    @Override
                    public void onSuccess(List<DealItem> promos) {
                        // Filter: keep only items where isPromotion == true
                        List<DealItem> promoOnly = new ArrayList<>();
                        for (DealItem d : promos) {
                            if (d.isPromotion()) promoOnly.add(d);
                        }
                        allPromos = convertToDealPromoItems(promoOnly, DealPromoItem.TYPE_PROMO);
                        Log.d(TAG, "Loaded " + allPromos.size() + " promotions.");
                        if (activeTab.equals("promos")) showTab("promos");
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e(TAG, "Failed to load promotions", e);
                        if (activeTab.equals("promos")) {
                            Toast.makeText(DealsAndPromoActivity.this,
                                    "Could not load promotions. Check your connection.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    // ── DealItem → DealPromoItem conversion ───────────────────────────────────

    /**
     * Converts a list of Firestore-backed DealItem objects to the RecyclerView
     * DealPromoItem UI model used by DealPromoAdapter.
     *
     * @param deals list of DealItem from Firestore
     * @param type  DealPromoItem.TYPE_DEAL or TYPE_PROMO
     * @return list of DealPromoItem ready for the adapter
     */
    private List<DealPromoItem> convertToDealPromoItems(List<DealItem> deals, int type) {
        List<DealPromoItem> result = new ArrayList<>();
        for (DealItem deal : deals) {
            String emoji = categoryToEmoji(deal.getCategory());
            result.add(new DealPromoItem(
                    type,
                    emoji,
                    deal.getTitle()       != null ? deal.getTitle()       : "Deal",
                    deal.getShopName()    != null ? deal.getShopName()    : "Nearby Shop",
                    deal.getDescription() != null ? deal.getDescription() : "",
                    deal.getDiscountLabel() != null ? deal.getDiscountLabel() : "",
                    deal.getExpiryLabel()             // computed from expiresAt epoch
            ));
        }
        return result;
    }

    // ── Tab switching ──────────────────────────────────────────────────────────

    /** Wire up the Deals / Promotions tab click listeners. */
    private void setupTabs() {
        tabDeals.setOnClickListener(v  -> showTab("deals"));
        tabPromos.setOnClickListener(v -> showTab("promos"));
    }

    /**
     * Switches the visible tab and binds the corresponding data to the adapter.
     *
     * @param tab "deals" or "promos"
     */
    private void showTab(String tab) {
        activeTab = tab;
        List<DealPromoItem> list = tab.equals("deals") ? allDeals : allPromos;

        // Bind data to the RecyclerView
        rvDealPromo.setAdapter(new DealPromoAdapter(this, list));

        // Update result count label
        int    count = list.size();
        String label = tab.equals("deals") ? "deal" : "promotion";
        tvResultLabel.setText(count + " " + label + (count != 1 ? "s" : "") + " available");

        // Highlight the active tab button
        setTabActive(tabDeals,  tab.equals("deals"));
        setTabActive(tabPromos, tab.equals("promos"));
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    /** Applies the active / inactive visual state to a tab button. */
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

    /**
     * Maps a deal category string to a representative emoji.
     * Promotions of unrecognised categories receive the default 🎁 emoji.
     */
    private String categoryToEmoji(String category) {
        if (category == null || category.isEmpty()) return "🏷️";
        switch (category.toLowerCase(Locale.ROOT)) {
            case "fruits":      return "🍎";
            case "vegetables":  return "🥦";
            case "food":        return "🍽️";
            case "dairy":       return "🥛";
            case "bakery":      return "🍞";
            case "beverages":   return "🧃";
            case "snacks":      return "🍟";
            case "meat":        return "🍗";
            case "seafood":     return "🐟";
            case "household":   return "🧹";
            case "groceries":   return "🛒";
            case "promo":       return "🎁";
            default:            return "🏷️";
        }
    }
}
