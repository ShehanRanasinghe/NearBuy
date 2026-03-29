package com.example.nearbuy.search;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.example.nearbuy.R;
import com.example.nearbuy.product.ProductDetailsActivity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
/**
 * SearchActivity – Location-aware product search.
 * Distance filter chips (1km, 2km, 5km, 10km), sort by distance or price.
 * Tapping a result opens ProductDetailsActivity.
 */
public class SearchActivity extends AppCompatActivity {
    private EditText etSearch;
    private ListView listSearchResults;
    private TextView tvResultCount;
    private TextView chip1km, chip2km, chip5km, chip10km;
    private TextView sortDistance, sortPrice;
    private SearchResultAdapter adapter;
    private List<SearchResultItem> allItems;
    private List<SearchResultItem> filteredItems;
    private double selectedDistanceKm = 1.0;
    private boolean sortByDistance = true;
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
        setupDistanceChips();
        setupSortButtons();
        setupSearch();
        String query = getIntent().getStringExtra("query");
        if (query != null) { etSearch.setText(query); filterAndDisplay(query); }
        else { filterAndDisplay(""); }
    }
    private void initViews() {
        etSearch          = findViewById(R.id.etSearch);
        listSearchResults = findViewById(R.id.listSearchResults);
        tvResultCount     = findViewById(R.id.tvResultCount);
        chip1km           = findViewById(R.id.chip1km);
        chip2km           = findViewById(R.id.chip2km);
        chip5km           = findViewById(R.id.chip5km);
        chip10km          = findViewById(R.id.chip10km);
        sortDistance      = findViewById(R.id.sortDistance);
        sortPrice         = findViewById(R.id.sortPrice);
    }
    private void loadSampleData() {
        allItems = new ArrayList<>();
        allItems.add(new SearchResultItem("Fresh Red Apples",   "FreshMart Grocery","🍎","Rs.120","Rs.200",0.8,"Fruits"));
        allItems.add(new SearchResultItem("Broccoli",           "GreenLeaf Store",  "🥦","Rs.80", "Rs.110",0.5,"Vegetables"));
        allItems.add(new SearchResultItem("Orange Juice 1L",    "QuickMart",        "🧃","Rs.180","Rs.250",1.1,"Beverages"));
        allItems.add(new SearchResultItem("Potato Chips 100g",  "SnackHub",         "🍟","Rs.95", "Rs.130",2.0,"Snacks"));
        allItems.add(new SearchResultItem("Whole Wheat Bread",  "BakeryPlus",       "🍞","Rs.130","Rs.160",1.5,"Bakery"));
        allItems.add(new SearchResultItem("Farm Fresh Eggs 12", "NatureFarm",       "🥚","Rs.200","Rs.240",0.9,"Dairy"));
        allItems.add(new SearchResultItem("Salmon Fillet 500g", "SeaFresh Market",  "🐟","Rs.750","Rs.900",3.2,"Seafood"));
        allItems.add(new SearchResultItem("Milk 1L",            "DairyPlus",        "🥛","Rs.95", "Rs.120",0.6,"Dairy"));
        allItems.add(new SearchResultItem("Bananas 1kg",        "FreshMart Grocery","🍌","Rs.60", "Rs.80", 0.8,"Fruits"));
        allItems.add(new SearchResultItem("Chicken Breast 1kg", "MeatHub",          "🍗","Rs.550","Rs.650",4.1,"Meat"));
        allItems.add(new SearchResultItem("Mango",              "TropicFresh",      "🥭","Rs.100","Rs.140",1.8,"Fruits"));
        allItems.add(new SearchResultItem("Tomatoes 1kg",       "GreenLeaf Store",  "🍅","Rs.70", "Rs.90", 0.5,"Vegetables"));
        filteredItems = new ArrayList<>(allItems);
    }
    private void setupDistanceChips() {
        setChipActive(chip1km);
        chip1km.setOnClickListener(v  -> { selectedDistanceKm=1.0;  setChipActive(chip1km);  filterAndDisplay(etSearch.getText().toString()); });
        chip2km.setOnClickListener(v  -> { selectedDistanceKm=2.0;  setChipActive(chip2km);  filterAndDisplay(etSearch.getText().toString()); });
        chip5km.setOnClickListener(v  -> { selectedDistanceKm=5.0;  setChipActive(chip5km);  filterAndDisplay(etSearch.getText().toString()); });
        chip10km.setOnClickListener(v -> { selectedDistanceKm=10.0; setChipActive(chip10km); filterAndDisplay(etSearch.getText().toString()); });
    }
    private void setChipActive(TextView active) {
        int white=ContextCompat.getColor(this,R.color.white), nb=ContextCompat.getColor(this,R.color.nb_primary);
        for (TextView c : new TextView[]{chip1km,chip2km,chip5km,chip10km}) {
            if (c==active){ c.setBackground(ContextCompat.getDrawable(this,R.drawable.bg_distance_chip_active)); c.setTextColor(white); }
            else          { c.setBackground(ContextCompat.getDrawable(this,R.drawable.bg_distance_chip));        c.setTextColor(nb);    }
        }
    }
    private void setupSortButtons() {
        sortDistance.setOnClickListener(v -> { sortByDistance=true;
            sortDistance.setBackground(ContextCompat.getDrawable(this,R.drawable.bg_distance_chip_active)); sortDistance.setTextColor(ContextCompat.getColor(this,R.color.white));
            sortPrice.setBackground(ContextCompat.getDrawable(this,R.drawable.bg_distance_chip));           sortPrice.setTextColor(ContextCompat.getColor(this,R.color.nb_primary));
            filterAndDisplay(etSearch.getText().toString()); });
        sortPrice.setOnClickListener(v -> { sortByDistance=false;
            sortPrice.setBackground(ContextCompat.getDrawable(this,R.drawable.bg_distance_chip_active)); sortPrice.setTextColor(ContextCompat.getColor(this,R.color.white));
            sortDistance.setBackground(ContextCompat.getDrawable(this,R.drawable.bg_distance_chip));    sortDistance.setTextColor(ContextCompat.getColor(this,R.color.nb_primary));
            filterAndDisplay(etSearch.getText().toString()); });
    }
    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher(){
            @Override public void beforeTextChanged(CharSequence s,int st,int c,int a){}
            @Override public void afterTextChanged(Editable s){}
            @Override public void onTextChanged(CharSequence s,int start,int before,int count){ filterAndDisplay(s.toString()); }
        });
    }
    private void filterAndDisplay(String query) {
        filteredItems = new ArrayList<>();
        String q = query.toLowerCase().trim();
        for (SearchResultItem item : allItems) {
            boolean match = q.isEmpty() || item.getProductName().toLowerCase().contains(q)
                    || item.getShopName().toLowerCase().contains(q) || item.getCategory().toLowerCase().contains(q);
            if (match && item.getDistanceKm() <= selectedDistanceKm) filteredItems.add(item);
        }
        if (sortByDistance) Collections.sort(filteredItems,(a,b)->Double.compare(a.getDistanceKm(),b.getDistanceKm()));
        else Collections.sort(filteredItems,(a,b)->Integer.compare(parsePrice(a.getPrice()),parsePrice(b.getPrice())));
        adapter = new SearchResultAdapter(this, filteredItems);
        listSearchResults.setAdapter(adapter);
        tvResultCount.setText(filteredItems.size()+" result"+(filteredItems.size()!=1?"s":"")+" found");
        listSearchResults.setOnItemClickListener((parent,view,pos,id) -> {
            SearchResultItem item = filteredItems.get(pos);
            Intent intent = new Intent(this, ProductDetailsActivity.class);
            intent.putExtra(ProductDetailsActivity.EXTRA_EMOJI,          item.getEmoji());
            intent.putExtra(ProductDetailsActivity.EXTRA_NAME,           item.getProductName());
            intent.putExtra(ProductDetailsActivity.EXTRA_SHOP_NAME,      item.getShopName());
            intent.putExtra(ProductDetailsActivity.EXTRA_PRICE,          item.getPrice());
            intent.putExtra(ProductDetailsActivity.EXTRA_ORIGINAL_PRICE, item.getOriginalPrice());
            intent.putExtra(ProductDetailsActivity.EXTRA_DISTANCE,       item.getDistanceLabel());
            intent.putExtra(ProductDetailsActivity.EXTRA_CATEGORY,       item.getCategory());
            startActivity(intent);
        });
    }
    private int parsePrice(String p){ try{ return Integer.parseInt(p.replaceAll("[^0-9]","")); } catch(Exception e){ return 0; } }
}
