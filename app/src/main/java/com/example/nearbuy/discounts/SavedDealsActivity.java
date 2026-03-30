package com.example.nearbuy.discounts;

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
import com.example.nearbuy.data.model.DealItem;
import com.example.nearbuy.data.repository.DataCallback;
import com.example.nearbuy.data.repository.DealRepository;
import com.example.nearbuy.data.repository.OperationCallback;
import com.example.nearbuy.product.ProductDetailsActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * SavedDealsActivity – shows all deals the customer has bookmarked.
 *
 * Data is loaded from Firestore via DealRepository.getSavedDeals() using the
 * customer UID stored in SessionManager.
 *
 * Firestore path: NearBuy/{customerId}/saved_deals/{dealId}
 *
 * Features:
 *   • Summary stats bar: total saved deals, expiring-soon count, total savings.
 *   • Filter tabs: All / Food / Beverages / Household.
 *   • RecyclerView list with a "Remove" button per item.
 *   • Removal calls DealRepository.removeSavedDeal() to delete from Firestore.
 *
 * Session guard: redirects to WelcomeActivity if no session is active.
 */
public class SavedDealsActivity extends AppCompatActivity {

    private static final String TAG = "NearBuy.SavedDeals";

    // ── UI references ──────────────────────────────────────────────────────────
    private RecyclerView        recyclerView;
    private SavedDealsAdapter   adapter;
    private View                layoutEmpty;   // shown when the list is empty

    private TextView tvTotalSaved, tvExpiringSoon, tvTotalSavings, tvDealCount;
    private TextView tabAll, tabFood, tabBeverages, tabHousehold;

    // ── Data ───────────────────────────────────────────────────────────────────
    private List<SavedDealItem> allItems      = new ArrayList<>();
    private List<SavedDealItem> displayedItems = new ArrayList<>();
    private String              activeTab      = "All";

    // ── Dependencies ───────────────────────────────────────────────────────────
    private DealRepository  dealRepository;
    private SessionManager  sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window w = getWindow();
        w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        w.setStatusBarColor(ContextCompat.getColor(this, R.color.nb_primary_dark));

        setContentView(R.layout.activity_saved_deals);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        dealRepository = new DealRepository();
        sessionManager = SessionManager.getInstance(this);

        // ── Session guard ─────────────────────────────────────────────────────
        if (!sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, WelcomeActivity.class));
            finish();
            return;
        }

        initViews();
        setupTabs();

        // Load the customer's saved deals from Firestore
        loadSavedDeals();
    }

    // ── View binding ───────────────────────────────────────────────────────────

    /** Bind all layout views and wire the back button. */
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
        layoutEmpty    = findViewById(R.id.layoutEmpty);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Back arrow → return to previous screen
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }

    // ── Firestore data load ────────────────────────────────────────────────────

    /**
     * Loads all deals the customer has bookmarked from Firestore.
     * On success: converts DealItem objects to SavedDealItem UI models,
     * applies the current filter tab, and updates the stats bar.
     * On failure: shows an empty-state message.
     */
    private void loadSavedDeals() {
        String customerId = sessionManager.getUserId();

        dealRepository.getSavedDeals(customerId, new DataCallback<List<DealItem>>() {
            @Override
            public void onSuccess(List<DealItem> deals) {
                // Convert Firestore DealItem models to UI SavedDealItem models
                allItems = new ArrayList<>();
                for (DealItem deal : deals) {
                    allItems.add(convertToSavedDealItem(deal));
                }

                Log.d(TAG, "Loaded " + allItems.size() + " saved deals.");
                applyFilter(activeTab);
                updateStats();
                showOrHideEmpty();
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Failed to load saved deals", e);
                showEmptyState();
                Toast.makeText(SavedDealsActivity.this,
                        "Could not load saved deals. Please try again.",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ── DealItem → SavedDealItem conversion ───────────────────────────────────

    /**
     * Maps a Firestore-backed DealItem to the UI-only SavedDealItem model.
     * The SavedDealItem.id is set to the DealItem.id so that removal calls
     * can reference the correct Firestore document.
     */
    private SavedDealItem convertToSavedDealItem(DealItem deal) {
        // Choose an emoji based on the deal category (DealItem has no emoji field)
        String emoji = categoryToEmoji(deal.getCategory());

        // Format prices as display strings
        String salePrice = deal.getSalePrice() > 0
                ? String.format(Locale.ROOT, "Rs.%.0f", deal.getSalePrice()) : "";
        String origPrice = deal.getOriginalPrice() > 0
                ? String.format(Locale.ROOT, "Rs.%.0f", deal.getOriginalPrice()) : "";

        // Determine the display category (fall back to "General")
        String category = (deal.getCategory() != null && !deal.getCategory().isEmpty())
                ? deal.getCategory() : "General";

        // Distance label from the shop's coordinates (already calculated)
        double dist = deal.hasDistance() ? deal.getDistanceKm() : 0.0;

        return new SavedDealItem(
                deal.getId(),                                             // Firestore doc ID for deletion
                emoji,
                deal.getTitle()    != null ? deal.getTitle()    : "Deal",
                deal.getShopName() != null ? deal.getShopName() : "Nearby Shop",
                deal.getDiscountLabel() != null ? deal.getDiscountLabel() : "",
                salePrice,
                origPrice,
                deal.getExpiryLabel(),                                    // e.g. "Expires in 3 days"
                category,
                dist
        );
    }

    // ── Filter tabs ────────────────────────────────────────────────────────────

    /** Wires up the four category filter tabs. */
    private void setupTabs() {
        if (tabAll       != null) tabAll.setOnClickListener(v       -> applyFilter("All"));
        if (tabFood      != null) tabFood.setOnClickListener(v      -> applyFilter("Food"));
        if (tabBeverages != null) tabBeverages.setOnClickListener(v -> applyFilter("Beverages"));
        if (tabHousehold != null) tabHousehold.setOnClickListener(v -> applyFilter("Household"));
    }

    /**
     * Filters {@code allItems} by the selected category and rebuilds the adapter.
     *
     * @param filter "All", "Food", "Beverages", or "Household"
     */
    private void applyFilter(String filter) {
        activeTab      = filter;
        displayedItems = new ArrayList<>();

        for (SavedDealItem item : allItems) {
            if (filter.equals("All") || item.getCategory().equals(filter))
                displayedItems.add(item);
        }

        // Rebuild the adapter with the filtered set
        adapter = new SavedDealsAdapter(this, displayedItems);
        setupAdapterCallbacks();
        recyclerView.setAdapter(adapter);

        updateTabHighlight();
        updateStats();
    }

    // ── Adapter callbacks ──────────────────────────────────────────────────────

    /**
     * Attaches click listeners to the RecyclerView adapter:
     *   • Item tap   → open ProductDetailsActivity with deal data.
     *   • Remove tap → delete from Firestore and remove from the local list.
     */
    private void setupAdapterCallbacks() {
        adapter.setOnItemClickListener(new SavedDealsAdapter.OnItemClickListener() {

            @Override
            public void onItemClick(SavedDealItem item) {
                // Open the product/deal detail screen with the bookmarked deal's data
                Intent intent = new Intent(SavedDealsActivity.this,
                        ProductDetailsActivity.class);
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
                // Remove from the UI immediately for a fast, responsive feel
                adapter.removeItem(position);
                allItems.remove(item);
                updateStats();

                // Persist the removal to Firestore in the background
                String customerId = sessionManager.getUserId();
                dealRepository.removeSavedDeal(customerId, item.getId(),
                        new OperationCallback() {
                            @Override
                            public void onSuccess() {
                                Log.d(TAG, "Removed saved deal: " + item.getId());
                                Toast.makeText(SavedDealsActivity.this,
                                        item.getDealName() + " removed from saved deals.",
                                        Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onError(Exception e) {
                                // Re-add to the UI if the Firestore delete failed
                                Log.e(TAG, "Failed to remove saved deal: " + item.getId(), e);
                                allItems.add(item);
                                applyFilter(activeTab);
                                Toast.makeText(SavedDealsActivity.this,
                                        "Could not remove deal. Please try again.",
                                        Toast.LENGTH_SHORT).show();
                            }
                        });

                showOrHideEmpty();
            }
        });
    }

    // ── Stats bar ──────────────────────────────────────────────────────────────

    /**
     * Recalculates and updates the three stats badges:
     *   • Total saved deals count.
     *   • Number expiring within 2 days.
     *   • Total savings in rupees (original − sale price sum).
     */
    private void updateStats() {
        int total        = allItems.size();
        int expiringSoon = 0;
        int totalSaving  = 0;

        for (SavedDealItem item : allItems) {
            // Count deals expiring today, tomorrow, or within 2 days
            String expiry = item.getExpiryLabel().toLowerCase(java.util.Locale.ROOT);
            if (expiry.contains("today")    || expiry.contains("tomorrow")
                    || expiry.contains("2 day") || expiry.contains("expires in 1")) {
                expiringSoon++;
            }

            // Sum the savings: original price − sale price per item
            try {
                int sale = Integer.parseInt(item.getDealPrice().replaceAll("[^0-9]", ""));
                int orig = Integer.parseInt(item.getOriginalPrice().replaceAll("[^0-9]", ""));
                totalSaving += Math.max(0, orig - sale);
            } catch (NumberFormatException ignored) { /* skip if price cannot be parsed */ }
        }

        if (tvTotalSaved   != null) tvTotalSaved.setText(String.valueOf(total));
        if (tvExpiringSoon != null) tvExpiringSoon.setText(String.valueOf(expiringSoon));
        if (tvTotalSavings != null) tvTotalSavings.setText("Rs." + totalSaving);
        if (tvDealCount    != null) tvDealCount.setText(total + " Deal" + (total != 1 ? "s" : ""));
    }

    // ── Empty state ────────────────────────────────────────────────────────────

    /** Shows the empty-state view and hides the RecyclerView. */
    private void showEmptyState() {
        if (layoutEmpty   != null) layoutEmpty.setVisibility(View.VISIBLE);
        if (recyclerView  != null) recyclerView.setVisibility(View.GONE);
    }

    /** Decides whether to show the empty state based on the current item count. */
    private void showOrHideEmpty() {
        boolean empty = allItems.isEmpty();
        if (layoutEmpty  != null) layoutEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (recyclerView != null) recyclerView.setVisibility(empty ? View.GONE  : View.VISIBLE);
    }

    // ── Tab highlight ──────────────────────────────────────────────────────────

    /** Applies active/inactive background and text colour to each tab button. */
    private void updateTabHighlight() {
        int white = ContextCompat.getColor(this, R.color.white);
        int grey  = 0xFF9AA0AC;
        for (TextView tab : new TextView[]{tabAll, tabFood, tabBeverages, tabHousehold}) {
            if (tab == null) continue;
            boolean active = tab.getText().toString().equals(activeTab);
            tab.setBackgroundResource(active
                    ? R.drawable.bg_tab_active : R.drawable.bg_tab_inactive);
            tab.setTextColor(active ? white : grey);
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    /**
     * Maps a deal category to an emoji character for the card icon.
     * Mirrors the mapping used in SearchActivity.categoryToEmoji().
     */
    private String categoryToEmoji(String category) {
        if (category == null || category.isEmpty()) return "🏷️";
        switch (category.toLowerCase(java.util.Locale.ROOT)) {
            case "fruits":       return "🍎";
            case "vegetables":   return "🥦";
            case "food":         return "🍽️";
            case "dairy":        return "🥛";
            case "bakery":       return "🍞";
            case "beverages":    return "🧃";
            case "snacks":       return "🍟";
            case "meat":         return "🍗";
            case "seafood":      return "🐟";
            case "household":    return "🧹";
            case "groceries":    return "🛒";
            default:             return "🏷️";
        }
    }
}

