import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

fun loadKeystoreProperty(name: String): String? {
    return project.findProperty(name) as String?
}

android {
    namespace = "com.sensortools"
    compileSdk = 34

    signingConfigs {
        create("release") {
            val storeFilePath = loadKeystoreProperty("storeFile")
            val storePasswordValue = loadKeystoreProperty("storePassword")
            val keyAliasValue = loadKeystoreProperty("keyAlias")
            val keyPasswordValue = loadKeystoreProperty("keyPassword")

            if (!storeFilePath.isNullOrBlank()) storeFile = rootProject.file(storeFilePath)
            if (!storePasswordValue.isNullOrBlank()) storePassword = storePasswordValue
            if (!keyAliasValue.isNullOrBlank()) keyAlias = keyAliasValue
            if (!keyPasswordValue.isNullOrBlank()) keyPassword = keyPasswordValue
        }
    }

    defaultConfig {
        applicationId = "com.sensortools"
        minSdk = 26
        targetSdk = 34
        versionCode = 3
        versionName = "1.0.3"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)

    // Compose UI
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Activity & Lifecycle
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.3")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Core
    implementation("androidx.core:core-ktx:1.13.1")

    // Debug
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
