package com.example.nearbuy.search;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import java.util.ArrayList;
import java.util.List;

/**
 * SearchActivity – Product search with 2-column grid view.
 * Shows the latest 20 products by default; filters on typing.
 */
public class SearchActivity extends AppCompatActivity {
    private EditText etSearch;
    private RecyclerView rvSearchResults;
    private TextView tvResultCount;
    private ImageView btnBack;
    private List<SearchResultItem> allItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window w = getWindow();
        w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        w.setStatusBarColor(ContextCompat.getColor(this, R.color.nb_primary_dark));
        setContentView(R.layout.activity_search);
        if (getSupportActionBar() != null) getSupportActionBar().hide();
        initViews();
        loadSampleData();
        setupSearch();
        String query = getIntent().getStringExtra("query");
        if (query != null) { etSearch.setText(query); filterAndDisplay(query); }
        else { filterAndDisplay(""); }
    }

    private void initViews() {
        etSearch        = findViewById(R.id.etSearch);
        rvSearchResults = findViewById(R.id.rvSearchResults);
        tvResultCount   = findViewById(R.id.tvResultCount);
        btnBack         = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
        if (rvSearchResults != null)
            rvSearchResults.setLayoutManager(new GridLayoutManager(this, 2));
    }

    private void loadSampleData() {
        allItems = new ArrayList<>();
        allItems.add(new SearchResultItem("Fresh Red Apples",    "FreshMart Grocery","🍎","Rs.120","Rs.200",0.8,"Fruits"));
        allItems.add(new SearchResultItem("Broccoli 500g",       "GreenLeaf Store",  "🥦","Rs.80", "Rs.110",0.5,"Vegetables"));
        allItems.add(new SearchResultItem("Orange Juice 1L",     "QuickMart",        "🧃","Rs.180","Rs.250",1.1,"Beverages"));
        allItems.add(new SearchResultItem("Potato Chips 100g",   "SnackHub",         "🍟","Rs.95", "Rs.130",2.0,"Snacks"));
        allItems.add(new SearchResultItem("Whole Wheat Bread",   "BakeryPlus",       "🍞","Rs.130","Rs.160",1.5,"Bakery"));
        allItems.add(new SearchResultItem("Farm Fresh Eggs 12",  "NatureFarm",       "🥚","Rs.200","Rs.240",0.9,"Dairy"));
        allItems.add(new SearchResultItem("Salmon Fillet 500g",  "SeaFresh Market",  "🐟","Rs.750","Rs.900",3.2,"Seafood"));
        allItems.add(new SearchResultItem("Milk 1L",             "DairyPlus",        "🥛","Rs.95", "Rs.120",0.6,"Dairy"));
        allItems.add(new SearchResultItem("Bananas 1kg",         "FreshMart Grocery","🍌","Rs.60", "Rs.80", 0.8,"Fruits"));
        allItems.add(new SearchResultItem("Chicken Breast 1kg",  "MeatHub",          "🍗","Rs.550","Rs.650",4.1,"Meat"));
        allItems.add(new SearchResultItem("Mango 1kg",           "TropicFresh",      "🥭","Rs.100","Rs.140",1.8,"Fruits"));
        allItems.add(new SearchResultItem("Tomatoes 1kg",        "GreenLeaf Store",  "🍅","Rs.70", "Rs.90", 0.5,"Vegetables"));
        allItems.add(new SearchResultItem("Greek Yoghurt 400g",  "DairyPlus",        "🍦","Rs.165","Rs.200",0.6,"Dairy"));
        allItems.add(new SearchResultItem("Cheddar Cheese 250g", "DairyPlus",        "🧀","Rs.320","Rs.380",0.6,"Dairy"));
        allItems.add(new SearchResultItem("Croissants x4",       "BakeryPlus",       "🥐","Rs.180","Rs.220",1.5,"Bakery"));
        allItems.add(new SearchResultItem("Coconut Water 330ml", "TropicFresh",      "🥥","Rs.85", "Rs.110",1.8,"Beverages"));
        allItems.add(new SearchResultItem("Chocolate Cookies",   "SnackHub",         "🍪","Rs.120","Rs.150",2.0,"Snacks"));
        allItems.add(new SearchResultItem("Carrot Bag 1kg",      "GreenLeaf Store",  "🥕","Rs.55", "Rs.75", 0.5,"Vegetables"));
        allItems.add(new SearchResultItem("Mixed Berries 250g",  "FreshMart Grocery","🍓","Rs.280","Rs.350",0.8,"Fruits"));
        allItems.add(new SearchResultItem("Tuna Can 185g",       "SeaFresh Market",  "🐠","Rs.195","Rs.230",3.2,"Seafood"));
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterAndDisplay(s.toString());
            }
        });
    }

    private void filterAndDisplay(String query) {
        List<SearchResultItem> filtered = new ArrayList<>();
        String q = query.toLowerCase().trim();
        for (SearchResultItem item : allItems) {
            if (q.isEmpty()
                    || item.getProductName().toLowerCase().contains(q)
                    || item.getShopName().toLowerCase().contains(q)
                    || item.getCategory().toLowerCase().contains(q)) {
                filtered.add(item);
            }
        }
        if (rvSearchResults != null) {
            rvSearchResults.setAdapter(new SearchGridAdapter(this, filtered));
        }
        int size = filtered.size();
        if (tvResultCount != null)
            tvResultCount.setText(size + " result" + (size != 1 ? "s" : "") + " found");
    }
}
