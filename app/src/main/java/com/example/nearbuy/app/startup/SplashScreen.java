package com.example.nearbuy.app.startup;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.nearbuy.BuildConfig;
import com.example.nearbuy.R;
import com.example.nearbuy.dashboard.DashboardActivity;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;

/**
 * SplashScreen – first activity launched on every app start.
 *
 * Startup sequence:
 *
 *   1. initFirebase()
 *        Confirms the google-services plugin auto-initialised Firebase and
 *        logs the result.  No manual FirebaseOptions.Builder needed.
 *
 *   2. checkPermissionsAndProceed()
 *        Requests runtime permissions in order:
 *          a. POST_NOTIFICATIONS (Android 13 / API 33+)  – push deal alerts
 *          b. ACCESS_FINE_LOCATION + ACCESS_COARSE_LOCATION – nearby search
 *        Navigation always continues even if the user denies either permission.
 *
 *   3. checkConnectivityAndNavigate()
 *        Ensures the device has an active, validated internet connection before
 *        proceeding.  Registers a NetworkCallback if offline and waits.
 *
 *   4. navigate()
 *        Performs a server-side Firebase Auth verification (firebaseUser.reload())
 *        to catch deleted / suspended accounts.
 *          • Valid session  → DashboardActivity  (login screens skipped)
 *          • No / invalid session → WelcomeActivity (login required)
 */
public class SplashScreen extends AppCompatActivity {

    private static final String TAG = "NearBuy.Splash";

    // Minimum time the splash screen is visible before any navigation
    private static final int SPLASH_DELAY_MS = 2500;

    // Request codes for runtime permissions
    private static final int REQ_NOTIFICATIONS_PERMISSION = 101;
    private static final int REQ_LOCATION_PERMISSION      = 100;

    private ConnectivityManager                 connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private boolean navigated = false; // guard – navigate() fires at most once
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Full-screen immersive splash – hides status and navigation bars
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        setContentView(R.layout.activity_splash_screen);

        connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        // Step 1 – confirm Firebase is initialised
        initFirebase();

        // Steps 2–4 begin after the minimum splash display time
        handler.postDelayed(this::checkPermissionsAndProceed, SPLASH_DELAY_MS);
    }

    // ── Step 1 – Firebase confirmation ────────────────────────────────────────

    /**
     * Logs whether the google-services plugin successfully auto-initialised
     * Firebase from  google-services.json.  No manual Options.Builder needed.
     *
     * If the app was built with  FIREBASE_ENABLED=false  in .env, it will
     * run in stub / offline mode and skip all live Firebase calls.
     */
    private void initFirebase() {
        if (!BuildConfig.FIREBASE_ENABLED) {
            Log.d(TAG, "Firebase disabled (FIREBASE_ENABLED=false in .env). Running in stub mode.");
            return;
        }

        if (!FirebaseApp.getApps(this).isEmpty()) {
            Log.d(TAG, "Firebase auto-initialised from google-services.json ✓");
        } else {
            Log.e(TAG, "Firebase auto-initialisation failed – check google-services.json.");
        }
    }

    // ── Step 2 – Permission checks ────────────────────────────────────────────

    /**
     * Requests runtime permissions in the correct order:
     *   1. POST_NOTIFICATIONS (Android 13+ / API 33+) – required to show push
     *      deal alerts on newer Android versions.
     *   2. ACCESS_FINE_LOCATION – required for nearby shop / product search.
     *
     * Navigation always continues even if the user denies either permission.
     * Features that need each permission degrade gracefully:
     *   - No notification permission → push alerts are silently dropped by Android.
     *   - No location permission → search uses the last saved GPS fix or Colombo centre.
     */
    private void checkPermissionsAndProceed() {
        // On Android 13+ (API 33), POST_NOTIFICATIONS requires an explicit runtime grant
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            boolean notifGranted = ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED;

            if (!notifGranted) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQ_NOTIFICATIONS_PERMISSION);
                return; // Wait for the result before proceeding to location
            }
        }

        // Notification permission is either already granted or not needed
        checkLocationPermission();
    }

    /** Requests ACCESS_FINE_LOCATION and ACCESS_COARSE_LOCATION. */
    private void checkLocationPermission() {
        boolean fineGranted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;

        if (fineGranted) {
            checkConnectivityAndNavigate(); // Already granted – proceed
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    REQ_LOCATION_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQ_NOTIFICATIONS_PERMISSION) {
            // POST_NOTIFICATIONS result – log outcome and move on to location
            boolean granted = grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            Log.d(TAG, "POST_NOTIFICATIONS permission "
                    + (granted ? "granted." : "denied (notifications will be silent)."));
            // Continue to location permission regardless
            checkLocationPermission();

        } else if (requestCode == REQ_LOCATION_PERMISSION) {
            // Location result – log outcome and proceed to connectivity check
            boolean granted = grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            if (!granted) {
                Toast.makeText(this,
                        "Location permission denied. Nearby search will use a default location.",
                        Toast.LENGTH_LONG).show();
            }
            checkConnectivityAndNavigate();
        }
    }

    // ── Step 3 – Connectivity check ───────────────────────────────────────────

    /**
     * Verifies that the device has an active, validated internet connection.
     * If connected → proceed to navigate().
     * If offline  → show a Toast and register a NetworkCallback to resume
     *               automatically when the connection is restored.
     */
    private void checkConnectivityAndNavigate() {
        if (isNetworkAvailable()) {
            navigate();
        } else {
            Toast.makeText(this,
                    "No internet connection. Please connect to continue.",
                    Toast.LENGTH_LONG).show();

            networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(@NonNull Network network) {
                    handler.post(() -> {
                        if (!navigated) {
                            Toast.makeText(SplashScreen.this,
                                    "Connected! Loading…", Toast.LENGTH_SHORT).show();
                            navigate();
                        }
                    });
                }
            };

            NetworkRequest request = new NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build();
            connectivityManager.registerNetworkCallback(request, networkCallback);
        }
    }

    // ── Step 4 – Auth check + routing ────────────────────────────────────────

    /**
     * Performs a server-side Firebase Auth check to verify the cached user
     * still exists (handles accounts deleted via the Firebase Console), then
     * routes to the appropriate screen.
     *
     *   firebaseUser.reload() succeeds  → session is valid → DashboardActivity
     *   FirebaseAuthInvalidUserException → account deleted/disabled → sign out → WelcomeActivity
     *   Any other reload failure         → treat as logged-out → WelcomeActivity
     *   No cached Firebase user          → WelcomeActivity (login required)
     */
    private void navigate() {
        if (navigated) return;

        // If Firebase failed to initialise (e.g. missing google-services.json), go to Welcome
        if (FirebaseApp.getApps(this).isEmpty()) {
            Log.w(TAG, "FirebaseApp not initialised – routing to WelcomeActivity.");
            navigated = true;
            goTo(WelcomeActivity.class);
            return;
        }

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        if (firebaseUser == null) {
            // No cached credentials → show login / welcome flow
            navigated = true;
            goTo(WelcomeActivity.class);
            return;
        }

        // Server-side reload verifies the account still exists and is not disabled
        firebaseUser.reload()
                .addOnSuccessListener(unused -> {
                    navigated = true;
                    Log.d(TAG, "Session valid – routing to DashboardActivity.");
                    goTo(DashboardActivity.class);
                })
                .addOnFailureListener(e -> {
                    if (e instanceof FirebaseAuthInvalidUserException) {
                        // Account deleted or disabled in Firebase Console → force sign out
                        Log.w(TAG, "Account disabled/deleted – signing out.");
                        FirebaseAuth.getInstance().signOut();
                    }
                    navigated = true;
                    goTo(WelcomeActivity.class);
                });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Launches an activity and clears the entire back stack so the user
     * cannot navigate back to the splash screen.
     */
    private void goTo(Class<?> activityClass) {
        Intent intent = new Intent(this, activityClass);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Returns true when the device has a working, validated internet connection.
     * Checks both connectivity and capability validation so VPN/captive-portal
     * scenarios are handled correctly.
     */
    private boolean isNetworkAvailable() {
        if (connectivityManager == null) return false;
        Network activeNetwork = connectivityManager.getActiveNetwork();
        if (activeNetwork == null) return false;
        NetworkCapabilities caps =
                connectivityManager.getNetworkCapabilities(activeNetwork);
        return caps != null
                && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unregister the connectivity callback to prevent memory leaks
        if (networkCallback != null && connectivityManager != null) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            } catch (IllegalArgumentException ignored) {}
        }
        handler.removeCallbacksAndMessages(null);
    }
}

