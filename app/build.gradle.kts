import java.util.Properties
import java.io.FileInputStream
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)


}

android {
    namespace = "com.finadapt.adaptivefinance"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.finadapt.adaptivefinance"
        minSdk = 26
        targetSdk = 36
        versionCode = 6
        versionName = "1.0.5"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val properties = Properties()
        val localPropertiesFile = project.rootProject.file("local.properties")

        if (localPropertiesFile.exists()) {
            // Use FileInputStream to load the file contents
            properties.load(FileInputStream(localPropertiesFile))
        }

        // Use quotes around the value for BuildConfig strings
        val token = properties.getProperty("API_TOKEN") ?: "MISSING_TOKEN"
        buildConfigField("String", "API_TOKEN", "\"$token\"")

    }
    buildFeatures {
        buildConfig = true
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}


dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Retrofit (Network calls to FastAPI)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // Coroutines (Async processing)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    //room database
    val room_version = "2.6.1"
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version")//(Coroutines/Flow).

    ksp("androidx.room:room-compiler:$room_version")

    implementation("androidx.compose.material:material-icons-extended")
    implementation("com.airbnb.android:lottie-compose:6.3.0")
    implementation("com.patrykandpatrick.vico:compose:1.14.0")
    implementation("com.patrykandpatrick.vico:compose-m3:1.14.0")

    implementation("androidx.work:work-runtime-ktx:2.9.0")//background work manager
    implementation("com.airbnb.android:lottie-compose:6.3.0")//for lottie files
    // Standard ML Kit Text Recognition
    implementation("com.google.android.gms:play-services-mlkit-text-recognition:19.0.1")
    // The New ML Kit Document Scanner UI
    implementation("com.google.android.gms:play-services-mlkit-document-scanner:16.0.0-beta1")
    // Allows Kotlin Coroutines to 'await()' Google Play Services Tasks
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.apache.poi:poi:5.2.3")
    implementation("org.apache.poi:poi-ooxml:5.2.3")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")





}