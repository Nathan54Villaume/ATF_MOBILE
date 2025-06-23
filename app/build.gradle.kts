import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Properties
import org.gradle.api.tasks.Exec

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
    id("org.jetbrains.kotlin.plugin.compose")

}

// --- VERSIONING AUTOMATIQUE ---
val versionPropsFile = rootProject.file("version.properties")
val versionProps = Properties().apply {
    if (!versionPropsFile.exists()) {
        versionPropsFile.parentFile.mkdirs()
        versionPropsFile.writeText("versionCode=1\nversionName=1.0")
    }
    load(versionPropsFile.inputStream())
}

val currentCode = versionProps.getProperty("versionCode").toInt()
val currentName = versionProps.getProperty("versionName").toDouble()

tasks.register("incrementVersion") {
    group = "versioning"
    description = "Incrémente versionCode et versionName avant chaque build"
    doLast {
        val nextCode = currentCode + 1
        val nextName = String.format(Locale.US, "%.2f", currentName + 0.01)
        versionProps.setProperty("versionCode", nextCode.toString())
        versionProps.setProperty("versionName", nextName)
        versionProps.store(versionPropsFile.outputStream(), null)
        println("→ versionCode = $nextCode, versionName = $nextName")
    }
}

tasks.named("preBuild") {
    dependsOn("incrementVersion")
}

android {
    namespace = "com.riva.atsmobile"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.riva.atsmobile"
        minSdk = 26
        targetSdk = 35

        versionCode = versionProps.getProperty("versionCode").toInt()
        versionName = versionProps.getProperty("versionName")

        buildConfigField("String", "BUILD_VERSION", "\"$versionName\"")
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
        kotlinCompilerExtensionVersion = "1.5.12"
    }

    kapt {
        correctErrorTypes = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.05.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // Core & Lifecycle
    implementation("androidx.core:core-ktx:1.8.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
    implementation("androidx.activity:activity-compose:1.8.0")
    implementation ("androidx.hilt:hilt-navigation-compose:1.0.0")

    // Room core
    implementation ("androidx.room:room-runtime:2.5.0")
    kapt       ("androidx.room:room-compiler:2.5.0")

    // **Coroutine support pour Room**
    implementation ("androidx.room:room-ktx:2.5.0")

    // Coroutines Android (si besoin)
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")
    // Compose UI
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.ui:ui-text")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Navigation & animations
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.constraintlayout:constraintlayout-compose:1.0.1")
    implementation("com.google.accompanist:accompanist-navigation-animation:0.34.0")
    implementation("com.google.accompanist:accompanist-placeholder-material:0.34.0")
    implementation("com.google.accompanist:accompanist-flowlayout:0.34.0")
    implementation("androidx.compose.animation:animation")

    // Réseau & JSON
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")

    // Graphiques
    implementation("com.github.PhilJay:MPAndroidChart:3.1.0")

    // Stockage local
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    implementation("androidx.room:room-runtime:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    // Remplacement de la ligne media3 non-résolue
    implementation("androidx.media3:media3-common:1.0.0")
    implementation("androidx.media3:media3-common-ktx:1.0.0")

    // (optionnel si tu veux l’UI ou la session)
    implementation("androidx.media3:media3-ui:1.0.0")
    implementation("androidx.media3:media3-session:1.0.0")
    // Caméra & Photo
    implementation("androidx.camera:camera-camera2:1.3.1")
    implementation("androidx.camera:camera-lifecycle:1.3.1")
    implementation("androidx.camera:camera-view:1.3.1")

    // QR & RFID
    implementation("com.google.zxing:core:3.5.2")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")

    // Material XML thème
    implementation("com.google.android.material:material:1.11.0")

    // Logging
    implementation("com.jakewharton.timber:timber:5.0.1")

    

    // PDF
    implementation("com.itextpdf:itext7-core:7.2.5")

    // ViewModel & LiveData Compose
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.compose.runtime:runtime-livedata:1.6.1")
    implementation("androidx.media3:media3-common:1.5.0")
    implementation("androidx.media3:media3-common-ktx:1.5.0")
    // Sécurité
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation ("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
    // Tests
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

}

// --- PUSH AUTO CROSS-PLATFORM APRÈS BUILD ---
val gitAddDev by tasks.registering(Exec::class) {
    group = "build"
    description = "Stage all changes"
    commandLine("git", "add", "-A")
}

val gitCommitDev by tasks.registering(Exec::class) {
    group = "build"
    description = "Commit staged changes if any"
    isIgnoreExitValue = true
    val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
    commandLine("git", "commit", "-m", "chore: build réussi $timestamp")
}

val gitPushDev by tasks.registering(Exec::class) {
    group = "build"
    description = "Push commits to dev branch"
    commandLine("git", "push", "origin", "dev")
}

tasks.matching { it.name.startsWith("assemble") }.configureEach {
    finalizedBy(gitAddDev)
}
gitAddDev.configure { finalizedBy(gitCommitDev) }
//gitCommitDev.configure { finalizedBy(gitPushDev) }
