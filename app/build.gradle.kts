plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.videoflow.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.videoflow.app"
        minSdk = 29          // Android 10 及以上（覆盖绝大多数机型）
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            // 为了让云端能直接产出可安装的 APK，release 关闭混淆，避免签名/缩减带来的额外配置
            isMinifyEnabled = false
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
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2")
    implementation("androidx.activity:activity-compose:1.9.0")

    // Jetpack Compose
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Media3 ExoPlayer 视频播放
    implementation("androidx.media3:media3-exoplayer:1.3.1")
    implementation("androidx.media3:media3-ui:1.3.1")
    // FFmpeg 软件解码扩展：覆盖更多冷门格式（MKV/AVI/FLV/TS 里的奇葩编码、AC3/EAC3/DTS 等）
    // EXTENSION_RENDERER_MODE_ON 模式下，能硬解的格式仍走硬解，只有当设备 MediaCodec 不支持时才回退到 FFmpeg 软解
    implementation("androidx.media3:media3-decoder-ffmpeg:1.3.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
