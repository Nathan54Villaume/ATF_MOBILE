// 1. En-tête : imports et gestion de version.properties
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Properties

// Chemin vers le fichier de propriétés
val versionPropsFile = rootProject.file("version.properties")

// Chargement des propriétés existantes
val versionProps = Properties().apply {
    if (versionPropsFile.exists()) {
        load(versionPropsFile.inputStream())
    } else {
        // Si le fichier n'existe pas, on le crée avec des valeurs par défaut
        setProperty("versionCode", "1")
        setProperty("versionName", "1.00")
        store(versionPropsFile.outputStream(), null)
    }
}

// Récupère et incrémente les versions
val currentCode = versionProps.getProperty("versionCode").toInt()
val currentName = versionProps.getProperty("versionName").toDouble()
val nextCode = currentCode + 1
val nextName = String.format(Locale.US, "%.2f", currentName + 0.01)

// Écrit les nouvelles versions dans le fichier
versionProps.setProperty("versionCode", nextCode.toString())
versionProps.setProperty("versionName", nextName)
versionProps.store(versionPropsFile.outputStream(), null)

// Expose les versions dans des extra properties
extra["nextVersionCode"] = nextCode
extra["nextVersionName"] = nextName

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.riva.atsmobile"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.riva.atsmobile"
        minSdk = 26
        targetSdk = 35

        // On utilise les extra properties
        versionCode = (extra["nextVersionCode"] as Int)
        versionName = (extra["nextVersionName"] as String)

        buildConfigField("String", "BUILD_VERSION", "\"${versionName}\"")
        val buildTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            .format(Date())
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
    kotlinOptions { jvmTarget = "11" }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10"
    }
}


dependencies {
    // --- Coroutines (pour Retrofit + ViewModel, etc.) ---
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")

    // --- OkHttp Logging (pour voir les requêtes/réponses dans Logcat) ---
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    // --- Compose & Material3 ---
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation("androidx.compose.material3:material3:1.2.1")
    implementation("androidx.compose.material:material-icons-extended")

    // --- Navigation & Accompanist ---
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.constraintlayout:constraintlayout-compose:1.0.1")
    implementation("com.google.accompanist:accompanist-navigation-animation:0.34.0")
    implementation("com.google.accompanist:accompanist-placeholder-material:0.34.0")
    implementation("com.google.accompanist:accompanist-flowlayout:0.34.0")
    implementation("androidx.compose.animation:animation:1.6.1")

    // --- Réseau & JSON ---
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.microsoft.signalr:signalr:6.0.10")

    // --- Graphiques ---
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // --- Stockage local ---
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // --- Caméra & QR/RFID ---
    implementation("androidx.camera:camera-camera2:1.3.1")
    implementation("androidx.camera:camera-lifecycle:1.3.1")
    implementation("androidx.camera:camera-view:1.3.1")
    implementation("com.google.zxing:core:3.5.2")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")

    // --- Divers ---
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.jakewharton.timber:timber:5.0.1")
    implementation("com.itextpdf:itext7-core:7.2.5")
    implementation("androidx.compose.runtime:runtime-livedata")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")

    // --- Tests ---
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
