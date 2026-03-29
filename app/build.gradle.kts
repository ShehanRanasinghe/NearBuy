import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.services)
}

// ── .env loader ──────────────────────────────────────────────────────────────
// Reads key=value pairs from the project-root .env file.
// Blank lines and lines starting with '#' are ignored.
// Values may optionally be quoted with double-quotes.
fun loadEnv(): Map<String, String> {
    val envFile = rootProject.file(".env")
    val map = mutableMapOf<String, String>()
    if (!envFile.exists()) return map
    envFile.readLines().forEach { raw ->
        val line = raw.trim()
        if (line.isEmpty() || line.startsWith("#")) return@forEach
        val idx = line.indexOf('=')
        if (idx <= 0) return@forEach
        val key   = line.substring(0, idx).trim()
        val value = line.substring(idx + 1).trim().removeSurrounding("\"")
        map[key] = value
    }
    return map
}

val env: Map<String, String> = loadEnv()

// Helper: resolve a key from .env; fall back to local.properties; then use the default.
fun envOrLocal(key: String, default: String = ""): String {
    if (env.containsKey(key)) return env[key]!!
    val localProps = Properties()
    val localFile  = rootProject.file("local.properties")
    if (localFile.exists()) localFile.inputStream().use { localProps.load(it) }
    return localProps.getProperty(key, default)
}

android {
    namespace = "com.example.nearbuy"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.nearbuy"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // ── Feature flags (sourced from .env) ────────────────────────────
        // Firebase connection details come from google-services.json (via the
        // google-services plugin). Only the on/off switch lives in .env so the
        // app can degrade gracefully while the project is being set up.
        buildConfigField("boolean", "FIREBASE_ENABLED",
            envOrLocal("FIREBASE_ENABLED", "false"))

        // ── Google Maps API key (sourced from .env) ───────────────────────
        buildConfigField("String", "GOOGLE_MAP_APIKEY",
            "\"${envOrLocal("GOOGLE_MAP_APIKEY")}\"")
    }

    buildFeatures {
        buildConfig = true   // enables BuildConfig generation
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    // ── AndroidX / Material ───────────────────────────────────────────────
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.recyclerview)
    implementation(libs.cardview)

    // ── Firebase (BOM manages individual library versions) ────────────────
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.messaging)

    // ── Google Play Services – Location (nearby distance calculation) ─────
    implementation(libs.play.services.location)

    // ── Tests ─────────────────────────────────────────────────────────────
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}