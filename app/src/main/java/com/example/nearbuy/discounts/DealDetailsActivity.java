package com.example.nearbuy.discounts;

import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.nearbuy.R;
import com.example.nearbuy.app.startup.WelcomeActivity;
import com.example.nearbuy.core.SessionManager;
import com.example.nearbuy.data.model.DealItem;
import com.example.nearbuy.data.repository.DealRepository;
import com.example.nearbuy.data.repository.OperationCallback;

/**
 * DealDetailsActivity – shows the full details of a deal or promotion.
 *
 * Receives deal information via Intent extras (populated by DealPromoAdapter or
 * SavedDealsActivity).  The following extras are supported:
 *   EXTRA_EMOJI    – emoji character for the deal icon
 *   EXTRA_TITLE    – deal title (e.g. "50% OFF Fresh Apples")
 *   EXTRA_SHOP     – shop name (e.g. "FreshMart Grocery")
 *   EXTRA_DISCOUNT – discount badge text (e.g. "50% OFF", "BOGO")
 *   EXTRA_PRICE    – sale price formatted string (e.g. "Rs.100")
 *   EXTRA_ORIG     – original price formatted string (e.g. "Rs.200")
 *   EXTRA_EXPIRY   – human-readable expiry label (e.g. "Expires in 3 days")
 *   EXTRA_CATEGORY – product category (e.g. "Fruits")
 *   EXTRA_DISTANCE – distance label (e.g. "0.8 km")
 *   EXTRA_DEAL_ID  – Firestore document ID of the deal (needed for Save action)
 *   EXTRA_SHOP_ID  – Firestore document ID of the shop (needed for Save action)
 *
 * Actions:
 *   • Share   – displays a placeholder; share-sheet integration can be added later.
 *   • Save    – bookmarks the deal under NearBuy/{customerId}/saved_deals.
 *   • Redeem  – confirmation toast (integration with order flow is a future feature).
 *
 * Session guard: redirects to WelcomeActivity if no session is active.
 */
public class DealDetailsActivity extends AppCompatActivity {

    private static final String TAG = "NearBuy.DealDetails";

    // ── Intent extra keys ──────────────────────────────────────────────────────
    public static final String EXTRA_EMOJI    = "emoji";
    public static final String EXTRA_TITLE    = "title";
    public static final String EXTRA_SHOP     = "shop";
    public static final String EXTRA_DISCOUNT = "discount";
    public static final String EXTRA_PRICE    = "price";
    public static final String EXTRA_ORIG     = "originalPrice";
    public static final String EXTRA_EXPIRY   = "expiry";
    public static final String EXTRA_CATEGORY = "category";
    public static final String EXTRA_DISTANCE = "distance";
    /** Firestore document ID – required to save / bookmark this deal. */
    public static final String EXTRA_DEAL_ID  = "dealId";
    /** Firestore shop ID – required when constructing the DealItem for saving. */
    public static final String EXTRA_SHOP_ID  = "shopId";

    // ── Dependencies ───────────────────────────────────────────────────────────
    private DealRepository dealRepository;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window w = getWindow();
        w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        w.setStatusBarColor(ContextCompat.getColor(this, R.color.nb_primary_dark));

        setContentView(R.layout.activity_deal_details);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        dealRepository = new DealRepository();
        sessionManager = SessionManager.getInstance(this);

        // ── Session guard ─────────────────────────────────────────────────────
        if (!sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, WelcomeActivity.class));
            finish();
            return;
        }

        populateData();
        setupButtons();
    }

    // ── UI population ──────────────────────────────────────────────────────────

    /**
     * Reads all intent extras and fills the deal detail views.
     * Any missing extra receives an empty string; views handle empty strings
     * gracefully so no NullPointerException is thrown.
     */
    private void populateData() {
        String emoji    = getIntent().getStringExtra(EXTRA_EMOJI);
        String title    = getIntent().getStringExtra(EXTRA_TITLE);
        String shop     = getIntent().getStringExtra(EXTRA_SHOP);
        String discount = getIntent().getStringExtra(EXTRA_DISCOUNT);
        String price    = getIntent().getStringExtra(EXTRA_PRICE);
        String orig     = getIntent().getStringExtra(EXTRA_ORIG);
        String expiry   = getIntent().getStringExtra(EXTRA_EXPIRY);
        String distance = getIntent().getStringExtra(EXTRA_DISTANCE);

        setTv(R.id.tv_deal_emoji,     emoji    != null ? emoji    : "🏷️");
        setTv(R.id.tv_deal_title,     title    != null ? title    : "");
        setTv(R.id.tv_store_name,     shop     != null ? shop     : "");
        setTv(R.id.tv_discount_badge, discount != null ? discount : "");
        setTv(R.id.tv_deal_price,     price    != null ? price    : "");
        setTv(R.id.tv_expiry_badge,   expiry   != null ? expiry   : "");
        setTv(R.id.tv_distance,
                distance != null ? distance + " away" : "");

        // Show original price with a strike-through to highlight the saving
        TextView tvOrig = findViewById(R.id.tv_original_price);
        if (tvOrig != null && orig != null) {
            tvOrig.setText(orig);
            tvOrig.setPaintFlags(tvOrig.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        }
    }

    // ── Button actions ─────────────────────────────────────────────────────────

    /**
     * Wires up the Back, Share, Save and Redeem action buttons.
     *
     * Share:  placeholder – can be replaced with Android share-sheet.
     * Save:   calls DealRepository.saveDeal() using the dealId / shopId extras.
     *         If either extra is missing the deal cannot be identified in Firestore,
     *         so a Toast is shown asking the customer to try from the deals list.
     * Redeem: confirmation Toast – full order-flow integration is a future feature.
     */
    private void setupButtons() {
        // Back arrow → return to the previous screen
        safeClick(R.id.btn_back, v -> finish());

        // Share → Android system share-sheet (placeholder)
        safeClick(R.id.btn_share, v ->
                Toast.makeText(this,
                        "Share this deal with friends!",
                        Toast.LENGTH_SHORT).show());

        // Save / Bookmark → persist to NearBuy/{customerId}/saved_deals
        safeClick(R.id.btn_save_deal, v -> saveDeal());

        // Redeem → confirmation (order-flow integration is a future feature)
        safeClick(R.id.btn_redeem_deal, v ->
                Toast.makeText(this,
                        "Deal redeemed! Show this screen at the store.",
                        Toast.LENGTH_LONG).show());
    }

    // ── Save deal ──────────────────────────────────────────────────────────────

    /**
     * Bookmarks this deal under the customer's saved_deals sub-collection.
     *
     * Requires EXTRA_DEAL_ID and EXTRA_SHOP_ID to be present in the intent.
     * If either is missing the action is skipped and the customer is prompted
     * to bookmark from the main Deals list where full IDs are always passed.
     */
    private void saveDeal() {
        String dealId = getIntent().getStringExtra(EXTRA_DEAL_ID);
        String shopId = getIntent().getStringExtra(EXTRA_SHOP_ID);

        // Cannot save without a Firestore document reference
        if (dealId == null || dealId.isEmpty()
                || shopId == null || shopId.isEmpty()) {
            Toast.makeText(this,
                    "Unable to save – please try from the Deals list.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Build a minimal DealItem from the intent extras for the Firestore write
        DealItem deal = new DealItem();
        deal.setId(dealId);
        deal.setShopId(shopId);
        deal.setTitle(getIntent().getStringExtra(EXTRA_TITLE));
        deal.setDiscountLabel(getIntent().getStringExtra(EXTRA_DISCOUNT));
        deal.setCategory(getIntent().getStringExtra(EXTRA_CATEGORY));
        deal.setCreatedAt(System.currentTimeMillis());

        String customerId = sessionManager.getUserId();

        dealRepository.saveDeal(customerId, deal, new OperationCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Deal saved: " + dealId);
                Toast.makeText(DealDetailsActivity.this,
                        "Deal saved to your bookmarks!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Failed to save deal: " + dealId, e);
                Toast.makeText(DealDetailsActivity.this,
                        "Could not save deal. Please try again.",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    /** Sets a TextView's text safely (no-op if the view is not in the layout). */
    private void setTv(int id, String text) {
        TextView tv = findViewById(id);
        if (tv != null) tv.setText(text);
    }

    /** Sets a click listener on a view safely (no-op if the view is not found). */
    private void safeClick(int id, android.view.View.OnClickListener listener) {
        android.view.View v = findViewById(id);
        if (v != null) v.setOnClickListener(listener);
    }
}