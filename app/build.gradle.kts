import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

val keystorePropertiesFile = rootProject.file("keystore.properties")

fun loadKeystoreProperties(): Properties? {
    if (!keystorePropertiesFile.exists()) return null
    val raw = Properties().apply { load(keystorePropertiesFile.inputStream()) }
    val normalized = Properties()
    raw.forEach { (key, value) ->
        normalized[key.toString().trim('\uFEFF', ' ')] = value.toString().trim()
    }
    return normalized
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
