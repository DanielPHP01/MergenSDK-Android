plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace  = "com.mergen.sample"
    compileSdk = 36

    defaultConfig {
        // КРИТИЧНО: должен совпадать с полем app_id в assets/license.json.
        // Лицензия Mergen выдаётся под конкретный applicationId.
        applicationId = "com.mergen.sample"
        minSdk        = 24
        targetSdk     = 36
        versionCode   = 1
        versionName   = "1.0"
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
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

    packaging {
        jniLibs {
            // libc++_shared и libonnxruntime могут присутствовать в нескольких AAR
            // (CameraX, ONNX Runtime, OpenCV). pickFirsts выбирает первую копию,
            // предотвращая ошибку дублирования at link time.
            pickFirsts += "lib/arm64-v8a/libc++_shared.so"
            pickFirsts += "lib/arm64-v8a/libonnxruntime.so"
            pickFirsts += "lib/arm64-v8a/libopencv_java4.so"
        }
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // ── Mergen ID Scanner SDK — единственная внешняя зависимость на SDK ────────
    // Приезжает из GitHub Packages (maven.pkg.github.com/DanielPHP01/MergenSDK-Android).
    // Никаких project() / files() — только Maven-координата.
    implementation("com.mergen:mergen-sdk:2.3.0")

    // ── Compose BOM — управляет версиями Compose-артефактов ───────────────────
    // Используется только для экранов самого sample-приложения.
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // ── Activity / Lifecycle ─────────────────────────────────────────────────
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")

    // ── Core ──────────────────────────────────────────────────────────────────
    implementation("androidx.core:core-ktx:1.15.0")
}
