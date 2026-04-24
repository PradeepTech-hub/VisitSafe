plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.visitsafe"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.visitsafe"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // 🔐 Signing configuration
    signingConfigs {
        create("release") {
            // 👉 OPTION 1: Put .jks file inside app folder
            storeFile = file("C:\\Users\\Pradeep M\\Documents\\visitsafe-key.jks")            // 👉 OR use full path like:
            // storeFile = file("C:\\Users\\Pradeep M\\Documents\\visitsafe-key.jks")

            storePassword = "123456789"
            keyAlias = "visitsafe"
            keyPassword = "123456789"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false

            // ✅ THIS LINE FIXES YOUR ISSUE
            signingConfig = signingConfigs.getByName("release")

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.messaging)

    // Lifecycle
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.livedata)

    // QR Code
    implementation(libs.zxing.core)
    implementation(libs.zxing.android.embedded)

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}