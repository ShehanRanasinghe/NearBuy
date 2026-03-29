package com.example.nearbuy.app.startup;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
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
 * SplashScreen – first activity launched on app start.
 *
 * Startup sequence (mirrors NearBuyHQ admin app):
 *   1. initFirebase()               – manually initialise Firebase from .env BuildConfig values
 *                                     (replaces the old NearBuyApplication class)
 *   2. checkPermissionsAndProceed() – request location permission (needed for nearby search)
 *   3. checkConnectivityAndNavigate()– ensure internet is available; wait via NetworkCallback
 *   4. navigate()                   – server-side Firebase auth verify (handles deleted accounts),
 *                                     then route → DashboardActivity or WelcomeActivity
 */
public class SplashScreen extends AppCompatActivity {

    private static final String TAG                   = "NearBuy.Splash";
    private static final int    SPLASH_DELAY_MS       = 2500;
    private static final int    REQ_LOCATION_PERMISSION = 100;

    private ConnectivityManager                  connectivityManager;
    private ConnectivityManager.NetworkCallback  networkCallback;
    private boolean navigated = false; // guard – navigate() runs at most once
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Full-screen immersive splash
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

        // ── Step 1: Initialise Firebase immediately (before the splash delay)
        // Done here instead of a custom Application class so the .env → BuildConfig
        // values are used and no google-services.json is required.
        initFirebase();

        // Steps 2-4 start after the minimum splash display time
        handler.postDelayed(this::checkPermissionsAndProceed, SPLASH_DELAY_MS);
    }

    // ── Step 1 – Firebase manual initialisation ───────────────────────────────

    /**
     * Firebase is auto-initialised by the google-services plugin, which reads
     * app/google-services.json at build time and registers FirebaseInitProvider
     * in the merged AndroidManifest.  No manual FirebaseOptions.Builder is needed.
     *
     * This method exists only to:
     *   • honour the FIREBASE_ENABLED feature flag from .env
     *   • log the initialisation result so it is visible in Logcat
     *
     * Safe to call multiple times – FirebaseInitProvider runs before onCreate(),
     * so FirebaseApp.getApps() will already be non-empty by the time we arrive here.
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

    // ── Step 2 – Runtime permission check ────────────────────────────────────

    /**
     * Request every runtime permission the customer app needs before navigation.
     *
     * Permissions requested:
     *   • ACCESS_FINE_LOCATION   – required for nearby product/shop distance calculation
     *   • ACCESS_COARSE_LOCATION – fallback for lower-accuracy location
     *
     * Navigation always proceeds after the user responds, even if denied.
     * Location is only required for the nearby search feature, not for core browsing.
     */
    private void checkPermissionsAndProceed() {
        boolean fineGranted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;

        if (fineGranted) {
            // Permission already granted – skip the dialog
            checkConnectivityAndNavigate();
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
        if (requestCode == REQ_LOCATION_PERMISSION) {
            boolean granted = grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            if (!granted) {
                Toast.makeText(this,
                        "Location permission denied. " +
                        "Nearby search will use your last known location. " +
                        "You can grant it later in Settings.",
                        Toast.LENGTH_LONG).show();
            }
            // Always continue regardless of the user's choice
            checkConnectivityAndNavigate();
        }
    }

    // ── Step 3 – Connectivity check ──────────────────────────────────────────

    /**
     * Check if the device has an active, validated internet connection.
     * If yes → proceed to navigate().
     * If no  → show toast and register a NetworkCallback to proceed
     *           automatically when internet returns.
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

    // ── Step 4 – Auth check + navigation ─────────────────────────────────────

    /**
     * Verify the cached Firebase user still exists on the server (handles
     * accounts deleted via the Firebase Console), then route to the right screen.
     *
     * reload() makes a live network call:
     *   • Success → user is valid → go to DashboardActivity
     *   • FirebaseAuthInvalidUserException → account deleted/disabled → sign out → WelcomeActivity
     *   • Any other failure → treat as logged-out → WelcomeActivity
     *
     * When Firebase is disabled (FIREBASE_ENABLED=false in .env), falls back to
     * SessionManager only so stub/offline mode still works.
     */
    private void navigate() {
        if (navigated) return;

        // If Firebase failed to initialise (e.g. missing keys), go straight to Welcome
        // to avoid an IllegalStateException from FirebaseAuth.getInstance().
        if (FirebaseApp.getApps(this).isEmpty()) {
            Log.w(TAG, "FirebaseApp not initialised – routing to WelcomeActivity.");
            navigated = true;
            goTo(WelcomeActivity.class);
            return;
        }

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        if (firebaseUser == null) {
            // No cached user → straight to Welcome
            navigated = true;
            goTo(WelcomeActivity.class);
            return;
        }

        // Server-side verification: catches deleted / disabled accounts
        firebaseUser.reload()
                .addOnSuccessListener(unused -> {
                    navigated = true;
                    goTo(DashboardActivity.class);
                })
                .addOnFailureListener(e -> {
                    if (e instanceof FirebaseAuthInvalidUserException) {
                        // Account deleted or disabled → clear local auth cache
                        FirebaseAuth.getInstance().signOut();
                    }
                    navigated = true;
                    goTo(WelcomeActivity.class);
                });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Launch an activity and clear the entire back stack. */
    private void goTo(Class<?> activityClass) {
        Intent intent = new Intent(this, activityClass);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /** Returns true when the device has a working, validated internet connection. */
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
        if (networkCallback != null && connectivityManager != null) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            } catch (IllegalArgumentException ignored) {}
        }
        handler.removeCallbacksAndMessages(null);
    }
}



