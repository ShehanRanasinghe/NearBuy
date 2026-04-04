<div align="center">

# 🛒 NearBuy — Customer App

**Discover products, deals, and shops near you — powered by Firebase & Google Maps**

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Java](https://img.shields.io/badge/Language-Java-ED8B00?logo=openjdk&logoColor=white)](https://www.java.com)
[![Firebase](https://img.shields.io/badge/Backend-Firebase-FFCA28?logo=firebase&logoColor=black)](https://firebase.google.com)
[![License](https://img.shields.io/badge/License-GPL--3.0-blue.svg)](LICENSE)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-24%20(Android%207.0)-brightgreen)](https://developer.android.com/about/versions/nougat)
[![Target SDK](https://img.shields.io/badge/Target%20SDK-36-brightgreen)](https://developer.android.com)

</div>

---

## 📖 Table of Contents

- [About](#-about)
- [Features](#-features)
- [Screenshots & App Flow](#-app-flow)
- [Tech Stack](#-tech-stack)
- [Project Structure](#-project-structure)
  - [Package Breakdown](#package-breakdown)
- [Firestore Data Architecture](#-firestore-data-architecture)
- [Prerequisites](#-prerequisites)
- [Setup & Configuration](#-setup--configuration)
- [Build & Run](#-build--run)
- [Required Permissions](#-required-permissions)
- [Dependencies](#-dependencies)
- [Contributors](#-contributors)
- [License](#-license)

---

## 📌 About

**NearBuy** is an Android customer-facing mobile application built as a coursework project for the **HND in Software Engineering** programme at NIBM (National Institute of Business Management, Sri Lanka).

NearBuy connects customers with nearby local shops, allowing them to discover products, browse active deals and promotions, place orders, and navigate directly to shops via an integrated Google Maps view — all from a single app.

It is the **customer-side companion** to the **NearBuyHQ** admin/shop-owner app. Both apps share the same Firebase project; NearBuyHQ writes product and deal data that NearBuy reads and displays.

---

## ✨ Features

| Feature | Description |
|---|---|
| 🔐 **OTP Registration** | Two-step sign-up: OTP sent to email via Gmail SMTP, verified against Firestore before account creation |
| 🔑 **Login / Logout** | Firebase Email & Password Authentication with SessionManager caching |
| 🔒 **Forgot Password** | Firebase password-reset email flow |
| 🏠 **Dashboard** | Personalized home screen with nearby shops, hot products, latest deals, and promotions |
| 🔍 **Location-Aware Search** | Search products across all shops within a configurable radius (1 – 10 km) using the Haversine formula |
| 🗺️ **Nearby Map** | Google Maps view showing all active shops as markers; tap a marker to view store details |
| 🏷️ **Deals & Promotions** | Browse, filter, and bookmark deals/promos; view full deal details with expiry countdown |
| 🔖 **Saved Deals** | Bookmarked deals list with swipe-to-remove |
| 📦 **Product Details** | Full product view including price, stock, unit, shop info, and distance |
| 🏪 **Store Details** | Shop profile page with contact info, opening hours, location, and product catalogue |
| 🛍️ **Orders** | Place orders with Delivery or Pick-Up fulfillment; track order status (Processing / Delivered / Cancelled) |
| 🔔 **Push Notifications** | Firebase Cloud Messaging (FCM) receives deal alerts and order updates |
| 👤 **Profile Management** | Edit name, email, phone; update delivery location via Google Places autocomplete |
| ⚙️ **Settings** | Manage search radius preference and app preferences |
| 🌐 **Offline Graceful Degradation** | Feature flag (`FIREBASE_ENABLED`) allows the app to start without Firebase configured |

---

## 🔄 App Flow

```
SplashScreen
    │
    ├─ Session active? ──Yes──► DashboardActivity
    │
    └─ No
         │
         ▼
    WelcomeActivity
         │
         ├──► LoginActivity ──► DashboardActivity
         │
         ├──► RegisterActivity ──► OTPVerificationActivity ──► DashboardActivity
         │
         └──► ForgotPasswordActivity

DashboardActivity (Bottom Navigation)
    ├──► NearbyMapActivity
    ├──► SearchActivity ──► ProductDetailsActivity
    │                   └──► StoreDetailsActivity
    ├──► DealsActivity ──► DealDetailsActivity
    │                  └──► SavedDealsActivity
    ├──► OrdersActivity
    └──► ProfileActivity ──► LocationPickerActivity
                         └──► SettingsActivity
```

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| **Language** | Java 17 |
| **Platform** | Android (Min SDK 24 / Target SDK 36) |
| **Build System** | Gradle 8 with Kotlin DSL (`build.gradle.kts`) |
| **Authentication** | Firebase Authentication (Email & Password) |
| **Database** | Cloud Firestore (NoSQL) |
| **File Storage** | Firebase Storage |
| **Push Notifications** | Firebase Cloud Messaging (FCM) |
| **App Security** | Firebase App Check (Debug provider; swap for Play Integrity in production) |
| **Maps & Location** | Google Maps SDK · Google Play Services Location · Google Places SDK |
| **Email (OTP)** | JavaMail for Android (Gmail SMTP / TLS port 587) |
| **UI Components** | Material Design 3 · RecyclerView · CardView · ConstraintLayout |
| **Secret Management** | `.env` file at project root → injected into `BuildConfig` at compile time |

---

## 📁 Project Structure

```
NearBuy/
├── app/
│   ├── src/main/
│   │   ├── java/com/example/nearbuy/
│   │   │   ├── app/                    # Application class & startup screens
│   │   │   ├── auth/                   # Login, Register, OTP, Forgot Password
│   │   │   ├── core/                   # Shared services: Session, Email, FCM, Firebase config
│   │   │   ├── dashboard/              # Home/Dashboard screen & its RecyclerView adapters
│   │   │   ├── data/
│   │   │   │   ├── model/              # Firestore data models (POJOs)
│   │   │   │   ├── remote/firebase/    # Firestore collection name constants
│   │   │   │   └── repository/         # Data access layer (Firestore read/write)
│   │   │   ├── discounts/              # Deals, Promotions, Saved Deals screens & adapters
│   │   │   ├── map/                    # Nearby shops Google Maps screen
│   │   │   ├── notifications/          # Notifications list screen
│   │   │   ├── orders/                 # Orders list screen, adapter, and model
│   │   │   ├── product/                # Product Details screen
│   │   │   ├── profile/                # Profile, Settings, Location Picker, Logout
│   │   │   ├── search/                 # Search screen, grid adapter, and result model
│   │   │   └── store/                  # Store Details screen and item adapter
│   │   ├── res/                        # Layouts, drawables, strings, colors, themes
│   │   └── AndroidManifest.xml
│   ├── build.gradle.kts                # App-level Gradle build (deps, BuildConfig fields)
│   └── google-services.json            # Firebase project config (not committed)
├── gradle/
│   └── libs.versions.toml              # Version catalog for all dependencies
├── build.gradle.kts                    # Root Gradle build
├── settings.gradle.kts                 # Project & module settings
├── .env                                # Secret keys (not committed – see Setup)
├── CHANGES.md                          # Code cleanup changelog
└── README.md
```

---

## 📦 Package Breakdown

### `app` — Application Bootstrap

| File | Description |
|---|---|
| `NearBuyApp.java` | Custom `Application` class. Initialises **Firebase App Check** (debug provider) on startup to prevent `PERMISSION_DENIED` errors from Firestore. Swap to `PlayIntegrityAppCheckProviderFactory` for production releases. |
| `startup/SplashScreen.java` | App entry point. Checks `AuthRepository.isSessionActive()` and routes to either `DashboardActivity` (existing session) or `WelcomeActivity` (no session). |
| `startup/WelcomeActivity.java` | Landing screen shown to new/logged-out users. Navigates to Login or Register. |

---

### `auth` — Authentication Screens

| File | Description |
|---|---|
| `LoginActivity.java` | Email + password sign-in screen. Delegates to `AuthRepository.login()`. |
| `RegisterActivity.java` | New account registration form (name, email, phone, password). Calls `AuthRepository.initiateRegistration()` which stores an OTP in Firestore and sends it to the customer's email via Gmail SMTP. Navigates to `OTPVerificationActivity`. |
| `OTPVerificationActivity.java` | Displays six digit-input boxes. Verifies the entered code against the Firestore `otp_codes/{email}` document via `AuthRepository.verifyOtpAndRegister()`. Creates the Firebase Auth account and customer profile document on success. |
| `ForgotPasswordActivity.java` | Sends a Firebase password-reset email via `AuthRepository.sendPasswordReset()`. |

---

### `core` — Shared Services

| File | Description |
|---|---|
| `SessionManager.java` | **Singleton** backed by `SharedPreferences`. Caches the signed-in customer's UID, name, email, phone, GPS coordinates, address, search radius, and FCM token so every screen can access them instantly without hitting Firestore. Provides `isLoggedIn()` and `clearSession()` for auth lifecycle management. |
| `EmailService.java` | Sends **HTML-formatted OTP emails** via Gmail SMTP (TLS port 587) using JavaMail for Android. Credentials (`SMTP_EMAIL`, `SMTP_PASSWORD`) are injected from `.env` via `BuildConfig`. All network I/O runs on a background thread; the result callback is always posted to the main thread. |
| `NearBuyMessagingService.java` | Extends `FirebaseMessagingService`. Handles FCM push notification delivery and token refresh events. Persists the new token in `SessionManager` and syncs it to the customer's Firestore profile. |
| `firebase/FirebaseConfig.java` | Utility class with a single method `isFirebaseEnabled()`. Reads `BuildConfig.FIREBASE_ENABLED` (set from `.env`). All repositories check this flag before making any live Firebase request, allowing the app to start gracefully without a configured Firebase project. |

---

### `dashboard` — Home / Dashboard

| File | Description |
|---|---|
| `DashboardActivity.java` | The main authenticated screen. Shows a personalised greeting, the customer's location, and four horizontally-scrolling sections: **Nearby Shops**, **Hot Products**, **Latest Deals**, and **Promotions**. Hosts the bottom navigation bar linking to Map, Search, Deals, Orders, and Profile. |
| `DashboardShopAdapter.java` | `RecyclerView.Adapter` for the **Nearby Shops** horizontal list. Displays shop name, distance badge, and status. Fires `OnShopClickListener` on tap → navigates to `StoreDetailsActivity`. |
| `DashboardProductAdapter.java` | `RecyclerView.Adapter` for the **Hot Products** horizontal list. Shows product image, name, price, and shop distance. Fires `OnProductClickListener` on tap → navigates to `ProductDetailsActivity`. |
| `DashboardDealAdapter.java` | `RecyclerView.Adapter` for the **Latest Deals** horizontal list. Shows deal title, discount badge, and expiry label. Fires `OnDealClickListener` on tap → navigates to `DealDetailsActivity`. |
| `DashboardPromoAdapter.java` | `RecyclerView.Adapter` for the **Promotions** horizontal list. Shows promo title, occasion tag, and valid-until date. Fires `OnPromoClickListener` on tap → navigates to `DealDetailsActivity`. |

---

### `data` — Data Layer

#### `data/model` — Firestore POJOs

| File | Firestore Path | Description |
|---|---|---|
| `Customer.java` | `NearBuy/{customerId}` | Registered customer profile. Fields: name, email, phone, status, totalOrders, totalSpent, totalSaved. Includes `toMap()` / `fromMap()` for Firestore serialisation. |
| `Shop.java` | `NearBuyHQ/{shopId}` | Shop registered via NearBuyHQ admin app. Fields: name, ownerName, email, phone, shopLocation, openingHours, latitude, longitude, status. Includes `distanceKm` as a runtime-only computed field (Haversine, not stored). |
| `Product.java` | `NearBuyHQ/{shopId}/products/{productId}` | Product listed by a shop. Fields: name, description, category, price, originalPrice, unit, stockQty, imageUrl, isAvailable. Runtime fields: shopName, shopLocation, shopCoordinates, distanceKm. Handles both `NearBuyHQ` admin field names and legacy aliases (`itemName`, `itemDetails`, `stockQuantity`). |
| `DealItem.java` | `NearBuyHQ/{shopId}/deals/{dealId}` or `.../promotions/{promoId}` | Unified model for both **deals** and **promotions** (distinguished by `isPromotion` flag). Includes discount label auto-generation, expiry countdown helpers (`daysUntilExpiry()`, `getExpiryLabel()`), and support for Firestore `Timestamp`, `Long`, and `String` date formats. |
| `NotificationItem.java` | `NearBuy/{customerId}/notifications/{id}` | Customer notification. Types: `deal`, `order`, `promo`, `system`. Fields: type, title, body, relatedId, shopId, shopName, isRead, createdAt. |

#### `data/remote/firebase`

| File | Description |
|---|---|
| `FirebaseCollections.java` | **Single source of truth** for all Firestore collection and sub-collection path constants used by both the customer app and the NearBuyHQ admin app. Prevents magic-string typos across repositories. |

#### `data/repository` — Data Access Layer

| File | Description |
|---|---|
| `AuthRepository.java` | Handles the full authentication lifecycle: **2-step OTP registration**, email/password login, logout, password reset, profile load, profile update, location update, and FCM token sync. |
| `ShopRepository.java` | READ-ONLY. Loads all `status == "Active"` shops from `NearBuyHQ`. Distance filtering is performed client-side via Haversine because Firestore does not support native geo-radius queries. |
| `ProductRepository.java` | Loads products from shop sub-collections. Enriches each product with parent shop metadata (name, address, coordinates, distance). |
| `DealRepository.java` | Loads deals and promotions from all active shops. Supports filtering by type (deal/promo), distance, and active status. |
| `OrderRepository.java` | **Dual-write**: writes new orders to both `NearBuyHQ/{shopId}/orders/` (visible to shop owner) and `NearBuy/{customerId}/orders/` (visible to customer). Also reads the customer's order history. |
| `NotificationRepository.java` | Reads and manages notifications from `NearBuy/{customerId}/notifications/`. Marks notifications as read. |
| `SearchRepository.java` | Location-aware product search. Algorithm: load all active shops → filter by radius (Haversine) → load each shop's products → filter by name/category → enrich with shop metadata → sort by nearest-first, then lowest price. |
| `DataCallback.java` | Generic callback interface with `onSuccess(T data)` and `onError(Exception e)` for asynchronous Firestore operations that return data. |
| `OperationCallback.java` | Generic callback interface with `onSuccess()` and `onError(Exception e)` for asynchronous operations that return no data (write/update/delete). |

---

### `discounts` — Deals & Promotions

| File | Description |
|---|---|
| `DealsActivity.java` | Full list of active deals fetched from all shops in the customer's vicinity. Supports category filter tabs. |
| `DealsAndPromoActivity.java` | Combined deals + promotions view accessed via "View All" from the Dashboard. |
| `DealDetailsActivity.java` | Full-screen deal/promo detail view with image, discount info, expiry countdown, shop details, and a "Save Deal" button. |
| `DealPromoAdapter.java` | `RecyclerView.Adapter` powering the deals/promotions list. Renders discount badges, expiry labels, and shop distance. |
| `SavedDealsActivity.java` | Lists deals bookmarked by the customer. Supports swipe-to-remove with animation. |
| `SavedDealsAdapter.java` | `RecyclerView.Adapter` for the saved deals list. Wires tap (open detail) and remove (delete bookmark) callbacks. |
| `SavedDealItem.java` | Lightweight model for a saved deal entry stored under `NearBuy/{customerId}/saved_deals/`. |

---

### `map` — Nearby Map

| File | Description |
|---|---|
| `NearbyMapActivity.java` | Displays a Google Maps fragment centred on the customer's location. Loads all active shops and places a map marker for each. Tapping a marker shows a shop info window; tapping the window navigates to `StoreDetailsActivity`. |

---

### `notifications` — Push Notifications

| File | Description |
|---|---|
| `NotificationsActivity.java` | Displays the customer's notification history (deals, orders, system messages). Marks notifications as read on open. |

---

### `orders` — Orders

| File | Description |
|---|---|
| `OrdersActivity.java` | Shows the customer's order history with status tabs (All / Processing / Delivered / Cancelled). Displays spending stats. Supports order reporting via a dialog. |
| `OrdersAdapter.java` | `RecyclerView.Adapter` rendering order row cards with shop name, date, items summary, total amount, and a colour-coded status badge. |
| `OrderItem.java` | Order model for both Firestore paths. Fields: orderId, shopId, shopName, orderDate, itemsSummary, totalAmount, status, itemCount, fulfillmentType (Delivery / Pick Up), and customer identity fields for admin visibility. |

---

### `product` — Product Details

| File | Description |
|---|---|
| `ProductDetailsActivity.java` | Full product detail screen showing name, image (if available), description, category, price/discount, stock quantity, unit, shop name, shop address, distance, and an **Order** button that opens the order placement dialog for `OrderRepository`. |

---

### `profile` — User Profile

| File | Description |
|---|---|
| `ProfileActivity.java` | Displays and edits the customer's profile (name, email, phone). Calls `AuthRepository.updateProfile()` to persist changes to Firestore and refresh `SessionManager`. |
| `LocationPickerActivity.java` | Lets the customer set their delivery/search location via a Google Maps view with a draggable pin or **Google Places autocomplete** search. Saves the chosen coordinates and address via `AuthRepository.updateUserLocation()`. |
| `SettingsActivity.java` | App preferences screen. Allows the customer to change the search radius (1, 2, 3, 5, or 10 km) which is persisted in `SessionManager`. |
| `LogoutActivity.java` | Confirms logout, calls `AuthRepository.logout()` to sign out of Firebase Auth and clear `SessionManager`, then returns to `WelcomeActivity`. |

---

### `search` — Product Search

| File | Description |
|---|---|
| `SearchActivity.java` | Search screen with a text input and radius filter. Passes query + customer location + radius to `SearchRepository.search()` and displays paginated results. |
| `SearchGridAdapter.java` | `RecyclerView.Adapter` rendering search results in a two-column grid. Each card shows product image placeholder, name, price, shop name, and distance badge. |
| `SearchResultItem.java` | Lightweight display model for a single search result row (wraps `Product` fields needed for the grid card). |

---

### `store` — Store Details

| File | Description |
|---|---|
| `StoreDetailsActivity.java` | Full shop profile screen. Shows shop name, owner name, address, phone, opening hours, distance, a Google Maps mini-view with the shop pin, and a vertical list of the shop's available products. |
| `StoreItemAdapter.java` | `RecyclerView.Adapter` rendering the product list inside `StoreDetailsActivity`. Tapping a product navigates to `ProductDetailsActivity`. |

---

## 🗄️ Firestore Data Architecture

```
Firestore Root
│
├── NearBuyHQ/                        ← Written by NearBuyHQ admin app
│   └── {shopId}/                     ← One document per shop
│       ├── (fields: name, shopName, email, phone, shopLocation,
│       │           openingHours, latitude, longitude, status, createdAt)
│       ├── products/                 ← Sub-collection
│       │   └── {productId}/         ← (name, description, category, price,
│       │                                originalPrice, unit, stockQty,
│       │                                imageUrl, isAvailable, createdAt)
│       ├── deals/                    ← Sub-collection
│       │   └── {dealId}/            ← (title, description, discountLabel,
│       │                                salePrice, originalPrice, expiresAt,
│       │                                imageUrl, isActive, isPromotion)
│       ├── promotions/               ← Sub-collection (same schema as deals)
│       │   └── {promoId}/
│       └── orders/                   ← Written by NearBuy customer app
│           └── {orderId}/
│
├── NearBuy/                          ← Written by NearBuy customer app
│   └── {customerId}/                 ← One document per registered customer
│       ├── (fields: name, email, phone, status, totalOrders,
│       │           totalSpent, totalSaved, latitude, longitude,
│       │           address, fcmToken, tokenUpdatedAt, createdAt)
│       ├── orders/                   ← Customer's order history
│       │   └── {orderId}/
│       ├── saved_deals/              ← Bookmarked deals
│       │   └── {dealId}/
│       └── notifications/            ← Push notification inbox
│           └── {notificationId}/
│
└── otp_codes/                        ← Temporary OTP documents (pre-auth)
    └── {email}/                      ← (code, phone, expiresAt, createdAt)
                                         Deleted after successful verification
```

---

## ✅ Prerequisites

- **Android Studio** Hedgehog (2023.1.1) or later
- **JDK 17** (bundled with Android Studio)
- **Android SDK** – API Level 24 – 36
- A **Firebase project** with:
  - Authentication (Email/Password provider enabled)
  - Cloud Firestore (in production or test mode)
  - Firebase Storage
  - Firebase Cloud Messaging
  - Firebase App Check (debug token registered)
- A **Google Cloud project** with:
  - Maps SDK for Android enabled
  - Places API enabled
  - A valid **API Key** (restrict it to your app's package name + SHA-1)
- A **Gmail account** with an [App Password](https://myaccount.google.com/apppasswords) generated for SMTP

---

## ⚙️ Setup & Configuration

### 1. Clone the repository

```bash
git clone https://github.com/<your-org>/NearBuy.git
cd NearBuy
```

### 2. Add `google-services.json`

Download your `google-services.json` from the **Firebase Console → Project Settings → Your Apps** and place it at:

```
app/google-services.json
```

> ⚠️ This file is excluded from version control via `.gitignore`. Never commit it.

### 3. Create the `.env` file

Create a `.env` file at the **project root** (same level as `settings.gradle.kts`):

```env
# Firebase feature flag – set to true once google-services.json is in place
FIREBASE_ENABLED=true

# Firebase project ID (from google-services.json → project_info.project_id)
FIREBASE_PROJECT_ID=your-firebase-project-id

# Google Maps & Places API key
GOOGLE_MAP_APIKEY=AIza...

# Gmail SMTP credentials for OTP email delivery
# Generate an App Password at: https://myaccount.google.com/apppasswords
SMTP_EMAIL=your-gmail@gmail.com
SMTP_PASSWORD=your-app-password
```

> ⚠️ `.env` is excluded from version control via `.gitignore`. Never commit credentials.

The Gradle build script (`app/build.gradle.kts`) reads this file and injects all values into `BuildConfig` fields and `AndroidManifest.xml` placeholders at compile time.

**Key priority order:** `.env` → Gradle property → System environment variable → empty string default.  
CI/CD pipelines can inject secrets as environment variables or Gradle properties without a physical `.env` file.

### 4. Register the debug App Check token (optional but recommended)

If Firebase App Check enforcement is enabled on your project:

1. Build and run the app once.
2. Find the **debug token** in Logcat (tag: `NearBuy.App`).
3. Register it in **Firebase Console → App Check → Apps → Add debug token**.

---

## 🔨 Build & Run

```bash
# Debug build
./gradlew assembleDebug

# Release build (update signingConfigs in build.gradle.kts first)
./gradlew assembleRelease

# Run on connected device / emulator
./gradlew installDebug
```

Or open the project in **Android Studio** and click **▶ Run**.

> **Note:** If `FIREBASE_ENABLED=false` (or the key is absent), the app starts but all repository calls return an error callback immediately. The UI handles this gracefully so you can develop UI without Firebase configured.

---

## 🔒 Required Permissions

| Permission | Purpose |
|---|---|
| `INTERNET` | All Firebase / SMTP / Maps API network calls |
| `ACCESS_NETWORK_STATE` | Check connectivity before making requests |
| `ACCESS_FINE_LOCATION` | GPS for nearby-shop distance filtering and map centering |
| `ACCESS_COARSE_LOCATION` | Fallback coarse location |
| `POST_NOTIFICATIONS` | Display FCM push notifications (runtime grant required on Android 13+) |
| `READ_EXTERNAL_STORAGE` *(max SDK 32)* | Fallback for picking images on Android 12 and below |

---

## 📚 Dependencies

| Dependency | Version | Purpose |
|---|---|---|
| `androidx.appcompat` | 1.7.1 | Backward-compatible Activity / AppCompat theming |
| `com.google.android.material` | 1.13.0 | Material Design 3 components |
| `androidx.activity` | 1.9.3 | `ComponentActivity` / result APIs |
| `androidx.constraintlayout` | 2.2.1 | Flexible XML layouts |
| `androidx.recyclerview` | 1.3.2 | Scrollable list & grid views |
| `androidx.cardview` | 1.0.0 | Card-style UI containers |
| `firebase-bom` | 34.11.0 | BOM — manages all Firebase library versions |
| `firebase-auth` | *(BOM)* | Email/password authentication |
| `firebase-firestore` | *(BOM)* | NoSQL cloud database |
| `firebase-storage` | *(BOM)* | Image / file storage |
| `firebase-messaging` | *(BOM)* | FCM push notifications |
| `firebase-appcheck-debug` | *(BOM)* | App Check debug provider |
| `play-services-location` | 21.3.0 | FusedLocationProviderClient for GPS |
| `play-services-maps` | 18.2.0 | Google Maps SDK for Android |
| `places` | 3.3.0 | Google Places autocomplete (location picker) |
| `android-mail` | 1.6.7 | JavaMail for Android — Gmail SMTP OTP emails |
| `android-activation` | 1.6.7 | JavaMail activation framework (required by android-mail) |

---

## 👥 Contributors

<table>
  <tr>
    <td align="center">
      <a href="https://github.com/ShehanRanasinghe">
        <img src="https://github.com/ShehanRanasinghe.png" width="80" style="border-radius:50%"/><br/>
        <sub><b>Shehan Ranasinghe</b></sub>
      </a>
    </td>
    <td align="center">
      <a href="https://github.com/sitharakavindi">
        <img src="https://github.com/sitharakavindi.png" width="80" style="border-radius:50%"/><br/>
        <sub><b>Sithara Kavindi</b></sub>
      </a>
    </td>
    <td align="center">
      <a href="https://github.com/PramodyaKarunathilake">
        <img src="https://github.com/PramodyaKarunathilake.png" width="80" style="border-radius:50%"/><br/>
        <sub><b>Pramodya Karunathilake</b></sub>
      </a>
    </td>
    <td align="center">
      <a href="https://github.com/SanduniKarunathilake">
        <img src="https://github.com/SanduniKarunathilake.png" width="80" style="border-radius:50%"/><br/>
        <sub><b>Sanduni Karunathilake</b></sub>
      </a>
    </td>
  </tr>
</table>

---

## 📄 License

This project is licensed under the **GNU General Public License v3.0 (GPL-3.0)**.

You are free to use, modify, and distribute this software under the terms of the GPL-3.0. Any derivative work must also be released under the same license.

See the [`LICENSE`](LICENSE) file for the full license text.

> The LICENSE file will be auto-generated when you add the GPL-3.0 license via the GitHub repository settings.

---

<div align="center">

Made with ❤️ as part of the **HND in Software Engineering** coursework at **NIBM, Sri Lanka**.

</div>
