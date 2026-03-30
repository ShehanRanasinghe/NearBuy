package com.example.nearbuy.app;

import android.app.Application;
import android.util.Log;

import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory;

/**
 * NearBuyApp – custom Application class.
 *
 * Responsibilities:
 *   • Initialises Firebase App Check so that Firestore / Auth requests carry
 *     a valid attestation token instead of the placeholder that causes
 *     PERMISSION_DENIED when App Check enforcement is enabled on the project.
 *
 * App Check provider strategy:
 *   • Debug builds  → DebugAppCheckProviderFactory  (works on emulators & dev devices)
 *   • Release builds → swap DebugAppCheckProviderFactory for
 *                       PlayIntegrityAppCheckProviderFactory once the app is
 *                       published to the Play Store.
 *
 * Registered in AndroidManifest.xml via  android:name=".app.NearBuyApp".
 */
public class NearBuyApp extends Application {

    private static final String TAG = "NearBuy.App";

    @Override
    public void onCreate() {
        super.onCreate();
        initAppCheck();
    }

    /**
     * Installs the Firebase App Check debug provider.
     *
     * The debug provider generates a local token that is accepted by the
     * Firebase back-end when App Check enforcement is in "debug" mode for
     * the project.  It prevents the warning:
     *   "Error getting App Check token; using placeholder token instead.
     *    Error: No AppCheckProvider installed."
     *
     * For production replace DebugAppCheckProviderFactory with
     * PlayIntegrityAppCheckProviderFactory (add
     * firebase-appcheck-playintegrity to build.gradle dependencies).
     */
    private void initAppCheck() {
        try {
            FirebaseAppCheck appCheck = FirebaseAppCheck.getInstance();
            appCheck.installAppCheckProviderFactory(
                    DebugAppCheckProviderFactory.getInstance());
            Log.d(TAG, "Firebase App Check initialised (debug provider).");
        } catch (Exception e) {
            Log.w(TAG, "Firebase App Check initialisation failed – non-fatal.", e);
        }
    }
}

