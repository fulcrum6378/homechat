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
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.7.0"
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
    implementation(libs.room.ktx)
    implementation(libs.room.runtime) // necessary for "suspend"
    implementation(libs.material)
    implementation(libs.keyboardvisibilityevent)
}
