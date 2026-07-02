import java.io.File
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

data class UploadSigningConfig(
    val storeFile: File,
    val storePassword: String,
    val keyAlias: String,
    val keyPassword: String,
)

fun resolveUploadSigning(): UploadSigningConfig? {
    System.getenv("KEYSTORE_FILE")?.takeIf { it.isNotBlank() }?.let { path ->
        val file = File(path)
        if (file.isFile && file.length() > 100L) {
            return UploadSigningConfig(
                storeFile = file,
                storePassword = System.getenv("KEYSTORE_PASSWORD") ?: "android",
                keyAlias = System.getenv("KEY_ALIAS") ?: "androiddebugkey",
                keyPassword = System.getenv("KEY_PASSWORD") ?: "android",
            )
        }
    }
    val centralDir = File(System.getProperty("user.home"), ".android/signing")
    val centralKeystore = File(centralDir, "upload-keystore.jks")
    if (centralKeystore.isFile && centralKeystore.length() > 100L) {
        val props = Properties()
        File(centralDir, "signing.properties").takeIf { it.isFile }?.inputStream()?.use {
            props.load(it)
        }
        return UploadSigningConfig(
            storeFile = centralKeystore,
            storePassword = props.getProperty("storePassword", "android"),
            keyAlias = props.getProperty("keyAlias", "androiddebugkey"),
            keyPassword = props.getProperty("keyPassword", "android"),
        )
    }
    return null
}

val uploadSigning = resolveUploadSigning()

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
        if (uploadSigning != null) {
            create("upload") {
                storeFile = uploadSigning.storeFile
                storePassword = uploadSigning.storePassword
                keyAlias = uploadSigning.keyAlias
                keyPassword = uploadSigning.keyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = if (uploadSigning != null) {
                signingConfigs.getByName("upload")
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
