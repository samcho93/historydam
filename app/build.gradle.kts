import java.util.Properties

plugins {
    alias(libs.plugins.yeoksadam.android.application)
    alias(libs.plugins.yeoksadam.android.hilt)
    alias(libs.plugins.yeoksadam.android.compose)
    alias(libs.plugins.kotlin.serialization)
}

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun secret(key: String, default: String): String =
    (localProps.getProperty(key) ?: System.getenv(key) ?: default)

android {
    namespace = "com.samdori93.yeoksadam"

    defaultConfig {
        applicationId = "com.samdori93.yeoksadam"
        // 네이버 지도 NCP Client ID — local.properties 의 NAVER_MAP_CLIENT_ID (없으면 빈 값 → 인증 실패 시 회색 지도)
        manifestPlaceholders["NAVER_MAP_CLIENT_ID"] = secret("NAVER_MAP_CLIENT_ID", "")
    }

    buildTypes {
        release {
            // 배포용: 코드·리소스 축소. 스토어 등록 전까지는 디버그 키로 서명해 직접 설치(사이드로드)한다.
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    // CPU 별 APK (네이버 지도 엔진이 CPU 마다 약 25MB) — 폰은 arm64-v8a, 에뮬레이터는 x86_64
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86_64")
            isUniversalApk = false
        }
    }
}

dependencies {
    // core
    implementation(project(":core:ui"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:domain"))
    implementation(project(":core:common"))
    implementation(project(":core:data"))

    // feature
    implementation(project(":feature:home"))
    implementation(project(":feature:figure"))
    implementation(project(":feature:map"))
    implementation(project(":feature:ar"))
    implementation(project(":feature:camera"))
    implementation(project(":feature:chat"))
    implementation(project(":feature:voice"))
    implementation(project(":feature:profile"))
    implementation(project(":feature:notification"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
}
