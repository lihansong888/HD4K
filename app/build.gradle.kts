plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

fun prop(name: String, def: String = "") = (project.findProperty(name) as String?) ?: def

android {
    namespace = "com.hd4k.player"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.hd4k.player"
        minSdk = 21
        targetSdk = 34
        versionCode = (System.getenv("HD4K_VERSION_CODE") ?: "1").toInt()
        versionName = System.getenv("HD4K_VERSION_NAME") ?: "1.0.0"
        buildConfigField("String", "CATALOG_URL", "\"${prop("hd4kCatalogUrl")}\"")
        buildConfigField("String", "UPDATE_URL", "\"${prop("hd4kUpdateUrl")}\"")
        buildConfigField("boolean", "DEMO_FALLBACK", prop("hd4kDemoFallback", "true"))
    }
    signingConfigs {
        create("test") {
            storeFile = rootProject.file("ci/test.jks")
            storePassword = "android"
            keyAlias = "hd4k-test"
            keyPassword = "android"
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("test")
        }
    }
    buildFeatures { compose = true; buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    lint { abortOnError = false; checkReleaseBuilds = false }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.core:core-ktx:1.15.0")

    val media3 = "1.5.1"
    implementation("androidx.media3:media3-exoplayer:$media3")
    implementation("androidx.media3:media3-exoplayer-hls:$media3")
    implementation("androidx.media3:media3-exoplayer-dash:$media3")
    implementation("androidx.media3:media3-exoplayer-rtsp:$media3")
    implementation("androidx.media3:media3-exoplayer-smoothstreaming:$media3")
    implementation("androidx.media3:media3-ui:$media3")
    implementation("androidx.media3:media3-datasource-okhttp:$media3")

    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
}
