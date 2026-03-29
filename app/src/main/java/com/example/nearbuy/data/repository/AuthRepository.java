package com.example.nearbuy.data.repository;

import android.content.Context;

import com.example.nearbuy.core.SessionManager;
import com.example.nearbuy.core.firebase.FirebaseConfig;
import com.example.nearbuy.data.model.Customer;
import com.example.nearbuy.data.remote.firebase.FirebaseCollections;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * AuthRepository – handles Firebase Authentication for the NearBuy customer app.
 *
 * Responsibilities:
 *   • Register a new customer (Firebase Auth + Firestore profile document).
 *   • Login with email/password → save session.
 *   • Logout → clear session.
 *   • Password reset via email.
 *   • Check whether a session is already active (used by SplashScreen).
 *
 * All public methods are stubs at this stage – no backend logic is wired yet.
 * The Firebase objects are obtained and ready; backend calls will be added in
 * a subsequent implementation phase.
 */
public class AuthRepository {

    private final FirebaseAuth      auth;
    private final FirebaseFirestore firestore;

    public AuthRepository() {
        this.auth      = FirebaseAuth.getInstance();
        this.firestore = FirebaseFirestore.getInstance();
    }

    // ── Session check ─────────────────────────────────────────────────────────

    /**
     * Returns true when a Firebase user is currently signed in AND their UID
     * matches the one saved in SessionManager (both checks must pass).
     *
     * Called by SplashScreen to decide whether to skip the login flow.
     */
    public boolean isSessionActive(Context context) {
        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser == null) return false;
        SessionManager session = SessionManager.getInstance(context);
        return session.isLoggedIn()
                && firebaseUser.getUid().equals(session.getUserId());
    }

    // ── Register ──────────────────────────────────────────────────────────────

    /**
     * Creates a new Firebase Auth account and writes the customer profile to
     * Firestore at  NearBuy/{uid}.
     *
     * @param fullName  customer's display name
     * @param email     registration email
     * @param phone     contact number
     * @param password  chosen password
     * @param context   needed to initialise SessionManager after success
     * @param callback  OperationCallback – onSuccess() or onError(e)
     */
    public void register(String fullName, String email, String phone,
                         String password, Context context,
                         OperationCallback callback) {
        if (!FirebaseConfig.isFirebaseEnabled()) {
            callback.onError(new IllegalStateException(
                    "Firebase not enabled. Fill in your .env file and set FIREBASE_ENABLED=true."));
            return;
        }
        // TODO: implement in the auth backend phase
        callback.onError(new UnsupportedOperationException("register() – not yet implemented"));
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    /**
     * Signs in the customer with their email and password, then loads their
     * Firestore profile into SessionManager.
     *
     * @param email    login email
     * @param password login password
     * @param context  needed to save SessionManager after success
     * @param callback OperationCallback
     */
    public void login(String email, String password,
                      Context context, OperationCallback callback) {
        if (!FirebaseConfig.isFirebaseEnabled()) {
            callback.onError(new IllegalStateException(
                    "Firebase not enabled. Fill in your .env file and set FIREBASE_ENABLED=true."));
            return;
        }
        // TODO: implement in the auth backend phase
        callback.onError(new UnsupportedOperationException("login() – not yet implemented"));
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    /**
     * Signs out of Firebase Auth and clears the local SessionManager cache.
     *
     * @param context needed to access SessionManager
     */
    public void logout(Context context) {
        auth.signOut();
        SessionManager.getInstance(context).clearSession();
    }

    // ── Password reset ────────────────────────────────────────────────────────

    /**
     * Sends a Firebase password-reset email to the given address.
     *
     * @param email    the customer's registered email
     * @param callback OperationCallback
     */
    public void sendPasswordReset(String email, OperationCallback callback) {
        if (!FirebaseConfig.isFirebaseEnabled()) {
            callback.onError(new IllegalStateException(
                    "Firebase not enabled. Fill in your .env file and set FIREBASE_ENABLED=true."));
            return;
        }
        // TODO: implement in the auth backend phase
        callback.onError(new UnsupportedOperationException("sendPasswordReset() – not yet implemented"));
    }

    // ── Profile load ──────────────────────────────────────────────────────────

    /**
     * Loads the customer's Firestore profile document and caches it in SessionManager.
     *
     * @param uid      Firebase Auth UID
     * @param context  needed to update SessionManager
     * @param callback DataCallback<Customer>
     */
    public void loadCustomerProfile(String uid, Context context,
                                    DataCallback<Customer> callback) {
        if (!FirebaseConfig.isFirebaseEnabled()) {
            callback.onError(new IllegalStateException(
                    "Firebase not enabled. Fill in your .env file and set FIREBASE_ENABLED=true."));
            return;
        }
        // TODO: implement in the auth backend phase
        callback.onError(new UnsupportedOperationException("loadCustomerProfile() – not yet implemented"));
    }
}

