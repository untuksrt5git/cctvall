plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.chaquo.python")
}

android {
    namespace = "id.cctvall.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "id.cctvall.app"
        minSdk = 23
        targetSdk = 34
        versionCode = 5
        versionName = "0.4.0"
        ndk { abiFilters += listOf("armeabi-v7a", "arm64-v8a") }
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
    implementation("com.google.android.exoplayer:exoplayer-hls:2.19.1")
}


/*
 * Python 3.10 is required by cloud-cam-viewer. Chaquopy 15.0.1 supports
 * Android API 21+ and 32-bit ARM, allowing this APK to run on Android 6 / ARMv7.
 */
chaquopy {
    defaultConfig {
        version = "3.10"
        pip {
            install("numpy==1.26.2")
            install("requests")
            install("pycryptodome==3.9.4")
        }
    }
}
