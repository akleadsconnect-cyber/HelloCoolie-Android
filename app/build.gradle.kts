plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt)
    alias(libs.plugins.navigation.safeargs)
}

android {
    namespace         = "in.hellocoolie"
    compileSdk        = 34

    defaultConfig {
        applicationId = "in.hellocoolie"
        minSdk        = 24
        targetSdk     = 34
        versionCode   = 1
        versionName   = "1.0.0"
        buildConfigField("String", "BASE_URL", "\"https://hellocoolie.onrender.com/api/\"")
        buildConfigField("String", "SOCKET_URL","\"https://hellocoolie.onrender.com\"")
        buildConfigField("String", "RAZORPAY_KEY","\"rzp_test_your_key_here\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        viewBinding  = true
        buildConfig  = true
        dataBinding  = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.viewpager2)
    implementation(libs.swiperefresh)
    implementation(libs.shimmer)

    // Lifecycle
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)
    implementation(libs.lifecycle.runtime)

    // Navigation
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)

    // Network
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)

    // Socket.IO
    implementation(libs.socketio) { exclude(group="org.json", module="json") }

    // Hilt DI
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    // Coroutines
    implementation(libs.coroutines.android)

    // Room (local cache)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    kapt(libs.room.compiler)

    // DataStore (auth token storage)
    implementation(libs.datastore)

    // Razorpay
    implementation(libs.razorpay)

    // Glide (images)
    implementation(libs.glide)

    // Lottie (animations)
    implementation(libs.lottie)

    // Charts (earnings)
    implementation(libs.mpandroidchart)
}
