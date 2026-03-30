plugins {
    alias(libs.plugins.android.application)
    // google-services is applied conditionally below – not here –
    // so the project does not break when google-services.json is absent.
}

// ── .env loader ───────────────────────────────────────────────────────────────
// Reads KEY=VALUE pairs from the project-root .env file.
// Blank lines and lines that start with '#' are skipped.
// Values may optionally be wrapped in double-quotes.
val envValues = mutableMapOf<String, String>()
val envFile = rootProject.file(".env")
if (envFile.exists()) {
    envFile.readLines().forEach { line ->
        val cleaned = line.trim()
        if (cleaned.isNotEmpty() && !cleaned.startsWith("#")) {
            val index = cleaned.indexOf('=')
            if (index > 0) {
                val key   = cleaned.substring(0, index).trim()
                val value = cleaned.substring(index + 1).trim().trim('"')
                envValues[key] = value
            }
        }
    }
}

// ── Key resolver ──────────────────────────────────────────────────────────────
// Priority order: .env file → Gradle property → System environment → default.
// This lets CI/CD pipelines inject secrets without a physical .env file.
fun envOrProperty(key: String, defaultValue: String = ""): String {
    return envValues[key]
        ?: providers.gradleProperty(key).orNull
        ?: System.getenv(key)
        ?: defaultValue
}

// ── Google-services plugin ────────────────────────────────────────────────────
// Applied here (not in the plugins {} block) so the build does not fail if
// google-services.json is missing (e.g. when the repo is first cloned).
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

android {
    namespace  = "com.example.nearbuy"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.nearbuy"
        minSdk        = 24
        targetSdk     = 36
        versionCode   = 1
        versionName   = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // ── BuildConfig fields (all sourced from .env) ─────────────────────
        // These are embedded into BuildConfig.java at compile time so they can
        // be read at runtime without touching the source code.
        val firebaseEnabled   = envOrProperty("FIREBASE_ENABLED",   "false")
                                    .toBooleanStrictOrNull() ?: false
        val firebaseProjectId = envOrProperty("FIREBASE_PROJECT_ID", "")
        val mapsApiKey        = envOrProperty("GOOGLE_MAP_APIKEY",   "")
        val smtpEmail         = envOrProperty("SMTP_EMAIL",          "")
        val smtpPassword      = envOrProperty("SMTP_PASSWORD",       "")

        buildConfigField("boolean", "FIREBASE_ENABLED",    firebaseEnabled.toString())
        buildConfigField("String",  "FIREBASE_PROJECT_ID", "\"$firebaseProjectId\"")
        buildConfigField("String",  "GOOGLE_MAP_APIKEY",   "\"$mapsApiKey\"")
        buildConfigField("String",  "SMTP_EMAIL",          "\"$smtpEmail\"")
        buildConfigField("String",  "SMTP_PASSWORD",       "\"$smtpPassword\"")

        // Expose the Maps API key to AndroidManifest.xml via ${MAPS_API_KEY}
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
    }

    buildFeatures {
        buildConfig = true   // enables BuildConfig class generation
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // ── Packaging rules ────────────────────────────────────────────────────────
    // android-mail (JavaMail) bundles META-INF licence files that conflict with
    // other libraries during APK packaging.  Excluding them prevents the
    // "More than one file was found with OS independent path" build error.
    packaging {
        resources {
            excludes += "META-INF/NOTICE.md"
            excludes += "META-INF/LICENSE.md"
            excludes += "META-INF/LICENSE-notice.md"
            excludes += "META-INF/NOTICE"
            excludes += "META-INF/LICENSE"
        }
    }
}

dependencies {

    // ── AndroidX / Material ───────────────────────────────────────────────────
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.recyclerview)
    implementation(libs.cardview)

    // ── Firebase (BOM manages all Firebase library versions) ─────────────────
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)        // Email/password authentication
    implementation(libs.firebase.firestore)   // NoSQL cloud database
    implementation(libs.firebase.storage)     // File / image storage
    implementation(libs.firebase.messaging)   // FCM push notifications
    implementation(libs.firebase.appcheck.debug) // App Check – debug provider (swap for play-integrity in production)

    // ── Google Play Services ──────────────────────────────────────────────────
    implementation(libs.play.services.location) // GPS for nearby-shop distance queries

    // ── JavaMail for Android – OTP verification emails via Gmail SMTP ─────────
    implementation(libs.android.mail)
    implementation(libs.android.activation)

    // ── Tests ──────────────────────────────────────────────────────────────────
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}