import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("org.jetbrains.kotlin.kapt") // ✅ pour Room, ZXing, etc.
}

// ✅ Gestion version auto-incrémentée
val versionPropsFile = rootProject.file("version.properties")
val versionProps = Properties().apply {
    load(versionPropsFile.inputStream())
}

val currentCode = versionProps["versionCode"].toString().toInt()
val currentName = versionProps["versionName"].toString().replace(",", ".").toDouble()

val nextCode = currentCode + 1
val nextName = String.format(Locale.US, "%.2f", currentName + 0.01)

versionProps["versionCode"] = nextCode.toString()
versionProps["versionName"] = nextName
versionProps.store(versionPropsFile.outputStream(), null)

android {
    namespace = "com.riva.atsmobile"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.Riva.atfmobile"
        minSdk = 26
        targetSdk = 35
        versionCode = nextCode
        versionName = nextName
        buildConfigField("String", "BUILD_VERSION", "\"$nextName\"")

        val buildTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        buildConfigField("String", "BUILD_TIME", "\"$buildTime\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10"
    }

    kapt {
        correctErrorTypes = true
    }
}

dependencies {
    // 🔧 Compose Core
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation("androidx.compose.material3:material3:1.2.1")
    implementation("com.google.android.material:material:1.11.0")

    implementation("androidx.compose.material:material-icons-extended")

    // 🔧 Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // ✅ UI moderne + animations
    implementation("androidx.constraintlayout:constraintlayout-compose:1.0.1")
    implementation("com.google.accompanist:accompanist-navigation-animation:0.34.0")
    implementation("com.google.accompanist:accompanist-placeholder-material:0.34.0")
    implementation("com.google.accompanist:accompanist-flowlayout:0.34.0")
    implementation("androidx.compose.animation:animation:1.6.1")

    // ✅ SignalR WebSocket
    implementation("com.microsoft.signalr:signalr:6.0.10")

    // ✅ Réseau & JSON
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")

    // ✅ Graphiques / courbes
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // ✅ Stockage local
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    implementation("androidx.room:room-runtime:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // ✅ Caméra / Prise de photo
    implementation("androidx.camera:camera-camera2:1.3.1")
    implementation("androidx.camera:camera-lifecycle:1.3.1")
    implementation("androidx.camera:camera-view:1.3.1")

    // ✅ QR Code / RFID (prévision)
    implementation("com.google.zxing:core:3.5.2")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")

    // ✅ Logs / debug
    implementation("com.jakewharton.timber:timber:5.0.1")

    // ✅ (Optionnel) PDF export
    implementation("com.itextpdf:itext7-core:7.2.5")

    // ✅ ViewModel / LiveData Compose
    implementation("androidx.compose.runtime:runtime-livedata")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    //✅  dépendance de chiffrement
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // ✅ Tests
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
