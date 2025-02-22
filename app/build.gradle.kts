plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.example.farmerapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.farmerapp"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        multiDexEnabled = true
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3" // Specify a complete version here
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))

    // Jetpack Compose
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.security.crypto.ktx)
    implementation(libs.googleid)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.core.android)
    androidTestImplementation(libs.androidx.espresso.core.v350)

    // Firebase dependencies
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.messaging)

    // Other dependencies
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.multidex)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.runtime.livedata)
    implementation(libs.androidx.ui.test.android)
    implementation(libs.generativeai)
    implementation(libs.androidx.benchmark.macro)
    implementation(libs.firebase.storage.ktx)
    implementation(libs.coil.kt.coil.compose)
    implementation(libs.checkout)

    // Testing dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)


    // Debugging tools
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Firebase Storage
    implementation(libs.google.firebase.storage.ktx)

    // Activity Result API
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.fragment.ktx)

    // Firebase Authentication dependency
    implementation(libs.firebase.auth)

    implementation(libs.play.services.maps)
    implementation (libs.firebase.auth)
    implementation (libs.play.services.auth)// Replace with the latest version
    implementation (libs.androidx.security.crypto.ktx.v110alpha06)  // Check the latest available version
    
}
