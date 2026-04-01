package com.example.nearbuy.profile;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.nearbuy.BuildConfig;
import com.example.nearbuy.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.PlacesClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * LocationPickerActivity – allows the customer to pick their location by:
 *   1. Typing a place name → Google Places autocomplete dropdown (Sri Lanka only)
 *   2. Tapping anywhere on the map → drops a pin + reverse-geocodes the address
 *
 * The map is centred on Sri Lanka and the camera is restricted to Sri Lanka bounds.
 * Returns EXTRA_LATITUDE, EXTRA_LONGITUDE, EXTRA_ADDRESS via setResult(RESULT_OK).
 */
public class LocationPickerActivity extends AppCompatActivity implements OnMapReadyCallback {

    // ── Result intent keys ───────────────────────────────────────────────
    public static final String EXTRA_LATITUDE  = "latitude";
    public static final String EXTRA_LONGITUDE = "longitude";
    public static final String EXTRA_ADDRESS   = "address";

    // ── Sri Lanka geographic bounds ──────────────────────────────────────
    private static final LatLngBounds SRI_LANKA_BOUNDS = new LatLngBounds(
            new LatLng(5.85, 79.65),   // South-West corner
            new LatLng(9.95, 81.95)    // North-East corner
    );

    // ── Views ────────────────────────────────────────────────────────────
    private EditText etSearch;
    private ListView lvSuggestions;
    private TextView tvSelectedAddress;
    private TextView tvCoordinates;
    private LinearLayout btnConfirm;
    private LinearLayout mapLoadingOverlay;

    // ── Maps / Places ────────────────────────────────────────────────────
    private GoogleMap googleMap;
    private Marker selectedMarker;
    private PlacesClient placesClient;
    private AutocompleteSessionToken sessionToken;
    private List<AutocompletePrediction> predictions = new ArrayList<>();

    // ── Selected location ────────────────────────────────────────────────
    private double selectedLat = 0.0;
    private double selectedLng = 0.0;
    private String selectedAddress = "";

    // ── Lifecycle ────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.nb_primary_dark));
        window.setNavigationBarColor(ContextCompat.getColor(this, R.color.bg_white));

        setContentView(R.layout.activity_location_picker);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        // Pre-fill if the caller passed an existing location
        selectedLat     = getIntent().getDoubleExtra(EXTRA_LATITUDE, 0.0);
        selectedLng     = getIntent().getDoubleExtra(EXTRA_LONGITUDE, 0.0);
        selectedAddress = getIntent().getStringExtra(EXTRA_ADDRESS) != null
                ? getIntent().getStringExtra(EXTRA_ADDRESS) : "";

        initPlaces();
        initViews();
        initMap();
    }

    // ── Initialisation ───────────────────────────────────────────────────

    private void initPlaces() {
        String apiKey = BuildConfig.GOOGLE_MAP_APIKEY;
        if (!apiKey.isEmpty() && !Places.isInitialized()) {
            Places.initialize(getApplicationContext(), apiKey);
        }
        if (Places.isInitialized()) {
            placesClient = Places.createClient(this);
            sessionToken = AutocompleteSessionToken.newInstance();
        }
    }

    private void initViews() {
        etSearch          = findViewById(R.id.etSearchLocation);
        lvSuggestions     = findViewById(R.id.lvLocationSuggestions);
        tvSelectedAddress = findViewById(R.id.tvSelectedAddress);
        tvCoordinates     = findViewById(R.id.tvCoordinates);
        btnConfirm        = findViewById(R.id.btnConfirmLocation);
        mapLoadingOverlay = findViewById(R.id.mapLoadingOverlay);

        // Pre-fill existing address
        if (!selectedAddress.isEmpty()) {
            etSearch.setText(selectedAddress);
        }
        updateInfoBar();
        setupSearch();

        // Back button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Confirm button
        btnConfirm.setOnClickListener(v -> confirmSelection());
    }

    private void initMap() {
        // Deferred fragment creation to avoid ANR on the main thread during enter-transition
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (isDestroyed() || isFinishing()) return;

            SupportMapFragment mapFragment = new SupportMapFragment();
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.mapContainer, mapFragment)
                    .commitAllowingStateLoss();
            mapFragment.getMapAsync(this);
        }, 400);
    }

    // ── Google Map ───────────────────────────────────────────────────────

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;

        // Map is ready – hide the loading overlay
        if (mapLoadingOverlay != null) mapLoadingOverlay.setVisibility(View.GONE);

        // Restrict map panning to Sri Lanka
        googleMap.setLatLngBoundsForCameraTarget(SRI_LANKA_BOUNDS);
        googleMap.setMinZoomPreference(6.0f);

        // Enable zoom controls and compass
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setCompassEnabled(true);
        googleMap.getUiSettings().setMapToolbarEnabled(false);

        // Show entire Sri Lanka initially
        googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(SRI_LANKA_BOUNDS, 60));

        // If we have a pre-existing location, place the marker
        if (selectedLat != 0.0 && selectedLng != 0.0) {
            placeMarker(new LatLng(selectedLat, selectedLng));
        }

        // Allow tapping the map to set a pin
        googleMap.setOnMapClickListener(latLng -> {
            selectedLat = latLng.latitude;
            selectedLng = latLng.longitude;
            placeMarker(latLng);
            reverseGeocode(latLng);
        });
    }

    private void placeMarker(LatLng latLng) {
        if (googleMap == null) return;
        if (selectedMarker != null) selectedMarker.remove();
        selectedMarker = googleMap.addMarker(new MarkerOptions()
                .position(latLng)
                .title("My Location"));
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14f));
        updateInfoBar();
    }

    // ── Places Autocomplete ──────────────────────────────────────────────

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchPlaces(s.toString().trim());
            }
        });

        lvSuggestions.setOnItemClickListener((parent, view, position, id) -> {
            if (position < predictions.size()) {
                AutocompletePrediction prediction = predictions.get(position);
                etSearch.setText(prediction.getFullText(null).toString());
                lvSuggestions.setVisibility(View.GONE);
                fetchPlaceDetails(prediction.getPlaceId());
            }
        });
    }

    private void searchPlaces(String query) {
        if (placesClient == null || query.length() < 2) {
            lvSuggestions.setVisibility(View.GONE);
            return;
        }

        FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                .setLocationBias(RectangularBounds.newInstance(SRI_LANKA_BOUNDS))
                .setCountries(Arrays.asList("LK"))
                .setSessionToken(sessionToken)
                .setQuery(query)
                .build();

        placesClient.findAutocompletePredictions(request)
                .addOnSuccessListener(response -> {
                    predictions = response.getAutocompletePredictions();
                    List<String> items = new ArrayList<>();
                    for (AutocompletePrediction p : predictions) {
                        items.add(p.getFullText(null).toString());
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            this, android.R.layout.simple_list_item_1, items);
                    lvSuggestions.setAdapter(adapter);
                    lvSuggestions.setVisibility(items.isEmpty() ? View.GONE : View.VISIBLE);
                })
                .addOnFailureListener(e -> lvSuggestions.setVisibility(View.GONE));
    }

    private void fetchPlaceDetails(String placeId) {
        if (placesClient == null) return;

        List<Place.Field> fields = Arrays.asList(
                Place.Field.NAME,
                Place.Field.LAT_LNG,
                Place.Field.ADDRESS
        );

        FetchPlaceRequest request = FetchPlaceRequest.newInstance(placeId, fields);
        placesClient.fetchPlace(request)
                .addOnSuccessListener(response -> {
                    Place place = response.getPlace();
                    if (place.getLatLng() != null) {
                        selectedLat     = place.getLatLng().latitude;
                        selectedLng     = place.getLatLng().longitude;
                        selectedAddress = place.getAddress() != null
                                ? place.getAddress()
                                : (place.getName() != null ? place.getName() : "");
                        placeMarker(place.getLatLng());
                        updateInfoBar();
                    }
                    // Renew session token after successful fetch
                    sessionToken = AutocompleteSessionToken.newInstance();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Could not load location details", Toast.LENGTH_SHORT).show());
    }

    // ── Reverse Geocoding (tap on map) ───────────────────────────────────

    private void reverseGeocode(LatLng latLng) {
        new Thread(() -> {
            try {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocation(
                        latLng.latitude, latLng.longitude, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address addr = addresses.get(0);
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i <= addr.getMaxAddressLineIndex(); i++) {
                        if (i > 0) sb.append(", ");
                        sb.append(addr.getAddressLine(i));
                    }
                    selectedAddress = sb.toString();
                    runOnUiThread(() -> {
                        etSearch.setText(selectedAddress);
                        etSearch.setSelection(selectedAddress.length());
                        updateInfoBar();
                    });
                }
            } catch (IOException e) {
                runOnUiThread(this::updateInfoBar);
            }
        }).start();
    }

    // ── UI helpers ───────────────────────────────────────────────────────

    private void updateInfoBar() {
        if (tvSelectedAddress == null) return;
        if (selectedLat != 0.0 && selectedLng != 0.0) {
            tvSelectedAddress.setText(
                    selectedAddress.isEmpty()
                            ? getString(R.string.location_tap_hint)
                            : "📍 " + selectedAddress);
            tvCoordinates.setText(
                    String.format(Locale.US, "%.6f, %.6f", selectedLat, selectedLng));
            tvCoordinates.setVisibility(View.VISIBLE);
        } else {
            tvSelectedAddress.setText(getString(R.string.location_tap_hint));
            tvCoordinates.setVisibility(View.GONE);
        }
    }

    private void confirmSelection() {
        if (selectedLat == 0.0 && selectedLng == 0.0) {
            Toast.makeText(this, "Please select a location first", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent result = new Intent();
        result.putExtra(EXTRA_LATITUDE,  selectedLat);
        result.putExtra(EXTRA_LONGITUDE, selectedLng);
        result.putExtra(EXTRA_ADDRESS,   selectedAddress);
        setResult(RESULT_OK, result);
        finish();
    }
}

