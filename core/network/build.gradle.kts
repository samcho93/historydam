import java.util.Properties

plugins {
    alias(libs.plugins.yeoksadam.android.library)
    alias(libs.plugins.yeoksadam.android.hilt)
    alias(libs.plugins.kotlin.serialization)
}

// local.properties 에서 비밀값을 읽어 BuildConfig 로 노출 (저장소 커밋 금지)
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun secret(key: String, default: String): String =
    (localProps.getProperty(key) ?: System.getenv(key) ?: default)

android {
    namespace = "com.samdori93.yeoksadam.core.network"
    buildFeatures { buildConfig = true }

    defaultConfig {
        buildConfigField(
            "String",
            "API_BASE_URL",
            value = "\"${secret(key = "API_BASE_URL", default = "http://10.0.2.2:8000/")}\""
        )
        // 역사담 웹앱 데이터(유적·인물·초상)와 대화·음성·인식 서버 — 웹앱과 같은 것을 쓴다
        buildConfigField("String", "CATALOG_BASE_URL", "\"${secret("CATALOG_BASE_URL", "https://samcho93.github.io/arHeritage/")}\"")
        buildConfigField("String", "WORKER_BASE_URL", "\"${secret("WORKER_BASE_URL", "https://yeoksadam-api.samdori93.workers.dev/")}\"")
        // Worker 의 APP_KEYS 와 같은 값 (비밀값 아님 — 웹앱의 Origin 확인과 같은 수준)
        buildConfigField("String", "APP_KEY", "\"${secret("APP_KEY", "historydam-android-v1")}\"")
        // Gemini 멀티모달 비전 키 — local.properties 의 GEMINI_API_KEY (커밋 금지)
        buildConfigField(
            "String",
            "GEMINI_API_KEY",
            "\"${secret("GEMINI_API_KEY", "")}\"",
        )
    }
}

dependencies {
    implementation(project(":core:common"))

    implementation(platform(libs.okhttp.bom))
    // core:data 가 HttpException·ResponseBody 를 다루므로 api 로 노출
    api(libs.okhttp.core)
    implementation(libs.okhttp.logging)
    api(libs.retrofit.core)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.kotlinx.serialization.json)

    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
}
