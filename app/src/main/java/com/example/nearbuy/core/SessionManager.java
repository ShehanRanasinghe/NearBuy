package com.example.nearbuy.core;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * SessionManager – Singleton that persists customer session data in SharedPreferences.
 *
 * After a successful login the customer's UID, display name, email, phone and
 * location preferences are cached here so every screen can read them instantly
 * without hitting Firestore on every load.
 *
 * Usage (anywhere in the app):
 *   SessionManager session = SessionManager.getInstance(context);
 *   String name = session.getUserName();
 */
public class SessionManager {

    // SharedPreferences file name
    private static final String PREFS_NAME = "nearbuy_session";

    // ── Preference keys ───────────────────────────────────────────────────────
    private static final String KEY_USER_ID        = "userId";
    private static final String KEY_USER_NAME      = "userName";
    private static final String KEY_USER_EMAIL     = "userEmail";
    private static final String KEY_USER_PHONE     = "userPhone";
    private static final String KEY_USER_AVATAR    = "userAvatarInitial";

    // Location
    private static final String KEY_LAST_LATITUDE  = "lastLatitude";
    private static final String KEY_LAST_LONGITUDE = "lastLongitude";

    // Search preference – radius in kilometres (default 2 km)
    private static final String KEY_SEARCH_RADIUS  = "searchRadiusKm";
    private static final float  DEFAULT_RADIUS_KM  = 2.0f;

    // ── Singleton ──────────────────────────────────────────────────────────────
    private static SessionManager instance;
    private final SharedPreferences prefs;

    private SessionManager(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /** Returns the single SessionManager instance. Thread-safe via double-checked locking. */
    public static SessionManager getInstance(Context context) {
        if (instance == null) {
            synchronized (SessionManager.class) {
                if (instance == null) {
                    instance = new SessionManager(context);
                }
            }
        }
        return instance;
    }

    // ── User ID ───────────────────────────────────────────────────────────────

    /** Save the Firebase Auth UID after successful login / registration. */
    public void saveUserId(String uid) {
        prefs.edit().putString(KEY_USER_ID, uid).apply();
    }

    /** Returns the Firebase Auth UID of the currently signed-in customer. */
    public String getUserId() {
        return prefs.getString(KEY_USER_ID, "");
    }

    // ── User Name ─────────────────────────────────────────────────────────────

    public void saveUserName(String name) {
        prefs.edit()
                .putString(KEY_USER_NAME, name)
                .putString(KEY_USER_AVATAR, name != null && !name.isEmpty()
                        ? String.valueOf(name.charAt(0)).toUpperCase() : "?")
                .apply();
    }

    /** Returns the customer's display name (defaults to "Customer"). */
    public String getUserName() {
        return prefs.getString(KEY_USER_NAME, "Customer");
    }

    /** Returns the first letter of the customer's name, used for avatar circle. */
    public String getAvatarInitial() {
        return prefs.getString(KEY_USER_AVATAR, "?");
    }

    // ── User Email ────────────────────────────────────────────────────────────

    public void saveUserEmail(String email) {
        prefs.edit().putString(KEY_USER_EMAIL, email).apply();
    }

    public String getUserEmail() {
        return prefs.getString(KEY_USER_EMAIL, "");
    }

    // ── User Phone ────────────────────────────────────────────────────────────

    public void saveUserPhone(String phone) {
        prefs.edit().putString(KEY_USER_PHONE, phone).apply();
    }

    public String getUserPhone() {
        return prefs.getString(KEY_USER_PHONE, "");
    }

    // ── Location ──────────────────────────────────────────────────────────────

    /** Save the customer's last known GPS coordinates. */
    public void saveLastLocation(double latitude, double longitude) {
        prefs.edit()
                .putFloat(KEY_LAST_LATITUDE,  (float) latitude)
                .putFloat(KEY_LAST_LONGITUDE, (float) longitude)
                .apply();
    }

    /** Returns the customer's last saved latitude (0.0 if never set). */
    public double getLastLatitude() {
        return prefs.getFloat(KEY_LAST_LATITUDE, 0f);
    }

    /** Returns the customer's last saved longitude (0.0 if never set). */
    public double getLastLongitude() {
        return prefs.getFloat(KEY_LAST_LONGITUDE, 0f);
    }

    /** Returns true when at least one location fix has been saved. */
    public boolean hasLocation() {
        return getLastLatitude() != 0.0 || getLastLongitude() != 0.0;
    }

    // ── Search Radius ─────────────────────────────────────────────────────────

    /**
     * Save the customer's preferred search radius in kilometres.
     * Shown in SearchActivity as a filter (1 km, 2 km, 3 km, 5 km, 10 km).
     */
    public void saveSearchRadius(float radiusKm) {
        prefs.edit().putFloat(KEY_SEARCH_RADIUS, radiusKm).apply();
    }

    /** Returns the customer's preferred search radius (default 2.0 km). */
    public float getSearchRadius() {
        return prefs.getFloat(KEY_SEARCH_RADIUS, DEFAULT_RADIUS_KM);
    }

    // ── Session control ───────────────────────────────────────────────────────

    /** Returns true when a UID is stored in the session. */
    public boolean isLoggedIn() {
        String uid = getUserId();
        return uid != null && !uid.trim().isEmpty();
    }

    /** Wipe all session data – call on logout. */
    public void clearSession() {
        prefs.edit().clear().apply();
    }
}

