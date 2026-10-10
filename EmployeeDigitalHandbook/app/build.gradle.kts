plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.employeedigitalhandbook"
    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.employeedigitalhandbook"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "v1.0.0-beta.2" //app version - to be changed on each new APK iteration

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        buildConfig = true //makes sure that the build config class is auto generated
        viewBinding = true
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
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    //navigation
    implementation(libs.androidx.navigation.fragment.ktx.v285)
    implementation(libs.androidx.navigation.ui.ktx.v285)


    // Retrofit for API calls
    implementation(libs.retrofit)
    implementation(libs.converter.gson)

    // OkHttp Logging Interceptor (for debugging API calls)
    implementation(libs.logging.interceptor)

    // Kotlin Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    //image view
    implementation(libs.glide)
}