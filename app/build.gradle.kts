plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.abspi.pitvexplorer"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.abspi.pitvexplorer"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

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
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.leanback)
    implementation(libs.glide)
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("androidx.recyclerview:recyclerview-selection:1.1.0")
    implementation(libs.androidx.appcompat)
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // ExoPlayer dependencies
    implementation("com.google.android.exoplayer:exoplayer-core:2.18.2")
    implementation("com.google.android.exoplayer:exoplayer-ui:2.18.2")
    implementation("com.google.android.exoplayer:exoplayer-hls:2.18.2")

    // PhotoView for zooming capabilities
    implementation("com.github.chrisbanes:PhotoView:2.3.0")

    // ViewPager2 for swiping between images
    implementation("androidx.viewpager2:viewpager2:1.1.0")
}