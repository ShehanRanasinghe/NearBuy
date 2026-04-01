package com.example.nearbuy.data.repository;

import android.content.Context;
import android.util.Log;

import com.example.nearbuy.core.EmailService;
import com.example.nearbuy.core.SessionManager;
import com.example.nearbuy.core.firebase.FirebaseConfig;
import com.example.nearbuy.data.model.Customer;
import com.example.nearbuy.data.remote.firebase.FirebaseCollections;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * AuthRepository – central authority for all authentication operations.
 *
 * Registration flow (2-step):
 *   Step 1 → initiateRegistration()  : generates a 6-digit OTP, stores it in
 *                                       Firestore at  otp_codes/{phone}, returns
 *                                       the code via DataCallback<String> so the
 *                                       caller can forward it (SMS / toast in dev).
 *   Step 2 → verifyOtpAndRegister()  : verifies the entered code against Firestore,
 *                                       creates the Firebase Auth account, writes the
 *                                       Customer profile document, and saves the session.
 *
 * Login flow:
 *   login()  → Firebase Auth signIn → load Firestore profile → save SessionManager
 *
 * Logout flow:
 *   logout() → Firebase Auth signOut → clear SessionManager
 *
 * Password reset:
 *   sendPasswordReset() → Firebase Auth sends reset email
 *
 * Profile:
 *   loadCustomerProfile() → reads Firestore → caches in SessionManager
 *   updateProfile()        → writes Firestore → refreshes SessionManager
 */
public class AuthRepository {

    private static final String TAG = "NearBuy.AuthRepo";

    private final FirebaseAuth      auth;
    private final FirebaseFirestore firestore;

    public AuthRepository() {
        this.auth      = FirebaseAuth.getInstance();
        this.firestore = FirebaseFirestore.getInstance();
    }

    // ── Session check ──────────────────────────────────────────────────────────

    /**
     * Returns true when BOTH Firebase Auth and SessionManager agree a user is
     * signed in and their UIDs match.  Called by SplashScreen to skip the login flow.
     */
    public boolean isSessionActive(Context context) {
        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser == null) return false;
        SessionManager session = SessionManager.getInstance(context);
        return session.isLoggedIn()
                && firebaseUser.getUid().equals(session.getUserId());
    }

    // ── Step 1: Generate and store OTP ────────────────────────────────────────

    /**
     * Generates a 6-digit OTP, stores it in Firestore under otp_codes/{phone}
     * with a 10-minute expiry, sends the code to the customer's email via Gmail
     * SMTP, and returns the code via callback.
     *
     * The callback fires immediately after the Firestore write succeeds — it does
     * NOT wait for SMTP delivery so the UI moves to the OTP screen right away.
     * The email is sent in the background by EmailService.
     *
     * @param phone    phone number – used as the Firestore OTP document ID
     * @param email    customer's email address – OTP is delivered here
     * @param name     customer's display name – personalises the email greeting
     * @param callback DataCallback – onSuccess(code) or onError(e)
     */
    public void initiateRegistration(String phone, String email, String name,
                                     DataCallback<String> callback) {
        if (!FirebaseConfig.isFirebaseEnabled()) {
            callback.onError(new IllegalStateException(
                    "Firebase is not enabled. Set FIREBASE_ENABLED=true in .env"));
            return;
        }

        // Generate a zero-padded 6-digit OTP
        String code      = String.format("%06d", new Random().nextInt(1_000_000));
        long   expiresAt = System.currentTimeMillis() + 10L * 60 * 1000; // 10 minutes

        Map<String, Object> data = new HashMap<>();
        data.put("code",      code);
        data.put("phone",     phone);
        data.put("expiresAt", expiresAt);
        data.put("createdAt", System.currentTimeMillis());

        // Write OTP document to Firestore (overwrites any previous request for this email)
        firestore.collection(FirebaseCollections.OTP_CODES)
                .document(email)
                .set(data)
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "OTP stored in Firestore for phone: " + phone);

                    // Send OTP to the customer's email in a background thread.
                    // The UI callback fires immediately — does not wait for SMTP.
                    EmailService.sendOtpEmail(email, name, code, new EmailService.EmailCallback() {
                        @Override public void onSuccess() {
                            Log.d(TAG, "OTP email delivered to: " + email);
                        }
                        @Override public void onError(Exception e) {
                            // Email failed but code is still valid in Firestore.
                            // User can read it from the on-screen AlertDialog.
                            Log.w(TAG, "OTP email failed – code still valid in Firestore: " + email, e);
                        }
                    });

                    callback.onSuccess(code); // return code to caller for on-screen display
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to store OTP for phone: " + phone, e);
                    callback.onError(e);
                });
    }

    // ── Step 2: Verify OTP and create Firebase account ────────────────────────

    /**
     * Verifies the OTP entered by the user against the Firestore document, then
     * on success creates the Firebase Auth account and the customer profile.
     *
     * The OTP document is deleted after a successful verification (one-time use).
     *
     * @param fullName    customer's display name
     * @param email       registration email
     * @param phone       phone number (used to look up the OTP document)
     * @param password    chosen password
     * @param enteredCode 6-digit code typed by the user
     * @param context     needed to initialise SessionManager
     * @param callback    OperationCallback – onSuccess() or onError(e)
     */
    public void verifyOtpAndRegister(String fullName, String email,
                                     String phone, String password,
                                     String enteredCode, Context context,
                                     OperationCallback callback) {
        if (!FirebaseConfig.isFirebaseEnabled()) {
            callback.onError(new IllegalStateException("Firebase is not enabled."));
            return;
        }

        // Read the stored OTP document
        firestore.collection(FirebaseCollections.OTP_CODES)
                .document(email)
                .get()
                .addOnSuccessListener(doc -> {

                    if (!doc.exists()) {
                        callback.onError(new Exception(
                                "OTP not found. Please request a new code."));
                        return;
                    }

                    String storedCode = doc.getString("code");
                    Long   expiresAt  = doc.getLong("expiresAt");

                    // Check if OTP has expired
                    if (expiresAt != null && System.currentTimeMillis() > expiresAt) {
                        doc.getReference().delete(); // clean up expired document
                        callback.onError(new Exception(
                                "OTP expired. Please request a new code."));
                        return;
                    }

                    // Check if code matches
                    if (!enteredCode.equals(storedCode)) {
                        callback.onError(new Exception(
                                "Incorrect OTP. Please try again."));
                        return;
                    }

                    // OTP is valid → proceed to create account.
                    // The OTP document is deleted only after the Firestore profile write
                    // succeeds so the user can retry if an intermediate step fails.
                    createFirebaseAccount(fullName, email, phone, password, context,
                            doc.getReference(), callback);
                })
                .addOnFailureListener(callback::onError);
    }

    /**
     * Creates the Firebase Auth user and writes the customer Firestore document.
     * Called only after OTP verification passes.  If the Firestore write fails,
     * the Auth account is also deleted to keep both systems consistent.
     *
     * The OTP document ({@code otpRef}) is deleted here – AFTER the Firestore
     * profile write succeeds – so that the user can retry the entire flow if any
     * intermediate step fails without needing to request a new code.
     */
    private void createFirebaseAccount(String fullName, String email,
                                       String phone, String password,
                                       Context context,
                                       DocumentReference otpRef,
                                       OperationCallback callback) {

        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    FirebaseUser user = result.getUser();
                    if (user == null) {
                        callback.onError(new Exception("Account creation failed – null user."));
                        return;
                    }

                    String uid = user.getUid();
                    Customer customer = new Customer(uid, fullName, email, phone);

                    // Write the customer profile to Firestore at NearBuy/{uid}
                    firestore.collection(FirebaseCollections.CUSTOMERS)
                            .document(uid)
                            .set(customer.toMap())
                            .addOnSuccessListener(unused -> {
                                // Everything succeeded – now safe to burn the OTP (one-time use)
                                otpRef.delete();
                                // Cache all session data locally
                                SessionManager session = SessionManager.getInstance(context);
                                session.saveUserId(uid);
                                session.saveUserName(fullName);
                                session.saveUserEmail(email);
                                session.saveUserPhone(phone);
                                // Sync the FCM device token so push notifications reach
                                // this device as soon as the account is created
                                syncFcmToken(uid, context);
                                Log.d(TAG, "Registration complete — UID: " + uid);
                                callback.onSuccess();
                            })
                            .addOnFailureListener(e -> {
                                // Roll back Auth account so the user can retry cleanly.
                                // The OTP document is intentionally NOT deleted here so the
                                // user can re-submit the same code after fixing the issue.
                                user.delete();
                                Log.e(TAG, "Firestore write failed; Auth account deleted", e);
                                callback.onError(e);
                            });
                })
                .addOnFailureListener(callback::onError);
    }

    // ── Login ──────────────────────────────────────────────────────────────────

    /**
     * Signs in the customer with email/password, loads their Firestore profile,
     * and caches everything in SessionManager.
     *
     * If the Firestore profile load fails (e.g. document missing), login still
     * succeeds using the minimal data available from the Firebase Auth object,
     * so the customer is never locked out due to a profile document issue.
     *
     * @param email    login email
     * @param password login password
     * @param context  needed to save SessionManager
     * @param callback OperationCallback
     */
    public void login(String email, String password,
                      Context context, OperationCallback callback) {
        if (!FirebaseConfig.isFirebaseEnabled()) {
            callback.onError(new IllegalStateException("Firebase is not enabled."));
            return;
        }

        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    FirebaseUser user = result.getUser();
                    if (user == null) {
                        callback.onError(new Exception("Login failed – null user."));
                        return;
                    }

                    // Load the full profile and cache it in SessionManager
                    loadCustomerProfile(user.getUid(), context, new DataCallback<Customer>() {
                        @Override
                        public void onSuccess(Customer customer) {
                            // Sync the FCM token to Firestore so targeted push notifications
                            // reach this device.  Uses the token saved by NearBuyMessagingService.
                            syncFcmToken(user.getUid(), context);
                            Log.d(TAG, "Login complete — UID: " + user.getUid());
                            callback.onSuccess();
                        }

                        @Override
                        public void onError(Exception e) {
                            // Profile missing → save minimal session from Auth object
                            SessionManager session = SessionManager.getInstance(context);
                            session.saveUserId(user.getUid());
                            if (user.getDisplayName() != null)
                                session.saveUserName(user.getDisplayName());
                            if (user.getEmail() != null)
                                session.saveUserEmail(user.getEmail());
                            // Still try to sync FCM even with minimal profile
                            syncFcmToken(user.getUid(), context);
                            Log.w(TAG, "Profile load failed after login; using Auth info", e);
                            callback.onSuccess(); // Let the customer in with minimal session
                        }
                    });
                })
                .addOnFailureListener(callback::onError);
    }

    // ── Logout ─────────────────────────────────────────────────────────────────

    /**
     * Signs out of Firebase Auth and wipes the local SessionManager cache.
     * Always call this instead of FirebaseAuth.signOut() directly so both
     * systems stay in sync.
     *
     * @param context needed to access SessionManager
     */
    public void logout(Context context) {
        auth.signOut();
        SessionManager.getInstance(context).clearSession();
        Log.d(TAG, "User logged out and session cleared.");
    }

    // ── Password reset ─────────────────────────────────────────────────────────

    /**
     * Sends a Firebase password-reset email to the provided address.
     * Firebase will reject the request if the email is not registered.
     *
     * @param email    the customer's registered email address
     * @param callback OperationCallback – onSuccess() or onError(e)
     */
    public void sendPasswordReset(String email, OperationCallback callback) {
        if (!FirebaseConfig.isFirebaseEnabled()) {
            callback.onError(new IllegalStateException("Firebase is not enabled."));
            return;
        }

        auth.sendPasswordResetEmail(email)
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "Password reset email sent to: " + email);
                    callback.onSuccess();
                })
                .addOnFailureListener(callback::onError);
    }

    // ── Profile load ───────────────────────────────────────────────────────────

    /**
     * Reads the customer's Firestore document at  NearBuy/{uid}  and caches all
     * fields in SessionManager for fast offline-ready access throughout the app.
     *
     * @param uid      Firebase Auth UID
     * @param context  needed to update SessionManager
     * @param callback DataCallback<Customer>
     */
    public void loadCustomerProfile(String uid, Context context,
                                    DataCallback<Customer> callback) {
        if (!FirebaseConfig.isFirebaseEnabled()) {
            callback.onError(new IllegalStateException("Firebase is not enabled."));
            return;
        }

        firestore.collection(FirebaseCollections.CUSTOMERS)
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        callback.onError(new Exception("Customer profile not found in Firestore."));
                        return;
                    }

                    Customer customer = Customer.fromMap(uid, doc.getData());
                    if (customer == null) {
                        callback.onError(new Exception("Failed to parse customer profile."));
                        return;
                    }

                    // Refresh the local SessionManager cache with the latest Firestore data
                    SessionManager session = SessionManager.getInstance(context);
                    session.saveUserId(uid);
                    session.saveUserName(customer.getName());
                    session.saveUserEmail(customer.getEmail());
                    session.saveUserPhone(customer.getPhone());

                    // Cache location address if present in the Firestore document
                    String address = doc.getString("address");
                    if (address != null && !address.isEmpty()) {
                        session.saveLocationAddress(address);
                    }
                    // Cache lat/lng if present
                    Object latObj = doc.get("latitude");
                    Object lngObj = doc.get("longitude");
                    if (latObj instanceof Number && lngObj instanceof Number) {
                        session.saveLastLocation(
                                ((Number) latObj).doubleValue(),
                                ((Number) lngObj).doubleValue());
                    }

                    Log.d(TAG, "Profile loaded for UID: " + uid);
                    callback.onSuccess(customer);
                })
                .addOnFailureListener(callback::onError);
    }

    // ── Profile update ─────────────────────────────────────────────────────────

    /**
     * Updates the customer's editable fields (name, email, phone) in Firestore
     * and refreshes the SessionManager cache.
     *
     * @param uid      Firebase Auth UID of the signed-in customer
     * @param newName  updated display name
     * @param newEmail updated email address
     * @param newPhone updated phone number
     * @param context  needed to refresh SessionManager
     * @param callback OperationCallback
     */
    public void updateProfile(String uid,
                              String newName, String newEmail, String newPhone,
                              Context context, OperationCallback callback) {
        if (!FirebaseConfig.isFirebaseEnabled()) {
            callback.onError(new IllegalStateException("Firebase is not enabled."));
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("name",      newName);
        updates.put("email",     newEmail);
        updates.put("phone",     newPhone);
        updates.put("updatedAt", System.currentTimeMillis());

        firestore.collection(FirebaseCollections.CUSTOMERS)
                .document(uid)
                .update(updates)
                .addOnSuccessListener(unused -> {
                    // Keep the local cache in sync with Firestore
                    SessionManager session = SessionManager.getInstance(context);
                    session.saveUserName(newName);
                    session.saveUserEmail(newEmail);
                    session.saveUserPhone(newPhone);
                    Log.d(TAG, "Profile updated for UID: " + uid);
                    callback.onSuccess();
                })
                .addOnFailureListener(callback::onError);
    }

    // ── Location update ────────────────────────────────────────────────────────

    /**
     * Updates the customer's location (latitude, longitude, address) in Firestore
     * and refreshes the SessionManager cache.
     *
     * @param uid      Firebase Auth UID of the signed-in customer
     * @param lat      new latitude
     * @param lng      new longitude
     * @param address  reverse-geocoded address string (may be empty)
     * @param callback OperationCallback
     */
    public void updateUserLocation(String uid, double lat, double lng,
                                   String address, OperationCallback callback) {
        if (!FirebaseConfig.isFirebaseEnabled()) {
            callback.onError(new IllegalStateException("Firebase is not enabled."));
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("latitude",  lat);
        updates.put("longitude", lng);
        updates.put("address",   address);
        updates.put("updatedAt", System.currentTimeMillis());

        firestore.collection(FirebaseCollections.CUSTOMERS)
                .document(uid)
                .update(updates)
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "Location updated for UID: " + uid);
                    callback.onSuccess();
                })
                .addOnFailureListener(callback::onError);
    }

    // ── FCM token sync ─────────────────────────────────────────────────────────

    /**
     * Writes the customer's current FCM device registration token to their
     * Firestore profile so NearBuyHQ can send targeted push notifications.
     *
     * The token is retrieved from SessionManager, where it was saved by
     * {@link com.example.nearbuy.core.NearBuyMessagingService#onNewToken}.
     * If no token has been saved yet (FCM not yet initialised), this is a no-op.
     *
     * @param uid     Firebase Auth UID of the signed-in customer
     * @param context needed to access SessionManager
     */
    private void syncFcmToken(String uid, Context context) {
        String token = SessionManager.getInstance(context).getFcmToken();
        if (token == null || token.isEmpty()) return; // FCM token not yet available

        Map<String, Object> update = new HashMap<>();
        update.put("fcmToken",       token);
        update.put("tokenUpdatedAt", System.currentTimeMillis());

        firestore.collection(FirebaseCollections.CUSTOMERS)
                .document(uid)
                .update(update)
                .addOnSuccessListener(v ->
                        Log.d(TAG, "FCM token synced after auth for UID: " + uid))
                .addOnFailureListener(e ->
                        Log.w(TAG, "FCM token sync failed (non-critical).", e));
    }
}

