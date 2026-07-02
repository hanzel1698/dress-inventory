import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

val keystorePropertiesFile = rootProject.file("keystore.properties")

/**
 * Returns signing properties only when ALL of the following are true:
 *  1. keystore.properties exists on disk.
 *  2. The storeFile path it references points to a file that exists and is
 *     at least 100 bytes (a valid JKS is always several hundred bytes; a
 *     truncated/empty file written by a CI step with missing secrets is not).
 *  3. keyAlias, keyPassword, and storePassword are all non-blank.
 *
 * Returning null causes the release build type to fall back to the debug
 * signing config, which lets the build succeed without crashing on a
 * missing or corrupt keystore.
 */
fun loadKeystoreProperties(): Properties? {
    if (!keystorePropertiesFile.exists()) return null
    val raw = Properties().apply { load(keystorePropertiesFile.inputStream()) }
    val props = Properties()
    raw.forEach { (key, value) ->
        props[key.toString().trim('\uFEFF', ' ')] = value.toString().trim()
    }
    // Ensure the actual .jks file is present and non-trivially sized
    val storeFilePath = props.getProperty("storeFile") ?: return null
    val jksFile = rootProject.file(storeFilePath)
    if (!jksFile.exists() || jksFile.length() < 100L) return null
    // Ensure all required signing fields are filled in
    listOf("keyAlias", "keyPassword", "storePassword").forEach { key ->
        if (props.getProperty(key).isNullOrBlank()) return null
    }
    return props
}

android {
    namespace = "com.hanzel.dressinventory"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.hanzel.dressinventory"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "1.1"
    }

    signingConfigs {
        create("release") {
            loadKeystoreProperties()?.let { props ->
                keyAlias = props.getProperty("keyAlias")
                keyPassword = props.getProperty("keyPassword")
                storePassword = props.getProperty("storePassword")
                storeFile = rootProject.file(
                    props.getProperty("storeFile") ?: "keystore/release.jks"
                )
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = if (loadKeystoreProperties() != null) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
}
