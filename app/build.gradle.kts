plugins {
id("com.android.application")
id("org.jetbrains.kotlin.android")
id("org.jetbrains.kotlin.plugin.compose")
}

android {
namespace = "com.melmatech.app"
compileSdk = 35

defaultConfig {
    applicationId = "com.melmatech.app"
    minSdk = 24
    targetSdk = 35
    versionCode = 1
    versionName = "1.0"

    buildConfigField("String", "SUPABASE_URL", "\"https://yxdwryjdckupdaduajuy.supabase.co\"")
    buildConfigField("String", "SUPABASE_KEY", "\"sb_publishable_PU_7owmDVUa_CFwYGkJlgw_SEFAuIHn\"")
}

buildFeatures {
compose = true
buildConfig = true
}

compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlinOptions {
    jvmTarget = "17"
}

}

dependencies {
implementation("androidx.core:core-ktx:1.15.0")
implementation("androidx.activity:activity-compose:1.9.3")
implementation(platform("androidx.compose:compose-bom:2024.10.01"))
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.ui:ui-tooling-preview")
implementation("androidx.compose.material3:material3")
implementation("io.coil-kt:coil-compose:2.7.0")
implementation("androidx.compose.material:material-icons-extended")
debugImplementation("androidx.compose.ui:ui-tooling")

// Client HTTP pour communiquer avec Supabase
implementation("com.squareup.okhttp3:okhttp:4.12.0")

}
// Scanner de codes-barres avec CameraX
implementation("androidx.camera:camera-camera2:1.4.1")
implementation("androidx.camera:camera-lifecycle:1.4.1")
implementation("androidx.camera:camera-view:1.4.1")

// Reconnaissance des codes-barres avec ML Kit
implementation("com.google.mlkit:barcode-scanning:17.3.0")
// Génération des codes-barres Code 128
implementation("com.google.zxing:core:3.5.3")

// Gestion du cycle de vie dans Jetpack Compose
implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
