plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
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
        versionName = "0.2.0"
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

    packagingOptions {
        //resources.excludes.apply {
        exclude("org/apache/httpcomponents/client5/httpclient5/version.properties")
        exclude("org/apache/httpcomponents/core5/httpcore5-h2/version.properties")
        exclude("org/apache/httpcomponents/core5/httpcore5/version.properties")
        //}
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.5.1")
    //implementation("androidx.core:core-ktx:1.9.0")
    implementation("com.google.android.material:material:1.6.1")
    implementation("org.apache.httpcomponents.client5:httpclient5:5.1.3")
    implementation("org.apache.httpcomponents.core5:httpcore5-h2:5.1.3")
}
