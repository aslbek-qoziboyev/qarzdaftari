plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.qarzdaftari.aslbek"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.qarzdaftari.aslbek"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "SUPABASE_URL", "\"https://dbhbdhub.supabase.co\"")
        buildConfigField("String", "SUPABASE_KEY", "\"YOUR_SUPABASE_ANON_KEY\"")
    }

    buildFeatures { compose = true; buildConfig = true }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.04.01"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("io.github.jan-tennert.supabase:auth-kt:3.2.4")
    implementation("io.github.jan-tennert.supabase:postgrest-kt:3.2.4")
    implementation("io.ktor:ktor-client-android:3.1.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
}
