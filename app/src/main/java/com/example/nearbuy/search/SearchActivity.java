package com.example.nearbuy.search;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nearbuy.R;
import com.example.nearbuy.app.startup.WelcomeActivity;
import com.example.nearbuy.core.SessionManager;
import com.example.nearbuy.data.model.Product;
import com.example.nearbuy.data.repository.DataCallback;
import com.example.nearbuy.data.repository.SearchRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * SearchActivity – location-aware product search backed by Firebase Firestore.
 *
 * How it works:
 *   • On launch with no query: loads all available products from nearby shops.
 *   • On typing: fires a debounced Firebase search (500 ms delay) across all
 *     active shop product sub-collections within the customer's search radius.
 *   • The customer's GPS coordinates and search radius are read from
 *     SessionManager, which is populated during login and updated by GPS fixes.
 *   • If no location is saved, defaults to Colombo centre (6.9271, 79.8612).
 *
 * Session guard: redirects to WelcomeActivity if no session is active, so
 * this screen is never accessible without being logged in.
 *
 * Firestore path queried:  NearBuyHQ/{shopId}/products (via SearchRepository)
 */
public class SearchActivity extends AppCompatActivity {

    private static final String TAG = "NearBuy.Search";

    // Time to wait after the last keystroke before firing a Firebase search
    private static final int SEARCH_DEBOUNCE_MS = 500;

    // ── UI references ──────────────────────────────────────────────────────────
    private EditText     etSearch;
    private RecyclerView rvSearchResults;
    private TextView     tvResultCount;
    private View         progressBar;   // optional loading spinner in the layout

    // ── Dependencies ───────────────────────────────────────────────────────────
    private SearchRepository searchRepository;
    private SessionManager   sessionManager;

    // ── Debounce handler ───────────────────────────────────────────────────────
    private final Handler  searchHandler  = new Handler(Looper.getMainLooper());
    private       Runnable searchRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Match the app's primary dark status bar colour
        Window w = getWindow();
        w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        w.setStatusBarColor(ContextCompat.getColor(this, R.color.nb_primary_dark));

        setContentView(R.layout.activity_search);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        sessionManager   = SessionManager.getInstance(this);
        searchRepository = new SearchRepository();

        // ── Session guard ─────────────────────────────────────────────────────
        // SplashScreen normally guarantees a valid session here, but this guard
        // prevents direct-launch access (e.g. during testing) without a session.
        if (!sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, WelcomeActivity.class));
            finish();
            return;
        }

        initViews();
        setupSearch();

        // If the activity was launched with a pre-filled query (e.g. from
        // CategoriesActivity), run that query immediately
        String launchQuery = getIntent().getStringExtra("query");
        if (launchQuery != null && !launchQuery.isEmpty()) {
            etSearch.setText(launchQuery);
            performSearch(launchQuery);
        } else {
            // Load all available products from nearby shops as the default view
            performSearch("");
        }
    }

    // ── View binding ───────────────────────────────────────────────────────────

    /** Bind all layout views and wire the back button. */
    private void initViews() {
        etSearch        = findViewById(R.id.etSearch);
        rvSearchResults = findViewById(R.id.rvSearchResults);
        tvResultCount   = findViewById(R.id.tvResultCount);
        progressBar     = findViewById(R.id.progressBar); // may be null if not in layout

        // btnBack is used only here, so kept as a local variable
        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        // 2-column grid for the search result cards
        if (rvSearchResults != null)
            rvSearchResults.setLayoutManager(new GridLayoutManager(this, 2));
    }

    // ── Search wiring ──────────────────────────────────────────────────────────

    /**
     * Attaches a TextWatcher that debounces the Firebase search call.
     * Waiting 500 ms after the last keystroke avoids firing a query on every
     * character and keeps Firestore read costs reasonable.
     */
    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Cancel any previously scheduled search before scheduling a new one
                if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);
                String query = s.toString().trim();
                searchRunnable = () -> performSearch(query);
                searchHandler.postDelayed(searchRunnable, SEARCH_DEBOUNCE_MS);
            }
        });
    }

    // ── Firebase search ────────────────────────────────────────────────────────

    /**
     * Fires a location-aware Firestore product search via SearchRepository.
     *
     * Customer location is read from SessionManager (updated after each GPS fix).
     * If no location has been saved yet, Colombo centre is used as a safe default
     * so the dashboard is never blank on first launch.
     *
     * @param query text typed by the customer – empty string returns all products
     */
    private void performSearch(String query) {
        // Read the customer's last known location from the local session cache
        double lat    = sessionManager.getLastLatitude();
        double lng    = sessionManager.getLastLongitude();
        float  radius = sessionManager.getSearchRadius();

        // Default to Colombo city centre if no GPS fix has been stored yet
        if (lat == 0.0 && lng == 0.0) {
            lat = 6.9271;
            lng = 79.8612;
        }

        setLoading(true);

        // SearchRepository fans out to all active shops within radius, filters
        // products by name/category, and returns results sorted by distance
        searchRepository.search(query, lat, lng, radius, new DataCallback<List<Product>>() {
            @Override
            public void onSuccess(List<Product> products) {
                setLoading(false);

                // Convert Firestore Product objects to the UI SearchResultItem model
                List<SearchResultItem> items = new ArrayList<>();
                for (Product p : products) {
                    items.add(new SearchResultItem(
                            p.getName(),
                            p.getShopName()  != null ? p.getShopName()  : "Nearby Shop",
                            categoryToEmoji(p.getCategory()),
                            p.getPriceLabel(),
                            p.hasDiscount()
                                    ? String.format(Locale.ROOT, "Rs. %.2f", p.getOriginalPrice()) : "",
                            p.hasDistance() ? p.getDistanceKm() : 0.0,
                            p.getCategory() != null ? p.getCategory() : "General"
                    ));
                }

                // Bind the new list to the RecyclerView
                if (rvSearchResults != null)
                    rvSearchResults.setAdapter(
                            new SearchGridAdapter(SearchActivity.this, items));

                // Update result count label
                int size = items.size();
                if (tvResultCount != null)
                    tvResultCount.setText(
                            size + " result" + (size != 1 ? "s" : "") + " found");

                Log.d(TAG, "Search '" + query + "' returned " + size + " results.");
            }

            @Override
            public void onError(Exception e) {
                // Show a graceful error message without crashing
                setLoading(false);
                Log.e(TAG, "Search failed for query: " + query, e);
                if (tvResultCount != null)
                    tvResultCount.setText("Search unavailable. Check your connection.");
            }
        });
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    /**
     * Maps a Firestore product category string to a representative emoji.
     * Products do not store emojis; this mapping ensures the grid cards always
     * show a relevant icon even for new / unrecognised categories.
     *
     * @param category the product's category field from Firestore
     * @return a single emoji character string
     */
    private String categoryToEmoji(String category) {
        if (category == null || category.isEmpty()) return "🛍️";
        switch (category.toLowerCase(Locale.ROOT)) {
            case "fruits":       return "🍎";
            case "vegetables":   return "🥦";
            case "dairy":        return "🥛";
            case "bakery":       return "🍞";
            case "beverages":    return "🧃";
            case "snacks":       return "🍟";
            case "meat":         return "🍗";
            case "seafood":      return "🐟";
            case "groceries":    return "🛒";
            case "food":         return "🍽️";
            case "electronics":  return "📱";
            case "fashion":      return "👗";
            case "health":       return "💊";
            case "household":    return "🧹";
            default:             return "🛍️";
        }
    }

    /**
     * Shows or hides the loading indicator while a Firebase query is in flight.
     * The ProgressBar is optional – if the layout does not include one, this
     * method is a no-op so no NullPointerException is thrown.
     */
    private void setLoading(boolean loading) {
        if (progressBar != null)
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Cancel any pending debounce callback to prevent memory leaks when
        // the activity is destroyed before the search delay fires
        if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);
    }
}
