plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
}

android {
    namespace = "ir.mahdiparastesh.homechat"
    compileSdk = 34
    buildToolsVersion = "34.0.0"

    defaultConfig {
        applicationId = "ir.mahdiparastesh.homechat"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.6.0"
    }

    sourceSets.getByName("main") {
        manifest.srcFile("AndroidManifest.xml")
        kotlin.srcDirs("kotlin")
        res.setSrcDirs(listOf("res"))
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_20; targetCompatibility = JavaVersion.VERSION_20
    }
    kotlinOptions { jvmTarget = "20" }
    buildFeatures { viewBinding = true }
}

dependencies {
    val roomVersion = "2.6.0"

    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.5")
    implementation("androidx.preference:preference-ktx:1.2.1")
    //noinspection KaptUsageInsteadOfKsp
    kapt("androidx.room:room-compiler:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion") // necessary for "suspend"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("com.google.android.material:material:1.10.0")
    implementation("net.yslibrary.keyboardvisibilityevent:keyboardvisibilityevent:3.0.0-RC3")
}
