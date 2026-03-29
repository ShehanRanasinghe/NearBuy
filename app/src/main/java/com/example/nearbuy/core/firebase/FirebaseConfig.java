package com.example.nearbuy.core.firebase;

import com.example.nearbuy.BuildConfig;

/**
 * FirebaseConfig – central feature switch for Firebase.
 *
 * Returns true only when FIREBASE_ENABLED=true in the project's .env file.
 * All repositories and services call this before making any live Firestore /
 * Firebase Auth request, so the app degrades gracefully to stub/offline mode
 * while the .env is being set up.
 *
 * Usage:
 *   if (!FirebaseConfig.isFirebaseEnabled()) { callback.onError(...); return; }
 */
public final class FirebaseConfig {

    private FirebaseConfig() { /* utility class – no instances */ }

    /**
     * Returns {@code true} when Firebase is active.
     * Driven by {@code FIREBASE_ENABLED} in the project-root {@code .env} file,
     * which is read by Gradle at build time and injected into {@link BuildConfig}.
     */
    public static boolean isFirebaseEnabled() {
        return BuildConfig.FIREBASE_ENABLED;
    }
}

