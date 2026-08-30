plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.parcelize")
}

android {
    namespace = "com.lifeclock"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.lifeclock"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file("lifeclock-release.keystore")
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: "lifeclock2024"
            keyAlias = System.getenv("KEY_ALIAS") ?: "lifeclock"
            keyPassword = System.getenv("KEY_PASSWORD") ?: "lifeclock2024"
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-service:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    // Play Services coroutines task wrapper (await() for FusedLocationProvider)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3:1.2.0")
    // NOTE: material-icons-extended intentionally NOT included — adds 5+ MB.
    // We use only the ~30 core icons that ship with material3.

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // WorkManager - battery efficient background tasks
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Location - FusedLocationProvider
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // NOTE: Retrofit / OkHttp / Moshi intentionally NOT included — adds ~3 MB.
    // The optional sunrise API call uses built-in HttpURLConnection + org.json.

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Date/Time
    implementation("joda-time:joda-time:2.12.7")

    // Debugging
    debugImplementation("androidx.compose.ui:ui-tooling")
}
