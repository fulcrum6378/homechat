plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
}

android {
    namespace = "ir.mahdiparastesh.homechat"
    compileSdk = 33
    buildToolsVersion = "33.0.0"

    defaultConfig {
        applicationId = "ir.mahdiparastesh.homechat"
        minSdk = 21
        targetSdk = 33
        versionCode = 1
        versionName = "0.4.0"
    }

    sourceSets.getByName("main") {
        java.srcDirs("src/main/java")
        kotlin.srcDirs("src/main/kotlin")
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions { jvmTarget = "1.8" }
    buildFeatures { viewBinding = true }
}

dependencies {
    val roomVersion = "2.4.3"

    implementation("androidx.appcompat:appcompat:1.5.1")
    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.navigation:navigation-fragment-ktx:2.5.2")
    implementation("androidx.preference:preference-ktx:1.2.0")
    kapt("androidx.room:room-compiler:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("com.google.android.material:material:1.6.1")
}
