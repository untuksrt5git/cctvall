plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "id.cctvall.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "id.cctvall.app"
        minSdk = 23
        targetSdk = 34
        versionCode = 3
        versionName = "0.3.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("com.google.android.exoplayer:exoplayer:2.19.1")
    // RTSP is a separate ExoPlayer module; required for cloud-cam-viewer streams.
    implementation("com.google.android.exoplayer:exoplayer-rtsp:2.19.1")
}
