plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.ksp)
}

android {
    namespace = "ir.mahdiparastesh.homechat"
    compileSdk = 35
    buildToolsVersion = System.getenv("ANDROID_BUILD_TOOLS_VERSION")

    defaultConfig {
        applicationId = "ir.mahdiparastesh.homechat"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "3.2.5"
    }

    sourceSets.getByName("main") {
        manifest.srcFile("AndroidManifest.xml")
        kotlin.srcDirs("kotlin")
        res.setSrcDirs(listOf("res"))
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_22
        targetCompatibility = JavaVersion.VERSION_22
    }
    kotlinOptions { jvmTarget = "22" }

    buildFeatures { buildConfig = true; viewBinding = true }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {
    implementation(libs.core.ktx)
    implementation(libs.navigation)
    implementation(libs.preference)
    ksp(libs.room.compiler)
    implementation(libs.room.ktx) // for "suspend"
    implementation(libs.room.runtime)
    implementation(libs.lottie)
    implementation(libs.material)
    implementation(libs.kbve)
}
