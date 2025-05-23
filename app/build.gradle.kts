plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.joe.weatherapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.joe.weatherapp"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        viewBinding = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    // NEW DEPENDENCY: Retrofit for networking
    implementation(libs.retrofit)
    // NEW DEPENDENCY: Gson converter for Retrofit to parse JSON
    implementation(libs.retrofit.converter.gson)
    // NEW DEPENDENCY: Gson library itself (sometimes implicitly included, but good to be explicit)
    implementation(libs.gson)

    // NEW DEPENDENCY: Picasso for image loading (for weather icons from URL)
    implementation(libs.picasso)

    // NEW DEPENDENCY: RecyclerView
    implementation(libs.androidx.recyclerview)

    // NEW DEPENDENCY: Google Play Services Location API
    implementation(libs.play.services.location)

    // NEW DEPENDENCY: AndroidX Transition for animations
    implementation(libs.androidx.transition)

}