package com.example.nearbuy.map;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.nearbuy.R;
import com.example.nearbuy.core.SessionManager;
import com.example.nearbuy.dashboard.DashboardActivity;
import com.example.nearbuy.data.model.Shop;
import com.example.nearbuy.data.repository.DataCallback;
import com.example.nearbuy.data.repository.ShopRepository;
import com.example.nearbuy.notifications.NotificationsActivity;
import com.example.nearbuy.orders.OrdersActivity;
import com.example.nearbuy.profile.ProfileActivity;
import com.example.nearbuy.search.SearchActivity;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.List;

/**
 * NearbyMapActivity – displays nearby shops on a Google Map.
 *
 * Features (all FREE with the Google Maps SDK for Android):
 *   • Customer location marked with a custom blue person-pin icon.
 *   • Blue translucent circle showing the customer's search radius
 *     (as configured on the Profile page via the radius chips).
 *   • Teal shop-pin markers for every shop within the radius.
 *     Each marker shows the shop name as a title and the distance as a snippet.
 *   • Tap any shop marker to see its name and distance in the info window.
 *   • Zoom level is automatically set so the full radius circle is visible.
 *
 * What is NOT available on the Google Maps free tier (requires paid APIs):
 *   • Turn-by-turn directions / routes   → Directions API (charges per request)
 *   • Geocoding shop addresses            → Geocoding API  (charges per request)
 *   • Real-time traffic overlay           → Premium feature
 *
 * All distance calculations are done client-side with the Haversine formula
 * (already implemented in ShopRepository / SearchRepository).
 *
 * The search radius is read from SessionManager.getSearchRadius() which is
 * set by the customer on the Profile page.
 */
public class NearbyMapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "NearBuy.NearbyMap";

    // Default centre (Colombo) used when no GPS fix has been saved yet.
    private static final double DEFAULT_LAT = 6.9271;
    private static final double DEFAULT_LNG = 79.8612;

    // ── Bottom navigation ──────────────────────────────────────────────────────
    private LinearLayout navHome, navSearch, navMap, navDeals, navProfile;
    private ImageView    navHomeIcon, navSearchIcon, navMapIcon, navDealsIcon, navProfileIcon;
    private TextView     navHomeText, navSearchText, navMapText, navDealsText, navProfileText;

    // ── Info overlay ───────────────────────────────────────────────────────────
    private TextView tvRadiusInfo;
    private TextView tvShopCount;

    // ── Dependencies ───────────────────────────────────────────────────────────
    private SessionManager  sessionManager;
    private ShopRepository  shopRepository;
    private GoogleMap       googleMap;

    // ── Cached marker icons (created once, reused for every shop marker) ───────
    private BitmapDescriptor customerIcon;
    private BitmapDescriptor shopIcon;

    // ──────────────────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window w = getWindow();
        w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        w.setStatusBarColor(ContextCompat.getColor(this, R.color.nb_primary_dark));
        w.setNavigationBarColor(ContextCompat.getColor(this, R.color.bg_white));

        setContentView(R.layout.activity_nearby_map);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        sessionManager = SessionManager.getInstance(this);
        shopRepository  = new ShopRepository();

        // Pre-build custom marker bitmaps once (avoids per-marker allocation)
        customerIcon = vectorToBitmapDescriptor(R.drawable.ic_map_customer, 100, 100);
        shopIcon     = vectorToBitmapDescriptor(R.drawable.ic_map_shop,     100, 100);

        tvRadiusInfo = findViewById(R.id.tvRadiusInfo);
        tvShopCount  = findViewById(R.id.tvShopCount);

        ImageView btnNotif = findViewById(R.id.btnNotifications);
        if (btnNotif != null) {
            btnNotif.setOnClickListener(v ->
                    startActivity(new Intent(this, NotificationsActivity.class)));
        }

        setupBottomNavigation();

        // Obtain the SupportMapFragment and request the async map callback
        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    // ── OnMapReadyCallback ─────────────────────────────────────────────────────

    /**
     * Called by the Maps SDK when the GoogleMap object is ready to use.
     * We enable UI controls and then populate the map.
     */
    @Override
    public void onMapReady(GoogleMap map) {
        this.googleMap = map;

        map.getUiSettings().setZoomControlsEnabled(true);
        map.getUiSettings().setCompassEnabled(true);
        map.getUiSettings().setMapToolbarEnabled(false);
        map.getUiSettings().setMyLocationButtonEnabled(false);

        // Show info window when a shop marker is tapped
        map.setOnMarkerClickListener(marker -> {
            marker.showInfoWindow();
            return false; // let the default camera centering still happen
        });

        loadMapData();
    }

    // ── Map data loading ───────────────────────────────────────────────────────

    private void loadMapData() {
        if (googleMap == null) return;

        double lat    = sessionManager.getLastLatitude();
        double lng    = sessionManager.getLastLongitude();
        float  radius = sessionManager.getSearchRadius();

        // Fall back to Colombo when no GPS fix has been stored yet
        if (lat == 0.0 && lng == 0.0) {
            lat = DEFAULT_LAT;
            lng = DEFAULT_LNG;
            Toast.makeText(this,
                    "Location not set – showing default view. Set your location in Profile.",
                    Toast.LENGTH_LONG).show();
        }

        final double customerLat = lat;
        final double customerLng = lng;

        LatLng customerPos = new LatLng(customerLat, customerLng);

        // ── Camera ────────────────────────────────────────────────────────────
        googleMap.animateCamera(
                CameraUpdateFactory.newLatLngZoom(customerPos, zoomForRadius(radius)));

        // ── Customer marker ───────────────────────────────────────────────────
        BitmapDescriptor custDesc = customerIcon != null
                ? customerIcon
                : BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE);

        googleMap.addMarker(new MarkerOptions()
                .position(customerPos)
                .title("📍 Your Location")
                .snippet("This is your current position")
                .icon(custDesc)
                .zIndex(1.0f));  // draw customer on top of shop markers

        // ── Blue radius circle ────────────────────────────────────────────────
        // strokeColor / fillColor are ARGB ints.
        // 0xFF1565C0 = opaque dark-blue border
        // 0x331565C0 = 20 % opacity blue fill
        googleMap.addCircle(new CircleOptions()
                .center(customerPos)
                .radius(radius * 1000.0)    // km → metres
                .strokeColor(0xFF1565C0)
                .fillColor(0x331565C0)
                .strokeWidth(4f));

        // ── Radius label ──────────────────────────────────────────────────────
        String radiusLabel = radius == (int) radius
                ? String.valueOf((int) radius)
                : String.valueOf(radius);
        if (tvRadiusInfo != null) {
            tvRadiusInfo.setText("Search radius: " + radiusLabel + " km");
        }

        // ── Load shops from Firestore and add markers ─────────────────────────
        shopRepository.getNearbyShops(customerLat, customerLng, radius,
                new DataCallback<List<Shop>>() {

                    @Override
                    public void onSuccess(List<Shop> shops) {
                        if (googleMap == null) return;

                        BitmapDescriptor shopDesc = shopIcon != null
                                ? shopIcon
                                : BitmapDescriptorFactory.defaultMarker(
                                BitmapDescriptorFactory.HUE_CYAN);

                        int placed = 0;
                        for (Shop shop : shops) {
                            if (!shop.hasLocation()) continue;

                            LatLng shopPos = new LatLng(
                                    shop.getLatitude(), shop.getLongitude());

                            String snippet = buildSnippet(shop);

                            Marker m = googleMap.addMarker(new MarkerOptions()
                                    .position(shopPos)
                                    .title(shop.getName())
                                    .snippet(snippet)
                                    .icon(shopDesc));

                            if (m != null) m.setTag(shop.getId());
                            placed++;
                        }

                        final int count = placed;
                        if (tvShopCount != null) {
                            tvShopCount.setText(count + " shop"
                                    + (count != 1 ? "s" : "") + " nearby");
                        }

                        Log.d(TAG, "Placed " + count + " shop markers on map.");
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.w(TAG, "Could not load shops for map", e);
                        if (tvShopCount != null) {
                            tvShopCount.setText("Could not load shops");
                        }
                    }
                });
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    /**
     * Returns a human-readable snippet string for a shop's marker info window,
     * showing distance (if available) and the shop's address.
     */
    private String buildSnippet(Shop shop) {
        StringBuilder sb = new StringBuilder();
        if (shop.hasDistance()) {
            sb.append(shop.getDistanceLabel()).append(" away");
        }
        String addr = shop.getShopLocation();
        if (addr != null && !addr.isEmpty()) {
            if (sb.length() > 0) sb.append("  •  ");
            sb.append(addr);
        }
        return sb.length() > 0 ? sb.toString() : "Nearby shop";
    }

    /**
     * Picks an appropriate Google Maps zoom level so the full radius circle
     * fits comfortably on screen.
     *
     * @param radiusKm customer search radius in kilometres
     * @return zoom level (float)
     */
    private float zoomForRadius(float radiusKm) {
        if (radiusKm <=  1f) return 15.0f;
        if (radiusKm <=  2f) return 13.8f;
        if (radiusKm <=  5f) return 12.5f;
        if (radiusKm <= 10f) return 11.3f;
        return 10.2f;
    }

    /**
     * Renders an Android vector drawable into a {@link BitmapDescriptor} suitable
     * for use as a Google Maps custom marker icon.
     *
     * @param vectorResId the drawable resource ID
     * @param widthPx     desired bitmap width in pixels
     * @param heightPx    desired bitmap height in pixels
     * @return BitmapDescriptor, or {@code null} if conversion fails
     */
    private BitmapDescriptor vectorToBitmapDescriptor(int vectorResId,
                                                       int widthPx, int heightPx) {
        try {
            Drawable drawable = ContextCompat.getDrawable(this, vectorResId);
            if (drawable == null) return null;

            drawable.setBounds(0, 0, widthPx, heightPx);
            Bitmap bmp = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bmp);
            drawable.draw(canvas);
            return BitmapDescriptorFactory.fromBitmap(bmp);
        } catch (Exception e) {
            Log.w(TAG, "Could not convert vector to BitmapDescriptor", e);
            return null;
        }
    }

    // ── Bottom navigation ──────────────────────────────────────────────────────

    private void setupBottomNavigation() {
        navHome    = findViewById(R.id.navHome);
        navSearch  = findViewById(R.id.navSearch);
        navMap     = findViewById(R.id.navMap);
        navDeals   = findViewById(R.id.navDeals);
        navProfile = findViewById(R.id.navProfile);

        navHomeIcon    = findViewById(R.id.navHomeIcon);
        navSearchIcon  = findViewById(R.id.navSearchIcon);
        navMapIcon     = findViewById(R.id.navMapIcon);
        navDealsIcon   = findViewById(R.id.navDealsIcon);
        navProfileIcon = findViewById(R.id.navProfileIcon);

        navHomeText    = findViewById(R.id.navHomeText);
        navSearchText  = findViewById(R.id.navSearchText);
        navMapText     = findViewById(R.id.navMapText);
        navDealsText   = findViewById(R.id.navDealsText);
        navProfileText = findViewById(R.id.navProfileText);

        // Highlight the active Map tab
        if (navMapIcon != null)
            navMapIcon.setColorFilter(ContextCompat.getColor(this, R.color.nb_primary));
        if (navMapText != null)
            navMapText.setTextColor(ContextCompat.getColor(this, R.color.nb_primary));

        if (navHome != null) navHome.setOnClickListener(v -> {
            startActivity(new Intent(this, DashboardActivity.class));
            finish();
        });
        if (navSearch  != null) navSearch.setOnClickListener(v ->
                startActivity(new Intent(this, SearchActivity.class)));
        // navMap = current page – no action
        if (navDeals   != null) navDeals.setOnClickListener(v ->
                startActivity(new Intent(this, OrdersActivity.class)));
        if (navProfile != null) navProfile.setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class)));
    }

    // ── Lifecycle ──────────────────────────────────────────────────────────────

    @Override
    protected void onResume() {
        super.onResume();
        // Ensure Map tab stays highlighted when returning to this activity
        if (navMapIcon != null)
            navMapIcon.setColorFilter(ContextCompat.getColor(this, R.color.nb_primary));
        if (navMapText != null)
            navMapText.setTextColor(ContextCompat.getColor(this, R.color.nb_primary));
        // Reset all other tabs to inactive colour
        int hint = ContextCompat.getColor(this, R.color.text_dark_hint);
        if (navHomeIcon    != null) navHomeIcon.setColorFilter(hint);
        if (navHomeText    != null) navHomeText.setTextColor(hint);
        if (navSearchIcon  != null) navSearchIcon.setColorFilter(hint);
        if (navSearchText  != null) navSearchText.setTextColor(hint);
        if (navDealsIcon   != null) navDealsIcon.setColorFilter(hint);
        if (navDealsText   != null) navDealsText.setTextColor(hint);
        if (navProfileIcon != null) navProfileIcon.setColorFilter(hint);
        if (navProfileText != null) navProfileText.setTextColor(hint);
    }
}


